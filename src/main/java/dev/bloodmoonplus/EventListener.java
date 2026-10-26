package dev.bloodmoonplus;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class EventListener implements Listener {
    private final BloodMoonPlus plugin;

    EventListener(BloodMoonPlus plugin) { this.plugin = plugin; }

    private static Entity source(Entity damager) {
        if (damager instanceof Projectile p && p.getShooter() instanceof Entity s) return s;
        return damager;
    }

    /** Escala el daño de los mobs de la Luna de Sangre según dificultad y multiplicador propio. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDamage(EntityDamageByEntityEvent e) {
        Entity src = source(e.getDamager());
        if (!(src instanceof LivingEntity le) || !EventManager.isTagged(le)) return;
        Double scale = le.getPersistentDataContainer().get(Keys.SCALE, PersistentDataType.DOUBLE);
        if (scale != null && scale != 1.0) e.setDamage(e.getDamage() * scale);
    }

    /**
     * Bloquea el spawn de CUALQUIER Wither que no sea el jefe de la Luna de Sangre.
     * El jefe se distingue porque el plugin marca su spawn (BossManager.isSpawningBoss())
     * y le añade la etiqueta "bloodmoon_boss". Todo lo demás (jugadores, huevos, /summon,
     * granjas de soul sand+calaveras, otros plugins) queda cancelado en el origen.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWitherSpawn(CreatureSpawnEvent e) {
        if (e.getEntityType() != EntityType.WITHER) return;
        if (!plugin.events().blockWitherSpawn()) return;
        if (BossManager.isSpawningBoss()) return; // es el jefe de la Luna
        if (e.getEntity().getScoreboardTags().contains("bloodmoon_boss")) return;
        e.setCancelled(true);
        Location loc = e.getLocation();
        if (loc.getWorld() == null) return;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= 48 * 48) {
                p.sendMessage(plugin.messages().prefixed("event.wither-blocked"));
            }
        }
    }

    /**
     * Protege el terreno durante el evento: las explosiones causadas por mobs
     * (creepers, ghasts, etc.) no rompen bloques, PERO el jefe de la Luna SÍ
     * conserva el comportamiento vanilla (rompe bloques con sus calaveras y su
     * explosión a media vida). Se puede desactivar con event.protect-terrain: false.
     *
     * Además de permitir al jefe, permitimos las explosiones originadas por
     * sus calaveras (Wither skulls) y sus proyectiles, ya que son parte de
     * las habilidades del jefe autorizadas a romper terreno.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobExplode(EntityExplodeEvent e) {
        if (!plugin.events().protectTerrain()) return;
        World w = e.getLocation().getWorld();
        if (w == null || !plugin.events().isActive(w)) return;
        Entity ent = e.getEntity();
        Entity src = source(ent);
        if (plugin.bosses().isBoss(src)) return; // el jefe rompe bloques, como el vanilla
        if (ent instanceof org.bukkit.entity.WitherSkull ws && ws.getShooter() instanceof LivingEntity shooter) {
            // Una calavera disparada por el jefe puede romper bloques
            if (plugin.bosses().isBoss(shooter)) return;
        }
        if (src instanceof LivingEntity le) {
            // Si el origen es el propio jefe o su proyectil, permitimos romper bloques
            if (plugin.bosses().isBoss(src)) return;
            e.blockList().clear();
            e.setYield(0f);
        }
    }

    /** Registra el daño a jefes para repartir la recompensa. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamaged(EntityDamageByEntityEvent e) {
        if (!plugin.bosses().isBoss(e.getEntity())) return;
        Entity src = source(e.getDamager());
        if (src instanceof Player p) plugin.bosses().addDamage(e.getEntity(), p, e.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity ent = e.getEntity();
        if (!EventManager.isTagged(ent)) return;

        if (ent.getPersistentDataContainer().has(Keys.BOSS, PersistentDataType.STRING)) {
            plugin.bosses().handleDeath(ent, e);
            return;
        }

        Player killer = ent.getKiller();
        EventManager.Session s = plugin.events().session(ent.getWorld());
        if (killer != null && s != null) s.kills.merge(killer.getUniqueId(), 1, Integer::sum);

        if (!plugin.events().keepVanillaDrops()) e.getDrops().clear();

        if (killer == null && plugin.getConfig().getBoolean("loot.require-player-kill", true)) return;
        var pdc = ent.getPersistentDataContainer();
        String table = pdc.get(Keys.TABLE, PersistentDataType.STRING);
        if (table == null) table = plugin.events().defaultTable();
        double diff = pdc.getOrDefault(Keys.DIFF, PersistentDataType.DOUBLE, 1.0);

        List<ItemStack> loot = plugin.loot().roll(table, killer, diff, true);
        Location loc = ent.getLocation();
        boolean directAll = killer != null && plugin.getConfig().getBoolean("loot.direct-to-inventory", false);
        for (ItemStack it : loot) {
            boolean direct = directAll || killer != null;
            if (!direct) {
                String rarity = it.getItemMeta() != null
                        ? it.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_RARITY, PersistentDataType.STRING)
                        : null;
                direct = plugin.loot().directToInventory(rarity);
            }
            if (direct) plugin.loot().giveOrDrop(killer, it);
            else plugin.loot().dropAt(loc, it);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        EventManager.Session s = plugin.events().session(e.getEntity().getWorld());
        if (s != null) {
            s.dead.add(e.getEntity().getUniqueId());
            s.died.add(e.getEntity().getUniqueId());
            // Rayo visual + anuncio inmersivo
            org.bukkit.World w = e.getEntity().getWorld();
            w.strikeLightningEffect(e.getEntity().getLocation());
            w.playSound(e.getEntity().getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 0.5f);
            String cause = e.getDeathMessage() != null ? e.getDeathMessage() : plugin.locales().get("event.death-cause-unknown");
            plugin.messages().all("event.player-died", "player", e.getEntity().getName(), "cause", cause);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeathAnnounce(EntityDeathEvent e) {
        // Anuncio aleatorio (no todos, para no spamear) cuando muere un mob de la horda
        if (!EventManager.isTagged(e.getEntity())) return;
        if (e.getEntity().getPersistentDataContainer().has(Keys.BOSS, PersistentDataType.STRING)) return; // bosses ya tienen su mensaje
        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(5) != 0) return; // 1 de cada 5
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        String mobName = e.getEntity().customName() != null
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getEntity().customName())
                : e.getEntity().getType().name().toLowerCase().replace('_', ' ');
        plugin.messages().all("event.mob-slain", "player", killer.getName(), "mob", mobName);
    }

    /** Al reaparecer vuelve a ser objetivo de hordas y jefes; sigue descalificado
     *  de supervivientes por Session.died. Sin esto, morir una vez frenaba las hordas
     *  del jugador durante TODO el evento. */
    @EventHandler
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent e) {
        EventManager.Session s = plugin.events().session(e.getPlayer().getWorld());
        if (s != null) s.dead.remove(e.getPlayer().getUniqueId());
        plugin.abilities().cleanStalePassives(e.getPlayer());
    }

    /** Al cambiar de mundo, oculta la barra del evento del que se aleja (evita recorrer
     *  todos los jugadores online en cada tick de la barra). */
    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent e) {
        EventManager.Session s = plugin.events().session(e.getFrom());
        if (s != null) e.getPlayer().hideBossBar(s.bar);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        EventManager.Session s = plugin.events().session(e.getPlayer().getWorld());
        if (s != null) {
            s.dead.remove(e.getPlayer().getUniqueId());
            s.participants.remove(e.getPlayer().getUniqueId());
        }
    }
}
