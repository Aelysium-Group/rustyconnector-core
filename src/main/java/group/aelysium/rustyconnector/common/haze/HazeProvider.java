package group.aelysium.rustyconnector.common.haze;

import group.aelysium.haze.Database;
import group.aelysium.ara.Particle;
import group.aelysium.haze.exceptions.HazeException;
import group.aelysium.haze.lib.DataRequest;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.plugins.PluginCollection;
import group.aelysium.rustyconnector.common.plugins.PluginHolder;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HazeProvider implements Particle, PluginHolder {
    private final PluginCollection databases = new PluginCollection();
    private final Map<String, HazeRequest> nodes = new ConcurrentHashMap<>();

    public HazeProvider(Flux<? extends Database> flux) throws Exception {
        Database database = flux.observe();
        this.databases.registerPlugin("main", flux);
        this.databases.registerPlugin(database.name(), flux);
    }

    public void registerHazeRequest(Method method) throws RuntimeException {
        if(method.isAnnotationPresent(Haze.class)) return;
        Haze annotation = method.getAnnotation(Haze.class);
        Class<?> clazz = method.getReturnType();
        Class<?>[] parameters = method.getParameterTypes();

        if(!DataRequest.class.isAssignableFrom(clazz)) return;

        nodes.put(annotation.value(), new HazeRequest() {
            @Override
            public DataRequest generate(Object... arguments) throws HazeException {
                if(parameters.length != arguments.length) throw new IllegalArgumentException("Incorrect number of arguments provided for the lang node: "+annotation.value());

                for (int i = 0; i < parameters.length; i++) {
                    Class<?> currentParameter = parameters[i];
                    Object currentArgument = arguments[i];
                    if(currentArgument == null) throw new IllegalArgumentException("Incorrect number of arguments provided for the lang node: "+annotation.value());
                    if(!currentParameter.isAssignableFrom(currentArgument.getClass())) throw new IllegalArgumentException("Incorrect type provided for argument "+(i + 1)+". Expected "+currentParameter.getSimpleName()+" but got "+currentArgument.getClass().getSimpleName());
                }

                try {
                    return (DataRequest) method.invoke(null, arguments);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void registerHazeRequest(Class<?> clazz) {
        Method[] methods = clazz.getMethods();

        Arrays.stream(methods).forEach(m -> {
            try {
                if(!Modifier.isStatic(m.getModifiers())) return;
                registerHazeRequest(m);
            } catch (Exception ignore) {}
        });
    }

    public @NotNull Set<String> nodes() {
        return Collections.unmodifiableSet(this.nodes.keySet());
    }
    public @NotNull Optional<HazeRequest> fetch(String key) {
        return Optional.ofNullable(this.nodes.get(key));
    }
    public @NotNull Flux<? extends Database> primaryDatabase() {
        return this.databases.fetchPlugin("main");
    }
    public @NotNull Optional<Flux<? extends Database>> fetchDatabase(@NotNull String name) {
        return Optional.ofNullable(this.databases.fetchPlugin(name));
    }
    public void removeDatabase(@NotNull String name) {
        Flux<? extends Database> flux = this.databases.fetchPlugin(name);
        this.databases.unregister(name);
        flux.close();
    }
    public void registerDatabase(@NotNull Flux<? extends Database> flux) throws Exception {
        Database database = flux.observe();
        if(containsDatabase(database.name())) {
            flux.close();
            throw new RuntimeException("You're not allowed to register two databases with the same name.");
        }
        this.databases.registerPlugin(database.name(), flux);
    }
    public boolean containsDatabase(@NotNull String name) {
        return this.databases.contains(name);
    }

    @Override
    public void close() throws Exception {
        this.databases.close();
    }

    @Override
    public Map<String, Flux<? extends Particle>> plugins() {
        return this.databases.plugins();
    }

    public static abstract class Tinder extends RC.Plugin.Tinder<HazeProvider> {
        public Tinder() {
            super(
                "Haze",
                "Provides abstracted database connections.",
                "rustyconnector-hazeDetails"
            );
        }
    }
}
