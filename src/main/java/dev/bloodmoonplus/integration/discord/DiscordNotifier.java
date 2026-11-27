package dev.bloodmoonplus.integration.discord;

import dev.bloodmoonplus.BloodMoonPlus;
import dev.bloodmoonplus.Util;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Avisos al canal de Discord configurado usando un webhook (no hace falta un bot ni token).
 *
 * <p>Publica tres cosas: la cuenta atrás antes del evento, el inicio y el final. Cada aviso es un
 * <b>embed</b> con jerarquía clara: emoji + título corto, una línea de ambientación y los datos
 * duros en campos en línea (cuándo, dificultad, mundo, duración). Las horas van como marcas de
 * tiempo de Discord ({@code <t:...:R>}), así que cada persona ve la cuenta atrás en vivo y en su
 * propio idioma sin que el plugin tenga que formatear nada.
 *
 * <p>Todo el envío ocurre en un hilo aparte, así que un Discord lento o caído nunca frena el tick
 * del servidor; si el webhook responde con error solo queda un aviso en consola y el evento sigue.
 *
 * <p>Los textos salen de los locales ({@code discord.*}) y se convierten a texto plano: MiniMessage
 * no existe en Discord y etiquetas como {@code <red>} aparecerían literalmente en el mensaje.
 */
public final class DiscordNotifier {

    /** Paleta del embed: el rojo se calienta a medida que la luna se acerca, el final es ámbar. */
    static final int COLOR_FAR = 0x7F1D2B;
    static final int COLOR_SOON = 0xC1121F;
    static final int COLOR_IMMINENT = 0xE63946;
    static final int COLOR_START = 0xB31217;
    static final int COLOR_END = 0xE9A23B;
    static final int COLOR_STOPPED = 0x6B7280;

    /** Valor de un campo cuando el dato no existe (p. ej. cuenta atrás sin próxima luna). */
    private static final String NONE = "—";

    private final BloodMoonPlus plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private boolean enabled;
    private String url = "";
    private String username = "";
    private String avatar = "";
    private List<Integer> warnMinutes = List.of();
    private boolean notifyWarnings, notifyStart, notifyEnd;

    public DiscordNotifier(BloodMoonPlus plugin) {
        this.plugin = plugin;
    }

    /** Relee la sección discord de config.yml. Un webhook vacío o con forma inválida deja la
     *  función apagada con un aviso claro en consola en vez de fallar en cada evento. */
    public void reload() {
        var d = plugin.getConfig().getConfigurationSection("discord");
        enabled = d != null && d.getBoolean("enabled", false);
        url = d == null ? "" : d.getString("webhook-url", "").trim();
        username = d == null ? "" : d.getString("username", "").trim();
        avatar = d == null ? "" : d.getString("avatar-url", "").trim();
        notifyWarnings = d != null && d.getBoolean("notify.warnings", true);
        notifyStart = d != null && d.getBoolean("notify.start", true);
        notifyEnd = d != null && d.getBoolean("notify.end", true);

        List<Integer> mins = new ArrayList<>(d == null ? List.of() : d.getIntegerList("warnings-minutes"));
        mins.removeIf(m -> m <= 0);
        mins.sort((a, b) -> Integer.compare(b, a));
        warnMinutes = List.copyOf(mins);

        if (!enabled) return;
        if (url.isEmpty()) {
            enabled = false;
            plugin.getLogger().warning("discord.enabled está en true pero discord.webhook-url está vacío: "
                    + "avisos de Discord desactivados.");
        } else if (!looksLikeWebhook(url)) {
            enabled = false;
            plugin.getLogger().warning("discord.webhook-url no parece una URL de webhook de Discord "
                    + "(https://discord.com/api/webhooks/...): avisos de Discord desactivados.");
        } else {
            plugin.getLogger().info("Avisos de Discord activados (antes: "
                    + (warnMinutes.isEmpty() ? "sin cuenta atrás" : warnMinutes + " min") + ").");
        }
    }

    /** Acepta webhooks de Discord y, para pruebas locales, un servidor en loopback. */
    static boolean looksLikeWebhook(String value) {
        String u = value.toLowerCase(java.util.Locale.ROOT);
        if (u.startsWith("http://127.0.0.1") || u.startsWith("http://localhost")) return true;
        return u.startsWith("https://discord.com/api/webhooks/")
                || u.startsWith("https://canary.discord.com/api/webhooks/")
                || u.startsWith("https://ptb.discord.com/api/webhooks/")
                || u.startsWith("https://discordapp.com/api/webhooks/");
    }

    /** ¿Toca mandar la cuenta atrás de este minuto? Lo consulta el programador para no
     *  depender de la opción de anuncios del chat, que es independiente. */
    public boolean wantsWarning(int minutes) {
        return enabled && notifyWarnings && warnMinutes.contains(minutes);
    }

    /** Color del embed según la urgencia: vino profundo lejos, carmesí cerca, rojo vivo al final. */
    static int warningColor(int minutes) {
        if (minutes <= 1) return COLOR_IMMINENT;
        if (minutes <= 10) return COLOR_SOON;
        return COLOR_FAR;
    }

    /**
     * Cuenta atrás del próximo evento. {@code startsAt} es el epoch en segundos de la hora de
     * inicio (0 si no se pudo calcular) y {@code when} el texto ya formateado de respaldo.
     */
    public void warning(int minutes, double difficulty, String when, long startsAt) {
        if (!wantsWarning(minutes)) return;
        // Con la luna ya en marcha no se anuncia la cuenta atrás del siguiente hueco: el canal
        // recibía "faltan 10 min" justo cuando los jugadores ya estaban dentro del evento.
        if (plugin.events().globalSession() != null) return;

        String countdown = startsAt > 0 ? "<t:" + startsAt + ":R>" : textOr(when);
        String begins = startsAt > 0 ? "<t:" + startsAt + ":f>" : textOr(when);
        send(new Embed(
                text("discord.warning-title"),
                text("discord.warning"),
                warningColor(minutes),
                List.of(
                        new Field(text("discord.field-left"), countdown, true),
                        new Field(text("discord.field-when"), begins, true),
                        new Field(text("discord.field-difficulty"), "**x" + Util.fmt(difficulty) + "**", true))));
    }

    public void started(String world, double difficulty, int minutes) {
        if (!enabled || !notifyStart) return;
        int duration = Math.max(1, minutes);
        long endsAt = Instant.now().plusSeconds(duration * 60L).getEpochSecond();
        send(new Embed(
                text("discord.start-title"),
                text("discord.start"),
                COLOR_START,
                List.of(
                        new Field(text("discord.field-world"), textOr(world), true),
                        new Field(text("discord.field-difficulty"), "**x" + Util.fmt(difficulty) + "**", true),
                        new Field(text("discord.field-duration"), "**" + duration + " min**", true),
                        new Field(text("discord.field-end"), "<t:" + endsAt + ":R>", true))));
    }

    public void ended(String world, boolean natural, int minutes) {
        if (!enabled || !notifyEnd) return;
        send(new Embed(
                text(natural ? "discord.end-title" : "discord.stopped-title"),
                text(natural ? "discord.end" : "discord.stopped"),
                natural ? COLOR_END : COLOR_STOPPED,
                List.of(
                        new Field(text("discord.field-world"), textOr(world), true),
                        new Field(text("discord.field-duration"), "**" + Math.max(0, minutes) + " min**", true),
                        new Field(text("discord.field-result"),
                                text(natural ? "discord.result-ended" : "discord.result-stopped"), false))));
    }

    /** Texto plano del locale, sin etiquetas de color. */
    private String text(String key, String... kv) {
        String raw = plugin.locales().get(key, kv);
        return PlainTextComponentSerializer.plainText().serialize(Util.mini(raw)).trim();
    }

    private String textOr(String value) {
        return value == null || value.isBlank() ? NONE : value.trim();
    }

    private void send(Embed embed) {
        if (embed.title().isEmpty() && embed.description().isEmpty()) return;
        String body = json(embed, username, avatar, text("discord.footer"));
        // Fuera del hilo principal: la petición HTTP puede tardar segundos y el evento no espera.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> post(body));
    }

    private void post(String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "BloodMoonPlus/" + plugin.getPluginMeta().getVersion())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                plugin.getLogger().warning("Discord respondió HTTP " + res.statusCode()
                        + " al enviar el aviso: " + shorten(res.body()));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            plugin.getLogger().warning("No se pudo enviar el aviso a Discord: " + ex.getMessage());
        }
    }

    private static String shorten(String s) {
        if (s == null) return "";
        String one = s.replace('\n', ' ').trim();
        return one.length() > 200 ? one.substring(0, 200) + "…" : one;
    }

    // ------------------------------------------------------------------ JSON

    /** Un campo en línea del embed: nombre corto y valor ya resuelto. */
    record Field(String name, String value, boolean inline) {}

    /** Un embed completo. El pie de página lo pone siempre el llamador. */
    record Embed(String title, String description, int color, List<Field> fields) {}

    /** Cuerpo JSON del webhook. Paquete-privado para poder probar el escapado sin red. */
    static String json(Embed embed, String username, String avatar, String footer) {
        StringBuilder sb = new StringBuilder("{\"embeds\":[{");
        sb.append("\"title\":\"").append(escape(embed.title())).append('"');
        if (embed.description() != null && !embed.description().isBlank()) {
            sb.append(",\"description\":\"").append(escape(embed.description())).append('"');
        }
        sb.append(",\"color\":").append(embed.color());
        if (!embed.fields().isEmpty()) {
            sb.append(",\"fields\":[");
            for (int i = 0; i < embed.fields().size(); i++) {
                Field f = embed.fields().get(i);
                if (i > 0) sb.append(',');
                sb.append("{\"name\":\"").append(escape(f.name()))
                        .append("\",\"value\":\"").append(escape(f.value()))
                        .append("\",\"inline\":").append(f.inline()).append('}');
            }
            sb.append(']');
        }
        if (footer != null && !footer.isBlank()) {
            sb.append(",\"footer\":{\"text\":\"").append(escape(footer)).append("\"}");
        }
        sb.append(",\"timestamp\":\"").append(Instant.now()).append('"');
        sb.append("}]");
        if (username != null && !username.isBlank()) {
            sb.append(",\"username\":\"").append(escape(username)).append('"');
        }
        if (avatar != null && !avatar.isBlank()) {
            sb.append(",\"avatar_url\":\"").append(escape(avatar)).append('"');
        }
        // Sin esto, un mensaje que contenga @everyone podría pingear al canal entero.
        sb.append(",\"allowed_mentions\":{\"parse\":[]}}");
        return sb.toString();
    }

    static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }
}
