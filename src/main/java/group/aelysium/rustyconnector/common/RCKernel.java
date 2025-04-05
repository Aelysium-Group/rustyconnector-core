package group.aelysium.rustyconnector.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.common.util.MetadataHolder;
import group.aelysium.rustyconnector.common.util.Parameter;
import group.aelysium.rustyconnector.proxy.util.Version;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RCKernel<A extends RCAdapter> extends ModuleCollection<Module> implements Module, MetadataHolder<Parameter> {
    protected final String id;
    protected final Version version;
    protected final A adapter;
    protected final Path directory;
    protected final Path moduleDirectory;
    private final Map<String, Parameter> metadata = new ConcurrentHashMap<>();

    public RCKernel(
            @NotNull String id,
            @NotNull A adapter,
            @NotNull Path directory,
            @NotNull Path moduleDirectory
    ) throws Exception {
        if(id.length() > 64) throw new IllegalArgumentException("The Kernel ID cannot be longer than 64 characters.");
        
        try (InputStream input = RCKernel.class.getClassLoader().getResourceAsStream("rustyconnector-metadata.json")) {
            if (input == null) throw new NullPointerException("Unable to initialize version number from jar.");
            Gson gson = new Gson();
            JsonObject object = gson.fromJson(new String(input.readAllBytes()), JsonObject.class);
            this.version = new Version(object.get("version").getAsString());
        }
        
        this.id = id;
        this.adapter = adapter;
        this.directory = directory;
        this.moduleDirectory = moduleDirectory;
    }
    
    @Override
    public boolean storeMetadata(String key, Parameter value) {
        return this.metadata.putIfAbsent(key, value) == null;
    }
    
    @Override
    public <T extends Parameter> Optional<T> fetchMetadata(String key) {
        return Optional.ofNullable((T) this.metadata.get(key));
    }
    
    @Override
    public void removeMetadata(String key) {
        this.metadata.remove(key);
    }
    
    @Override
    public Map<String, Parameter> metadata() {
        return Collections.unmodifiableMap(this.metadata);
    }

    /**
     * @return The id of this kernel.
     *         The id shouldn't change between re-boots.
     */
    public String id() {
        return this.id;
    }
    
    /**
     * @return The directory that the RustyConnector kernel jar is located.
     *         This path use useful for deciding where to load configuration files.
     */
    public Path directory() {
        return this.directory;
    }
    
    /**
     * @return The directory that RustyConnector modules are located (typically an `rc-modules` folder)
     */
    public Path moduleDirectory() {
        return this.directory;
    }
    
    /**
     * @return The directory that RustyConnector modules are located (typically an `rc-modules` folder)
     *         This path use useful for deciding where to load configuration files.
     */
    public Path moduleConfigDirectory() {
        return this.directory;
    }

    /**
     * @return The current version of RustyConnector
     */
    public Version version() {
        return this.version;
    }

    public A Adapter() {
        return this.adapter;
    }

    @Override
    public void close() throws Exception {
        super.close();
    }
}
