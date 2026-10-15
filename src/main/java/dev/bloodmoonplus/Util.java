package dev.bloodmoonplus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Util {
    static final MiniMessage MM = MiniMessage.miniMessage();

    private Util() {}

    /**
     * Parsea MiniMessage tolerando texto heredado de Vanilla (§c, §4§l... que traen los
     * mensajes de muerte). Si aun así falla, devuelve texto plano: un anuncio jamás
     * debe reventar un evento por un texto raro.
     */
    public static Component mini(String s) {
        try {
            return MM.deserialize(legacyToMini(s));
        } catch (Exception ex) {
            return Component.text(plain(s));
        }
    }

    public static Component item(String s) {
        try {
            return MM.deserialize(legacyToMini(s)).decoration(TextDecoration.ITALIC, false);
        } catch (Exception ex) {
            return Component.text(plain(s)).decoration(TextDecoration.ITALIC, false);
        }
    }

    /** Último recurso: quita etiquetas para que un texto malo nunca lance excepción. */
    private static String plain(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^<>]*>", "").replace('§', ' ');
    }

    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)§x(§[0-9a-f]){6}");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)§([0-9a-fk-or])");

    /** Convierte códigos legacy (§c, §4§l, §x§R§R§G§G§B§B) a etiquetas MiniMessage. */
    public static String legacyToMini(String s) {
        if (s == null || s.indexOf('§') < 0) return s;
        StringBuilder out = new StringBuilder();
        Matcher m = LEGACY_HEX.matcher(s);
        while (m.find()) {
            String full = m.group();
            StringBuilder hex = new StringBuilder();
            for (int i = 3; i < full.length(); i += 2) hex.append(full.charAt(i));
            m.appendReplacement(out, Matcher.quoteReplacement("<#" + hex + ">"));
        }
        m.appendTail(out);
        s = out.toString();
        out = new StringBuilder();
        m = LEGACY_CODE.matcher(s);
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(tagFor(m.group(1).charAt(0))));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String tagFor(char c) {
        return switch (Character.toLowerCase(c)) {
            case '0' -> "<black>"; case '1' -> "<dark_blue>"; case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>"; case '4' -> "<dark_red>"; case '5' -> "<dark_purple>";
            case '6' -> "<gold>"; case '7' -> "<gray>"; case '8' -> "<dark_gray>";
            case '9' -> "<blue>"; case 'a' -> "<green>"; case 'b' -> "<aqua>";
            case 'c' -> "<red>"; case 'd' -> "<light_purple>"; case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>"; case 'l' -> "<bold>"; case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>"; case 'o' -> "<italic>"; case 'r' -> "<reset>";
            default -> "";
        };
    }

    static double num(Map<?, ?> m, String k, double d) {
        Object o = m.get(k);
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) { }
        }
        return d;
    }

    static String str(Map<?, ?> m, String k, String d) {
        Object o = m.get(k);
        return o == null ? d : o.toString();
    }

    /** Acepta 3 o "1-3". */
    static int[] range(Object o, int dMin, int dMax) {
        if (o == null) return new int[]{dMin, dMax};
        String s = o.toString().trim();
        try {
            int i = s.indexOf('-', 1);
            if (i > 0) {
                int a = Integer.parseInt(s.substring(0, i).trim());
                int b = Integer.parseInt(s.substring(i + 1).trim());
                return new int[]{Math.min(a, b), Math.max(a, b)};
            }
            int v = Integer.parseInt(s);
            return new int[]{v, v};
        } catch (NumberFormatException e) {
            return new int[]{dMin, dMax};
        }
    }

    static int between(int[] r) {
        return r[0] >= r[1] ? r[0] : ThreadLocalRandom.current().nextInt(r[0], r[1] + 1);
    }

    static <T> T pick(List<T> list, ToDoubleFunction<T> weight) {
        double total = 0;
        for (T t : list) total += Math.max(0, weight.applyAsDouble(t));
        if (list.isEmpty() || total <= 0) return null;
        double r = ThreadLocalRandom.current().nextDouble() * total;
        for (T t : list) {
            r -= Math.max(0, weight.applyAsDouble(t));
            if (r <= 0) return t;
        }
        return list.get(list.size() - 1);
    }

    static String dur(long sec) {
        sec = Math.max(0, sec);
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%02d:%02d", m, s);
    }

    static String human(long sec) {
        sec = Math.max(0, sec);
        long d = sec / 86400, h = (sec % 86400) / 3600, m = (sec % 3600) / 60;
        if (d > 0) return d + " d " + h + " h";
        if (h > 0) return h + " h " + m + " min";
        return Math.max(1, m) + " min";
    }

    static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d);
    }

    static String key(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.trim().toUpperCase(Locale.ROOT);
    }

    static double maxHealth(LivingEntity le) {
        AttributeInstance a = le.getAttribute(Attribute.MAX_HEALTH);
        return a == null ? Math.max(1, le.getHealth()) : a.getValue();
    }
}
