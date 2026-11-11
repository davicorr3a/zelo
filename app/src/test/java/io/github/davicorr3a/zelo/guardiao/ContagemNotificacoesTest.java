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
    public void agrupaPorAppEPorCanal() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.shopee.br", "promo"), 30);
        salvas.put(ContagemNotificacoes.chave(HOJE - 1, "com.shopee.br", "pedidos"), 2);
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.whatsapp", "msg"), 5);

        Map<String, Map<String, Integer>> total = ContagemNotificacoes.porAppECanal(salvas, HOJE);

        assertEquals(2, total.size());
        assertEquals(Integer.valueOf(30), total.get("com.shopee.br").get("promo"));
        assertEquals(Integer.valueOf(2), total.get("com.shopee.br").get("pedidos"));
        assertEquals(Integer.valueOf(5), total.get("com.whatsapp").get("msg"));
    }

    @Test
    public void ignoraDiasForaDaJanela() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE - ContagemNotificacoes.DIAS + 1, "a.b", "c"), 1);
        salvas.put(ContagemNotificacoes.chave(HOJE - ContagemNotificacoes.DIAS, "a.b", "c"), 100);

        assertEquals(Integer.valueOf(1), ContagemNotificacoes.porAppECanal(salvas, HOJE).get("a.b").get("c"));
    }

    @Test
    public void somaOsDiasDoMesmoCanal() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.shopee.br", "promo"), 30);
        salvas.put(ContagemNotificacoes.chave(HOJE - 2, "com.shopee.br", "promo"), 10);
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.shopee.br", "pedidos"), 2);
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.outro", "promo"), 99);

        Map<String, Integer> canais = ContagemNotificacoes.porAppECanal(salvas, HOJE).get("com.shopee.br");

        assertEquals(2, canais.size());
        assertEquals(Integer.valueOf(40), canais.get("promo"));
        assertEquals(Integer.valueOf(2), canais.get("pedidos"));
    }

    @Test
    public void canalComBarraVerticalNaoQuebraAChave() {
        Map<String, Object> salvas = new HashMap<>();
        salvas.put(ContagemNotificacoes.chave(HOJE, "com.android.chrome", "web:https://a.com|x"), 3);

        Map<String, Integer> canais = ContagemNotificacoes.porAppECanal(salvas, HOJE).get("com.android.chrome");

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

        assertEquals(1, ContagemNotificacoes.porAppECanal(salvas, HOJE).size());
        assertEquals(Integer.valueOf(1), ContagemNotificacoes.porAppECanal(salvas, HOJE).get("a.b").get("c"));
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
