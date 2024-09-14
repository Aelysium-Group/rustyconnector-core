package group.aelysium.rustyconnector.common.magic_link.packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketListener {
    /**
     * The packet that this listen is targeting.
     * It's expected that, when you
     */
    Class<? extends Packet.Remote> value();
}