package dev.bloodmoonplus.placeholder;

import java.util.UUID;

/**
 * Estado inmutable de una sesión: deaths, participants, survivors, horda viva,
 * jefes derrotados y tiempo transcurrido.
 */
public record SessionPlaceholderState(
        UUID sessionUuid,
        String worldName,
        long timeRemainingSeconds,
        long deaths,
        long uniqueDeaths,
        long participantsCurrent,
        long survivorsCurrent,
        long hordeAlive,
        long bossesDefeated,
        long elapsedSeconds
) {
    public static SessionPlaceholderState empty(String world) {
        return new SessionPlaceholderState(
                new UUID(0, 0),
                world,
                0, 0, 0, 0, 0, 0, 0, 0
        );
    }
}
