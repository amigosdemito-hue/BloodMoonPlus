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
import java.util.ArrayList;
import java.util.List;

/**
 * Avisos al canal de Discord configurado usando un webhook (no hace falta un bot ni token).
 *
 * <p>Publica tres cosas: la cuenta atrás antes del evento, el inicio y el final. Todo el envío
 * ocurre en un hilo aparte, así que un Discord lento o caído nunca frena el tick del servidor;
 * si el webhook responde con error solo queda un aviso en consola y el evento sigue igual.
 *
 * <p>Los textos salen de los locales ({@code discord.*}) para respetar el idioma elegido y se
 * convierten a texto plano: MiniMessage no existe en Discord y etiquetas como {@code <red>}
 * aparecerían literalmente en el mensaje.
 */
public final class DiscordNotifier {

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

    public void warning(int minutes, double difficulty, String when) {
        if (!wantsWarning(minutes)) return;
        send(plain("discord.warning", "min", String.valueOf(minutes),
                "when", when, "difficulty", Util.fmt(difficulty)));
    }

    public void started(String world, double difficulty, int minutes) {
        if (!enabled || !notifyStart) return;
        send(plain("discord.start", "world", world,
                "difficulty", Util.fmt(difficulty), "minutes", String.valueOf(minutes)));
    }

    public void ended(String world, boolean natural, int minutes) {
        if (!enabled || !notifyEnd) return;
        send(plain(natural ? "discord.end" : "discord.stopped",
                "world", world, "minutes", String.valueOf(Math.max(0, minutes))));
    }

    /** Texto plano del locale, sin etiquetas de color. */
    private String plain(String key, String... kv) {
        String raw = plugin.locales().get(key, kv);
        return PlainTextComponentSerializer.plainText().serialize(Util.mini(raw)).trim();
    }

    private void send(String content) {
        if (content.isEmpty()) return;
        String body = json(content, username, avatar);
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

    /** Cuerpo JSON del webhook. Paquete-privado para poder probar el escapado sin red. */
    static String json(String content, String username, String avatar) {
        StringBuilder sb = new StringBuilder("{\"content\":\"").append(escape(content)).append('"');
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
