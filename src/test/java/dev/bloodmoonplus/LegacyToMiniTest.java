package dev.bloodmoonplus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El texto de muerte de Vanilla trae §c/§4§l; MiniMessage 5.x lo rechaza con excepción.
 *  Estas pruebas garantizan que la conversión evita que PlayerDeathEvent reviente. */
class LegacyToMiniTest {

    @Test
    void convertsSimpleColorAndStyles() {
        assertEquals("slain by <red>Zombi", Util.legacyToMini("slain by §cZombi"));
        assertEquals("<dark_red><bold>Rey", Util.legacyToMini("§4§lRey"));
        assertEquals("<gray>(<white>x<gray>)", Util.legacyToMini("§7(§fx§7)"));
    }

    @Test
    void convertsHexLegacy() {
        assertEquals("<#AABBCC>texto", Util.legacyToMini("§x§A§A§B§B§C§Ctexto"));
    }

    @Test
    void leavesCleanTextUntouched() {
        String clean = "<red>MiniMessage normal";
        assertEquals(clean, Util.legacyToMini(clean));
        assertNull(Util.legacyToMini(null));
    }

    @Test
    void miniNeverThrowsOnLegacyText() {
        assertDoesNotThrow(() -> Util.mini("§c§l¡murió §7por un Zombi Sanguinario"));
        assertDoesNotThrow(() -> Util.item("§x§F§F§0§0§4§0gradiente"));
    }
}
