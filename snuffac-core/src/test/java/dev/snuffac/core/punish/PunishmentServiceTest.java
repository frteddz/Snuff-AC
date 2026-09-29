package dev.snuffac.core.punish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.punish.PunishmentService.Kind;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PunishmentServiceTest {

    private static final Executor DIRECT = Runnable::run;

    @Test
    void parsesMinuteHourDay() {
        assertEquals(60_000L, Durations.parseMillis("1m"));
        assertEquals(3_600_000L, Durations.parseMillis("1h"));
        assertEquals(86_400_000L, Durations.parseMillis("1d"));
    }

    @Test
    void parsesTheRequestedTenSecondForm() {
        assertEquals(3_600_000L + 10_000L, Durations.parseMillis("1h 10s"));
        assertEquals(86_400_000L + 3_600_000L, Durations.parseMillis("1d 1h"));
    }

    @Test
    void parsesCompoundInAnyOrder() {
        assertEquals(Durations.parseMillis("1d 1h"), Durations.parseMillis("1h 1d"));
    }

    @Test
    void rejectsUnitlessInput() {
        assertThrows(IllegalArgumentException.class, () -> Durations.parseMillis("10"));
    }

    @Test
    void rejectsUnitFirstInput() {
        assertThrows(IllegalArgumentException.class, () -> Durations.parseMillis("m10"));
    }

    @Test
    void rejectsZeroAndNegative() {
        assertThrows(IllegalArgumentException.class, () -> Durations.parseMillis("0d"));
    }

    @Test
    void rejectsUnknownUnit() {
        assertThrows(IllegalArgumentException.class, () -> Durations.parseMillis("5x"));
    }

    @Test
    void rejectsAbsurdLength() {
        assertThrows(IllegalArgumentException.class, () -> Durations.parseMillis("100y"));
    }

    @Test
    void describesDurationsBackToText() {
        assertEquals("1h10s", Durations.describe(3_610_000L));
        assertEquals("permanent", Durations.describe(0L));
    }

    @Test
    void tempbanExpiresAndStopsCounting(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.TEMPBAN, id, "Steve", "", "cheating", "Admin", 50L);
            assertTrue(service.isBanned(id));
            sleep(90L);
            assertFalse(service.isBanned(id), "an expired tempban must not count as a ban");
        }
    }

    @Test
    void permanentBanNeverExpires(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.BAN, id, "Steve", "", "cheating", "Admin", 0L);
            sleep(60L);
            assertTrue(service.isBanned(id), "a permanent ban has no expiry");
        }
    }

    @Test
    void unbanRemovesTheBanAndIsIdempotent(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.BAN, id, "Steve", "", "cheating", "Admin", 0L);
            assertEquals(1, service.remove(id, Kind.BAN, "Admin").size());
            assertFalse(service.isBanned(id));
            assertEquals(0, service.remove(id, Kind.BAN, "Admin").size(), "second unban does nothing");
        }
    }

    @Test
    void unbanAlsoClearsATempban(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.TEMPBAN, id, "Steve", "", "cheating", "Admin", 3_600_000L);
            assertEquals(1, service.remove(id, Kind.TEMPBAN, "Admin").size());
            assertFalse(service.isBanned(id));
        }
    }

    @Test
    void warnsAreSeparateFromBans(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.WARN, id, "Steve", "", "behave", "Admin", 0L);
            assertFalse(service.isBanned(id), "a warning is not a ban");
            assertEquals(1, service.history(id).size());
        }
    }

    @Test
    void mutesAreTrackedSeparatelyFromBans(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.MUTE, id, "Steve", "", "spam", "Admin", 0L);
            assertTrue(service.isMuted(id));
            assertFalse(service.isBanned(id));
        }
    }

    @Test
    void punishmentsSurviveAReload(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.BAN, id, "Steve", "", "cheating", "Admin", 0L);
        }
        try (PunishmentService reloaded = new PunishmentService(directory, DIRECT)) {
            reloaded.load();
            assertTrue(reloaded.isBanned(id), "an active ban must survive a restart");
        }
    }

    @Test
    void ipbanBlocksTheHashedAddress(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        String hash = PunishmentService.hashIp("10.0.0.5");
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.IPBAN, id, "Steve", hash, "multi account", "Admin", 0L);
            assertTrue(service.isIpBanned(hash));
            assertFalse(service.isIpBanned(PunishmentService.hashIp("10.0.0.6")));
        }
    }

    @Test
    void reasonIsRequiredBySanitisation(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            PunishmentService.Punishment record =
                    service.punish(Kind.WARN, id, "Steve", "", "bad\treason\nhere", "Admin", 0L);
            assertFalse(record.reason().contains("\t"));
            assertFalse(record.reason().contains("\n"));
        }
    }

    @Test
    void activeExcludesRemovedAndExpired(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (PunishmentService service = new PunishmentService(directory, DIRECT)) {
            service.punish(Kind.BAN, id, "Steve", "", "a", "Admin", 0L);
            service.punish(Kind.TEMPBAN, id, "Steve", "", "b", "Admin", 30L);
            assertEquals(2, service.active(id).size());
            service.remove(id, Kind.BAN, "Admin");
            sleep(80L);
            List<PunishmentService.Punishment> live = service.active(id);
            assertEquals(0, live.size(), "removed and expired records are not active");
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
