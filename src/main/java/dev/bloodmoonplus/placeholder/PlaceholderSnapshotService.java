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
        // Refresca el histórico desde el almacén: antes vivía congelado desde onEnable
        // (solo lo "actualizaba" recrear la instancia en un reload).
        history.set(stats.get());
        // Una entrada por CADA mundo con evento. Antes solo se publicaba el primero
        // (globalSession), así que los placeholders de los demás mundos salían vacíos.
        Set<String> live = new HashSet<>();
        for (EventManager.Session s : plugin.events().sessions()) {
            live.add(s.world.getName());
            sessionCache.put(s.world.getName(), new SessionPlaceholderState(
                    UUID.randomUUID(),  // por simplicidad
                    s.world.getName(),
                    Math.max(0, (s.endMs - System.currentTimeMillis()) / 1000),
                    // Muertes de JUGADORES (antes se colaba s.kills.size(): jugadores que mataron mobs).
                    s.deathCount,
                    s.died.size(),
                    s.participants.size(),
                    countSurvivors(s)
            ));
        }
        sessionCache.keySet().removeIf(k -> !live.contains(k));
        // Players: el estado se toma de la sesión del MUNDO del jugador, no de la primera sesión.
        for (Player p : Bukkit.getOnlinePlayers()) {
            EventManager.Session own = plugin.events().session(p.getWorld());
            PlayerPlaceholderState snap = PlayerPlaceholderState.empty(p);
            if (own != null && own.participants.contains(p.getUniqueId())) {
                snap = new PlayerPlaceholderState(
                        p.getUniqueId(), p.getName(),
                        true, 0, 0,
                        !own.died.contains(p.getUniqueId()),
                        own.died.contains(p.getUniqueId()) ? "placeholder.disqualified" : "placeholder.eligible"
                );
            }
            playerCache.put(p.getUniqueId(), snap);
        }
        // Evita que el caché crezca con jugadores que ya se desconectaron.
        playerCache.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        // Boss
        var bosses = plugin.bosses().aliveSnapshot();
        if (bosses != null) {
            boss.set(bosses);
        }
    }

    /** Participantes que nunca murieron. Contarlos así evita resultados negativos. */
    private long countSurvivors(EventManager.Session s) {
        long survived = 0;
        for (UUID id : s.participants) if (!s.died.contains(id)) survived++;
        return survived;
    }

    public HistoricalStatistics history() { return history.get(); }
    public SessionPlaceholderState sessionFor(String world) { return sessionCache.get(world); }
    public PlayerPlaceholderState playerFor(UUID id) { return playerCache.get(id); }
    public BossPlaceholderSnapshot boss() { return boss.get(); }
}
