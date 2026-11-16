package dev.bloodmoonplus.placeholder;

/**
 * Estado inmutable de una sesión: deaths, participants, survivors, horda viva,
 * jefes derrotados y tiempo transcurrido.
 */
public record SessionPlaceholderState(
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
                world,
                0, 0, 0, 0, 0, 0, 0, 0
        );
    }
}
