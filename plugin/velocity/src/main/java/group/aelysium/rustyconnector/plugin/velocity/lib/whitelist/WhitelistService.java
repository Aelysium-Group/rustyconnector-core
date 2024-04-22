package group.aelysium.rustyconnector.plugin.velocity.lib.whitelist;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.whitelist.IWhitelistService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WhitelistService extends IWhitelistService {
    private final Map<String, IWhitelist> registeredWhitelists = new HashMap<>();
    private @Nullable IWhitelist proxyWhitelist;

    public Optional<IWhitelist> proxyWhitelist() {
        return Optional.ofNullable(proxyWhitelist);
    }

    public void proxyWhitelist(@Nullable IWhitelist whitelist) {
        this.proxyWhitelist = whitelist;
        if(whitelist == null) return;
        this.registeredWhitelists.put(whitelist.name(), whitelist);
    }

    public Optional<IWhitelist> find(String name) {
        IWhitelist whitelist = this.registeredWhitelists.get(name);
        if(whitelist == null) return Optional.empty();

        return Optional.of(whitelist);
    }

    /**
     * Add a whitelist to this manager.
     * @param whitelist The whitelist to add to this manager.
     */
    @Override
    public void add(IWhitelist whitelist) {
        this.registeredWhitelists.put(whitelist.name(),whitelist);
    }

    /**
     * Remove a whitelist from this manager.
     * @param whitelist The whitelist to remove from this manager.
     */
    @Override
    public void remove(IWhitelist whitelist) {
        this.registeredWhitelists.remove(whitelist.name());
    }

    @Override
    public List<IWhitelist> dump() {
        return this.registeredWhitelists.values().stream().toList();
    }

    @Override
    public void clear() {
        this.registeredWhitelists.clear();
    }

    @Override
    public void close() throws Exception {
        this.registeredWhitelists.clear();
    }

    public static class Tinder extends Particle.Tinder<Particle> {
        @Override
        public @NotNull Particle ignite() throws Exception {
            return new WhitelistService();
        }
    }
}
