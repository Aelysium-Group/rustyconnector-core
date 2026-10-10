package group.aelysium.rustyconnector.common.modules;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a plugin leveraging a {@link URLClassLoader}. However, it restricts the plugin from
 * using the system classloader thereby trimming access to all system classes.
 */
public class ModuleClassLoader extends URLClassLoader {
    private final List<String> parentSharedPackages = new ArrayList<>(List.of(
            "group.aelysium",
            "net.kyori.adventure",
            "java"
    ));
    private final List<String> blockedPackages = new ArrayList<>(List.of(
            "group.aelysium.rustyconnector.modules"
    ));

    private final ClassLoader parentClassLoader;
    private final List<String> exportedPackages = new ArrayList<>();
    private final ModuleLoader moduleLoader;

    public ModuleClassLoader(List<URL> urls, ClassLoader parentClassLoader, List<String> exportedPackages, ModuleLoader moduleLoader) {
        super(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        this.parentClassLoader = parentClassLoader;
        if (exportedPackages != null) {
            this.exportedPackages.addAll(exportedPackages);
        }
        this.moduleLoader = moduleLoader;
    }

    public boolean exportsPackage(String className) {
        return exportedPackages.stream().anyMatch(className::startsWith);
    }

    public Class<?> loadExportedClass(String name) throws ClassNotFoundException {
        return super.loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // has the class loaded already?
        Class<?> loadedClass = findLoadedClass(name);
        
        if (loadedClass == null) {
            final boolean isParentShared = parentSharedPackages.stream().anyMatch(name::startsWith) && blockedPackages.stream().noneMatch(name::startsWith);
            
            if (isParentShared) {
                try {
                    loadedClass = parentClassLoader.loadClass(name);
                } catch (ClassNotFoundException ignore) {}
            }
            
            if (loadedClass == null && moduleLoader != null) {
                loadedClass = moduleLoader.findClassInModules(name, this);
            }
            
            if (loadedClass == null) {
                loadedClass = super.loadClass(name, resolve);
            }
        }

        if (resolve && loadedClass != null) resolveClass(loadedClass);
        return loadedClass;
    }
}
