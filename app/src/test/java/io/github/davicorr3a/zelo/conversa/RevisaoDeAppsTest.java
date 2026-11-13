package io.github.davicorr3a.zelo.conversa;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class RevisaoDeAppsTest {

    private static final String E = " e ";

    @Test
    public void umaParteSaiSozinha() {
        assertEquals("ler suas notificações", RevisaoDeApps.juntar(Collections.singletonList("ler suas notificações"), E));
    }

    @Test
    public void duasPartesUsamSoOConector() {
        assertEquals("a e b", RevisaoDeApps.juntar(Arrays.asList("a", "b"), E));
    }

    @Test
    public void tresOuMaisUsamVirgulaEConectorNoFim() {
        assertEquals("a, b e c", RevisaoDeApps.juntar(Arrays.asList("a", "b", "c"), E));
        assertEquals("a, b, c e d", RevisaoDeApps.juntar(Arrays.asList("a", "b", "c", "d"), E));
    }
}
