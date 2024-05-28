package group.aelysium.rustyconnector.toolkit.common.message_cache;

import group.aelysium.rustyconnector.toolkit.common.packet.PacketStatus;

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
