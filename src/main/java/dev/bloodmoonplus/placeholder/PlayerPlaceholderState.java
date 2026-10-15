package dev.bloodmoonplus.placeholder;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Estado inmutable por jugador: participación, tiempo, elegibilidad.
 * Una vez publicado, ningún thread puede modificarlo.
 */
public record PlayerPlaceholderState(
        UUID playerUuid,
        String playerName,
        boolean participating,
        long participationSeconds,
        long participationPercent,
        boolean survivorEligible,
        String survivorStatus  // localized
) {
    public static PlayerPlaceholderState empty(Player p) {
        return new PlayerPlaceholderState(
                p.getUniqueId(),
                p.getName(),
                false, 0, 0, false,
                "placeholder.not-participating"
        );
    }
}
