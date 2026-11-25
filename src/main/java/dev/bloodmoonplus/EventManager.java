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
import org.bukkit.block.BlockFace;
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
        /** Abandonó el mundo del evento a mitad del evento. Lo usa
         *  SurvivorRewards.DisqualifyOnWorldLeave para descalificar de la recompensa. */
        public final Set<UUID> leftWorld = new HashSet<>();
        public final Set<UUID> participants = new HashSet<>();
        /** Primera vez que cada jugador participó: tiempo real de participación. */
        public final Map<UUID, Long> joinedAtMs = new HashMap<>();
        public final Map<UUID, Integer> kills = new HashMap<>();
        final List<BossManager.Pending> pendingBosses = new ArrayList<>();
        /** Jefes cuyo horario ya venció y esperan a la próxima oleada, donde se invocan con su
         *  hueco reservado en el pozo. */
        final List<String> dueBosses = new ArrayList<>();
        /** Mobs de la Luna vivos en este mundo (horda + jefes + esbirros). Es lo que suma al pozo
         *  compartido de horde.max-alive-total; lo resincroniza countHordeAlive cada ~2 s. */
        int aliveTotal;
        /** Dificultad original del mundo, para restaurarla al terminar si fue forzada. */
        Difficulty originalDifficulty;
        long lastWaveMs;
        /** Número de oleadas ya generadas (escalan 5, 10, 15... hasta horde.wave-max-size). */
        int waveCount = 0;
        /** Última vez que se anunció un spawn de horda (para no spamear). */
        long lastHordeAnnounceMs = 0;
        /** Última vez que se reprodujo un trueno ambiental aleatorio. */
        long lastAmbientThunderMs = 0;
        /** Hasta cuándo dura la tormenta del evento especial (0 = ninguna). Sin esto el
         *  tick apagaba la tormenta en el mismo tick en que se encendía. */
        long stormUntilMs = 0;
        /** Muertes de JUGADORES durante el evento (eventos de muerte, no jugadores únicos).
         *  Ojo: s.kills son los MOBS matados por jugadores, que es otra cosa. */
        public int deathCount = 0;
        /** Jefes derrotados en este mundo durante el evento (para placeholders). */
        public int bossesDefeated = 0;
        /** Ya se pagaron las recompensas de jefe de este evento (Boss.Rewards.RewardOnce). */
        boolean bossRewardGiven = false;
        /** Última vez que se disparó un evento especial EN ESTE mundo. */
        long lastSpecialEventMs = 0;
        /** Últimas veces que se reprodujo cada ambiente / se pintó la niebla. Sin esto las
         *  comprobaciones "now/1000 % N == 0" se cumplían durante todo un segundo (20 ticks),
         *  así que el sonido y las partículas salían 20 veces seguidas. */
        long lastAmbientCaveMs = 0, lastHeartbeatMs = 0, lastFogMs = 0;
        /** Último jefe derrotado en este mundo (para el histórico de estadísticas). */
        String lastBossName = "";
        String lastBossType = "NONE";

        /** Participantes que nunca murieron. Contarlo así evita resultados negativos. */
        public long survivors() {
            long n = 0;
            for (UUID id : participants) if (!died.contains(id)) n++;
            return n;
        }

        Session(World world, long startMs, long endMs, double difficulty) {
            this.world = world;
            this.startMs = startMs;
            this.endMs = endMs;
            this.difficulty = difficulty;
        }
    }

    /** display llega ya parseado desde reload(): antes cada mob de cada oleada volvía a pasar su
     *  nombre por MiniMessage (y por el conversor de códigos legacy) al spawnear. */
    record MobDef(EntityType type, double weight, double health, double damage, double speed, Component display,
                  Map<EquipmentSlot, Material> equipment, double equipChance, String table) {}

    private static final int[] DY = {0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, -8};

    /** Suelos sobre los que un mob de la horda no debe aparecer. Antes se comprobaba con
     *  toString().contains(...) por cada bloque candidato de cada búsqueda de spawn. */
    private static final Set<Material> UNSAFE_GROUND = EnumSet.of(Material.LAVA, Material.FIRE,
            Material.SOUL_FIRE, Material.MAGMA_BLOCK, Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW,
            Material.CACTUS);

    private final BloodMoonPlus plugin;
    /** Una sesión por mundo (clave = nombre del mundo), todas las de un mismo evento
     *  arrancan y terminan juntas: es lo que hace que el evento sea GLOBAL. */
    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final List<MobDef> mobs = new ArrayList<>();
    /** Mobs de la horda vivos POR JUGADOR (clave = UUID del dueño), jefes incluidos. Es el cupo
     *  real del evento: lo ajustan spawn/onHordeMobKilled y lo reconstruye countHordeAlive desde
     *  las entidades del mundo cada ~2 s, así que una oleada no necesita recorrer entidades. */
    private final Map<UUID, Integer> aliveByPlayer = new HashMap<>();

    private boolean hordeEnabled;
    /** event.global: el evento cubre TODOS los mundos cargados, no solo los de la lista. */
    private boolean globalMode = true;
    private boolean skipEmpty = true;
    private double waveInterval, minDist, maxDist, firstWaveDelay;
    /** Tope de la progresión de oleadas (1ª=5, 2ª=10, 3ª=15... hasta este valor). */
    private int waveMaxSize = 30;
    /** baseSize = mobs por jugador en la 1ª oleada; maxPerPlayer = techo individual dentro del
     *  pozo; maxAliveTotal = pozo compartido por todos los mundos con evento activo. */
    private int baseSize, maxPerPlayer, maxAliveTotal;
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
        maxAliveTotal = Math.max(1, c.getInt("horde.max-alive-total", 1000));
        maxPerPlayer = Math.max(1, c.getInt("horde.max-alive-per-player", 80));
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

        mobs.clear();
        for (Map<?, ?> m : c.getMapList("horde.mobs")) {
            EntityType type;
            try {
                type = EntityType.valueOf(Util.str(m, "type", "ZOMBIE").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("horde.mobs: tipo inválido " + m.get("type"));
                continue;
            }
            String name = Util.str(m, "name", null);
            mobs.add(new MobDef(type, Util.num(m, "weight", 1), Util.num(m, "health", 0),
                    Util.num(m, "damage-multiplier", 1), Util.num(m, "speed-multiplier", 1),
                    name == null ? null : Util.mini(name), parseEquipment(m.get("equipment")),
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
        // Nether/End no tienen ciclo de día: se marcan como "sin reloj" sin avisar. Antes se
        // intentaba igual y cada evento global (event.global: true) escupía un WARN en consola.
        if (w.getEnvironment() != World.Environment.NORMAL) {
            worldsWithoutClock.add(w.getName());
            return;
        }
        try {
            // setTime reenvía el paquete de la hora a todos los jugadores del mundo. Como el evento
            // vuelve a fijar la noche cada segundo, ese paquete salía 1 vez por segundo y jugador;
            // ahora solo cuando la hora se ha desviado más de ~30 s de juego del valor objetivo.
            long t = w.getTime();
            if (t < time || t > time + 600) w.setTime(time);
        } catch (IllegalArgumentException ex) {
            worldsWithoutClock.add(w.getName());
            plugin.getLogger().warning("No se puede fijar la hora en '" + w.getName()
                    + "': el mundo no tiene reloj propio en esta versión de Paper; la noche fija no aplica ahí.");
        }
    }

    // ------------------------------------------------------------------ inicio / fin

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
            // Trueno ambiental (sin lluvia: clear-weather manda y tickSession la apaga)
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
            runCommands(cmdStart, w, null, diff);
            // Comandos por mundo (CommandsOnStart de <mundo>/config.yml). Eran leídos por
            // WorldConfig pero nunca ejecutados: la opción documentada no hacía nada.
            runCommands(plugin.worldConfigs().get(w.getName()).commandsOnStart(), w, null, diff);

            // Aviso de las reglas de muerte del mundo: estas dos traducciones existían en los
            // locales pero nunca se enviaban, así que el jugador descubría la pérdida de
            // inventario/experiencia al morir.
            var wc = plugin.worldConfigs().get(w.getName());
            for (Player p : w.getPlayers()) {
                if (wc.dyingLossesInventory()) plugin.messages().send(p, "dying-results-inventory-loss");
                if (wc.dyingLossesExperience()) plugin.messages().send(p, "dying-results-xp-loss");
            }
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
            // Muertes de JUGADORES. Antes se sumaban los kills de mobs (s.kills), que es otra cosa.
            deaths += s.deathCount;
            uniqueDeaths += s.died.size();
            participants += s.participants.size();
            // Supervivientes exactos: participantes que nunca murieron (nunca negativo).
            survivorsCount += s.survivors();
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
        // Jefes que quedaron en cola para una oleada que ya no va a llegar.
        s.dueBosses.clear();
        if (lockNight) setWorldTime(s.world, endTime);
        restoreDifficulty(s.world, s);

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
        runCommands(cmdEnd, s.world, null, s.difficulty);
        // Comandos por mundo (CommandsOnEnd), igual que los de inicio.
        runCommands(plugin.worldConfigs().get(s.world.getName()).commandsOnEnd(), s.world, null, s.difficulty);
    }

    private void giveSurvivorRewards(Session s, List<Player> survivors) {
        for (Player p : survivors) {
            for (ItemStack it : plugin.loot().roll(survivorTable, p, s.difficulty, false)) plugin.loot().giveOrDrop(p, it);
            runCommands(cmdSurvivor, s.world, p, s.difficulty);
            p.sendMessage(plugin.messages().prefixed("event.survivors"));
        }
    }

    /** Ejecuta comandos de config. Soporta %world%/%player%/%difficulty% (lo documentado en
     *  worlds-template) y también {world}/{player}/{difficulty}. */
    void runCommands(List<String> cmds, World w, Player p, double diff) {
        String diffText = Util.fmt(diff);
        for (String c : cmds) {
            String cmd = c.replace("{world}", w.getName()).replace("%world%", w.getName())
                    .replace("{difficulty}", diffText).replace("%difficulty%", diffText);
            if (p != null) cmd = cmd.replace("{player}", p.getName()).replace("%player%", p.getName());
            else if (cmd.contains("{player}") || cmd.contains("%player%")) continue;
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
        // Limpieza de mobs huérfanos tras el reinicio: en TODOS los mundos que no tengan una
        // sesión retomada (antes solo se limpiaba si no había ninguna sesión viva, así que
        // con un evento global retomado los mobs viejos de los demás mundos quedaban sueltos).
        for (World world : Bukkit.getWorlds()) {
            if (!sessions.containsKey(world.getName())) removeTagged(world);
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
            } else if (now >= s.endMs
                    && !plugin.worldConfigs().get(s.world.getName()).permanentBloodMoon()) {
                // WorldPermanent: el evento no termina solo, solo se detiene con /luna stop.
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
        // WorldPermanent: en lugar de dejar el evento "terminado" con la barra en 00:00,
        // se desliza el final minuto a minuto para que barra, placeholders y el guardado
        // de data.yml (retomar tras reiniciar) sigan siendo coherentes.
        if (now >= global.endMs && plugin.worldConfigs().get(w.getName()).permanentBloodMoon()) {
            global.endMs = now + 60_000L;
            plugin.data().saveActive(w.getName(), global.startMs, global.endMs, global.difficulty);
        }

        // Una sola copia de la lista por tick: antes se pedía 6 veces (participantes, tres
        // sonidos ambientales, niebla y barra) y cada llamada recorre los jugadores del mundo.
        List<Player> players = w.getPlayers();
        for (Player p : players) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                global.participants.add(p.getUniqueId());
                // Hora de entrada real (los placeholders y SurvivorRewards ya no usan la
                // duración total del evento como "participación").
                global.joinedAtMs.putIfAbsent(p.getUniqueId(), now);
            }
        }
        updateBar(global, now);
        if (lockNight) setWorldTime(w, nightTime);
        // Ambiente cinematográfico reducido para servidores con mods y muchos jugadores:
        // se mantiene el ambiente pero se van los truenos/sonidos muy frecuentes y la niebla constante.
        // La tormenta del evento especial dura lo que marca stormUntilMs; el resto del
        // tiempo se mantiene el cielo despejado (clear-weather).
        if (w.hasStorm() && now > global.stormUntilMs) w.setStorm(false);
        if (!w.isThundering()) w.setThundering(true);
        if (now - global.lastAmbientThunderMs > 40_000L + ThreadLocalRandom.current().nextInt(30_000)) {
            global.lastAmbientThunderMs = now;
            if (!players.isEmpty()) {
                Player rp = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                rp.playSound(rp.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.35f);
            }
        }
        if (now - global.lastAmbientCaveMs >= 30_000L) {
            global.lastAmbientCaveMs = now;
            if (!players.isEmpty()) {
                Player rp = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                rp.playSound(rp.getLocation(), org.bukkit.Sound.AMBIENT_CAVE, 0.35f, 0.45f);
            }
        }
        if (now - global.lastHeartbeatMs >= 40_000L) {
            global.lastHeartbeatMs = now;
            if (!players.isEmpty()) {
                Player rp = players.get(ThreadLocalRandom.current().nextInt(players.size()));
                rp.playSound(rp.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_HEARTBEAT, 0.25f, 0.7f);
            }
        }
        // Niebla roja ligera cada 4 segundos en lugar de cada 2, y solo glow puntual.
        if (now - global.lastFogMs >= 4_000L) {
            global.lastFogMs = now;
            for (Player p : players) {
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

    /** Dispara un evento especial aleatorio cada 5-10 minutos, POR MUNDO.
     *  El temporizador vive en la sesión: antes era un campo único del manager y con evento
     *  global solo el primer mundo que tickeaba recibía eclipses, tormentas y niebla. */
    private void tickSpecialEvent(Session s, long now) {
        // Inicializar el temporizador al primer tick de ESTA sesión
        if (s.lastSpecialEventMs == 0) s.lastSpecialEventMs = now;
        long since = now - s.lastSpecialEventMs;
        // Próximo evento entre 5 y 10 minutos desde el último
        if (since < 5 * 60_000L) return;
        // Ventana de 5 a 10 minutos: probabilidad por segundo que crece con el tiempo
        // transcurrido. Antes era (since - 5 min) / 30 s, que llegaba a 1 (100%) a los
        // 30 segundos y disparaba los eventos especiales todos seguidos.
        // La integral de esta probabilidad lineal a lo largo de los 5 minutos vale 1:
        // el evento cae repartido de forma uniforme entre el minuto 5 y el 10.
        double p = Math.min(0.2, (since - 5 * 60_000L) / (5 * 60_000L) / 150.0);
        if (ThreadLocalRandom.current().nextDouble() < p) {
            s.lastSpecialEventMs = now;
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
            case 1 -> { // Tormenta breve: 60 s (el tick la apaga cuando expira)
                s.stormUntilMs = System.currentTimeMillis() + 60_000L;
                w.setStorm(true);
                w.setThundering(true);
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

    /** ¿Ese jugador ya ve la barra? `BossBar.viewers()` no ofrece `contains()`, y volver a llamar a
     *  `showBossBar` reenvía el paquete aunque el jugador ya la tenga delante. */
    public static boolean barShows(BossBar bar, Player p) {
        for (Object a : bar.viewers()) if (a == p) return true;
        return false;
    }

    private void updateBar(Session s, long now) {
        long rem = s.endMs - now;
        float prog = (float) Math.max(0, Math.min(1, rem / (double) Math.max(1, s.endMs - s.startMs)));
        s.bar.progress(prog);
        s.bar.name(plugin.messages().get("event.bar", "time", Util.dur(rem / 1000), "difficulty", Util.fmt(s.difficulty)));
        // showBossBar es idempotente, pero cada llamada reenvó el paquete de la barra: con varios
        // jugadores y este tick cada segundo era un goteo constante de paquetes sin efecto real.
        for (Player p : s.world.getPlayers()) if (!barShows(s.bar, p)) p.showBossBar(s.bar);
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

    /** Mobs de la Luna vivos en TODOS los mundos con evento activo (horda + jefes + esbirros).
     *  Un mundo sin sesión no aporta nada: sus entidades no son del evento. */
    int poolAlive() {
        int n = 0;
        for (Session s : sessions.values()) n += s.aliveTotal;
        return n;
    }

    /** Elegibles de todos los mundos con evento: el pozo se reparte entre todos ellos para que
     *  ningún mundo ni jugador acapare el hueco. */
    int poolEligible() {
        int n = 0;
        for (Session s : sessions.values()) {
            for (Player p : s.world.getPlayers()) if (eligible(p, s)) n++;
        }
        return n;
    }

    /** Invoca los jefes que vencieron su horario y esperaban turno. Se hace al inicio de la oleada
     *  y se reserva su hueco: si el pozo está lleno, esperan a la siguiente. */
    private void spawnQueuedBosses(Session s) {
        if (s.dueBosses.isEmpty()) return;
        List<String> queue = new ArrayList<>(s.dueBosses);
        s.dueBosses.clear();
        for (String id : queue) {
            if (poolAlive() >= maxAliveTotal) {
                s.dueBosses.add(id);
                continue;
            }
            if (!plugin.bosses().spawn(s, id)) s.dueBosses.add(id);
        }
    }

    public int spawnWave(Session s) { return spawnWave(s, null); }

    /** Oleada dirigida a un solo jugador (/luna horde <jugador>). Con null es la oleada global. */
    public int spawnWave(Session s, Player only) {
        if (!hordeEnabled || mobs.isEmpty()) return 0;
        if (only != null) {
            if (!eligible(only, s)) return 0;
        } else if (!hasEligiblePlayers(s)) return 0;
        World w = s.world;
        List<Player> targets = only != null ? List.of(only) : w.getPlayers();

        // Jugadores elegibles UNA sola vez: antes se recalculaba con stream() y otra vez en el bucle.
        List<Player> eligiblePlayers = new ArrayList<>(targets.size());
        for (Player p : targets) if (eligible(p, s)) eligiblePlayers.add(p);
        if (eligiblePlayers.isEmpty()) return 0;

        // Los jefes que ya vencieron su horario salen al inicio de esta oleada, no a mitad de tick.
        spawnQueuedBosses(s);

        double sizeScale = plugin.scaled(s.difficulty, "horde-size");
        // Oleadas progresivas por número de oleada: 1ª=5, 2ª=10, 3ª=15... hasta wave-max-size (30)
        s.waveCount++;
        int dynamicBase = Math.min(waveMaxSize, baseSize * s.waveCount);
        int perWave = Math.max(1, (int) Math.round(dynamicBase * sizeScale));
        // Pozo compartido por los mundos con evento: el hueco libre se reparte entre los elegibles
        // de TODOS esos mundos, no solo los del mundo que tickea primero. Así nadie acapara y
        // quien entra tarde recibe su parte sin que se le quiten mobs a otro. Los mundos sin
        // evento no suman ni reciben nada (spawnWave solo corre sobre sesiones).
        int free = Math.max(0, maxAliveTotal - poolAlive());
        int totalEligible = Math.max(1, poolEligible());
        int share = free <= 0 ? 0 : Math.max(1, free / totalEligible);
        int remaining = free;
        int spawned = 0;
        boolean announceHorde = s.lastHordeAnnounceMs == 0 || System.currentTimeMillis() - s.lastHordeAnnounceMs > 30_000L;
        for (Player p : eligiblePlayers) {
            if (remaining <= 0) break;
            // Cuota de la oleada: su parte del pozo, su techo individual y lo que aún queda libre.
            int room = Math.min(Math.min(share, maxPerPlayer - aliveByPlayer.getOrDefault(p.getUniqueId(), 0)),
                    remaining);
            if (room <= 0) continue;
            int amount = Math.min(perWave, room);
            // Rayo cinematográfico + anuncio de horda cayendo (como mucho uno cada 30 s)
            if (announceHorde) {
                announceHorde = false;
                // Encontrar un punto de spawn para el rayo cinematográfico
                Location rayPoint = findSpawn(p.getLocation(), minDist, maxDist);
                if (rayPoint != null) {
                    w.strikeLightningEffect(rayPoint);
                    w.playSound(rayPoint, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f);
                }
                s.lastHordeAnnounceMs = System.currentTimeMillis();
                if (plugin.messages().announce("horde", true))
                    // Solo el mundo del jugador: antes se filtraba a los demás mundos.
                    plugin.messages().world(p.getWorld(), "event.horde-descending", "player", p.getName());
            }
            int made = spawnBatch(p, s, amount);
            spawned += made;
            remaining -= made;
        }
        return spawned;
    }

    /** Spawnea hasta `amount` mobs de la horda alrededor de `p` y los anota como suyos.
     *  Es el único camino de spawn de la oleada: los huecos que dejan las bajas los repone la
     *  oleada siguiente, no un repoblado aparte. */
    private int spawnBatch(Player p, Session s, int amount) {
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            // Escala visual moderada: cada 5 mobs en lugar de cada 3, para no saturar el mundo
            // con rayos y partículas cuando ya hay mods y 20 jugadores.
            if (i % 5 == 0) spawnEffect(p);
            if (spawnOne(p, s)) spawned++;
        }
        if (spawned > 0) aliveByPlayer.merge(p.getUniqueId(), spawned, Integer::sum);
        return spawned;
    }

    /** Un mob de la horda junto a `p`, elegido de la tabla de pesos. */
    private boolean spawnOne(Player p, Session s) {
        Location loc = findSpawn(p.getLocation(), minDist, maxDist);
        if (loc == null) return false;
        MobDef d = Util.pick(mobs, MobDef::weight);
        if (d == null) return false;
        return spawn(loc, d.type(), d.health(), d.damage(), d.speed(), d.display(), d.equipment(),
                d.equipChance(), s.difficulty, d.table(), p) != null;
    }

    /** Rayo + polvo rojo que acompañan la caída de cada 5 mobs. */
    private void spawnEffect(Player p) {
        World w = p.getWorld();
        Location spot = findSpawn(p.getLocation(), minDist, maxDist);
        if (spot == null) return;
        w.strikeLightningEffect(spot);
        w.spawnParticle(org.bukkit.Particle.DUST, spot.add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.05,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.2f));
    }

    /** Mob de refuerzo (invocaciones de jefes): usa la definición de la horda si existe. */
    public LivingEntity spawnMinion(Location loc, EntityType type, double diff, Player target) {
        for (MobDef d : mobs) {
            if (d.type() == type) {
                return spawn(loc, type, d.health(), d.damage(), d.speed(), d.display(), d.equipment(), d.equipChance(), diff, d.table(), target);
            }
        }
        return spawn(loc, type, 0, 1, 1, (Component) null, Map.of(), 1, diff, null, target);
    }

    /** Variante para textos sueltos (jefes): parsea el nombre una vez y delega en la de Component. */
    public LivingEntity spawn(Location loc, EntityType type, double health, double dmg, double speed, String name,
                              Map<EquipmentSlot, Material> equip, double equipChance, double diff, String table, Player target) {
        return spawn(loc, type, health, dmg, speed, name == null ? null : Util.mini(name),
                equip, equipChance, diff, table, target);
    }

    public LivingEntity spawn(Location loc, EntityType type, double health, double dmg, double speed, Component display,
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

        // Suma al pozo del evento solo si el mundo tiene una sesión: un mob invocado a mano fuera
        // de evento no consume presupuesto.
        Session sess = session(loc.getWorld());
        if (sess != null) sess.aliveTotal++;

        var pdc = le.getPersistentDataContainer();
        pdc.set(Keys.MOB, PersistentDataType.BYTE, (byte) 1);
        // Dueño del mob: con esta marca se cuenta el techo por jugador (max-alive-per-player).
        if (target != null) pdc.set(Keys.OWNER, PersistentDataType.STRING, target.getUniqueId().toString());
        // Etiqueta de scoreboard ADEMÁS del PDC: si vanilla convierte el mob (zombi → ahogado)
        // la entidad nueva puede perder el PDC, pero la etiqueta sobrevive al menos al evento
        // de transformación, que la vuelve a copiar.
        le.addScoreboardTag(HORDE_TAG);
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

        if (display != null) {
            // El Component llega ya parseado desde reload(): antes cada mob repetía el MiniMessage.
            // Sin setCustomNameVisible el nombre configurado en horde.mobs nunca se veía.
            le.customName(display);
            le.setCustomNameVisible(true);
        }
        if (le instanceof Ageable a) a.setAdult();
        le.setRemoveWhenFarAway(false);
        le.setPersistent(false);

        EntityEquipment eq = equip == null || equip.isEmpty() ? null : le.getEquipment();
        if (eq != null) {
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

    /** Etiquetas de scoreboard de los mobs y jefes de la Luna. */
    public static final String HORDE_TAG = "bloodmoon_horde";
    public static final String BOSS_TAG = "bloodmoon_boss";

    public static boolean isTagged(Entity e) {
        if (e == null) return false;
        return e.getPersistentDataContainer().has(Keys.MOB, PersistentDataType.BYTE)
                || e.getScoreboardTags().contains(HORDE_TAG);
    }

    /**
     * Pasa la marca de la Luna de Sangre a una entidad transformada por vanilla (zombi →
     * ahogado, aldeano → zombi, etc.). Vanilla crea una entidad NUEVA y el PDC se pierde, así
     * que el mob transformado dejaba de contar como mob del evento: ni se contaba en la horda
     * ni lo eliminaba /luna stop ni el final de la Luna de Sangre.
     */
    public static void copyHordeData(LivingEntity from, LivingEntity to) {
        if (from == null || to == null || from.equals(to)) return;
        var src = from.getPersistentDataContainer();
        var dst = to.getPersistentDataContainer();
        Byte mob = src.get(Keys.MOB, PersistentDataType.BYTE);
        if (mob != null) dst.set(Keys.MOB, PersistentDataType.BYTE, mob);
        Double diff = src.get(Keys.DIFF, PersistentDataType.DOUBLE);
        if (diff != null) dst.set(Keys.DIFF, PersistentDataType.DOUBLE, diff);
        Double scale = src.get(Keys.SCALE, PersistentDataType.DOUBLE);
        if (scale != null) dst.set(Keys.SCALE, PersistentDataType.DOUBLE, scale);
        String table = src.get(Keys.TABLE, PersistentDataType.STRING);
        if (table != null) dst.set(Keys.TABLE, PersistentDataType.STRING, table);
        // El mob transformado sigue siendo del mismo jugador: sin esto el cupo se liberaba solo.
        String owner = src.get(Keys.OWNER, PersistentDataType.STRING);
        if (owner != null) dst.set(Keys.OWNER, PersistentDataType.STRING, owner);
        String boss = src.get(Keys.BOSS, PersistentDataType.STRING);
        if (boss != null) dst.set(Keys.BOSS, PersistentDataType.STRING, boss);

        to.addScoreboardTag(HORDE_TAG);
        if (from.getScoreboardTags().contains(BOSS_TAG)) to.addScoreboardTag(BOSS_TAG);
        if (from.customName() != null) {
            to.customName(from.customName());
            to.setCustomNameVisible(true);
        }
        to.setRemoveWhenFarAway(false);
        to.setPersistent(false);
        // El ahogado nace con 20 de vida: conserva la vida escalada del mob original.
        AttributeInstance hpFrom = from.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance hpTo = to.getAttribute(Attribute.MAX_HEALTH);
        if (hpFrom != null && hpTo != null) {
            hpTo.setBaseValue(hpFrom.getBaseValue());
            to.setHealth(Math.max(1, Math.min(hpTo.getValue(), from.getHealth())));
        }
        AttributeInstance spFrom = from.getAttribute(Attribute.MOVEMENT_SPEED);
        AttributeInstance spTo = to.getAttribute(Attribute.MOVEMENT_SPEED);
        if (spFrom != null && spTo != null) spTo.setBaseValue(spFrom.getBaseValue());
    }

    /** Mobs de la horda VIVOS en un mundo (etiquetados y que no son jefes). Lo usa el
     *  placeholder %bloodmoon_horde_count%; lo llama el snapshot cada ~2 s, no PAPI. De paso
     *  resincroniza el cupo por jugador con las entidades reales del mundo. */
    public int countHordeAlive(World w) {
        if (w == null) return 0;
        int n = 0;
        int total = 0;
        Map<UUID, Integer> owned = new HashMap<>();
        for (LivingEntity le : w.getLivingEntities()) {
            if (!isTagged(le)) continue;
            total++;
            UUID owner = ownerOf(le);
            if (owner != null) owned.merge(owner, 1, Integer::sum);
            if (!le.getPersistentDataContainer().has(Keys.BOSS, PersistentDataType.STRING)) n++;
        }
        // Los jugadores del mundo se resincronizan con lo que hay de verdad: así un mob muerto,
        // despawnado o transformado no deja el cupo inflado para siempre. El pozo del mundo se
        // reajusta igual (jefes y esbirros incluidos).
        Session s = session(w);
        if (s != null) s.aliveTotal = total;
        for (Player p : w.getPlayers()) {
            Integer count = owned.get(p.getUniqueId());
            if (count == null) aliveByPlayer.remove(p.getUniqueId());
            else aliveByPlayer.put(p.getUniqueId(), count);
        }
        return n;
    }

    /** Dueño (jugador al que se le asignó) de un mob del evento, o null si no tiene. */
    static UUID ownerOf(Entity e) {
        if (e == null) return null;
        String raw = e.getPersistentDataContainer().get(Keys.OWNER, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Baja de un mob de la horda o de un jefe: devuelve el hueco al pozo y descuenta el cupo
     *  de su dueño. El hueco NO se rellena al instante: lo repone la siguiente oleada, de forma
     *  progresiva. Lo llama EventListener en EntityDeathEvent. */
    public void onHordeMobKilled(LivingEntity mob) {
        Session s = session(mob.getWorld());
        if (s != null && s.aliveTotal > 0) s.aliveTotal--;

        UUID owner = ownerOf(mob);
        if (owner == null) return;
        int alive = aliveByPlayer.getOrDefault(owner, 0);
        if (alive <= 1) aliveByPlayer.remove(owner);
        else aliveByPlayer.put(owner, alive - 1);
    }

    public int removeTagged(World w) {
        int n = 0;
        Set<UUID> owners = new HashSet<>();
        for (LivingEntity le : w.getLivingEntities()) {
            if (!isTagged(le)) continue;
            UUID owner = ownerOf(le);
            if (owner != null) owners.add(owner);
            le.remove();
            n++;
        }
        // Sin mobs vivos tampoco quedan cupos de esos jugadores ni hueco ocupado en el pozo.
        for (UUID id : owners) aliveByPlayer.remove(id);
        Session s = session(w);
        if (s != null) s.aliveTotal = 0;
        return n;
    }

    public Location findSpawn(Location c, double min, double max) {
        World w = c.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int minY = w.getMinHeight() + 1, maxY = w.getMaxHeight() - 2;
        // 8 intentos en lugar de 12 y las comprobaciones baratas primero: una búsqueda llegaba a
        // pagar 12×17 bloques por CADA mob de la oleada, y por cada bloque tres Block nuevos y
        // tres toString().contains(...).
        for (int i = 0; i < 8; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = min + rnd.nextDouble() * (max - min);
            int x = c.getBlockX() + (int) Math.round(Math.cos(ang) * dist);
            int z = c.getBlockZ() + (int) Math.round(Math.sin(ang) * dist);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            for (int dy : DY) {
                int y = c.getBlockY() + dy;
                if (y <= minY || y >= maxY) continue;
                Block feet = w.getBlockAt(x, y, z);
                if (!feet.isPassable() || feet.isLiquid()) continue;
                Block head = feet.getRelative(BlockFace.UP);
                if (!head.isPassable() || head.isLiquid()) continue;
                Material ground = feet.getRelative(BlockFace.DOWN).getType();
                if (!ground.isSolid() || UNSAFE_GROUND.contains(ground)) continue;
                return new Location(w, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }
}
