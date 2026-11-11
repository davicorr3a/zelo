package io.github.davicorr3a.zelo.guardiao;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.Map;

/**
 * Quantas notificações cada app mandou nos últimos sete dias, separado por canal.
 *
 * Guarda só números. Título e texto das notificações nunca são salvos.
 */
public final class HistoricoNotificacoes {

    private static final String ARQUIVO = "historico_notificacoes";
    // Não tem a barra vertical, então nunca é confundida com uma chave de contagem.
    private static final String ULTIMA_LIMPEZA = "ultima_limpeza";

    private final SharedPreferences prefs;

    public HistoricoNotificacoes(Context contexto) {
        prefs = contexto.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    void registrar(String pacote, String canal) {
        long hoje = hoje();
        String chave = ContagemNotificacoes.chave(hoje, pacote, canal);
        SharedPreferences.Editor editor = prefs.edit().putInt(chave, prefs.getInt(chave, 0) + 1);

        // Uma limpeza por dia basta, e evita percorrer o arquivo inteiro a cada notificação.
        if (prefs.getLong(ULTIMA_LIMPEZA, -1) != hoje) {
            for (String vencida : ContagemNotificacoes.vencidas(prefs.getAll().keySet(), hoje)) {
                editor.remove(vencida);
            }
            editor.putLong(ULTIMA_LIMPEZA, hoje);
        }
        editor.apply();
    }

    /** Contagem dos últimos sete dias, por app e por canal. */
    public Map<String, Map<String, Integer>> porAppECanal() {
        return ContagemNotificacoes.porAppECanal(prefs.getAll(), hoje());
    }

    private static long hoje() {
        return LocalDate.now().toEpochDay();
    }
}
