package dev.bloodmoonplus;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Puesta al día EN SITIO de config.yml: los servidores que ya tienen el archivo reciben las
 * claves nuevas ({@code cleanup}, {@code items.migrate-on-join}, {@code discord}…) sin que el
 * plugin sobrescriba nada de lo que el admin ya ajustó.
 *
 * <p>Funciona como la puesta al día de items.yml: al arrancar se comparan las claves del
 * config.yml del JAR con las del servidor y se AÑADEN las que faltan al final de su sección,
 * con sus comentarios. Los valores existentes, los comentarios del admin, el orden y el resto
 * del archivo no se tocan. Antes de escribir queda un {@code config.yml.pre-vN.bak}.
 *
 * <p>El resultado se valida antes de guardarlo: si algún valor del servidor cambiara o el YAML
 * quedara inválido, no se escribe nada y se avisa en consola.
 *
 * <p>{@code config-version: N} marca la versión de claves del archivo. Mientras sea igual o mayor
 * que la de esta build no se vuelve a tocar, así que puedes editar el archivo a mano sin que las
 * actualizaciones lo pisen; súbelo (o bórralo) para forzar otra pasada.
 */
final class ConfigUpdater {

    /** Versión del juego de claves de config.yml que trae esta build. Súbela cuando añadas
     *  claves nuevas al config.yml del JAR. */
    static final int CONFIG_CONTENT_VERSION = 2;

    /** Tope de inserciones por arranque: una config absurda no puede colgar el arranque. */
    private static final int MAX_INSERTIONS = 500;

    /** Resultado de la fusión: el texto nuevo y las claves añadidas. {@code text == null} =
     *  no se puede/né debe escribir (YAML roto o validación fallida). */
    record Result(String text, List<String> added, int version) {}

    private ConfigUpdater() {}

    /** Punto de entrada desde onEnable(): antes de crear los managers, para que todo lo que lea
     *  la config (schedule, horde, discord, cleanup…) vea ya las claves nuevas. */
    static void run(BloodMoonPlus plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.isFile()) return; // primer arranque: saveDefaultConfig ya copió el nuevo
        String bundled;
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return;
            bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo leer el config.yml del JAR: " + ex.getMessage());
            return;
        }
        String current;
        try {
            current = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo leer config.yml: " + ex.getMessage());
            return;
        }
        Result result = merge(current, bundled);
        if (result == null) {
            plugin.getLogger().warning("config.yml no es YAML válido o la puesta al día no se pudo"
                    + " validar: no se modifica el archivo (revisa el archivo o bórralo para que se"
                    + " regenere con los valores por defecto).");
            return;
        }
        if (result.added().isEmpty()) return;
        File backup = new File(plugin.getDataFolder(), "config.yml.pre-v" + result.version() + ".bak");
        if (!backup.exists()) {
            try {
                Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().warning("No hay copia de seguridad de config.yml ("
                        + ex.getMessage() + "): no se modifica el archivo.");
                return;
            }
        }
        try {
            Files.writeString(file.toPath(), result.text(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar config.yml: " + ex.getMessage());
            return;
        }
        plugin.getLogger().info("config.yml: " + result.added().size() + " clave(s) nueva(s)"
                + " añadida(s) (contenido v" + result.version() + "): "
                + String.join(", ", result.added()) + ". Copia anterior: " + backup.getName()
                + ". Tus valores y comentarios siguen intactos.");
    }

    // ------------------------------------------------------------------ fusión

    /** Fusión pura de texto a texto, para poder probarla sin disco. Devuelve null si el archivo
     *  del servidor no es YAML válido o si el resultado no valida. */
    static Result merge(String userText, String bundledText) {
        String eol = userText.contains("\r\n") ? "\r\n" : "\n";
        List<String> user = splitLines(userText);
        List<String> bundled = splitLines(bundledText);

        int bundledVersion = Math.max(CONFIG_CONTENT_VERSION, versionOf(bundled));
        int userVersion = versionOf(user);
        if (userVersion >= bundledVersion) return new Result(userText, List.of(), userVersion);

        Map<String, Node> theirs = parse(bundled);
        List<String> added = new ArrayList<>();
        List<String> lines = new ArrayList<>(user);
        try {
            for (int guard = 0; guard < MAX_INSERTIONS; guard++) {
                Map<String, Node> mine = parse(lines);
                String target = firstMissing(theirs, mine, added);
                if (target == null) break;
                List<String> block = blockOf(bundled, theirs.get(target));
                List<String> placed = place(lines, mine, target, block);
                if (placed == null) {
                    // No se encontró dónde encajarla: mejor no tocar nada que dejarla fuera de
                    // sitio (el aviso queda en consola y el archivo sigue como estaba).
                    return null;
                }
                lines = placed;
                added.add(target);
            }
        } catch (RuntimeException ex) {
            return null;
        }
        if (added.isEmpty()) return new Result(userText, List.of(), userVersion);

        lines = withVersion(lines, bundledVersion);
        String merged = String.join(eol, lines);
        if (userText.endsWith("\n")) merged = merged + eol;
        if (!valid(userText, merged, theirs)) return null;
        return new Result(merged, added, bundledVersion);
    }

    /** Una clave del YAML: línea, indentación y si tiene hijos. */
    private record Node(int line, int indent, boolean section) {}

    /**
     * Claves por ruta ({@code horde.require-armor.enabled}). Se ignoran comentarios, líneas en
     * blanco y todo lo que cuelga de una lista ({@code - type: ZOMBIE}): las listas se comparan
     * como una sola clave, así que un admin con menos oleadas o menos horarios no recibe avisos
     * de claves que "faltan".
     */
    private static Map<String, Node> parse(List<String> lines) {
        Map<String, Node> out = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        List<Integer> indents = new ArrayList<>();
        int listIndent = -1;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int ind = indentOf(raw);
            if (t.startsWith("-")) {
                listIndent = ind;
                continue;
            }
            if (listIndent >= 0) {
                if (ind > listIndent) continue; // colgado de la lista
                listIndent = -1;
            }
            int colon = t.indexOf(':');
            if (colon <= 0) continue;
            String key = t.substring(0, colon).trim();
            if (key.isEmpty() || key.indexOf(' ') >= 0) continue;
            // La marca de versión la pone withVersion(), no es una clave que se "añada".
            if (key.equals("config-version")) continue;
            String rest = t.substring(colon + 1).trim();
            while (!indents.isEmpty() && indents.get(indents.size() - 1) >= ind) {
                indents.remove(indents.size() - 1);
                names.remove(names.size() - 1);
            }
            StringBuilder path = new StringBuilder();
            for (String n : names) path.append(n).append('.');
            path.append(key);
            boolean section = rest.isEmpty() || rest.startsWith("#");
            out.putIfAbsent(path.toString(), new Node(i, ind, section));
            if (section) {
                names.add(key);
                indents.add(ind);
            }
        }
        return out;
    }

    /** Primera clave del JAR que no está en el servidor (en el orden del archivo). Si falta una
     *  sección entera, se devuelve la sección y sus hijos vienen con ella. */
    private static String firstMissing(Map<String, Node> theirs, Map<String, Node> mine, List<String> added) {
        for (String path : theirs.keySet()) {
            if (mine.containsKey(path)) continue;
            boolean covered = false;
            for (String done : added) {
                if (path.startsWith(done + ".")) { covered = true; break; }
            }
            if (!covered) return path;
        }
        return null;
    }

    /** Texto del JAR para esa clave: comentarios de cabecera contiguos + sus líneas, sin la
     *  línea en blanco que cierra el bloque. */
    private static List<String> blockOf(List<String> bundled, Node node) {
        int start = node.line();
        while (start > 0) {
            String prev = bundled.get(start - 1);
            if (prev.trim().startsWith("#") && indentOf(prev) == node.indent()) start--;
            else break;
        }
        int end = node.line() + 1;
        while (end < bundled.size()) {
            String next = bundled.get(end);
            if (next.isBlank()) {
                // Una línea en blanco interior solo se queda si el bloque sigue después.
                if (end + 1 < bundled.size() && !bundled.get(end + 1).isBlank()
                        && indentOf(bundled.get(end + 1)) > node.indent()) {
                    end++;
                    continue;
                }
                break;
            }
            if (indentOf(next) <= node.indent()) break;
            end++;
        }
        return new ArrayList<>(bundled.subList(start, end));
    }

    /** Coloca el bloque en el archivo del servidor: al final de su sección (o al final del
     *  archivo si es una clave de primer nivel), con la indentación que ya usa esa sección. */
    private static List<String> place(List<String> lines, Map<String, Node> mine, String path, List<String> block) {
        int dot = path.lastIndexOf('.');
        String parent = dot < 0 ? "" : path.substring(0, dot);
        List<String> out = new ArrayList<>(lines);
        if (parent.isEmpty()) {
            if (!out.isEmpty() && !out.get(out.size() - 1).isBlank()) out.add("");
            out.addAll(reindent(block, -blockIndent(block)));
            return out;
        }
        Node p = mine.get(parent);
        if (p == null) return null;
        int targetIndent = firstChildIndent(lines, p);
        int end = blockEnd(lines, p);
        if (end < 0) return null;
        out.addAll(end, reindent(block, targetIndent - blockIndent(block)));
        return out;
    }

    /** Indentación de las líneas del bloque (la de su primera línea no comentada). */
    private static int blockIndent(List<String> block) {
        for (String line : block) {
            if (!line.isBlank() && !line.trim().startsWith("#")) return indentOf(line);
        }
        return 0;
    }

    /** Indentación de los hijos de una sección (la primera línea con más sangría). */
    private static int firstChildIndent(List<String> lines, Node parent) {
        for (int i = parent.line() + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int ind = indentOf(line);
            if (ind <= parent.indent()) break;
            return ind;
        }
        return parent.indent() + 2;
    }

    /** Índice donde termina el bloque de una sección (justo después de su última línea con
     *  contenido). */
    private static int blockEnd(List<String> lines, Node parent) {
        int end = parent.line() + 1;
        for (int i = parent.line() + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            if (indentOf(line) <= parent.indent()) break;
            end = i + 1;
        }
        return end;
    }

    private static List<String> reindent(List<String> block, int delta) {
        if (delta == 0) return new ArrayList<>(block);
        List<String> out = new ArrayList<>(block.size());
        for (String line : block) {
            if (line.isBlank()) {
                out.add("");
                continue;
            }
            int spaces = indentOf(line);
            out.add(" ".repeat(Math.max(0, spaces + delta)) + line.substring(spaces));
        }
        return out;
    }

    /** ¿El resultado conserva cada valor del servidor y contiene todas las claves del JAR? */
    private static boolean valid(String userText, String mergedText, Map<String, Node> bundled) {
        YamlConfiguration before = load(userText);
        YamlConfiguration after = load(mergedText);
        if (before == null || after == null) return false;
        for (String path : before.getKeys(true)) {
            if (path.equals("config-version") || before.isConfigurationSection(path)) continue;
            if (!Objects.equals(before.get(path), after.get(path))) return false;
        }
        for (String path : bundled.keySet()) {
            if (!after.contains(path)) return false;
        }
        return true;
    }

    private static YamlConfiguration load(String text) {
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(text);
            return y;
        } catch (InvalidConfigurationException | IllegalArgumentException ex) {
            return null;
        }
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

    private static List<String> withVersion(List<String> lines, int version) {
        String marker = "config-version: " + version;
        for (int i = 0; i < lines.size(); i++) {
            if (indentOf(lines.get(i)) == 0 && lines.get(i).trim().startsWith("config-version:")) {
                lines.set(i, marker);
                return lines;
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            lines.add(i, marker);
            return lines;
        }
        lines.add(marker);
        return lines;
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
            if (text.charAt(i) != '\n') continue;
            int end = i > start && text.charAt(i - 1) == '\r' ? i - 1 : i;
            out.add(text.substring(start, end));
            start = i + 1;
        }
        if (start < text.length()) out.add(text.substring(start));
        return out;
    }
}
