package dev.bloodmoonplus;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleManagerTest {

    // ---------------------------------------------------------------- parseTime

    @Test
    void parseTime_null_returnsNull() {
        assertNull(ScheduleManager.parseTime(null));
    }

    @Test
    void parseTime_stringValid_returnsTime() {
        assertEquals(LocalTime.of(21, 30), ScheduleManager.parseTime("21:30"));
    }

    @Test
    void parseTime_stringWithSpaces_trimmed() {
        assertEquals(LocalTime.of(8, 5), ScheduleManager.parseTime(" 08:05 "));
    }

    @Test
    void parseTime_stringInvalid_returnsNull() {
        assertNull(ScheduleManager.parseTime("no-es-hora"));
    }

    @Test
    void parseTime_stringOutOfRange_returnsNull() {
        assertNull(ScheduleManager.parseTime("25:00"));
    }

    @Test
    void parseTime_number1290_returns21_30() {
        // YAML sin comillas convierte 21:30 a 1290 (minutos)
        assertEquals(LocalTime.of(21, 30), ScheduleManager.parseTime(1290));
    }

    @Test
    void parseTime_number0_returns00_00() {
        assertEquals(LocalTime.of(0, 0), ScheduleManager.parseTime(0));
    }

    @Test
    void parseTime_number1439_returns23_59() {
        assertEquals(LocalTime.of(23, 59), ScheduleManager.parseTime(1439));
    }

    @Test
    void parseTime_number1440_returnsNull() {
        assertNull(ScheduleManager.parseTime(1440));
    }

    @Test
    void parseTime_negativeNumber_returnsNull() {
        assertNull(ScheduleManager.parseTime(-1));
    }

    // ---------------------------------------------------------------- parseDays

    @Test
    void parseDays_spanishMonday() {
        assertEquals(List.of(DayOfWeek.MONDAY), ScheduleManager.parseDays("LUNES"));
    }

    @Test
    void parseDays_spanishWithAccent() {
        assertEquals(List.of(DayOfWeek.WEDNESDAY), ScheduleManager.parseDays("MIERCOLES"));
    }

    @Test
    void parseDays_spanishSaturday() {
        assertEquals(List.of(DayOfWeek.SATURDAY), ScheduleManager.parseDays("SABADO"));
    }

    @Test
    void parseDays_spanishSunday() {
        assertEquals(List.of(DayOfWeek.SUNDAY), ScheduleManager.parseDays("DOMINGO"));
    }

    @Test
    void parseDays_englishTuesday() {
        assertEquals(List.of(DayOfWeek.TUESDAY), ScheduleManager.parseDays("TUESDAY"));
    }

    @Test
    void parseDays_all() {
        List<DayOfWeek> all = ScheduleManager.parseDays("ALL");
        assertEquals(7, all.size());
    }

    @Test
    void parseDays_todos() {
        assertEquals(7, ScheduleManager.parseDays("TODOS").size());
    }

    @Test
    void parseDays_diario() {
        assertEquals(7, ScheduleManager.parseDays("DIARIO").size());
    }

    @Test
    void parseDays_unknown_returnsEmpty() {
        assertTrue(ScheduleManager.parseDays("FESTIVAL").isEmpty());
    }

    @Test
    void parseDays_null_returnsEmpty() {
        assertTrue(ScheduleManager.parseDays(null).isEmpty());
    }

    @Test
    void parseDays_caseInsensitive() {
        assertEquals(List.of(DayOfWeek.FRIDAY), ScheduleManager.parseDays("friday"));
    }
}
