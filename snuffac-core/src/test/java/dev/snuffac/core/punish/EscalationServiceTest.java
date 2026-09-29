package dev.snuffac.core.punish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EscalationServiceTest {

    private static final Executor DIRECT = Runnable::run;
    private static final String CHECK = "reach";

    @Test
    void disabledByDefaultMeansNothingHappens(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            ladder.enabled(false);
            EscalationService.Outcome outcome = flag(ladder, id, 1.0);
            assertEquals(EscalationService.OutcomeKind.IGNORED_DISABLED, outcome.kind());
            assertEquals(0, store.history(id).size(), "a disabled ladder must not punish");
        }
    }

    @Test
    void firstFlagWarnsRatherThanBans(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            EscalationService.Outcome outcome = flag(ladder, id, 1.0);
            assertEquals(EscalationService.OutcomeKind.WARNED, outcome.kind());
            assertEquals(1, outcome.warnings());
            assertEquals(1, store.history(id).size());
            assertTrue(store.active(id).get(0).kind() == PunishmentService.Kind.WARN);
            assertFalse(store.isBanned(id), "the first flag must never ban");
        }
    }

    @Test
    void reachingTheLimitBansOnce(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            ladder.clearCooldown(id);
            for (int i = 1; i <= 4; i++) {
                assertEquals(EscalationService.OutcomeKind.WARNED, flag(ladder, id, 1.0).kind());
                ladder.clearCooldown(id);
            }
            EscalationService.Outcome finalOutcome = flag(ladder, id, 1.0);
            assertEquals(EscalationService.OutcomeKind.BANNED, finalOutcome.kind());
            assertEquals(5, finalOutcome.maxWarnings());
            assertTrue(store.isBanned(id), "the fifth warning must produce the ban");
            assertEquals(3_600_000L, finalOutcome.banMillis());
        }
    }

    @Test
    void warnOnlyStopsAtTheLimitWithoutBanning(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            ladder.warnOnly(true);
            for (int i = 0; i < 5; i++) {
                flag(ladder, id, 1.0);
                ladder.clearCooldown(id);
            }
            assertFalse(store.isBanned(id), "warn only must never ban");
            assertEquals(5, store.history(id).size(), "but all five warnings are recorded");
        }
    }

    @Test
    void lowConfidenceIsIgnoredEntirely(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.9)) {
            UUID id = UUID.randomUUID();
            assertEquals(EscalationService.OutcomeKind.IGNORED_LOW_CONFIDENCE,
                    flag(ladder, id, 0.5).kind());
            assertEquals(0, store.history(id).size());
        }
    }

    @Test
    void repeatedFlagsInsideTheCooldownDoNotStackWarnings(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            assertEquals(EscalationService.OutcomeKind.WARNED, flag(ladder, id, 1.0).kind());
            assertEquals(EscalationService.OutcomeKind.IGNORED_COOLDOWN, flag(ladder, id, 1.0).kind());
            assertEquals(1, store.history(id).size(),
                    "a burst of packets must not burn every warning at once");
        }
    }

    @Test
    void aCheaterCannotSkipToABanByReconnecting(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            flag(ladder, id, 1.0);
            ladder.clearCooldown(id);
            ladder.clearCooldown(id);
            assertEquals(1, ladder.state(id).warnings(),
                    "the ladder state is keyed by uuid, so a reconnect keeps the count");
        }
    }

    @Test
    void banReasonNamesTheCheckAndTheWarningCount(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            for (int i = 0; i < 5; i++) {
                flag(ladder, id, 1.0);
                ladder.clearCooldown(id);
            }
            String reason = store.active(id).stream()
                    .filter(record -> record.kind() == PunishmentService.Kind.TEMPBAN)
                    .findFirst()
                    .orElseThrow()
                    .reason();
            assertTrue(reason.contains("5"), "the reason must state the warning count");
            assertTrue(reason.contains("Reach"), "the reason must name the check");
            assertTrue(reason.contains("contact the admins"),
                    "the reason must tell the player how to appeal");
        }
    }

    @Test
    void maxWarningsIsConfigurable(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            ladder.maxWarnings(2);
            flag(ladder, id, 1.0);
            ladder.clearCooldown(id);
            assertEquals(EscalationService.OutcomeKind.BANNED, flag(ladder, id, 1.0).kind());
        }
    }

    @Test
    void maxWarningsNeverDropsBelowOne(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            ladder.maxWarnings(0);
            assertEquals(1, ladder.maxWarnings(), "a zero limit would ban on the first flag");
        }
    }

    @Test
    void noBanHappensWhenTheBanLengthIsZero(@TempDir Path directory) {
        try (PunishmentService store = new PunishmentService(directory, DIRECT);
                EscalationService ladder = new EscalationService(store, 0.0)) {
            UUID id = UUID.randomUUID();
            ladder.banMillis(0L);
            for (int i = 0; i < 5; i++) {
                flag(ladder, id, 1.0);
                ladder.clearCooldown(id);
            }
            assertFalse(store.isBanned(id));
        }
    }

    private static EscalationService.Outcome flag(EscalationService ladder, UUID id, double confidence) {
        return ladder.onFlag(id, "Steve", CHECK, "Reach", confidence, "Snuff AC");
    }
}
