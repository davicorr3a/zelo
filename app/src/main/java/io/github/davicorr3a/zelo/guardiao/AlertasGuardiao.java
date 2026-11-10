package io.github.davicorr3a.zelo.guardiao;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import io.github.davicorr3a.zelo.R;
import io.github.davicorr3a.zelo.analise.AnalisadorApps;
import io.github.davicorr3a.zelo.ui.ConversaActivity;

/**
 * Monta as notificações do guardião. Tocar na notificação abre a conversa
 * já no ponto certo, perguntando sobre o app que causou o aviso.
 */
public final class AlertasGuardiao {

    public static final String EXTRA_PACOTE = "io.github.davicorr3a.zelo.extra.PACOTE";
    public static final String EXTRA_MOTIVO = "io.github.davicorr3a.zelo.extra.MOTIVO";

    public static final String MOTIVO_INSTALACAO = "instalacao";
    public static final String MOTIVO_ACESSIBILIDADE = "acessibilidade";

    private static final String CANAL = "guardiao";

    private AlertasGuardiao() {
    }

    static void appInstalado(Context contexto, String pacote) {
        int titulo = AnalisadorApps.ehAcessoRemoto(pacote)
                ? R.string.notif_remoto_titulo
                : R.string.notif_instalacao_titulo;
        notificar(contexto, pacote, MOTIVO_INSTALACAO, contexto.getString(titulo));
    }

    static void acessibilidadeAtivada(Context contexto, String pacote) {
        // Ligar o TalkBack ou outro recurso que veio no aparelho não é motivo de alarme.
        if (AnalisadorApps.ehDoSistema(contexto, pacote)) return;
        notificar(contexto, pacote, MOTIVO_ACESSIBILIDADE, contexto.getString(R.string.notif_acessibilidade_titulo));
    }

    private static void notificar(Context contexto, String pacote, String motivo, String titulo) {
        NotificationManager gerenciador = contexto.getSystemService(NotificationManager.class);
        if (gerenciador == null) return;
        criarCanal(contexto, gerenciador);

        Intent abrirConversa = new Intent(contexto, ConversaActivity.class)
                .putExtra(EXTRA_PACOTE, pacote)
                .putExtra(EXTRA_MOTIVO, motivo)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int id = (motivo + pacote).hashCode();
        PendingIntent aoTocar = PendingIntent.getActivity(contexto, id, abrirConversa,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        String texto = contexto.getString(R.string.notif_texto, AnalisadorApps.nomeDoApp(contexto, pacote));
        Notification notificacao = new Notification.Builder(contexto, CANAL)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new Notification.BigTextStyle().bigText(texto))
                .setContentIntent(aoTocar)
                .setAutoCancel(true)
                .build();

        try {
            gerenciador.notify(id, notificacao);
        } catch (SecurityException ignorada) {
            // Sem permissão de notificação no Android 13+. A pessoa vai ver o aviso na próxima revisão.
        }
    }

    private static void criarCanal(Context contexto, NotificationManager gerenciador) {
        NotificationChannel canal = new NotificationChannel(CANAL,
                contexto.getString(R.string.notif_canal), NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(contexto.getString(R.string.notif_canal_desc));
        gerenciador.createNotificationChannel(canal);
    }
}
