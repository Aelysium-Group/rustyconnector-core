package group.aelysium.rustyconnector.common.lang;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
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
    public void close() throws Exception {

    }

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
    }

    public static ASCIIAlphabet DEFAULT_ASCII_ALPHABET = new EnglishAlphabet();
}
