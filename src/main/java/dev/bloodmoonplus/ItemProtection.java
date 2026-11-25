package dev.bloodmoonplus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    /** Ranuras con una puesta al día ya encolada (uuid:slot): un solo clic dispara varios eventos. */
    private final Set<String> refreshQueued = new HashSet<>();

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

    /** Un objeto de la Luna recogido del suelo se pone al día ANTES de que entre al inventario:
     *  se edita el stack del item que está en el suelo, así que no se reescribe ninguna ranura
     *  del jugador ni el cursor. Antes había que reconectar o esperar a `/luna refresh`. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        ItemStack stack = e.getItem().getItemStack();
        if (!plugin.items().isMoonItem(stack)) return;
        if (plugin.items().refresh(stack)) e.getItem().setItemStack(stack);
    }

    /** Clic que deja el objeto en el inventario del jugador: se pone al día SOLO la ranura en la
     *  que queda, en el tick siguiente. El cursor no se toca (escribirlo a mitad de un clic
     *  desincroniza la pantalla del cliente) y no se recorre el inventario entero, así que el
     *  resto de ranuras no se reescribe nunca. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        boolean moonCurrent = plugin.items().isMoonItem(e.getCurrentItem());
        if (!moonCurrent && !plugin.items().isMoonItem(e.getCursor())) return;
        if (e.getClickedInventory() instanceof PlayerInventory) {
            refreshSlotsSoon(p, e.getView(), List.of(e.getRawSlot()));
            return;
        }
        // Clic en un cofre, Shulker o cofre del End: con un clic normal el objeto se queda en el
        // cursor (y ahí no se reescribe nada), pero con shift-clic o con la tecla de hotbar salta
        // al inventario del jugador y no hay forma barata de saber a qué ranura, así que en ese
        // caso se pone al día el inventario una vez. Es una acción suelta, no el clic de cada día.
        if (!moonCurrent) return;
        switch (e.getAction()) {
            case MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> refreshPlayerSoon(p);
            default -> { }
        }
    }

    /** Encola UNA puesta al día completa (inventario, armadura, mano secundaria y cofre del End)
     *  para el tick siguiente: el shift-clic desde un cofre ya ha terminado y así no se escribe
     *  inventario a mitad de un clic. */
    private void refreshPlayerSoon(Player p) {
        if (!refreshQueued.add(p.getUniqueId().toString())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            refreshQueued.remove(p.getUniqueId().toString());
            if (p.isOnline()) plugin.items().refreshPlayer(p);
        });
    }

    /** Encola UNA puesta al día por ranura (slots crudos de la vista) para el tick siguiente:
     *  escribir el inventario a mitad de un clic desincroniza la pantalla del cliente, y un clic
     *  genera varios eventos seguidos. */
    private void refreshSlotsSoon(Player p, InventoryView view, List<Integer> rawSlots) {
        for (int raw : rawSlots) {
            if (raw < 0 || raw >= view.countSlots()) continue;
            String key = p.getUniqueId() + ":" + raw;
            if (!refreshQueued.add(key)) continue;
            Bukkit.getScheduler().runTask(plugin, () -> {
                refreshQueued.remove(key);
                if (!p.isOnline()) return;
                ItemStack it = view.getItem(raw);
                if (plugin.items().refresh(it)) view.setItem(raw, it);
            });
        }
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
