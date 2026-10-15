package dev.bloodmoonplus.placeholder;

import java.util.UUID;

/**
 * Estado inmutable de una sesión: deaths, participants, survivors.
 */
public record SessionPlaceholderState(
        UUID sessionUuid,
        String worldName,
        long timeRemainingSeconds,
        long deaths,
        long uniqueDeaths,
        long participantsCurrent,
        long survivorsCurrent
) {
    public static SessionPlaceholderState empty(String world) {
        return new SessionPlaceholderState(
                new UUID(0, 0),
                world,
                0, 0, 0, 0, 0
        );
    }
}
