package dev.snuffac.core.punish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class BanExpiryTest {

    private static final UUID TARGET = UUID.randomUUID();

    @Test
    void aTempbanIsLiveBeforeExpiryAndDeadAfter() {
        long now = System.currentTimeMillis();
        var record = new PunishmentService.Punishment(
                UUID.randomUUID(),
                PunishmentService.Kind.TEMPBAN,
                TARGET,
                "player",
                "",
                "test",
                "staff",
                now,
                now + 60_000L,
                true,
                "",
                0L);

        assertFalse(record.expired(now), "a fresh tempban is not expired");
        assertTrue(record.live(now));
        assertTrue(record.live(now + 59_000L), "still live one second before expiry");
        assertTrue(record.expired(now + 60_000L), "expired exactly at the expiry instant");
        assertFalse(record.live(now + 60_000L), "a live record is not live once expired");
        assertFalse(record.live(now + 120_000L));
    }

    @Test
    void aPermanentBanNeverExpires() {
        long now = System.currentTimeMillis();
        var record = new PunishmentService.Punishment(
                UUID.randomUUID(),
                PunishmentService.Kind.BAN,
                TARGET,
                "player",
                "",
                "test",
                "staff",
                now,
                0L,
                true,
                "",
                0L);

        assertFalse(record.expired(now));
        assertFalse(record.expired(now + 86_400_000L));
        assertFalse(record.expired(Long.MAX_VALUE / 2));
        assertTrue(record.live(now + 86_400_000L));
    }

    @Test
    void aLiftedPunishmentIsNeverLive() {
        long now = System.currentTimeMillis();
        var record = new PunishmentService.Punishment(
                UUID.randomUUID(),
                PunishmentService.Kind.TEMPBAN,
                TARGET,
                "player",
                "",
                "test",
                "staff",
                now,
                now + 60_000L,
                false,
                "staff",
                now);
        assertFalse(record.live(now), "lifting a punishment must kill it immediately");
    }

    @Test
    void oneMinuteParsesToSixtyThousandMillis() {
        assertEquals(60_000L, Durations.parseMillis("1m"));
    }

    @Test
    void aTempbanIsReachableAndFoundByIsBanned() {
        PunishmentService service = new PunishmentService(null, Runnable::run);
        service.punish(PunishmentService.Kind.TEMPBAN, TARGET, "player", "", "test", "staff", 60_000L);
        assertTrue(service.isBanned(TARGET),
                "a live tempban must be found, this is what entry 008 depends on");
        assertTrue(service.isBanned(TARGET) == service.active(TARGET).stream()
                .anyMatch(record -> record.live(System.currentTimeMillis())));
    }

    @Test
    void clearingPunishmentsRemovesThemFromTheBanCheck() {
        PunishmentService service = new PunishmentService(null, Runnable::run);
        service.punish(PunishmentService.Kind.TEMPBAN, TARGET, "player", "", "test", "staff", 60_000L);
        assertTrue(service.isBanned(TARGET));
        int cleared = service.clearAll(TARGET);
        assertEquals(1, cleared);
        assertFalse(service.isBanned(TARGET), "cleared punishments must stop reporting as a ban");
    }

    @Test
    void clearingAPlayerWithNothingToClearReportsZero() {
        PunishmentService service = new PunishmentService(null, Runnable::run);
        assertEquals(0, service.clearAll(UUID.randomUUID()));
    }

    @Test
    void aMuteIsNotABan() {
        PunishmentService service = new PunishmentService(null, Runnable::run);
        service.punish(PunishmentService.Kind.MUTE, TARGET, "player", "", "test", "staff", 60_000L);
        assertFalse(service.isBanned(TARGET), "a mute must not read as a ban");
        assertTrue(service.isMuted(TARGET));
    }
}
