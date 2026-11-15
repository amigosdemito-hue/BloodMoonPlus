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
        // "rest" es TODO lo que sigue al primer "_". PAPI ya parte el nombre en trozos, así que
        // mirar solo 'sub' hacía que los placeholders de varias palabras (%bloodmoon_last_event_world%,
        // %bloodmoon_total_bosses_spawned%, %bloodmoon_boss_health_percent%, %bloodmoon_death_count%...)
        // no entraran nunca en su case y devolvieran vacío.
        String rest = extra.isEmpty() ? sub : sub + "_" + extra;

        // Mundo del jugador si está online (placeholders POR MUNDO). Sin jugador (consola)
        // se usa el primer mundo con sesión. Antes siempre se usaba globalSession(), así que
        // con evento global todos los mundos mostraban los datos del primero.
        String worldName = null;
        if (player instanceof Player p2) worldName = p2.getWorld().getName();
        var global = plugin.events().globalSession();
        SessionPlaceholderState session = worldName != null
                ? plugin.snapshots().sessionFor(worldName)
                : (global == null ? null : plugin.snapshots().sessionFor(global.world.getName()));
        boolean active = session != null;
        PlayerPlaceholderState playerState = (player != null && player.isOnline())
                ? plugin.snapshots().playerFor(player.getUniqueId())
                : null;
        // Jefe del mundo del jugador (aunque no haya evento: un jefe invocado a mano también
        // debe verse) y, si no hay, el de la sesión y luego el de cualquier mundo.
        BossPlaceholderSnapshot bossSnap = worldName != null ? plugin.snapshots().boss(worldName) : null;
        if (bossSnap == null && session != null) bossSnap = plugin.snapshots().boss(session.worldName());
        if (bossSnap == null) bossSnap = plugin.snapshots().boss();
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
            case "time" -> switch (rest) {
                case "remaining", "remainingseconds", "remaining_seconds" -> String.valueOf(active ? session.timeRemainingSeconds() : 0);
                case "remainingformatted", "remaining_formatted" -> formatTime(active ? session.timeRemainingSeconds() : 0);
                default -> null;
            };
            case "boss" -> switch (rest) {
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
                case "display_line_1" -> formatDisplayLine1(bossSnap);
                case "display_line_2" -> formatDisplayLine2(bossSnap);
                case "display_line_3" -> formatDisplayLine3(bossSnap);
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
            // %bloodmoon_death_count% también cuenta: antes solo servían "death"/"deaths".
            case "death", "deaths" -> rest.isEmpty() || rest.equals("count")
                    ? String.valueOf(active ? session.deaths() : 0)
                    : null;
            case "unique" -> rest.isEmpty() || rest.equals("deaths")
                    ? String.valueOf(active ? session.uniqueDeaths() : 0)
                    : null;
            case "participants" -> rest.isEmpty() || rest.equals("current")
                    ? String.valueOf(active ? session.participantsCurrent() : 0)
                    : null;
            case "survivors" -> rest.isEmpty() || rest.equals("current")
                    ? String.valueOf(active ? session.survivorsCurrent() : 0)
                    : null;
            case "total" -> switch (rest) {
                case "events" -> String.valueOf(hist.totalEvents());
                case "death_events" -> String.valueOf(hist.totalDeathEvents());
                case "unique_deaths" -> String.valueOf(hist.totalUniqueDeaths());
                case "bosses_spawned" -> String.valueOf(hist.totalBossesSpawned());
                case "bosses_defeated" -> String.valueOf(hist.totalBossesDefeated());
                default -> null;
            };
            case "last" -> switch (rest) {
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
                if (rest.startsWith("chance_") && rest.length() > 7) {
                    yield formatDropChance(rest.substring(7));
                }
                yield null;
            }
            case "pity" -> {
                if (!sub.isEmpty()) {
                    var pid = player != null ? player.getUniqueId() : null;
                    // Las rarezas se guardan con Util.key (mayúsculas y sin acentos): usar la
                    // misma normalización evita que %bloodmoon_pity_mítico% devuelva 0.
                    if (pid != null) yield String.valueOf(plugin.data().pity(pid, Util.key(sub)));
                }
                yield null;
            }
            // %bloodmoon_bosses_defeated_session% -> parts: "bosses" | "defeated" | "session"
            case "bosses" -> switch (rest) {
                case "defeated", "defeated_session" -> String.valueOf(session != null ? session.bossesDefeated() : 0);
                default -> null;
            };
            case "horde" -> switch (rest) {
                case "active" -> String.valueOf(active);
                // Mobs de la horda realmente vivos en el mundo (contados en el snapshot).
                case "count" -> String.valueOf(session != null ? session.hordeAlive() : 0);
                default -> null;
            };
            case "abilities" -> sub.equals("count") && player instanceof Player pp
                    ? String.valueOf(countAbilities(pp))
                    : null;
            case "items" -> rest.equals("unique_owned") && player instanceof Player pp2
                    ? String.valueOf(countUniqueItems(pp2))
                    : null;
            case "mythic" -> sub.equals("active")
                    ? String.valueOf(Bukkit.getPluginManager().getPlugin("MythicMobs") != null)
                    : null;
            // Sin jugador (consola, hologramas, --null) se usa el mundo de la sesión activa en
            // lugar de devolver siempre "false".
            case "permanent" -> {
                String wname = worldName != null ? worldName
                        : (global != null ? global.world.getName() : null);
                yield rest.equals("world") && wname != null
                        ? String.valueOf(plugin.worldConfigs().get(wname).permanentBloodMoon())
                        : "false";
            }
            // Tiempo transcurrido real de la sesión (antes se asumían 20 min fijos).
            case "event" -> sub.equals("elapsed")
                    ? formatTime(session != null ? session.elapsedSeconds() : 0)
                    : null;
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
