package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.ara.Closure;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PacketCache implements Closure {
    private final List<Packet.Type> ignoredTypes;
    protected final int max;
    protected final Map<NanoID, Packet> packets;
    protected final List<Packet> packetsOrdered;

    public PacketCache(int max, List<Packet.Type> ignoredTypes) {
        if(max <= 0) max = 0;
        if(max > 1000) max = 1000;

        this.max = max;
        this.ignoredTypes = ignoredTypes;
        this.packets = Collections.synchronizedMap(new LinkedHashMap<>(this.max){
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > PacketCache.this.max;
            }
        });
        this.packetsOrdered = new Vector<>(this.max) {
            @Override
            public boolean add(Packet item) {
                if (size() >= PacketCache.this.max) remove(0);
                return super.add(item);
            }
        };
    }
    public PacketCache(int max) {
        this(max, List.of());
    }
    public PacketCache() {
        this(100);
    }

    /**
     * Caches the specific packet.
     * If this message cache is set to ignore certain statuses or ids, then this method will ignore them and nothing will happen.
     * @param packet The packet to cache.
     */
    public void cache(Packet packet) {
        if(this.ignoredTypes.contains(packet.type())) return;
        this.packets.put(packet.local().replyEndpoint().orElseThrow(), packet); // The local reply endpoint should always be defined.
        this.packetsOrdered.add(packet);
    }

    /**
     * Checks if the specific packet is one that the cache ignores.
     * @param packet The packet to check.
     * @return `true` if the packet will be ignored, `false` otehrwise.
     */
    public boolean ignoredType(@NotNull Packet packet) {
        return this.ignoredTypes.contains(packet.type());
    }

    /**
     * Finds a cached packet.
     * @param id The id of the cached packet.
     * @return The cached packet if it's available. Otherwise, an empty optional.
     */
    public @NotNull Optional<Packet> find(@NotNull NanoID id) {
        return Optional.ofNullable(this.packets.get(id));
    }

    /**
     * @return All currently cached packets.
     */
    public @NotNull List<Packet> packets() {
        return this.packetsOrdered;
    }

    public int size() { return this.packets.size(); }

    public void empty() { this.packets.clear(); }

    @Override
    public void close() {
        this.packets.clear();
    }
}
