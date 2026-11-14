package dev.bloodmoonplus.i18n;

import dev.bloodmoonplus.BloodMoonPlus;
import dev.bloodmoonplus.Util;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Gestor de internacionalización. Carga en/es desde el JAR en plugins/BloodMoonPlus/locales/.
 * El usuario elige el idioma en locales.yml. Claves faltantes caen al inglés.
 */
public final class LocaleManager {

    public static final String VERSION = "1.0.2-beta";

    private final BloodMoonPlus plugin;
    private final File localesDir;
    private YamlConfiguration active = new YamlConfiguration();
    private YamlConfiguration fallback = new YamlConfiguration();
    /** Copias DENTRO del jar: red de seguridad para claves nuevas en servidores que ya tienen
     *  un locales/*.yml de una versión anterior (el plugin no sobrescribe archivos del usuario). */
    private YamlConfiguration bundled = new YamlConfiguration();
    private YamlConfiguration bundledEn = new YamlConfiguration();
    private String language = "";
    private boolean useBundled = true;

    public LocaleManager(BloodMoonPlus plugin) {
        this.plugin = plugin;
        this.localesDir = new File(plugin.getDataFolder(), "locales");
    }

    public void init() {
        localesDir.mkdirs();
        // Copia en.yml y es.yml desde el JAR solo si no existen
        for (String lang : new String[]{"en", "es"}) {
            File f = new File(localesDir, lang + ".yml");
            if (!f.exists()) plugin.saveResource("locales/" + lang + ".yml", false);
        }
        reload();
    }

    public void reload() {
        File cfgFile = new File(localesDir, "locales.yml");
        if (!cfgFile.exists()) {
            plugin.saveResource("locales/locales.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
        boolean firstLoad = active == null;
        String previous = language;
        language = cfg.getString("Language", "en").toLowerCase(Locale.ROOT);
        useBundled = cfg.getBoolean("UseBundledLocales", true);
        active = YamlConfiguration.loadConfiguration(new File(localesDir, language + ".yml"));
        fallback = YamlConfiguration.loadConfiguration(new File(localesDir, "en.yml"));
        bundled = loadBundled(language);
        bundledEn = loadBundled("en");
        // Solo se anuncia cuando cambia de verdad: reloadAll carga el locale varias veces entre
        // managers y antes el arranque escupía "Idioma activo" tres veces seguidas.
        if (firstLoad || !language.equals(previous)) {
            plugin.getLogger().info("Idioma activo: " + language);
        }
    }

    /** Carga un locale empaquetado en el jar (sin copiarlo al disco). */
    private YamlConfiguration loadBundled(String lang) {
        var in = plugin.getResource("locales/" + lang + ".yml");
        if (in == null) return new YamlConfiguration();
        try (var reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (Exception ex) {
            return new YamlConfiguration();
        }
    }

    /** Obtiene la cadena localizada: archivo del servidor -> en.yml -> jar del idioma -> jar en. */
    public String get(String key) {
        String v = active.getString(key);
        if (v == null) v = fallback.getString(key);
        if (v == null && useBundled) {
            v = bundled.getString(key);
            if (v == null) v = bundledEn.getString(key);
        }
        if (v != null) return v;
        return "<red>[i18n missing: " + key + "]</red>";
    }

    public String get(String key, String... kv) {
        return fill(get(key), kv);
    }

    /** Reemplaza {placeholders} en una cadena. Los valores pueden traer códigos legacy de
     *  Vanilla (§c en mensajes de muerte): se convierten a MiniMessage aquí. */
    public static String fill(String text, String... kv) {
        if (text == null) return "";
        for (int i = 0; i + 1 < kv.length; i += 2) {
            text = text.replace("{" + kv[i] + "}", Util.legacyToMini(kv[i + 1]));
        }
        return text;
    }

    public String language() { return language; }
}
