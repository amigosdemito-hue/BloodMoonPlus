package dev.bloodmoonplus.config;

import dev.bloodmoonplus.BloodMoonPlus;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Registro de configuración por mundo. Carga plugins/BloodMoonPlus/<world>/config.yml.
 * Si el archivo no existe, lo crea desde el template del JAR.
 * El config.yml global contiene los valores por defecto (schedule, difficulty, horde).
 */
public final class WorldConfigRegistry {

    private final BloodMoonPlus plugin;
    private final File worldsDir;
    private final Map<String, WorldConfig> cache = new LinkedHashMap<>();

    public WorldConfigRegistry(BloodMoonPlus plugin) {
        this.plugin = plugin;
        this.worldsDir = new File(plugin.getDataFolder(), "worlds");
    }

    public void init() {
        worldsDir.mkdirs();
        // Auto-instala README y EXAMPLES como Reborn (no se sobreescriben)
        installIfMissing("README.txt", "distribution/README.txt");
        File examples = new File(plugin.getDataFolder(), "EXAMPLES");
        if (!examples.exists()) {
            examples.mkdirs();
            installIfMissing("EXAMPLES/BossRewards-en.yml", "distribution/EXAMPLES/BossRewards-en.yml");
            installIfMissing("EXAMPLES/BossRewards-es.yml", "distribution/EXAMPLES/BossRewards-es.yml");
            installIfMissing("EXAMPLES/CommandsOnStart-en.yml", "distribution/EXAMPLES/CommandsOnStart-en.yml");
            installIfMissing("EXAMPLES/CommandsOnStart-es.yml", "distribution/EXAMPLES/CommandsOnStart-es.yml");
            installIfMissing("EXAMPLES/CommandsOnEnd-en.yml", "distribution/EXAMPLES/CommandsOnEnd-en.yml");
            installIfMissing("EXAMPLES/CommandsOnEnd-es.yml", "distribution/EXAMPLES/CommandsOnEnd-es.yml");
            installIfMissing("EXAMPLES/SurvivorRewards-en.yml", "distribution/EXAMPLES/SurvivorRewards-en.yml");
            installIfMissing("EXAMPLES/SurvivorRewards-es.yml", "distribution/EXAMPLES/SurvivorRewards-es.yml");
        }
        reload();
    }

    private void installIfMissing(String target, String resource) {
        File f = new File(plugin.getDataFolder(), target);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try {
                plugin.saveResource(resource, false);
            } catch (IllegalArgumentException ex) {
                // El recurso no está en el JAR; solo lo advertimos.
                plugin.getLogger().fine("Recurso no empaquetado: " + resource + " (se omite)");
            }
        }
    }

    public void reload() {
        cache.clear();
        // Cargar todos los mundos actualmente cargados
        for (World w : Bukkit.getWorlds()) {
            loadFor(w.getName());
        }
    }

    /** Carga o crea el config.yml de un mundo. */
    public WorldConfig loadFor(String worldName) {
        File dir = new File(worldsDir, worldName);
        if (!dir.exists()) dir.mkdirs();
        File cfgFile = new File(dir, "config.yml");
        if (!cfgFile.exists()) {
            // Generar desde el template del JAR
            plugin.saveResource("worlds-template/config.yml", false);
            File template = new File(plugin.getDataFolder(), "worlds-template/config.yml");
            if (template.exists()) {
                try {
                    Files.copy(template.toPath(), cfgFile.toPath());
                } catch (IOException e) {
                    plugin.getLogger().warning("No se pudo crear config para '" + worldName + "': " + e.getMessage());
                }
            }
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(cfgFile);
        WorldConfig wc = new WorldConfig(worldName, yml);
        cache.put(worldName, wc);
        return wc;
    }

    /** Obtiene el config de un mundo. Si no existe, lo crea. */
    public WorldConfig get(String worldName) {
        WorldConfig c = cache.get(worldName);
        if (c == null) c = loadFor(worldName);
        return c;
    }

    public Collection<WorldConfig> all() { return cache.values(); }
}
