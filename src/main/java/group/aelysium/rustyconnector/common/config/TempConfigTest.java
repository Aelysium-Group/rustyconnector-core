package group.aelysium.rustyconnector.common.config;

@Config("families/{family_name}.yml") // Want to add support for dynamic filenames
@Comment({"",""})
public class TempConfigTest {
        @PathParameter("family_name")
        private String familyName;

        @Comment({
                "############################################################",
                "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
                "#                       Display Name                       #",
                "#                                                          #",
                "#               ---------------------------                #",
                "# | Display name is the name of your family, as players    #",
                "# | will see it, in-game.                                  #",
                "#                                                          #",
                "#               ---------------------------                #",
                "#                                                          #",
                "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
                "############################################################"
        })
        @Node(order = 0, key = "display-name", defaultValue = "none")
        private String displayName;
}