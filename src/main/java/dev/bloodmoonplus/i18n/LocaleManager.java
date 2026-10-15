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

    public static final String VERSION = "2.0.0";

    private final BloodMoonPlus plugin;
    private final File localesDir;
    private YamlConfiguration active = new YamlConfiguration();
    private YamlConfiguration fallback = new YamlConfiguration();
    private String language = "en";
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
        language = cfg.getString("Language", "en").toLowerCase(Locale.ROOT);
        useBundled = cfg.getBoolean("UseBundledLocales", true);
        active = YamlConfiguration.loadConfiguration(new File(localesDir, language + ".yml"));
        fallback = YamlConfiguration.loadConfiguration(new File(localesDir, "en.yml"));
        plugin.getLogger().info("Idioma activo: " + language);
    }

    /** Obtiene la cadena localizada con fallback a inglés. */
    public String get(String key) {
        if (useBundled) {
            String v = active.getString(key);
            if (v == null) v = fallback.getString(key);
            if (v != null) return v;
        } else {
            String v = active.getString(key);
            if (v != null) return v;
        }
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
