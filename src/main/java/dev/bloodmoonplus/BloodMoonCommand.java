package dev.bloodmoonplus;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class BloodMoonCommand implements TabExecutor {
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();
    private static final List<String> USE_SUBS = List.of("next");
    private static final List<String> ADMIN_SUBS = List.of("start", "stop", "difficulty", "boss", "horde",
            "killmobs", "give", "items", "chances", "reload");

    static {
        for (String s : new String[]{"next", "start", "stop", "difficulty", "boss", "horde", "killmobs",
                "give", "items", "chances", "reload"}) ALIASES.put(s, s);
        ALIASES.put("proxima", "next"); ALIASES.put("siguiente", "next");
        ALIASES.put("iniciar", "start"); ALIASES.put("detener", "stop"); ALIASES.put("parar", "stop");
        ALIASES.put("dificultad", "difficulty"); ALIASES.put("jefe", "boss"); ALIASES.put("horda", "horde");
        ALIASES.put("limpiar", "killmobs"); ALIASES.put("dar", "give"); ALIASES.put("probabilidades", "chances");
        ALIASES.put("recargar", "reload");
    }

    private final BloodMoonPlus plugin;

    BloodMoonCommand(BloodMoonPlus plugin) { this.plugin = plugin; }

    private Messages m() { return plugin.messages(); }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 0) { status(s); return true; }
        String sub = ALIASES.getOrDefault(args[0].toLowerCase(Locale.ROOT), "");
        if (sub.isEmpty()) { m().send(s, "cmd.usage"); return true; }
        if (!USE_SUBS.contains(sub) && !s.hasPermission("bloodmoon.admin")) { m().send(s, "no-permission"); return true; }
        String[] a = args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);

        switch (sub) {
            case "next" -> next(s);
            case "start" -> start(s, a);
            case "stop" -> stop(s, a);
            case "difficulty" -> difficulty(s, a);
            case "boss" -> boss(s, a);
            case "horde" -> horde(s, a);
            case "killmobs" -> killmobs(s, a);
            case "give" -> give(s, a);
            case "items" -> {
                m().send(s, "cmd.items-header");
                for (String id : plugin.items().ids()) {
                    ItemFactory.ItemDef d = plugin.items().get(id);
                    s.sendMessage(Util.mini("<dark_gray>- <white>" + id + " <gray>" + plugin.loot().rarityDisplay(d.rarity())
                            + (d.limit() > 0 ? " <red>(" + plugin.data().created(id) + "/" + d.limit() + ")" : "")));
                }
            }
            case "chances" -> {
                if (a.length < 1) { m().send(s, "cmd.usage-chances"); return true; }
                List<String> lines = plugin.loot().describe(a[0]);
                if (lines == null) { m().send(s, "cmd.table-unknown", "table", a[0]); return true; }
                for (String l : lines) s.sendMessage(Util.mini(l));
            }
            case "reload" -> { plugin.reloadAll(); m().send(s, "cmd.reloaded"); }
            default -> m().send(s, "cmd.usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        boolean admin = s.hasPermission("bloodmoon.admin");
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(USE_SUBS);
            if (admin) subs.addAll(ADMIN_SUBS);
            return filter(subs, args[0]);
        }
        if (!admin) return List.of();
        String sub = ALIASES.getOrDefault(args[0].toLowerCase(Locale.ROOT), "");
        List<String> worlds = Bukkit.getWorlds().stream().map(org.bukkit.World::getName).toList();
        String cur = args[args.length - 1];
        switch (sub) {
            case "start" -> {
                List<String> o = new ArrayList<>(worlds);
                o.addAll(List.of("x1", "x2", "x3", "x5", "20m", "30m"));
                return filter(o, cur);
            }
            case "stop" -> {
                List<String> o = new ArrayList<>(worlds);
                o.add("all");
                return filter(o, cur);
            }
            case "difficulty" -> {
                return args.length == 2 ? filter(List.of("x1", "x2", "x3", "x5"), cur) : filter(worlds, cur);
            }
            case "boss" -> {
                return args.length == 2 ? filter(new ArrayList<>(plugin.bosses().ids()), cur) : filter(worlds, cur);
            }
            case "horde" -> {
                return filter(worlds, cur);
            }
            case "killmobs" -> {
                return filter(worlds, cur);
            }
            case "chances" -> {
                return args.length == 2 ? filter(new ArrayList<>(plugin.loot().tableIds()), cur) : List.of();
            }
            case "give" -> {
                if (args.length == 2) return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), cur);
                if (args.length == 3) return filter(new ArrayList<>(plugin.items().ids()), cur);
                return List.of();
            }
            default -> {
                return List.of();
            }
        }
    }

    private static List<String> filter(List<String> opts, String cur) {
        String c = cur.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : opts) if (o.toLowerCase(Locale.ROOT).startsWith(c)) out.add(o);
        return out;
    }

    static Double parseMult(String t) {
        String x = t.toLowerCase(Locale.ROOT);
        if (x.equals("triple")) return 3.0;
        if (x.length() > 1 && x.charAt(0) == 'x') {
            try { return Double.parseDouble(x.substring(1)); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    static Integer parseMinutes(String t) {
        String x = t.toLowerCase(Locale.ROOT);
        if (x.length() > 1 && x.endsWith("m")) {
            try { return Integer.parseInt(x.substring(0, x.length() - 1)); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private void start(CommandSender s, String[] a) {
        Double diff = null;
        Integer minutes = null;
        String worldName = null;
        List<String> knownWorlds = Bukkit.getWorlds().stream().map(org.bukkit.World::getName).toList();
        for (String t : a) {
            Double d = parseMult(t);
            Integer mi = parseMinutes(t);
            if (d != null) diff = d;
            else if (mi != null) minutes = mi;
            else if (knownWorlds.contains(t)) worldName = t;
        }
        double dv = diff != null ? diff : plugin.getConfig().getDouble("difficulty.default", 1.0);
        int mv = minutes != null ? minutes : plugin.getConfig().getInt("event.duration-minutes", 20);
        if (worldName != null) {
            org.bukkit.World w = Bukkit.getWorld(worldName);
            if (w == null) { m().send(s, "unknown-world"); return; }
            if (!plugin.events().start(w, dv, Math.max(1, mv))) { m().send(s, "event.already-active"); return; }
        } else {
            boolean started = false;
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                if (plugin.events().start(w, dv, Math.max(1, mv))) started = true;
            }
            if (!started) { m().send(s, "event.already-active"); return; }
        }
        m().send(s, "cmd.started");
    }

    private void stop(CommandSender s, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("all")) {
            if (!plugin.events().stop(null, false)) { m().send(s, "event.not-active"); return; }
            m().send(s, "cmd.stopped-all");
            return;
        }
        if (a.length > 0) {
            org.bukkit.World w = Bukkit.getWorld(a[0]);
            if (w == null) { m().send(s, "unknown-world"); return; }
            if (!plugin.events().stop(w, false)) { m().send(s, "event.not-active"); return; }
        } else {
            if (!plugin.events().stop(null, false)) { m().send(s, "event.not-active"); return; }
        }
        m().send(s, "cmd.stopped");
    }

    private void difficulty(CommandSender s, String[] a) {
        if (a.length < 1) { m().send(s, "cmd.usage-difficulty"); return; }
        Double d = parseMult(a[0]);
        if (d == null) { m().send(s, "cmd.usage-difficulty"); return; }
        EventManager.Session ses = plugin.events().globalSession();
        if (ses == null) { m().send(s, "event.not-active"); return; }
        ses.difficulty = Math.max(0.1, Math.min(plugin.getConfig().getDouble("difficulty.max", 10), d));
        m().send(s, "cmd.difficulty-set", "difficulty", Util.fmt(ses.difficulty));
    }

    private void boss(CommandSender s, String[] a) {
        if (a.length < 1) { m().send(s, "cmd.usage-boss", "bosses", String.join(", ", plugin.bosses().ids())); return; }
        EventManager.Session ses = plugin.events().globalSession();
        if (ses == null) { m().send(s, "event.not-active"); return; }
        if (plugin.bosses().spawn(ses, a[0])) m().send(s, "cmd.boss-spawned", "boss", a[0]);
        else m().send(s, "cmd.boss-failed", "boss", a[0]);
    }

    private void horde(CommandSender s, String[] a) {
        EventManager.Session ses = plugin.events().globalSession();
        if (ses == null) { m().send(s, "event.not-active"); return; }
        m().send(s, "cmd.horde-spawned", "count", String.valueOf(plugin.events().spawnWave(ses)));
    }

    private void killmobs(CommandSender s, String[] a) {
        EventManager.Session ses = plugin.events().globalSession();
        if (ses == null) { m().send(s, "cmd.killed", "count", "0"); return; }
        plugin.bosses().removeAll(ses.world);
        m().send(s, "cmd.killed", "count", String.valueOf(plugin.events().removeTagged(ses.world)));
    }

    private void give(CommandSender s, String[] a) {
        if (a.length < 2) { m().send(s, "cmd.usage-give"); return; }
        Player p = Bukkit.getPlayerExact(a[0]);
        if (p == null) { m().send(s, "cmd.player-unknown", "player", a[0]); return; }
        ItemFactory.ItemDef d = plugin.items().get(a[1]);
        if (d == null) { m().send(s, "cmd.item-unknown", "item", a[1]); return; }
        int n = 1;
        if (a.length > 2) { try { n = Math.max(1, Math.min(64, Integer.parseInt(a[2]))); } catch (NumberFormatException ignored) { } }
        int serial = 0;
        List<ItemStack> created = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            ItemStack ci = plugin.items().create(d, 1, p.getName());
            if (ci == null) break;
            created.add(ci);
            serial++;
            if (serial >= n) break;
        }
        if (created.isEmpty()) {
            m().send(s, "cmd.item-unknown", "item", a[1]);
            return;
        }
        for (ItemStack it : created) {
            plugin.loot().giveOrDrop(p, it);
        }
        m().send(s, "cmd.given", "player", p.getName(), "item", a[1], "amount", String.valueOf(created.size()));
    }

    private void status(CommandSender s) {
        var ses = plugin.events().globalSession();
        if (ses == null) {
            m().send(s, "cmd.status-none");
            List<ScheduleManager.Upcoming> up = plugin.schedule().upcoming(1);
            if (!up.isEmpty()) {
                ScheduleManager.Upcoming u = up.get(0);
                m().send(s, "cmd.next-line", "when", plugin.schedule().format(u),
                        "in", Util.human(plugin.schedule().secondsUntil(u)), "difficulty", Util.fmt(u.slot().difficulty()));
            }
            return;
        }
        m().send(s, "cmd.status-line", "world", ses.world.getName(),
                "time", Util.dur((ses.endMs - System.currentTimeMillis()) / 1000), "difficulty", Util.fmt(ses.difficulty));
        List<ScheduleManager.Upcoming> up = plugin.schedule().upcoming(1);
        if (!up.isEmpty()) {
            ScheduleManager.Upcoming u = up.get(0);
            m().send(s, "cmd.next-line", "when", plugin.schedule().format(u),
                    "in", Util.human(plugin.schedule().secondsUntil(u)), "difficulty", Util.fmt(u.slot().difficulty()));
        }
    }

    private void next(CommandSender s) {
        List<ScheduleManager.Upcoming> up = plugin.schedule().upcoming(6);
        if (up.isEmpty()) { m().send(s, "cmd.next-none"); return; }
        m().send(s, "cmd.next-header");
        for (ScheduleManager.Upcoming u : up) {
            s.sendMessage(Util.mini(m().fill(m().raw("cmd.next-entry"), "when", plugin.schedule().format(u),
                    "in", Util.human(plugin.schedule().secondsUntil(u)), "difficulty", Util.fmt(u.slot().difficulty()))));
        }
    }
}
