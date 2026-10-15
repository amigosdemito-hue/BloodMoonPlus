package dev.bloodmoonplus.survivor;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contexto para resolver placeholders en recompensas y comandos.
 * Compatible con el sistema de Reborn.
 */
public final class RewardContext {

    public enum Audience {
        CONSOLE_ONCE,      // ;s  consola una vez
        CONSOLE_PER_PLAYER,// ;f  consola por cada jugador target
        PLAYER_EXEC,       // ;p  el jugador ejecuta el comando
        CONSOLE           // default, sin sufijo
    }

    public record ParsedCommand(String command, Audience audience) {}

    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-z_]+)(?::([^%]+))?%", Pattern.CASE_INSENSITIVE);

    private final String worldName;
    private final String sessionUuid;
    private final long startTime;
    private final long endTime;
    private final long durationSeconds;
    private final long participantCount;
    private final long deathCount;
    private final long survivorCount;
    private final String bossName;
    private final String bossType;
    private final String bossKiller;
    private final UUID bossKillerUuid;
    private final String bossWorld;
    private final double bossX, bossY, bossZ;
    private final double bossHealth, bossMaxHealth, bossHealthPercent;

    public RewardContext(World world, String sessionUuid, long startMs, long endMs,
                         long participants, long deaths, long survivors,
                         String bossName, String bossType,
                         String bossKiller, UUID bossKillerUuid, String bossWorld,
                         double bossX, double bossY, double bossZ,
                         double bossHealth, double bossMaxHealth) {
        this.worldName = world == null ? "" : world.getName();
        this.sessionUuid = sessionUuid;
        this.startTime = startMs;
        this.endTime = endMs;
        this.durationSeconds = Math.max(0, (endMs - startMs) / 1000);
        this.participantCount = participants;
        this.deathCount = deaths;
        this.survivorCount = survivors;
        this.bossName = bossName;
        this.bossType = bossType;
        this.bossKiller = bossKiller;
        this.bossKillerUuid = bossKillerUuid;
        this.bossWorld = bossWorld;
        this.bossX = bossX; this.bossY = bossY; this.bossZ = bossZ;
        this.bossHealth = bossHealth;
        this.bossMaxHealth = bossMaxHealth;
        this.bossHealthPercent = bossMaxHealth > 0 ? (bossHealth * 100 / bossMaxHealth) : 0;
    }

    /** Resuelve todos los placeholders modernos (%player%, %world%, %boss_name% etc.). */
    public String resolve(String text, Player target) {
        if (text == null) return "";
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1).toLowerCase();
            String opt = m.group(2);
            String replacement = switch (key) {
                case "player", "player_name" -> target == null ? "" : target.getName();
                case "player_uuid" -> target == null ? "" : target.getUniqueId().toString();
                case "world", "world_name" -> worldName;
                case "world_uuid" -> worldName.isEmpty() ? "" : Bukkit.getWorld(worldName).getUID().toString();
                case "session_uuid" -> sessionUuid;
                case "start_time" -> String.valueOf(startTime);
                case "end_time" -> String.valueOf(endTime);
                case "duration_seconds" -> String.valueOf(durationSeconds);
                case "participant_count" -> String.valueOf(participantCount);
                case "survivor_count" -> String.valueOf(survivorCount);
                case "death_count" -> String.valueOf(deathCount);
                case "participation_seconds" -> opt;  // se rellena por jugador en runtime
                case "participation_percent" -> opt;
                case "died" -> opt;
                case "survived" -> opt;
                case "boss_name" -> bossName;
                case "boss_type" -> bossType;
                case "boss_uuid" -> "";
                case "boss_killer" -> bossKiller;
                case "boss_killer_uuid" -> bossKillerUuid == null ? "" : bossKillerUuid.toString();
                case "boss_world" -> bossWorld == null ? "" : bossWorld;
                case "boss_x" -> String.valueOf((int) bossX);
                case "boss_y" -> String.valueOf((int) bossY);
                case "boss_z" -> String.valueOf((int) bossZ);
                case "boss_health" -> String.valueOf((int) bossHealth);
                case "boss_max_health" -> String.valueOf((int) bossMaxHealth);
                case "boss_health_percent" -> String.valueOf((int) bossHealthPercent);
                default -> m.group(0);
            };
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        // Legacy tokens: $w, $p, $b
        String result = sb.toString()
                .replace("$w", worldName)
                .replace("$b", bossName);
        if (target != null) result = result.replace("$p", target.getName());
        return result;
    }

    /** Parsea un comando con su sufijo de audiencia. */
    public static ParsedCommand parse(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        Audience aud = Audience.CONSOLE;
        if (trimmed.endsWith(";s")) { aud = Audience.CONSOLE_ONCE; trimmed = trimmed.substring(0, trimmed.length() - 2).trim(); }
        else if (trimmed.endsWith(";f")) { aud = Audience.CONSOLE_PER_PLAYER; trimmed = trimmed.substring(0, trimmed.length() - 2).trim(); }
        else if (trimmed.endsWith(";p")) { aud = Audience.PLAYER_EXEC; trimmed = trimmed.substring(0, trimmed.length() - 2).trim(); }
        return new ParsedCommand(trimmed, aud);
    }
}
