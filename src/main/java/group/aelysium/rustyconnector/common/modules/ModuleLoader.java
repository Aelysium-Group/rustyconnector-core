package group.aelysium.rustyconnector.common.modules;

import com.google.gson.Gson;
import group.aelysium.ara.Particle;
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

public class ModuleLoader implements AutoCloseable {
    protected Gson gson = new Gson();
    protected List<ModuleClassLoader> classLoaders = new ArrayList<>();
    protected final List<String> sharedPackages;

    public ModuleLoader() {
        this.sharedPackages = List.of();
    }
    public ModuleLoader(
            List<String> sharedPackages
    ) {
        this.sharedPackages = sharedPackages;
    }

    public void loadFromFolder(Particle.Flux<? extends RCKernel<?>> flux, String modulesDirectory) {
        System.out.println("Loading modules.");
        File modules = new File(modulesDirectory);
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if(!modules.exists()) modules.mkdirs();

            File[] files = modules.listFiles((dir, name) -> name.endsWith(".jar"));

            if (files == null) return;

            Map<String, ExternalModuleTinder<?>> preparedModules = new HashMap<>();
            Map<String, PluginConfiguration> preparedModulesConfigs = new HashMap<>();
            for (File file : files) {
                try {
                    ModuleClassLoader classLoader = new ModuleClassLoader(
                            List.of(file.toURI().toURL()),
                            getClass().getClassLoader(),
                            this.sharedPackages
                    );
                    Thread.currentThread().setContextClassLoader(classLoader);

                    InputStream stream = classLoader.getResourceAsStream("rc-module.json");
                    if(stream == null) throw new NullPointerException("No rc-module.json exists for "+file.getName());

                    PluginConfiguration config;
                    try(InputStreamReader reader = new InputStreamReader(stream)) {
                        config = gson.fromJson(reader, PluginConfiguration.class);
                    }

                    Class<?> entrypoint = classLoader.loadClass(config.main());
                    if(!ExternalModuleTinder.class.isAssignableFrom(entrypoint))
                        throw new ClassCastException("The `main` class must extend "+ExternalModuleTinder.class.getName());

                    Constructor<ExternalModuleTinder<?>> constructor = (Constructor<ExternalModuleTinder<?>>) entrypoint.getDeclaredConstructor();

                    constructor.setAccessible(true);
                    ExternalModuleTinder<?> plugin = constructor.newInstance();
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

                    ExternalModuleTinder<?> t = preparedModules.get(o);
                    ModuleParticle m = k.registerModule(new ModuleTinder<>(c.name(), c.description()) {
                        @Override
                        public @NotNull ModuleParticle ignite() throws Exception {
                            return t.ignite();
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
        @Nullable List<String> dependencies,
        @Nullable List<String> softDependencies
    ) {}

    private static List<String> sortPlugins(Map<String, PluginConfiguration> configs) throws IllegalArgumentException {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();

        configs.forEach((k,v)->{
            inDegree.put(k, 0);
            graph.put(k, new ArrayList<>());
        });

        Set<String> missingDependencies = new HashSet<>();
        configs.forEach((k, v) -> {
            if (v.dependencies() != null) {
                for (String dep : v.dependencies()) {
                    if (!configs.containsKey(dep.toLowerCase())) {
                        missingDependencies.add(k);
                        inDegree.put(k, -1);
                        return;
                    }
                    graph.get(dep).add(k);
                    inDegree.put(k, inDegree.get(k) + 1);
                }
            }
            
            if (v.softDependencies() != null) {
                for (String softDep : v.softDependencies()) {
                    if (configs.containsKey(softDep.toLowerCase())) {
                        graph.get(softDep).add(k);
                        inDegree.put(k, inDegree.get(k) + 1);
                    }
                }
            }
        });

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() != 0) continue;

            queue.add(entry.getKey());
        }

        List<String> sortedList = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sortedList.add(current);

            for (String neighbor : graph.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);

                if (inDegree.get(neighbor) == 0) queue.add(neighbor);
            }
        }

        Set<String> circularDependencies = new HashSet<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet())
            if (entry.getValue() > 0)
                circularDependencies.add(entry.getKey());

        if (!missingDependencies.isEmpty())
            RC.Error(Error.from("The following plugins have missing dependencies and were not loaded: " + String.join(", ", missingDependencies)).urgent(true));
        if (!circularDependencies.isEmpty())
            RC.Error(Error.from("The following plugins have circular dependencies and were not loaded: " + String.join(", ", circularDependencies)).urgent(true));

        return sortedList;
    }
}
