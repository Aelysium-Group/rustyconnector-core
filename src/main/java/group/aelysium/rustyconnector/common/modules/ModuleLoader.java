package group.aelysium.rustyconnector.common.modules;

import com.google.gson.Gson;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleLoader implements AutoCloseable {
    protected Gson gson = new Gson();
    protected List<ModuleClassLoader> classLoaders = new ArrayList<>();

    public void loadFromFolder(Flux<? extends RCKernel<?>> flux, String modulesDirectory) {
        System.out.println("Loading modules.");
        File modules = new File(modulesDirectory);
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if(!modules.exists()) modules.mkdirs();

            File[] files = modules.listFiles((dir, name) -> name.endsWith(".jar"));

            if (files == null) return;

            Map<String, ExternalModuleBuilder<Module>> preparedModules = new HashMap<>();
            Map<String, PluginConfiguration> preparedModulesConfigs = new HashMap<>();
            for (File file : files) {
                try {
                    PluginConfiguration config;
                    {
                        ModuleClassLoader resourceGrabber = new ModuleClassLoader(
                            List.of(file.toURI().toURL()),
                            getClass().getClassLoader(),
                            List.of()
                        );
                        InputStream stream = resourceGrabber.getResourceAsStream("rc-module.json");
                        if(stream == null) throw new NullPointerException("No rc-module.json exists for "+file.getName());
    
                        try(InputStreamReader reader = new InputStreamReader(stream)) {
                            config = gson.fromJson(reader, PluginConfiguration.class);
                        }
                        resourceGrabber.close();
                    }
                    
                    ModuleClassLoader classLoader = new ModuleClassLoader(
                        List.of(file.toURI().toURL()),
                        getClass().getClassLoader(),
                        config.sharedPackages == null ? List.of() : config.sharedPackages
                    );
                    Thread.currentThread().setContextClassLoader(classLoader);
                    Class<?> entrypoint = classLoader.loadClass(config.main());
                    if(!ExternalModuleBuilder.class.isAssignableFrom(entrypoint))
                        throw new ClassCastException("The `main` class must extend "+ ExternalModuleBuilder.class.getName());

                    Constructor<ExternalModuleBuilder<Module>> constructor = (Constructor<ExternalModuleBuilder<Module>>) entrypoint.getDeclaredConstructor();

                    constructor.setAccessible(true);
                    ExternalModuleBuilder<Module> plugin = constructor.newInstance();
                    constructor.setAccessible(false);

                    String lowerConfigName = config.name().toLowerCase();
                    if(preparedModules.containsKey(lowerConfigName))
                        throw new IllegalStateException("Duplicate module names "+config.name()+" are not allowed! Ignoring "+file.getName());

                    preparedModules.put(lowerConfigName, plugin);
                    preparedModulesConfigs.put(lowerConfigName, config);
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To register the module: "+file.getName()));
                } finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            }

            List<String> order = sortPlugins(preparedModulesConfigs);

            flux.onStart(k->order.forEach(o -> {
                System.out.println("Loading "+o);
                try {
                    PluginConfiguration c = preparedModulesConfigs.get(o);

                    try {
                        if (k instanceof ServerKernel && !c.environments.contains("server"))
                            RC.Error(Error.from(c.name() + " does not support server environments! " + c.name() + " only supports: " + String.join(", ", c.environments)).urgent(true));
                        if (k instanceof ProxyKernel && !c.environments.contains("proxy"))
                            RC.Error(Error.from(c.name() + " does not support proxy environments! " + c.name() + " only supports: " + String.join(", ", c.environments)).urgent(true));
                    } catch (IllegalStateException e) {
                        System.out.println(e.getMessage());
                        return;
                    }

                    ExternalModuleBuilder<Module> t = preparedModules.get(o);
                    Module m = k.registerModule(new Module.Builder<>(c.name(), c.description()) {
                        @Override
                        public Module get() {
                            try {
                                return t.onStart(k.moduleDirectory());
                            } catch (Exception e) {
                                RC.Error(Error.from(e));
                            }
                            return null;
                        }
                    });

                    if(k instanceof ServerKernel s) t.bind(s, m);
                    if(k instanceof ProxyKernel p) t.bind(p, m);

                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To register the module: "+o));
                }
            }));
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To register external modules via `"+modules.getAbsolutePath()+"`"));
        } finally {
            System.out.println("Done loading modules.");
        }
    }

    @Override
    public void close() throws Exception {
        classLoaders.forEach(c -> {
            try {
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private record PluginConfiguration(
        @NotNull String main,
        @NotNull String name,
        @NotNull String description,
        @NotNull List<String> environments,
        @Nullable List<String> sharedPackages,
        // These are variants of eachother for better UX.
        @Nullable List<String> dependencies,
        @Nullable List<String> dependency,
        @Nullable List<String> depend,
        // These are variants of eachother for better UX.
        @Nullable List<String> softDependencies,
        @Nullable List<String> softDependency,
        @Nullable List<String> softDepend
    ) {}
    
    private static List<String> sortPlugins(Map<String, PluginConfiguration> configs) throws IllegalArgumentException {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();
        
        
        configs.forEach((k, v) -> {
            String key = k.toLowerCase();
            inDegree.put(key, 0);
            graph.put(key, new ArrayList<>());
        });
        
        Set<String> missingDependencies = new HashSet<>();
        configs.forEach((k, v) -> {
            String key = k.toLowerCase();
            
            Stream.of(v.dependencies(), v.dependency(), v.depend())
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .forEach(dep -> {
                    String depKey = dep.toLowerCase();
                    if (!configs.containsKey(depKey)) {
                        missingDependencies.add(key);
                        inDegree.put(key, -1);
                        return;
                    }
                    graph.get(depKey).add(key);
                    inDegree.put(key, inDegree.get(key) + 1);
                });
            Stream.of(v.softDependencies(), v.softDependency(), v.softDepend())
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .forEach(softDep -> {
                    String softDepKey = softDep.toLowerCase();
                    if (!configs.containsKey(softDepKey)) return;
                    graph.get(softDepKey).add(key);
                    inDegree.put(key, inDegree.get(key) + 1);
                });
        });
        
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() == 0)
                queue.add(entry.getKey());
        
        List<String> sortedList = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sortedList.add(current);
            
            for (String neighbor : graph.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                
                if (inDegree.get(neighbor) != 0) continue;
                
                queue.add(neighbor);
            }
        }
        
        Set<String> circularDependencies = new HashSet<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() > 0)
                circularDependencies.add(entry.getKey());
        
        if (!missingDependencies.isEmpty())
            System.out.println("The following plugins have missing dependencies and were not loaded: " + String.join(", ", missingDependencies));
        if (!circularDependencies.isEmpty())
            System.out.println("The following plugins have circular dependencies and were not loaded: " + String.join(", ", circularDependencies));
        
        return sortedList;
    }
    
    
}
