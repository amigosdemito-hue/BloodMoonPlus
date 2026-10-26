package dev.bloodmoonplus.stats;

import dev.bloodmoonplus.BloodMoonPlus;
import dev.bloodmoonplus.placeholder.HistoricalStatistics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Almacén de estadísticas históricas server-wide. Thread-safe (lock) para lecturas
 * y escrituras concurrentes, y escritura atómica con statistics.yml.tmp + rename.
 * Sin datos personales (sin UUIDs de jugadores, sin IPs, sin coordenadas).
 * Carga una vez en onEnable. Flush en eventos importantes.
 *
 * Estructura (compatible con Reborn):
 *   _version: 2
 *   total_events: int
 *   total_death_events: int
 *   total_unique_deaths: int
 *   total_bosses_spawned: int
 *   total_bosses_defeated: int
 *   last_event:
 *     world, duration_seconds, death_count, unique_deaths, participants, survivors,
 *     boss_name, boss_type, ended_at (ISO-8601 UTC)
 */
public final class StatisticsStore {

    public static final String VERSION = "2";

    private final BloodMoonPlus plugin;
    private final File file;
    private final ReentrantLock lock = new ReentrantLock();
    private HistoricalStatistics data = HistoricalStatistics.empty();

    public StatisticsStore(BloodMoonPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "statistics.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getLogger().info("statistics.yml no existe, se crea con valores por defecto.");
            saveNow();
            return;
        }
        lock.lock();
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            HistoricalStatistics loaded = new HistoricalStatistics(
                    y.getLong("total_events", 0),
                    y.getLong("total_death_events", 0),
                    y.getLong("total_unique_deaths", 0),
                    y.getLong("total_bosses_spawned", 0),
                    y.getLong("total_bosses_defeated", 0),
                    parseLastEvent(y.getConfigurationSection("last_event"))
            );
            data = loaded;
        } catch (Exception e) {
            // Copia corrupta: archivar y empezar limpio
            String backupName = "statistics.corrupt-" + System.currentTimeMillis() + ".yml";
            File backup = new File(plugin.getDataFolder(), backupName);
            try {
                Files.move(file.toPath(), backup.toPath());
                plugin.getLogger().warning("statistics.yml corrupto, archivado como " + backupName);
            } catch (IOException ignored) {}
            data = HistoricalStatistics.empty();
        } finally {
            lock.unlock();
        }
    }

    private HistoricalStatistics.LastEvent parseLastEvent(org.bukkit.configuration.ConfigurationSection s) {
        if (s == null) return HistoricalStatistics.empty().lastEvent();
        return new HistoricalStatistics.LastEvent(
                s.getString("world", ""),
                s.getLong("duration_seconds", 0),
                s.getLong("death_count", 0),
                s.getLong("unique_deaths", 0),
                s.getLong("participants", 0),
                s.getLong("survivors", 0),
                s.getString("boss_name", ""),
                s.getString("boss_type", "NONE"),
                parseInstant(s.getString("ended_at"))
        );
    }

    private Instant parseInstant(String s) {
        if (s == null || s.isEmpty()) return Instant.EPOCH;
        try { return Instant.parse(s); } catch (Exception e) { return Instant.EPOCH; }
    }

    public void saveNow() {
        lock.lock();
        try {
            YamlConfiguration y = new YamlConfiguration();
            HistoricalStatistics d = data;
            y.set("_version", VERSION);
            y.set("total_events", d.totalEvents());
            y.set("total_death_events", d.totalDeathEvents());
            y.set("total_unique_deaths", d.totalUniqueDeaths());
            y.set("total_bosses_spawned", d.totalBossesSpawned());
            y.set("total_bosses_defeated", d.totalBossesDefeated());
            var last = d.lastEvent();
            y.set("last_event.world", last.world());
            y.set("last_event.duration_seconds", last.durationSeconds());
            y.set("last_event.death_count", last.deathCount());
            y.set("last_event.unique_deaths", last.uniqueDeaths());
            y.set("last_event.participants", last.participants());
            y.set("last_event.survivors", last.survivors());
            y.set("last_event.boss_name", last.bossName());
            y.set("last_event.boss_type", last.bossType());
            y.set("last_event.ended_at", last.endedAt() != null && !last.endedAt().equals(Instant.EPOCH)
                    ? last.endedAt().toString() : null);
            writeAtomic(y);
        } finally {
            lock.unlock();
        }
    }

    private void writeAtomic(YamlConfiguration y) {
        File tmp = new File(plugin.getDataFolder(), "statistics.yml.tmp");
        try {
            y.save(tmp);
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo escribir statistics.yml: " + e.getMessage());
        }
    }

    private static class FileUtils {
        static void copyDirectory(File src, File dest) throws IOException {
            if (src == null || !src.exists()) return;
            if (!dest.exists()) dest.mkdirs();
            for (File child : src.listFiles()) {
                if (child.isDirectory()) copyDirectory(child, new File(dest, child.getName()));
                else Files.copy(child.toPath(), new File(dest, child.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // ------------ API: incrementar contadores ------------

    public void recordEventCompleted(long durationSec, long deaths, long uniqueDeaths,
                                     long participants, long survivors,
                                     String bossName, String bossType) {
        lock.lock();
        try {
            data = new HistoricalStatistics(
                    data.totalEvents() + 1,
                    data.totalDeathEvents() + deaths,
                    data.totalUniqueDeaths() + uniqueDeaths,
                    data.totalBossesSpawned(),
                    data.totalBossesDefeated(),
                    new HistoricalStatistics.LastEvent(
                            /*world*/ "", durationSec, deaths, uniqueDeaths, participants, survivors,
                            bossName, bossType, Instant.now())
            );
        } finally {
            lock.unlock();
        }
        // Escritura asíncrona para no bloquear el thread principal
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveNow);
    }

    public void recordBossSpawned() {
        lock.lock();
        try {
            data = new HistoricalStatistics(
                    data.totalEvents(),
                    data.totalDeathEvents(),
                    data.totalUniqueDeaths(),
                    data.totalBossesSpawned() + 1,
                    data.totalBossesDefeated(),
                    data.lastEvent());
        } finally {
            lock.unlock();
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveNow);
    }

    public void recordBossDefeated() {
        lock.lock();
        try {
            data = new HistoricalStatistics(
                    data.totalEvents(),
                    data.totalDeathEvents(),
                    data.totalUniqueDeaths(),
                    data.totalBossesSpawned(),
                    data.totalBossesDefeated() + 1,
                    data.lastEvent());
        } finally {
            lock.unlock();
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveNow);
    }

    public HistoricalStatistics get() {
        lock.lock();
        try {
            return data;
        } finally {
            lock.unlock();
        }
    }
}
