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
    /** Tablas inexistentes ya avisadas (evita spamar la consola cada kill). */
    private final Set<String> unknownReported = new HashSet<>();

    LootManager(BloodMoonPlus plugin) { this.plugin = plugin; }

    void reload() {
        rarities.clear();
        tables.clear();
        unknownReported.clear();
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
        validate();
        plugin.getLogger().info("Loot: " + rarities.size() + " rarezas, " + tables.size() + " tablas.");
    }

    /** Revisa los números de loot.yml una sola vez al cargar: un peso negativo, un NaN o una
     *  probabilidad fuera de 0..1 desviaban el sorteo (o dejaban la tabla muda) sin avisar. */
    private void validate() {
        for (Table t : tables.values()) {
            if (!Double.isFinite(t.dropChance) || t.dropChance < 0 || t.dropChance > 1) {
                plugin.getLogger().warning("loot.yml: '" + t.id + "'.drop-chance fuera de 0..1 ("
                        + t.dropChance + "); se ajusta al rango.");
                t.dropChance = Double.isFinite(t.dropChance) ? Math.max(0, Math.min(1, t.dropChance)) : 1.0;
            }
            double sum = 0;
            for (Entry e : t.effective) {
                if (!Double.isFinite(e.weight()) || e.weight() < 0) {
                    plugin.getLogger().warning("loot.yml: '" + t.id + "' tiene una entrada con peso "
                            + "inválido (" + e.item() + ": " + e.weight() + "); se ignora.");
                    continue;
                }
                sum += e.weight();
            }
            if (sum <= 0) {
                plugin.getLogger().warning("loot.yml: la tabla '" + t.id
                        + "' no tiene ninguna entrada con peso > 0; no soltará nada.");
            }
            for (Map.Entry<String, Double> rw : t.rarityWeights.entrySet()) {
                if (Double.isFinite(rw.getValue()) && rw.getValue() >= 0) continue;
                plugin.getLogger().warning("loot.yml: '" + t.id + "'.rarity-weights." + rw.getKey()
                        + " inválido (" + rw.getValue() + "); se ignora.");
            }
        }
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

    /** true si la rareza (ya normalizada con Util.key) está definida en loot.yml. */
    public boolean hasRarity(String id) { return id != null && rarities.containsKey(id); }

    /** Posición de una rareza en loot.yml (orden de definición): 0 = la más común.
     *  Devuelve -1 si no existe. Sirve para comparar rarezas ("Raro en adelante"). */
    public int rarityRank(String id) {
        if (id == null || id.isEmpty()) return -1;
        String k = Util.key(id);
        int rank = 0;
        for (String key : rarities.keySet()) {
            if (key.equals(k)) return rank;
            rank++;
        }
        return -1;
    }

    public Set<String> tableIds() { return tables.keySet(); }

    /** Todas las tablas cargadas (para placeholders). */
    public Collection<Table> allTables() { return tables.values(); }

    /** Tira una tabla. applyDropChance=true usa la probabilidad base de la tabla (para mobs). */
    public List<ItemStack> roll(String tableId, Player player, double diff, boolean applyDropChance) {
        List<ItemStack> out = new ArrayList<>();
        Table t = tables.get(tableId);
        if (t == null) {
            // Avisar una sola vez por tabla: si no, cada kill fallaría en silencio y el
            // jugador no recibiría NADA sin ninguna pista en consola.
            if (unknownReported.add(tableId)) {
                plugin.getLogger().warning("Tabla de loot inexistente: '" + tableId
                        + "' (revísala en loot.yml); este drop no soltará nada.");
            }
            return out;
        }
        String owner = player != null ? player.getName() : "?";

        for (Entry g : t.guaranteed) {
            ItemFactory.ItemDef d = plugin.items().get(g.item());
            if (d == null) continue;
            ItemStack s = plugin.items().create(d, Util.between(g.amount()), owner);
            if (s != null) out.add(s);
        }
        if (applyDropChance) {
            double c = Math.max(0, Math.min(1.0, t.dropChance * plugin.scaled(diff, "loot-chance")));
            if (ThreadLocalRandom.current().nextDouble() >= c) return out;
        }
        int rolls = Util.between(t.rolls);
        // Un mismo roll() no repite el mismo item si la tabla tiene alternativas: un jefe
        // soltando dos veces la misma espada se sentía como un fallo del sorteo.
        Set<String> given = new HashSet<>();
        for (int i = 0; i < rolls; i++) {
            ItemStack s = rollOnce(t, player, diff, owner, given);
            if (s != null) out.add(s);
        }
        return out;
    }

    private ItemStack rollOnce(Table t, Player player, double diff, String owner, Set<String> given) {
        Map<String, List<Entry>> byRarity = new LinkedHashMap<>();
        for (Entry e : t.effective) {
            if (!Double.isFinite(e.weight()) || e.weight() <= 0) continue;
            ItemFactory.ItemDef d = plugin.items().get(e.item());
            if (d == null) continue;
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
            if (!Double.isFinite(w) || w <= 0) continue;
            if (ra.luck()) w *= luckMult;
            // El pity es por tabla: matar mobs de la horda no regala suerte en las tablas de jefe.
            if (uid != null && ra.pityPerRoll() > 0) {
                w *= 1 + plugin.data().pity(uid, t.id, rid) * ra.pityPerRoll();
            }
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
                if (rid.equals(chosen)) plugin.data().setPity(uid, t.id, rid, 0);
                else plugin.data().setPity(uid, t.id, rid,
                        Math.min(ra.pityMax(), plugin.data().pity(uid, t.id, rid) + 1));
            }
        }

        List<Entry> pool = byRarity.get(chosen);
        List<Entry> fresh = new ArrayList<>();
        for (Entry en : pool) if (!given.contains(en.item())) fresh.add(en);
        if (!fresh.isEmpty()) pool = fresh;
        Entry e = Util.pick(pool, Entry::weight);
        if (e == null) return null;
        // Drop-chance individual por item: los simples caen siempre, los rotos son raros
        if (e.dropChance() < 1.0 && ThreadLocalRandom.current().nextDouble() >= e.dropChance()) return null;
        ItemFactory.ItemDef d = plugin.items().get(e.item());
        ItemStack stack = plugin.items().create(d, Util.between(e.amount()), owner);
        given.add(e.item());

        Rarity ra = rarities.get(chosen);
        if (ra != null && ra.broadcast() && player != null && plugin.messages().announce("loot", true)) {
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

    /** Rareza etiquetada de un item; null si no la tiene. */
    public String rarityOf(ItemStack it) {
        if (it == null || it.getItemMeta() == null) return null;
        return it.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_RARITY, PersistentDataType.STRING);
    }

    /** Raridades que deben ir directo al inventario, no suelta al suelo.
     *  Acepta null (item sin rareza) sin lanzar NPE. */
    public boolean directToInventory(String rarity) {
        if (rarity == null || rarity.isEmpty()) return false;
        String k = Util.key(rarity);
        return k.equals(Util.key("RARO"))
                || k.equals(Util.key("EPICO"))
                || k.equals(Util.key("LEGENDARIO"))
                || k.equals(Util.key("MITICO"));
    }

    /** Suelta el item; los de rarezas altas brillan y no se destruyen. */
    public void dropAt(Location loc, ItemStack it) {
        // Un mundo descargado deja getWorld() a null: sin esto, el drop de un mob que murió
        // justo cuando se descargaba el mundo tiraba una NPE dentro del evento de muerte.
        var world = loc.getWorld();
        if (world == null) return;
        Item drop = world.dropItemNaturally(loc, it);
        if (!it.hasItemMeta()) return;
        String rarity = it.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_RARITY, PersistentDataType.STRING);
        if (rarity != null && plugin.getConfig().getStringList("loot.glow-rarities").stream().anyMatch(s -> Util.key(s).equals(rarity))) {
            drop.setGlowing(true);
            drop.setInvulnerable(true);
        }
    }

    /** Porcentajes reales de una tabla a dificultad 1 (para /luna chances). Muestra la
     *  probabilidad FINAL de cada item (rareza x peso x drop-chance) y el pity actual del
     *  jugador que ejecuta el comando en esa tabla. */
    public List<String> describe(String id, Player viewer) {
        Table t = tables.get(id);
        if (t == null) return null;
        List<String> lines = new ArrayList<>();
        lines.add("<gold>Tabla <white>" + id + " <gray>· prob. base de drop: <white>"
                + pct(t.dropChance) + " <gray>· tiradas: <white>" + t.rolls[0] + "-" + t.rolls[1]);
        if (!t.guaranteed.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Entry g : t.guaranteed) {
                ItemFactory.ItemDef d = plugin.items().get(g.item());
                if (d == null) continue;
                names.add((d.name() != null ? d.name() : d.material().name())
                        + " <gray>x" + g.amount()[0] + (g.amount()[1] > g.amount()[0] ? "-" + g.amount()[1] : ""));
            }
            if (!names.isEmpty()) lines.add("<gray>Garantizado: <white>" + String.join("<gray>, ", names));
        }
        Map<String, List<Entry>> byRarity = new LinkedHashMap<>();
        for (Entry e : t.effective) {
            if (!Double.isFinite(e.weight()) || e.weight() <= 0) continue;
            ItemFactory.ItemDef d = plugin.items().get(e.item());
            if (d == null) continue;
            byRarity.computeIfAbsent(d.rarity(), k -> new ArrayList<>()).add(e);
        }
        double total = 0;
        Map<String, Double> w = new LinkedHashMap<>();
        for (String rid : byRarity.keySet()) {
            Rarity ra = rarities.get(rid);
            if (ra == null) continue;
            double v = t.rarityWeights.getOrDefault(rid, ra.weight());
            if (!Double.isFinite(v) || v <= 0) continue;
            w.put(rid, v);
            total += v;
        }
        if (total <= 0) {
            // Sin pesos validos (rarities ausentes o con peso 0) la division daba NaN%.
            lines.add(" <gray>(esta tabla no tiene rarezas validas en loot.yml)");
            return lines;
        }
        for (Map.Entry<String, Double> en : w.entrySet()) {
            double chance = en.getValue() / total;
            lines.add(" " + rarityDisplay(en.getKey()) + " <white>" + pct(chance));
            List<Entry> es = byRarity.get(en.getKey());
            double sum = 0;
            for (Entry e : es) sum += e.weight();
            for (Entry e : es) {
                ItemFactory.ItemDef d = plugin.items().get(e.item());
                String nm = d.name() != null ? d.name() : d.material().name();
                double finalPct = chance * (e.weight() / sum) * e.dropChance();
                String dropInfo = e.dropChance() < 1.0
                        ? " <gray>· drop: <white>" + pct(e.dropChance())
                        : "";
                lines.add("   <dark_gray>- <reset>" + nm + " <white>" + pct(finalPct) + dropInfo);
            }
        }
        if (viewer != null) {
            StringBuilder pity = new StringBuilder();
            for (Rarity ra : rarities.values()) {
                if (ra.pityPerRoll() <= 0) continue;
                int stacks = plugin.data().pity(viewer.getUniqueId(), t.id, ra.id());
                if (stacks <= 0) continue;
                if (pity.length() > 0) pity.append(" <dark_gray>· <gray>");
                pity.append(ra.display()).append(" <white>").append(stacks)
                        .append("<gray>/").append(ra.pityMax())
                        .append(" <dark_gray>x").append(String.format(Locale.ROOT, "%.2f",
                                1 + stacks * ra.pityPerRoll()));
            }
            lines.add("<gray>Tu pity en esta tabla: " + (pity.length() == 0 ? "<white>0" : pity.toString()));
        }
        lines.add("<dark_gray>El % de cada item ya incluye su drop-chance propio; el sorteo de rareza va antes.");
        return lines;
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%.2f%%", v * 100);
    }
}
