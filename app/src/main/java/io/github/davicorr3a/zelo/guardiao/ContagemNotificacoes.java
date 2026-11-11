package io.github.davicorr3a.zelo.guardiao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parte do histórico de notificações que não depende do Android: formato das chaves,
 * janela de dias e somas. Fica separada para ser testada com JUnit puro.
 *
 * Cada chave é "dia|pacote|canal", com o dia contado desde 1970. Nome de pacote nunca tem
 * barra vertical, mas id de canal pode ter, por isso o canal fica por último.
 */
final class ContagemNotificacoes {

    static final int DIAS = 7;

    private static final String SEPARADOR = "|";

    private ContagemNotificacoes() {
    }

    static String chave(long dia, String pacote, String canal) {
        return dia + SEPARADOR + pacote + SEPARADOR + (canal == null ? "" : canal);
    }

    /** Total de notificações dentro da janela, agrupado por app e, dentro de cada app, por canal. */
    static Map<String, Map<String, Integer>> porAppECanal(Map<String, ?> salvas, long hoje) {
        Map<String, Map<String, Integer>> total = new HashMap<>();
        for (Map.Entry<String, ?> item : salvas.entrySet()) {
            Registro registro = ler(item.getKey());
            if (registro == null || !naJanela(registro.dia, hoje) || !(item.getValue() instanceof Integer)) continue;
            total.computeIfAbsent(registro.pacote, pacote -> new HashMap<>())
                    .merge(registro.canal, (Integer) item.getValue(), Integer::sum);
        }
        return total;
    }

    /** Chaves de contagem que já saíram da janela e podem ser apagadas. */
    static List<String> vencidas(Collection<String> chaves, long hoje) {
        List<String> vencidas = new ArrayList<>();
        for (String chave : chaves) {
            Registro registro = ler(chave);
            if (registro != null && !naJanela(registro.dia, hoje)) vencidas.add(chave);
        }
        return vencidas;
    }

    private static boolean naJanela(long dia, long hoje) {
        return hoje - dia < DIAS;
    }

    private static Registro ler(String chave) {
        String[] partes = chave.split("\\|", 3);
        if (partes.length != 3 || partes[1].isEmpty()) return null;
        try {
            return new Registro(Long.parseLong(partes[0]), partes[1], partes[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class Registro {
        final long dia;
        final String pacote;
        final String canal;

        Registro(long dia, String pacote, String canal) {
            this.dia = dia;
            this.pacote = pacote;
            this.canal = canal;
        }
    }
}
