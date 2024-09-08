package group.aelysium.rustyconnector.common.cache;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

import java.util.Date;

public class CacheableMessage {
    private final Long snowflake;
    private final Date date;
    private final String contents;
    private String reason;
    private Packet.Status status;

    public CacheableMessage(Long snowflake, String contents, Packet.Status status) {
        this.snowflake = snowflake;
        this.contents = contents;
        this.date = new Date();
        this.status = status;
    }

    public Long getSnowflake() {
        return this.snowflake;
    }

    public String getContents() {
        return this.contents;
    }

    public Date getDate() {
        return this.date;
    }

    public Packet.Status getSentence() {
        return this.status;
    }
    public String getSentenceReason() {
        return this.reason;
    }

    public void sentenceMessage(Packet.Status status) {
        this.status = status;
        this.reason = null;
    }

    public void sentenceMessage(Packet.Status status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "Snowflake ID: "+this.snowflake.toString()+
               " Contents: "+this.contents+
               " Date: "+this.date+
               " Status: "+this.contents;
    }
}
