package io.github.davicorr3a.zelo.organizacao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlanoDeFaxinaTest {

    private static final Map<String, Map<String, Integer>> NADA = Collections.emptyMap();
    private static final Set<String> NENHUM = Collections.emptySet();

    @Test
    public void sitesDoNavegadorViramSitesENaoContamParaOApp() {
        Map<String, Map<String, Integer>> semana = new HashMap<>();
        canal(semana, "com.android.chrome", "web:https://www.premio-xyz.com", 12);
        canal(semana, "com.android.chrome", "web:https://noticias.com.br", 3);
        canal(semana, "com.android.chrome", "downloads", 1);

        PlanoDeFaxina plano = PlanoDeFaxina.montar(semana, NADA, NENHUM);

        assertEquals(2, plano.getSites().size());
        assertEquals("premio-xyz.com", plano.getSites().get(0).dominio);
        assertEquals(12, plano.getSites().get(0).quantidade);
        assertEquals("com.android.chrome", plano.getSites().get(0).navegador);
        assertTrue(plano.getApps().isEmpty());
    }

    @Test
    public void appBarulhentoApontaOCanalQueMaisMandou() {
        Map<String, Map<String, Integer>> semana = new HashMap<>();
        canal(semana, "com.shopee.br", "promocoes", 80);
        canal(semana, "com.shopee.br", "pedidos", 3);

        List<PlanoDeFaxina.AppBarulhento> apps = PlanoDeFaxina.montar(semana, NADA, NENHUM).getApps();

        assertEquals(1, apps.size());
        assertEquals("promocoes", apps.get(0).canal);
        assertEquals(83, apps.get(0).quantidade);
        assertTrue(apps.get(0).porCanal);
        assertTrue(apps.get(0).temOutrosCanais);
    }

    @Test
    public void appSemCanaisProprioSilenciaOAppInteiro() {
        Map<String, Map<String, Integer>> semana = new HashMap<>();
        canal(semana, "com.antigo", "miscellaneous", 20);

        PlanoDeFaxina.AppBarulhento app = PlanoDeFaxina.montar(semana, NADA, NENHUM).getApps().get(0);

        assertFalse(app.porCanal);
        assertFalse(app.temOutrosCanais);
    }

    @Test
    public void usaOMaiorEntreSemanaEBandejaSemSomar() {
        Map<String, Map<String, Integer>> semana = new HashMap<>();
        canal(semana, "com.loja", "promo", 4);
        Map<String, Map<String, Integer>> bandeja = new HashMap<>();
        canal(bandeja, "com.loja", "promo", 50);
        canal(bandeja, "com.outra", "promo", 6);
        canal(semana, "com.outra", "promo", 6);

        List<PlanoDeFaxina.AppBarulhento> apps = PlanoDeFaxina.montar(semana, bandeja, NENHUM).getApps();

        assertEquals(50, apps.get(0).quantidade);
        assertEquals(6, apps.get(1).quantidade);
    }

    @Test
    public void ignoraAppsDeConversaEAppsQuietos() {
        Map<String, Map<String, Integer>> semana = new HashMap<>();
        canal(semana, "com.whatsapp", "mensagens", 300);
        canal(semana, "com.quieto", "avisos", PlanoDeFaxina.MINIMO_POR_APP - 1);

        assertTrue(PlanoDeFaxina.montar(semana, NADA, NENHUM).getApps().isEmpty());
    }

    @Test
    public void limitaATresAppsDoMaisBarulhentoParaOMenos() {
        Map<String, Map<String, Integer>> semana = new HashMap<>();
        canal(semana, "a.a", "x", 10);
        canal(semana, "b.b", "x", 40);
        canal(semana, "c.c", "x", 20);
        canal(semana, "d.d", "x", 30);

        List<PlanoDeFaxina.AppBarulhento> apps = PlanoDeFaxina.montar(semana, NADA, NENHUM).getApps();

        assertEquals(PlanoDeFaxina.MAXIMO_DE_APPS, apps.size());
        assertEquals("b.b", apps.get(0).pacote);
        assertEquals("d.d", apps.get(1).pacote);
        assertEquals("c.c", apps.get(2).pacote);
    }

    @Test
    public void naoPerguntaDeNovoSobreCanalResolvido() {
        Map<String, Map<String, Integer>> semana = new HashMap<>();
        canal(semana, "com.shopee.br", "promocoes", 80);
        canal(semana, "com.shopee.br", "pedidos", 9);
        canal(semana, "com.android.chrome", "web:https://a.com", 5);
        Set<String> resolvidos = new HashSet<>();
        resolvidos.add(PlanoDeFaxina.chave("com.shopee.br", "promocoes"));
        resolvidos.add(PlanoDeFaxina.chave("com.android.chrome", "web:https://a.com"));

        PlanoDeFaxina plano = PlanoDeFaxina.montar(semana, NADA, resolvidos);

        assertTrue(plano.getSites().isEmpty());
        assertEquals("pedidos", plano.getApps().get(0).canal);
        assertFalse(plano.getApps().get(0).temOutrosCanais);
    }

    @Test
    public void dominioSemEsquemaOuInvalidoVoltaComoVeio() {
        assertEquals("premio.com", PlanoDeFaxina.dominio("web:https://www.premio.com"));
        assertEquals("sub.site.com.br", PlanoDeFaxina.dominio("web:https://sub.site.com.br:8443"));
        assertEquals("algo estranho", PlanoDeFaxina.dominio("web:algo estranho"));
    }

    private static void canal(Map<String, Map<String, Integer>> mapa, String pacote, String canal, int quantidade) {
        mapa.computeIfAbsent(pacote, p -> new HashMap<>()).put(canal, quantidade);
    }
}
