package io.github.davicorr3a.zelo.ui;

/**
 * Embrulha algo que deve acontecer uma vez só, como abrir uma tela do sistema.
 * Sem isso, o LiveData entregaria o mesmo pedido de novo ao girar a tela.
 */
public final class Evento<T> {

    private final T conteudo;
    private boolean consumido;

    public Evento(T conteudo) {
        this.conteudo = conteudo;
    }

    /** Devolve o conteúdo na primeira chamada e null nas seguintes. */
    public T consumir() {
        if (consumido) return null;
        consumido = true;
        return conteudo;
    }
}
