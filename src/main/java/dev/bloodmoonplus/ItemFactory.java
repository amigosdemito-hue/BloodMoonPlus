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
    /** Rareza mínima (posición en loot.yml) que recibe Maldición de desaparición en armas,
     *  herramientas y armadura; -1 = desactivado. Por defecto RARO (posición 2). */
    private int vanishingMinRank = 2;

    ItemFactory(BloodMoonPlus plugin) { this.plugin = plugin; }

    void reload() {
        defs.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "items.yml"));
        showRarityLine = y.getBoolean("settings.show-rarity-line", true);
        maxEnchantLevel = Math.max(1, y.getInt("settings.max-enchant-level", 7));
        // Maldición de desaparición automática desde cierta rareza (OFF/NONE la desactiva).
        String vanishFrom = y.getString("settings.vanishing-curse-min-rarity", "RARO");
        if (vanishFrom == null || vanishFrom.isBlank()
                || Util.key(vanishFrom).equals("OFF") || Util.key(vanishFrom).equals("NONE")) {
            vanishingMinRank = -1;
        } else {
            vanishingMinRank = plugin.loot().rarityRank(vanishFrom);
            if (vanishingMinRank < 0) {
                plugin.getLogger().warning("items.yml: vanishing-curse-min-rarity '" + vanishFrom
                        + "' no existe en loot.yml (rarities); no se aplicará la maldición.");
            }
        }
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
            String rarity = Util.key(s.getString("rarity", "COMUN"));
            // Una rareza no definida en loot.yml es un agujero SILENCIOSO: el roll la
            // descarta al ponderar y la tabla suelta menos cosas sin ningún aviso.
            if (!plugin.loot().hasRarity(rarity)) {
                plugin.getLogger().warning("items.yml: rareza '" + rarity + "' de '" + id
                        + "' no existe en loot.yml (rarities); caerá como COMUN. Añádela a loot.yml.");
            }
            defs.put(id, new ItemDef(id, m, s.getString("name"), s.getStringList("lore"),
                    rarity, ench, s.getBoolean("unbreakable"),
                    s.getBoolean("glow"), s.getString("item-model"), s.getInt("limit", 0),
                    s.getStringList("abilities"), s.getStringList("passives")));
        }
        plugin.getLogger().info("Items cargados: " + defs.size());
    }

    /** Equipo que recibe la Maldición de desaparición: armas y herramientas (espadas, hachas,
     *  picos, palas, azadas, arcos, ballestas, tridentes y mazas) MÁS armadura (yelmo, peto,
     *  grebas, botas y élitros). Se compara por nombre de material para no depender de tags
     *  que cambian entre versiones de la API. Los escudos y los materiales quedan fuera. */
    private static boolean isGear(Material m) {
        if (m == null) return false;
        String n = m.name();
        return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_PICKAXE")
                || n.endsWith("_SHOVEL") || n.endsWith("_HOE")
                || n.equals("BOW") || n.equals("CROSSBOW") || n.equals("TRIDENT") || n.equals("MACE")
                || n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS")
                || n.endsWith("_BOOTS") || n.equals("ELYTRA");
    }

    public ItemDef get(String id) { return id == null ? null : defs.get(id); }

    /** ¿Este stack es un objeto creado por el plugin? (lo marca el PDC con el id del item). */
    public boolean isMoonItem(ItemStack s) {
        if (s == null || s.getType().isAir()) return false;
        ItemMeta m = s.getItemMeta();
        return m != null && m.getPersistentDataContainer().has(Keys.ITEM_ID, PersistentDataType.STRING);
    }

    /** Copia el resultado del yunque o de la herrería garantizando que no se pierde nada del
     *  original: encantamientos (con su nivel más alto), maldiciones, indestructibilidad y las
     *  claves de habilidad/rareza/ejemplar. Devuelve null si el resultado ya estaba bien. */
    public ItemStack applyGuarantees(ItemStack original, ItemStack result) {
        if (original == null || result == null || result.getType().isAir()) return null;
        ItemMeta om = original.getItemMeta();
        ItemMeta rm = result.getItemMeta();
        if (om == null || rm == null) return null;
        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> e : om.getEnchants().entrySet()) {
            if (rm.getEnchantLevel(e.getKey()) >= e.getValue()) continue;
            rm.addEnchant(e.getKey(), e.getValue(), true);
            changed = true;
        }
        var op = om.getPersistentDataContainer();
        var rp = rm.getPersistentDataContainer();
        for (NamespacedKey k : new NamespacedKey[]{Keys.ITEM_ID, Keys.ITEM_ABILITIES, Keys.ITEM_PASSIVES}) {
            String v = op.get(k, PersistentDataType.STRING);
            if (v == null || v.equals(rp.get(k, PersistentDataType.STRING))) continue;
            rp.set(k, PersistentDataType.STRING, v);
            changed = true;
        }
        Integer serial = op.get(Keys.ITEM_SERIAL, PersistentDataType.INTEGER);
        if (serial != null && !rp.has(Keys.ITEM_SERIAL, PersistentDataType.INTEGER)) {
            rp.set(Keys.ITEM_SERIAL, PersistentDataType.INTEGER, serial);
            changed = true;
        }
        if (om.isUnbreakable() && !rm.isUnbreakable()) {
            rm.setUnbreakable(true);
            changed = true;
        }
        if (!changed) return null;
        ItemStack out = result.clone();
        out.setItemMeta(rm);
        return out;
    }

    /** Pone al día un objeto ya existente con la definición ACTUAL de items.yml: añade los
     *  encantamientos, la maldición de desaparición y las habilidades que le falten. Nunca
     *  quita nada, no toca el nombre que le puso el jugador, ni el desgaste, ni el lore.
     *  Devuelve true si modificó el stack (que se edita in situ). */
    public boolean refresh(ItemStack stack) {
        if (!isMoonItem(stack)) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        ItemDef d = get(meta.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING));
        if (d == null) return false; // item de una config que ya no existe: se deja como está
        var pdc = meta.getPersistentDataContainer();
        boolean changed = false;

        for (Map.Entry<String, Integer> e : d.enchants().entrySet()) {
            Enchantment en = Reg.enchantment(e.getKey());
            if (en == null) continue;
            int want = Math.min(maxEnchantLevel, Math.max(1, e.getValue()));
            if (meta.getEnchantLevel(en) >= want) continue;
            try {
                meta.addEnchant(en, want, true);
            } catch (IllegalArgumentException ex) {
                // Encantamiento incompatible con el material: se ignora en vez de romper la
                // puesta al día (podría venir de un items.yml editado a mano).
                continue;
            }
            changed = true;
        }
        Enchantment vanish = Reg.enchantment("vanishing_curse");
        if (vanish != null && vanishingMinRank >= 0 && isGear(d.material())
                && plugin.loot().rarityRank(d.rarity()) >= vanishingMinRank
                && !meta.hasEnchant(vanish)) {
            try {
                meta.addEnchant(vanish, 1, true);
                changed = true;
            } catch (IllegalArgumentException ignored) { }
        }
        changed |= addMissing(pdc, Keys.ITEM_ABILITIES, d.abilities());
        changed |= addMissing(pdc, Keys.ITEM_PASSIVES, d.passives());
        if (d.unbreakable() && !meta.isUnbreakable()) {
            meta.setUnbreakable(true);
            changed = true;
        }
        if (d.glow() && meta.getEnchantmentGlintOverride() == null) {
            meta.setEnchantmentGlintOverride(true);
            changed = true;
        }
        if (pdc.get(Keys.ITEM_RARITY, PersistentDataType.STRING) == null) {
            pdc.set(Keys.ITEM_RARITY, PersistentDataType.STRING, d.rarity());
            changed = true;
        }
        if (meta.displayName() == null && d.name() != null) {
            meta.displayName(Util.item(d.name()));
            changed = true;
        }
        if (changed) stack.setItemMeta(meta);
        return changed;
    }

    /** Pone al día TODOS los objetos de la Luna de un jugador: inventario, armadura, mano
     *  secundaria y cofre del End. Devuelve cuántos objetos cambiaron. Se usa al entrar y en
     *  /luna refresh. */
    public int refreshPlayer(org.bukkit.entity.Player p) {
        int changed = 0;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (!refresh(contents[i])) continue;
            p.getInventory().setItem(i, contents[i]);
            changed++;
        }
        ItemStack[] armor = p.getInventory().getArmorContents();
        boolean armorChanged = false;
        for (ItemStack a : armor) {
            if (!refresh(a)) continue;
            armorChanged = true;
            changed++;
        }
        if (armorChanged) p.getInventory().setArmorContents(armor);
        ItemStack off = p.getInventory().getItemInOffHand();
        if (refresh(off)) {
            p.getInventory().setItemInOffHand(off);
            changed++;
        }
        ItemStack[] ender = p.getEnderChest().getContents();
        for (int i = 0; i < ender.length; i++) {
            if (!refresh(ender[i])) continue;
            p.getEnderChest().setItem(i, ender[i]);
            changed++;
        }
        return changed;
    }

    /** Une las habilidades que ya tiene el item con las de la definición (sin quitar ninguna). */
    private static boolean addMissing(org.bukkit.persistence.PersistentDataContainer pdc,
                                      NamespacedKey key, List<String> wanted) {
        if (wanted.isEmpty()) return false;
        String current = pdc.get(key, PersistentDataType.STRING);
        Set<String> have = new LinkedHashSet<>();
        if (current != null && !current.isBlank()) have.addAll(Arrays.asList(current.split(";")));
        int before = have.size();
        for (String w : wanted) if (!w.isBlank()) have.add(w);
        if (have.size() == before) return false;
        pdc.set(key, PersistentDataType.STRING, String.join(";", have));
        return true;
    }

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
        // Maldición de desaparición obligatoria en ARMAS, HERRAMIENTAS y ARMADURA de rareza
        // alta (Raro en adelante por defecto): si mueres con ellas se pierden, como corresponde
        // a su rareza. Ajustable con settings.vanishing-curse-min-rarity (OFF la desactiva).
        Enchantment vanish = Reg.enchantment("vanishing_curse");
        if (vanish != null && vanishingMinRank >= 0 && isGear(d.material())
                && plugin.loot().rarityRank(d.rarity()) >= vanishingMinRank) {
            meta.addEnchant(vanish, 1, true);
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
