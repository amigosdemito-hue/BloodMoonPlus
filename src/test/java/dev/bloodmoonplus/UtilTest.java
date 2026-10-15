package dev.bloodmoonplus;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @Test
    void pick_emptyList_returnsNull() {
        assertNull(Util.pick(List.of(), x -> 1.0));
    }

    @Test
    void pick_allZeroWeights_returnsNull() {
        assertNull(Util.pick(List.of("a", "b"), x -> 0.0));
    }

    @Test
    void pick_singleElement_returnsIt() {
        assertEquals("solo", Util.pick(List.of("solo"), x -> 5.0));
    }

    @Test
    void pick_weightedDistribution_prefersHigherWeight() {
        // Con peso 1 vs 100, "b" debería salir ~99% de las veces
        List<String> items = List.of("a", "b");
        int aCount = 0;
        for (int i = 0; i < 500; i++) {
            String r = Util.pick(items, x -> x.equals("a") ? 1.0 : 100.0);
            if ("a".equals(r)) aCount++;
        }
        assertTrue(aCount < 50, "Con peso 1:100, 'a' salió " + aCount + "/500 veces (esperado <50)");
    }

    @Test
    void pick_negativeWeight_ignored() {
        assertEquals("ok", Util.pick(List.of("bad", "ok"), x -> x.equals("bad") ? -10.0 : 1.0));
    }

    @Test
    void range_singleValue() {
        assertArrayEquals(new int[]{3, 3}, Util.range("3", 1, 1));
    }

    @Test
    void range_rangeValue() {
        assertArrayEquals(new int[]{2, 5}, Util.range("2-5", 1, 1));
    }

    @Test
    void range_reversedRange_normalized() {
        assertArrayEquals(new int[]{2, 5}, Util.range("5-2", 1, 1));
    }

    @Test
    void range_invalidString_returnsDefault() {
        assertArrayEquals(new int[]{1, 1}, Util.range("abc", 1, 1));
    }

    @Test
    void range_null_returnsDefault() {
        assertArrayEquals(new int[]{4, 7}, Util.range(null, 4, 7));
    }

    @Test
    void between_alwaysInRange() {
        int[] r = {3, 7};
        for (int i = 0; i < 100; i++) {
            int v = Util.between(r);
            assertTrue(v >= 3 && v <= 7, "between devolvió " + v + " fuera de rango [3,7]");
        }
    }

    @Test
    void between_sameMinMax_alwaysThatValue() {
        assertEquals(5, Util.between(new int[]{5, 5}));
    }

    @Test
    void dur_zero_returns00_00() {
        assertEquals("00:00", Util.dur(0));
    }

    @Test
    void dur_seconds() {
        assertEquals("01:05", Util.dur(65));
    }

    @Test
    void dur_hours() {
        assertEquals("1:02:03", Util.dur(3723));
    }

    @Test
    void dur_negative_clampedToZero() {
        assertEquals("00:00", Util.dur(-10));
    }

    @Test
    void fmt_wholeNumber_noDecimal() {
        assertEquals("3", Util.fmt(3.0));
    }

    @Test
    void fmt_decimal_showsOneDecimal() {
        assertEquals("2.5", Util.fmt(2.5));
    }

    @Test
    void fmt_negativeDecimal() {
        assertEquals("-1.5", Util.fmt(-1.5));
    }

    @Test
    void key_uppercaseAndTrimmed() {
        assertEquals("HOLA", Util.key("hola"));
    }

    @Test
    void key_stripsAccents() {
        assertEquals("COHETE", Util.key("cohete"));
        assertEquals("NINO", Util.key("niño"));
    }

    @Test
    void key_null_orEmpty_handled() {
        // key(null) devolvería NPE; el código nunca lo usa con null
        assertEquals("", Util.key(""));
    }

    @Test
    void num_fromNumber() {
        Map<String, Object> m = new HashMap<>();
        m.put("x", 42.5);
        assertEquals(42.5, Util.num(m, "x", 0));
    }

    @Test
    void num_fromString() {
        Map<String, Object> m = new HashMap<>();
        m.put("x", "3.14");
        assertEquals(3.14, Util.num(m, "x", 0), 0.001);
    }

    @Test
    void num_missingKey_returnsDefault() {
        Map<String, Object> m = new HashMap<>();
        assertEquals(9.9, Util.num(m, "y", 9.9), 0.001);
    }

    @Test
    void num_invalidString_returnsDefault() {
        Map<String, Object> m = new HashMap<>();
        m.put("x", "abc");
        assertEquals(7.0, Util.num(m, "x", 7.0), 0.001);
    }

    @Test
    void str_fromMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("k", "val");
        assertEquals("val", Util.str(m, "k", "default"));
    }

    @Test
    void str_missingKey_returnsDefault() {
        Map<String, Object> m = new HashMap<>();
        assertEquals("default", Util.str(m, "missing", "default"));
    }
}
