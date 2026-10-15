package dev.bloodmoonplus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistencia en data.yml: slots ya disparados, contadores de items únicos, pity y evento activo. */
public final class DataStore {
    private final BloodMoonPlus plugin;
    private final File file;
    private final YamlConfiguration cfg;
    private boolean dirty;

    DataStore(BloodMoonPlus plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    void saveNow() {
        try {
            cfg.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar data.yml: " + e.getMessage());
        }
    }

    void flushIfDirty() { if (dirty) saveNow(); }

    boolean isFired(String key) { return cfg.getStringList("fired").contains(key); }

    void markFired(String key) {
        List<String> l = new ArrayList<>(cfg.getStringList("fired"));
        l.add(key);
        while (l.size() > 80) l.remove(0);
        cfg.set("fired", l);
        dirty = true;
    }

    int created(String itemId) { return cfg.getInt("created." + itemId, 0); }

    int nextSerial(String itemId) {
        int n = created(itemId) + 1;
        cfg.set("created." + itemId, n);
        dirty = true;
        return n;
    }

    int pity(UUID u, String rarity) { return cfg.getInt("pity." + u + "." + rarity, 0); }

    void setPity(UUID u, String rarity, int v) {
        cfg.set("pity." + u + "." + rarity, v <= 0 ? null : v);
        dirty = true;
    }

    void saveActive(String world, long start, long end, double diff) {
        String b = "active." + world;
        cfg.set(b + ".start", start);
        cfg.set(b + ".end", end);
        cfg.set(b + ".difficulty", diff);
        dirty = true;
    }

    void clearActive(String world) {
        cfg.set("active." + world, null);
        dirty = true;
    }

    ConfigurationSection active() { return cfg.getConfigurationSection("active"); }

    Long getLastIntervalFire() {
        return cfg.contains("interval.lastFire") ? cfg.getLong("interval.lastFire") : null;
    }

    void setLastIntervalFire(long ms) {
        cfg.set("interval.lastFire", ms);
        dirty = true;
    }
}
