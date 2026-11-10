package io.github.davicorr3a.zelo.conversa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Uma fala do Zelo, com os botões de resposta que a pessoa pode tocar. */
public final class Mensagem {

    private final String texto;
    private final boolean alerta;
    private final List<Acao> acoes = new ArrayList<>();

    private Mensagem(String texto, boolean alerta) {
        this.texto = texto;
        this.alerta = alerta;
    }

    public static Mensagem de(String texto) {
        return new Mensagem(texto, false);
    }

    /** Mesma coisa, mas com destaque visual de atenção. */
    public static Mensagem alerta(String texto) {
        return new Mensagem(texto, true);
    }

    public Mensagem com(Acao acao) {
        acoes.add(acao);
        return this;
    }

    public String getTexto() {
        return texto;
    }

    public boolean isAlerta() {
        return alerta;
    }

    public List<Acao> getAcoes() {
        return Collections.unmodifiableList(acoes);
    }
}
