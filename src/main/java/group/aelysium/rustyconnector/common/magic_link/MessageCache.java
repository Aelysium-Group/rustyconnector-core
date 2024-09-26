package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.ara.Closure;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.crypt.Snowflake;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

import java.util.*;

public class MessageCache implements Closure {
    private final Snowflake snowflakeGenerator = new Snowflake();
    private final List<Packet.Status> ignoredStatuses;
    private final List<Packet.Identification> ignoredTypes;
    protected final int max;
    protected final Map<NanoID, Packet.Remote> messages;
    protected final List<Packet.Remote> messagesOrdered;

    public MessageCache(int max, List<Packet.Status> ignoredStatuses, List<Packet.Identification> ignoredTypes) {
        if(max <= 0) max = 0;
        if(max > 500) max = 500;

        this.max = max;
        this.ignoredStatuses = ignoredStatuses;
        this.ignoredTypes = ignoredTypes;
        this.messages = Collections.synchronizedMap(new LinkedHashMap<>(this.max){
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > MessageCache.this.max;
            }
        });
        this.messagesOrdered = new Vector<>(this.max) {
            @Override
            public boolean add(Packet.Remote item) {
                if (size() >= MessageCache.this.max) remove(0);
                return super.add(item);
            }
        };
    }
    public MessageCache(int max) {
        this(max, List.of(), List.of());
    }
    public MessageCache() {
        this(50);
    }

    /**
     * Caches the specific packet.
     * If this message cache is set to ignore certain statuses or ids, then this method will ignore them and nothing will happen.
     * @param packet The packet to cache.
     */
    public void cache(Packet.Remote packet) {
        if(this.ignoredStatuses.contains(packet.status())) return;
        if(this.ignoredTypes.contains(packet.identification())) return;
        this.messages.put(packet.local().replyEndpoint().orElseThrow(), packet); // The local reply endpoint should always be defined.
        this.messagesOrdered.add(packet);
    }

    public boolean ignoredType(Packet message) {
        return this.ignoredTypes.contains(message.identification());
    }

    /**
     * Gets a cached packet.
     * @param id The id of the cached packet.
     * @return The cached message if it's available. Otherwise, an empty optional.
     */
    public Optional<Packet.Remote> findMessage(NanoID id) {
        return Optional.ofNullable(this.messages.get(id));
    }

    /**
     * Get all currently cached messages.
     * @return All currently cached messages.
     */
    public List<Packet.Remote> messages() {
        return this.messagesOrdered;
    }

    /**
     * Get a page view of all currently cached messages.
     * @param pageNumber The page number to look at. Pages are split by 10. Page numbers start at 1 and go up.
     * @return A list of all cached messages inside a page.
     */
    public List<Packet.Remote> fetchMessagesPage(int pageNumber) {
        if(pageNumber < 1) pageNumber = 1;

        pageNumber--;

        int lowerIndex = (10 * pageNumber);
        int upperIndex = lowerIndex + 10;

        if(upperIndex > this.size()) upperIndex = this.size();

        List<Packet.Remote> messages = new ArrayList<>(this.messages());

        Collections.reverse(messages);

        return messages.subList(lowerIndex,upperIndex);
    }

    public int size() { return this.messages.size(); }

    public void empty() { this.messages.clear(); }

    @Override
    public void close() {
        this.messages.clear();
    }
}
