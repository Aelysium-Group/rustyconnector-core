package group.aelysium.rustyconnector.common.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

public class ConfigLoader {
    /**
     * Retrieve data from a specific configuration node.
     * @param data The configuration data to search for a specific node.
     * @param node The node to search for.
     * @param type The type to convert the retrieved data to.
     * @return Data with a type matching `type`
     * @throws IllegalStateException If there was an issue while retrieving the data or converting it to `type`.
     */
    static Object getValue(CommentedConfigurationNode data, String node, Type type) throws IllegalStateException {
        try {
            String[] steps = node.split("\\.");

            final CommentedConfigurationNode[] currentNode = {data};
            Arrays.stream(steps).forEach(step -> {
                currentNode[0] = currentNode[0].node(step);
            });

            if(currentNode[0] == null) throw new NullPointerException();

            return currentNode[0].get(type);
        } catch (NullPointerException e) {
            throw new IllegalStateException("The node ["+node+"] doesn't exist!");
        } catch (ClassCastException e) {
            throw new IllegalStateException("The node ["+node+"] is of the wrong data type! Make sure you are using the correct type of data!");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to register the node: "+node);
        }
    }

    protected static CommentedConfigurationNode loadOrGenerate(String path, List<ConfigEntry> contents) {
        File configPointer = new File(path);

        try {
            if (!configPointer.exists()) {
                File parent = configPointer.getParentFile();
                if (!parent.exists()) parent.mkdirs();

                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .file(configPointer)
                        .build();

                CommentedConfigurationNode root = loader.load(ConfigurationOptions.defaults());

                for (ConfigEntry entry : contents) {
                    if(entry instanceof ConfigNode node)
                        root.node(Arrays.stream(node.key().split("\\.")).toList()).set(node.value);
                    if(entry instanceof ConfigComment comment)
                        root.node(Arrays.stream(comment.key().split("\\.")).toList()).comment(String.join("\n", comment.value()));
                }
                loader.save(root);
            }

            return YamlConfigurationLoader.builder()
                        .indent(2)
                        .path(configPointer.toPath())
                        .build().load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static void enterValue(Map<Integer, List<ConfigEntry>> entries, ConfigEntry entry) {
        entries.computeIfAbsent(entry.order(), k -> new ArrayList<>()).add(entry);
    }

    /**
     * Loads a configuration from a class definition.
     * Config class must be annotated with the {@link Config} annotation, and members must be marked with {@link Comment} and/or {@link Node} annotations.
     * If a method has the {@link Node} annotation, the value of the entry will be loaded into the actual method.
     * If you want to add naked comments, you can add the {@link Comment} annotation to an empty method.
     * @param clazz The class definition.
     * @param pathReplacements If the {@link Config#value()} has curly braces covered paths (i.e. "some/{dynamic}/path.yml", those will be replaced with the provided replacements in the order they appear.
     * @throws IOException If the config filepath contains invalid characters.
     */
    public static <T> T loadConfiguration(Class<T> clazz, String... pathReplacements) throws IOException {
        if(!clazz.isAnnotationPresent(Config.class)) throw new RuntimeException("Configs must be annotated with @Config");

        Config config = clazz.getAnnotation(Config.class);

        // Prep config file path
        Pattern filePathValidation = Pattern.compile("^[a-zA-Z0-9\\_\\-\\.\\/\\\\]+$");
        String path;
        {
            List<String> splitPath = Arrays.stream(config.value().split("/")).map(v -> {
                if (v.startsWith("{") && v.endsWith("}")) return v.substring(1, v.length() - 1);
                return v;
            }).toList();
            path = String.join("/", splitPath);
        }
        if(!filePathValidation.matcher(path).matches())
            throw new IOException("Invalid file path defined for config: "+path);

        Map<Integer, List<ConfigEntry>> entries = new HashMap<>();
        try {
            Comment comment = clazz.getAnnotation(Comment.class);
            enterValue(entries, new ConfigComment(Integer.MIN_VALUE, "", comment.value()));
        } catch (Exception ignore) {}

        // Load all Comment and Entry annotations
        Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers())).toList()
                .forEach(f -> {
            boolean hasComment = f.isAnnotationPresent(Comment.class);
            boolean hasEntry = f.isAnnotationPresent(Node.class);
            if(!(hasComment || hasEntry)) return;

            if(hasEntry) {
                Node node = f.getAnnotation(Node.class);
                enterValue(entries, new ConfigNode(node.order(), node.key(), node.defaultValue(), f));

                if(hasComment) {
                    Comment comment = f.getAnnotation(Comment.class);
                    enterValue(entries, new ConfigComment(node.order(), node.key(), comment.value()));
                }
            }
        });

        // Compile Comment and Entry annotations for file printing.
        List<ConfigEntry> sortedEntries = new ArrayList<>();
        {
            List<Map.Entry<Integer, List<ConfigEntry>>> list = new ArrayList<>(entries.entrySet());
            list.sort((entry1, entry2) -> entry2.getKey().compareTo(entry1.getKey()));

            list.forEach(entry -> sortedEntries.addAll(entry.getValue()));
        }

        // Generates the config if it doesn't exist then loads the contents of the config.
        CommentedConfigurationNode yaml = loadOrGenerate("", sortedEntries);

        // Construct the Java object with all the provided data
        List<ConfigNode> nodes = (List<ConfigNode>) (Object) sortedEntries.stream().filter(v -> v instanceof ConfigNode).toList();

        // Populate the object instance with the data.
        try {
            T instance = clazz.getConstructor().newInstance();

            for (ConfigNode node : nodes) {
                Type type = node.field.getGenericType();
                node.field.set(instance, getValue(yaml, node.key(), type));
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static abstract class ConfigEntry {
        private final int order;
        public ConfigEntry(int order) {
            this.order = order;
        }

        public int order() {
            return this.order;
        }
    }
    public static class ConfigComment extends ConfigEntry {
        private final String key;
        private final List<String> value;
        public ConfigComment(int order, String key, String[] value) {
            super(order);
            this.key = key;
            this.value = Arrays.stream(value).map(s -> {
                if(s.startsWith("#")) return s;
                if(s.startsWith(" ")) return "#"+s;
                return "# "+s;
            }).toList();
        }
        public String key() {
            return this.key;
        }
        public List<String> value() {
            return this.value;
        }
    }
    public static class ConfigNode extends ConfigEntry {
        private final String key;
        private final Object value;
        private final Field field;
        public ConfigNode(int order, String key, Object value, Field field) {
            super(order);
            this.key = key;
            this.value = value;
            this.field = field;
        }

        public String key() {
            return this.key;
        }

        public Object value() {
            return this.value;
        }

        public Field field() {
            return this.field;
        }
    }
}
