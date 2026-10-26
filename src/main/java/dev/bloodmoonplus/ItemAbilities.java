package dev.bloodmoonplus;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Habilidades de items (items.yml):
 * Armas:    DAMAGE_BONUS:f  LIFESTEAL:f  LIGHTNING:prob:daño  IGNITE:prob:seg:nivel  WITHER:prob:seg:nivel  SLOW:prob:seg:nivel
 *           EXECUTE:vidaFrac:bonusFrac  BOSS_SLAYER:f  (bonus contra mobs de la Luna de Sangre)
 *           CHAIN_LIGHTNING:prob:cadenas:daño:decaimiento  POISON:prob:seg:nivel  BLEED:prob:seg:nivel
 *           CRIT:prob:multiplicador  (golpe crítico con partículas)
 * Armadura: DAMAGE_REDUCTION:f  DODGE:prob  THORNS:prob:daño  REFLECT:f
 *           THUNDER_GUARD:prob:daño:attacker|self  (rayo al recibir un golpe, sin necesidad de tormenta)
 *           BLAST_WAVE:prob:daño:fuerza  (onda expansiva que lanza al atacante; NO rompe bloques)
 *           LEVITATE:prob:seg:nivel  (levitación al atacante)
 * Pasivos:  efecto:amplificador (ej. speed:1, night_vision:0, strength:1) mientras se porta el item.
 *           HEAL_AURA:radio:curacion  (cura a aliados cercanos).
 */
public final class ItemAbilities implements Listener {
    private final BloodMoonPlus plugin;
    /** Mobs con sangrado activo: evita escanear TODOS los mobs de TODOS los mundos cada 2 s. */
    private final Set<UUID> bleeding = new HashSet<>();

    ItemAbilities(BloodMoonPlus plugin) { this.plugin = plugin; }

    /** Cache de habilidades ya parseadas por cadena cruda: evita split + allocaciones en
     *  cada golpe y en cada tick de pasivos (con 40 jugadores el ahorro es notable). */
    private static final Map<String, List<String[]>> PARSE_CACHE = new ConcurrentHashMap<>();

    private static List<String[]> read(ItemStack it, NamespacedKey key) {
        if (it == null || !it.hasItemMeta()) return List.of();
        String raw = it.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return List.of();
        String ck = key.getKey() + "|" + raw;
        List<String[]> cached = PARSE_CACHE.get(ck);
        if (cached != null) return cached;
        List<String[]> out = new ArrayList<>();
        for (String part : raw.split(";")) {
            if (part.isBlank()) continue;
            String[] a = part.trim().split(":");
            a[0] = a[0].toUpperCase(Locale.ROOT);
            out.add(a);
        }
        List<String[]> immutable = List.copyOf(out);
        if (PARSE_CACHE.size() > 512) PARSE_CACHE.clear();
        PARSE_CACHE.put(ck, immutable);
        return immutable;
    }

    private static double d(String[] a, int i, double def) {
        if (i >= a.length) return def;
        try { return Double.parseDouble(a[i].trim()); } catch (NumberFormatException e) { return def; }
    }

    /** Indica que el evento de daño actual ya está siendo mutado por este plugin. */
    private static Player playerSource(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Player attacker = playerSource(e.getDamager());
        if (attacker != null && e.getEntity() instanceof LivingEntity victim) offense(attacker, victim, e);
        if (e.getEntity() instanceof Player defender) defense(defender, e);
    }

    private void offense(Player attacker, LivingEntity victim, EntityDamageByEntityEvent e) {
        List<String[]> abs = read(attacker.getInventory().getItemInMainHand(), Keys.ITEM_ABILITIES);
        if (abs.isEmpty()) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double dmg = e.getDamage();
        boolean crit = false;
        for (String[] a : abs) {
            switch (a[0]) {
                case "DAMAGE_BONUS" -> dmg *= 1 + d(a, 1, 0);
                case "BOSS_SLAYER" -> { if (EventManager.isTagged(victim)) dmg *= 1 + d(a, 1, 0); }
                case "EXECUTE" -> { if (victim.getHealth() / Util.maxHealth(victim) <= d(a, 1, 0.3)) dmg *= 1 + d(a, 2, 0.5); }
                case "CRIT" -> {
                    if (rnd.nextDouble() < d(a, 1, 0.15)) {
                        dmg *= d(a, 2, 2.0);
                        crit = true;
                    }
                }
                default -> { }
            }
        }
        e.setDamage(dmg);
        if (crit) {
            victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.25);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.9f);
        }

        for (String[] a : abs) {
            switch (a[0]) {
                case "LIFESTEAL" -> {
                    double heal = e.getFinalDamage() * d(a, 1, 0);
                    attacker.setHealth(Math.min(Util.maxHealth(attacker), attacker.getHealth() + heal));
                }
                case "LIGHTNING" -> {
                    if (rnd.nextDouble() < d(a, 1, 0.1)) {
                        victim.getWorld().strikeLightningEffect(victim.getLocation());
                        victim.damage(d(a, 2, 4), attacker);
                    }
                }
                case "CHAIN_LIGHTNING" -> {
                    // Golpe principal + salta a N enemigos cercanos
                    if (rnd.nextDouble() < d(a, 1, 0.1)) {
                        int chains = (int) d(a, 2, 2);
                        double chainDmg = d(a, 3, 4);
                        double decay = d(a, 4, 0.5);
                        LivingEntity current = victim;
                        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
                        hit.add(current);
                        for (int i = 0; i < chains; i++) {
                            LivingEntity next = null;
                            double bestDist = Double.MAX_VALUE;
                            for (LivingEntity nearby : current.getLocation().getNearbyLivingEntities(8)) {
                                if (hit.contains(nearby) || nearby.equals(attacker)) continue;
                                double dist = current.getLocation().distanceSquared(nearby.getLocation());
                                if (dist < bestDist) { bestDist = dist; next = nearby; }
                            }
                            if (next == null) break;
                            next.getWorld().strikeLightningEffect(next.getLocation());
                            next.damage(chainDmg, attacker);
                            hit.add(next);
                            current = next;
                            chainDmg *= (1 - decay);
                        }
                    }
                }
                case "POISON" -> {
                    if (rnd.nextDouble() < d(a, 1, 0.2)) {
                        PotionEffectType t = Reg.effect("poison");
                        if (t != null) victim.addPotionEffect(new PotionEffect(t, (int)(d(a, 2, 4) * 20), (int) d(a, 3, 1)));
                    }
                }
                case "BLEED" -> {
                    if (rnd.nextDouble() < d(a, 1, 0.25)) {
                        // Marca de sangrado: un marker en el PDC que el scheduler aplica daño
                        var pdc = victim.getPersistentDataContainer();
                        int ticks = (int) (d(a, 2, 5) * 20);
                        int perSec = (int) d(a, 3, 1);
                        pdc.set(Keys.BLEED, org.bukkit.persistence.PersistentDataType.INTEGER, ticks);
                        bleeding.add(victim.getUniqueId());
                        victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                        // La aplicación del daño real la hace el tickPassives
                    }
                }
                case "IGNITE" -> { if (rnd.nextDouble() < d(a, 1, 0.2)) victim.setFireTicks((int) (d(a, 2, 4) * 20)); }
                case "WITHER" -> applyEffect(victim, "wither", a, rnd.nextDouble());
                case "SLOW" -> applyEffect(victim, "slowness", a, rnd.nextDouble());
                default -> { }
            }
        }
    }

    /** Aplica un efecto de poción con probabilidad. DSL: efecto:prob:seg[:nivel]
     *  Si no se pasa nivel, amp = 1.
     */
    private void applyEffect(LivingEntity victim, String effect, String[] a, double roll) {
        if (roll >= d(a, 1, 0.2)) return;
        PotionEffectType t = Reg.effect(effect);
        if (t == null) return;
        int ticks = (int) (d(a, 2, 4) * 20);
        int amp = (int) d(a, 3, 1);
        victim.addPotionEffect(new PotionEffect(t, ticks, amp));
    }

    private void defense(Player defender, EntityDamageByEntityEvent e) {
        double reduction = 0, dodge = 0, reflect = 0;
        List<String[]> thorns = new ArrayList<>();
        List<String[]> thunder = new ArrayList<>();
        List<String[]> blast = new ArrayList<>();
        List<String[]> levitate = new ArrayList<>();
        for (ItemStack piece : defender.getInventory().getArmorContents()) {
            for (String[] a : read(piece, Keys.ITEM_ABILITIES)) {
                switch (a[0]) {
                    case "DAMAGE_REDUCTION" -> reduction += d(a, 1, 0);
                    case "DODGE" -> dodge += d(a, 1, 0);
                    case "REFLECT" -> reflect += d(a, 1, 0);
                    case "THORNS" -> thorns.add(a);
                    case "THUNDER_GUARD" -> thunder.add(a);
                    case "BLAST_WAVE" -> blast.add(a);
                    case "LEVITATE" -> levitate.add(a);
                    default -> { }
                }
            }
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (dodge > 0 && rnd.nextDouble() < Math.min(0.6, dodge)) {
            e.setCancelled(true);
            defender.getWorld().spawnParticle(Particle.CLOUD, defender.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
            defender.playSound(defender.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.6f);
            return;
        }
        if (reduction > 0) e.setDamage(e.getDamage() * (1 - Math.min(0.8, reduction)));
        if (reflect > 0 && e.getDamager() instanceof LivingEntity refAttacker) {
            if (rnd.nextDouble() < Math.min(0.5, reflect)) {
                double ret = e.getFinalDamage() * d(new String[]{"REFLECT", String.valueOf(reflect)}, 1, 0.3);
                refAttacker.damage(ret, defender);
                defender.getWorld().spawnParticle(Particle.ENCHANT, defender.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
        if (!thorns.isEmpty() && e.getDamager() instanceof LivingEntity attacker) {
            for (String[] a : thorns) {
                if (rnd.nextDouble() < d(a, 1, 0.2)) {
                    attacker.damage(d(a, 2, 3), defender);
                }
            }
        }
        // THUNDER_GUARD: cae un rayo (sin necesidad de tormenta) sobre el atacante... o sobre ti (self)
        if (!thunder.isEmpty()) {
            for (String[] a : thunder) {
                if (rnd.nextDouble() >= d(a, 1, 0.2)) continue;
                boolean self = a.length > 3 && "self".equalsIgnoreCase(a[3].trim());
                LivingEntity strike = self ? defender : sourceOf(e.getDamager());
                if (strike == null || strike.isDead()) continue;
                strike.getWorld().strikeLightningEffect(strike.getLocation());
                if (self) strike.damage(d(a, 2, 6));
                else strike.damage(d(a, 2, 6), defender);
            }
        }
        // BLAST_WAVE: onda expansiva que daña y lanza al atacante; NO rompe bloques
        if (!blast.isEmpty()) {
            for (String[] a : blast) {
                if (rnd.nextDouble() >= d(a, 1, 0.2)) continue;
                LivingEntity src = sourceOf(e.getDamager());
                // No aplicamos la onda al propio atacante que recibe el golpe (evita self-damage)
                if (src == null || src.equals(defender) || src.isDead()) continue;
                Vector dir = src.getLocation().toVector().subtract(defender.getLocation().toVector()).setY(0);
                if (dir.lengthSquared() < 0.01) dir = new Vector(rnd.nextDouble() - 0.5, 0, rnd.nextDouble() - 0.5);
                src.setVelocity(dir.normalize().multiply(d(a, 3, 1.3)).setY(0.7));
                src.damage(d(a, 2, 5), defender);
                src.getWorld().spawnParticle(Particle.EXPLOSION, src.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.1);
                src.getWorld().playSound(src.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);
            }
        }
        // LEVITATE: el atacante queda en el aire un momento (inofensivo y muy útil)
        if (!levitate.isEmpty()) {
            for (String[] a : levitate) {
                if (rnd.nextDouble() >= d(a, 1, 0.15)) continue;
                LivingEntity src = sourceOf(e.getDamager());
                if (src == null || src.equals(defender) || src.isDead()) continue;
                PotionEffectType t = Reg.effect("levitation");
                if (t == null) continue;
                src.addPotionEffect(new PotionEffect(t, (int) (d(a, 2, 3) * 20), (int) d(a, 3, 0)));
                src.getWorld().spawnParticle(Particle.END_ROD, src.getLocation().add(0, 1.2, 0), 12, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    /** Entidad responsable del golpe (incluye flechas/proyectiles lanzados por un jugador). */
    private static LivingEntity sourceOf(Entity damager) {
        if (damager instanceof LivingEntity le) return le;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof LivingEntity sl) return sl;
        return null;
    }

    /** Efectos pasivos de armadura/arma en mano; se llama cada 2 segundos. */
    void tickPassives() {
        // Aplicar sangrado (bleed) SOLO a los mobs marcados (antes se escaneaban todos los
        // mobs de todos los mundos cada 2 s: coste enorme con 40 jugadores).
        if (!bleeding.isEmpty()) {
            for (Iterator<UUID> it = bleeding.iterator(); it.hasNext(); ) {
                Entity ent = Bukkit.getEntity(it.next());
                if (!(ent instanceof LivingEntity le) || le.isDead() || !le.isValid()) { it.remove(); continue; }
                var pdc = le.getPersistentDataContainer();
                Integer bleedTicks = pdc.get(Keys.BLEED, org.bukkit.persistence.PersistentDataType.INTEGER);
                if (bleedTicks == null || bleedTicks <= 0) { it.remove(); continue; }
                double perHit = 1.0;
                int left = bleedTicks - 40;
                le.damage(perHit);
                if (left <= 0) { pdc.remove(Keys.BLEED); it.remove(); }
                else pdc.set(Keys.BLEED, org.bukkit.persistence.PersistentDataType.INTEGER, left);
                le.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, le.getLocation().add(0, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
            }
        }
        // Pasivos de items de jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyPassives(p, p.getInventory().getItemInMainHand());
            applyPassives(p, p.getInventory().getItemInOffHand());
            for (ItemStack piece : p.getInventory().getArmorContents()) applyPassives(p, piece);
            // HEAL_AURA: si algún item tiene la pasiva, cura a aliados cercanos
            applyHealAura(p);
        }
    }

    private void applyHealAura(Player p) {
        int radius = 0;
        double healPerSec = 0;
        for (ItemStack piece : p.getInventory().getArmorContents()) {
            for (String[] a : read(piece, Keys.ITEM_PASSIVES)) {
                if (a[0].equalsIgnoreCase("HEAL_AURA")) {
                    radius = Math.max(radius, (int) d(a, 1, 5));
                    healPerSec = Math.max(healPerSec, d(a, 2, 1.0));
                }
            }
        }
        if (radius == 0 || healPerSec <= 0) return;
        for (Player nearby : p.getWorld().getPlayers()) {
            if (nearby.equals(p)) continue;
            if (nearby.getLocation().distanceSquared(p.getLocation()) > radius * radius) continue;
            double newHp = Math.min(Util.maxHealth(nearby), nearby.getHealth() + healPerSec);
            nearby.setHealth(newHp);
            nearby.getWorld().spawnParticle(Particle.HEART, nearby.getLocation().add(0, 1.5, 0), 2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    private void applyPassives(Player p, ItemStack it) {
        if (it == null || !it.hasItemMeta()) return;
        for (String[] a : read(it, Keys.ITEM_PASSIVES)) {
            PotionEffectType t = Reg.effect(a[0]);
            if (t == null) continue;
            int amp = (int) d(a, 1, 0);
            // Duración del tick de pasivos; el efecto se refresca cada 2 s, no se acumula.
            int dur = 140;
            p.addPotionEffect(new PotionEffect(t, dur, amp, true, false, true));
        }
    }

    /** Limpia pociones pasivas huérfanas tras respawn o cambio de armadura.
     *  Solo quita un efecto ambiental si, tras mirar todos los items del jugador,
     *  ninguno lo vuelve a pedir. Asi evitamos quitar efectos que otro item mantiene.
 */
    void cleanStalePassives(Player p) {
        if (p == null || !p.isOnline()) return;
        Map<PotionEffectType, Integer> stillNeeded = new java.util.HashMap<>();
        for (ItemStack it : p.getInventory().getArmorContents()) {
            if (it == null || !it.hasItemMeta()) continue;
            for (String[] a : read(it, Keys.ITEM_PASSIVES)) {
                PotionEffectType t = Reg.effect(a[0]);
                if (t == null) continue;
                stillNeeded.merge(t, 1, Integer::sum);
            }
        }
        for (ItemStack it : List.of(p.getInventory().getItemInMainHand(), p.getInventory().getItemInOffHand())) {
            if (it == null || !it.hasItemMeta()) continue;
            for (String[] a : read(it, Keys.ITEM_PASSIVES)) {
                PotionEffectType t = Reg.effect(a[0]);
                if (t == null) continue;
                stillNeeded.merge(t, 1, Integer::sum);
            }
        }
        for (PotionEffect pe : p.getActivePotionEffects()) {
            if (!pe.isAmbient()) continue;
            int still = stillNeeded.getOrDefault(pe.getType(), 0);
            if (still <= 0) {
                p.removePotionEffect(pe.getType());
            }
        }
    }
}
