package dev.bloodmoonplus;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

/** Definiciones de items (items.yml) y construcción de ItemStacks con habilidades y números de serie. */
public final class ItemFactory {

    public record ItemDef(String id, Material material, String name, List<String> lore, String rarity,
                          Map<String, Integer> enchants, boolean unbreakable, boolean glow, String itemModel,
                          int limit, List<String> abilities, List<String> passives) {}

    private final BloodMoonPlus plugin;
    private final Map<String, ItemDef> defs = new LinkedHashMap<>();
    private boolean showRarityLine = true;
    /** Nivel máximo de encantamiento en items (los que superen el límite se recortan al cargar). */
    private int maxEnchantLevel = 7;

    ItemFactory(BloodMoonPlus plugin) { this.plugin = plugin; }

    void reload() {
        defs.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "items.yml"));
        showRarityLine = y.getBoolean("settings.show-rarity-line", true);
        maxEnchantLevel = Math.max(1, y.getInt("settings.max-enchant-level", 7));
        ConfigurationSection sec = y.getConfigurationSection("items");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            Material m = Material.matchMaterial(s.getString("material", "STONE"));
            if (m == null || m.isAir() || !m.isItem()) {
                plugin.getLogger().warning("items.yml: material inválido en '" + id + "'");
                continue;
            }
            Map<String, Integer> ench = new LinkedHashMap<>();
            ConfigurationSection es = s.getConfigurationSection("enchants");
            if (es != null) for (String k : es.getKeys(false)) ench.put(k, es.getInt(k));
            defs.put(id, new ItemDef(id, m, s.getString("name"), s.getStringList("lore"),
                    Util.key(s.getString("rarity", "COMUN")), ench, s.getBoolean("unbreakable"),
                    s.getBoolean("glow"), s.getString("item-model"), s.getInt("limit", 0),
                    s.getStringList("abilities"), s.getStringList("passives")));
        }
        plugin.getLogger().info("Items cargados: " + defs.size());
    }

    public ItemDef get(String id) { return id == null ? null : defs.get(id); }

    public Set<String> ids() { return defs.keySet(); }

    public boolean available(ItemDef d) { return d.limit() <= 0 || plugin.data().created(d.id()) < d.limit(); }

    /** Crea un item respetando el límite global (items únicos). Devuelve null si ya se agotó. */
    public ItemStack create(ItemDef d, int amount, String owner) {
        int serial = 0;
        if (d.limit() > 0) {
            if (plugin.data().created(d.id()) >= d.limit()) return null;
            serial = plugin.data().nextSerial(d.id());
        }
        return build(d, amount, owner, serial);
    }

    public ItemStack build(ItemDef d, int amount, String owner, int serial) {
        ItemStack stack = new ItemStack(d.material(), Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        String rarityDisp = plugin.loot().rarityDisplay(d.rarity());
        Messages msg = plugin.messages();

        if (d.name() != null) meta.displayName(Util.item(d.name()));

        List<Component> lore = new ArrayList<>();
        for (String line : d.lore()) {
            lore.add(Util.item(line.replace("{owner}", owner == null ? "?" : owner).replace("{rarity}", rarityDisp)));
        }
        if (showRarityLine && d.name() != null) {
            lore.add(Component.empty());
            lore.add(Util.item(msg.fill(msg.raw("item.rarity-line"), "rarity", rarityDisp)));
        }
        if (d.limit() > 0 && serial > 0) {
            lore.add(Util.item(msg.fill(msg.raw("item.serial-line"), "serial", String.valueOf(serial), "limit", String.valueOf(d.limit()))));
        }
        if (!lore.isEmpty()) meta.lore(lore);

        for (Map.Entry<String, Integer> e : d.enchants().entrySet()) {
            Enchantment en = Reg.enchantment(e.getKey());
            if (en == null) {
                plugin.getLogger().warning("items.yml: encantamiento desconocido '" + e.getKey() + "' en " + d.id());
                continue;
            }
            // Nivel de encantamiento recortado al máximo permitido (7 por defecto)
            meta.addEnchant(en, Math.min(maxEnchantLevel, Math.max(1, e.getValue())), true);
        }
        if (d.unbreakable()) meta.setUnbreakable(true);
        if (d.glow()) meta.setEnchantmentGlintOverride(true);
        if (d.itemModel() != null) {
            NamespacedKey k = NamespacedKey.fromString(d.itemModel());
            if (k != null) meta.setItemModel(k);
        }

        var pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.ITEM_ID, PersistentDataType.STRING, d.id());
        pdc.set(Keys.ITEM_RARITY, PersistentDataType.STRING, d.rarity());
        if (serial > 0) pdc.set(Keys.ITEM_SERIAL, PersistentDataType.INTEGER, serial);
        if (!d.abilities().isEmpty()) pdc.set(Keys.ITEM_ABILITIES, PersistentDataType.STRING, String.join(";", d.abilities()));
        if (!d.passives().isEmpty()) pdc.set(Keys.ITEM_PASSIVES, PersistentDataType.STRING, String.join(";", d.passives()));
        stack.setItemMeta(meta);
        return stack;
    }
}
