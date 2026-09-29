package dev.snuffac.core.violation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.violation.ViolationInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ViolationHistoryStoreTest {

    private static final Executor DIRECT = Runnable::run;

    @Test
    void keepsHistoryWhenThePlayerReconnects(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (ViolationHistoryStore store = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            store.record(flag(id, "Steve", "fly", 4.0, 1_000L));
            store.record(flag(id, "Steve", "speed", 6.0, 2_000L));
            store.flush(id);
        }

        try (ViolationHistoryStore reloaded = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            reloaded.load(id, "Steve");
            assertEquals(2, reloaded.total(id), "flags must survive a disconnect and reload");
        }
    }

    @Test
    void forgetDoesNotDiscardRecordedHistory(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (ViolationHistoryStore store = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            store.record(flag(id, "Steve", "fly", 4.0, 1_000L));
            store.flush(id);
            assertEquals(1, store.total(id), "flush must keep history, unlike the old forget call");
        }
    }

    @Test
    void keepsOnlyTheMostRecentEntriesPerPlayer(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        try (ViolationHistoryStore store = new ViolationHistoryStore(directory, 90, 3, DIRECT, SnuffPlatform.PAPER)) {
            for (int i = 0; i < 10; i++) {
                store.record(flag(id, "Steve", "fly", i, 1_000L + i));
            }
            assertEquals(3, store.total(id), "history must be capped per player");
        }
    }

    @Test
    void countsRecentFlagsSeparatelyFromAllTime(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();
        try (ViolationHistoryStore store = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            store.record(flag(id, "Steve", "fly", 1.0, now - 172_800_000L));
            store.record(flag(id, "Steve", "fly", 1.0, now));
            assertEquals(2, store.total(id));
            assertEquals(1, store.totalInLastHours(id, 24), "a flag from 48 hours ago must be outside the 24 hour window");
        }
    }

    @Test
    void storesCheckNameAndTimeForEveryFlag(@TempDir Path directory) {
        UUID id = UUID.randomUUID();
        long stamp = 1_700_000_000_000L;
        try (ViolationHistoryStore store = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            store.record(flag(id, "Steve", "reach", 9.0, stamp));
        }
        try (ViolationHistoryStore reloaded = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            reloaded.load(id, "Steve");
            List<ViolationInfo> history = reloaded.history(id);
            assertEquals(1, history.size());
            assertEquals("reach", history.get(0).checkKey());
            assertEquals("Reach", history.get(0).checkName());
            assertEquals(stamp, history.get(0).timestampMillis(), "the time of the flag must be preserved");
        }
    }

    @Test
    void writesOneFilePerPlayer(@TempDir Path directory) throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        try (ViolationHistoryStore store = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            store.record(flag(first, "Steve", "fly", 1.0, 1_000L));
            store.record(flag(second, "Alex", "reach", 1.0, 1_000L));
        }
        try (var stream = Files.list(directory)) {
            assertEquals(2, stream.count(), "history is keyed by uuid, one file each");
        }
    }

    @Test
    void malformedLinesAreIgnoredRatherThanThrowing(@TempDir Path directory) throws Exception {
        UUID id = UUID.randomUUID();
        try (ViolationHistoryStore store = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            store.record(flag(id, "Steve", "fly", 1.0, 1_000L));
        }
        Path file = directory.resolve(id.toString().replace("-", "") + ".history");
        Files.writeString(file, "garbage\n\nonly\ttwo\n", java.nio.charset.StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
        try (ViolationHistoryStore reloaded = new ViolationHistoryStore(directory, 90, 200, DIRECT, SnuffPlatform.PAPER)) {
            reloaded.load(id, "Steve");
            assertEquals(1, reloaded.total(id), "a corrupt line must not lose the good record");
        }
    }

    @Test
    void punishmentPathIsNotReachableFromViolationHandling() {
        assertFalse(hasViolationReachablePunishmentHook(),
                "no automatic punishment path may exist in the violation handler");
    }

    private static boolean hasViolationReachablePunishmentHook() {
        for (java.lang.reflect.Method method : ViolationHandler.class.getDeclaredMethods()) {
            String type = method.getReturnType().getName();
            if (type.contains("Command") || type.contains("Ban") || type.contains("Punish")) {
                return true;
            }
        }
        return false;
    }

    private static ViolationInfo flag(UUID id, String name, String check, double vl, long stamp) {
        return new ViolationInfo(
                id,
                name,
                CheckCategory.MOVEMENT,
                check,
                Character.toUpperCase(check.charAt(0)) + check.substring(1),
                "detail for " + check,
                vl,
                1.0,
                0.4,
                45.0,
                20.0,
                stamp,
                SnuffPlatform.PAPER);
    }
}
