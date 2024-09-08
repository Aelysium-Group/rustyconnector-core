package group.aelysium.rustyconnector.server.lang;

import group.aelysium.rustyconnector.common.lang.ASCIIAlphabet;
import group.aelysium.rustyconnector.common.lang.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class ServerLang extends Lang {

    public ServerLang(ASCIIAlphabet asciiAlphabet) {
        super(asciiAlphabet);
    }

    public Component magicLink() {
        return join(
                newlines(),
                space(),
                text("              /(¯`·._.·´¯`·._.·´¯`·._.·´¯`·._.·´¯)\\"),
                text("          --<||(     MAGIC LINK -- CONNECTED     )||>--"),
                text("              \\(_.·´¯`·._.·´¯`·._.·´¯`·._.·´¯`·._)/")
        ).color(DARK_PURPLE);
    }

    public Component paperFolia(boolean isFolia) {
        if(isFolia) return text("RustyConnector-Folia");
        return text("RustyConnector-Paper");
    };

    public Component paperFoliaLowercase(boolean isFolia) {
        if(isFolia) return text("rustyconnector-folia");
        return text("rustyconnector-paper");
    };

    public Component magicLinkHandshakeFailure(String reason, int delayAmount, TimeUnit delayUnit) {
        return join(
            newlines(),
            text(reason, NamedTextColor.RED),
            text("Waiting "+delayAmount+" "+delayUnit+" before trying again...", NamedTextColor.GRAY)
        );
    }
}
