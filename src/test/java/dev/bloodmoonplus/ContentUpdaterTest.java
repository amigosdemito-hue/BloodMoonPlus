package dev.bloodmoonplus;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La puesta al día del lore de items.yml no puede borrar el archivo del servidor: solo cambia
 * los bloques `lore:` de los ítems que existen en el catálogo del JAR. Estas pruebas fijan esa
 * regla (y que un archivo ya puesto al día no se vuelva a tocar).
 */
class ContentUpdaterTest {

    private static String oldFile() {
        return String.join("\n",
                "# cabecera del admin (no se toca)",
                "# 2) UNA sola línea con los encantamientos...",
                "settings:",
                "  show-rarity-line: true",
                "  mi-ajuste: 42",
                "",
                "items:",
                "  # ---------------- ARMAS ----------------",
                "  carne: { material: COOKED_BEEF, rarity: COMUN }",
                "  espada_sanguinaria:",
                "    material: DIAMOND_SWORD",
                "    name: \"<red>Espada Sanguinaria\"",
                "    lore:",
                "      - \"<gray>Sangre de la noche, afilada.\"",
                "      - \"<aqua>Vampírico II · Veneno II · Cortador III · Marchitar II · Trueno II\"",
                "    rarity: RARO",
                "    enchants: { vampire: 2, venom: 2 }",
                "  mi_espada_propia:",
                "    material: NETHERITE_SWORD",
                "    name: \"<gold>Espada del Admin\"",
                "    lore: [\"<gray>Solo mía.\"]",
                "    rarity: LEGENDARIO",
                "    enchants: { sharpness: 5 }",
                "");
    }

    private static String bundledFile() {
        return String.join("\n",
                "# cabecera del JAR",
                "config-version: 2",
                "",
                "settings:",
                "  show-rarity-line: true",
                "  sync-lore: true",
                "",
                "items:",
                "  carne: { material: COOKED_BEEF, rarity: COMUN }",
                "  espada_sanguinaria:",
                "    material: DIAMOND_SWORD",
                "    name: \"<red>Espada Sanguinaria\"",
                "    lore:",
                "      - \"<gray>Sangre de la noche, afilada.\"",
                "      - \"<aqua>Vampírico II · Veneno II\"",
                "    rarity: RARO",
                "    enchants: { vampire: 2, venom: 2 }",
                "  arco_eclipse:",
                "    material: BOW",
                "    name: \"<gradient:#ff0040:#8a00ff><bold>Arco del Eclipse</bold></gradient>\"",
                "    lore:",
                "      - \"<gray>La noche entera en una flecha.\"",
                "    rarity: MITICO",
                "");
    }

    @Test
    void replacesOnlyTheLoreOfSharedItems() {
        ContentUpdater.Result r = ContentUpdater.merge(oldFile(), bundledFile());
        assertNotNull(r.text());
        assertEquals(1, r.updated(), "solo la espada cambia de lore");
        assertEquals(2, r.version());

        List<String> lines = List.of(r.text().split("\n"));
        // El lore viejo (ancho) ya no está y el nuevo sí.
        assertTrue(r.text().contains("\"<aqua>Vampírico II · Veneno II\""), r.text());
        assertFalse(r.text().contains("Cortador III"), "el lore ancho se reemplaza, no se duplica");
        // Nada más se mueve: comentarios, ajustes propios, encantamientos e ítems del admin.
        assertTrue(r.text().contains("# cabecera del admin (no se toca)"));
        assertTrue(r.text().contains("  mi-ajuste: 42"));
        assertTrue(r.text().contains("enchants: { vampire: 2, venom: 2 }"));
        assertTrue(r.text().contains("  mi_espada_propia:"));
        assertTrue(r.text().contains("    enchants: { sharpness: 5 }"));
        assertFalse(r.text().contains("arco_eclipse"), "los ítems que el servidor no tiene no se crean");
        // La marca de versión queda como primera clave.
        assertEquals("config-version: 2", lines.stream()
                .filter(l -> !l.startsWith("#") && !l.isBlank()).findFirst().orElse(""));
    }

    /** Varios ítems con lore de distinto tamaño en el mismo archivo: el reemplazo de uno no
     *  puede desplazar los índices del siguiente (antes reventaba con IndexOutOfBounds). */
    @Test
    void manyItemsWithDifferentLoreSizes() {
        StringBuilder user = new StringBuilder("items:\n");
        StringBuilder bundled = new StringBuilder("config-version: 2\nitems:\n");
        for (int i = 0; i < 40; i++) {
            user.append("  item_" + i + ":\n    material: STONE\n    lore:\n")
                    .append("      - \"<gray>Vieja " + i + "\"\n      - \"<gray>Ancha " + i
                            + " · A · B · C · D · E\"\n")
                    .append("    rarity: RARO\n");
            bundled.append("  item_" + i + ":\n    material: STONE\n    lore:\n")
                    .append("      - \"<gray>Nueva " + i + "\"\n")
                    .append("    rarity: RARO\n");
        }
        ContentUpdater.Result r = ContentUpdater.merge(user.toString(), bundled.toString());
        assertEquals(40, r.updated());
        assertFalse(r.text().contains("Ancha"), r.text());
        assertTrue(r.text().contains("\"<gray>Nueva 39\""));
        assertTrue(r.text().contains("  item_0:\n"));
    }

    @Test
    void upToDateFileIsLeftAlone() {
        ContentUpdater.Result first = ContentUpdater.merge(oldFile(), bundledFile());
        ContentUpdater.Result again = ContentUpdater.merge(first.text(), bundledFile());
        assertEquals(0, again.updated());
        assertEquals(first.text(), again.text());
    }

    @Test
    void syncCanBeDisabled() {
        String off = oldFile().replace("settings:", "settings:\n  sync-lore: false");
        ContentUpdater.Result r = ContentUpdater.merge(off, bundledFile());
        assertEquals(0, r.updated());
        assertEquals(off, r.text());
    }

    @Test
    void brokenYamlIsNotTouched() {
        String broken = "items:\n  malo:\n   - [sin: cerrar\n";
        ContentUpdater.Result r = ContentUpdater.merge(broken, bundledFile());
        assertEquals(0, r.updated());
        assertEquals(broken, r.text());
    }

    /** Con el catálogo real: un items.yml con la lore vieja (ancha) recupera la nueva sin perder
     *  el resto del archivo. */
    @Test
    void realCatalogRestoresNarrowLore() throws Exception {
        File resource = new File("src/main/resources/items.yml");
        String bundled = Files.readString(resource.toPath(), StandardCharsets.UTF_8);
        assertTrue(bundled.contains("config-version: " + ContentUpdater.ITEMS_CONTENT_VERSION),
                "items.yml debe declarar la versión de contenido que trae esta build");
        // Simula un servidor con la versión anterior: sin marca y con el lore ancho de 1.0.8.
        String old = bundled
                .replaceAll("(?m)^config-version: \\d+\\r?\\n", "")
                .replace("\"<aqua>Vampírico II · Veneno II\"",
                        "\"<aqua>Vampírico II · Veneno II · Cortador III · Marchitar II · Trueno II\"");
        assertTrue(old.contains("Cortador III"));

        ContentUpdater.Result r = ContentUpdater.merge(old, bundled);
        assertEquals(1, r.updated());
        assertTrue(r.text().contains("\"<aqua>Vampírico II · Veneno II\""));
        assertFalse(r.text().contains("Vampírico II · Veneno II · Cortador III"),
                "la línea ancha de la espada sanguinaria se reemplaza entera");
        assertTrue(r.text().contains("Enchants en una sola línea") || r.text().contains("LORE (leer antes de editar)"),
                "la cabecera del archivo se conserva");
        assertTrue(r.text().contains("config-version: " + ContentUpdater.ITEMS_CONTENT_VERSION));
    }
}
