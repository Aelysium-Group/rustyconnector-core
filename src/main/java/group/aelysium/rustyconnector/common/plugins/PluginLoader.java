package group.aelysium.rustyconnector.common.plugins;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.Error;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class PluginLoader implements AutoCloseable {
    protected Gson gson = new Gson();
    protected List<PluginClassLoader> classLoaders = new ArrayList<>();
    protected final List<String> sharedPackages;

    public PluginLoader() {
        this.sharedPackages = List.of();
    }
    public PluginLoader(
            List<String> sharedPackages
    ) {
        this.sharedPackages = sharedPackages;
    }

    public void loadPlugins(Particle.Flux<? extends RCKernel<?>> flux, String modulesDirectory) {
        File modules = new File(modulesDirectory);
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if(!modules.exists()) modules.mkdirs();

            File[] files = modules.listFiles((dir, name) -> name.endsWith(".jar"));

            if (files == null) return;
            for (File file : files) {
                try {
                    PluginClassLoader classLoader = new PluginClassLoader(
                            List.of(file.toURI().toURL()),
                            getClass().getClassLoader(),
                            this.sharedPackages
                    );
                    Thread.currentThread().setContextClassLoader(classLoader);

                    InputStream stream = classLoader.getResourceAsStream("rc-module.json");
                    if(stream == null) throw new NullPointerException("No rc-module.json exists for "+file.getName());
                    try(InputStreamReader reader = new InputStreamReader(stream)) {
                        JsonObject object = gson.fromJson(reader, JsonObject.class);

                        if(!object.has("main")) throw new NullPointerException("rc-module.json must contain `main` which points to the entrypoint of the module.");
                        Class<?> entrypoint = classLoader.loadClass(object.get("main").getAsString());

                        if(!RC.Plugin.Initializer.class.isAssignableFrom(entrypoint))
                            throw new ClassCastException("The `main` class must implement "+RC.Plugin.Initializer.class.getName());

                        Constructor<RC.Plugin.Initializer> constructor = (Constructor<RC.Plugin.Initializer>) entrypoint.getDeclaredConstructor();

                        constructor.setAccessible(true);
                        RC.Plugin.Initializer plugin = constructor.newInstance();
                        constructor.setAccessible(false);

                        flux.onStart(plugin::onStart);
                        flux.onClose(plugin::onClose);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }
            }
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
}
