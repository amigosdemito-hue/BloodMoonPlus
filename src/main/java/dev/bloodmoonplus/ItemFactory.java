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

/** Definiciones de items (items.yml) y construcción de ItemStacks con encantamientos y números de serie. */
public final class ItemFactory {

    public record ItemDef(String id, Material material, String name, List<String> lore, String rarity,
                          Map<String, Integer> enchants, boolean unbreakable, boolean glow, String itemModel,
                          int limit) {}

    private final BloodMoonPlus plugin;
    private final Map<String, ItemDef> defs = new LinkedHashMap<>();
    private boolean showRarityLine = true;
    /** Si la puesta al día al entrar también migra items antiguos (niveles y habilidades). */
    private boolean migrateOnJoin = true;
    /** Claves heredadas de 1.0.2 y anteriores: sus datos ya no se usan y se borran al migrar. */
    private final NamespacedKey legacyAbilities, legacyPassives;
    ItemFactory(BloodMoonPlus plugin) {
        this.plugin = plugin;
        this.legacyAbilities = new NamespacedKey(plugin, "abilities");
        this.legacyPassives = new NamespacedKey(plugin, "passives");
    }

    void reload() {
        defs.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "items.yml"));
        showRarityLine = y.getBoolean("settings.show-rarity-line", true);
        migrateOnJoin = plugin.getConfig().getBoolean("items.migrate-on-join", true);
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
                    s.getBoolean("glow"), s.getString("item-model"), s.getInt("limit", 0)));
        }
        warnUnknownEnchants();
        warnUnsupportedEnchants();
        plugin.getLogger().info("Items cargados: " + defs.size());
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
        String id = op.get(Keys.ITEM_ID, PersistentDataType.STRING);
        if (id != null && !id.equals(rp.get(Keys.ITEM_ID, PersistentDataType.STRING))) {
            rp.set(Keys.ITEM_ID, PersistentDataType.STRING, id);
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

    /** Pone al día un objeto ya existente con la definición ACTUAL de items.yml: ajusta los
     *  encantamientos al nivel pedido (sube los que falten y baja los que superen el máximo del
     *  item), quita la maldición de desaparición de la 1.0.2 y borra las claves de las viejas
     *  habilidades. No toca el nombre que le puso el jugador, ni el desgaste, ni el lore.
     *  Devuelve true si modificó el stack (que se edita in situ). */
    public boolean refresh(ItemStack stack) {
        if (!isMoonItem(stack)) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        var pdc = meta.getPersistentDataContainer();
        boolean changed = false;
        // Ningún objeto de la Luna lleva maldiciones: a los repartidos con la Maldición de
        // desaparición que ponía la 1.0.2 se les quita en la siguiente puesta al día (entrar,
        // recogerlo del suelo, sacarlo de un cofre o /luna refresh). Se toca SOLO esa maldición:
        // si un jugador añadió otra cosa en el yunque, se queda. Va antes de resolver la
        // definición para que también se limpie un objeto cuyo id ya no exista en items.yml.
        Enchantment vanish = Reg.enchantment("vanishing_curse");
        if (vanish != null && meta.hasEnchant(vanish)) {
            meta.removeEnchant(vanish);
            changed = true;
        }
        ItemDef d = get(pdc.get(Keys.ITEM_ID, PersistentDataType.STRING));
        if (d == null) { // item de una config que ya no existe: solo se le quita la maldición
            if (changed) stack.setItemMeta(meta);
            return changed;
        }

        // Puesta al día de encantamientos: sube los que falten Y baja los que estén por encima
        // del máximo del item (antes se recortaba a 7, así que "filo 7" o "protección 7"
        // sobrevivían). En vanilla el máximo es el real o 5 en legendarios y míticos; en
        // ExcellentEnchants, el que fije su plugin.
        for (Map.Entry<String, Integer> e : d.enchants().entrySet()) {
            Enchantment en = Reg.enchantment(e.getKey());
            if (en == null) continue;
            int want = cappedLevel(d, en, e.getValue());
            if (meta.getEnchantLevel(en) == want) continue;
            try {
                meta.addEnchant(en, want, true);
            } catch (IllegalArgumentException ex) {
                // Encantamiento incompatible con el material: se ignora en vez de romper la
                // puesta al día (podría venir de un items.yml editado a mano).
                continue;
            }
            changed = true;
        }
        // Migración: los datos de las viejas habilidades de item (1.0.2 y antes) ya no se usan.
        for (NamespacedKey legacy : new NamespacedKey[]{legacyAbilities, legacyPassives}) {
            if (!pdc.has(legacy, PersistentDataType.STRING)) continue;
            pdc.remove(legacy);
            changed = true;
        }
        if (d.unbreakable() && !meta.isUnbreakable()) {
            meta.setUnbreakable(true);
            changed = true;
        }
        if (d.glow() && !meta.hasEnchantmentGlintOverride()) {
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

    /** ¿La rareza es LEGENDARIO o MITICO? Se compara por posición en loot.yml para que una
     *  rareza nueva por encima de leyenda también cuente. */
    private boolean isHighRarity(String rarity) {
        int legend = plugin.loot().rarityRank("LEGENDARIO");
        return legend >= 0 && plugin.loot().rarityRank(rarity) >= legend;
    }

    /** Nivel máximo que puede llevar un encantamiento en este item: los vanilla de un legendario
     *  o mítico llegan a 5 aunque el máximo de vanilla sea menor (protección 5, espinas 5...);
     *  los de ExcellentEnchants se quedan en el que fija su plugin. */
    private int cappedLevel(ItemDef d, Enchantment en, int want) {
        int max = en.getMaxLevel();
        if (!Reg.isCustom(en) && isHighRarity(d.rarity())) max = Math.max(max, 5);
        return Math.max(1, Math.min(want, max));
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

    /** Avisa UNA vez de los encantamientos de items.yml que el servidor no conoce: casi
     *  siempre significa que falta ExcellentEnchants (o que el id no existe en su versión). */
    private void warnUnknownEnchants() {
        Set<String> unknown = new TreeSet<>();
        for (ItemDef d : defs.values()) {
            for (String key : d.enchants().keySet()) {
                if (Reg.enchantment(key) == null) unknown.add(key);
            }
        }
        if (unknown.isEmpty()) return;
        plugin.getLogger().warning("items.yml: encantamientos que este servidor no conoce: " + unknown
                + " (los encantamientos custom necesitan ExcellentEnchants instalado;"
                + " el resto de encantamientos del item se aplica igual). ATENCIÓN: Minecraft borra los"
                + " items con encantamientos inexistentes, así que los items ya repartidos con"
                + " encantamientos de EE se perderán al entrar sus dueños. Reinstala ExcellentEnchants"
                + " y ejecuta /luna refresh <jugador|all> antes de retirarlo.");
    }

    /** Avisa de los encantamientos de items.yml que el MATERIAL no acepta (por ejemplo Tunnel en
     *  un hacha, o Dragon Heart en unas botas). Minecraft los omite al crear el objeto, así que
     *  el item salía sin ellos y en silencio; ahora queda en el log al arrancar. */
    private void warnUnsupportedEnchants() {
        Set<String> bad = new TreeSet<>();
        for (ItemDef d : defs.values()) {
            ItemStack sample = new ItemStack(d.material());
            for (String key : d.enchants().keySet()) {
                Enchantment en = Reg.enchantment(key);
                if (en == null || en.canEnchantItem(sample)) continue;
                bad.add(d.id() + "/" + key + " (" + d.material() + ")");
            }
        }
        if (bad.isEmpty()) return;
        plugin.getLogger().warning("items.yml: encantamientos que no se pueden aplicar al material y se omiten: " + bad);
    }

    /** ¿La puesta al día al entrar debe migrar items antiguos? (config items.migrate-on-join). */
    public boolean migrateOnJoin() { return migrateOnJoin; }

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
            if (en == null) continue; // ya se avisó al recargar items.yml
            try {
                // Nunca por encima del máximo del item: el real de vanilla (5 en legendarios y
                // míticos) o el que fije ExcellentEnchants para los suyos.
                meta.addEnchant(en, cappedLevel(d, en, e.getValue()), true);
            } catch (IllegalArgumentException ex) {
                // Incompatible con el material: se salta ese encantamiento, no el item entero.
            }
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
        stack.setItemMeta(meta);
        return stack;
    }
}
