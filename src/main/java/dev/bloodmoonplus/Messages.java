package dev.bloodmoonplus;

import dev.bloodmoonplus.i18n.LocaleManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Capa de compatibilidad que delega en LocaleManager. Mantiene la API que el resto del código
 * ya usa (send, world, get, prefixed) sin tener que reescribir todo de golpe.
 */
public final class Messages {
    private final BloodMoonPlus plugin;
    private final LocaleManager locales;

    Messages(BloodMoonPlus plugin, LocaleManager locales) {
        this.plugin = plugin;
        this.locales = locales;
    }

    void reload() {
        locales.reload();
    }

    public String raw(String key) {
        return locales.get(key);
    }

    public String fill(String text, String... kv) {
        return LocaleManager.fill(text, kv);
    }

    public Component get(String key, String... kv) {
        return Util.mini(locales.get(key, kv));
    }

    public Component prefixed(String key, String... kv) {
        return Util.mini(locales.get("prefix") + locales.get(key, kv));
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(prefixed(key, kv));
    }

    public void world(World w, String key, String... kv) {
        Component c = prefixed(key, kv);
        for (Player p : w.getPlayers()) p.sendMessage(c);
    }

    public void all(String key, String... kv) {
        Bukkit.broadcast(prefixed(key, kv));
    }
}
