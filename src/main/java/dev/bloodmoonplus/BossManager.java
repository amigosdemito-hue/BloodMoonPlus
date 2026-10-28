package dev.bloodmoonplus;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Jefes configurables (bosses.yml) con barra de vida, habilidades, tabla de loot y recompensa al mayor daño. */
public final class BossManager {

    record Ability(String type, double cooldown, String[] args) {}

    record BossDef(String id, String name, EntityType type, double health, double damage, double speed, double scale,
                   double knockbackRes, BossBar.Color color, BossBar.Overlay overlay,
                   Map<EquipmentSlot, Material> equipment, List<Ability> abilities, String lootTable,
                   String topTable, boolean announce, int exp,
                   double enrageAt, double enrageCooldown, double enrageDamage) {}

    record Spawn(String boss, int delay, double chance, double minDifficulty, int count, boolean scaleCount) {}

    public record Pending(String bossId, long atMs) {}

    static final class Instance {
        final UUID id;
        final BossDef def;
        final BossBar bar;
        final double diff;
        final World world;
        final Map<UUID, Double> damage = new HashMap<>();
        final long[] nextUse;
        /** Segunda fase: se activa una sola vez al bajar de enrage-at de vida. */
        boolean enraged = false;

        Instance(UUID id, BossDef def, double diff, World world) {
            this.id = id;
            this.def = def;
            this.diff = diff;
            this.world = world;
            this.bar = BossBar.bossBar(Util.mini(def.name()), 1f, def.color(), def.overlay());
            this.nextUse = new long[def.abilities().size()];
            long now = System.currentTimeMillis();
            for (int i = 0; i < nextUse.length; i++) {
                nextUse[i] = now + (long) (def.abilities().get(i).cooldown() * 1000);
            }
        }
    }

    private final BloodMoonPlus plugin;
    private final Map<String, BossDef> defs = new LinkedHashMap<>();
    private final List<Spawn> spawns = new ArrayList<>();
    private final Map<UUID, Instance> alive = new HashMap<>();
    private long defeatedCount = 0;

    /**
     * Verdadero mientras el plugin está invocando a un jefe de la Luna.
     * Sirve para que el guardián anti-Wither permita el spawn del jefe
     * (que aún no lleva la etiqueta en el instante del CreatureSpawnEvent).
     */
    private static volatile boolean spawningBoss = false;

    static boolean isSpawningBoss() { return spawningBoss; }

    BossManager(BloodMoonPlus plugin) { this.plugin = plugin; }

    void reload() {
        defs.clear();
        spawns.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "bosses.yml"));
        ConfigurationSection bs = y.getConfigurationSection("bosses");
        if (bs != null) {
            for (String id : bs.getKeys(false)) {
                ConfigurationSection s = bs.getConfigurationSection(id);
                if (s == null) continue;
                EntityType type;
                BossBar.Color color;
                BossBar.Overlay overlay;
                try {
                    type = EntityType.valueOf(s.getString("type", "ZOMBIE").toUpperCase(Locale.ROOT));
                    color = BossBar.Color.valueOf(s.getString("bar-color", "RED").toUpperCase(Locale.ROOT));
                    overlay = BossBar.Overlay.valueOf(s.getString("bar-overlay", "NOTCHED_10").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("bosses.yml: valor inválido en '" + id + "': " + ex.getMessage());
                    continue;
                }
                List<Ability> abilities = new ArrayList<>();
                for (String a : s.getStringList("abilities")) {
                    String[] p = a.split(":");
                    if (p.length < 2) continue;
                    try {
                        abilities.add(new Ability(p[0].trim().toUpperCase(Locale.ROOT), Double.parseDouble(p[1].trim()),
                                Arrays.copyOfRange(p, 2, p.length)));
                    } catch (NumberFormatException ex) {
                        plugin.getLogger().warning("bosses.yml: habilidad inválida '" + a + "' en " + id);
                    }
                }
                ConfigurationSection eqSec = s.getConfigurationSection("equipment");
                defs.put(id, new BossDef(id, s.getString("name", id), type, s.getDouble("health", 200),
                        s.getDouble("damage", 1), s.getDouble("speed", 1), s.getDouble("scale", 0),
                        s.getDouble("knockback-resistance", 0), color, overlay,
                        EventManager.parseEquipment(eqSec == null ? null : eqSec.getValues(false)),
                        abilities, s.getString("loot-table"), s.getString("top-damage-table"),
                        s.getBoolean("announce", true), s.getInt("exp", 0),
                        s.getDouble("enrage-at", 0), s.getDouble("enrage-cooldown", 0.6),
                        s.getDouble("enrage-damage", 1.5)));
            }
        }
        for (Map<?, ?> m : y.getMapList("spawns")) {
            Object sc = m.get("scale-count");
            spawns.add(new Spawn(Util.str(m, "boss", ""), (int) Util.num(m, "delay-seconds", 60),
                    Util.num(m, "chance", 1), Util.num(m, "min-difficulty", 1), (int) Util.num(m, "count", 1),
                    sc != null && Boolean.parseBoolean(sc.toString())));
        }
        plugin.getLogger().info("Jefes cargados: " + defs.size());
    }

    public Set<String> ids() { return defs.keySet(); }

    public boolean isBoss(Entity e) { return alive.containsKey(e.getUniqueId()); }

    // ------------------------------------------------------------------ programación

    void plan(EventManager.Session s, boolean resumed) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Spawn sp : spawns) {
            if (!defs.containsKey(sp.boss()) || s.difficulty < sp.minDifficulty()) continue;
            if (rnd.nextDouble() > sp.chance()) continue;
            int n = sp.count();
            if (sp.scaleCount()) n = Math.max(1, (int) Math.round(n * Math.sqrt(s.difficulty)));
            long at = s.startMs + sp.delay() * 1000L;
            if (resumed && at < System.currentTimeMillis()) continue;
            for (int i = 0; i < n; i++) s.pendingBosses.add(new Pending(sp.boss(), at + i * 2000L));
        }
    }

    void tickSpawns(EventManager.Session s, long now) {
        if (s.pendingBosses.isEmpty()) return;
        List<Pending> retry = new ArrayList<>();
        Iterator<Pending> it = s.pendingBosses.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now < p.atMs()) continue;
            it.remove();
            if (!spawn(s, p.bossId())) retry.add(new Pending(p.bossId(), now + 10_000L));
        }
        s.pendingBosses.addAll(retry);
    }

    public boolean spawn(EventManager.Session s, String bossId) {
        BossDef d = defs.get(bossId);
        if (d == null) return false;
        dev.bloodmoonplus.config.WorldConfig wc = plugin.worldConfigs().get(s.world.getName());
        dev.bloodmoonplus.config.WorldConfig.BossConfig bossCfg = wc.boss();
        List<Player> candidates = new ArrayList<>();
        for (Player p : s.world.getPlayers()) if (plugin.events().eligible(p, s)) candidates.add(p);
        if (candidates.isEmpty()) return false;
        Player target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        Location loc = plugin.events().findSpawn(target.getLocation(), 10, 18);
        if (loc == null) loc = target.getLocation();

        LivingEntity le = null;

        // Modo MYTHICMOBS: delega a Mythic si está disponible
        if ("MYTHICMOBS".equals(bossCfg.mode()) && bossCfg.mythicEnabled() && plugin.mythic().isAvailable()) {
            String mythicName = bossCfg.mythicInternalName();
            if (plugin.mythic().mobExists(mythicName)) {
                spawningBoss = true;
                try {
                    le = plugin.mythic().spawn(mythicName, loc, s.difficulty);
                } finally {
                    spawningBoss = false;
                }
                if (le != null) {
                    String display = plugin.mythic().getDisplayName(mythicName);
                    le.customName(Util.mini(display));
                    s.world.strikeLightningEffect(loc);
                    if (plugin.messages().announce("bosses", true))
                        plugin.messages().all("boss.spawn", "boss", display);
                    for (Player p : s.world.getPlayers()) p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.7f);
                    plugin.stats().recordBossSpawned();
                }
            } else {
                if (plugin.messages().announce("bosses", true))
                    plugin.messages().world(s.world, "boss.mythic-not-found", "name", mythicName, "world", s.world.getName());
            }
            // Si Mythic falla y hay fallback, crear vanilla
            if (le == null && bossCfg.mythicFallback()) {
                if (plugin.messages().announce("bosses", true))
                    plugin.messages().world(s.world, "boss.mythic-fallback", "world", s.world.getName());
            } else if (le == null) {
                return false;
            }
        }

        // Modo NONE: no crear nada
        if ("NONE".equals(bossCfg.mode()) && le == null) {
            if (plugin.messages().announce("bosses", true)) plugin.messages().world(s.world, "boss.disabled");
            return false;
        }

        // Vanilla fallback o modo VANILLA
        if (le == null) {
            spawningBoss = true;
            try {
                le = plugin.events().spawn(loc, d.type(), d.health(), d.damage(), d.speed(), d.name(),
                        d.equipment(), 1.0, s.difficulty, null, target);
            } finally {
                spawningBoss = false;
            }
            if (le == null) return false;
            le.getPersistentDataContainer().set(Keys.BOSS, PersistentDataType.STRING, d.id());
            le.setCustomNameVisible(true);
            if (d.scale() > 0) {
                AttributeInstance sc = le.getAttribute(Attribute.SCALE);
                if (sc != null) sc.setBaseValue(d.scale());
            }
            if (d.knockbackRes() > 0) {
                AttributeInstance kb = le.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
                if (kb != null) kb.setBaseValue(Math.min(1.0, d.knockbackRes()));
            }
            if (d.announce() && plugin.messages().announce("bosses", true)) {
                s.world.strikeLightningEffect(loc);
                plugin.messages().all("boss.spawn", "boss", d.name());
                for (Player p : s.world.getPlayers()) p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.7f);
                plugin.stats().recordBossSpawned();
            }
        } else {
            // Marcar también los Mythic como boss para que la BossBar y recompensas funcionen
            le.getPersistentDataContainer().set(Keys.BOSS, PersistentDataType.STRING, d.id());
            le.setCustomNameVisible(true);
        }

        // Etiqueta para que el datapack anti-Wither exima a los jefes de la Luna de Sangre.
        le.addScoreboardTag("bloodmoon_boss");
        alive.put(le.getUniqueId(), new Instance(le.getUniqueId(), d, s.difficulty, s.world));
        return true;
    }

    // ------------------------------------------------------------------ tick de habilidades y barras

    void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Instance> it = alive.values().iterator(); it.hasNext(); ) {
            Instance bi = it.next();
            Entity ent = Bukkit.getEntity(bi.id);
            if (!(ent instanceof LivingEntity le) || le.isDead() || !le.isValid()) {
                hideBar(bi);
                it.remove();
                continue;
            }
            double frac = Math.max(0, Math.min(1, le.getHealth() / Util.maxHealth(le)));
            bi.bar.progress((float) frac);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(le.getWorld())) p.showBossBar(bi.bar);
                else p.hideBossBar(bi.bar);
            }
            // Segunda fase (enloquecida): al bajar del umbral, se activa una sola vez
            if (!bi.enraged && bi.def.enrageAt() > 0 && frac <= bi.def.enrageAt()) {
                enrage(bi, le);
            }
            for (int i = 0; i < bi.def.abilities().size(); i++) {
                if (now < bi.nextUse[i]) continue;
                Ability a = bi.def.abilities().get(i);
                if (use(bi, le, a)) {
                    double cd = a.cooldown() * (bi.enraged ? bi.def.enrageCooldown() : 1.0);
                    bi.nextUse[i] = now + (long) (cd * 1000);
                }
            }
        }
    }

    /** Activa la segunda fase: más rápido, más fuerte y con las habilidades recargadas. */
    private void enrage(Instance bi, LivingEntity boss) {
        bi.enraged = true;
        World w = boss.getWorld();
        int dur = 20 * 60 * 60; // 60 min (cubre de sobra el resto del evento)
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 1));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, dur, 1));
        Location loc = boss.getLocation().add(0, 1, 0);
        w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.4f);
        w.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 120, 1.4, 1.4, 1.4, 0.05);
        w.spawnParticle(Particle.FLAME, loc, 120, 1.4, 1.4, 1.4, 0.08);
        w.spawnParticle(Particle.DUST, loc, 90, 1.4, 1.4, 1.4, 0.05,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 0), 2.2f));
        // Recarga inmediata de todas las habilidades
        long now = System.currentTimeMillis();
        for (int i = 0; i < bi.nextUse.length; i++) bi.nextUse[i] = now;
        for (Player p : w.getPlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.5f);
            p.sendMessage(plugin.messages().prefixed("boss.enraged", "boss", bi.def.name()));
        }
    }

    private List<Player> nearbyPlayers(LivingEntity boss, double radius) {
        List<Player> out = new ArrayList<>();
        for (Player p : boss.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
            if (p.isDead()) continue;
            if (p.getLocation().distanceSquared(boss.getLocation()) <= radius * radius) out.add(p);
        }
        return out;
    }

    private static double arg(Ability a, int i, double def) {
        if (i >= a.args().length) return def;
        try { return Double.parseDouble(a.args()[i].trim()); } catch (NumberFormatException e) { return def; }
    }

    private boolean use(Instance bi, LivingEntity boss, Ability a) {
        List<Player> near = nearbyPlayers(boss, 40);
        if (near.isEmpty()) return false;
        World w = boss.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // Multiplicador de daño de la segunda fase (enloquecido)
        double em = bi.enraged ? bi.def.enrageDamage() : 1.0;
        switch (a.type()) {
            case "SUMMON" -> {
                int count = (int) arg(a, 0, 3);
                EntityType type;
                try {
                    type = EntityType.valueOf((a.args().length > 1 ? a.args()[1] : "ZOMBIE").trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) { return false; }
                int n = Math.max(1, (int) Math.round(count * plugin.scaled(bi.diff, "horde-size")));
                for (int i = 0; i < n; i++) {
                    Location loc = plugin.events().findSpawn(boss.getLocation(), 2, 6);
                    if (loc != null) plugin.events().spawnMinion(loc, type, bi.diff, near.get(rnd.nextInt(near.size())));
                }
                w.playSound(boss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.8f);
                w.spawnParticle(Particle.SOUL, boss.getLocation().add(0, 1, 0), 40, 1, 1, 1, 0.05);
                return true;
            }
            case "SLAM" -> {
                double radius = arg(a, 0, 7);
                double dmg = arg(a, 1, 6);
                List<Player> hit = nearbyPlayers(boss, radius);
                if (hit.isEmpty()) return false;
                for (Player p : hit) {
                    p.damage(dmg * em, boss);
                    Vector dir = p.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0);
                    if (dir.lengthSquared() < 0.01) dir = new Vector(rnd.nextDouble() - 0.5, 0, rnd.nextDouble() - 0.5);
                    p.setVelocity(dir.normalize().multiply(1.1).setY(0.55));
                }
                w.playSound(boss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
                w.spawnParticle(Particle.CLOUD, boss.getLocation(), 70, radius / 2, 0.3, radius / 2, 0.08);
                return true;
            }
            case "LIGHTNING" -> {
                int count = (int) arg(a, 0, 3);
                double dmg = arg(a, 1, 8);
                Collections.shuffle(near);
                for (int i = 0; i < Math.min(count, near.size()); i++) {
                    Player p = near.get(i);
                    w.strikeLightningEffect(p.getLocation());
                    p.damage(dmg * em, boss);
                }
                return true;
            }
            case "AURA" -> {
                double radius = arg(a, 0, 12);
                String eff = a.args().length > 1 ? a.args()[1] : "wither";
                int secs = (int) arg(a, 2, 6);
                int amp = (int) arg(a, 3, 0);
                PotionEffectType type = Reg.effect(eff);
                if (type == null) return false;
                List<Player> hit = nearbyPlayers(boss, radius);
                if (hit.isEmpty()) return false;
                for (Player p : hit) p.addPotionEffect(new PotionEffect(type, secs * 20, amp));
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation().add(0, 1, 0), 60, radius / 3, 1, radius / 3, 0.02);
                return true;
            }
            // ------------------------------------------------------- habilidades avanzadas (Plus 2.0)
            case "SONIC_BOOM" -> {
                // Onda expansiva del Warden: daña y lanza lejos a los jugadores cercanos
                double radius = arg(a, 0, 10);
                double dmg = arg(a, 1, 10);
                List<Player> hit = nearbyPlayers(boss, radius);
                if (hit.isEmpty()) return false;
                for (Player p : hit) {
                    p.damage(dmg * em, boss);
                    Vector dir = p.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0);
                    if (dir.lengthSquared() < 0.01) dir = new Vector(rnd.nextDouble() - 0.5, 0, rnd.nextDouble() - 0.5);
                    p.setVelocity(dir.normalize().multiply(1.8).setY(0.7));
                }
                w.playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
                w.spawnParticle(Particle.SONIC_BOOM, boss.getLocation().add(0, 1, 0), 1);
                w.spawnParticle(Particle.SCULK_SOUL, boss.getLocation().add(0, 1, 0), 40, radius / 3, 1, radius / 3, 0.02);
                return true;
            }
            case "PULL" -> {
                // Atrae a los jugadores hacia el jefe (gravedad de sangre)
                double radius = arg(a, 0, 14);
                double power = arg(a, 1, 1.2);
                List<Player> hit = nearbyPlayers(boss, radius);
                if (hit.isEmpty()) return false;
                for (Player p : hit) {
                    Vector dir = boss.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
                    if (dir.lengthSquared() < 0.01) continue;
                    p.setVelocity(dir.normalize().multiply(power).setY(0.35));
                }
                w.playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 0.7f);
                w.spawnParticle(Particle.SQUID_INK, boss.getLocation().add(0, 1, 0), 50, radius / 3, 1, radius / 3, 0.05);
                return true;
            }
            case "SKULL" -> {
                // Dispara calaveras de wither a los jugadores (sin romper terreno: yield 0)
                int count = (int) arg(a, 0, 3);
                Collections.shuffle(near);
                int fired = 0;
                for (int i = 0; i < Math.min(count, near.size()); i++) {
                    Player p = near.get(i);
                    Vector dir = p.getLocation().add(0, 1, 0).toVector()
                            .subtract(boss.getLocation().add(0, 1.5, 0).toVector());
                    if (dir.lengthSquared() < 0.01) continue;
                    org.bukkit.entity.WitherSkull ws = w.spawn(boss.getLocation().add(0, 1.5, 0),
                            org.bukkit.entity.WitherSkull.class);
                    ws.setShooter(boss);
                    ws.setDirection(dir.normalize().multiply(1.2));
                    ws.setCharged(true);
                    // El jefe SÍ puede romper bloques con sus calaveras (comportamiento vanilla).
                    // El listener de explosiones protege el terreno de los mobs de la horda,
                    // pero deja pasar al jefe.
                    ws.setYield(1f);
                    fired++;
                }
                w.playSound(boss.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.8f);
                return fired > 0;
            }
            case "CHARGE" -> {
                // Embestida del Devastador: se lanza hacia un jugador y arrolla durante ~1s
                double power = arg(a, 0, 1.4);
                double dmg = arg(a, 1, 9);
                Player tp = near.get(rnd.nextInt(near.size()));
                Vector dir = tp.getLocation().toVector().subtract(boss.getLocation().toVector()).setY(0);
                if (dir.lengthSquared() < 0.01) return false;
                dir.normalize();
                boss.setVelocity(dir.clone().multiply(power).setY(0.2));
                w.playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.6f, 0.6f);
                final LivingEntity fb = boss;
                final double fdmg = dmg;
                final int[] ticks = {20};
                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    if (ticks[0]-- <= 0 || fb.isDead() || !fb.isValid()) { task.cancel(); return; }
                    for (Player p : fb.getWorld().getPlayers()) {
                        if (p.isDead()) continue;
                        if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                        if (p.getLocation().distanceSquared(fb.getLocation()) <= 9) {
                            p.damage(fdmg * em, fb);
                            Vector away = p.getLocation().toVector().subtract(fb.getLocation().toVector()).setY(0);
                            if (away.lengthSquared() > 0.01) p.setVelocity(away.normalize().multiply(1.2).setY(0.5));
                        }
                    }
                    fb.getWorld().spawnParticle(Particle.CLOUD, fb.getLocation(), 10, 0.4, 0.2, 0.4, 0.05);
                }, 0L, 2L);
                return true;
            }
            case "ROAR" -> {
                // Rugido: aplica un efecto (por defecto oscuridad) y aturde en área
                double radius = arg(a, 0, 16);
                String eff = a.args().length > 1 ? a.args()[1] : "darkness";
                int secs = (int) arg(a, 2, 6);
                int amp = (int) arg(a, 3, 0);
                PotionEffectType type = Reg.effect(eff);
                if (type == null) return false;
                List<Player> hit = nearbyPlayers(boss, radius);
                if (hit.isEmpty()) return false;
                for (Player p : hit) p.addPotionEffect(new PotionEffect(type, secs * 20, amp));
                w.playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.5f);
                w.spawnParticle(Particle.SONIC_BOOM, boss.getLocation().add(0, 1, 0), 1);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation().add(0, 1, 0), 50, radius / 4, 1, radius / 4, 0.03);
                return true;
            }
            case "GUARD" -> {
                // Bloqueo con escudo: resistencia + absorción temporal y destello de partículas
                int secs = (int) arg(a, 0, 5);
                boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, secs * 20, 2));
                boss.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, secs * 20, 3));
                w.playSound(boss.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.5f, 1f);
                w.spawnParticle(Particle.ENCHANT, boss.getLocation().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0.4);
                return true;
            }
            case "SELF_HEAL" -> {
                // Regeneración del jefe (lo hace un saco de vida difícil de tumbar)
                double amount = arg(a, 0, 60);
                double max = Util.maxHealth(boss);
                if (boss.getHealth() >= max - 0.5) return false;
                boss.setHealth(Math.min(max, boss.getHealth() + amount));
                w.playSound(boss.getLocation(), Sound.ENTITY_WITHER_HURT, 1.4f, 0.5f);
                w.spawnParticle(Particle.HEART, boss.getLocation().add(0, 1.5, 0), 24, 0.7, 0.8, 0.7, 0.05);
                w.spawnParticle(Particle.SOUL, boss.getLocation().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.03);
                return true;
            }
            default -> { return false; }
        }
    }

    // ------------------------------------------------------------------ daño y muerte

    public void addDamage(Entity boss, Player attacker, double amount) {
        Instance bi = alive.get(boss.getUniqueId());
        if (bi != null) bi.damage.merge(attacker.getUniqueId(), amount, Double::sum);
    }

    public void handleDeath(LivingEntity ent, EntityDeathEvent e) {
        Instance bi = alive.remove(ent.getUniqueId());
        if (bi == null) return;
        hideBar(bi);
        defeatedCount++;
        plugin.stats().recordBossDefeated();
        // Recuerda el último jefe caído en su mundo para el histórico de estadísticas
        EventManager.Session ses = plugin.events().session(bi.world);
        if (ses != null) {
            ses.lastBossName = bi.def.name();
            ses.lastBossType = bi.def.type().name();
        }
        e.getDrops().clear();
        e.setDroppedExp(bi.def.exp());

        List<Map.Entry<UUID, Double>> top = new ArrayList<>(bi.damage.entrySet());
        top.sort((x, y) -> Double.compare(y.getValue(), x.getValue()));
        Player topPlayer = top.isEmpty() ? null : Bukkit.getPlayer(top.get(0).getKey());
        Player killer = ent.getKiller() != null ? ent.getKiller() : topPlayer;

        Location loc = ent.getLocation();
        if (bi.def.lootTable() != null) {
            boolean forceAll = plugin.getConfig().getBoolean("loot.direct-to-inventory", false);
            for (ItemStack it : plugin.loot().roll(bi.def.lootTable(), killer, bi.diff, false)) {
                // Nunca se entrega a un destinatario nulo: sin killer (ni top-damage) el loot cae al suelo.
                boolean direct = killer != null && (forceAll || plugin.loot().directToInventory(plugin.loot().rarityOf(it)));
                if (direct) plugin.loot().giveOrDrop(killer, it);
                else plugin.loot().dropAt(loc, it);
            }
        }
        if (plugin.messages().announce("bosses", true)) {
            plugin.messages().world(ent.getWorld(), "boss.killed", "boss", bi.def.name(),
                    "player", killer != null ? killer.getName() : "?");
            if (!top.isEmpty()) {
                plugin.messages().world(ent.getWorld(), "boss.top-header");
                for (int i = 0; i < Math.min(3, top.size()); i++) {
                    String nm = Bukkit.getOfflinePlayer(top.get(i).getKey()).getName();
                    plugin.messages().world(ent.getWorld(), "boss.top-line", "pos", String.valueOf(i + 1),
                            "player", nm == null ? "?" : nm, "damage", String.valueOf(Math.round(top.get(i).getValue())));
                }
            }
        }
        if (topPlayer != null && bi.def.topTable() != null) {
            for (ItemStack it : plugin.loot().roll(bi.def.topTable(), topPlayer, bi.diff, false)) plugin.loot().giveOrDrop(topPlayer, it);
            topPlayer.sendMessage(plugin.messages().prefixed("boss.top-reward"));
        }
    }

    private void hideBar(Instance bi) {
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bi.bar);
    }

    public int removeAll(World w) {
        int n = 0;
        for (Iterator<Instance> it = alive.values().iterator(); it.hasNext(); ) {
            Instance bi = it.next();
            if (!bi.world.equals(w)) continue;
            Entity ent = Bukkit.getEntity(bi.id);
            if (ent != null) ent.remove();
            hideBar(bi);
            it.remove();
            n++;
        }
        return n;
    }

    public long defeatedCount() { return defeatedCount; }

    /** Snapshot del primer boss vivo (para placeholders). */
    public dev.bloodmoonplus.placeholder.BossPlaceholderSnapshot aliveSnapshot() {
        for (Instance bi : alive.values()) {
            Entity ent = Bukkit.getEntity(bi.id);
            if (ent instanceof LivingEntity le && !le.isDead() && le.isValid()) {
                double maxHp = Util.maxHealth(le);
                String name = le.customName() != null
                        ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(le.customName())
                        : bi.def.name();
                return dev.bloodmoonplus.placeholder.BossPlaceholderSnapshot.fromEntity(
                        le, maxHp, name, "VANILLA",
                        dev.bloodmoonplus.placeholder.BossPlaceholderState.ALIVE);
            }
        }
        return null;
    }
}
