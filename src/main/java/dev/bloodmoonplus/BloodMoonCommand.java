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
    private static final List<String> USE_SUBS = List.of("next", "status");
    private static final List<String> ADMIN_SUBS = List.of("start", "stop", "difficulty", "boss", "horde",
            "killmobs", "give", "items", "chances", "refresh", "reload");

    static {
        for (String s : new String[]{"status", "next", "start", "stop", "difficulty", "boss", "horde", "killmobs",
                "give", "items", "chances", "refresh", "reload"}) ALIASES.put(s, s);
        ALIASES.put("estado", "status");
        ALIASES.put("proxima", "next"); ALIASES.put("siguiente", "next");
        ALIASES.put("iniciar", "start"); ALIASES.put("detener", "stop"); ALIASES.put("parar", "stop");
        ALIASES.put("dificultad", "difficulty"); ALIASES.put("jefe", "boss"); ALIASES.put("horda", "horde");
        ALIASES.put("limpiar", "killmobs"); ALIASES.put("dar", "give"); ALIASES.put("probabilidades", "chances");
        ALIASES.put("actualizar", "refresh"); ALIASES.put("refrescar", "refresh");
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
            case "status" -> status(s);
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
            case "refresh" -> refresh(s, a);
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
                if (args.length == 2) return filter(new ArrayList<>(plugin.bosses().ids()), cur);
                if (args.length == 3) {
                    // El segundo argumento acepta jugador O mundo: se sugieren los dos.
                    List<String> o = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                    o.addAll(worlds);
                    return filter(o, cur);
                }
                return List.of();
            }
            case "horde" -> {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), cur);
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
            case "refresh" -> {
                if (args.length != 2) return List.of();
                List<String> o = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                o.add("all");
                return filter(o, cur);
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
        String startedLabel;
        if (worldName != null) {
            org.bukkit.World w = Bukkit.getWorld(worldName);
            if (w == null) { m().send(s, "unknown-world"); return; }
            // Se pasa el mundo también en el error: los servidores que conservan un locales/*.yml
            // viejo tienen ese mensaje con {world} y sin argumento se imprimía el placeholder crudo.
            if (!plugin.events().startIn(List.of(w), dv, Math.max(1, mv))) {
                m().send(s, "event.already-active", "world", w.getName());
                return;
            }
            startedLabel = w.getName();
        } else {
            // Sin mundo explícito: el evento es GLOBAL y arranca en todos los mundos de golpe
            List<org.bukkit.World> targets = plugin.events().defaultWorlds();
            if (targets.isEmpty()) { m().send(s, "unknown-world"); return; }
            String label = targets.size() == 1 ? targets.get(0).getName() : "global";
            if (!plugin.events().startIn(targets, dv, Math.max(1, mv))) {
                m().send(s, "event.already-active", "world", label);
                return;
            }
            startedLabel = label;
        }
        m().send(s, "cmd.started", "world", startedLabel);
    }

    private void stop(CommandSender s, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("all")) {
            if (!plugin.events().stop(null, false)) { m().send(s, "cmd.status-none"); return; }
            m().send(s, "cmd.stopped-all");
            return;
        }
        if (a.length > 0) {
            org.bukkit.World w = Bukkit.getWorld(a[0]);
            if (w == null) { m().send(s, "unknown-world"); return; }
            if (!plugin.events().stop(w, false)) { m().send(s, "event.not-active", "world", w.getName()); return; }
            m().send(s, "cmd.stopped", "world", w.getName());
            return;
        }
        if (!plugin.events().stop(null, false)) { m().send(s, "cmd.status-none"); return; }
        m().send(s, "cmd.stopped-all");
    }

    private void difficulty(CommandSender s, String[] a) {
        if (a.length < 1) { m().send(s, "cmd.usage-difficulty"); return; }
        Double d = parseMult(a[0]);
        if (d == null) { m().send(s, "cmd.usage-difficulty"); return; }
        double v = Math.max(0.1, Math.min(plugin.getConfig().getDouble("difficulty.max", 10), d));
        // /luna difficulty <xN> [mundo]: con mundo solo cambia ese mundo (el tabulador ya lo
        // sugería, pero el argumento se ignoraba y siempre se cambiaba la dificultad global).
        if (a.length > 1) {
            org.bukkit.World w = Bukkit.getWorld(a[1]);
            if (w == null) { m().send(s, "unknown-world"); return; }
            EventManager.Session ses = sessionOf(w);
            if (ses == null) { m().send(s, "event.not-active", "world", w.getName()); return; }
            ses.difficulty = v;
            m().send(s, "cmd.difficulty-set", "difficulty", Util.fmt(v));
            return;
        }
        Collection<EventManager.Session> all = plugin.events().sessions();
        if (all.isEmpty()) { m().send(s, "cmd.status-none"); return; }
        // Cambio global: afecta a TODOS los mundos del evento en curso
        for (EventManager.Session ses : all) ses.difficulty = v;
        m().send(s, "cmd.difficulty-set", "difficulty", Util.fmt(v));
    }

    private void boss(CommandSender s, String[] a) {
        if (a.length < 1) { m().send(s, "cmd.usage-boss", "bosses", String.join(", ", plugin.bosses().ids())); return; }
        if (!plugin.bosses().ids().contains(a[0])) {
            m().send(s, "cmd.boss-unknown", "boss", a[0], "bosses", String.join(", ", plugin.bosses().ids()));
            return;
        }
        // /luna boss <id> <jugador|mundo>: el jefe aparece junto a ESE jugador (haya o no
        // evento activo) o en el spawn de ESE mundo. Antes exigía un evento y solo aceptaba
        // jugadores, así que no se podía invocar un jefe fuera de la Luna de Sangre.
        if (a.length > 1) {
            Player target = Bukkit.getPlayerExact(a[1]);
            if (target != null) {
                EventManager.Session ses = sessionOf(target.getWorld());
                boolean ok = ses != null
                        ? plugin.bosses().spawnAt(ses, a[0], target)
                        : plugin.bosses().spawnNear(a[0], target);
                if (ok) m().send(s, "cmd.boss-spawned-player", "boss", a[0], "player", target.getName());
                else m().send(s, "cmd.boss-failed-target", "boss", a[0], "player", target.getName());
                return;
            }
            org.bukkit.World w = Bukkit.getWorld(a[1]);
            if (w == null) { m().send(s, "cmd.target-unknown", "target", a[1]); return; }
            if (plugin.bosses().spawnInWorld(a[0], w))
                m().send(s, "cmd.boss-spawned-world", "boss", a[0], "world", w.getName());
            else m().send(s, "cmd.boss-failed", "boss", a[0]);
            return;
        }
        Collection<EventManager.Session> all = plugin.events().sessions();
        if (!all.isEmpty()) {
            int spawned = 0;
            for (EventManager.Session ses : all) if (plugin.bosses().spawn(ses, a[0])) spawned++;
            if (spawned > 0) m().send(s, "cmd.boss-spawned", "boss", a[0]);
            else m().send(s, "cmd.boss-failed", "boss", a[0]);
            return;
        }
        // Sin evento activo: se invoca junto al jugador que ejecuta el comando.
        if (s instanceof Player p) {
            if (plugin.bosses().spawnNear(a[0], p))
                m().send(s, "cmd.boss-spawned-player", "boss", a[0], "player", p.getName());
            else m().send(s, "cmd.boss-failed", "boss", a[0]);
        } else {
            m().send(s, "cmd.boss-usage-no-event");
        }
    }

    /** Sesión del evento en ese mundo, o null si ese mundo no tiene Luna de Sangre activa. */
    private EventManager.Session sessionOf(org.bukkit.World w) {
        for (EventManager.Session ses : plugin.events().sessions()) if (ses.world.equals(w)) return ses;
        return null;
    }

    private void horde(CommandSender s, String[] a) {
        Collection<EventManager.Session> all = plugin.events().sessions();
        if (all.isEmpty()) { m().send(s, "cmd.status-none"); return; }
        // /luna horde <jugador>: la oleada cae solo sobre ESE jugador.
        if (a.length > 0) {
            Player target = Bukkit.getPlayerExact(a[0]);
            if (target == null) { m().send(s, "cmd.player-unknown", "player", a[0]); return; }
            EventManager.Session ses = sessionOf(target.getWorld());
            if (ses == null) { m().send(s, "event.not-active", "world", target.getWorld().getName()); return; }
            int n = plugin.events().spawnWave(ses, target);
            m().send(s, "cmd.horde-spawned-player", "count", String.valueOf(n), "player", target.getName());
            return;
        }
        int total = 0;
        for (EventManager.Session ses : all) total += plugin.events().spawnWave(ses);
        m().send(s, "cmd.horde-spawned", "count", String.valueOf(total));
    }

    /** /luna refresh <jugador|all>: aplica al instante la puesta al día de los objetos de la
     *  Luna (encantamientos, maldición y habilidades nuevas) sin esperar a que reconecten. */
    private void refresh(CommandSender s, String[] a) {
        if (a.length < 1) { m().send(s, "cmd.usage-refresh"); return; }
        if (a[0].equalsIgnoreCase("all")) {
            int total = 0;
            for (Player p : Bukkit.getOnlinePlayers()) total += plugin.items().refreshPlayer(p);
            m().send(s, "cmd.refreshed-all", "count", String.valueOf(total));
            return;
        }
        Player p = Bukkit.getPlayerExact(a[0]);
        if (p == null) { m().send(s, "cmd.player-unknown", "player", a[0]); return; }
        int n = plugin.items().refreshPlayer(p);
        m().send(s, "cmd.refreshed", "player", p.getName(), "count", String.valueOf(n));
    }

    private void killmobs(CommandSender s, String[] a) {
        // Limpia TODO lo de la Luna en todos los mundos, haya evento o no: antes solo miraba
        // los mundos con sesión, así que un jefe invocado a mano no se podía quitar.
        int removed = 0;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            removed += plugin.bosses().removeAll(w);
            removed += plugin.events().removeTagged(w);
        }
        m().send(s, "cmd.killed", "count", String.valueOf(removed));
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
            // El item existe pero ya no quedan ejemplares únicos: antes se avisaba de que el
            // item no existía, que es falso y despista al administrador.
            if (d.limit() > 0) m().send(s, "cmd.item-limit", "item", a[1], "limit", String.valueOf(d.limit()));
            else m().send(s, "cmd.item-unknown", "item", a[1]);
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
        String worldLabel = plugin.events().activeWorlds() > 1 ? "global" : ses.world.getName();
        m().send(s, "cmd.status-line", "world", worldLabel,
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
