package dev.bloodmoonplus.placeholder;

import dev.bloodmoonplus.BloodMoonPlus;
import dev.bloodmoonplus.EventManager;
import dev.bloodmoonplus.stats.StatisticsStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servicio que publica snapshots inmutables cada 10 ticks (0.5 s).
 * Los placeholders (que pueden llamarse desde threads async como TAB) leen del snapshot
 * sin tocar el estado de Bukkit. Thread-safe.
 */
public final class PlaceholderSnapshotService {

    private final BloodMoonPlus plugin;
    private final StatisticsStore stats;
    private final AtomicReference<HistoricalStatistics> history;
    private final Map<String, SessionPlaceholderState> sessionCache = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerPlaceholderState> playerCache = new ConcurrentHashMap<>();
    private final AtomicReference<BossPlaceholderSnapshot> boss = new AtomicReference<>(BossPlaceholderSnapshot.empty());

    public PlaceholderSnapshotService(BloodMoonPlus plugin, StatisticsStore stats) {
        this.plugin = plugin;
        this.stats = stats;
        this.history = new AtomicReference<>(stats.get());
    }

    /** Publica snapshots desde el thread principal. Llamar cada 10 ticks. */
    public void publish() {
        for (var session : plugin.events().sessions()) {
            SessionPlaceholderState snap = new SessionPlaceholderState(
                    UUID.randomUUID(),  // por simplicidad
                    session.world.getName(),
                    Math.max(0, (session.endMs - System.currentTimeMillis()) / 1000),
                    session.kills.size(),
                    session.died.size(),
                    session.participants.size(),
                    countSurvivors(session)
            );
            sessionCache.put(session.world.getName(), snap);
        }
        // Players
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerPlaceholderState snap = PlayerPlaceholderState.empty(p);
            for (var session : plugin.events().sessions()) {
                if (session.participants.contains(p.getUniqueId())) {
                    snap = new PlayerPlaceholderState(
                            p.getUniqueId(), p.getName(),
                            true, 0, 0,
                            !session.died.contains(p.getUniqueId()),
                            session.died.contains(p.getUniqueId()) ? "placeholder.disqualified" : "placeholder.eligible"
                    );
                    break;
                }
            }
            playerCache.put(p.getUniqueId(), snap);
        }
        // Boss
        var bosses = plugin.bosses().aliveSnapshot();
        if (bosses != null) {
            boss.set(bosses);
        }
    }

    private long countSurvivors(EventManager.Session s) {
        return s.participants.size() - s.died.size();
    }

    public HistoricalStatistics history() { return history.get(); }
    public SessionPlaceholderState sessionFor(String world) { return sessionCache.get(world); }
    public PlayerPlaceholderState playerFor(UUID id) { return playerCache.get(id); }
    public BossPlaceholderSnapshot boss() { return boss.get(); }
}
