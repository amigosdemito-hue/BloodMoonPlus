package dev.bloodmoonplus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/** Persistencia en data.yml: slots ya disparados, pity por tabla/rareza y evento activo.
 *  Usa un lock simple para serializar lecturas/escrituras del YAML en memoria y del archivo.
 *  El hilo principal es el único que escribe en la mayoría de casos, pero los placeholders
 *  (que pueden llegar desde un hilo async) leen el archivo, así que protegemos ambas operaciones.
 */
public final class DataStore {
    private final BloodMoonPlus plugin;
    private final File file;
    private YamlConfiguration cfg;
    private final ReentrantLock lock = new ReentrantLock();
    // volatile: flushIfDirty() lo lee fuera del lock y el hilo que escribe puede ser otro.
    private volatile boolean dirty;

    DataStore(BloodMoonPlus plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    void saveNow() {
        lock.lock();
        try {
            try {
                cfg.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo guardar data.yml: " + e.getMessage());
            }
            dirty = false;
        } finally {
            lock.unlock();
        }
    }

    void flushIfDirty() {
        if (dirty) saveNow();
    }

    boolean isFired(String key) {
        lock.lock();
        try {
            return cfg.getStringList("fired").contains(key);
        } finally {
            lock.unlock();
        }
    }

    void markFired(String key) {
        lock.lock();
        try {
            List<String> l = new ArrayList<>(cfg.getStringList("fired"));
            l.add(key);
            while (l.size() > 80) l.remove(0);
            cfg.set("fired", l);
            dirty = true;
        } finally {
            lock.unlock();
        }
    }

    /** Pity acumulado de una rareza en una tabla concreta. La tabla forma parte de la clave: la
     *  mala suerte matando mobs de la horda ya no infla las tiradas de los jefes. */
    int pity(UUID u, String table, String rarity) {
        lock.lock();
        try {
            return cfg.getInt("pity." + u + "." + table + "." + rarity, 0);
        } finally {
            lock.unlock();
        }
    }

    void setPity(UUID u, String table, String rarity, int v) {
        lock.lock();
        try {
            cfg.set("pity." + u + "." + table + "." + rarity, v <= 0 ? null : v);
            dirty = true;
        } finally {
            lock.unlock();
        }
    }

    /** Mayor pity de una rareza entre todas las tablas (para el placeholder %bloodmoon_pity_x%). */
    int pityBest(UUID u, String rarity) {
        lock.lock();
        try {
            ConfigurationSection root = cfg.getConfigurationSection("pity." + u);
            if (root == null) return 0;
            int best = 0;
            for (String table : root.getKeys(false)) {
                ConfigurationSection t = root.getConfigurationSection(table);
                if (t == null) continue; // claves antiguas pity.<uuid>.<rareza> (formato 1.0.3)
                best = Math.max(best, t.getInt(rarity, 0));
            }
            return best;
        } finally {
            lock.unlock();
        }
    }

    void saveActive(String world, long start, long end, double diff) {
        lock.lock();
        try {
            String b = "active." + world;
            cfg.set(b + ".start", start);
            cfg.set(b + ".end", end);
            cfg.set(b + ".difficulty", diff);
            dirty = true;
        } finally {
            lock.unlock();
        }
    }

    void clearActive(String world) {
        lock.lock();
        try {
            cfg.set("active." + world, null);
            dirty = true;
        } finally {
            lock.unlock();
        }
    }

    ConfigurationSection active() {
        lock.lock();
        try {
            return cfg.getConfigurationSection("active");
        } finally {
            lock.unlock();
        }
    }

    Long getLastIntervalFire() {
        lock.lock();
        try {
            return cfg.getLong("interval.lastFire");
        } finally {
            lock.unlock();
        }
    }

    void setLastIntervalFire(long ms) {
        lock.lock();
        try {
            cfg.set("interval.lastFire", ms);
            dirty = true;
        } finally {
            lock.unlock();
        }
    }
}
