package group.aelysium.rustyconnector.common.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class URL {
    private Protocol protocol;
    private final String domain;
    private final Integer port;
    private final List<String> path;
    private final Map<String, String> query;
    private final String fragment;
    protected URL(
            @NotNull Protocol protocol,
            @NotNull String domainName,
            @Nullable Integer port,
            @Nullable List<String> path,
            @Nullable Map<String, String> query,
            @Nullable String fragment
    ) {
        this.protocol = protocol;
        this.domain = domainName;
        this.port = port;
        this.path = (path == null ? new ArrayList<>() : path);
        this.query = query;
        this.fragment = fragment;
    }

    public Protocol protocol() {
        return protocol;
    }

    public String domain() {
        return domain;
    }

    public Integer port() {
        return port;
    }

    public List<String> path() {
        return path;
    }

    public Map<String, String> query() {
        return query;
    }

    public String fragment() {
        return fragment;
    }

    /**
     * Copies the contents of this URL into another one.
     * The new URL is a completely seperate instance from this one.
     * @return A new URL.
     */
    public URL copy() {
        return new URL(
                this.protocol,
                this.domain,
                this.port,
                this.path,
                this.query,
                this.fragment
        );
    }

    /**
     * Appends the path string to the existing URL path.
     * If slashes are included in the path string they will be handled accordingly.
     * @param path The path to add at the end of the URL.
     * @return The URL, modified with the new path. The returned object is the same as the one used to make this call.
     */
    public URL prependPath(String path) {
        if(path.startsWith("/")) path = path.substring(1);
        if(path.endsWith("/")) path = path.substring(0, path.length() - 1);
        List<String> oldPath = new ArrayList<>(this.path);
        this.path.clear();
        if(path.contains("/")) this.path.addAll(Arrays.stream(path.split("/")).toList());
        else this.path.add(path);
        this.path.addAll(oldPath);
        return this;
    }

    /**
     * Appends the path string to the existing URL path.
     * If slashes are included in the path string they will be handled accordingly.
     * @param path The path to add at the end of the URL.
     * @return The URL, modified with the new path. The returned object is the same as the one used to make this call.
     */
    public URL appendPath(String path) {
        if(path.startsWith("/")) path = path.substring(1);
        if(path.endsWith("/")) path = path.substring(0, path.length() - 1);
        if(path.contains("/")) {
            this.path.addAll(Arrays.stream(path.split("/")).toList());
            return this;
        }
        this.path.add(path);
        return this;
    }

    /**
     * Empties the URL's path.
     */
    public URL clearPath() {
        this.path.clear();
        return this;
    }

    /**
     * Changes the protocol used by this URL.
     * @param protocol The new protocol to use.
     * @return The URL, modified with the new path. The returned object is the same as the one used to make this call.
     */
    public URL changeProtocol(Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public URI toURI() {
        return URI.create(this.toString());
    }

    /**
     * Returns the string representation of the url.
     * If a specific part of the URL doesn't exist, it will be removed from the output.
     */
    @Override
    public String toString() {
        return this.protocol.name().toLowerCase() + "://" +
               String.join(".", domain) + (this.port != null ? ":" + this.port : "") + "/" +
               (!this.path.isEmpty() ? String.join("/", this.path) : "") +
               (this.query != null && !this.query.isEmpty() ? "?" + String.join("&", this.query.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"))) : "") +
               (this.fragment != null ? "#"+fragment : "");

    }

    /**
     * Parses the provided string as a URL. The string may be in the format of:<br/>
     * https://some.example.com:100/test/endpoint?with=params#andFragment<br/>
     * <br/>
     * The minimum required url parameters are:<br/><br/>
     * https://example.com/
     * @param url The string to parse.
     * @return A parsed URL.
     * @throws ParseException If there was an issue parsing the string.
     */
    public static URL parseURL(String url) throws ParseException {
        Map<String, String> parsedURL = Map.of(
                "protocol",     extractPart("^([a-z]*):\\/\\/", url),
                "domainName",       extractPart("\\:\\/\\/([^:/]*)", url),
                "port",         extractPart(":(\\d+)", url),
                "path",         extractPart(":\\d+(\\/[^?#]*)", url),
                "query",        extractPart("\\?([^#]*)", url),
                "fragment",     extractPart("#(.*)", url)
        );

        return new URL(
                Protocol.valueOf(parsedURL.get("protocol").toUpperCase()),
                parsedURL.get("domainName"),
                parsedURL.get("port").isEmpty() ? null : Integer.parseInt(parsedURL.get("port")),
                parsedURL.get("path").isEmpty() ? null : List.of(parsedURL.get("path").split("/")),
                parsedURL.get("query").isEmpty() ? null : Arrays.stream(parsedURL.get("query").split("&")).map(s -> s.split("=")).collect(Collectors.toMap(keyValue -> keyValue[0], keyValue -> keyValue[1])),
                parsedURL.get("fragment").isEmpty() ? null : parsedURL.get("fragment")
        );
    }

    private static String extractPart(String pattern, String url) {
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        return matcher.find() ? matcher.group(1) : "";
    }

    public enum Protocol {
        HTTP,
        HTTPS,
        FTP,
        FILE,
        DATA,
        WS,
        WSS,
        IRC,
        TCP
    }
}
