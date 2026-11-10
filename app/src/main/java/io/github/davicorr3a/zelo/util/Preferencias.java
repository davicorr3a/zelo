package io.github.davicorr3a.zelo.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Guarda os apps que a pessoa disse reconhecer, para não perguntar de novo a cada revisão. */
public final class Preferencias {

    private static final String ARQUIVO = "zelo";
    private static final String APPS_CONFIAVEIS = "apps_confiaveis";

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

    private Set<String> confiaveis() {
        return prefs.getStringSet(APPS_CONFIAVEIS, Collections.emptySet());
    }
}
