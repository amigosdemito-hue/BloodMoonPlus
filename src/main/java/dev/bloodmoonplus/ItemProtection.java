package dev.bloodmoonplus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Protege los objetos de la Luna de Sangre.
 *
 * Reglas:
 *  - NO se pueden quitar encantamientos, maldiciones, efectos ni habilidades (ni el id del item).
 *  - SÍ se pueden AÑADIR encantamientos (mesa de encantamientos, yunques con libros, herrería).
 *  - SÍ se pueden renombrar.
 *
 * El grindstone es el único método vanilla para QUITAR encantamientos y maldiciones (por eso
 * servía para limpiar la Maldición de desaparición), así que ahí se anula el resultado. En el
 * yunque y en la mesa de herrería, que solo pueden conservar o añadir, se repara el resultado
 * por si alguna combinación dejaba el objeto con menos encantamientos que el original.
 */
public final class ItemProtection implements Listener {

    private final BloodMoonPlus plugin;
    /** Aviso del grindstone con enfriamiento: el evento se dispara varias veces seguidas. */
    private final Map<UUID, Long> warned = new HashMap<>();

    ItemProtection(BloodMoonPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGrindstone(PrepareGrindstoneEvent e) {
        ItemStack top = e.getInventory().getItem(0);
        ItemStack bottom = e.getInventory().getItem(1);
        if (!plugin.items().isMoonItem(top) && !plugin.items().isMoonItem(bottom)) return;
        // El resultado sería el objeto "limpio" (sin encantamientos ni maldiciones): se anula.
        boolean wouldStrip = e.getResult() != null && !e.getResult().getType().isAir();
        e.setResult(null);
        if (!wouldStrip) return;
        if (e.getView().getPlayer() instanceof Player p) {
            long now = System.currentTimeMillis();
            if (now - warned.getOrDefault(p.getUniqueId(), 0L) < 3000L) return;
            if (warned.size() > 500) warned.values().removeIf(t -> now - t > 60_000L);
            warned.put(p.getUniqueId(), now);
            plugin.messages().send(p, "item.no-disenchant");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack base = e.getInventory().getItem(0);
        if (!plugin.items().isMoonItem(base)) return;
        ItemStack result = e.getResult();
        if (result == null) return;
        ItemStack fixed = plugin.items().applyGuarantees(base, result);
        if (fixed != null) e.setResult(fixed);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithing(PrepareSmithingEvent e) {
        ItemStack base = e.getInventory().getItem(1);
        if (!plugin.items().isMoonItem(base)) return;
        ItemStack result = e.getResult();
        if (result == null) return;
        ItemStack fixed = plugin.items().applyGuarantees(base, result);
        if (fixed != null) e.setResult(fixed);
    }

    /** Pone al día los objetos que el jugador ya tenía (incluso renombrados) con la definición
     *  actual de items.yml: encantamientos, maldición de desaparición y habilidades que falten.
     *  Nunca quita nada, así que es seguro repetirlo en cada entrada. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // Un par de segundos después: el inventario del jugador ya está cargado del todo.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.items().refreshPlayer(p) > 0) plugin.messages().send(p, "item.updated");
        }, 40L);
    }
}
