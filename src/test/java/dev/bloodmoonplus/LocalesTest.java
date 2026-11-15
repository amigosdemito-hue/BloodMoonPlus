package dev.bloodmoonplus;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Garantiza que los 7 idiomas empaquetados estén completos y no se desincronicen:
 *  - existen todos los archivos,
 *  - cada uno tiene exactamente las mismas claves que el español,
 *  - cada clave usa los mismos {placeholders} que el español (una traducción que pierde
 *    {world} o {count} rompería el mensaje en ese idioma).
 */
class LocalesTest {

    private static final List<String> LANGS = List.of("es", "en", "pt", "fr", "de", "it", "ru");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_-]+)}");

    private static File file(String lang) {
        return new File("src/main/resources/locales/" + lang + ".yml");
    }

    private static Set<String> keys(YamlConfiguration y) {
        Set<String> out = new LinkedHashSet<>();
        for (String k : y.getKeys(true)) {
            if (y.isConfigurationSection(k)) continue;
            if (k.equals("LocalesVersion") || k.equals("LanguageName")) continue;
            out.add(k);
        }
        return out;
    }

    private static Set<String> placeholders(String text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Test
    void allLocalesExistAndLoad() {
        List<String> broken = new ArrayList<>();
        for (String lang : LANGS) {
            File f = file(lang);
            if (!f.isFile()) { broken.add(lang + " (falta el archivo)"); continue; }
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            if (y.getString("prefix") == null) broken.add(lang + " (sin 'prefix')");
            if (y.getString("LanguageName") == null) broken.add(lang + " (sin 'LanguageName')");
        }
        assertTrue(broken.isEmpty(), "Idiomas incompletos: " + broken);
    }

    @Test
    void everyLocaleHasTheSameKeysAsSpanish() {
        YamlConfiguration es = YamlConfiguration.loadConfiguration(file("es"));
        Set<String> expected = keys(es);
        assertFalse(expected.isEmpty(), "es.yml no tiene claves");

        List<String> problems = new ArrayList<>();
        for (String lang : LANGS) {
            if (lang.equals("es")) continue;
            Set<String> actual = keys(YamlConfiguration.loadConfiguration(file(lang)));
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(actual);
            Set<String> extra = new LinkedHashSet<>(actual);
            extra.removeAll(expected);
            if (!missing.isEmpty()) problems.add(lang + " le faltan " + missing);
            if (!extra.isEmpty()) problems.add(lang + " tiene claves de más " + extra);
        }
        assertTrue(problems.isEmpty(), "Claves desincronizadas: " + problems);
    }

    @Test
    void everyLocaleKeepsTheSamePlaceholders() {
        YamlConfiguration es = YamlConfiguration.loadConfiguration(file("es"));
        List<String> problems = new ArrayList<>();
        for (String lang : LANGS) {
            if (lang.equals("es")) continue;
            YamlConfiguration other = YamlConfiguration.loadConfiguration(file(lang));
            for (String key : keys(es)) {
                Set<String> want = placeholders(String.valueOf(es.getString(key)));
                Set<String> have = placeholders(String.valueOf(other.getString(key)));
                if (!want.equals(have)) {
                    problems.add(lang + "." + key + " esperaba " + want + " y tiene " + have);
                }
            }
        }
        assertTrue(problems.isEmpty(), "Placeholders rotos en traducciones: " + problems);
    }

    @Test
    void localeManagerListsAllPackagedLanguages() {
        for (String lang : LANGS) {
            assertTrue(dev.bloodmoonplus.i18n.LocaleManager.supported().contains(lang),
                    "LocaleManager no incluye '" + lang + "' en SUPPORTED");
        }
        assertEquals(LANGS.size(), dev.bloodmoonplus.i18n.LocaleManager.supported().size(),
                "SUPPORTED debe tener exactamente los idiomas empaquetados");
    }
}
