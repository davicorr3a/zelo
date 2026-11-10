package io.github.davicorr3a.zelo.ui;

import io.github.davicorr3a.zelo.conversa.Mensagem;

/**
 * Um item da lista da conversa: ou uma fala do Zelo, ou a resposta que a pessoa tocou.
 * É imutável para funcionar bem com o DiffUtil. Marcar como respondida gera um item novo.
 */
public final class ItemConversa {

    public enum Tipo { ZELO, PESSOA }

    private final long id;
    private final Tipo tipo;
    private final Mensagem mensagem;
    private final String texto;
    private final boolean respondida;

    private ItemConversa(long id, Tipo tipo, Mensagem mensagem, String texto, boolean respondida) {
        this.id = id;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.texto = texto;
        this.respondida = respondida;
    }

    static ItemConversa doZelo(long id, Mensagem mensagem) {
        return new ItemConversa(id, Tipo.ZELO, mensagem, mensagem.getTexto(), false);
    }

    static ItemConversa daPessoa(long id, String texto) {
        return new ItemConversa(id, Tipo.PESSOA, null, texto, true);
    }

    ItemConversa comoRespondida() {
        return new ItemConversa(id, tipo, mensagem, texto, true);
    }

    public long getId() {
        return id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Só existe em falas do Zelo. */
    public Mensagem getMensagem() {
        return mensagem;
    }

    public String getTexto() {
        return texto;
    }

    public boolean isRespondida() {
        return respondida;
    }

    /** Depois que a pessoa responde, os botões somem e a resposta aparece como um balão dela. */
    public boolean mostraBotoes() {
        return tipo == Tipo.ZELO && !respondida && !mensagem.getAcoes().isEmpty();
    }
}
