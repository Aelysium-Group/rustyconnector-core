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
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

                    if(preparedModules.containsKey(config.name()))
                        throw new IllegalStateException("Duplicate module names "+config.name()+" are not allowed! Ignoring "+file.getName());

                    preparedModules.put(config.name(), plugin);
                    preparedModulesConfigs.put(config.name(), config);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            }

            List<String> order = getLoadingOrder(preparedModulesConfigs);

            flux.onStart(k->order.forEach(o -> {
                try {
                    PluginConfiguration c = preparedModulesConfigs.get(o);
                    ExternalModuleTinder<?> t = preparedModules.get(o);
                    k.registerModule(new ModuleTinder<>(c.name(), c.description(), c.details()) {
                        @Override
                        public @NotNull Particle ignite() throws Exception {
                            return t.ignite();
                        }
                    });

                    if(k instanceof ServerKernel s) t.bind(s);
                    if(k instanceof ProxyKernel p) t.bind(p);
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To register the module: "+o));
                }
            }));
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To register external modules via `"+modules.getAbsolutePath()+"`"));
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

    private List<String> getLoadingOrder(Map<String, PluginConfiguration> plugins) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjList = new HashMap<>();

        // Initialize inDegree and adjacency list
        for (String plugin : plugins.keySet()) {
            inDegree.put(plugin, 0);
            adjList.put(plugin, new ArrayList<>());
        }

        // Build the graph
        for (String plugin : plugins.keySet()) {
            PluginConfiguration config = plugins.get(plugin);
            if (config.dependencies() != null) {
                for (String dep : config.dependencies()) {
                    adjList.get(dep).add(plugin);
                    inDegree.put(plugin, inDegree.get(plugin) + 1);
                }
            }
        }

        // Topological sort using Kahn's algorithm
        Queue<String> queue = new LinkedList<>();
        for (String plugin : inDegree.keySet()) {
            if (inDegree.get(plugin) == 0) {
                queue.offer(plugin);
            }
        }

        List<String> loadingOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String plugin = queue.poll();
            loadingOrder.add(plugin);

            for (String neighbor : adjList.get(plugin)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // If the graph has cycles, loading order won't include all plugins
        if (loadingOrder.size() != plugins.size())
            throw new RuntimeException("Cyclic dependency detected unable to load plugins.");

        return loadingOrder;
    }

    private record PluginConfiguration(
            @NotNull String main,
            @NotNull String name,
            @NotNull String description,
            @NotNull String details,
            @Nullable List<String> dependencies
    ) {}
}
