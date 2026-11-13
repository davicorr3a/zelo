package io.github.davicorr3a.zelo.conversa;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.analise.AnalisadorApps;
import io.github.davicorr3a.zelo.analise.AppSuspeito;
import io.github.davicorr3a.zelo.guardiao.AlertasGuardiao;
import io.github.davicorr3a.zelo.util.Preferencias;

/** A conversa que começa quando a pessoa toca num aviso do guardião. */
final class AvisoDoGuardiao {

    private final Palco palco;
    private final AnalisadorApps analisador;
    private final Preferencias preferencias;
    private final RemocaoGuiada remocao;

    AvisoDoGuardiao(Palco palco, AnalisadorApps analisador, Preferencias preferencias, RemocaoGuiada remocao) {
        this.palco = palco;
        this.analisador = analisador;
        this.preferencias = preferencias;
        this.remocao = remocao;
    }

    void mostrar(String pacote, String motivo) {
        palco.emSegundoPlano(() -> analisador.analisarPacote(pacote), app -> perguntar(app, pacote, motivo));
    }

    private void perguntar(AppSuspeito app, String pacote, String motivo) {
        if (app == null) {
            palco.dizer(Mensagem.de(palco.t(R.string.app_ja_saiu)));
            return;
        }

        String texto;
        if (AlertasGuardiao.MOTIVO_ACESSIBILIDADE.equals(motivo)) {
            texto = palco.t(R.string.alerta_acessibilidade, app.getNome());
        } else if (app.isAcessoRemoto()) {
            texto = palco.t(R.string.alerta_instalacao_remoto, app.getNome());
        } else {
            texto = palco.t(R.string.alerta_instalacao, app.getNome());
        }

        palco.dizer(Mensagem.alerta(texto)
                .com(Acao.secundaria(palco.t(R.string.fui_eu), () -> {
                    preferencias.confiar(pacote);
                    palco.dizer(Mensagem.de(palco.t(R.string.tudo_certo)));
                }))
                .com(Acao.principal(palco.t(R.string.alguem_pediu), () -> {
                    preferencias.desconfiar(pacote);
                    palco.dizer(Mensagem.alerta(palco.t(R.string.golpe_orientacao)));
                    remocao.tirar(app, palco::encerrar);
                })));
    }
}
