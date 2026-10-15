package dev.bloodmoonplus.survivor;

import dev.bloodmoonplus.BloodMoonPlus;
import dev.bloodmoonplus.EventManager;
import dev.bloodmoonplus.config.WorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Orquesta las recompensas de superviviente al final de un evento.
 * Respeta SurvivorPolicy del WorldConfig: RequireOnlineAtEnd, MinParticipationSeconds,
 * DisqualifyOnDeath, RewardOncePerSession, etc.
 */
public final class SurvivorManager {

    private final BloodMoonPlus plugin;

    public SurvivorManager(BloodMoonPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Procesa las recompensas al final del evento. Devuelve la lista de supervivientes elegibles.
     */
    public List<Player> processRewards(EventManager.Session s, boolean natural) {
        WorldConfig wc = plugin.worldConfigs().get(s.world.getName());
        WorldConfig.SurvivorPolicy policy = wc.survivor();
        if (!policy.enabled()) return List.of();
        // Por ahora solo natural: las manuales son opt-in
        if (!natural) return List.of();

        long startMs = s.startMs;
        long endMs = s.endMs;
        UUID sessionUuid = UUID.randomUUID();

        List<Player> eligible = new ArrayList<>();
        for (UUID id : s.participants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                if (policy.requireOnlineAtEnd()) continue;
                // No podemos entregar comandos a offline players sin engine async
                continue;
            }
            if (!p.getWorld().equals(s.world)) {
                if (policy.disqualifyOnWorldLeave()) continue;
                continue;
            }
            if (policy.disqualifyOnDeath() && s.died.contains(id)) continue;
            // Participation: estimación simple = duración del evento
            long participation = (endMs - startMs) / 1000;
            if (participation < policy.minParticipationSeconds()) continue;
            eligible.add(p);
        }

        if (eligible.isEmpty()) {
            plugin.messages().all("event.survivors-none", "world", s.world.getName());
            return List.of();
        }

        // Mensaje de cabecera
        plugin.messages().world(s.world, "event.survivors-header", "world", s.world.getName());

        // Construir contexto base
        RewardContext base = new RewardContext(
                s.world, sessionUuid.toString(), startMs, endMs,
                s.participants.size(), s.died.size(), eligible.size(),
                "", "NONE", null, null, s.world.getName(),
                0, 0, 0, 0, 0);

        // Mensajes individuales
        for (Player p : eligible) {
            for (String msg : policy.messages()) {
                String resolved = base.resolve(msg, p);
                p.sendMessage(dev.bloodmoonplus.Util.mini(resolved));
            }
        }

        // Comandos
        for (String raw : policy.commands()) {
            RewardContext.ParsedCommand pc = RewardContext.parse(raw);
            if (pc == null) continue;
            switch (pc.audience()) {
                case CONSOLE_ONCE, CONSOLE -> {
                    String resolved = base.resolve(pc.command(), null);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                }
                case CONSOLE_PER_PLAYER -> {
                    for (Player p : eligible) {
                        String resolved = base.resolve(pc.command(), p);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                    }
                }
                case PLAYER_EXEC -> {
                    for (Player p : eligible) {
                        String resolved = base.resolve(pc.command(), p);
                        p.performCommand(resolved);
                    }
                }
            }
        }
        return eligible;
    }
}
