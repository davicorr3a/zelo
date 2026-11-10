package io.github.davicorr3a.zelo.guardiao;

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

    /** Retorna o serviço se o acesso às notificações estiver liberado e conectado, ou null. */
    public static GuardiaoService ativo() {
        return ativo;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        ativo = this;
        escutarInstalacoes();
        observarAcessibilidade();
    }

    @Override
    public void onListenerDisconnected() {
        ativo = null;
        pararDeEscutar();
        super.onListenerDisconnected();
    }

    @Override
    public void onDestroy() {
        ativo = null;
        pararDeEscutar();
        super.onDestroy();
    }

    /** Quantas notificações cada app tem na bandeja agora (só as que podem ser limpas). */
    public Map<String, Integer> contarPorApp() {
        Map<String, Integer> contagem = new HashMap<>();
        StatusBarNotification[] ativas;
        try {
            ativas = getActiveNotifications();
        } catch (SecurityException e) {
            return contagem;
        }
        if (ativas == null) return contagem;

        for (StatusBarNotification notificacao : ativas) {
            if (!notificacao.isClearable() || notificacao.getPackageName().equals(getPackageName())) continue;
            contagem.merge(notificacao.getPackageName(), 1, Integer::sum);
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
