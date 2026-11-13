package io.github.davicorr3a.zelo.conversa;

import android.content.Intent;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.analise.AnalisadorApps;
import io.github.davicorr3a.zelo.analise.AppSuspeito;
import io.github.davicorr3a.zelo.analise.AppSuspeito.Acesso;
import io.github.davicorr3a.zelo.util.Atalhos;

/**
 * Tira um app do celular na ordem que funciona: primeiro a acessibilidade e o administrador,
 * que impedem ou atrapalham a remoção, e só depois a desinstalação.
 */
final class RemocaoGuiada {

    private final Palco palco;
    private final AnalisadorApps analisador;

    RemocaoGuiada(Palco palco, AnalisadorApps analisador) {
        this.palco = palco;
        this.analisador = analisador;
    }

    /** {@code depois} roda no fim, tenha a pessoa removido o app ou pulado algum passo. */
    void tirar(AppSuspeito app, Runnable depois) {
        if (app.tem(Acesso.ACESSIBILIDADE)) {
            pedirPassoNasConfiguracoes(palco.t(R.string.desligar_acessibilidade, app.getNome()),
                    Atalhos.acessibilidade(), () -> tirarAdministrador(app, depois), depois);
        } else {
            tirarAdministrador(app, depois);
        }
    }

    private void tirarAdministrador(AppSuspeito app, Runnable depois) {
        if (app.tem(Acesso.ADMINISTRADOR)) {
            pedirPassoNasConfiguracoes(palco.t(R.string.desligar_admin, app.getNome()),
                    Atalhos.seguranca(), () -> pedirDesinstalacao(app, depois), depois);
        } else {
            pedirDesinstalacao(app, depois);
        }
    }

    private void pedirPassoNasConfiguracoes(String texto, Intent tela, Runnable seguir, Runnable pular) {
        palco.dizer(Mensagem.de(texto)
                .com(Acao.principal(palco.t(R.string.abrir_tela), () -> palco.abrirEDepois(tela, seguir)))
                .com(Acao.secundaria(palco.t(R.string.pular), pular)));
    }

    private void pedirDesinstalacao(AppSuspeito app, Runnable depois) {
        palco.dizer(Mensagem.de(palco.t(R.string.desinstalar_pedido))
                .com(Acao.principal(palco.t(R.string.desinstalar),
                        () -> palco.abrirEDepois(Atalhos.desinstalar(app.getPacote()), () -> conferirRemocao(app, depois))))
                .com(Acao.secundaria(palco.t(R.string.pular), depois)));
    }

    private void conferirRemocao(AppSuspeito app, Runnable depois) {
        if (analisador.estaInstalado(app.getPacote())) {
            palco.dizer(Mensagem.alerta(palco.t(R.string.ainda_instalado, app.getNome())));
        } else {
            palco.dizer(Mensagem.de(palco.t(R.string.removido, app.getNome())));
        }
        depois.run();
    }
}
