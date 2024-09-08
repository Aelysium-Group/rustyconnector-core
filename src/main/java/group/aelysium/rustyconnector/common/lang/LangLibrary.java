package group.aelysium.rustyconnector.common.lang;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.server.lang.ServerLang;
import group.aelysium.rustyconnector.proxy.lang.ProxyLang;
import org.jetbrains.annotations.NotNull;

public class LangLibrary<L extends Lang> implements Particle {
    private final L lang;
    private final ASCIIAlphabet asciiAlphabet;

    protected LangLibrary(
            @NotNull L lang,
            @NotNull ASCIIAlphabet asciiAlphabet
    ) {
        this.lang = lang;
        this.asciiAlphabet = asciiAlphabet;
    }

    public L lang() {
        return this.lang;
    }

    public ASCIIAlphabet asciiAlphabet() {
        return this.asciiAlphabet;
    }

    @Override
    public void close() {}

    public static class Tinder<L extends Lang> extends Particle.Tinder<LangLibrary<L>> {
        private final L lang;
        private final ASCIIAlphabet asciiAlphabet;

        public Tinder(
                @NotNull L lang,
                @NotNull ASCIIAlphabet asciiAlphabet
        ) {
            this.lang = lang;
            this.asciiAlphabet = asciiAlphabet;
        }

        @Override
        public @NotNull LangLibrary<L> ignite() throws Exception {
            return new LangLibrary<>(
                    this.lang,
                    this.asciiAlphabet
            );
        }

        public static LangLibrary.Tinder<ProxyLang> DEFAULT_PROXY_CONFIGURATION = new Tinder<>(
                new ProxyLang(DEFAULT_ASCII_ALPHABET),
                DEFAULT_ASCII_ALPHABET
        );
        public static LangLibrary.Tinder<ServerLang> DEFAULT_SERVER_CONFIGURATION = new Tinder<>(
                new ServerLang(DEFAULT_ASCII_ALPHABET),
                DEFAULT_ASCII_ALPHABET
        );
    }

    public static ASCIIAlphabet DEFAULT_ASCII_ALPHABET = new EnglishAlphabet();
}
