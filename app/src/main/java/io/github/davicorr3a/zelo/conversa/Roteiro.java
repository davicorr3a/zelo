package io.github.davicorr3a.zelo.conversa;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.analise.AnalisadorApps;
import io.github.davicorr3a.zelo.analise.AppSuspeito;
import io.github.davicorr3a.zelo.analise.AppSuspeito.Acesso;
import io.github.davicorr3a.zelo.guardiao.AlertasGuardiao;
import io.github.davicorr3a.zelo.guardiao.GuardiaoService;
import io.github.davicorr3a.zelo.util.Atalhos;
import io.github.davicorr3a.zelo.util.Preferencias;

/**
 * O roteiro da conversa.
 *
 * Cada método privado é um momento da conversa: o Zelo diz alguma coisa, oferece botões,
 * e cada botão leva ao próximo momento. Quando um passo precisa de uma tela do sistema,
 * o próximo momento fica guardado em {@code aoRetomar} e roda quando a pessoa voltar.
 */
public final class Roteiro {

    private static final int MINIMO_PARA_FALAR_DE_NOTIFICACOES = 5;
    private static final long ESPERA_AO_VOLTAR_MS = 800;

    private final Context contexto;
    private final Conversa conversa;
    private final Preferencias preferencias;
    private final AnalisadorApps analisador;
    private final ExecutorService segundoPlano = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final Deque<AppSuspeito> fila = new ArrayDeque<>();

    private Runnable aoRetomar;
    // Muda a cada recomeço. Resultados de uma análise antiga são descartados.
    private int sessao;

    public Roteiro(Context contexto, Conversa conversa) {
        this.contexto = contexto.getApplicationContext();
        this.conversa = conversa;
        this.preferencias = new Preferencias(this.contexto);
        this.analisador = new AnalisadorApps(this.contexto, preferencias);
    }

    // ---- Pontos de entrada ----

    public void comecar() {
        reiniciar();
        conversa.dizer(Mensagem.de(t(R.string.oi))
                .com(Acao.principal(t(R.string.pode_olhar), this::analisarApps))
                .com(Acao.secundaria(t(R.string.agora_nao),
                        () -> conversa.dizer(Mensagem.de(t(R.string.tudo_bem_depois))))));
    }

    /** Começa a conversa a partir de um aviso do guardião. */
    public void alertaDoGuardiao(String pacote, String motivo) {
        reiniciar();
        int minhaSessao = sessao;
        segundoPlano.execute(() -> {
            AppSuspeito app = analisador.analisarPacote(pacote);
            principal.post(() -> {
                if (minhaSessao == sessao) mostrarAlerta(app, pacote, motivo);
            });
        });
    }

    /** A tela chama isso sempre que volta a aparecer, por exemplo depois de abrir uma configuração. */
    public void retomar() {
        Runnable proximo = aoRetomar;
        aoRetomar = null;
        if (proximo != null) principal.postDelayed(proximo, ESPERA_AO_VOLTAR_MS);
    }

    public void liberar() {
        reiniciar();
        segundoPlano.shutdownNow();
    }

    // ---- Revisão dos apps ----

    private void analisarApps() {
        conversa.dizer(Mensagem.de(t(R.string.um_instante)));
        int minhaSessao = sessao;
        segundoPlano.execute(() -> {
            List<AppSuspeito> achados = analisador.analisar();
            principal.post(() -> {
                if (minhaSessao == sessao) apresentarAchados(achados);
            });
        });
    }

    private void apresentarAchados(List<AppSuspeito> achados) {
        fila.clear();
        fila.addAll(achados);

        if (achados.isEmpty()) {
            conversa.dizer(Mensagem.de(t(R.string.nenhum_suspeito)));
            verNotificacoes(false);
            return;
        }

        String texto = contexto.getResources().getQuantityString(R.plurals.achados, achados.size(), achados.size());
        conversa.dizer(Mensagem.de(texto).com(Acao.principal(t(R.string.vamos), this::proximoApp)));
    }

    private void proximoApp() {
        AppSuspeito app = fila.poll();
        if (app == null) {
            conversa.dizer(Mensagem.de(t(R.string.fim_apps)));
            verNotificacoes(false);
            return;
        }

        conversa.dizer(Mensagem.alerta(descrever(app) + "\n\n" + t(R.string.reconhece))
                .com(Acao.secundaria(t(R.string.reconheco), () -> {
                    preferencias.confiar(app.getPacote());
                    proximoApp();
                }))
                .com(Acao.principal(t(R.string.nao_conheco), () -> tirarApp(app, this::proximoApp))));
    }

    // ---- Remoção guiada: desligar acessos primeiro, desinstalar depois ----

    private void tirarApp(AppSuspeito app, Runnable depois) {
        if (app.tem(Acesso.ACESSIBILIDADE)) {
            pedirPassoNasConfiguracoes(t(R.string.desligar_acessibilidade, app.getNome()),
                    Atalhos.acessibilidade(), () -> tirarAdministrador(app, depois), depois);
        } else {
            tirarAdministrador(app, depois);
        }
    }

    private void tirarAdministrador(AppSuspeito app, Runnable depois) {
        if (app.tem(Acesso.ADMINISTRADOR)) {
            pedirPassoNasConfiguracoes(t(R.string.desligar_admin, app.getNome()),
                    Atalhos.seguranca(), () -> pedirDesinstalacao(app, depois), depois);
        } else {
            pedirDesinstalacao(app, depois);
        }
    }

    private void pedirPassoNasConfiguracoes(String texto, Intent tela, Runnable seguir, Runnable pular) {
        conversa.dizer(Mensagem.de(texto)
                .com(Acao.principal(t(R.string.abrir_tela), () -> abrirEDepois(tela, seguir)))
                .com(Acao.secundaria(t(R.string.pular), pular)));
    }

    private void pedirDesinstalacao(AppSuspeito app, Runnable depois) {
        conversa.dizer(Mensagem.de(t(R.string.desinstalar_pedido))
                .com(Acao.principal(t(R.string.desinstalar),
                        () -> abrirEDepois(Atalhos.desinstalar(app.getPacote()), () -> conferirRemocao(app, depois))))
                .com(Acao.secundaria(t(R.string.pular), depois)));
    }

    private void conferirRemocao(AppSuspeito app, Runnable depois) {
        if (analisador.estaInstalado(app.getPacote())) {
            conversa.dizer(Mensagem.alerta(t(R.string.ainda_instalado, app.getNome())));
        } else {
            conversa.dizer(Mensagem.de(t(R.string.removido, app.getNome())));
        }
        depois.run();
    }

    // ---- Notificações ----

    private void verNotificacoes(boolean jaPediuAcesso) {
        GuardiaoService guardiao = GuardiaoService.ativo();

        if (guardiao == null) {
            if (jaPediuAcesso) {
                explicarAcessoNegado();
            } else {
                conversa.dizer(Mensagem.de(t(R.string.pedir_acesso_notificacoes))
                        .com(Acao.principal(t(R.string.liberar),
                                () -> abrirEDepois(Atalhos.acessoANotificacoes(contexto), () -> verNotificacoes(true))))
                        .com(Acao.secundaria(t(R.string.agora_nao), this::encerrar)));
            }
            return;
        }

        Map<String, Integer> contagem = guardiao.contarPorApp();
        int total = 0;
        String campeao = null;
        int doCampeao = 0;
        for (Map.Entry<String, Integer> item : contagem.entrySet()) {
            total += item.getValue();
            if (item.getValue() > doCampeao) {
                campeao = item.getKey();
                doCampeao = item.getValue();
            }
        }

        if (total < MINIMO_PARA_FALAR_DE_NOTIFICACOES || campeao == null) {
            conversa.dizer(Mensagem.de(t(R.string.notificacoes_ok)));
            encerrar();
            return;
        }

        String pacoteCampeao = campeao;
        String nomeCampeao = AnalisadorApps.nomeDoApp(contexto, campeao);
        conversa.dizer(Mensagem.de(t(R.string.notificacoes_acumuladas, total, nomeCampeao, doCampeao))
                .com(Acao.principal(t(R.string.limpar_todas), () -> {
                    GuardiaoService atual = GuardiaoService.ativo();
                    if (atual != null) atual.limparTudo();
                    conversa.dizer(Mensagem.de(t(R.string.limpei)));
                    oferecerSilenciar(pacoteCampeao, nomeCampeao);
                }))
                .com(Acao.secundaria(t(R.string.deixar), this::encerrar)));
    }

    private void explicarAcessoNegado() {
        // A partir do Android 13, apps instalados fora de uma loja têm esse acesso bloqueado
        // até a pessoa liberar as "configurações restritas" nas informações do app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            conversa.dizer(Mensagem.de(t(R.string.acesso_negado_restrito))
                    .com(Acao.principal(t(R.string.abrir_info_zelo),
                            () -> abrirEDepois(Atalhos.detalhesDoApp(contexto.getPackageName()), () -> verNotificacoes(false))))
                    .com(Acao.secundaria(t(R.string.agora_nao), this::encerrar)));
        } else {
            conversa.dizer(Mensagem.de(t(R.string.acesso_negado)));
            encerrar();
        }
    }

    private void oferecerSilenciar(String pacote, String nome) {
        conversa.dizer(Mensagem.de(t(R.string.silenciar_pergunta, nome))
                .com(Acao.principal(t(R.string.silenciar),
                        () -> abrirEDepois(Atalhos.notificacoesDoApp(pacote), this::encerrar)))
                .com(Acao.secundaria(t(R.string.nao_precisa), this::encerrar)));
    }

    // ---- Avisos do guardião ----

    private void mostrarAlerta(AppSuspeito app, String pacote, String motivo) {
        if (app == null) {
            conversa.dizer(Mensagem.de(t(R.string.app_ja_saiu)));
            return;
        }

        String texto;
        if (AlertasGuardiao.MOTIVO_ACESSIBILIDADE.equals(motivo)) {
            texto = t(R.string.alerta_acessibilidade, app.getNome());
        } else if (app.isAcessoRemoto()) {
            texto = t(R.string.alerta_instalacao_remoto, app.getNome());
        } else {
            texto = t(R.string.alerta_instalacao, app.getNome());
        }

        conversa.dizer(Mensagem.alerta(texto)
                .com(Acao.secundaria(t(R.string.fui_eu), () -> {
                    preferencias.confiar(pacote);
                    conversa.dizer(Mensagem.de(t(R.string.tudo_certo)));
                }))
                .com(Acao.principal(t(R.string.alguem_pediu), () -> {
                    preferencias.desconfiar(pacote);
                    conversa.dizer(Mensagem.alerta(t(R.string.golpe_orientacao)));
                    tirarApp(app, this::encerrar);
                })));
    }

    // ---- Auxiliares ----

    private void encerrar() {
        boolean deGuarda = GuardiaoService.ativo() != null;
        conversa.dizer(Mensagem.de(t(deGuarda ? R.string.fim_com_guardiao : R.string.fim)));
    }

    private void abrirEDepois(Intent tela, Runnable depois) {
        aoRetomar = depois;
        conversa.abrir(tela);
    }

    private void reiniciar() {
        sessao++;
        aoRetomar = null;
        fila.clear();
        principal.removeCallbacksAndMessages(null);
    }

    private String descrever(AppSuspeito app) {
        StringBuilder texto = new StringBuilder();

        if (!app.getAcessos().isEmpty()) {
            List<String> partes = new ArrayList<>();
            for (Acesso acesso : app.getAcessos()) partes.add(t(textoDo(acesso)));
            texto.append(t(R.string.app_pode, app.getNome(), juntar(partes)));
            if (app.isForaDaLoja()) texto.append(' ').append(t(R.string.fora_da_loja));
        } else if (app.isForaDaLoja()) {
            texto.append(t(R.string.so_fora_da_loja, app.getNome()));
        }

        if (app.isAcessoRemoto()) {
            if (texto.length() == 0) {
                texto.append(t(R.string.acesso_remoto_com_nome, app.getNome()));
            } else {
                texto.append(' ').append(t(R.string.acesso_remoto));
            }
        }
        return texto.toString();
    }

    private static int textoDo(Acesso acesso) {
        switch (acesso) {
            case ACESSIBILIDADE:
                return R.string.acesso_acessibilidade;
            case ADMINISTRADOR:
                return R.string.acesso_administrador;
            default:
                return R.string.acesso_notificacoes;
        }
    }

    /** "a", "a e b", "a, b e c". */
    private String juntar(List<String> partes) {
        if (partes.size() == 1) return partes.get(0);
        String inicio = String.join(", ", partes.subList(0, partes.size() - 1));
        return inicio + t(R.string.conector_e) + partes.get(partes.size() - 1);
    }

    private String t(int id, Object... argumentos) {
        return argumentos.length == 0 ? contexto.getString(id) : contexto.getString(id, argumentos);
    }
}
