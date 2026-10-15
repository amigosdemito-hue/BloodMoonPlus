package dev.bloodmoonplus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BloodMoonCommandTest {

    // ---------------------------------------------------------------- parseMult

    @Test
    void parseMult_triple_returns3() {
        assertEquals(3.0, BloodMoonCommand.parseMult("triple"));
    }

    @Test
    void parseMult_tripleCaseInsensitive() {
        assertEquals(3.0, BloodMoonCommand.parseMult("TRIPLE"));
    }

    @Test
    void parseMult_x3_returns3() {
        assertEquals(3.0, BloodMoonCommand.parseMult("x3"));
    }

    @Test
    void parseMult_x2_5_returns2_5() {
        assertEquals(2.5, BloodMoonCommand.parseMult("x2.5"));
    }

    @Test
    void parseMult_x10_returns10() {
        assertEquals(10.0, BloodMoonCommand.parseMult("x10"));
    }

    @Test
    void parseMult_invalid_returnsNull() {
        assertNull(BloodMoonCommand.parseMult("abc"));
    }

    @Test
    void parseMult_justX_returnsNull() {
        assertNull(BloodMoonCommand.parseMult("x"));
    }

    @Test
    void parseMult_xInvalid_returnsNull() {
        assertNull(BloodMoonCommand.parseMult("xabc"));
    }

    // ---------------------------------------------------------------- parseMinutes

    @Test
    void parseMinutes_30m_returns30() {
        assertEquals(30, BloodMoonCommand.parseMinutes("30m"));
    }

    @Test
    void parseMinutes_120m_returns120() {
        assertEquals(120, BloodMoonCommand.parseMinutes("120m"));
    }

    @Test
    void parseMinutes_caseInsensitive() {
        assertEquals(45, BloodMoonCommand.parseMinutes("45M"));
    }

    @Test
    void parseMinutes_plainNumber_returnsNull() {
        assertNull(BloodMoonCommand.parseMinutes("30"));
    }

    @Test
    void parseMinutes_justM_returnsNull() {
        assertNull(BloodMoonCommand.parseMinutes("m"));
    }

    @Test
    void parseMinutes_invalid_returnsNull() {
        assertNull(BloodMoonCommand.parseMinutes("abc"));
    }

    @Test
    void parseMinutes_empty_returnsNull() {
        assertNull(BloodMoonCommand.parseMinutes(""));
    }
}
