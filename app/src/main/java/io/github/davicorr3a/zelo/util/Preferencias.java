package io.github.davicorr3a.zelo.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Guarda as respostas que não devem ser perguntadas de novo: os apps que a pessoa disse reconhecer
 * e os tipos de aviso sobre os quais ela já decidiu na faxina de notificações.
 */
public final class Preferencias {

    private static final String ARQUIVO = "zelo";
    private static final String APPS_CONFIAVEIS = "apps_confiaveis";
    private static final String CANAIS_RESOLVIDOS = "canais_resolvidos";

    private final SharedPreferences prefs;

    public Preferencias(Context contexto) {
        prefs = contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public boolean confia(String pacote) {
        return confiaveis().contains(pacote);
    }

    public void confiar(String pacote) {
        Set<String> apps = new HashSet<>(confiaveis());
        apps.add(pacote);
        prefs.edit().putStringSet(APPS_CONFIAVEIS, apps).apply();
    }

    public void desconfiar(String pacote) {
        Set<String> apps = new HashSet<>(confiaveis());
        if (apps.remove(pacote)) {
            prefs.edit().putStringSet(APPS_CONFIAVEIS, apps).apply();
        }
    }

    public Set<String> canaisResolvidos() {
        return prefs.getStringSet(CANAIS_RESOLVIDOS, Collections.emptySet());
    }

    public void resolverCanal(String chave) {
        Set<String> canais = new HashSet<>(canaisResolvidos());
        canais.add(chave);
        prefs.edit().putStringSet(CANAIS_RESOLVIDOS, canais).apply();
    }

    private Set<String> confiaveis() {
        return prefs.getStringSet(APPS_CONFIAVEIS, Collections.emptySet());
    }
}
