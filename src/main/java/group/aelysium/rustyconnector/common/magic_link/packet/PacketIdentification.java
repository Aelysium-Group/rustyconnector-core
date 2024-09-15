package group.aelysium.rustyconnector.common.magic_link.packet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for {@link group.aelysium.rustyconnector.common.magic_link.packet.Packet.Remote} packet extensions.
 * Must contain a string which follows the requirements of {@link group.aelysium.rustyconnector.common.magic_link.packet.Packet.Identification#parseString(String)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PacketIdentification {
    String value();
}