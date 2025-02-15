package group.aelysium.rustyconnector.common.modules;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a plugin leveraging a {@link URLClassLoader}. However, it restricts the plugin from
 * using the system classloader thereby trimming access to all system classes.
 *
 * Only the classes in SHARED_PACKAGES are visible to the plugin.
 */
public class ModuleClassLoader extends URLClassLoader {
    private final List<String> sharedPackages = new ArrayList<>(List.of(
            "group.aelysium",
            "net.kyori.adventure",
            "java"
    ));
    private final List<String> blockedPackages = new ArrayList<>(List.of(
            "group.aelysium.rustyconnector.modules"
    ));

    private final ClassLoader parentClassLoader;

    public ModuleClassLoader(List<URL> urls, ClassLoader parentClassLoader, List<String> sharedPackages) {
        super(urls.toArray(new URL[0]), null);
        this.parentClassLoader = parentClassLoader;
        this.sharedPackages.addAll(sharedPackages);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // has the class loaded already?
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            final boolean isSharedClass = sharedPackages.stream().anyMatch(name::startsWith) && blockedPackages.stream().noneMatch(name::startsWith);
            if(isSharedClass) loadedClass = parentClassLoader.loadClass(name);
            else loadedClass = super.loadClass(name, resolve);
        }

        if(resolve) resolveClass(loadedClass);
        return loadedClass;
    }
}