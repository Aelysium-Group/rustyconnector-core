package group.aelysium.rustyconnector.common.lang;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;

import java.text.ParseException;

public abstract class LangNode {
    protected final String name;

    public LangNode(String name) {
        this.name = name;
    }

    /**
     * Generates the associated Component from the lang node.
     * @param arguments The arguments, if any, to use for the node.
     * @return A newly constructed Component.
     */
    public abstract Component generate(Object ...arguments) throws RuntimeException;

    /**
     * Sends the LangNode to the console.
     * This method simply runs {@link #generate(Object...)} and then logs it using the RC Adapter.
     * @param arguments The arguments, if any, to use for the node.
     */
    public final void send(Object ...arguments) throws RuntimeException {
        try {
            RC.S.Adapter().log(this.generate(arguments));
        } catch (Exception ignore) {}
        try {
            RC.P.Adapter().log(this.generate(arguments));
        } catch (Exception ignore) {}
        throw new RuntimeException("No adapter exists to handle this lang node.");
    }
}
