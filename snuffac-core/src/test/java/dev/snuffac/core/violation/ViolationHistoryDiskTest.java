package dev.snuffac.core.violation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.violation.ViolationInfo;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ViolationHistoryDiskTest {

    private static final Executor DIRECT = Runnable::run;

    private static ViolationInfo record(UUID id, String name, String world, double x, double y, double z) {
        return new ViolationInfo(id, name, CheckCategory.MOVEMENT, "highjump", "HighJump",
                "jumped too high", 12.0, 3.0, 0.9, 42.0, 19.5,
                System.currentTimeMillis(), SnuffPlatform.PAPER, world, x, y, z);
    }

    private static ViolationHistoryStore store(Path root) {
        return new ViolationHistoryStore(root, 30, 50, DIRECT, SnuffPlatform.PAPER);
    }

    @Test
    void locationSurvivesARestart(@TempDir Path root) {
        UUID id = UUID.randomUUID();
        store(root).record(record(id, "Tester", "world_nether", -12.5, 64.0, 300.25));

        ViolationHistoryStore reopened = store(root);
        var history = reopened.history(id);
        assertEquals(1, history.size(), "the record should have been written to disk");
        ViolationInfo back = history.getLast();
        assertEquals("world_nether", back.worldName(), "the world was dropped when reloading");
        assertEquals(-12.5, back.x(), 0.001, "x was dropped when reloading");
        assertEquals(64.0, back.y(), 0.001, "y was dropped when reloading");
        assertEquals(300.25, back.z(), 0.001, "z was dropped when reloading");
    }

    @Test
    void clearRemovesTheFileFromDisk(@TempDir Path root) {
        UUID id = UUID.randomUUID();
        ViolationHistoryStore first = store(root);
        first.record(record(id, "Tester", "world", 1.0, 2.0, 3.0));
        first.record(record(id, "Tester", "world", 1.0, 2.0, 3.0));
        assertEquals(2, first.clear(id));
        assertEquals(0, first.total(id));

        ViolationHistoryStore reopened = store(root);
        assertEquals(0, reopened.total(id),
                "the history came back after a restart, so clear never deleted the file");
    }

    @Test
    void totalsWorkForAPlayerWhoIsOffline(@TempDir Path root) {
        UUID id = UUID.randomUUID();
        store(root).record(record(id, "Tester", "world", 1.0, 2.0, 3.0));
        assertEquals(1, store(root).total(id),
                "an offline player must still report the history that is on disk");
    }

    @Test
    void aRecordWithoutLocationStillLoads(@TempDir Path root) throws IOException {
        UUID id = UUID.randomUUID();
        Files.createDirectories(root);
        Files.writeString(root.resolve(id.toString().replace("-", "") + ".history"),
                System.currentTimeMillis() + "\tkey\tName\tMOVEMENT\t5\t1\t0.5\t20\t19.9\tTester\tdetail\n");

        var history = store(root).history(id);
        assertEquals(1, history.size(), "a record written by an older build must still load");
        assertEquals(1, history.getLast().worldName().isEmpty() ? 1 : 0,
                "a record with no world should read as empty, not fail");
    }

    @Test
    void everySavedLineRoundTrips(@TempDir Path root) {
        UUID id = UUID.randomUUID();
        ViolationHistoryStore first = store(root);
        for (int i = 0; i < 5; i++) {
            first.record(record(id, "Tester", "world", i, i + 0.5, i + 0.25));
        }
        var history = store(root).history(id);
        assertEquals(5, history.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, history.get(i).x(), 0.001);
        }
        assertTrue(history.getLast().x() == 4.0);
    }
}
