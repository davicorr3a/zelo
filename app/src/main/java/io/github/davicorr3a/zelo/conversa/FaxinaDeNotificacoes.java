package io.github.davicorr3a.zelo.conversa;

import android.content.Intent;
import android.os.Build;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.analise.AnalisadorApps;
import io.github.davicorr3a.zelo.guardiao.GuardiaoService;
import io.github.davicorr3a.zelo.guardiao.HistoricoNotificacoes;
import io.github.davicorr3a.zelo.organizacao.PlanoDeFaxina;
import io.github.davicorr3a.zelo.util.Atalhos;
import io.github.davicorr3a.zelo.util.Preferencias;

/**
 * Pede o acesso às notificações, se ainda não tiver, limpa a bandeja e depois passa pelos sites
 * e pelos apps que mais incomodam. Quem decide o que oferecer é o {@link PlanoDeFaxina}.
 */
final class FaxinaDeNotificacoes {

    private static final int MINIMO_PARA_FALAR_DE_NOTIFICACOES = 5;

    private final Palco palco;
    private final Preferencias preferencias;
    private final HistoricoNotificacoes historico;
    private final Deque<PlanoDeFaxina.Site> sites = new ArrayDeque<>();
    private final Deque<PlanoDeFaxina.AppBarulhento> barulhentos = new ArrayDeque<>();

    FaxinaDeNotificacoes(Palco palco, Preferencias preferencias, HistoricoNotificacoes historico) {
        this.palco = palco;
        this.preferencias = preferencias;
        this.historico = historico;
    }

    void comecar() {
        verNotificacoes(false);
    }

    private void verNotificacoes(boolean jaPediuAcesso) {
        GuardiaoService guardiao = GuardiaoService.ativo();

        if (guardiao == null) {
            if (jaPediuAcesso) {
                explicarAcessoNegado();
            } else {
                palco.dizer(Mensagem.de(palco.t(R.string.pedir_acesso_notificacoes))
                        .com(Acao.principal(palco.t(R.string.liberar),
                                () -> palco.abrirEDepois(Atalhos.acessoANotificacoes(palco.contexto()), () -> verNotificacoes(true))))
                        .com(Acao.secundaria(palco.t(R.string.agora_nao), palco::encerrar)));
            }
            return;
        }

        Map<String, Map<String, Integer>> bandeja = guardiao.contarNaBandeja();
        PlanoDeFaxina plano = PlanoDeFaxina.montar(historico.porAppECanal(), bandeja, preferencias.canaisResolvidos());
        sites.clear();
        sites.addAll(plano.getSites());
        barulhentos.clear();
        barulhentos.addAll(plano.getApps());

        int total = somar(bandeja);
        if (total < MINIMO_PARA_FALAR_DE_NOTIFICACOES) {
            if (sites.isEmpty() && barulhentos.isEmpty()) {
                palco.dizer(Mensagem.de(palco.t(R.string.notificacoes_ok)));
                palco.encerrar();
            } else {
                proximoSite();
            }
            return;
        }

        palco.dizer(Mensagem.de(palco.p(R.plurals.notificacoes_acumuladas, total, total))
                .com(Acao.principal(palco.t(R.string.limpar_todas), () -> {
                    GuardiaoService atual = GuardiaoService.ativo();
                    if (atual != null) atual.limparTudo();
                    palco.dizer(Mensagem.de(palco.t(R.string.limpei)));
                    proximoSite();
                }))
                .com(Acao.secundaria(palco.t(R.string.deixar), this::proximoSite)));
    }

    // Sites primeiro: são eles que mandam "vírus detectado" e "você ganhou um prêmio".
    private void proximoSite() {
        PlanoDeFaxina.Site site = sites.poll();
        if (site == null) {
            proximoBarulhento();
            return;
        }

        String chave = PlanoDeFaxina.chave(site.navegador, site.canal);
        String navegador = AnalisadorApps.nomeDoApp(palco.contexto(), site.navegador);
        palco.dizer(Mensagem.de(palco.p(R.plurals.site_notificando, site.quantidade, site.dominio, site.quantidade, navegador))
                .com(Acao.principal(palco.t(R.string.bloquear), () -> {
                    preferencias.resolverCanal(chave);
                    palco.abrirEDepois(Atalhos.canalDoApp(site.navegador, site.canal), this::proximoSite);
                }))
                .com(Acao.secundaria(palco.t(R.string.esse_eu_conheco), () -> {
                    preferencias.resolverCanal(chave);
                    proximoSite();
                })));
    }

    private void proximoBarulhento() {
        PlanoDeFaxina.AppBarulhento app = barulhentos.poll();
        if (app == null) {
            palco.encerrar();
            return;
        }

        int pergunta;
        if (!app.porCanal) {
            pergunta = R.string.barulhento_app_inteiro;
        } else if (app.temOutrosCanais) {
            pergunta = R.string.barulhento_canal_outros;
        } else {
            pergunta = R.string.barulhento_canal;
        }
        String nome = AnalisadorApps.nomeDoApp(palco.contexto(), app.pacote);
        String texto = palco.p(R.plurals.app_mandou, app.quantidade, nome, app.quantidade) + ' ' + palco.t(pergunta);
        Intent tela = app.porCanal ? Atalhos.canalDoApp(app.pacote, app.canal) : Atalhos.notificacoesDoApp(app.pacote);
        String chave = PlanoDeFaxina.chave(app.pacote, app.canal);

        palco.dizer(Mensagem.de(texto)
                .com(Acao.principal(palco.t(R.string.desligar), () -> {
                    preferencias.resolverCanal(chave);
                    palco.abrirEDepois(tela, this::proximoBarulhento);
                }))
                .com(Acao.secundaria(palco.t(R.string.nao_precisa), () -> {
                    preferencias.resolverCanal(chave);
                    proximoBarulhento();
                })));
    }

    private void explicarAcessoNegado() {
        // A partir do Android 13, apps instalados fora de uma loja têm esse acesso bloqueado
        // até a pessoa liberar as "configurações restritas" nas informações do app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            palco.dizer(Mensagem.de(palco.t(R.string.acesso_negado_restrito))
                    .com(Acao.principal(palco.t(R.string.abrir_info_zelo),
                            () -> palco.abrirEDepois(Atalhos.detalhesDoApp(palco.contexto().getPackageName()), () -> verNotificacoes(false))))
                    .com(Acao.secundaria(palco.t(R.string.agora_nao), palco::encerrar)));
        } else {
            palco.dizer(Mensagem.de(palco.t(R.string.acesso_negado)));
            palco.encerrar();
        }
    }

    private static int somar(Map<String, Map<String, Integer>> contagem) {
        int total = 0;
        for (Map<String, Integer> canais : contagem.values()) {
            for (int quantidade : canais.values()) total += quantidade;
        }
        return total;
    }
}
