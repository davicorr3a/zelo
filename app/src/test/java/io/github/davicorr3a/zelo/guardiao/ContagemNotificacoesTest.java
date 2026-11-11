package io.github.davicorr3a.zelo.guardiao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContagemNotificacoesTest {

    private static final long HOJE = 20_000;

    @Test
    public void somaPorAppJuntandoDiasECanais() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.shopee.br", "promo"), 30);
        salvas.put(ContagemNotificacoes.chave(HOJE - 1, "com.shopee.br", "pedidos"), 2);
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.whatsapp", "msg"), 5);

        Map<String, Integer> total = ContagemNotificacoes.porApp(salvas, HOJE);

        assertEquals(Integer.valueOf(32), total.get("com.shopee.br"));
        assertEquals(Integer.valueOf(5), total.get("com.whatsapp"));
    }

    @Test
    public void ignoraDiasForaDaJanela() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE - ContagemNotificacoes.DIAS + 1, "a.b", "c"), 1);
        salvas.put(ContagemNotificacoes.chave(HOJE - ContagemNotificacoes.DIAS, "a.b", "c"), 100);

        assertEquals(Integer.valueOf(1), ContagemNotificacoes.porApp(salvas, HOJE).get("a.b"));
    }

    @Test
    public void somaPorCanalSoDoAppPedido() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.shopee.br", "promo"), 30);
        salvas.put(ContagemNotificacoes.chave(HOJE - 2, "com.shopee.br", "promo"), 10);
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.shopee.br", "pedidos"), 2);
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.outro", "promo"), 99);

        Map<String, Integer> canais = ContagemNotificacoes.porCanal(salvas, "com.shopee.br", HOJE);

        assertEquals(2, canais.size());
        assertEquals(Integer.valueOf(40), canais.get("promo"));
        assertEquals(Integer.valueOf(2), canais.get("pedidos"));
    }

    @Test
    public void canalComBarraVerticalNaoQuebraAChave() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.android.chrome", "web:https://a.com|x"), 3);

        Map<String, Integer> canais = ContagemNotificacoes.porCanal(salvas, "com.android.chrome", HOJE);

        assertEquals(Integer.valueOf(3), canais.get("web:https://a.com|x"));
    }

    @Test
    public void canalNuloViraVazio() {
        assertEquals(HOJE + "|a.b|", ContagemNotificacoes.chave(HOJE, "a.b", null));
    }

    @Test
    public void ignoraChavesQueNaoSaoContagem() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put("ultima_limpeza", HOJE);
        salvas.put("abc|a.b|c", 4);
        salvas.put(ContagemNotificacoes.chave(HOJE, "a.b", "c"), 1);

        assertEquals(Integer.valueOf(1), ContagemNotificacoes.porApp(salvas, HOJE).get("a.b"));
    }

    @Test
    public void vencidasSoListaContagensAntigas() {
        String antiga = ContagemNotificacoes.chave(HOJE - ContagemNotificacoes.DIAS, "a.b", "c");
        String recente = ContagemNotificacoes.chave(HOJE, "a.b", "c");

        List<String> vencidas = ContagemNotificacoes.vencidas(List.of(antiga, recente, "ultima_limpeza"), HOJE);

        assertEquals(1, vencidas.size());
        assertTrue(vencidas.contains(antiga));
    }
}
