package io.github.davicorr3a.zelo.guardiao;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.content.ContextCompat;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.davicorr3a.zelo.analise.AnalisadorApps;

/**
 * O guardião do Zelo.
 *
 * É um NotificationListenerService por dois motivos: precisa ler as notificações para contar e limpar,
 * e o sistema mantém esse tipo de serviço vivo enquanto o acesso estiver liberado. Aproveitando isso,
 * ele também fica escutando instalações de apps e mudanças na acessibilidade.
 */
public class GuardiaoService extends NotificationListenerService {

    private static volatile GuardiaoService ativo;

    private BroadcastReceiver receptorDeInstalacao;
    private ContentObserver observadorDeAcessibilidade;
    private Set<String> acessibilidadeConhecida = Collections.emptySet();
    private HistoricoNotificacoes historico;
    // Chaves das notificações que estão na bandeja. Serve para não contar de novo quando um app
    // só atualiza a mesma notificação (barra de download, por exemplo).
    private final Set<String> naBandeja = new HashSet<>();

    /** Retorna o serviço se o acesso às notificações estiver liberado e conectado, ou null. */
    public static GuardiaoService ativo() {
        return ativo;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        historico = new HistoricoNotificacoes(this);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        ativo = this;
        naBandeja.clear();
        for (StatusBarNotification notificacao : notificacoesNaBandeja()) naBandeja.add(notificacao.getKey());
        escutarInstalacoes();
        observarAcessibilidade();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notificacao) {
        if (!naBandeja.add(notificacao.getKey())) return;
        if (!contaParaOZelo(notificacao) || ehResumoDeGrupo(notificacao)) return;
        historico.registrar(notificacao.getPackageName(), canalDe(notificacao));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notificacao) {
        naBandeja.remove(notificacao.getKey());
    }

    @Override
    public void onListenerDisconnected() {
        ativo = null;
        naBandeja.clear();
        pararDeEscutar();
        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {
        ativo = null;
        pararDeEscutar();
        super.onDestroy();
    }

    /** Quantas notificações estão na bandeja agora, por app e por canal (só as que podem ser limpas). */
    public Map<String, Map<String, Integer>> contarNaBandeja() {
        Map<String, Map<String, Integer>> contagem = new HashMap<>();
        for (StatusBarNotification notificacao : notificacoesNaBandeja()) {
            if (!contaParaOZelo(notificacao) || ehResumoDeGrupo(notificacao)) continue;
            contagem.computeIfAbsent(notificacao.getPackageName(), pacote -> new HashMap<>())
                    .merge(canalDe(notificacao), 1, Integer::sum);
        }
        return contagem;
    }

    public void limparTudo() {
        try {
            cancelAllNotifications();
        } catch (SecurityException ignorada) {
            // O acesso foi revogado entre a contagem e o toque no botão.
        }
    }

    private StatusBarNotification[] notificacoesNaBandeja() {
        try {
            StatusBarNotification[] ativas = getActiveNotifications();
            return ativas != null ? ativas : new StatusBarNotification[0];
        } catch (SecurityException e) {
            return new StatusBarNotification[0];
        }
    }

    /** Notificações fixas (música tocando, GPS) e as do próprio Zelo ficam de fora. */
    private boolean contaParaOZelo(StatusBarNotification notificacao) {
        return notificacao.isClearable() && !notificacao.getPackageName().equals(getPackageName());
    }

    private static String canalDe(StatusBarNotification notificacao) {
        String canal = notificacao.getNotification().getChannelId();
        return canal != null ? canal : "";
    }

    // O resumo é o cabeçalho que o Android junta em cima de várias notificações do mesmo app.
    // Contar ele seria contar uma notificação que a pessoa não recebeu.
    private static boolean ehResumoDeGrupo(StatusBarNotification notificacao) {
        return (notificacao.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0;
    }

    private void escutarInstalacoes() {
        if (receptorDeInstalacao != null) return;

        receptorDeInstalacao = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Atualização de um app que já existia não interessa.
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return;
                Uri dados = intent.getData();
                if (dados == null) return;
                AlertasGuardiao.appInstalado(context, dados.getSchemeSpecificPart());
            }
        };

        IntentFilter filtro = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filtro.addDataScheme("package");
        ContextCompat.registerReceiver(this, receptorDeInstalacao, filtro, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    private void observarAcessibilidade() {
        if (observadorDeAcessibilidade != null) return;

        acessibilidadeConhecida = AnalisadorApps.pacotesComAcessibilidade(this);
        observadorDeAcessibilidade = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                Set<String> atual = AnalisadorApps.pacotesComAcessibilidade(GuardiaoService.this);
                for (String pacote : atual) {
                    if (!acessibilidadeConhecida.contains(pacote) && !pacote.equals(getPackageName())) {
                        AlertasGuardiao.acessibilidadeAtivada(GuardiaoService.this, pacote);
                    }
                }
                acessibilidadeConhecida = atual;
            }
        };
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                observadorDeAcessibilidade);
    }

    private void pararDeEscutar() {
        if (receptorDeInstalacao != null) {
            try {
                unregisterReceiver(receptorDeInstalacao);
            } catch (IllegalArgumentException ignorada) {
                // Já tinha sido removido.
            }
            receptorDeInstalacao = null;
        }
        if (observadorDeAcessibilidade != null) {
            getContentResolver().unregisterContentObserver(observadorDeAcessibilidade);
            observadorDeAcessibilidade = null;
        }
    }
}
