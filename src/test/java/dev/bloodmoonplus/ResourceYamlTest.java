package dev.bloodmoonplus;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valida los recursos de loot:
 *  - items.yml y loot.yml deben ser YAML válido y tener contenido.
 *  - Todo item referenciado en loot.yml debe existir en items.yml.
 *  - Ningún encantamiento puede superar el límite de nivel 7.
 */
class ResourceYamlTest {

    private static final int MAX_ENCHANT = 7;

    private static File res(String name) {
        return new File("src/main/resources", name);
    }

    @Test
    void itemsYamlLoadsAndHasContent() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(res("items.yml"));
        assertNotNull(y.getConfigurationSection("items"), "items.yml: falta la sección 'items'");
        assertFalse(y.getConfigurationSection("items").getKeys(false).isEmpty(), "items.yml: sin items");
        assertEquals(7, y.getInt("settings.max-enchant-level", -1),
                "items.yml: settings.max-enchant-level debe ser 7");
    }

    @Test
    void lootYamlLoadsAndHasTables() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(res("loot.yml"));
        assertNotNull(y.getConfigurationSection("tables"), "loot.yml: falta la sección 'tables'");
        assertNotNull(y.getConfigurationSection("rarities"), "loot.yml: falta la sección 'rarities'");
        assertFalse(y.getConfigurationSection("tables").getKeys(false).isEmpty());
    }

    @Test
    void everyLootEntryReferencesAnExistingItem() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(res("items.yml"));
        YamlConfiguration loot = YamlConfiguration.loadConfiguration(res("loot.yml"));
        List<String> missing = new ArrayList<>();
        for (String tableId : loot.getConfigurationSection("tables").getKeys(false)) {
            for (String listKey : List.of("entries", "guaranteed")) {
                for (Map<?, ?> entry : loot.getMapList("tables." + tableId + "." + listKey)) {
                    Object item = entry.get("item");
                    if (item == null) continue;
                    if (!items.contains("items." + item)) missing.add(tableId + " -> " + item);
                }
            }
        }
        assertTrue(missing.isEmpty(), "Entradas de loot sin item definido: " + missing);
    }

    @Test
    void noEnchantmentExceedsMaxLevel() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(res("items.yml"));
        int max = items.getInt("settings.max-enchant-level", 7);
        List<String> over = new ArrayList<>();
        for (String id : items.getConfigurationSection("items").getKeys(false)) {
            var enchSec = items.getConfigurationSection("items." + id + ".enchants");
            if (enchSec == null) continue;
            for (String ench : enchSec.getKeys(false)) {
                int lvl = items.getInt("items." + id + ".enchants." + ench);
                if (lvl > max) over.add(id + "/" + ench + "=" + lvl);
            }
        }
        assertTrue(over.isEmpty(), "Encantamientos por encima del límite " + max + ": " + over);
        assertTrue(max <= MAX_ENCHANT, "El límite de encantamiento no debe superar 7");
    }

    @Test
    void commonDropsAlwaysFall() {
        YamlConfiguration loot = YamlConfiguration.loadConfiguration(res("loot.yml"));
        List<Map<?, ?>> entries = loot.getMapList("tables.mob_horde.entries");
        assertFalse(entries.isEmpty(), "mob_horde sin entradas");
        // Al menos los items comunes deben tener drop-chance 1.0
        long always = entries.stream().filter(e -> e.get("drop-chance") == null
                || ((Number) e.get("drop-chance")).doubleValue() >= 1.0).count();
        assertTrue(always >= 4, "Se esperaban al menos 4 drops siempre, hay " + always);
    }

    /**
     * Valida la agenda por defecto del config.yml:
     *  - Días reconocidos, horas válidas y dificultad dentro del rango configurado.
     *  - 7 eventos semanales (LUNES 1, MIÉRCOLES 2, VIERNES 2, DOMINGO 2).
     */
    @Test
    void defaultScheduleIsValid() {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(res("config.yml"));
        var days = c.getConfigurationSection("schedule.days");
        assertNotNull(days, "config.yml: falta schedule.days");
        double max = c.getDouble("difficulty.max", 10);
        double def = c.getDouble("difficulty.default", 1.0);
        int slots = 0;
        for (String key : days.getKeys(false)) {
            assertFalse(ScheduleManager.parseDays(key).isEmpty(), "Día inválido en schedule.days: " + key);
            for (Object o : days.getList(key, List.of())) {
                LocalTime t;
                double diff = def;
                if (o instanceof Map<?, ?> m) {
                    t = ScheduleManager.parseTime(m.get("time"));
                    Object d = m.get("difficulty");
                    if (d instanceof Number n) diff = n.doubleValue();
                } else {
                    t = ScheduleManager.parseTime(o);
                }
                assertNotNull(t, "Hora inválida en schedule.days." + key + ": " + o);
                assertTrue(diff >= 0.1 && diff <= max, "Dificultad fuera de rango en " + key + ": x" + diff);
                slots++;
            }
        }
        assertEquals(7, slots, "Se esperaban 7 eventos semanales por defecto");
    }
}
