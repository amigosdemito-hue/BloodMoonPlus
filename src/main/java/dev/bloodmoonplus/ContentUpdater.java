package dev.bloodmoonplus;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puesta al día del CONTENIDO de items.yml sin borrar el archivo del servidor.
 *
 * <p>El plugin nunca sobrescribe items.yml (podría haber encantamientos, precios de tienda o
 * ítems propios del admin). Eso dejaba a los servidores que ya lo tenían con la lore vieja,
 * ancha, para siempre: la única salida era borrar el archivo y perder sus ajustes.
 *
 * <p>Esta clase hace una fusión mínima y reversible: recorre el items.yml del servidor y
 * reemplaza ÚNICAMENTE los bloques {@code lore:} de los ítems que también existen en el
 * catálogo del JAR. Nombres, materiales, rarezas, encantamientos, ítems añadidos a mano,
 * comentarios, orden y formato del archivo se quedan como estaban.
 *
 * <p>Se marca el archivo con {@code config-version: N} (primera clave del YAML). Mientras el
 * número sea igual o mayor que el de esta build no se vuelve a tocar nada, así que un admin
 * puede editar el lore a mano y sus cambios sobreviven a las actualizaciones. Para forzar otra
 * puesta al día basta con bajar/borrar {@code config-version} (o poner
 * {@code settings.sync-lore: false} para desactivarla del todo).
 */
final class ContentUpdater {

    /** Versión del contenido de items.yml que trae esta build. Súbela cuando cambien las
     *  descripciones del catálogo para que los servidores existentes reciban el cambio. */
    static final int ITEMS_CONTENT_VERSION = 2;

    /** Resultado de una fusión: el texto nuevo, cuántos ítems cambiaron y la versión resultante.
     *  {@code text == null} significa que el archivo del servidor no es YAML válido y no se toca. */
    record Result(String text, int updated, int version) {}

    private ContentUpdater() {}

    /** Punto de entrada desde onEnable(): se ejecuta ANTES de crear los managers, así que
     *  ItemFactory ya carga el lore nuevo. */
    static void run(BloodMoonPlus plugin) {
        File file = new File(plugin.getDataFolder(), "items.yml");
        if (!file.isFile()) return; // primer arranque: lo copia saveResource con el lore nuevo
        String bundled;
        try (InputStream in = plugin.getResource("items.yml")) {
            if (in == null) return;
            bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo leer el items.yml del JAR: " + ex.getMessage());
            return;
        }
        String current;
        try {
            current = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo leer items.yml: " + ex.getMessage());
            return;
        }
        Result result = merge(current, bundled);
        if (result.text() == null) {
            plugin.getLogger().warning("items.yml no es YAML válido: no se pone al día el lore"
                    + " (revísalo con un editor o bórralo para que se regenere).");
            return;
        }
        if (result.updated() <= 0) return;
        File backup = new File(plugin.getDataFolder(), "items.yml.pre-v" + result.version() + ".bak");
        if (!backup.exists()) {
            try {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().warning("No hay copia de seguridad de items.yml ("
                        + ex.getMessage() + "): no se modifica el archivo.");
                return;
            }
        }
        try {
            Files.writeString(file.toPath(), result.text(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar items.yml: " + ex.getMessage());
            return;
        }
        plugin.getLogger().info("items.yml: lore puesto al día en " + result.updated()
                + " objeto(s) (contenido v" + result.version() + "). Copia anterior: "
                + backup.getName() + " · los objetos ya repartidos se actualizan al entrar"
                + " o con /luna refresh <jugador|all>.");
    }

    /** Fusión pura de texto a texto (sin Bukkit ni disco), para poder probarla. */
    static Result merge(String userText, String bundledText) {
        String eol = userText.contains("\r\n") ? "\r\n" : "\n";
        List<String> user = splitLines(userText);
        List<String> bundled = splitLines(bundledText);

        int bundledVersion = Math.max(ITEMS_CONTENT_VERSION, versionOf(bundled));
        int userVersion = versionOf(user);
        if (userVersion >= bundledVersion) return new Result(userText, 0, userVersion);
        if (!syncEnabled(userText)) return new Result(userText, 0, userVersion);

        Map<String, LoreBlock> mine = loreBlocks(user);
        Map<String, LoreBlock> theirs = loreBlocks(bundled);

        List<LoreBlock> edits = new ArrayList<>();
        for (Map.Entry<String, LoreBlock> e : theirs.entrySet()) {
            LoreBlock target = mine.get(e.getKey());
            if (target == null) continue; // el servidor no tiene ese ítem (o lo renombró): no se crea
            if (sameLore(target.lines(), e.getValue().lines())) continue;
            edits.add(new LoreBlock(target.start(), target.end(), target.indent(),
                    reindent(e.getValue().lines(), target.indent() - e.getValue().indent())));
        }
        // De atrás hacia delante, y cada bloque se reemplaza en el acto: así los índices de las
        // ediciones que quedan por aplicar (más arriba en el archivo) siguen siendo válidos.
        // Limpiarlos todos antes de insertar desplazaba unas ediciones sobre otras y reventaba
        // con IndexOutOfBounds en cuanto cambiaban dos ítems de distinto tamaño.
        edits.sort((a, b) -> Integer.compare(b.start(), a.start()));
        List<String> out = new ArrayList<>(user);
        for (LoreBlock edit : edits) {
            out.subList(edit.start(), edit.end()).clear();
            out.addAll(edit.start(), edit.lines());
        }

        out = withVersion(out, bundledVersion);
        String joined = String.join(eol, out);
        if (userText.endsWith("\n")) joined = joined + eol;
        return new Result(joined, edits.size(), bundledVersion);
    }

    // ------------------------------------------------------------------ YAML por líneas

    /** Un bloque `lore:` de un ítem: [start, end) en la lista de líneas, su indentación y sus
     *  líneas tal cual (con la indentación incluida). */
    private record LoreBlock(int start, int end, int indent, List<String> lines) {}

    private static Map<String, LoreBlock> loreBlocks(List<String> lines) {
        Map<String, LoreBlock> out = new LinkedHashMap<>();
        int items = topLevelKey(lines, "items:");
        if (items < 0) return out;
        int sectionEnd = lines.size();
        for (int i = items + 1; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            if (indentOf(lines.get(i)) == 0) {
                sectionEnd = i;
                break;
            }
        }
        String id = null;
        int from = items + 1;
        for (int i = items + 1; i <= sectionEnd; i++) {
            String t = i < sectionEnd ? lines.get(i).trim() : "";
            boolean idLine = i < sectionEnd && !t.isEmpty() && !t.startsWith("#")
                    && indentOf(lines.get(i)) == 2 && t.endsWith(":");
            if (i == sectionEnd || idLine) {
                if (id != null) {
                    LoreBlock block = findLore(lines, from, i);
                    if (block != null) out.put(id, block);
                }
                if (i == sectionEnd) break;
                id = t.substring(0, t.length() - 1).trim();
                from = i + 1;
            }
        }
        return out;
    }

    /** Bloque lore: dentro de la región [from, to) del ítem. */
    private static LoreBlock findLore(List<String> lines, int from, int to) {
        for (int i = from; i < to; i++) {
            String line = lines.get(i);
            if (line.isBlank() || indentOf(line) == 0) return null;
            if (!line.trim().startsWith("lore:")) continue;
            int indent = indentOf(line);
            int end = i + 1;
            while (end < to) {
                String next = lines.get(end);
                if (next.isBlank() || indentOf(next) <= indent) break;
                end++;
            }
            return new LoreBlock(i, end, indent, new ArrayList<>(lines.subList(i, end)));
        }
        return null;
    }

    /** ¿Las dos versiones del bloque dicen lo mismo? Se comparan sin indentación: una lista
     *  escrita con otro margen no es un cambio de lore. */
    private static boolean sameLore(List<String> a, List<String> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).trim().equals(b.get(i).trim())) return false;
        }
        return true;
    }

    /** Lleva las líneas del JAR a la indentación del ítem del servidor. */
    private static List<String> reindent(List<String> lines, int delta) {
        if (delta == 0) return new ArrayList<>(lines);
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            int spaces = indentOf(line);
            int wanted = Math.max(0, spaces + delta);
            out.add(" ".repeat(wanted) + line.substring(spaces));
        }
        return out;
    }

    private static int versionOf(List<String> lines) {
        for (String line : lines) {
            if (indentOf(line) != 0) continue;
            String t = line.trim();
            if (!t.startsWith("config-version:")) continue;
            try {
                return Integer.parseInt(t.substring("config-version:".length()).trim());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        return 0;
    }

    /** Escribe (o inserta) la marca de versión como primera clave del archivo. */
    private static List<String> withVersion(List<String> lines, int version) {
        String marker = "config-version: " + version;
        for (int i = 0; i < lines.size(); i++) {
            if (indentOf(lines.get(i)) == 0 && lines.get(i).trim().startsWith("config-version:")) {
                lines.set(i, marker);
                return lines;
            }
        }
        int first = topLevelKey(lines, null);
        if (first < 0) {
            lines.add(marker);
            return lines;
        }
        lines.add(first, marker);
        return lines;
    }

    /** Índice de la primera clave de nivel 0 (o de la clave pedida). -1 si no hay ninguna. */
    private static int topLevelKey(List<String> lines, String wanted) {
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            if (indentOf(lines.get(i)) != 0) continue;
            if (wanted == null || t.equals(wanted) || t.startsWith(wanted + " ")
                    || t.startsWith(wanted + "#")) return i;
        }
        return -1;
    }

    /** settings.sync-lore: false desactiva la fusión (el admin controla el lore a mano). */
    private static boolean syncEnabled(String text) {
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(text);
            return y.getBoolean("settings.sync-lore", true);
        } catch (InvalidConfigurationException | IllegalArgumentException ex) {
            return false; // YAML roto: mejor no tocarlo (run() avisa si el merge no escribe)
        }
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\n') continue;
            int end = i > start && text.charAt(i - 1) == '\r' ? i - 1 : i;
            out.add(text.substring(start, end));
            start = i + 1;
        }
        if (start < text.length()) out.add(text.substring(start));
        return out;
    }
}
