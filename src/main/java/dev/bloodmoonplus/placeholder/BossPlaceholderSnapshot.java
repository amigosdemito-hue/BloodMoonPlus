package dev.bloodmoonplus.placeholder;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Snapshot inmutable de un boss: nombre, tipo, vida, estado narrativo.
 * Se publica desde el thread principal y se lee atómicamente desde los placeholders.
 */
public record BossPlaceholderSnapshot(
        UUID bossUuid,
        String bossName,
        String bossType,        // VANILLA | MYTHICMOBS | NONE
        double health,
        double maxHealth,
        int healthPercent,
        BossPlaceholderState state
) {
    public static BossPlaceholderSnapshot empty() {
        return new BossPlaceholderSnapshot(
                new UUID(0, 0), "", "NONE", 0, 0, 0,
                BossPlaceholderState.NONE
        );
    }

    public static BossPlaceholderSnapshot fromEntity(LivingEntity e, double maxHealth, String name, String type, BossPlaceholderState state) {
        double h = Math.max(0, e.getHealth());
        double m = Math.max(1, maxHealth);
        int pct = (int) Math.min(100, Math.max(0, Math.round(h * 100 / m)));
        return new BossPlaceholderSnapshot(
                e.getUniqueId(),
                name == null ? "" : name,
                type == null ? "VANILLA" : type,
                h, m, pct, state
        );
    }
}
