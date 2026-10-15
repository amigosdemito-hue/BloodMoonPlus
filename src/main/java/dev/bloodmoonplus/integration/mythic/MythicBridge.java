package dev.bloodmoonplus.integration.mythic;

import dev.bloodmoonplus.BloodMoonPlus;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Puente opcional con MythicMobs. Usa reflexión para evitar dependencia dura (softdepend).
 * Si MythicMobs está cargado, registra listeners; si no, no hace nada.
 *
 * API objetivo: MythicMobs 5.x (5.12.1+ / 5.13.0+).
 */
public final class MythicBridge {

    private final BloodMoonPlus plugin;
    private boolean available = false;
    private Object mythicProvider;     // io.lumine.mythic.bukkit.MythicBukkit (singleton)
    private Method getMobManager;
    private Method getMythicMobInstance;
    private Method spawnMob;
    private Method getInternalName;
    private Method isPresent;

    public MythicBridge(BloodMoonPlus plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            plugin.getLogger().info("MythicMobs no está instalado; modo MythicMobs deshabilitado.");
            return;
        }
        try {
            Class<?> mythicBukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Method inst = mythicBukkit.getMethod("inst");
            this.mythicProvider = inst.invoke(null);
            this.getMobManager = mythicProvider.getClass().getMethod("getMobManager");
            Class<?> mobManagerClass = Class.forName("io.lumine.mythic.core.mobs.MobManager");
            this.getMythicMobInstance = mobManagerClass.getMethod("getMythicMob", String.class);
            Class<?> mythicMobClass = Class.forName("io.lumine.mythic.core.mobs.ActiveMob");
            this.spawnMob = mythicMobClass.getMethod("spawn", org.bukkit.entity.LivingEntity.class, org.bukkit.Location.class, double.class, double.class);
            this.getInternalName = Class.forName("io.lumine.mythic.core.mobs.MobExecutor").getMethod("getInternalName");
            this.isPresent = Class.forName("io.lumine.mythic.core.mobs.MobManager").getMethod("getMythicMob", String.class);
            this.available = true;
            plugin.getLogger().info("MythicMobs enlazado correctamente.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo enlazar MythicMobs (versión incompatible): " + e.getMessage(), e);
            this.available = false;
        }
    }

    public boolean isAvailable() { return available; }

    /** Verifica si un mob Mythic existe. */
    public boolean mobExists(String internalName) {
        if (!available) return false;
        try {
            Object mm = getMobManager.invoke(mythicProvider);
            Object mob = getMythicMobInstance.invoke(mm, internalName);
            return mob != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Spawnea un mob Mythic y devuelve la entidad. Devuelve null si falla. */
    public LivingEntity spawn(String internalName, Location loc, double level) {
        if (!available) return null;
        try {
            Object mm = getMobManager.invoke(mythicProvider);
            Object mob = getMythicMobInstance.invoke(mm, internalName);
            if (mob == null) return null;
            // MythicMob.spawn(LivingEntity, Location, double level, double mobLevel) -> ActiveMob
            Class<?> mythicMobClass = Class.forName("io.lumine.mythic.api.mobs.MythicMob");
            Method spawn = mythicMobClass.getMethod("spawn", org.bukkit.entity.LivingEntity.class, org.bukkit.Location.class, double.class, double.class);
            Object active = spawn.invoke(mob, (LivingEntity) null, loc, level, level);
            if (active == null) return null;
            Method getEntity = active.getClass().getMethod("getEntity");
            Object entity = getEntity.invoke(active);
            return entity instanceof LivingEntity le ? le : null;
        } catch (Exception e) {
            plugin.getLogger().warning("Error spawneando MythicMob '" + internalName + "': " + e.getMessage());
            return null;
        }
    }

    /** Devuelve el display name configurado en MythicMobs (Mob mob). */
    public String getDisplayName(String internalName) {
        if (!available) return internalName;
        try {
            Object mm = getMobManager.invoke(mythicProvider);
            Object mob = getMythicMobInstance.invoke(mm, internalName);
            if (mob == null) return internalName;
            Class<?> mobClass = Class.forName("io.lumine.mythic.api.mobs.MythicMob");
            Method getDisplay = mobClass.getMethod("getDisplayName");
            Object display = getDisplay.invoke(mob);
            if (display == null) return internalName;
            // Es un String con códigos legacy &, los limpiamos
            return display.toString().replaceAll("&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
        } catch (Exception e) {
            return internalName;
        }
    }
}
