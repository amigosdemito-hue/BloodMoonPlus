package dev.bloodmoonplus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del recorte de la mecha de los mini-jefes creeper. Paper lanza
 * "IllegalArgumentException: ticks > maxFuseTicks" si se le da una mecha mayor que el
 * máximo de la entidad (30 por defecto), que es lo que rompía la habilidad SWARM con las
 * crías de 3 segundos (60 ticks).
 */
class BossManagerTest {

    @Test
    void fuseAboveDefaultLimit_isClampedToDefault() {
        assertEquals(30, BossManager.fuseWithinLimits(60, 30));
    }

    @Test
    void fuseBelowLimit_isKept() {
        assertEquals(20, BossManager.fuseWithinLimits(20, 30));
    }

    @Test
    void fuseEqualToLimit_isKept() {
        assertEquals(30, BossManager.fuseWithinLimits(30, 30));
    }

    @Test
    void raisedLimit_allowsLongFuse() {
        // Cuando spawnMini amplía maxFuseTicks, la mecha de 3 s cabe entera.
        assertEquals(60, BossManager.fuseWithinLimits(60, 60));
    }

    @Test
    void zeroOrNegativeFuse_neverZero() {
        // Una mecha de 0 explotaría al instante; el mínimo es 1 tick.
        assertEquals(1, BossManager.fuseWithinLimits(0, 30));
        assertEquals(1, BossManager.fuseWithinLimits(-50, 30));
    }

    @Test
    void invalidMaxFuseTicks_fallsBackToOne() {
        assertEquals(1, BossManager.fuseWithinLimits(60, 0));
        assertEquals(1, BossManager.fuseWithinLimits(60, -10));
    }

    @Test
    void neverExceedsMaxFuseTicks() {
        int[] fuses = {1, 20, 40, 60, 200};
        int[] limits = {1, 20, 30, 60, 120};
        for (int f : fuses) {
            for (int l : limits) {
                int out = BossManager.fuseWithinLimits(f, l);
                assertTrue(out >= 1, "mecha " + out + " < 1 (fuse=" + f + ", max=" + l + ")");
                assertTrue(out <= Math.max(1, l), "mecha " + out + " > max " + l);
                assertTrue(out <= f, "la mecha no debe alargarse sola");
            }
        }
    }
}
