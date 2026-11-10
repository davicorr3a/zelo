package io.github.davicorr3a.zelo.conversa;

/** Um botão de resposta dentro de uma mensagem do Zelo. */
public final class Acao {

    private final String rotulo;
    private final Runnable aoTocar;
    private final boolean principal;

    private Acao(String rotulo, Runnable aoTocar, boolean principal) {
        this.rotulo = rotulo;
        this.aoTocar = aoTocar;
        this.principal = principal;
    }

    /** O caminho recomendado. Aparece com destaque. */
    public static Acao principal(String rotulo, Runnable aoTocar) {
        return new Acao(rotulo, aoTocar, true);
    }

    public static Acao secundaria(String rotulo, Runnable aoTocar) {
        return new Acao(rotulo, aoTocar, false);
    }

    public String getRotulo() {
        return rotulo;
    }

    public Runnable getAoTocar() {
        return aoTocar;
    }

    public boolean isPrincipal() {
        return principal;
    }
}
