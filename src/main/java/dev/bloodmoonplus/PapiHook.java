package dev.bloodmoonplus;

import dev.bloodmoonplus.placeholder.BossPlaceholderSnapshot;
import dev.bloodmoonplus.placeholder.BossPlaceholderState;
import dev.bloodmoonplus.placeholder.HistoricalStatistics;
import dev.bloodmoonplus.placeholder.PlayerPlaceholderState;
import dev.bloodmoonplus.placeholder.SessionPlaceholderState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * PlaceholderAPI expansion compatible con BloodMoon-Reborn.
 * Implementa los 41 placeholders públicos de Reborn + 10 únicos de Plus.
 *
 * Reborn: %bloodmoon_active%, %bloodmoon_world%, %bloodmoon_time_remaining_*%,
 *         %bloodmoon_boss_*, %bloodmoon_participating%, %bloodmoon_survivor_*%,
 *         %bloodmoon_death_count%, %bloodmoon_unique_deaths%,
 *         %bloodmoon_participants_current%, %bloodmoon_survivors_current%,
 *         %bloodmoon_total_*%, %bloodmoon_last_event_*, %bloodmoon_last_boss_*
 *
 * Plus:   %bloodmoon_drop_chance_<item>%, %bloodmoon_pity_<rarity>%,
 *         %bloodmoon_bosses_defeated_session%, %bloodmoon_horde_active%,
 *         %bloodmoon_horde_count%, %bloodmoon_abilities_count%,
 *         %bloodmoon_items_unique_owned%, %bloodmoon_mythic_active%,
 *         %bloodmoon_permanent_world%, %bloodmoon_event_elapsed%
 */
public final class PapiHook extends PlaceholderExpansion {

    private final BloodMoonPlus plugin;

    PapiHook(BloodMoonPlus plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "bloodmoon"; }
    @Override public String getAuthor() { return "BloodMoonPlus"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.isEmpty()) return null;
        String[] parts = params.toLowerCase(Locale.ROOT).split("_", 3);
        String p = parts[0];
        String sub = parts.length > 1 ? parts[1] : "";
        String extra = parts.length > 2 ? parts[2] : "";

        // Resolver el mundo del jugador si está online, si no el primer mundo con sesión
        String worldName = null;
        if (player instanceof Player p2) worldName = p2.getWorld().getName();
        SessionPlaceholderState session = null;
        var global = plugin.events().globalSession();
        if (global != null) session = plugin.snapshots().sessionFor(global.world.getName());
        boolean active = session != null;
        PlayerPlaceholderState playerState = (player != null && player.isOnline())
                ? plugin.snapshots().playerFor(player.getUniqueId())
                : null;
        BossPlaceholderSnapshot bossSnap = plugin.snapshots().boss();
        if (bossSnap == null) bossSnap = BossPlaceholderSnapshot.empty();
        HistoricalStatistics hist = plugin.snapshots().history();

        return switch (p) {
            case "active" -> active ? "true" : "false";
            case "activeformatted" -> active
                    ? plugin.locales().get("placeholder.active")
                    : plugin.locales().get("placeholder.inactive");
            case "world" -> active
                    ? session.worldName()
                    : plugin.locales().get("placeholder.none");
            case "time" -> switch (sub) {
                case "remaining", "remainingseconds" -> String.valueOf(active ? session.timeRemainingSeconds() : 0);
                case "remainingformatted" -> formatTime(active ? session.timeRemainingSeconds() : 0);
                default -> null;
            };
            case "boss" -> switch (sub) {
                case "alive" -> String.valueOf(bossSnap.state() == BossPlaceholderState.ALIVE);
                case "name" -> bossSnap.state() == BossPlaceholderState.ALIVE
                        ? bossSnap.bossName()
                        : plugin.locales().get("placeholder.no-boss");
                case "type" -> bossSnap.bossType();
                case "health" -> String.valueOf((int) bossSnap.health());
                case "max", "max_health" -> String.valueOf((int) bossSnap.maxHealth());
                case "health_percent" -> String.valueOf(bossSnap.healthPercent());
                case "health_formatted" -> bossSnap.healthPercent() + "%";
                case "state" -> bossSnap.state().name();
                case "state_formatted" -> formatBossState(bossSnap.state());
                case "display" -> switch (extra) {
                    case "line_1" -> formatDisplayLine1(bossSnap);
                    case "line_2" -> formatDisplayLine2(bossSnap);
                    case "line_3" -> formatDisplayLine3(bossSnap);
                    default -> null;
                };
                default -> null;
            };
            case "participating" -> switch (sub) {
                case "" -> String.valueOf(playerState != null && playerState.participating());
                case "seconds" -> String.valueOf(playerState != null ? playerState.participationSeconds() : 0);
                case "percent" -> String.valueOf(playerState != null ? playerState.participationPercent() : 0);
                case "formatted" -> formatTime(playerState != null ? playerState.participationSeconds() : 0);
                default -> null;
            };
            case "survivor" -> switch (sub) {
                case "eligible" -> String.valueOf(playerState != null && playerState.survivorEligible());
                case "status" -> playerState != null
                        ? plugin.locales().get(playerState.survivorStatus(), playerState.playerName())
                        : plugin.locales().get("placeholder.not-participating");
                default -> null;
            };
            case "death", "deaths" -> sub.isEmpty()
                    ? String.valueOf(active ? session.deaths() : 0)
                    : null;
            case "unique", "unique_deaths" -> sub.equals("deaths")
                    ? String.valueOf(active ? session.uniqueDeaths() : 0)
                    : null;
            case "participants", "participants_current" -> String.valueOf(active ? session.participantsCurrent() : 0);
            case "survivors", "survivors_current" -> String.valueOf(active ? session.survivorsCurrent() : 0);
            case "total" -> switch (sub) {
                case "events" -> String.valueOf(hist.totalEvents());
                case "death_events" -> String.valueOf(hist.totalDeathEvents());
                case "unique_deaths" -> String.valueOf(hist.totalUniqueDeaths());
                case "bosses_spawned" -> String.valueOf(hist.totalBossesSpawned());
                case "bosses_defeated" -> String.valueOf(hist.totalBossesDefeated());
                default -> null;
            };
            case "last" -> switch (sub) {
                case "event_world" -> hist.lastEvent().world().isEmpty() ? plugin.locales().get("placeholder.none") : hist.lastEvent().world();
                case "event_duration_seconds" -> String.valueOf(hist.lastEvent().durationSeconds());
                case "event_duration_formatted" -> formatTime(hist.lastEvent().durationSeconds());
                case "event_death_count" -> String.valueOf(hist.lastEvent().deathCount());
                case "event_unique_deaths" -> String.valueOf(hist.lastEvent().uniqueDeaths());
                case "event_participants" -> String.valueOf(hist.lastEvent().participants());
                case "event_survivors" -> String.valueOf(hist.lastEvent().survivors());
                case "event_ended_at" -> hist.lastEvent().endedAt() == null || hist.lastEvent().endedAt().equals(java.time.Instant.EPOCH)
                        ? plugin.locales().get("placeholder.none")
                        : DateTimeFormatter.ISO_INSTANT.format(hist.lastEvent().endedAt());
                case "boss_name" -> hist.lastEvent().bossName().isEmpty() ? plugin.locales().get("placeholder.no-boss") : hist.lastEvent().bossName();
                case "boss_type" -> hist.lastEvent().bossType();
                default -> null;
            };
            // ==================== PLUS únicos ====================
            case "drop", "drop_chance" -> {
                if (sub.equals("chance") && !extra.isEmpty()) {
                    yield formatDropChance(extra);
                }
                yield null;
            }
            case "pity" -> {
                if (!sub.isEmpty()) {
                    var pid = player != null ? player.getUniqueId() : null;
                    if (pid != null) yield String.valueOf(plugin.data().pity(pid, sub.toUpperCase(Locale.ROOT)));
                }
                yield null;
            }
            case "bosses_defeated_session" -> {
                long n = 0;
                if (global != null && global.kills.size() > 0) n = countDefeatedBosses();
                yield String.valueOf(n);
            }
            case "horde" -> switch (sub) {
                case "active" -> String.valueOf(active);
                case "count" -> {
                    if (!active) { yield "0"; }
                    // Conteo aproximado de mobs de la horda vivos en el mundo del evento.
                    // No hacemos un recorrido completo porque puede ser muy costoso con
                    // muchos jugadores/mobs. El valor es conservador: part. * 8 como
                    // estimación superior de mobs activos por jugador.
                    long approx = Math.max(0, session.participantsCurrent() * 8);
                    yield String.valueOf(approx);
                }
                default -> null;
            };
            case "abilities" -> sub.equals("count") && player instanceof Player pp
                    ? String.valueOf(countAbilities(pp))
                    : null;
            case "items" -> sub.equals("unique_owned") && player instanceof Player pp2
                    ? String.valueOf(countUniqueItems(pp2))
                    : null;
            case "mythic" -> sub.equals("active")
                    ? String.valueOf(Bukkit.getPluginManager().getPlugin("MythicMobs") != null)
                    : null;
            case "permanent" -> sub.equals("world") && worldName != null
                    ? String.valueOf(plugin.worldConfigs().get(worldName).permanentBloodMoon())
                    : "false";
            case "event" -> sub.equals("elapsed") && active
                    ? formatTime((20 * 60 - session.timeRemainingSeconds()))
                    : "0:00";
            default -> null;
        };
    }

    private String formatTime(long sec) {
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    private String formatBossState(BossPlaceholderState s) {
        return switch (s) {
            case ALIVE -> plugin.locales().get("placeholder.boss-state-alive");
            case DEFEATED -> plugin.locales().get("placeholder.boss-state-defeated");
            case NOT_SPAWNED -> plugin.locales().get("placeholder.boss-state-not-spawned");
            case NONE -> plugin.locales().get("placeholder.boss-state-none");
        };
    }

    private String formatDisplayLine1(BossPlaceholderSnapshot s) {
        if (s.state() == BossPlaceholderState.ALIVE) return plugin.locales().get("placeholder.boss-display-name", "name", s.bossName());
        if (s.state() == BossPlaceholderState.DEFEATED) return plugin.locales().get("placeholder.boss-display-defeated", "name", s.bossName());
        if (s.state() == BossPlaceholderState.NOT_SPAWNED) return plugin.locales().get("placeholder.boss-display-not-spawned");
        return "";
    }

    private String formatDisplayLine2(BossPlaceholderSnapshot s) {
        if (s.state() == BossPlaceholderState.ALIVE) return plugin.locales().get("placeholder.boss-display-type", "type", s.bossType());
        return "";
    }

    private String formatDisplayLine3(BossPlaceholderSnapshot s) {
        if (s.state() == BossPlaceholderState.ALIVE) return plugin.locales().get("placeholder.boss-display-health", "health", s.healthPercent() + "%");
        return "";
    }

    private String formatDropChance(String itemId) {
        var def = plugin.items().get(itemId);
        if (def == null) return "?";
        for (var table : plugin.loot().allTables()) {
            for (var entry : table.effective) {
                if (entry.item().equals(itemId)) {
                    return Math.round(entry.dropChance() * 100) + "%";
                }
            }
        }
        return "100%";
    }

    private long countDefeatedBosses() {
        return plugin.bosses().defeatedCount();
    }

    private int countAbilities(Player p) {
        int n = 0;
        for (var item : p.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(dev.bloodmoonplus.Keys.ITEM_ABILITIES, org.bukkit.persistence.PersistentDataType.STRING)) n++;
        }
        return n;
    }

    private int countUniqueItems(Player p) {
        int n = 0;
        for (var item : p.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (pdc.has(dev.bloodmoonplus.Keys.ITEM_SERIAL, org.bukkit.persistence.PersistentDataType.INTEGER)) n++;
        }
        return n;
    }
}
