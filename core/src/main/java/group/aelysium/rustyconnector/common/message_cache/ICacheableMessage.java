package group.aelysium.rustyconnector.common.message_cache;

import group.aelysium.rustyconnector.common.magic_link.packet.PacketStatus;

import java.util.Date;

public interface ICacheableMessage {
    Long getSnowflake();

    String getContents();

    Date getDate();

    PacketStatus getSentence();

    String getSentenceReason();

    void sentenceMessage(PacketStatus status);

    void sentenceMessage(PacketStatus status, String reason);
}
