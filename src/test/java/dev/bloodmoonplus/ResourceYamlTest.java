package dev.bloodmoonplus;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valida los recursos de loot:
 *  - items.yml y loot.yml deben ser YAML válido y tener contenido.
 *  - Todo item referenciado en loot.yml debe existir en items.yml.
 *  - Ningún encantamiento puede superar su máximo: los vanilla, el suyo (filo 5, irrompible 3...
 *    o 5 en legendarios y míticos) y los de ExcellentEnchants, el que declaramos aquí.
 *  - Los items ya no llevan las viejas claves `abilities`/`passives`.
 */
class ResourceYamlTest {

    /** Máximos vanilla (los mismos que devuelve Enchantment#getMaxLevel()). */
    private static final Map<String, Integer> VANILLA_MAX = Map.ofEntries(
            Map.entry("sharpness", 5), Map.entry("smite", 5), Map.entry("bane_of_arthropods", 5),
            Map.entry("knockback", 2), Map.entry("fire_aspect", 2), Map.entry("looting", 3),
            Map.entry("sweeping_edge", 3), Map.entry("unbreaking", 3), Map.entry("mending", 1),
            Map.entry("vanishing_curse", 1), Map.entry("protection", 4), Map.entry("fire_protection", 4),
            Map.entry("blast_protection", 4), Map.entry("projectile_protection", 4), Map.entry("thorns", 3),
            Map.entry("respiration", 3), Map.entry("aqua_affinity", 1), Map.entry("feather_falling", 4),
            Map.entry("depth_strider", 3), Map.entry("frost_walker", 2), Map.entry("soul_speed", 3),
            Map.entry("swift_sneak", 3), Map.entry("power", 5), Map.entry("punch", 2),
            Map.entry("flame", 1), Map.entry("infinity", 1), Map.entry("multishot", 1),
            Map.entry("quick_charge", 3), Map.entry("piercing", 4), Map.entry("efficiency", 5),
            Map.entry("fortune", 3), Map.entry("silk_touch", 1), Map.entry("density", 5),
            Map.entry("breach", 4), Map.entry("wind_burst", 3), Map.entry("impaling", 5),
            Map.entry("loyalty", 3), Map.entry("channeling", 1), Map.entry("riptide", 3));

    /** Encantamientos de ExcellentEnchants usados y el nivel máximo que declaramos para cada
     *  uno. Si un id se escribe mal aquí, el test falla y no se llega al servidor. */
    private static final Map<String, Integer> EE_MAX = Map.ofEntries(
            Map.entry("vampire", 3), Map.entry("venom", 2), Map.entry("wither", 2),
            Map.entry("ice_aspect", 3), Map.entry("double_strike", 2), Map.entry("paralyze", 5),
            Map.entry("thunder", 5), Map.entry("sniper", 2), Map.entry("electrified_arrows", 3),
            Map.entry("vampiric_arrows", 3), Map.entry("haste", 3), Map.entry("smelter", 5),
            Map.entry("cutter", 3), Map.entry("decapitator", 2), Map.entry("hardened", 2),
            Map.entry("dragon_heart", 5), Map.entry("regrowth", 4), Map.entry("speed", 2),
            Map.entry("jumping", 2), Map.entry("stopping_force", 3), Map.entry("fire_shield", 4),
            Map.entry("night_vision", 1), Map.entry("lightweight", 1), Map.entry("infernus", 3),
            Map.entry("tunnel", 3), Map.entry("veinminer", 3), Map.entry("telekinesis", 1),
            Map.entry("blast_mining", 5), Map.entry("replanter", 1), Map.entry("treefeller", 1),
            Map.entry("explosive_arrows", 3), Map.entry("poisoned_arrows", 3),
            Map.entry("dragonfire_arrows", 3), Map.entry("elemental_protection", 4),
            Map.entry("saturation", 2), Map.entry("water_breathing", 1), Map.entry("cold_steel", 3),
            Map.entry("flame_walker", 2), Map.entry("rebound", 1), Map.entry("soulbound", 1),
            Map.entry("restore", 3), Map.entry("rocket", 2), Map.entry("nimble", 1));

    /** Rarezas que suben los encantamientos vanilla hasta el nivel 5 (protección 5, espinas 5...). */
    private static boolean highRarity(String rarity) {
        String k = java.text.Normalizer.normalize(rarity, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toUpperCase(java.util.Locale.ROOT);
        return k.equals("LEGENDARIO") || k.equals("MITICO");
    }

    private static File res(String name) {
        return new File("src/main/resources", name);
    }

    @Test
    void itemsYamlLoadsAndHasContent() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(res("items.yml"));
        assertNotNull(y.getConfigurationSection("items"), "items.yml: falta la sección 'items'");
        assertFalse(y.getConfigurationSection("items").getKeys(false).isEmpty(), "items.yml: sin items");
        assertNull(y.get("settings.max-enchant-level"),
                "items.yml: el límite de nivel 7 ya no existe; cada encantamiento usa su máximo");
    }

    @Test
    void itemsHaveNoLegacyAbilityKeys() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(res("items.yml"));
        List<String> legacy = new ArrayList<>();
        for (String id : items.getConfigurationSection("items").getKeys(false)) {
            if (items.contains("items." + id + ".abilities")) legacy.add(id + "/abilities");
            if (items.contains("items." + id + ".passives")) legacy.add(id + "/passives");
        }
        assertTrue(legacy.isEmpty(), "Items con habilidades propias (ya no existen): " + legacy);
    }

    /** Ningún item puede llevar maldiciones: la Maldición de desaparición automática de la 1.0.2
     *  se retiró y el código ya no lee `settings.vanishing-curse-min-rarity`. */
    @Test
    void noItemCarriesCurses() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(res("items.yml"));
        List<String> cursed = new ArrayList<>();
        for (String id : items.getConfigurationSection("items").getKeys(false)) {
            var ench = items.getConfigurationSection("items." + id + ".enchants");
            if (ench == null) continue;
            for (String key : ench.getKeys(false)) {
                String k = key.trim().toLowerCase(java.util.Locale.ROOT).replace("minecraft:", "");
                if (k.equals("vanishing_curse") || k.equals("binding_curse")) cursed.add(id + "/" + key);
            }
        }
        assertTrue(cursed.isEmpty(), "Items con maldiciones (ya no se usan): " + cursed);
        assertNull(items.get("settings.vanishing-curse-min-rarity"),
                "items.yml: la Maldición de desaparición ya no existe; borra settings.vanishing-curse-min-rarity");
    }

    /** Cada clase de arma, herramienta y armadura (espada, hacha, pico, pala, azada, arco,
     *  ballesta, tridente, maza, casco, peto, grebas, botas, escudo) tiene una versión de CADA
     *  rareza. La clase se deduce del sufijo del material (DIAMOND_SWORD -> SWORD). */
    @Test
    void everyEquipmentClassHasAllRarities() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(res("items.yml"));
        List<String> classes = List.of("SWORD", "AXE", "PICKAXE", "SHOVEL", "HOE", "BOW",
                "CROSSBOW", "TRIDENT", "MACE", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "SHIELD");
        List<String> rarities = List.of("COMUN", "POCO_COMUN", "RARO", "EPICO", "LEGENDARIO", "MITICO");
        Map<String, Set<String>> found = new java.util.HashMap<>();
        for (String id : items.getConfigurationSection("items").getKeys(false)) {
            String material = items.getString("items." + id + ".material", "");
            for (String cls : classes) {
                if (!material.equals(cls) && !material.endsWith("_" + cls)) continue;
                String rarity = java.text.Normalizer.normalize(
                                items.getString("items." + id + ".rarity", "COMUN"),
                                java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").trim().toUpperCase(java.util.Locale.ROOT);
                found.computeIfAbsent(cls, k -> new HashSet<>()).add(rarity);
            }
        }
        List<String> missing = new ArrayList<>();
        for (String cls : classes) {
            Set<String> have = found.getOrDefault(cls, Set.of());
            for (String r : rarities) if (!have.contains(r)) missing.add(cls + " sin " + r);
        }
        assertTrue(missing.isEmpty(), "Clases sin una versión de cada rareza: " + missing);
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
    void everyItemUsesAKnownRarity() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(res("items.yml"));
        YamlConfiguration loot = YamlConfiguration.loadConfiguration(res("loot.yml"));
        Set<String> known = new HashSet<>(loot.getConfigurationSection("rarities").getKeys(false));
        List<String> unknown = new ArrayList<>();
        for (String id : items.getConfigurationSection("items").getKeys(false)) {
            String rarity = items.getString("items." + id + ".rarity", "COMUN");
            // LootManager normaliza las rarezas con Util.key (mayúsculas, sin acentos)
            String key = java.text.Normalizer.normalize(rarity, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").trim().toUpperCase(java.util.Locale.ROOT);
            if (!known.contains(key)) unknown.add(id + " -> " + rarity);
        }
        assertTrue(unknown.isEmpty(), "Items con rareza no definida en loot.yml (rarities): " + unknown);
    }

    @Test
    void noEnchantmentExceedsItsRealMaxLevel() {
        YamlConfiguration items = YamlConfiguration.loadConfiguration(res("items.yml"));
        List<String> over = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (String id : items.getConfigurationSection("items").getKeys(false)) {
            var enchSec = items.getConfigurationSection("items." + id + ".enchants");
            if (enchSec == null) continue;
            boolean high = highRarity(items.getString("items." + id + ".rarity", "COMUN"));
            for (String ench : enchSec.getKeys(false)) {
                String key = ench.trim().toLowerCase(java.util.Locale.ROOT).replace("minecraft:", "");
                int lvl = items.getInt("items." + id + ".enchants." + ench);
                Integer max = VANILLA_MAX.get(key);
                if (max == null) {
                    max = EE_MAX.get(key.replace("excellentenchants:", ""));
                    if (max == null) unknown.add(id + "/" + ench);
                } else if (high) {
                    // Los legendarios y míticos suben el tope vanilla a 5 (protección 5).
                    max = Math.max(max, 5);
                }
                if (max != null && lvl > max) over.add(id + "/" + ench + "=" + lvl + " (máx " + max + ")");
            }
        }
        assertTrue(over.isEmpty(), "Encantamientos por encima de su máximo: " + over);
        assertTrue(unknown.isEmpty(), "Encantamientos desconocidos en items.yml (¿id mal escrito?): " + unknown);
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

    /**
     * Valida que el roster de jefes sea coherente con la programación:
     *  - Todo jefe listado en spawns debe existir en bosses.
     *  - Todo jefe definido en bosses debe aparecer en spawns (ningún jefe "huérfano").
     */
    @Test
    void bossesAndSpawnsAreConsistent() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(res("bosses.yml"));
        var bosses = y.getConfigurationSection("bosses");
        assertNotNull(bosses, "bosses.yml: falta la sección 'bosses'");
        Set<String> ids = new HashSet<>(bosses.getKeys(false));
        assertFalse(ids.isEmpty(), "bosses.yml: no hay jefes definidos");

        List<Map<?, ?>> spawns = y.getMapList("spawns");
        assertFalse(spawns.isEmpty(), "bosses.yml: falta la lista 'spawns'");

        Set<String> spawnIds = new HashSet<>();
        for (Map<?, ?> s : spawns) {
            Object boss = s.get("boss");
            assertNotNull(boss, "Cada entrada de spawns debe tener el campo 'boss'");
            String id = String.valueOf(boss);
            assertTrue(ids.contains(id), "spawns referencia un jefe inexistente: " + id);
            spawnIds.add(id);
        }

        Set<String> orphans = new HashSet<>(ids);
        orphans.removeAll(spawnIds);
        assertTrue(orphans.isEmpty(), "Jefes que nunca aparecen (no están en spawns): " + orphans);
    }
}
