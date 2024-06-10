package group.aelysium.rustyconnector.proxy.event_handlers;

import group.aelysium.rustyconnector.toolkit.common.events.Listener;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.MCLoaderLeaveEvent;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;

public class OnMCLoaderLeave implements Listener<MCLoaderLeaveEvent> {
    public void handler(MCLoaderLeaveEvent event) {
        IMCLoader mcLoader = event.mcLoader();

        mcLoader.leave(event.player());
    }
}