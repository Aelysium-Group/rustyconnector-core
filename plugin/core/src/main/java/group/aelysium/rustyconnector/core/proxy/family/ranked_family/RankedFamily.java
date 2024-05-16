package group.aelysium.rustyconnector.core.proxy.family.ranked_family;

import group.aelysium.rustyconnector.core.proxy.family.Family;
import group.aelysium.rustyconnector.core.proxy.family.matchmaking.Matchmaker;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.ranked_family.IRankedFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IMatchmaker;
import group.aelysium.rustyconnector.core.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public class RankedFamily extends Family implements IRankedFamily {
    public RankedFamily(
            @NotNull String id,
            @NotNull Connector connector,
            String displayName,
            String parent
    ) throws Exception {
        super(id, connector, displayName, parent);
    }

    public static class Tinder extends Particle.Tinder<IFamily> {
        private final IRankedFamily.Settings settings;

        public Tinder(@NotNull IRankedFamily.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull IFamily ignite() throws Exception {
            Flux<IMatchmaker> matchmaker = (new Matchmaker.Tinder(settings.matchmaker(), settings.gameId())).flux();

            Flux<IWhitelist> whitelist = null;
            if (settings.whitelist() != null)
                whitelist = (new Whitelist.Tinder(settings.whitelist())).flux();

            return new RankedFamily(
                    settings.id(),
                    new IRankedFamily.Connector(matchmaker, whitelist),
                    settings.displayName(),
                    settings.parent()
            );
        }
    }
}