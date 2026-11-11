package io.github.davicorr3a.zelo.organizacao;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decide o que a faxina de notificações vai oferecer: os sites que mandam notificação pelo navegador
 * e os apps que mais mandam. Não depende do Android, para ser testada com JUnit.
 *
 * As contagens chegam agrupadas por app e por canal, de duas fontes: o histórico da semana e o que
 * está na bandeja agora. O histórico só começa quando o Zelo é instalado, e a bandeja tem o que se
 * acumulou antes disso. Para cada canal vale o maior dos dois, porque uma notificação recente
 * aparece nas duas fontes e somar contaria duas vezes.
 */
public final class PlanoDeFaxina {

    static final int MAXIMO_DE_SITES = 5;
    static final int MAXIMO_DE_APPS = 3;
    static final int MINIMO_POR_APP = 5;

    // O Chrome e os navegadores feitos a partir dele criam um canal por site, com esse prefixo no id.
    private static final String PREFIXO_DE_SITE = "web:";
    // Canal que o Android cria para apps antigos, que não separam as notificações por tipo.
    private static final String CANAL_PADRAO = "miscellaneous";

    // Mandar muita notificação é o normal de quem conversa. Sugerir silenciar só atrapalharia.
    private static final Set<String> APPS_DE_CONVERSA = new HashSet<>(Arrays.asList(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.google.android.dialer",
            "com.samsung.android.dialer"
    ));

    private final List<Site> sites;
    private final List<AppBarulhento> apps;

    private PlanoDeFaxina(List<Site> sites, List<AppBarulhento> apps) {
        this.sites = sites;
        this.apps = apps;
    }

    /**
     * @param resolvidos canais sobre os quais a pessoa já respondeu, no formato de {@link #chave}.
     */
    public static PlanoDeFaxina montar(Map<String, Map<String, Integer>> semana,
                                       Map<String, Map<String, Integer>> bandeja,
                                       Set<String> resolvidos) {
        Map<String, Map<String, Integer>> juntas = juntar(semana, bandeja);
        List<Site> sites = new ArrayList<>();
        List<AppBarulhento> apps = new ArrayList<>();

        for (Map.Entry<String, Map<String, Integer>> app : juntas.entrySet()) {
            String pacote = app.getKey();
            Map<String, Integer> canaisDoApp = new HashMap<>();

            for (Map.Entry<String, Integer> canal : app.getValue().entrySet()) {
                if (resolvidos.contains(chave(pacote, canal.getKey()))) continue;
                if (canal.getKey().startsWith(PREFIXO_DE_SITE)) {
                    sites.add(new Site(pacote, canal.getKey(), dominio(canal.getKey()), canal.getValue()));
                } else {
                    canaisDoApp.put(canal.getKey(), canal.getValue());
                }
            }

            AppBarulhento barulhento = avaliar(pacote, canaisDoApp);
            if (barulhento != null) apps.add(barulhento);
        }

        sites.sort(Comparator.comparingInt((Site s) -> -s.quantidade).thenComparing(s -> s.dominio));
        apps.sort(Comparator.comparingInt((AppBarulhento a) -> -a.quantidade).thenComparing(a -> a.pacote));
        return new PlanoDeFaxina(
                new ArrayList<>(sites.subList(0, Math.min(sites.size(), MAXIMO_DE_SITES))),
                new ArrayList<>(apps.subList(0, Math.min(apps.size(), MAXIMO_DE_APPS))));
    }

    /** Identifica um canal de um app, para lembrar que a pessoa já respondeu sobre ele. */
    public static String chave(String pacote, String canal) {
        return pacote + "|" + canal;
    }

    public List<Site> getSites() {
        return Collections.unmodifiableList(sites);
    }

    public List<AppBarulhento> getApps() {
        return Collections.unmodifiableList(apps);
    }

    private static AppBarulhento avaliar(String pacote, Map<String, Integer> canais) {
        if (APPS_DE_CONVERSA.contains(pacote) || canais.isEmpty()) return null;

        int total = 0;
        String maisBarulhento = null;
        int doMaisBarulhento = -1;
        for (Map.Entry<String, Integer> canal : canais.entrySet()) {
            total += canal.getValue();
            boolean ganha = canal.getValue() > doMaisBarulhento
                    || (canal.getValue() == doMaisBarulhento && canal.getKey().compareTo(maisBarulhento) < 0);
            if (ganha) {
                maisBarulhento = canal.getKey();
                doMaisBarulhento = canal.getValue();
            }
        }
        if (total < MINIMO_POR_APP) return null;

        boolean porCanal = !maisBarulhento.isEmpty() && !maisBarulhento.equals(CANAL_PADRAO);
        return new AppBarulhento(pacote, maisBarulhento, total, porCanal, canais.size() > 1);
    }

    private static Map<String, Map<String, Integer>> juntar(Map<String, Map<String, Integer>> a,
                                                            Map<String, Map<String, Integer>> b) {
        Map<String, Map<String, Integer>> juntas = new HashMap<>();
        for (Map<String, Map<String, Integer>> fonte : Arrays.asList(a, b)) {
            for (Map.Entry<String, Map<String, Integer>> app : fonte.entrySet()) {
                Map<String, Integer> canais = juntas.computeIfAbsent(app.getKey(), p -> new HashMap<>());
                for (Map.Entry<String, Integer> canal : app.getValue().entrySet()) {
                    canais.merge(canal.getKey(), canal.getValue(), Math::max);
                }
            }
        }
        return juntas;
    }

    /** "web:https://www.premio.com" vira "premio.com". */
    static String dominio(String canal) {
        String endereco = canal.substring(PREFIXO_DE_SITE.length());
        try {
            String host = new URI(endereco).getHost();
            if (host == null || host.isEmpty()) return endereco;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException e) {
            return endereco;
        }
    }

    /** Um site que mandou notificação por um navegador. */
    public static final class Site {
        public final String navegador;
        public final String canal;
        public final String dominio;
        public final int quantidade;

        Site(String navegador, String canal, String dominio, int quantidade) {
            this.navegador = navegador;
            this.canal = canal;
            this.dominio = dominio;
            this.quantidade = quantidade;
        }
    }

    /** Um app que mandou notificação demais. */
    public static final class AppBarulhento {
        public final String pacote;
        /** O canal que mais mandou. É ele que a pessoa vai poder desligar. */
        public final String canal;
        public final int quantidade;
        /** Falso quando o app não separa as notificações por tipo: aí só dá para silenciar o app inteiro. */
        public final boolean porCanal;
        /** Se outros tipos de aviso dele apareceram, eles continuam chegando depois de desligar este. */
        public final boolean temOutrosCanais;

        AppBarulhento(String pacote, String canal, int quantidade, boolean porCanal, boolean temOutrosCanais) {
            this.pacote = pacote;
            this.canal = canal;
            this.quantidade = quantidade;
            this.porCanal = porCanal;
            this.temOutrosCanais = temOutrosCanais;
        }
    }
}
