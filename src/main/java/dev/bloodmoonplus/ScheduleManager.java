package dev.bloodmoonplus;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Programación de eventos en 2 modos:
 *
 * 1. "interval" — Cada N horas reales (ej. cada 72h = cada 3 días reales).
 *    Config: schedule.interval-hours: 72
 *
 * 2. "weekly" — Por día de la semana a hora fija en zona horaria (ej. cada MONDAY a las 14:00 MX).
 *    Config: schedule.days: { MONDAY: ["14:00"], ... }
 *
 * Por defecto: weekly, America/Mexico_City.
 */
public final class ScheduleManager {

    public enum Mode { INTERVAL, WEEKLY }

    public record Slot(DayOfWeek day, LocalTime time, double difficulty, int duration, List<String> worlds) {}

    public record Upcoming(Slot slot, ZonedDateTime at) {}

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final Locale ES = Locale.of("es", "MX");

    private final BloodMoonPlus plugin;
    private final List<Slot> slots = new ArrayList<>();
    /** Mapa key -> timestamp del último aviso; evita duplicados sin riesgo de clear() masivo. */
    private final Map<String, Long> warned = new HashMap<>();
    private ZoneId zone = ZoneId.of("America/Mexico_City");
    private boolean enabled, skipEmpty;
    private int graceSec;
    private List<Integer> warnMin = List.of();

    // Estado interval mode
    private Mode mode = Mode.WEEKLY;
    private int intervalHours = 72;

    ScheduleManager(BloodMoonPlus plugin) { this.plugin = plugin; }

    void reload() {
        slots.clear();
        warned.clear();
        var c = plugin.getConfig();
        enabled = c.getBoolean("schedule.enabled", true);
        skipEmpty = c.getBoolean("schedule.skip-if-no-players", true);
        graceSec = c.getInt("schedule.grace-seconds", 180);
        warnMin = c.getIntegerList("schedule.warnings-minutes");
        try {
            zone = ZoneId.of(c.getString("schedule.timezone", "America/Mexico_City"));
        } catch (DateTimeException e) {
            plugin.getLogger().warning("Zona horaria inválida, usando America/Mexico_City");
            zone = ZoneId.of("America/Mexico_City");
        }
        double defDiff = c.getDouble("difficulty.default", 1.0);
        int defDur = c.getInt("event.duration-minutes", 20);

        String modeStr = c.getString("schedule.mode", "weekly").toLowerCase(Locale.ROOT);
        switch (modeStr) {
            case "interval", "horas", "hours" -> mode = Mode.INTERVAL;
            default -> mode = Mode.WEEKLY;
        }

        if (mode == Mode.INTERVAL) {
            intervalHours = Math.max(1, c.getInt("schedule.interval-hours", 72));
            plugin.getLogger().info("Modo interval: cada " + intervalHours + "h reales");
            return;
        }

        // WEEKLY mode
        ConfigurationSection days = c.getConfigurationSection("schedule.days");
        if (days == null) return;
        for (String key : days.getKeys(false)) {
            List<DayOfWeek> targets = parseDays(key);
            if (targets.isEmpty()) {
                plugin.getLogger().warning("schedule.days: día desconocido '" + key + "'");
                continue;
            }
            for (Object o : days.getList(key, List.of())) {
                LocalTime t = null;
                double diff = defDiff;
                int dur = defDur;
                List<String> worlds = List.of();
                if (o instanceof Map<?, ?> m) {
                    t = parseTime(m.get("time"));
                    diff = Util.num(m, "difficulty", defDiff);
                    dur = (int) Util.num(m, "duration-minutes", defDur);
                    if (m.get("worlds") instanceof List<?> wl) {
                        List<String> tmp = new ArrayList<>();
                        for (Object x : wl) tmp.add(x.toString());
                        worlds = tmp;
                    }
                } else {
                    t = parseTime(o);
                }
                if (t == null) {
                    plugin.getLogger().warning("schedule.days." + key + ": hora inválida '" + o + "' (usa formato \"21:30\" entre comillas)");
                    continue;
                }
                for (DayOfWeek d : targets) slots.add(new Slot(d, t, diff, dur, worlds));
            }
        }
        plugin.getLogger().info("Horarios cargados: " + slots.size() + " (modo weekly, zona " + zone + ")");
    }

    static LocalTime parseTime(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) { // YAML interpreta 21:30 sin comillas como 1290 (minutos)
            int v = n.intValue();
            return v >= 0 && v < 1440 ? LocalTime.of(v / 60, v % 60) : null;
        }
        try { return LocalTime.parse(o.toString().trim(), TIME_FMT); } catch (DateTimeException e) { return null; }
    }

    static List<DayOfWeek> parseDays(String key) {
        if (key == null) return List.of();
        String k = Normalizer.normalize(key, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toUpperCase(Locale.ROOT);
        switch (k) {
            case "ALL", "TODOS", "DIARIO", "DIARIAMENTE" -> { return Arrays.asList(DayOfWeek.values()); }
            case "LUNES" -> k = "MONDAY";
            case "MARTES" -> k = "TUESDAY";
            case "MIERCOLES" -> k = "WEDNESDAY";
            case "JUEVES" -> k = "THURSDAY";
            case "VIERNES" -> k = "FRIDAY";
            case "SABADO" -> k = "SATURDAY";
            case "DOMINGO" -> k = "SUNDAY";
            default -> { }
        }
        try { return List.of(DayOfWeek.valueOf(k)); } catch (IllegalArgumentException e) { return List.of(); }
    }

    public ZoneId zone() { return zone; }
    public Mode mode() { return mode; }

    // ------------------------------------------------------------------ tick

    public void tick() {
        if (!enabled) return;
        if (mode == Mode.INTERVAL) { tickInterval(); return; }
        tickWeekly();
    }

    /** Modo weekly: horarios fijos por día de la semana. */
    private void tickWeekly() {
        if (slots.isEmpty()) return;
        ZonedDateTime now = ZonedDateTime.now(zone);
        for (int off = -1; off <= 1; off++) {
            LocalDate date = now.toLocalDate().plusDays(off);
            for (Slot slot : slots) {
                if (slot.day() != date.getDayOfWeek()) continue;
                ZonedDateTime at = ZonedDateTime.of(date, slot.time(), zone);
                long past = Duration.between(at, now).getSeconds();
                String key = date + "T" + slot.time();

                if (past >= 0 && past <= graceSec) {
                    if (!plugin.data().isFired(key)) {
                        plugin.data().markFired(key);
                        fire(slot);
                    }
                } else if (past < 0) {
                    long until = -past;
                    for (int m : warnMin) {
                        String wkey = key + "/" + m;
                        if (until <= m * 60L && until > m * 60L - 30 && !warned.containsKey(wkey)) {
                            warned.put(wkey, System.currentTimeMillis());
                            warn(slot, m);
                        }
                    }
                }
            }
        }
    }

    /** Modo interval: cada N horas reales desde el último disparo. */
    private void tickInterval() {
        long nowMs = System.currentTimeMillis();
        String key = "interval";

        Long lastFired = plugin.data().getLastIntervalFire();
        // Primera vez en modo interval (data.yml sin registro): el próximo evento es
        // interval-hours desde AHORA, igual que calcula upcoming(). Sin esto, con lastFired
        // ausente/0 se disparaba en el primer tick, o con la marca fija "interval" anterior
        // solo llegaba a dispararse UNA vez para siempre.
        if (lastFired == null || lastFired <= 0) {
            plugin.data().setLastIntervalFire(nowMs);
            warned.clear();
            return;
        }

        // Verificar warnings
        long nextFire = lastFired + (long) intervalHours * 3600_000L;
        long untilMs = nextFire - nowMs;
        long untilSec = untilMs / 1000;

        for (int m : warnMin) {
            String wkey = key + "/" + m;
            if (untilSec <= m * 60L && untilSec > m * 60L - 30 && !warned.containsKey(wkey)) {
                warned.put(wkey, System.currentTimeMillis());
                warnInterval(m);
            }
        }

        // Disparar si llegó la hora: lastFired marca el ciclo, así que cada N horas vuelve
        // a disparar (la marca "interval" en fired era permanente y bloqueaba los ciclos
        // siguientes).
        if (untilMs <= 0) {
            plugin.data().setLastIntervalFire(nowMs);
            warned.clear();
            fireInterval();
        }

        // Limpiar warned viejo (más de 1 hora)
        warned.entrySet().removeIf(e -> nowMs - e.getValue() > 3600_000L);
    }

    /** Mundos sobre los que opera un aviso o un disparo.
     *  En modo global (event.global: true) son TODOS los mundos cargados; si el slot
     *  trae una lista explícita de mundos, se respeta esa lista. */
    private List<World> resolveWorlds(List<String> slotWorlds) {
        List<World> out = new ArrayList<>();
        if (slotWorlds == null || slotWorlds.isEmpty()) {
            if (plugin.events().isGlobalMode()) {
                out.addAll(Bukkit.getWorlds());
            } else {
                for (String wn : plugin.getConfig().getStringList("worlds")) {
                    World w = Bukkit.getWorld(wn);
                    if (w != null) out.add(w);
                }
            }
            return out;
        }
        for (String wn : slotWorlds) {
            World w = Bukkit.getWorld(wn);
            if (w == null) plugin.getLogger().warning("Mundo '" + wn + "' no está cargado.");
            else out.add(w);
        }
        return out;
    }

    /** Filtro común: no arrancamos en mundos vacíos si skip-if-no-players está activo.
     *  Con nadie online TODOS los mundos quedan vacíos y el evento no se dispara. */
    private List<World> startable(List<World> worlds) {
        List<World> out = new ArrayList<>();
        for (World w : worlds) {
            if (plugin.events().isActive(w)) continue;
            if (skipEmpty && w.getPlayers().isEmpty()) {
                plugin.getLogger().info("Evento omitido en '" + w.getName() + "': no hay jugadores.");
                continue;
            }
            out.add(w);
        }
        return out;
    }

    private void warnInterval(int minutes) {
        double defDiff = plugin.getConfig().getDouble("difficulty.default", 1.0);
        for (World w : resolveWorlds(List.of())) {
            if (plugin.events().isActive(w)) continue;
            if (!plugin.messages().announce("warnings", false)) continue;
            plugin.messages().world(w, "schedule.warning", "min", String.valueOf(minutes), "difficulty", Util.fmt(defDiff));
        }
    }

    private void fireInterval() {
        double defDiff = plugin.getConfig().getDouble("difficulty.default", 1.0);
        int defDur = plugin.getConfig().getInt("event.duration-minutes", 20);

        List<World> targets = startable(resolveWorlds(List.of()));
        if (targets.isEmpty()) return;
        if (plugin.events().startIn(targets, defDiff, Math.max(1, defDur))) {
            plugin.getLogger().info("Evento interval disparado globalmente en " + targets.size()
                    + " mundo(s) (cada " + intervalHours + "h)");
        }
    }

    private void fire(Slot slot) {
        List<World> targets = startable(resolveWorlds(slot.worlds()));
        if (!targets.isEmpty()) plugin.events().startIn(targets, slot.difficulty(), slot.duration());
    }

    private void warn(Slot slot, int minutes) {
        for (World w : resolveWorlds(slot.worlds())) {
            if (plugin.events().isActive(w)) continue;
            if (!plugin.messages().announce("warnings", false)) continue;
            plugin.messages().world(w, "schedule.warning", "min", String.valueOf(minutes), "difficulty", Util.fmt(slot.difficulty()));
        }
    }

    // ------------------------------------------------------------------ consulta

    public List<Upcoming> upcoming(int limit) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        List<Upcoming> out = new ArrayList<>();

        if (mode == Mode.INTERVAL) {
            Long lastFired = plugin.data().getLastIntervalFire();
            long nextFireMs = (lastFired != null ? lastFired : nowMs()) + (long) intervalHours * 3600_000L;
            long secUntil = Math.max(0, (nextFireMs - nowMs()) / 1000);
            ZonedDateTime at = now.plusSeconds(secUntil);
            double defDiff = plugin.getConfig().getDouble("difficulty.default", 1.0);
            int defDur = plugin.getConfig().getInt("event.duration-minutes", 20);
            List<String> worlds = plugin.getConfig().getStringList("worlds");
            out.add(new Upcoming(new Slot(null, null, defDiff, defDur, worlds), at));
            return out;
        }

        for (int off = 0; off <= 7; off++) {
            LocalDate date = now.toLocalDate().plusDays(off);
            for (Slot s : slots) {
                if (s.day() != date.getDayOfWeek()) continue;
                ZonedDateTime at = ZonedDateTime.of(date, s.time(), zone);
                if (at.isAfter(now)) out.add(new Upcoming(s, at));
            }
        }
        out.sort(Comparator.comparing(Upcoming::at));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    private static long nowMs() { return System.currentTimeMillis(); }

    public long secondsUntil(Upcoming u) {
        return Math.max(0, Duration.between(ZonedDateTime.now(zone), u.at()).getSeconds());
    }

    public String format(Upcoming u) {
        if (mode == Mode.INTERVAL || u.slot().day() == null) {
            return u.at().format(DateTimeFormatter.ofPattern("EEEE HH:mm", ES)) + " (" + zone.getId() + ")";
        }
        String day = u.at().getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
        return day + " " + u.at().format(DateTimeFormatter.ofPattern("HH:mm")) + " (" + zone.getId() + ")";
    }

    public int intervalHours() { return intervalHours; }
}
