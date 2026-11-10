package io.github.davicorr3a.zelo.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import io.github.davicorr3a.zelo.guardiao.GuardiaoService;

/**
 * Atalhos para as telas de configuração do Android.
 *
 * Um app comum não pode revogar permissões nem desinstalar outros apps sozinho.
 * O que dá para fazer é levar a pessoa direto até a tela certa, e é isso que essas funções fazem.
 */
public final class Atalhos {

    private Atalhos() {
    }

    public static Intent detalhesDoApp(String pacote) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pacote, null));
    }

    public static Intent desinstalar(String pacote) {
        return new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pacote, null));
    }

    public static Intent notificacoesDoApp(String pacote) {
        return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, pacote);
    }

    public static Intent acessibilidade() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    public static Intent seguranca() {
        return new Intent(Settings.ACTION_SECURITY_SETTINGS);
    }

    /** No Android 11 ou mais novo, abre direto no interruptor do Zelo em vez da lista inteira. */
    public static Intent acessoANotificacoes(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ComponentName guardiao = new ComponentName(contexto, GuardiaoService.class);
            return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, guardiao.flattenToString());
        }
        return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
    }

    /** Abre a tela. Se o fabricante tiver mudado alguma coisa e ela não existir, cai nas configurações gerais. */
    public static void abrir(Context contexto, Intent intent) {
        if (!(contexto instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            contexto.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent geral = new Intent(Settings.ACTION_SETTINGS);
            if (!(contexto instanceof Activity)) geral.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            contexto.startActivity(geral);
        }
    }
}
