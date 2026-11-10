package io.github.davicorr3a.zelo.conversa;

import android.content.Intent;

/** O que o roteiro precisa da tela: mostrar uma mensagem e abrir outra tela do sistema. */
public interface Conversa {

    void dizer(Mensagem mensagem);

    void abrir(Intent intent);
}
