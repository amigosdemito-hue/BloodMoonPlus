package dev.bloodmoonplus;

import dev.bloodmoonplus.config.WorldConfigRegistry;
import dev.bloodmoonplus.i18n.LocaleManager;
import dev.bloodmoonplus.integration.mythic.MythicBridge;
import dev.bloodmoonplus.placeholder.PlaceholderSnapshotService;
import dev.bloodmoonplus.stats.StatisticsStore;
import dev.bloodmoonplus.survivor.SurvivorManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class BloodMoonPlus extends JavaPlugin {
    private Messages messages;
    private LocaleManager locales;
    private WorldConfigRegistry worldConfigs;
    private PlaceholderSnapshotService snapshots;
    private StatisticsStore stats;
    private SurvivorManager survivors;
    private MythicBridge mythic;
    private DataStore data;
    private ItemFactory items;
    private LootManager loot;
    private EventManager events;
    private BossManager bosses;
    private ScheduleManager schedule;
    private ItemAbilities abilities;

    @Override
    public void onEnable() {
        Keys.init(this);
        saveDefaultConfig();
        for (String f : new String[]{"items.yml", "loot.yml", "bosses.yml"}) {
            if (!new File(getDataFolder(), f).exists()) saveResource(f, false);
        }
        locales = new LocaleManager(this);
        locales.init();
        worldConfigs = new WorldConfigRegistry(this);
        worldConfigs.init();
        stats = new StatisticsStore(this);
        stats.load();
        messages = new Messages(this, locales);
        data = new DataStore(this);
        items = new ItemFactory(this);
        loot = new LootManager(this);
        events = new EventManager(this);
        bosses = new BossManager(this);
        schedule = new ScheduleManager(this);
        abilities = new ItemAbilities(this);
        snapshots = new PlaceholderSnapshotService(this, stats);
        survivors = new SurvivorManager(this);
        mythic = new MythicBridge(this);
        reloadAll();

        // Orden de tareas: eventos y boss antes que abilities y snapshots para
        // garantizar que los placeholders lean un estado coherente.
        Bukkit.getScheduler().runTaskTimer(this, () -> { schedule.tick(); events.tick(); }, 40L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> bosses.tick(), 40L, 10L);
        Bukkit.getScheduler().runTaskTimer(this, () -> abilities.tickPassives(), 60L, 40L);
        Bukkit.getScheduler().runTaskTimer(this, () -> snapshots.publish(), 40L, 10L);
        Bukkit.getScheduler().runTaskTimer(this, () -> data.flushIfDirty(), 200L, 200L);

        BloodMoonCommand exec = new BloodMoonCommand(this);
        PluginCommand luna = getCommand("luna");
        if (luna != null) {
            luna.setExecutor(exec);
            luna.setTabCompleter(exec);
        }
        Bukkit.getPluginManager().registerEvents(new EventListener(this), this);
        Bukkit.getPluginManager().registerEvents(abilities, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PapiHook(this).register();
            getLogger().info("PlaceholderAPI enlazado (%bloodmoon_...%).");
        }

        Bukkit.getScheduler().runTaskLater(this, () -> events.resume(), 100L);
    }

    @Override
    public void onDisable() {
        if (events != null) events.shutdown();
        if (data != null) data.saveNow();
        if (stats != null) stats.saveNow();
    }

    public void reloadAll() {
        reloadConfig();
        locales.reload();
        worldConfigs.reload();
        messages.reload();
        items.reload();
        loot.reload();
        bosses.reload();
        events.reload();
        schedule.reload();
        reassign();
        getLogger().info("BloodMoonPlus recargado.");
    }

    private void reassign() {
        messages = new Messages(this, locales);
        data = new DataStore(this);
        items = new ItemFactory(this);
        loot = new LootManager(this);
        events = new EventManager(this);
        bosses = new BossManager(this);
        schedule = new ScheduleManager(this);
        abilities = new ItemAbilities(this);
        snapshots = new PlaceholderSnapshotService(this, stats);
        survivors = new SurvivorManager(this);
        mythic = new MythicBridge(this);
    }

    public Keys keys() { return Keys.instance(); }

    /** Escala una estadística con la dificultad: 1 + (dif - 1) * factor configurado en difficulty.scaling.<clave>. */
    public double scaled(double diff, String key) {
        return Math.max(0.05, 1 + (diff - 1) * getConfig().getDouble("difficulty.scaling." + key, 0));
    }

    public Messages messages() { return messages; }
    public LocaleManager locales() { return locales; }
    public WorldConfigRegistry worldConfigs() { return worldConfigs; }
    public PlaceholderSnapshotService snapshots() { return snapshots; }
    public StatisticsStore stats() { return stats; }
    public SurvivorManager survivors() { return survivors; }
    public MythicBridge mythic() { return mythic; }
    public DataStore data() { return data; }
    public ItemFactory items() { return items; }
    public LootManager loot() { return loot; }
    public EventManager events() { return events; }
    public ItemAbilities abilities() { return abilities; }
    public BossManager bosses() { return bosses; }
    public ScheduleManager schedule() { return schedule; }
}
