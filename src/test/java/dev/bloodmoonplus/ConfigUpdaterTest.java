package dev.bloodmoonplus;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La puesta al día de config.yml solo puede AÑADIR claves: los valores que el admin ya ajustó,
 * sus comentarios y sus claves propias tienen que seguir exactamente como estaban.
 */
class ConfigUpdaterTest {

    private static String oldConfig() {
        return String.join("\n",
                "# config de un servidor de la 1.0.5 (sin cleanup, sin items, sin discord)",
                "worlds:",
                "  - mundo",
                "",
                "event:",
                "  duration-minutes: 30          # mi ajuste, no se toca",
                "  global: true",
                "  mi-clave-propia: 7",
                "",
                "announce:",
                "  horde: true",
                "  mob-kills: true               # yo lo tengo encendido",
                "",
                "horde:",
                "  enabled: false",
                "  base-size: 12",
                "  mobs:",
                "    - type: ZOMBIE",
                "      weight: 1",
                "",
                "# mi sección de otro plugin",
                "otro-plugin:",
                "  algo: 1",
                "");
    }

    private static String bundledConfig() {
        return String.join("\n",
                "# cabecera del JAR",
                "config-version: 2",
                "",
                "worlds:",
                "  - world",
                "",
                "event:",
                "  duration-minutes: 20",
                "  global: true",
                "",
                "announce:",
                "  horde: true",
                "  bosses: true                  # aparición de jefes",
                "  loot: true",
                "  mob-kills: false",
                "",
                "# ------------------------------------------------------------",
                "#  LIMPIEZA DE BASURA DEL EVENTO",
                "# ------------------------------------------------------------",
                "cleanup:",
                "  enabled: true",
                "  interval-seconds: 45",
                "",
                "items:",
                "  # Puesta al día al entrar",
                "  migrate-on-join: true",
                "",
                "discord:",
                "  enabled: false",
                "  webhook-url: \"\"",
                "  notify:",
                "    warnings: true",
                "    start: true",
                "");
    }

    @Test
    void addsOnlyTheMissingKeys() {
        ConfigUpdater.Result r = ConfigUpdater.merge(oldConfig(), bundledConfig());
        assertNotNull(r.text(), "la fusión tiene que validar");
        assertEquals(2, r.version());
        assertTrue(r.added().containsAll(java.util.List.of(
                        "announce.bosses", "announce.loot", "cleanup", "items", "discord")),
                r.added().toString());

        YamlConfiguration y = load(r.text());
        // Lo del admin sigue igual.
        assertEquals(false, y.getBoolean("horde.enabled"));
        assertEquals(12, y.getInt("horde.base-size"));
        assertEquals(30, y.getInt("event.duration-minutes"));
        assertEquals(true, y.getBoolean("announce.mob-kills"));
        assertEquals(7, y.getInt("event.mi-clave-propia"));
        assertEquals(1, y.getInt("otro-plugin.algo"));
        // Lo nuevo entra con los valores del JAR.
        assertEquals(true, y.getBoolean("announce.bosses"));
        assertEquals(45, y.getInt("cleanup.interval-seconds"));
        assertEquals(true, y.getBoolean("items.migrate-on-join"));
        assertEquals(true, y.getBoolean("discord.notify.start"));
        // Y los comentarios: los del admin se quedan y los nuevos llegan con los del JAR.
        assertTrue(r.text().contains("# mi ajuste, no se toca"));
        assertTrue(r.text().contains("#  LIMPIEZA DE BASURA DEL EVENTO"));
        assertTrue(r.text().contains("# Puesta al día al entrar"));
        // Nadie duplica secciones ni claves existentes.
        assertEquals(1, count(r.text(), "\nhorde:"));
        assertEquals(1, count(r.text(), "\nannounce:"));
        assertEquals(1, count(r.text(), "\n  mi-clave-propia:"));
    }

    @Test
    void secondRunDoesNothing() {
        ConfigUpdater.Result first = ConfigUpdater.merge(oldConfig(), bundledConfig());
        ConfigUpdater.Result again = ConfigUpdater.merge(first.text(), bundledConfig());
        assertTrue(again.added().isEmpty());
        assertEquals(first.text(), again.text());
    }

    @Test
    void brokenYamlIsNotTouched() {
        String broken = "event:\n  - [sin: cerrar\n";
        assertNull(ConfigUpdater.merge(broken, bundledConfig()));
    }

    /** Con el config.yml real: quitar dos claves de distintas secciones las devuelve a su sitio,
     *  con sus comentarios, sin duplicar listas (horde.mobs) ni tocar el resto. */
    @Test
    void realConfigRestoresMissingKeys() throws Exception {
        String bundled = Files.readString(new File("src/main/resources/config.yml").toPath(),
                StandardCharsets.UTF_8);
        assertTrue(bundled.contains("config-version: " + ConfigUpdater.CONFIG_CONTENT_VERSION),
                "config.yml debe declarar la versión de claves de esta build");
        int mobsBefore = count(bundled, "- type:");

        String old = bundled
                .replaceAll("(?m)^config-version: .*\\r?\\n", "")
                .replaceAll("(?m)^\\s*migrate-on-join:.*\\r?\\n", "")
                .replaceAll("(?m)^\\s*max-projectiles-per-world:.*\\r?\\n", "");
        assertFalse(old.contains("migrate-on-join:"));

        ConfigUpdater.Result r = ConfigUpdater.merge(old, bundled);
        assertNotNull(r.text());
        assertEquals(java.util.List.of("cleanup.max-projectiles-per-world", "items.migrate-on-join"),
                r.added(), "se añaden en el orden del archivo");
        assertTrue(r.text().contains("  # Puesta al día al entrar:"), "el comentario de la clave nueva llega con ella");
        assertTrue(r.text().contains("config-version: " + ConfigUpdater.CONFIG_CONTENT_VERSION));
        assertEquals(mobsBefore, count(r.text(), "- type:"), "la lista horde.mobs no se duplica");
        assertEquals(1, count(r.text(), "max-alive-per-player:"));

        YamlConfiguration y = load(r.text());
        assertEquals(true, y.getBoolean("items.migrate-on-join"));
        assertEquals(150, y.getInt("cleanup.max-projectiles-per-world"));
        assertEquals(1000, y.getInt("horde.max-alive-total"));
    }

    private static YamlConfiguration load(String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (Exception ex) {
            fail("YAML inválido: " + ex.getMessage());
        }
        return y;
    }

    private static int count(String haystack, String needle) {
        int n = 0, i = 0;
        while ((i = haystack.indexOf(needle, i)) >= 0) {
            n++;
            i += needle.length();
        }
        return n;
    }
}
