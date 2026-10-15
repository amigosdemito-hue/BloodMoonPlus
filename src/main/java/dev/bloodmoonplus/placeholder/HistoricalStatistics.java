package dev.bloodmoonplus.placeholder;

import java.time.Instant;
import java.util.UUID;

/**
 * Estadísticas históricas del servidor: agregados desde la primera instalación.
 * Atómicas, sin datos personales.
 */
public record HistoricalStatistics(
        long totalEvents,
        long totalDeathEvents,
        long totalUniqueDeaths,
        long totalBossesSpawned,
        long totalBossesDefeated,
        LastEvent lastEvent
) {
    public record LastEvent(
            String world,
            long durationSeconds,
            long deathCount,
            long uniqueDeaths,
            long participants,
            long survivors,
            String bossName,
            String bossType,
            Instant endedAt
    ) {}

    public static HistoricalStatistics empty() {
        return new HistoricalStatistics(0, 0, 0, 0, 0,
                new LastEvent("", 0, 0, 0, 0, 0, "", "NONE", Instant.EPOCH));
    }
}
