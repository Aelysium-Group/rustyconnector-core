package group.aelysium.rustyconnector.proxy;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.player.Player;

import java.util.Locale;

public class Permission {
    /**
     * Checks of the player has the defined permissions.
     * Will return `true` even if only one of these permissions is valid.
     * @param player The player to validate
     * @param nodes The permissions to check for
     * @return `true` If the player has any one of the defined permissions. `false` If the player has none of them.
     */
    public static boolean validate(Player player, String... nodes) {
        if(player == null) return false;

        ProxyAdapter adapter = RC.P.Adapter();

        if(adapter.checkPermission(player, "*")) return true;
        if(adapter.checkPermission(player, "rustyconnector.*")) return true;

        for (String node : nodes) {
            String nodeToLower = node.toLowerCase(Locale.ROOT);

            if(adapter.checkPermission(player, nodeToLower)) return true;

            /*
             * Check for wildcard variants of permissions like: rustyconnector.* or rustyconnector.admin.*
             */
            String adjustedNode = nodeToLower.replaceFirst("[A-z_\\-]*$","*");
            if(adapter.checkPermission(player, adjustedNode)) return true;
        }
        return false;
    }

    /**
     * Construct a permission node using insertion nodes. Define your string to be inserted into.
     * By surrounding a string with `{@literal <}{@literal >}` brackets you can mark it to be replaced.
     * `rustyconnector.{@literal <}server id{@literal >}.access`
     * <br><br>
     * Insertion points are replaced with the defined values in the order that they are defined.
     * @param pattern The pattern to change
     * @param insertions The insertions to add
     */
    public static String constructNode(String pattern, String... insertions) {
        for (String node : insertions)
            pattern = pattern.replaceFirst("<[A-z\\s]*>",node);
        return pattern;
    }
}
