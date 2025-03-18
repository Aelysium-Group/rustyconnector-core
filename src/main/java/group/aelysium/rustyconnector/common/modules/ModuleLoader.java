package group.aelysium.rustyconnector.common.modules;

import com.google.gson.Gson;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCAdapter;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleLoader implements AutoCloseable {
    protected Gson gson = new Gson();
    protected final List<ModuleClassLoader> classLoaders = new ArrayList<>();
    private final Map<String, ModuleRegistrar> registrars = new HashMap<>();

    public void queueFromFolder(String modulesDirectory) {
        System.out.println("Loading modules.");
        File modules = new File(modulesDirectory);
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if(!modules.exists()) modules.mkdirs();

            File[] files = modules.listFiles((dir, name) -> name.endsWith(".jar"));

            if (files == null) return;

            for (File file : files) {
                try {
                    ModuleConfiguration config;
                    {
                        ModuleClassLoader resourceGrabber = new ModuleClassLoader(
                            List.of(file.toURI().toURL()),
                            getClass().getClassLoader(),
                            List.of()
                        );
                        InputStream stream = resourceGrabber.getResourceAsStream("rc-module.json");
                        if(stream == null) throw new NullPointerException("No rc-module.json exists for "+file.getName());
    
                        try(InputStreamReader reader = new InputStreamReader(stream)) {
                            config = gson.fromJson(reader, ModuleConfiguration.class);
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
                    if(this.registrars.containsKey(lowerConfigName))
                        throw new IllegalStateException("Duplicate module names '"+config.name()+"' are not allowed! Ignoring "+file.getName());
                    List<String> softDeps = Stream.of(config.softDependencies(), config.softDependency(), config.softDepend())
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .distinct()
                        .toList();

                    Consumer<RCKernel<?>> registerRunnable = k -> {
                        try {
                            String environment = (
                                k instanceof ServerKernel ? "server" :
                                k instanceof ProxyKernel ? "proxy" :
                                "other"
                            );

                            if (!config.environments.contains(environment)) {
                                RC.Error(Error.from(config.name() + " does not support '"+environment+"' environments! " + config.name() + " only supports: " + String.join(", ", config.environments)).urgent(true));
                                return;
                            }

                            Module m = k.registerModule(new Module.Builder<>(config.name(), config.description()) {
                                @Override
                                public Module get() {
                                    try {
                                        return plugin.onStart(new ExternalModuleBuilder.Context(
                                            config.name(),
                                            config.description(),
                                            Set.copyOf(config.environments()),
                                            softDeps.stream().filter(ModuleLoader.this.registrars::containsKey).collect(Collectors.toSet()),
                                            environment,
                                            k.moduleDirectory(),
                                            k.directory()
                                        ));
                                    } catch (Exception e) {
                                        RC.Error(Error.from(e).whileAttempting("To register "+config.name()).urgent(true));
                                    }
                                    return null;
                                }
                            });
                            
                            try {
                                if (k instanceof ServerKernel s) plugin.bind(s, m);
                                if (k instanceof ProxyKernel p) plugin.bind(p, m);
                            } catch (Exception e) {
                                RC.Error(Error.from(e).whileAttempting("To bind "+config.name()+" to the kernel.").urgent(true));
                                k.unregisterModule(config.name());
                            }
                        } catch (Exception e) {
                            RC.Error(Error.from(e).whileAttempting("To register the module: "+lowerConfigName));
                        }
                    };
                    
                    this.registrars.put(lowerConfigName, new ModuleRegistrar(
                        lowerConfigName,
                        registerRunnable,
                        Stream.of(config.dependencies(), config.dependency(), config.depend())
                            .filter(Objects::nonNull)
                            .flatMap(List::stream)
                            .distinct()
                            .toList(),
                        softDeps
                    ));
                } catch (Exception e) {
                    RC.Error(Error.from(e).whileAttempting("To register the module: "+file.getName()));
                } finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            }
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To register external modules via `"+modules.getAbsolutePath()+"`"));
        } finally {
            System.out.println("Done loading modules.");
        }
    }
    
    public void queue(ModuleRegistrar registrar) {
        this.registrars.put(registrar.name, registrar);
    }
    
    /**
     * Resolves the dependencies of all modules and registers them to the kernel.
     * Once this has run, all queued registrars will be dequeued.
     */
    public void resolveAndRegister(Flux<? extends RCKernel<?>> flux) {
        List<String> order = ModuleDependencyResolver.sortPlugins(Set.copyOf(this.registrars.values()));
        
        flux.onStart(k->order.forEach(o -> {
            System.out.println("Loading "+o);
            try {
                ModuleRegistrar registrar = this.registrars.get(o);
                registrar.register.accept(k);
            } catch (Exception e) {
                RC.Error(Error.from(e).whileAttempting("To register the module `"+o+"`"));
            }
        }));
        
        this.registrars.clear();
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
    
    private record ModuleConfiguration(
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
    
    public record ModuleRegistrar(
        String name,
        Consumer<RCKernel<?>> register,
        List<String> dependencies,
        List<String> softDependencies
    ) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ModuleRegistrar moduleRegistrar = (ModuleRegistrar) o;
            return Objects.equals(name, moduleRegistrar.name);
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }
}
