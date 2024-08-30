package group.aelysium.rustyconnector.common.magic_link;

public interface WebSocketMagicLink {
    /**
     * This token is used in conjunction with AES-256 encryption to authenticate the server to the proxy.
     * This string exists simply as an agreed upon string to
     */
    String AUTH_TOKEN = "RUSTYCONNECTOR-CONNECTION-TOKEN";
}
