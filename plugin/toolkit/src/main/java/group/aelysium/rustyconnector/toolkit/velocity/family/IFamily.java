package group.aelysium.rustyconnector.toolkit.velocity.family;

import group.aelysium.rustyconnector.toolkit.RustyConnector;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.central.Kernel;
import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;

import java.util.Optional;

public interface IFamily<Connector extends IFamilyConnector<?>> extends IPlayerConnectable, Particle {

    String id();
    String displayName();
    Optional<Flux<IWhitelist>> whitelist();
    long players();

    /**
     * Returns this family's {@link IFamilyConnector}.
     * The family's connector is used to handle players when they connect or disconnect from this family.
     * @return {@link IFamilyConnector}
     */
    Connector connector();

    /**
     * Fetches a reference to the parent of this family.
     * The parent of this family should always be either another family, or the root family.
     * If this family is the root family, this method will always return `null`.
     * @return {@link IFamily}
     */
    Optional<Particle.Flux<IFamily<?>>> parent();

    class Reference extends group.aelysium.rustyconnector.toolkit.velocity.util.Reference<IFamily, String> {
        private boolean rootFamily = false;

        public Reference(String name) {
            super(name);
        }
        protected Reference() {
            super(null);
            this.rootFamily = true;
        }

        public <TFamily extends IFamily> TFamily get() {
            Kernel tinder = RustyConnector.Toolkit.proxy().orElseThrow();

            if(rootFamily) return (TFamily) tinder.services().family().rootFamily();
            return (TFamily) tinder.services().family().find(this.referencer).orElseThrow();
        }

        public <TFamily extends IFamily> TFamily get(boolean fetchRoot) {
            Kernel tinder = RustyConnector.Toolkit.proxy().orElseThrow();

            if(rootFamily) return (TFamily) tinder.services().family().rootFamily();
            if(fetchRoot)
                try {
                    return (TFamily) tinder.services().family().find(this.referencer).orElseThrow();
                } catch (Exception ignore) {
                    return (TFamily) tinder.services().family().rootFamily();
                }
            else return (TFamily) tinder.services().family().find(this.referencer).orElseThrow();
        }

        public static Reference rootFamily() {
            return new Reference();
        }
    }
}
