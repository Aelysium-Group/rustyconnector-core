package group.aelysium.rustyconnector.common.model;

import group.aelysium.rustyconnector.toolkit.common.serviceable.ServiceHandler;
import group.aelysium.rustyconnector.toolkit.common.serviceable.Serviceable;

public abstract class IKLifecycle<H extends ServiceHandler> extends Serviceable<H> {
    protected IKLifecycle(H services) {
        super(services);
    }

    /**
     * Kill the object. Once this has been called the object should be guaranteed to be garbage collected.
     */
    protected abstract void kill();

    /**
     * Init the object.
     */
    protected static IKLifecycle init() {
        return null;
    }
}
