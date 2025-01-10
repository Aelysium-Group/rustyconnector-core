package group.aelysium.rustyconnector.common.plugins;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.Error;
import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.classloader.IsolatedClassLoader;
import net.byteflux.libby.logging.LogLevel;
import net.byteflux.libby.logging.adapters.LogAdapter;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class PluginLoader {
    private final static IsolatedClassLoader classLoader = new IsolatedClassLoader();
    private final static LogAdapter logger = new LogAdapter() {
        @Override
        public void log(LogLevel level, String message) {
            try {
                RC.Adapter().log(Component.text(message));
            } catch (Exception ignore) {
                System.out.println(message);
            }
        }

        @Override
        public void log(LogLevel level, String message, Throwable throwable) {
            try {
                RC.Adapter().log(Error.from(throwable).detail("Message", message).toComponent());
            } catch (Exception ignore) {
                System.out.println(message);
            }
        }
    };

    public static void loadPlugins(Particle.Flux<? extends RCKernel<?>> kernel, String modulesDirectory) {
        System.out.println("Loading RustyConnector Modules...");
        File modules = new File(modulesDirectory);
        try {
            if(!modules.exists()) modules.mkdirs();

            File[] files = modules.listFiles((dir, name) -> name.endsWith(".jar"));

            if (files == null) return;
            for (File file : files) {
                System.out.println("Found: "+file.getName());
                loadAndRelocateJar(kernel, file);
            }
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To register external modules via `"+modules.getAbsolutePath()+"`"));
        }
        System.out.println("RC Done!");
    }

    private static void loadAndRelocateJar(Particle.Flux<? extends RCKernel<?>> flux, File jarFile) throws Exception {
//        LibraryManager libraryManager = new LibraryManager(logger, jarFile.getParentFile().toPath(), "libs") {
//            @Override
//            protected void addToClasspath(Path file) {
//                try {
//                    URL url = file.toUri().toURL();
//                    classLoader.addURL(url);
//                } catch (Exception ex) {
//                    throw new RuntimeException("Error adding " + file + " to classpath", ex);
//                }
//            }
//        };
//        Library lib = Library.builder()
//                .url(jarFile.getAbsolutePath())
//                .build();
//        libraryManager.loadLibrary(lib);

        System.out.println("Done relocating!");

        // Load classes from the isolated class loader
        try (JarInputStream jarStream = new JarInputStream(new FileInputStream(jarFile))) {
            JarEntry jarEntry;
            while ((jarEntry = jarStream.getNextJarEntry()) != null) {
                if (!jarEntry.getName().endsWith(".class")) continue;

                String className = jarEntry.getName().replace("/", ".").replace(".class", "");
                System.out.println("Scanning "+className);
                Class<?> clazz = null;
                try {
                    clazz = classLoader.loadClass(className);
                } catch (Exception ignore) {
                    continue;
                }

                System.out.println("--------------------FOUND "+className);
                if(!RC.Plugin.Initializer.class.isAssignableFrom(clazz)) continue;
                System.out.println("--------------------RESOLVED "+className);
                RC.Plugin.Initializer plugin = (RC.Plugin.Initializer) clazz.getDeclaredConstructor().newInstance();

                flux.onStart(plugin::onStart);
                flux.onClose(plugin::onClose);
            }
        } catch (Exception e) {
            (new Exception("Failed to load "+jarFile.getName()+"!", e)).printStackTrace();
        }
    }
}
