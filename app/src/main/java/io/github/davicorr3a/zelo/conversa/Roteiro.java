package io.github.davicorr3a.zelo.conversa;

import android.content.Context;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.analise.AnalisadorApps;
import io.github.davicorr3a.zelo.guardiao.HistoricoNotificacoes;
import io.github.davicorr3a.zelo.util.Preferencias;

/**
 * O roteiro da conversa: decide por onde ela começa e em que ordem as cenas acontecem.
 *
 * Cada cena ({@link RevisaoDeApps}, {@link RemocaoGuiada}, {@link FaxinaDeNotificacoes} e
 * {@link AvisoDoGuardiao}) é uma sequência de momentos: o Zelo diz alguma coisa, oferece botões,
 * e cada botão leva ao próximo momento. O que elas compartilham fica no {@link Palco}.
 */
public final class Roteiro {

    private final Palco palco;
    private final RevisaoDeApps revisao;
    private final AvisoDoGuardiao aviso;

    public Roteiro(Context contexto, Conversa conversa) {
        palco = new Palco(contexto, conversa);
        Context app = palco.contexto();
        Preferencias preferencias = new Preferencias(app);
        AnalisadorApps analisador = new AnalisadorApps(app, preferencias);

        RemocaoGuiada remocao = new RemocaoGuiada(palco, analisador);
        FaxinaDeNotificacoes faxina = new FaxinaDeNotificacoes(palco, preferencias, new HistoricoNotificacoes(app));
        revisao = new RevisaoDeApps(palco, analisador, preferencias, remocao, faxina::comecar);
        aviso = new AvisoDoGuardiao(palco, analisador, preferencias, remocao);
    }

    /** A conversa normal: revisão dos apps e, em seguida, a faxina de notificações. */
    public void comecar() {
        palco.reiniciar();
        palco.dizer(Mensagem.de(palco.t(R.string.oi))
                .com(Acao.principal(palco.t(R.string.pode_olhar), revisao::comecar))
                .com(Acao.secundaria(palco.t(R.string.agora_nao),
                        () -> palco.dizer(Mensagem.de(palco.t(R.string.tudo_bem_depois))))));
    }

    /** Começa a conversa a partir de um aviso do guardião. */
    public void alertaDoGuardiao(String pacote, String motivo) {
        palco.reiniciar();
        aviso.mostrar(pacote, motivo);
    }

    /** A tela chama isso sempre que volta a aparecer, por exemplo depois de abrir uma configuração. */
    public void retomar() {
        palco.retomar();
    }

    public void liberar() {
        palco.liberar();
    }
}
