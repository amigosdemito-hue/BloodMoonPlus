package dev.bloodmoonplus.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Configuración por mundo. Cada mundo tiene su propio archivo en plugins/BloodMoonPlus/<mundo>/config.yml.
 * Si el archivo no existe, se crea desde el template del JAR con la config global por defecto.
 *
 * Estructura (igual a Reborn para permitir migrar):
 *   SurvivorRewards: { Enabled, RequireOnlineAtEnd, MinimumParticipationSeconds, ... }
 *   Boss: { Mode, Rewards, MythicMobs }   (la barra se define en bosses.yml)
 *   CommandsOnStart: [ lista con sufijos ;s, ;f, ;p ]
 *   CommandsOnEnd:   [ lista con sufijos ;s, ;f, ;p ]
 *   BedNotAllowed: bool                  (bloquea dormir durante el evento)
 *   WorldPermanent: bool                 (el evento no termina solo; solo con /luna stop)
 *   DyingLossesInventory: bool           (morir en el evento borra el inventario)
 *   DyingLossesExperience: bool          (morir en el evento no suelta experiencia)
 */
public final class WorldConfig {

    public record SurvivorPolicy(
            boolean enabled,
            boolean requireOnlineAtEnd,
            int minParticipationSeconds,
            boolean disqualifyOnDeath,
            boolean disqualifyOnWorldLeave,
            List<String> messages,
            List<String> commands
    ) {}

    public record BossConfig(
            String mode,                 // VANILLA, MYTHICMOBS, NONE
            boolean rewardsEnabled,
            String rewardsMode,          // KILLER, TOP_DAMAGE, ALL
            boolean requirePlayerKiller,
            boolean rewardOnce,
            List<String> rewardCommands,
            boolean mythicEnabled,
            String mythicInternalName,
            boolean mythicFallback
    ) {}

    private final String worldName;
    private final YamlConfiguration yml;
    private final boolean permanentBloodMoon;
    private final boolean bedNotAllowed;
    private final boolean dyingLossesInventory;
    private final boolean dyingLossesExperience;
    private final SurvivorPolicy survivor;
    private final BossConfig boss;
    private final List<String> commandsOnStart;
    private final List<String> commandsOnEnd;

    public WorldConfig(String worldName, YamlConfiguration yml) {
        this.worldName = worldName;
        this.yml = yml;
        this.permanentBloodMoon = yml.getBoolean("WorldPermanent", false);
        this.bedNotAllowed = yml.getBoolean("BedNotAllowed", true);
        this.dyingLossesInventory = yml.getBoolean("DyingLossesInventory", false);
        this.dyingLossesExperience = yml.getBoolean("DyingLossesExperience", false);
        this.survivor = parseSurvivor(yml.getConfigurationSection("SurvivorRewards"));
        this.boss = parseBoss(yml.getConfigurationSection("Boss"));
        this.commandsOnStart = yml.getStringList("CommandsOnStart");
        this.commandsOnEnd = yml.getStringList("CommandsOnEnd");
    }

    private static SurvivorPolicy parseSurvivor(ConfigurationSection s) {
        if (s == null) return new SurvivorPolicy(false, true, 60, true, false, List.of(), List.of());
        return new SurvivorPolicy(
                s.getBoolean("Enabled", false),
                s.getBoolean("RequireOnlineAtEnd", true),
                s.getInt("MinimumParticipationSeconds", 60),
                s.getBoolean("DisqualifyOnDeath", true),
                s.getBoolean("DisqualifyOnWorldLeave", false),
                s.getStringList("Messages"),
                s.getStringList("Commands")
        );
    }

    private static BossConfig parseBoss(ConfigurationSection s) {
        if (s == null) {
            return new BossConfig("VANILLA", false, "KILLER", true, true, List.of(),
                    false, "BloodMoonBoss", true);
        }
        ConfigurationSection rew = s.getConfigurationSection("Rewards");
        ConfigurationSection myt = s.getConfigurationSection("MythicMobs");
        return new BossConfig(
                s.getString("Mode", "VANILLA"),
                rew != null && rew.getBoolean("Enabled", false),
                rew != null ? rew.getString("Mode", "KILLER") : "KILLER",
                rew != null && rew.getBoolean("RequirePlayerKiller", true),
                rew != null && rew.getBoolean("RewardOnce", true),
                rew != null ? rew.getStringList("Commands") : List.of(),
                myt != null && myt.getBoolean("Enabled", false),
                myt != null ? myt.getString("InternalName", "BloodMoonBoss") : "BloodMoonBoss",
                myt != null && myt.getBoolean("FallbackToVanilla", true)
        );
    }

    public String worldName() { return worldName; }
    public boolean permanentBloodMoon() { return permanentBloodMoon; }
    public boolean bedNotAllowed() { return bedNotAllowed; }
    public boolean dyingLossesInventory() { return dyingLossesInventory; }
    public boolean dyingLossesExperience() { return dyingLossesExperience; }
    public SurvivorPolicy survivor() { return survivor; }
    public BossConfig boss() { return boss; }
    public List<String> commandsOnStart() { return commandsOnStart; }
    public List<String> commandsOnEnd() { return commandsOnEnd; }
    public YamlConfiguration raw() { return yml; }
}
