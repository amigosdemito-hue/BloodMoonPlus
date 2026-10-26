package dev.bloodmoonplus;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Sistema de loot por rareza: tablas, pesos, suerte por dificultad, pity (protección contra mala suerte) y límites globales. */
public final class LootManager {

    public record Rarity(String id, String display, double weight, boolean luck, boolean broadcast,
                         double pityPerRoll, int pityMax) {}

    record Entry(String item, double weight, int[] amount, double dropChance) {}

    static final class Table {
        String id;
        double dropChance = 1.0;
        int[] rolls = {1, 1};
        final Map<String, Double> rarityWeights = new HashMap<>();
        List<Entry> entries = new ArrayList<>();
        List<Entry> guaranteed = new ArrayList<>();
        List<Entry> effective = new ArrayList<>();
        String inherits;
    }

    private final BloodMoonPlus plugin;
    private final Map<String, Rarity> rarities = new LinkedHashMap<>();
    private final Map<String, Table> tables = new LinkedHashMap<>();

    LootManager(BloodMoonPlus plugin) { this.plugin = plugin; }

    void reload() {
        rarities.clear();
        tables.clear();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "loot.yml"));

        ConfigurationSection rs = y.getConfigurationSection("rarities");
        if (rs != null) {
            for (String id : rs.getKeys(false)) {
                ConfigurationSection s = rs.getConfigurationSection(id);
                if (s == null) continue;
                String key = Util.key(id);
                rarities.put(key, new Rarity(key, s.getString("display", id), s.getDouble("weight", 1),
                        s.getBoolean("luck-affected"), s.getBoolean("broadcast"),
                        s.getDouble("pity-per-roll", 0), s.getInt("pity-max-stacks", 200)));
            }
        }

        ConfigurationSection ts = y.getConfigurationSection("tables");
        if (ts != null) {
            for (String id : ts.getKeys(false)) {
                ConfigurationSection s = ts.getConfigurationSection(id);
                if (s == null) continue;
                Table t = new Table();
                t.id = id;
                t.dropChance = s.getDouble("drop-chance", 1.0);
                t.rolls = Util.range(s.get("rolls"), 1, 1);
                t.inherits = s.getString("inherits");
                ConfigurationSection rw = s.getConfigurationSection("rarity-weights");
                if (rw != null) for (String k : rw.getKeys(false)) t.rarityWeights.put(Util.key(k), rw.getDouble(k));
                t.entries = parseEntries(s.getMapList("entries"));
                t.guaranteed = parseEntries(s.getMapList("guaranteed"));
                tables.put(id, t);
            }
            for (Table t : tables.values()) t.effective = resolved(t, new HashSet<>());
        }
        plugin.getLogger().info("Loot: " + rarities.size() + " rarezas, " + tables.size() + " tablas.");
    }

    private List<Entry> parseEntries(List<Map<?, ?>> list) {
        List<Entry> out = new ArrayList<>();
        for (Map<?, ?> m : list) {
            String item = Util.str(m, "item", null);
            if (item == null) continue;
            double dropChance = Util.num(m, "drop-chance", 1.0);
            out.add(new Entry(item, Util.num(m, "weight", 1), Util.range(m.get("amount"), 1, 1), Math.max(0, Math.min(1, dropChance))));
        }
        return out;
    }

    private List<Entry> resolved(Table t, Set<String> seen) {
        List<Entry> out = new ArrayList<>();
        if (t.inherits != null && seen.add(t.id)) {
            Table p = tables.get(t.inherits);
            if (p != null) out.addAll(resolved(p, seen));
        }
        out.addAll(t.entries);
        return out;
    }

    public String rarityDisplay(String id) {
        Rarity r = rarities.get(id);
        return r == null ? id : r.display();
    }

    public Set<String> tableIds() { return tables.keySet(); }

    public boolean hasTable(String id) { return tables.containsKey(id); }

    /** Todas las tablas cargadas (para placeholders). */
    public Collection<Table> allTables() { return tables.values(); }

    /** Tira una tabla. applyDropChance=true usa la probabilidad base de la tabla (para mobs). */
    public List<ItemStack> roll(String tableId, Player player, double diff, boolean applyDropChance) {
        List<ItemStack> out = new ArrayList<>();
        Table t = tables.get(tableId);
        if (t == null) return out;
        String owner = player != null ? player.getName() : "?";

        for (Entry g : t.guaranteed) {
            ItemFactory.ItemDef d = plugin.items().get(g.item());
            if (d == null) continue;
            ItemStack s = plugin.items().create(d, Util.between(g.amount()), owner);
            if (s != null) out.add(s);
        }
        if (applyDropChance) {
            double c = Math.min(1.0, t.dropChance * plugin.scaled(diff, "loot-chance"));
            if (ThreadLocalRandom.current().nextDouble() >= c) return out;
        }
        int rolls = Util.between(t.rolls);
        for (int i = 0; i < rolls; i++) {
            ItemStack s = rollOnce(t, player, diff, owner);
            if (s != null) out.add(s);
        }
        return out;
    }

    private ItemStack rollOnce(Table t, Player player, double diff, String owner) {
        Map<String, List<Entry>> byRarity = new LinkedHashMap<>();
        for (Entry e : t.effective) {
            ItemFactory.ItemDef d = plugin.items().get(e.item());
            if (d == null || e.weight() <= 0 || !plugin.items().available(d)) continue;
            byRarity.computeIfAbsent(d.rarity(), k -> new ArrayList<>()).add(e);
        }
        if (byRarity.isEmpty()) return null;

        double luckMult = plugin.scaled(diff, "rarity-luck");
        UUID uid = player != null ? player.getUniqueId() : null;
        Map<String, Double> weights = new LinkedHashMap<>();
        double total = 0;
        for (String rid : byRarity.keySet()) {
            Rarity ra = rarities.get(rid);
            if (ra == null) continue;
            double w = t.rarityWeights.getOrDefault(rid, ra.weight());
            if (w <= 0) continue;
            if (ra.luck()) w *= luckMult;
            if (uid != null && ra.pityPerRoll() > 0) w *= 1 + plugin.data().pity(uid, rid) * ra.pityPerRoll();
            weights.put(rid, w);
            total += w;
        }
        if (total <= 0) return null;

        double r = ThreadLocalRandom.current().nextDouble() * total;
        String chosen = null;
        String last = null;
        for (Map.Entry<String, Double> en : weights.entrySet()) {
            last = en.getKey();
            r -= en.getValue();
            if (r <= 0) { chosen = en.getKey(); break; }
        }
        if (chosen == null) chosen = last;

        if (uid != null) {
            for (String rid : byRarity.keySet()) {
                Rarity ra = rarities.get(rid);
                if (ra == null || ra.pityPerRoll() <= 0) continue;
                if (rid.equals(chosen)) plugin.data().setPity(uid, rid, 0);
                else plugin.data().setPity(uid, rid, Math.min(ra.pityMax(), plugin.data().pity(uid, rid) + 1));
            }
        }

        Entry e = Util.pick(byRarity.get(chosen), Entry::weight);
        if (e == null) return null;
        // Drop-chance individual por item: los simples caen siempre, los rotos son raros
        if (e.dropChance() < 1.0 && ThreadLocalRandom.current().nextDouble() >= e.dropChance()) return null;
        ItemFactory.ItemDef d = plugin.items().get(e.item());
        ItemStack stack = plugin.items().create(d, Util.between(e.amount()), owner);
        if (stack == null) return null;

        Rarity ra = rarities.get(chosen);
        if (ra != null && ra.broadcast() && player != null) {
            plugin.messages().all("loot.broadcast", "player", player.getName(),
                    "item", d.name() != null ? d.name() : d.material().name(), "rarity", ra.display());
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        return stack;
    }

    public void giveOrDrop(Player p, ItemStack it) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        for (ItemStack l : left.values()) dropAt(p.getLocation(), l);
    }

    /** Raridades que deben ir directo al inventario, no suelta al suelo. */
    public boolean directToInventory(String rarity) {
        return Util.key(rarity).equalsIgnoreCase(Util.key("RARO"))
                || Util.key(rarity).equalsIgnoreCase(Util.key("EPICO"))
                || Util.key(rarity).equalsIgnoreCase(Util.key("LEGENDARIO"))
                || Util.key(rarity).equalsIgnoreCase(Util.key("MITICO"));
    }

    /** Suelta el item; los de rarezas altas brillan y no se destruyen. */
    public void dropAt(Location loc, ItemStack it) {
        Item drop = loc.getWorld().dropItemNaturally(loc, it);
        if (!it.hasItemMeta()) return;
        String rarity = it.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_RARITY, PersistentDataType.STRING);
        if (rarity != null && plugin.getConfig().getStringList("loot.glow-rarities").stream().anyMatch(s -> Util.key(s).equals(rarity))) {
            drop.setGlowing(true);
            drop.setInvulnerable(true);
        }
    }

    /** Porcentajes reales de una tabla a dificultad 1 y sin pity (para /bloodmoon chances). */
    public List<String> describe(String id) {
        Table t = tables.get(id);
        if (t == null) return null;
        List<String> lines = new ArrayList<>();
        lines.add("<gold>Tabla <white>" + id + " <gray>· prob. base de drop: <white>"
                + String.format(Locale.ROOT, "%.1f%%", t.dropChance * 100) + " <gray>· tiradas: <white>" + t.rolls[0] + "-" + t.rolls[1]);
        Map<String, List<Entry>> byRarity = new LinkedHashMap<>();
        for (Entry e : t.effective) {
            ItemFactory.ItemDef d = plugin.items().get(e.item());
            if (d == null || e.weight() <= 0) continue;
            byRarity.computeIfAbsent(d.rarity(), k -> new ArrayList<>()).add(e);
        }
        double total = 0;
        Map<String, Double> w = new LinkedHashMap<>();
        for (String rid : byRarity.keySet()) {
            Rarity ra = rarities.get(rid);
            if (ra == null) continue;
            double v = t.rarityWeights.getOrDefault(rid, ra.weight());
            if (v <= 0) continue;
            w.put(rid, v);
            total += v;
        }
        for (Map.Entry<String, Double> en : w.entrySet()) {
            double pct = en.getValue() / total * 100;
            lines.add(" " + rarityDisplay(en.getKey()) + " <white>" + String.format(Locale.ROOT, "%.2f%%", pct));
            List<Entry> es = byRarity.get(en.getKey());
            double sum = 0;
            for (Entry e : es) sum += e.weight();
            for (Entry e : es) {
                ItemFactory.ItemDef d = plugin.items().get(e.item());
                String nm = d.name() != null ? d.name() : d.material().name();
                String dropInfo = e.dropChance() < 1.0
                        ? " <gray>· drop: <white>" + String.format(Locale.ROOT, "%.0f%%", e.dropChance() * 100)
                        : "";
                lines.add("   <dark_gray>- <reset>" + nm + " <gray>"
                        + String.format(Locale.ROOT, "%.2f%%", pct * e.weight() / sum)
                        + dropInfo
                        + (d.limit() > 0 ? " <red>(únicos: " + plugin.data().created(d.id()) + "/" + d.limit() + ")" : ""));
            }
        }
        return lines;
    }
}
