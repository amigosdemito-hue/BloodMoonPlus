package dev.bloodmoonplus.survivor;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de los placeholders de participación de RewardContext. Son los que resuelven la
 * recompensa por jugador (%participation_seconds%, %participation_percent%, %died% y
 * %survived%), que antes siempre devolvían una cadena vacía.
 *
 * Se usan la variante de resolve(text, UUID, String) para no necesitar un servidor ni un
 * Player real: la lógica es exactamente la misma que la de resolve(text, Player).
 */
class RewardContextTest {

    private static final UUID ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID LUIS = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID AUSENTE = UUID.fromString("33333333-3333-3333-3333-333333333333");

    /** Evento de 300 s (5 min) con participación conocida y ANA como única muerte. */
    private static RewardContext context() {
        Map<UUID, Long> joined = new HashMap<>();
        joined.put(ANA, 60_000L);   // entró al segundo 60  -> 240 s de participación (80 %)
        joined.put(LUIS, 0L);       // entró al inicio       -> 300 s de participación (100 %)
        return new RewardContext(null, "sesion-1", 0L, 300_000L, 2, 1, 1,
                "Fauces del Cataclismo", "CREEPER", "Killer", null, "world",
                0, 0, 0, 100, 320, joined, Set.of(ANA));
    }

    // ------------------------------------------------------------ participation_seconds

    @Test
    void participationSeconds_usesRealJoinTime() {
        assertEquals("240", context().resolve("%participation_seconds%", ANA, "Ana"));
    }

    @Test
    void participationSeconds_fullEventForFirstJoiner() {
        assertEquals("300", context().resolve("%participation_seconds%", LUIS, "Luis"));
    }

    @Test
    void participationSeconds_playerWithoutJoinTime_isZero() {
        assertEquals("0", context().resolve("%participation_seconds%", AUSENTE, "Nadie"));
    }

    @Test
    void participationSeconds_neverNegative() {
        // Entrada posterior al final (no debería pasar, pero no puede dar negativo)
        Map<UUID, Long> joined = Map.of(ANA, 900_000L);
        RewardContext ctx = new RewardContext(null, "s", 0L, 300_000L, 1, 0, 1,
                "", "NONE", null, null, "world", 0, 0, 0, 0, 0, joined, Set.of());
        assertEquals("0", ctx.resolve("%participation_seconds%", ANA, "Ana"));
    }

    // ------------------------------------------------------------ participation_percent

    @Test
    void participationPercent_computedAgainstEventDuration() {
        assertEquals("80", context().resolve("%participation_percent%", ANA, "Ana"));
    }

    @Test
    void participationPercent_fullAttendance_is100() {
        assertEquals("100", context().resolve("%participation_percent%", LUIS, "Luis"));
    }

    @Test
    void participationPercent_clampedTo100() {
        Map<UUID, Long> joined = Map.of(ANA, -600_000L); // "entró" antes del inicio
        RewardContext ctx = new RewardContext(null, "s", 0L, 300_000L, 1, 0, 1,
                "", "NONE", null, null, "world", 0, 0, 0, 0, 0, joined, Set.of());
        assertEquals("100", ctx.resolve("%participation_percent%", ANA, "Ana"));
    }

    @Test
    void participationPercent_playerWithoutJoinTime_isZero() {
        assertEquals("0", context().resolve("%participation_percent%", AUSENTE, "Nadie"));
    }

    // ------------------------------------------------------------ died / survived

    @Test
    void died_trueForDeadPlayer() {
        assertEquals("true", context().resolve("%died%", ANA, "Ana"));
    }

    @Test
    void survived_falseForDeadPlayer() {
        assertEquals("false", context().resolve("%survived%", ANA, "Ana"));
    }

    @Test
    void died_falseForLivingPlayer() {
        assertEquals("false", context().resolve("%died%", LUIS, "Luis"));
    }

    @Test
    void survived_trueForLivingPlayer() {
        assertEquals("true", context().resolve("%survived%", LUIS, "Luis"));
    }

    @Test
    void died_and_survived_areComplementary() {
        RewardContext ctx = context();
        for (UUID id : new UUID[]{ANA, LUIS, AUSENTE}) {
            String died = ctx.resolve("%died%", id, "X");
            String survived = ctx.resolve("%survived%", id, "X");
            assertNotEquals(died, survived, "died y survived no pueden coincidir para " + id);
        }
    }

    // ------------------------------------------------------------ mensajes completos

    @Test
    void resolvesPlaceholdersInsideFullMessage() {
        String msg = context().resolve(
                "%player% participó %participation_seconds% s (%participation_percent%) y %died%|%survived%",
                ANA, "Ana");
        assertEquals("Ana participó 240 s (80) y true|false", msg);
    }

    @Test
    void explicitSuffixWinsOverComputedValue() {
        RewardContext ctx = context();
        assertEquals("99", ctx.resolve("%participation_seconds:99%", ANA, "Ana"));
        assertEquals("50", ctx.resolve("%participation_percent:50%", ANA, "Ana"));
        assertEquals("si", ctx.resolve("%died:si%", ANA, "Ana"));
        assertEquals("no", ctx.resolve("%survived:no%", ANA, "Ana"));
    }

    @Test
    void otherPlaceholdersStillResolve() {
        RewardContext ctx = context();
        assertEquals("sesion-1", ctx.resolve("%session_uuid%", ANA, "Ana"));
        assertEquals("300", ctx.resolve("%duration_seconds%", ANA, "Ana"));
        assertEquals("Fauces del Cataclismo", ctx.resolve("%boss_name%", ANA, "Ana"));
        assertEquals("320", ctx.resolve("%boss_max_health%", ANA, "Ana"));
        assertEquals("Ana", ctx.resolve("$p", ANA, "Ana"));
    }

    @Test
    void unknownPlaceholder_isLeftUntouched() {
        assertEquals("%nope%", context().resolve("%nope%", ANA, "Ana"));
    }

    // ------------------------------------------------------------ sin destinatario

    @Test
    void withoutTarget_participationIsEmpty() {
        RewardContext ctx = context();
        assertEquals("", ctx.resolve("%participation_seconds%", null, null));
        assertEquals("", ctx.resolve("%participation_percent%", null, null));
        assertEquals("", ctx.resolve("%player%", null, null));
    }

    @Test
    void withoutTarget_diedAndSurvivedAreBothFalse() {
        // Sin jugador no hay forma de saberlo: nunca se anuncia "sobrevivió" por defecto.
        RewardContext ctx = context();
        assertEquals("false", ctx.resolve("%died%", null, null));
        assertEquals("false", ctx.resolve("%survived%", null, null));
    }

    // ------------------------------------------------------------ contexto sin sesión

    @Test
    void contextWithoutSessionData_returnsSafeDefaults() {
        // Constructor "de siempre" (sin mapa de entradas ni muertes): comandos de jefe, etc.
        RewardContext ctx = new RewardContext(null, "s", 0L, 120_000L, 0, 0, 0,
                "", "NONE", null, null, "world", 0, 0, 0, 0, 0);
        assertEquals("0", ctx.resolve("%participation_seconds%", ANA, "Ana"));
        assertEquals("0", ctx.resolve("%participation_percent%", ANA, "Ana"));
        assertEquals("false", ctx.resolve("%died%", ANA, "Ana"));
        assertEquals("true", ctx.resolve("%survived%", ANA, "Ana"));
    }

    @Test
    void zeroDurationEvent_doesNotDivideByZero() {
        RewardContext ctx = new RewardContext(null, "s", 0L, 0L, 1, 0, 1,
                "", "NONE", null, null, "world", 0, 0, 0, 0, 0,
                Map.of(ANA, 0L), Set.of());
        assertEquals("0", ctx.resolve("%participation_percent%", ANA, "Ana"));
    }

    @Test
    void nullText_returnsEmptyString() {
        assertEquals("", context().resolve(null, ANA, "Ana"));
    }
}
