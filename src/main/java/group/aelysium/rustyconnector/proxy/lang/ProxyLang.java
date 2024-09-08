package group.aelysium.rustyconnector.proxy.lang;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.lang.ASCIIAlphabet;
import group.aelysium.rustyconnector.common.lang.Lang;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.proxy.family.scalar_family.ScalarFamily;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class ProxyLang extends Lang {
    public ProxyLang(ASCIIAlphabet asciiAlphabet) {
        super(asciiAlphabet);
    }

    public String noPlayer(String username) {
        return "There is no online player with the username " + username;
    }
    public String alreadyConnected() {
        return "You're already connected to this server.";
    }

    public Component usage() {
        return usageBox(
            join(
                newlines(),
                text("/rc family", AQUA),
                text("View family related information.", DARK_GRAY),
                space(),
                text("/rc message", AQUA),
                text("Access recently sent RustyConnector messages.", DARK_GRAY),
                space(),
                text("/rc reload", GOLD),
                text("Reload entire plugin.", DARK_GRAY),
                space(),
                text("/rc send", AQUA),
                text("Send players from families and servers to other families or servers.", DARK_GRAY)
            )
        );
    }

    public String noFamily(@NotNull String familyName) {
        return "There is no family with the name: " + familyName;
    }
    public String noServer(@NotNull String serverName) {
        return "There is no server with the name: " + serverName;
    }
    public String sameFamily() {
        return "You're already in that server.";
    }

    public Component families() {
        AtomicReference<Component> families = new AtomicReference<>(text(""));

        for (Particle.Flux<? extends Family> family : RC.P.Families().dump())
            family.executeNow(f -> families.set(families.get().append(text("["+f.id()+"*] ").color(BLUE))));

        return boxed(
            join(
                newlines(),
                this.asciiAlphabet.generate("registered"),
                space(),
                this.asciiAlphabet.generate("families"),
                space(),
                border(),
                space(),
                families.get(),
                text("*root family", GRAY),
                space(),
                border(),
                space(),
                text("/rc family <family word_id>",DARK_AQUA),
                text("See more details about a particular family.", GRAY)
            )
        );
    };

    public Component server(Server server) {
        boolean hasServerName = server.uuid().toString().equals(server.uuidOrDisplayName());
        return Component.text("["+server.uuid()+"] "+ (hasServerName ? server.uuidOrDisplayName() : "") +"("+ AddressUtil.addressToString(server.address()) +") ["+server.players()+" ("+server.softPlayerCap()+" <--> "+server.hardPlayerCap()+") w-"+server.weight()+"]");
    }

    public Component family(
            @NotNull String familyID,
            @Nullable String parentFamily,
            @NotNull Map<String, String> parameters,
            @NotNull List<Server> familyServers,
            @Nullable Component status
    ) {
        AtomicReference<Component> serversComponent = new AtomicReference<>(familyServers.isEmpty() ? text("There are no servers to show.", DARK_GRAY) : empty());
        familyServers.forEach(s -> serversComponent.set(serversComponent.get().appendNewline().append(this.server(s))));

        Component parentFamilyComponent = parentFamily == null ? text("none") : text(parentFamily);

        AtomicReference<Component> parametersComponent = new AtomicReference<>(empty());
        parameters.forEach((k, v)->parametersComponent.set(parametersComponent.get().appendNewline().append(text("   ---| "+k+": "+v))));

        return headerBox(familyID,
            join(
                newlines(),
                text("   ---| Parent Family: ").append(parentFamilyComponent),
                parametersComponent.get(),
                space(),
                border(),
                space(),
                status == null ? empty() : status.appendNewline().append(space()),
                serversComponent.get()
            )
        );
    };
}
