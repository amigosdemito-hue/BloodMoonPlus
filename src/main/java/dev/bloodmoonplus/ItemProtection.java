package dev.bloodmoonplus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    /** Jugadores con una puesta al día ya encolada: un solo clic dispara varios eventos. */
    private final Set<UUID> refreshQueued = new HashSet<>();

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

    /** Un objeto de la Luna recogido del suelo se pone al día en el mismo tick en que entra al
     *  inventario: antes había que reconectar o esperar a `/luna refresh`. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!plugin.items().isMoonItem(e.getItem().getItemStack())) return;
        refreshSoon(p);
    }

    /** Sacarlo de un cofre o shulker, o moverlo dentro del inventario, también cuenta: ese clic
     *  deja el objeto en el cursor o en otra ranura y se refresca en el tick siguiente. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!plugin.items().isMoonItem(e.getCurrentItem()) && !plugin.items().isMoonItem(e.getCursor())) return;
        refreshSoon(p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        for (ItemStack it : e.getNewItems().values()) {
            if (!plugin.items().isMoonItem(it)) continue;
            refreshSoon(p);
            return;
        }
    }

    /** Encola UNA puesta al día para el tick siguiente: tocar el inventario a mitad de un evento
     *  de clic desincroniza la pantalla del cliente, y un clic genera varios eventos seguidos. */
    private void refreshSoon(Player p) {
        if (!refreshQueued.add(p.getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            refreshQueued.remove(p.getUniqueId());
            if (!p.isOnline()) return;
            ItemStack cursor = p.getItemOnCursor();
            if (plugin.items().refresh(cursor)) p.setItemOnCursor(cursor);
            plugin.items().refreshPlayer(p);
        });
    }

    /** Efectos que aplicaban los viejos pasivos de item (1.0.2) con duración gigante. Ya no
     *  existe nada que los renueve, así que sin esta limpieza se quedarían pegados para
     *  siempre. Solo se quitan los de duración enorme: una poción o un faro legítimos duran
     *  mucho menos. */
    private static final String[] LEGACY_PASSIVES = {"speed", "strength", "resistance",
            "health_boost", "regeneration", "haste", "night_vision", "slow_falling", "jump_boost"};

    private static void clearLegacyPassives(Player p) {
        for (String key : LEGACY_PASSIVES) {
            var type = Reg.effect(key);
            if (type == null) continue;
            var effect = p.getPotionEffect(type);
            if (effect != null && effect.getDuration() > 20 * 60 * 20) p.removePotionEffect(type);
        }
    }

    /** Pone al día los objetos que el jugador ya tenía (incluso renombrados) con la definición
     *  actual de items.yml: encantamientos, maldición de desaparición y habilidades que falten.
     *  También borra los pasivos viejos; es seguro repetirlo en cada entrada. */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        clearLegacyPassives(p);
        if (!plugin.items().migrateOnJoin()) return;
        // Un par de segundos después: el inventario del jugador ya está cargado del todo.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.items().refreshPlayer(p) > 0) plugin.messages().send(p, "item.updated");
        }, 40L);
    }
}
