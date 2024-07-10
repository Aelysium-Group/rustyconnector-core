package group.aelysium.rustyconnector.proxy.lang;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.lang.ASCIIAlphabet;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.common.config.Config;
import org.jetbrains.annotations.NotNull;

public class ProxyLangLibrary implements Particle {
    private final ProxyLang lang;
    private final ASCIIAlphabet asciiAlphabet;
    private final Config<?> git;
    private final Config<?> config;
    private final Config<?> family;
    private final Config<Particle.Tinder<LoadBalancer>> loadBalancer;
    private final Config<Whitelist.Tinder> whitelist;
    private final Config<?> magicConfig;
    private final Config<?> magicLink;

    protected ProxyLangLibrary(
            @NotNull ProxyLang lang,
            @NotNull ASCIIAlphabet asciiAlphabet,
            @NotNull Config<?> git,
            @NotNull Config<?> config,
            @NotNull Config<?> family,
            @NotNull Config<LoadBalancer.Settings> loadBalancer,
            @NotNull Config<Whitelist.Settings> whitelist,
            @NotNull Config<?> magicConfig,
            @NotNull Config<?> magicLink
    ) {

        this.lang = lang;
        this.asciiAlphabet = asciiAlphabet;
        this.git = git;
        this.config = config;
        this.family = family;
        this.loadBalancer = loadBalancer;
        this.whitelist = whitelist;
        this.magicConfig = magicConfig;
        this.magicLink = magicLink;
    }

    public ProxyLang lang() {
        return this.lang;
    }

    public ASCIIAlphabet asciiAlphabet() {
        return this.asciiAlphabet;
    }

    @Override
    public void close() throws Exception {

    }

    public static class Tinder extends Particle.Tinder<ProxyLangLibrary> {
        private final Settings settings;

        public Tinder(@NotNull Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull ProxyLangLibrary ignite() throws Exception {
            return new ProxyLangLibrary(
                    settings.lang(),
                    settings.asciiAlphabet(),
                    settings.git(),
                    settings.config(),
                    settings.family(),
                    settings.loadBalancer(),
                    settings.whitelist(),
                    settings.magicConfig(),
                    settings.magicLink()
            );
        }
    }

    public record Settings(
    ) {}
}
