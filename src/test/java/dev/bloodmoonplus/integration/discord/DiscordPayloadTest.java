package dev.bloodmoonplus.integration.discord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El cuerpo del webhook se arma a mano (sin Gson en la API de Paper): si el escapado falla,
 *  Discord rechaza el mensaje con 400 y el aviso se pierde en silencio. */
class DiscordPayloadTest {

    @Test
    void escapesQuotesBackslashesAndControlChars() {
        String out = DiscordNotifier.escape("di \"hola\" \\ y\nsalto\tcon\ttab");
        assertEquals("di \\\"hola\\\" \\\\ y\\nsalto\\tcon\\ttab", out);
        assertFalse(out.contains("\n"), "un salto literal rompería el JSON");
        assertEquals("\\u0001", DiscordNotifier.escape("\u0001"));
    }

    @Test
    void payloadHasContentAndNeverPings() {
        String json = DiscordNotifier.json("La luna empieza en 30 min", "Luna de Sangre", "");
        assertTrue(json.startsWith("{\"content\":\"La luna empieza en 30 min\""), json);
        assertTrue(json.contains("\"username\":\"Luna de Sangre\""), json);
        assertFalse(json.contains("avatar_url"), "sin avatar configurado no debe enviarse el campo");
        assertTrue(json.contains("\"allowed_mentions\":{\"parse\":[]}"),
                "un @everyone en el texto no debe pingear al canal");
        assertTrue(json.endsWith("}"));
    }

    @Test
    void payloadIncludesAvatarWhenConfigured() {
        String json = DiscordNotifier.json("x", "", "https://cdn.example/a.png");
        assertTrue(json.contains("\"avatar_url\":\"https://cdn.example/a.png\""), json);
        assertFalse(json.contains("\"username\""), "sin username configurado se usa el del webhook");
    }

    @Test
    void webhookUrlValidation() {
        assertTrue(DiscordNotifier.looksLikeWebhook("https://discord.com/api/webhooks/1/abc"));
        assertTrue(DiscordNotifier.looksLikeWebhook("https://canary.discord.com/api/webhooks/1/abc"));
        assertTrue(DiscordNotifier.looksLikeWebhook("https://discordapp.com/api/webhooks/1/abc"));
        // Loopback permitido: sirve para probar el envío con un receptor local.
        assertTrue(DiscordNotifier.looksLikeWebhook("http://127.0.0.1:8799/hook"));
        assertFalse(DiscordNotifier.looksLikeWebhook("https://example.com/hook"));
        assertFalse(DiscordNotifier.looksLikeWebhook(""));
    }
}
