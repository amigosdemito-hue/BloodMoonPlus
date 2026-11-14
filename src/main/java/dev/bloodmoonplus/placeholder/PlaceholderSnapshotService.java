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
    /** Mobs de la horda vivos por mundo; se recalcula cada ~2 s, no en cada consulta del placeholder. */
    private final Map<String, Long> hordeCache = new ConcurrentHashMap<>();
    private int publishTick = 0;
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
        long now = System.currentTimeMillis();
        // El conteo de horda es un recorrido del mundo: se hace cada ~2 s (4 publicaciones),
        // nunca por consulta de placeholder (PAPI puede pedirlo muchas veces por segundo).
        publishTick++;
        boolean recount = publishTick % 4 == 1;
        // Una entrada por CADA mundo con evento. Antes solo se publicaba el primero
        // (globalSession), así que los placeholders de los demás mundos salían vacíos.
        Set<String> live = new HashSet<>();
        for (EventManager.Session s : plugin.events().sessions()) {
            String wn = s.world.getName();
            live.add(wn);
            if (recount) hordeCache.put(wn, (long) plugin.events().countHordeAlive(s.world));
            sessionCache.put(wn, new SessionPlaceholderState(
                    UUID.randomUUID(),  // por simplicidad
                    wn,
                    Math.max(0, (s.endMs - now) / 1000),
                    // Muertes de JUGADORES (antes se colaba s.kills.size(): jugadores que mataron mobs).
                    s.deathCount,
                    s.died.size(),
                    s.participants.size(),
                    s.survivors(),
                    hordeCache.getOrDefault(wn, 0L),
                    s.bossesDefeated,
                    Math.max(0, (now - s.startMs) / 1000)
            ));
        }
        sessionCache.keySet().removeIf(k -> !live.contains(k));
        if (recount) hordeCache.keySet().removeIf(k -> !live.contains(k));
        // Players: el estado se toma de la sesión del MUNDO del jugador, no de la primera sesión.
        for (Player p : Bukkit.getOnlinePlayers()) {
            EventManager.Session own = plugin.events().session(p.getWorld());
            PlayerPlaceholderState snap = PlayerPlaceholderState.empty(p);
            if (own != null && own.participants.contains(p.getUniqueId())) {
                // Tiempo de participación REAL desde que entró al evento.
                long joined = own.joinedAtMs.getOrDefault(p.getUniqueId(), now);
                long secs = Math.max(0, (now - joined) / 1000);
                long total = Math.max(1, (own.endMs - own.startMs) / 1000);
                long pct = Math.min(100, secs * 100 / total);
                snap = new PlayerPlaceholderState(
                        p.getUniqueId(), p.getName(),
                        true, secs, pct,
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

    public HistoricalStatistics history() { return history.get(); }
    public SessionPlaceholderState sessionFor(String world) { return sessionCache.get(world); }
    public PlayerPlaceholderState playerFor(UUID id) { return playerCache.get(id); }
    public BossPlaceholderSnapshot boss() { return boss.get(); }
}
