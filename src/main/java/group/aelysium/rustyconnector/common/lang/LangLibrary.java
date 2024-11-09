package group.aelysium.rustyconnector.common.lang;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.plugins.Plugin;
import group.aelysium.rustyconnector.common.errors.Error;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.newline;

public class LangLibrary implements Plugin {
    private final Map<String, LangNode> nodes = new ConcurrentHashMap<>();
    private final ASCIIAlphabet asciiAlphabet;

    protected LangLibrary(
            @NotNull ASCIIAlphabet asciiAlphabet
    ) {
        this.asciiAlphabet = asciiAlphabet;
    }

    public void registerLangNode(String id, LangNode node) {
        this.nodes.put(id, node);
    }

    public void registerLangNode(Field field) throws RuntimeException {
        if(!field.isAnnotationPresent(Lang.class)) throw new RuntimeException("The field "+field.getName()+" isn't annotated with @Lang.");
        Lang annotation = field.getAnnotation(Lang.class);
        if(this.nodes.containsKey(annotation.value()) && annotation.strict()) return;
        if(!this.nodes.containsKey(annotation.value()) && annotation.required()) return;

        Class<?> clazz = field.getType();
        Type type = field.getGenericType();

        LangNode node = null;
        if(Component.class.isAssignableFrom(clazz)) node = createNode(annotation.value(), arguments->{
            try {
                return (Component) field.get(Component.class);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });
        if(String.class.isAssignableFrom(clazz)) node = createNode(annotation.value(), arguments->{
            try {
                return Component.text((String) field.get(String.class));
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });
        if(String[].class.isAssignableFrom(clazz)) node = createNode(annotation.value(), arguments->{
            try {
                return Component.join(
                        JoinConfiguration.separator(newline()),
                        Arrays.stream(((String[]) field.get(String[].class))).map(Component::text).toList()
                );
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (type instanceof ParameterizedType parameterizedType) {
            if (List.class.isAssignableFrom(clazz)) {
                Class<?> entryClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                if (String.class.isAssignableFrom(entryClass)) node = createNode(annotation.value(), arguments->{
                    try {
                        return Component.join(
                                JoinConfiguration.separator(newline()),
                                ((List<String>) field.get(List.class)).stream().map(Component::text).toList()
                        );
                    } catch(Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        if(node == null) throw new RuntimeException("Fields annotated with @Lang must be of type: String, String[], List<String>, or Component.");

        this.nodes.put(annotation.value(), node);
    }
    public void registerLangNode(Method method) throws RuntimeException {
        if(!method.isAnnotationPresent(Lang.class)) throw new RuntimeException("The method "+method.getName()+" isn't annotated with @Lang.");
        Lang annotation = method.getAnnotation(Lang.class);
        if(this.nodes.containsKey(annotation.value()) && annotation.strict()) return;
        if(!this.nodes.containsKey(annotation.value()) && annotation.required()) return;

        Class<?> clazz = method.getReturnType();
        Class<?>[] parameters = method.getParameterTypes();

        LangNode node = null;
        if(Component.class.isAssignableFrom(clazz)) node = createNode(annotation.value(), arguments->{
            if(parameters.length != arguments.length) throw new IllegalArgumentException("Incorrect number of arguments provided for the lang node: "+annotation.value());

            for (int i = 0; i < parameters.length; i++) {
                Class<?> currentParameter = parameters[i];
                Object currentArgument = arguments[i];
                if(currentArgument == null) throw new IllegalArgumentException("Incorrect number of arguments provided for the lang node: "+annotation.value());
                if(!currentParameter.isAssignableFrom(currentArgument.getClass())) throw new IllegalArgumentException("Incorrect type provided for argument "+(i + 1)+". Expected "+currentParameter.getSimpleName()+" but got "+currentArgument.getClass().getSimpleName());
            }

            try {
                return (Component) method.invoke(null, arguments);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if(String.class.isAssignableFrom(clazz)) node = createNode(annotation.value(), arguments->{
            if(parameters.length != arguments.length) throw new IllegalArgumentException("Incorrect number of arguments provided for the lang node: "+annotation.value());

            for (int i = 0; i < parameters.length; i++) {
                Class<?> currentParameter = parameters[i];
                Object currentArgument = arguments[i];
                if(currentArgument == null) throw new IllegalArgumentException("Incorrect number of arguments provided for the lang node: "+annotation.value());
                if(!currentParameter.isAssignableFrom(currentArgument.getClass())) throw new IllegalArgumentException("Incorrect type provided for argument "+(i + 1)+". Expected "+currentParameter.getSimpleName()+" but got "+currentArgument.getClass().getSimpleName());
            }

            try {
                return Component.text((String) method.invoke(null, arguments));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if(node == null) throw new RuntimeException("Methods annotated with @Lang must return either Component or String.");

        this.registerLangNode(annotation.value(), node);
    }

    /**
     * Registers all methods and fields annotated with {@link Lang} in the class.
     * Specifically, this method uses {@link Class#getMethods()} and {@link Class#getFields()} which means it will also work if the class extends a superclass.
     * However, you must ensure that all members of the class are public. In the case of a field, you should make sure it's final so that it won't be mutated.
     * @param clazz The class to register nodes from.
     */
    public void registerLangNodes(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Field[] fields = clazz.getFields();

        Arrays.stream(methods).forEach(m -> {
            try {
                if(!Modifier.isStatic(m.getModifiers())) return;
                registerLangNode(m);
            } catch (Exception ignore) {}
        });
        Arrays.stream(fields).forEach(f -> {
            try {
                if(!Modifier.isStatic(f.getModifiers())) return;
                registerLangNode(f);
            } catch (Exception ignore) {}
        });
    }

    public LangNode lang(String id) {
        return Optional.ofNullable(this.nodes.get(id)).orElse(createNode(id, a -> Component.text("[Missing: "+id+"]")));
    }

    public ASCIIAlphabet asciiAlphabet() {
        return this.asciiAlphabet;
    }

    /**
     * Gets the full list of currently registered lang nodes.
     * @return The full list of registered lang nodes.
     */
    public Set<String> langNodes() {
        return Collections.unmodifiableSet(this.nodes.keySet());
    }

    @Override
    public void close() {
        this.nodes.clear();
    }

    private static LangNode createNode(@NotNull String name, @NotNull Function<Object[], Component> function) {
        return new LangNode(name) {
            @Override
            public Component generate(Object... arguments) throws RuntimeException {
                try {
                    return function.apply(arguments);
                } catch (Exception e) {
                    Error error = Error.from(e).whileAttempting("To call the Lang tag: "+this.name);
                    RC.Error(error);
                    return Component.text("[Error: "+this.name+"]("+error.uuid()+")");
                }
            }
        };
    }

    @Override
    public @NotNull String name() {
        return LangLibrary.class.getSimpleName();
    }

    @Override
    public @NotNull String description() {
        return "Provides declarative language services.";
    }

    @Override
    public @NotNull Component details() {
        return RC.Lang("rustyconnector-langLibraryDetails").generate(this);
    }

    @Override
    public boolean hasPlugins() {
        return false;
    }

    @Override
    public @NotNull Map<String, Flux<? extends Plugin>> plugins() {
        return Map.of();
    }

    public static class Tinder extends Particle.Tinder<LangLibrary> {
        private ASCIIAlphabet asciiAlphabet = DEFAULT_ASCII_ALPHABET;

        public Tinder() {}

        public Tinder asciiAlphabet(ASCIIAlphabet asciiAlphabet) {
            this.asciiAlphabet = asciiAlphabet;
            return this;
        }

        @Override
        public @NotNull LangLibrary ignite() throws Exception {
            return new LangLibrary(
                    this.asciiAlphabet
            );
        }

        public static LangLibrary.Tinder DEFAULT_LANG_LIBRARY = new LangLibrary.Tinder();
    }

    public static ASCIIAlphabet DEFAULT_ASCII_ALPHABET = new EnglishAlphabet();
}
