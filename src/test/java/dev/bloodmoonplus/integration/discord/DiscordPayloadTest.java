package dev.bloodmoonplus.integration.discord;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** El cuerpo del webhook se arma a mano (sin Gson en la API de Paper): si el escapado falla,
 *  Discord rechaza el mensaje con 400 y el aviso se pierde en silencio. */
class DiscordPayloadTest {

    private static DiscordNotifier.Embed sample() {
        return new DiscordNotifier.Embed("🌘 Se acerca la Luna de Sangre",
                "La noche se tiñe de rojo. Afila tu espada.", 0xC1121F,
                List.of(new DiscordNotifier.Field("⏳ Cuenta atrás", "<t:1700000000:R>", true),
                        new DiscordNotifier.Field("⚔️ Dificultad", "**x2**", true)));
    }

    @Test
    void escapesQuotesBackslashesAndControlChars() {
        String out = DiscordNotifier.escape("di \"hola\" \\ y\nsalto\tcon\ttab");
        assertEquals("di \\\"hola\\\" \\\\ y\\nsalto\\tcon\\ttab", out);
        assertFalse(out.contains("\n"), "un salto literal rompería el JSON");
        assertEquals("\\u0001", DiscordNotifier.escape("\u0001"));
    }

    @Test
    void payloadIsAnEmbedWithFieldsAndNoContent() {
        String json = DiscordNotifier.json(sample(), "Luna de Sangre", "", "BloodMoonPlus · Luna de Sangre");
        assertTrue(json.startsWith("{\"embeds\":[{\"title\":\"🌘 Se acerca la Luna de Sangre\""), json);
        assertTrue(json.contains("\"description\":\"La noche se tiñe de rojo. Afila tu espada.\""), json);
        assertTrue(json.contains("\"color\":" + 0xC1121F), json);
        assertTrue(json.contains("\"fields\":[{\"name\":\"⏳ Cuenta atrás\",\"value\":\"<t:1700000000:R>\","
                + "\"inline\":true},{\"name\":\"⚔️ Dificultad\",\"value\":\"**x2**\",\"inline\":true}]"), json);
        assertTrue(json.contains("\"footer\":{\"text\":\"BloodMoonPlus · Luna de Sangre\"}"), json);
        assertTrue(json.contains("\"timestamp\":\""), "el embed lleva la hora de envío");
        assertTrue(json.contains("\"username\":\"Luna de Sangre\""), json);
        assertFalse(json.contains("\"avatar_url\""), "sin avatar configurado no debe enviarse el campo");
        assertFalse(json.contains("\"content\""), "el aviso va solo como embed, sin texto duplicado");
        assertTrue(json.contains("\"allowed_mentions\":{\"parse\":[]}"),
                "un @everyone en el texto no debe pingear al canal");
        assertTrue(json.endsWith("}"));
    }

    @Test
    void payloadIncludesAvatarWhenConfigured() {
        String json = DiscordNotifier.json(sample(), "", "https://cdn.example/a.png", "");
        assertTrue(json.contains("\"avatar_url\":\"https://cdn.example/a.png\""), json);
        assertFalse(json.contains("\"username\""), "sin username configurado se usa el del webhook");
        assertFalse(json.contains("\"footer\""), "sin pie de página el campo no se envía");
    }

    @Test
    void colorGetsHotterAsTheMoonApproaches() {
        assertEquals(DiscordNotifier.COLOR_FAR, DiscordNotifier.warningColor(60));
        assertEquals(DiscordNotifier.COLOR_FAR, DiscordNotifier.warningColor(11));
        assertEquals(DiscordNotifier.COLOR_SOON, DiscordNotifier.warningColor(10));
        assertEquals(DiscordNotifier.COLOR_SOON, DiscordNotifier.warningColor(2));
        assertEquals(DiscordNotifier.COLOR_IMMINENT, DiscordNotifier.warningColor(1));
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
