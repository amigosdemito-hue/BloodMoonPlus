package dev.bloodmoonplus;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Difficulty;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Sesiones de Luna de Sangre por mundo: inicio/fin, hordas, dificultad, barra de tiempo y recompensas. */
public final class EventManager {

    public static final class Session {
        public final World world;
        public final long startMs;
        public long endMs;
        public double difficulty;
        final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        public final Set<UUID> dead = new HashSet<>();
        /** Murió al menos una vez: descalifica de supervivientes/estadísticas, pero NO
         *  deja de recibir hordas (dead solo marca mientras está muerto y se limpia al reaparecer). */
        public final Set<UUID> died = new HashSet<>();
        public final Set<UUID> participants = new HashSet<>();
        public final Map<UUID, Integer> kills = new HashMap<>();
        final List<BossManager.Pending> pendingBosses = new ArrayList<>();
        /** Dificultad original del mundo, para restaurarla al terminar si fue forzada. */
        Difficulty originalDifficulty;
        long lastWaveMs;
        /** Número de oleadas ya generadas (escalan 5, 10, 15... hasta horde.wave-max-size). */
        int waveCount = 0;
        /** Última vez que se anunció un spawn de horda (para no spamear). */
        long lastHordeAnnounceMs = 0;
        /** Última vez que se reprodujo un trueno ambiental aleatorio. */
        long lastAmbientThunderMs = 0;
        /** Último jefe derrotado en este mundo (para el histórico de estadísticas). */
        String lastBossName = "";
        String lastBossType = "NONE";

        Session(World world, long startMs, long endMs, double difficulty) {
            this.world = world;
            this.startMs = startMs;
            this.endMs = endMs;
            this.difficulty = difficulty;
        }
    }

    record MobDef(EntityType type, double weight, double health, double damage, double speed, String name,
                  Map<EquipmentSlot, Material> equipment, double equipChance, String table) {}

    private static final int[] DY = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8};

    private final BloodMoonPlus plugin;
    /** Una sesión por mundo (clave = nombre del mundo), todas las de un mismo evento
     *  arrancan y terminan juntas: es lo que hace que el evento sea GLOBAL. */
    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final List<MobDef> mobs = new ArrayList<>();

    private boolean hordeEnabled;
    /** event.global: el evento cubre TODOS los mundos cargados, no solo los de la lista. */
    private boolean globalMode = true;
    private boolean skipEmpty = true;
    private double waveInterval, minDist, maxDist, firstWaveDelay;
    /** Tope de la progresión de oleadas (1ª=5, 2ª=10, 3ª=15... hasta este valor). */
    private int waveMaxSize = 30;
    private int baseSize, maxPerPlayer;
    private boolean reqArmor, lockNight, clearWeather, removeOnEnd, rewardOnStop;
    private int reqPieces;
    private List<String> reqMaterials = List.of();
    private long nightTime, endTime;
    private double maxMult;
    private List<String> cmdStart = List.of(), cmdEnd = List.of(), cmdSurvivor = List.of();
    private String survivorTable, defaultTable;
    private boolean keepVanilla;
    private boolean forceDifficulty;
    private boolean protectTerrain;
    private boolean blockWitherSpawn;

    EventManager(BloodMoonPlus plugin) { this.plugin = plugin; }

    void reload() {
        FileConfiguration c = plugin.getConfig();
        hordeEnabled = c.getBoolean("horde.enabled", true);
        waveInterval = c.getDouble("horde.interval-seconds", 25);
        baseSize = c.getInt("horde.base-size", 5);
        waveMaxSize = Math.max(1, c.getInt("horde.wave-max-size", 30));
        maxPerPlayer = c.getInt("horde.max-alive-per-player", 25);
        minDist = c.getDouble("horde.spawn-distance.min", 14);
        maxDist = Math.max(minDist + 1, c.getDouble("horde.spawn-distance.max", 28));
        firstWaveDelay = c.getDouble("event.first-wave-delay-seconds", 10);
        reqArmor = c.getBoolean("horde.require-armor.enabled", false);
        reqPieces = c.getInt("horde.require-armor.min-pieces", 1);
        reqMaterials = c.getStringList("horde.require-armor.materials");
        lockNight = c.getBoolean("event.lock-night", true);
        nightTime = c.getLong("event.night-time-ticks", 18000);
        endTime = c.getLong("event.end-time-ticks", 23000);
        clearWeather = c.getBoolean("event.clear-weather", true);
        removeOnEnd = c.getBoolean("event.remove-mobs-on-end", true);
        forceDifficulty = c.getBoolean("event.force-difficulty", true);
        protectTerrain = c.getBoolean("event.protect-terrain", true);
        blockWitherSpawn = c.getBoolean("event.block-wither-spawn", true);
        rewardOnStop = c.getBoolean("event.reward-on-stop", false);
        cmdStart = c.getStringList("event.commands-on-start");
        cmdEnd = c.getStringList("event.commands-on-end");
        cmdSurvivor = c.getStringList("event.survivor-commands");
        maxMult = c.getDouble("difficulty.max", 10);
        survivorTable = c.getString("loot.survivor-table", "survivors");
        defaultTable = c.getString("loot.default-table", "mob_horde");
        // Enforce: si no hay players online en el mundo del evento, no generamos la horda
        skipEmpty = c.getBoolean("schedule.skip-if-no-players", true);
        globalMode = c.getBoolean("event.global", true);
        keepVanilla = c.getBoolean("loot.keep-vanilla-drops", false);
        if (!c.contains("loot.keep-vanilla-drops")) keepVanilla = false;

        mobs.clear();
        for (Map<?, ?> m : c.getMapList("horde.mobs")) {
            EntityType type;
            try {
                type = EntityType.valueOf(Util.str(m, "type", "ZOMBIE").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("horde.mobs: tipo inválido " + m.get("type"));
                continue;
            }
            mobs.add(new MobDef(type, Util.num(m, "weight", 1), Util.num(m, "health", 0),
                    Util.num(m, "damage-multiplier", 1), Util.num(m, "speed-multiplier", 1),
                    Util.str(m, "name", null), parseEquipment(m.get("equipment")),
                    Util.num(m, "equipment-chance", 1), Util.str(m, "loot-table", null)));
        }
    }

    static Map<EquipmentSlot, Material> parseEquipment(Object o) {
        Map<EquipmentSlot, Material> out = new EnumMap<>(EquipmentSlot.class);
        if (!(o instanceof Map<?, ?> map)) return out;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            EquipmentSlot slot = switch (e.getKey().toString().toLowerCase(Locale.ROOT)) {
                case "main-hand", "hand" -> EquipmentSlot.HAND;
                case "off-hand" -> EquipmentSlot.OFF_HAND;
                case "head", "helmet" -> EquipmentSlot.HEAD;
                case "chest", "chestplate" -> EquipmentSlot.CHEST;
                case "legs", "leggings" -> EquipmentSlot.LEGS;
                case "feet", "boots" -> EquipmentSlot.FEET;
                default -> null;
            };
            Material mat = Material.matchMaterial(e.getValue().toString());
            if (slot != null && mat != null) out.put(slot, mat);
        }
        return out;
    }

    public boolean keepVanillaDrops() { return keepVanilla; }

    public String defaultTable() { return defaultTable; }

    /** Si true, solo el jefe de la Luna (Wither etiquetado) puede aparecer; el resto se bloquea. */
    public boolean blockWitherSpawn() { return blockWitherSpawn; }

    /** Si true, las explosiones de los mobs de la horda no rompen bloques (el jefe sí puede). */
    public boolean protectTerrain() { return protectTerrain; }

    /** Sesión de UN mundo concreto: hordas, jefes y barra viven en su propio mundo. */
    public Session session(World w) { return w == null ? null : sessions.get(w.getName()); }

    public boolean isActive(World w) { return w != null && sessions.containsKey(w.getName()); }

    /** Sesión "principal" para placeholders/estado: todas comparten inicio, fin y dificultad. */
    public Session globalSession() { return sessions.isEmpty() ? null : sessions.values().iterator().next(); }

    public boolean hasGlobalSession() { return !sessions.isEmpty(); }

    public Collection<Session> sessions() { return sessions.values(); }

    public int activeWorlds() { return sessions.size(); }

    /** event.global activo: el evento se lanza en todos los mundos cargados. */
    public boolean isGlobalMode() { return globalMode; }

    /** Mundos donde debe arrancar el evento cuando no se especifica una lista. */
    public List<World> defaultWorlds() {
        List<World> out = new ArrayList<>();
        if (globalMode) {
            out.addAll(Bukkit.getWorlds());
        } else {
            for (String wn : plugin.getConfig().getStringList("worlds")) {
                World w = Bukkit.getWorld(wn);
                if (w != null) out.add(w);
            }
        }
        return out;
    }

    /** Mundos sin reloj propio: Paper 26.2+ lanza IllegalArgumentException en World.setTime. */
    private final Set<String> worldsWithoutClock = new HashSet<>();

    /** Fija la hora del mundo SIN romper el tick si el mundo no tiene reloj propio
     *  (Paper 26.2+: Nether/End u otros mundos sin ciclo de día). Se recuerda por mundo
     *  para no reintentar cada segundo ni ensuciar la consola. Sin esta protección,
     *  la excepción abortaba startInternal y CORTABA tickSession ANTES de spawnWave,
     *  tickSpawns y los eventos especiales: por eso "no caían las hordas". */
    private void setWorldTime(World w, long time) {
        if (w == null || worldsWithoutClock.contains(w.getName())) return;
        try {
            w.setTime(time);
        } catch (IllegalArgumentException ex) {
            worldsWithoutClock.add(w.getName());
            plugin.getLogger().warning("No se puede fijar la hora en '" + w.getName()
                    + "': el mundo no tiene reloj propio en esta versión de Paper; la noche fija no aplica ahí.");
        }
    }

    // ------------------------------------------------------------------ inicio / fin

    public boolean start(World w, double diff, int minutes) {
        long now = System.currentTimeMillis();
        return startInternal(w, diff, now, now + Math.max(1, minutes) * 60_000L, false, true);
    }

    /** Arranca el evento en una lista de mundos a la vez (evento global). El anuncio
     *  global se emite UNA sola vez, aunque arranquen varios mundos. */
    public boolean startIn(Collection<World> worlds, double diff, int minutes) {
        if (worlds == null || worlds.isEmpty()) return false;
        long now = System.currentTimeMillis();
        long endMs = now + Math.max(1, minutes) * 60_000L;
        boolean any = false;
        for (World w : worlds) {
            if (w == null) continue;
            if (startInternal(w, diff, now, endMs, false, !any)) any = true;
        }
        return any;
    }

    private boolean startInternal(World w, double diff, long startMs, long endMs, boolean resumed, boolean announce) {
        if (w == null || sessions.containsKey(w.getName())) return false;
        diff = Math.max(0.1, Math.min(maxMult, diff));
        Session s = new Session(w, startMs, endMs, diff);
        // La primera oleada llega a firstWaveDelay segundos EXACTOS del arranque (no tras
        // firstWaveDelay + un intervalo completo como antes). Retrocedemos lastWaveMs un
        // intervalo para que el primer tick que supere el delay lance la horda de inmediato.
        double firstInterval = Math.max(4, waveInterval / plugin.scaled(diff, "spawn-speed"));
        s.lastWaveMs = startMs + (long) (firstWaveDelay * 1000) - (long) (firstInterval * 1000);
        sessions.put(w.getName(), s);
        // Clave = nombre del mundo: así el evento se RETOMA tras un reinicio (resume()
        // resuelve cada clave con Bukkit.getWorld). Antes se guardaba como "global" y se perdía.
        plugin.data().saveActive(w.getName(), startMs, endMs, diff);

        if (lockNight) setWorldTime(w, nightTime);
        if (clearWeather) { w.setStorm(false); w.setThundering(false); }

        // En Pacífico no se pueden spawnear monstruos: se sube la dificultad durante el evento
        if (forceDifficulty && w.getDifficulty() == Difficulty.PEACEFUL) {
            s.originalDifficulty = Difficulty.PEACEFUL;
            w.setDifficulty(Difficulty.EASY);
            plugin.getLogger().info("Dificultad de '" + w.getName() + "' subida a EASY durante el evento (estaba en Pacífico).");
        }

        // El Wither vanilla (y los mobs de la horda) pueden romper bloques: NO tocamos
        // la gamerule mob-griefing. La protección del terreno se hace por explosión
        // (EntityExplodeEvent) y deja pasar al jefe de la Luna.

        if (!resumed) {
            long minutes = Math.max(1, (endMs - startMs) / 60_000L);
            String d = Util.fmt(diff);
            // Niebla roja cinematográfica: tormenta + trueno + rayo en cada jugador
            w.setStorm(true);
            w.setThundering(true);
            Component title = plugin.messages().get("event.title", "difficulty", d);
            Component sub = plugin.messages().get("event.subtitle", "difficulty", d);
            for (Player p : w.getPlayers()) {
                p.showTitle(Title.title(title, sub, Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))));
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.6f);
                // Partículas rojas rodeando al jugador (niebla de sangre)
                p.getWorld().spawnParticle(org.bukkit.Particle.DUST, p.getLocation().add(0, 1, 0), 30, 1.5, 1.5, 1.5, 0.05,
                        new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.8f));
                // Glowing rojo para que la niebla visual sea continua
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 20 * 30, 0, false, false));
            }
            if (announce && plugin.messages().announce("event", false)) plugin.messages().all("event.start", "difficulty", d, "minutes", String.valueOf(minutes));
            runCommands(cmdStart, w, null);
        }
        plugin.bosses().plan(s, resumed);
        return true;
    }

    /** w == null detiene TODOS los mundos del evento (parada global). */
    public boolean stop(World w, boolean natural) {
        List<Session> toStop = new ArrayList<>();
        if (w == null) {
            toStop.addAll(sessions.values());
        } else {
            Session s = sessions.get(w.getName());
            if (s != null) toStop.add(s);
        }
        if (toStop.isEmpty()) return false;
        stopSessions(toStop, natural);
        return true;
    }

    /** Detiene varios mundos a la vez. Las estadísticas históricas se registran UNA sola vez
     *  con los totales agregados del grupo (todos arrancan y terminan juntos). */
    private void stopSessions(List<Session> toStop, boolean natural) {
        if (natural) recordHistory(toStop);
        for (Session s : toStop) {
            sessions.remove(s.world.getName());
            stopOne(s, natural);
        }
    }

    private void recordHistory(List<Session> group) {
        long durationSec = 0, deaths = 0, uniqueDeaths = 0, participants = 0, survivorsCount = 0;
        String bossName = "", bossType = "NONE";
        for (Session s : group) {
            durationSec = Math.max(durationSec, Math.max(0, (s.endMs - s.startMs) / 1000));
            deaths += s.kills.values().stream().mapToInt(Integer::intValue).sum();
            uniqueDeaths += s.died.size();
            participants += s.participants.size();
            survivorsCount += s.participants.size() - s.died.size();
            if (s.lastBossName != null && !s.lastBossName.isEmpty()) {
                bossName = s.lastBossName;
                bossType = s.lastBossType;
            }
        }
        String world = group.size() == 1 ? group.get(0).world.getName() : "global";
        plugin.stats().recordEventCompleted(world, durationSec, deaths, uniqueDeaths,
                participants, survivorsCount, bossName, bossType);
    }

    private void stopOne(Session s, boolean natural) {
        plugin.data().clearActive(s.world.getName());
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(s.bar);
        plugin.bosses().removeAll(s.world);
        if (removeOnEnd) removeTagged(s.world);
        if (lockNight) setWorldTime(s.world, endTime);
        restoreDifficulty(s.world, s);

        List<Player> survivors = new ArrayList<>();
        for (UUID id : s.participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getWorld().equals(s.world) && !s.died.contains(id)) survivors.add(p);
        }
        if (natural || rewardOnStop) {
            // Primero el sistema de recompensas granular del SurvivorManager (deshabilitado por default en WorldConfig)
            var eligible = plugin.survivors().processRewards(s, natural);
            // Además, el sistema legacy de loot (tabla survivors) sigue funcionando
            if (!eligible.isEmpty()) giveSurvivorRewards(s, eligible);
        }

        if (plugin.messages().announce("event", false)) {
            plugin.messages().world(s.world, natural ? "event.end" : "event.stopped");
            if (!s.kills.isEmpty()) {
                List<Map.Entry<UUID, Integer>> top = new ArrayList<>(s.kills.entrySet());
                top.sort((a, b) -> b.getValue() - a.getValue());
                plugin.messages().world(s.world, "event.summary-header");
                for (int i = 0; i < Math.min(3, top.size()); i++) {
                    String name = Bukkit.getOfflinePlayer(top.get(i).getKey()).getName();
                    plugin.messages().world(s.world, "event.summary-line", "pos", String.valueOf(i + 1),
                            "player", name == null ? "?" : name, "kills", String.valueOf(top.get(i).getValue()));
                }
            }
        }
        runCommands(cmdEnd, s.world, null);
    }

    private void giveSurvivorRewards(Session s, List<Player> survivors) {
        for (Player p : survivors) {
            for (ItemStack it : plugin.loot().roll(survivorTable, p, s.difficulty, false)) plugin.loot().giveOrDrop(p, it);
            runCommands(cmdSurvivor, s.world, p);
            p.sendMessage(plugin.messages().prefixed("event.survivors"));
        }
    }

    void runCommands(List<String> cmds, World w, Player p) {
        for (String c : cmds) {
            String cmd = c.replace("{world}", w.getName());
            if (p != null) cmd = cmd.replace("{player}", p.getName());
            else if (cmd.contains("{player}")) continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    /** Se llama al reiniciar el servidor: retoma eventos que seguían activos y limpia mobs huérfanos.
     *  Las claves de data.yml son nombres de mundo, así que el evento SOBREVIVE al reinicio. */
    public void resume() {
        long now = System.currentTimeMillis();
        ConfigurationSection a = plugin.data().active();
        if (a != null) {
            for (String wn : new ArrayList<>(a.getKeys(false))) {
                World w = Bukkit.getWorld(wn);
                long end = a.getLong(wn + ".end");
                if (w == null || end <= now) { plugin.data().clearActive(wn); continue; }
                if (startInternal(w, a.getDouble(wn + ".difficulty", 1), a.getLong(wn + ".start", now), end, true, false)) {
                    plugin.getLogger().info("Evento retomado en '" + wn + "' (quedan " + Util.dur((end - now) / 1000) + ").");
                }
            }
        }
        for (String wn : plugin.getConfig().getStringList("worlds")) {
            World w = Bukkit.getWorld(wn);
            if (w != null && sessions.isEmpty()) removeTagged(w);
        }
    }

    /** Apagado del servidor: limpia mobs y barras pero conserva el estado en data.yml para retomar. */
    public void shutdown() {
        for (Session s : new ArrayList<>(sessions.values())) {
            for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(s.bar);
            plugin.bosses().removeAll(s.world);
            removeTagged(s.world);
        }
        sessions.clear();
    }

    /** Restaura la dificultad original del mundo si fue forzada por el evento. */
    private void restoreDifficulty(World w, Session s) {
        if (s.originalDifficulty != null && w.getDifficulty() != s.originalDifficulty) {
            w.setDifficulty(s.originalDifficulty);
        }
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        long now = System.currentTimeMillis();
        if (sessions.isEmpty()) return;

        // Mundos cuyo evento ya terminó o que se descargaron: se retiran como grupo
        List<Session> ended = null;
        List<Session> orphan = null;
        for (Session s : sessions.values()) {
            if (Bukkit.getWorld(s.world.getName()) == null) {
                if (orphan == null) orphan = new ArrayList<>();
                orphan.add(s);
            } else if (now >= s.endMs) {
                if (ended == null) ended = new ArrayList<>();
                ended.add(s);
            }
        }
        if (orphan != null) {
            for (Session s : orphan) {
                sessions.remove(s.world.getName());
                plugin.data().clearActive(s.world.getName());
            }
        }
        if (ended != null) stopSessions(ended, true);

        // Una sesión por mundo: CADA mundo recibe su noche, su horda, su barra y sus partidas.
        // Un fallo en un mundo NO debe dejar sin tick al resto (matar el tick del scheduler
        // dejaba a los demás mundos sin noche, sin hordas y sin jefes).
        for (Session s : new ArrayList<>(sessions.values())) {
            try {
                tickSession(s, now);
            } catch (Exception ex) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Error en el tick del evento del mundo '" + s.world.getName() + "': " + ex.getMessage(), ex);
            }
        }
    }

    private void tickSession(Session global, long now) {
        World w = global.world;
        if (Bukkit.getWorld(w.getName()) == null) return;

        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) global.participants.add(p.getUniqueId());
        }
        updateBar(global, now);
        if (lockNight) setWorldTime(w, nightTime);
        // Ambiente cinematográfico reducido para servidores con mods y muchos jugadores:
        // se mantiene el ambiente pero se van los truenos/sonidos muy frecuentes y la niebla constante.
        if (w.hasStorm()) w.setStorm(false);
        if (!w.isThundering()) w.setThundering(true);
        if (now - global.lastAmbientThunderMs > 40_000L + ThreadLocalRandom.current().nextInt(30_000)) {
            global.lastAmbientThunderMs = now;
            if (!w.getPlayers().isEmpty()) {
                Player rp = w.getPlayers().get(ThreadLocalRandom.current().nextInt(w.getPlayers().size()));
                rp.playSound(rp.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.35f);
            }
        }
        if (now / 1000 % 30 == 0) {
            if (!w.getPlayers().isEmpty()) {
                Player rp = w.getPlayers().get(ThreadLocalRandom.current().nextInt(w.getPlayers().size()));
                rp.playSound(rp.getLocation(), org.bukkit.Sound.AMBIENT_CAVE, 0.35f, 0.45f);
            }
        }
        if (now / 1000 % 40 == 10) {
            if (!w.getPlayers().isEmpty()) {
                Player rp = w.getPlayers().get(ThreadLocalRandom.current().nextInt(w.getPlayers().size()));
                rp.playSound(rp.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_HEARTBEAT, 0.25f, 0.7f);
            }
        }
        // Niebla roja ligera cada 4 segundos en lugar de cada 2, y solo glow puntual.
        if (now / 1000 % 4 == 0) {
            for (Player p : w.getPlayers()) {
                w.spawnParticle(org.bukkit.Particle.DUST, p.getLocation().add(0, 0.5, 0), 6, 0.7, 0.35, 0.7, 0.02,
                        new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(140, 0, 0), 0.9f));
                if (!p.hasPotionEffect(org.bukkit.potion.PotionEffectType.GLOWING)) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 20 * 10, 0, false, false));
                }
            }
        }

        double interval = Math.max(4, waveInterval / plugin.scaled(global.difficulty, "spawn-speed"));
        if (now - global.lastWaveMs >= interval * 1000) {
            global.lastWaveMs = now;
            spawnWave(global);
        }
        plugin.bosses().tickSpawns(global, now);
        tickSpecialEvent(global, now);
    }

    private long lastSpecialEventMs = 0;

    /** Dispara un evento especial aleatorio cada 5-10 minutos. */
    private void tickSpecialEvent(Session s, long now) {
        // Inicializar el temporizador al primer tick
        if (lastSpecialEventMs == 0) lastSpecialEventMs = now;
        long since = now - lastSpecialEventMs;
        // Próximo evento entre 5 y 10 minutos desde el último
        if (since < 5 * 60_000L) return;
        if (ThreadLocalRandom.current().nextDouble() < (since - 5 * 60_000L) / (5 * 60_000L * 0.1)) {
            lastSpecialEventMs = now;
            triggerRandomSpecial(s);
        }
    }

    private void triggerRandomSpecial(Session s) {
        int pick = ThreadLocalRandom.current().nextInt(4);
        World w = s.world;
        boolean sp = plugin.messages().announce("specials", false);
        switch (pick) {
            case 0 -> { // Eclipse: oscurecer el cielo
                setWorldTime(w, 18000);
                w.playSound(net.kyori.adventure.sound.Sound.sound(org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, net.kyori.adventure.sound.Sound.Source.MASTER, 1f, 0.6f));
                if (sp) plugin.messages().world(w, "special.eclipse");
            }
            case 1 -> { // Tormenta breve: 60s
                w.setStorm(true);
                w.setThundering(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (Bukkit.getWorld(w.getName()) != null) {
                        w.setStorm(false);
                        w.setThundering(false);
                    }
                }, 20L * 60L);
                if (sp) plugin.messages().world(w, "special.storm");
            }
            case 2 -> { // Niebla densa
                for (Player p : w.getPlayers()) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.BLINDNESS, 20 * 45, 0));
                }
                if (sp) plugin.messages().world(w, "special.fog");
            }
            case 3 -> { // Silencio: mobs no hacen sonido, jugadores escuchan solo su corazón
                w.playSound(net.kyori.adventure.sound.Sound.sound(org.bukkit.Sound.ENTITY_WARDEN_HEARTBEAT, net.kyori.adventure.sound.Sound.Source.MASTER, 1f, 1f));
                for (Player p : w.getPlayers()) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.DARKNESS, 20 * 30, 0));
                }
                if (sp) plugin.messages().world(w, "special.silence");
            }
        }
    }

    private void updateBar(Session s, long now) {
        long rem = s.endMs - now;
        float prog = (float) Math.max(0, Math.min(1, rem / (double) Math.max(1, s.endMs - s.startMs)));
        s.bar.progress(prog);
        s.bar.name(plugin.messages().get("event.bar", "time", Util.dur(rem / 1000), "difficulty", Util.fmt(s.difficulty)));
        for (Player p : s.world.getPlayers()) p.showBossBar(s.bar);
    }

    // ------------------------------------------------------------------ hordas

    boolean eligible(Player p, Session s) {
        if (p.isDead() || s.dead.contains(p.getUniqueId())) return false;
        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) return false;
        if (!reqArmor) return true;
        int c = 0;
        for (ItemStack a : p.getInventory().getArmorContents()) {
            if (a == null) continue;
            String n = a.getType().name();
            for (String m : reqMaterials) if (n.startsWith(m.toUpperCase(Locale.ROOT) + "_")) { c++; break; }
        }
        return c >= reqPieces;
    }

    /** Verifica si hay al menos un jugador elegible en el mundo del evento. */
    boolean hasEligiblePlayers(Session s) {
        for (Player p : s.world.getPlayers()) {
            if (eligible(p, s)) return true;
        }
        return false;
    }

    public int spawnWave(Session s) {
        if (!hordeEnabled || mobs.isEmpty()) return 0;
        if (!hasEligiblePlayers(s)) return 0;
        World w = s.world;

        double sizeScale = plugin.scaled(s.difficulty, "horde-size");
        // Oleadas progresivas por número de oleada: 1ª=5, 2ª=10, 3ª=15... hasta wave-max-size (30)
        s.waveCount++;
        int dynamicBase = Math.min(waveMaxSize, baseSize * s.waveCount);
        int perWave = Math.max(1, (int) Math.round(dynamicBase * sizeScale));
        // Con muchos jugadores o muchos mobs ya vivos, bajamos el tope de la oleada para evitar
        // que el mundo se sature rápido (llegada de mods + 20 jugadores + muchos mods spawneando).
        int nearGlobal = 0;
        for (Player p : w.getPlayers()) {
            if (!eligible(p, s)) continue;
            for (Entity e : p.getNearbyEntities(48, 48, 48)) {
                if (e instanceof LivingEntity le && isTagged(le)) { nearGlobal++; break; }
            }
        }
        int playerCount = (int) w.getPlayers().stream().filter(p -> eligible(p, s)).count();
        int saturation = Math.min(100, nearGlobal / Math.max(1, playerCount) + (playerCount * 6));
        int cap = Math.max(1, (int) Math.round(maxPerPlayer * sizeScale * (1 + s.waveCount * 0.15)
                * (100 - Math.min(40, saturation)) / 100.0));
        int spawned = 0;
        for (Player p : w.getPlayers()) {
            if (!eligible(p, s)) continue;
            int near = 0;
            // Contamos solo los mobs de la Luna CERCA (48b) usando los chunks vecinos del
            // jugador (getNearbyEntities) en lugar de recorrer TODAS las entidades del mundo,
            // que con 40 jugadores y cientos de mobs es muy costoso en cada oleada.
            for (Entity e : p.getNearbyEntities(48, 48, 48)) {
                if (e instanceof LivingEntity le && isTagged(le)) near++;
            }
            int amount = Math.min(perWave, Math.max(0, cap - near));
            // Rayo cinematográfico + anuncio de horda cayendo (solo para la primera oleada del jugador)
            if (s.lastHordeAnnounceMs == 0 || System.currentTimeMillis() - s.lastHordeAnnounceMs > 30_000L) {
                // Encontrar un punto de spawn para el rayo cinematográfico
                Location rayPoint = findSpawn(p.getLocation(), minDist, maxDist);
                if (rayPoint != null) {
                    w.strikeLightningEffect(rayPoint);
                    w.playSound(rayPoint, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f);
                }
                s.lastHordeAnnounceMs = System.currentTimeMillis();
                if (plugin.messages().announce("horde", true))
                    plugin.messages().all("event.horde-descending", "player", p.getName());
            }
            for (int i = 0; i < amount; i++) {
                Location loc = findSpawn(p.getLocation(), minDist, maxDist);
                if (loc == null) continue;
                // Escalera visual moderada: cada 5 mobes en lugar de cada 3, para no saturan el mundo
                // con rayos y partículas cuando ya hay mods y 20 jugadores.
                if (i % 5 == 0) {
                    Location spot = findSpawn(p.getLocation(), minDist, maxDist);
                    if (spot != null) {
                        w.strikeLightningEffect(spot);
                        w.spawnParticle(org.bukkit.Particle.DUST, spot.add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.05,
                                new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.2f));
                    }
                }
                MobDef d = Util.pick(mobs, MobDef::weight);
                if (d == null) continue;
                LivingEntity le = spawn(loc, d.type(), d.health(), d.damage(), d.speed(), d.name(), d.equipment(),
                        d.equipChance(), s.difficulty, d.table(), p);
                if (le != null) spawned++;
            }
        }
        return spawned;
    }

    /** Mob de refuerzo (invocaciones de jefes): usa la definición de la horda si existe. */
    public LivingEntity spawnMinion(Location loc, EntityType type, double diff, Player target) {
        for (MobDef d : mobs) {
            if (d.type() == type) {
                return spawn(loc, type, d.health(), d.damage(), d.speed(), d.name(), d.equipment(), d.equipChance(), diff, d.table(), target);
            }
        }
        return spawn(loc, type, 0, 1, 1, null, Map.of(), 1, diff, null, target);
    }

    public LivingEntity spawn(Location loc, EntityType type, double health, double dmg, double speed, String name,
                              Map<EquipmentSlot, Material> equip, double equipChance, double diff, String table, Player target) {
        Entity ent;
        try {
            ent = loc.getWorld().spawnEntity(loc, type);
        } catch (IllegalStateException ex) {
            // En Pacífico no se puede spawnear monstruos. Si se fuerza dificultad no debería pasar;
            // si está desactivado, evitamos llenar la consola con el mismo warning cada oleada.
            if (ex.getMessage() == null || !ex.getMessage().contains("Peaceful")) {
                plugin.getLogger().warning("No se pudo spawnear " + type + ": " + ex.getMessage());
            }
            return null;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("No se pudo spawnear " + type + ": " + ex.getMessage());
            return null;
        }
        if (!(ent instanceof LivingEntity le)) { ent.remove(); return null; }

        var pdc = le.getPersistentDataContainer();
        pdc.set(Keys.MOB, PersistentDataType.BYTE, (byte) 1);
        pdc.set(Keys.DIFF, PersistentDataType.DOUBLE, diff);
        pdc.set(Keys.SCALE, PersistentDataType.DOUBLE, dmg * plugin.scaled(diff, "damage"));
        if (table != null) pdc.set(Keys.TABLE, PersistentDataType.STRING, table);

        AttributeInstance hp = le.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            double base = health > 0 ? health : hp.getBaseValue();
            double v = Math.min(1024, base * plugin.scaled(diff, "health"));
            hp.setBaseValue(v);
            le.setHealth(Math.min(v, hp.getValue()));
        }
        AttributeInstance sp = le.getAttribute(Attribute.MOVEMENT_SPEED);
        if (sp != null) sp.setBaseValue(sp.getBaseValue() * speed * plugin.scaled(diff, "speed"));

        if (name != null) le.customName(Util.mini(name));
        if (le instanceof Ageable a) a.setAdult();
        le.setRemoveWhenFarAway(false);
        le.setPersistent(false);

        EntityEquipment eq = le.getEquipment();
        if (eq != null && equip != null) {
            for (Map.Entry<EquipmentSlot, Material> en : equip.entrySet()) {
                if (ThreadLocalRandom.current().nextDouble() <= equipChance) {
                    eq.setItem(en.getKey(), new ItemStack(en.getValue()));
                    eq.setDropChance(en.getKey(), 0f);
                }
            }
        }
        if (target != null && le instanceof Mob m) m.setTarget(target);
        return le;
    }

    public static boolean isTagged(Entity e) {
        return e.getPersistentDataContainer().has(Keys.MOB, PersistentDataType.BYTE);
    }

    public int removeTagged(World w) {
        int n = 0;
        for (LivingEntity le : w.getLivingEntities()) {
            if (isTagged(le)) { le.remove(); n++; }
        }
        return n;
    }

    public Location findSpawn(Location c, double min, double max) {
        World w = c.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 12; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = min + rnd.nextDouble() * (max - min);
            int x = c.getBlockX() + (int) Math.round(Math.cos(ang) * dist);
            int z = c.getBlockZ() + (int) Math.round(Math.sin(ang) * dist);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            for (int dy : DY) {
                int y = c.getBlockY() + dy;
                if (y <= w.getMinHeight() + 1 || y >= w.getMaxHeight() - 2) continue;
                Block feet = w.getBlockAt(x, y, z);
                Block head = w.getBlockAt(x, y + 1, z);
                Block below = w.getBlockAt(x, y - 1, z);
                if (feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid()
                        && below.getType().isSolid() && !below.getType().isAir()
                        && !below.getType().toString().contains("LAVA")
                        && !below.getType().toString().contains("FIRE")
                        && !below.getType().toString().contains("MAGMA")
                        && !below.getType().toString().contains("SWEET_BERRY")) {
                    return new Location(w, x + 0.5, y, z + 0.5);
                }
            }
        }
        return null;
    }
}
