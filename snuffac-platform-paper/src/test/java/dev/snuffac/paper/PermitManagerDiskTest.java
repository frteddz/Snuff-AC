package dev.snuffac.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PermitManagerDiskTest {

    @Test
    void aGrantSurvivesARestart(@TempDir Path root) {
        UUID id = UUID.randomUUID();
        try (PermitManager first = new PermitManager(root.resolve("bypass.tsv"))) {
            first.load();
            first.setBypass(id, true, "Tester", "Tester2");
            assertTrue(first.isBypassed(id), "the grant should be active immediately");
        }

        try (PermitManager reloaded = new PermitManager(root.resolve("bypass.tsv"))) {
            reloaded.load();
            assertTrue(reloaded.isBypassed(id),
                    "a bypass did not survive a restart, so it silently stopped protecting the player");
            assertEquals("Tester2", reloaded.nameOf(id), "the name should survive for staff messages");
        }
    }

    @Test
    void aRevokeSurvivesARestart(@TempDir Path root) {
        UUID id = UUID.randomUUID();
        try (PermitManager first = new PermitManager(root.resolve("bypass.tsv"))) {
            first.load();
            first.setBypass(id, true, "Tester", "Tester2");
            first.setBypass(id, false, "Tester", "Tester2");
        }

        try (PermitManager reloaded = new PermitManager(root.resolve("bypass.tsv"))) {
            reloaded.load();
            assertFalse(reloaded.isBypassed(id), "a revoked bypass came back after a restart");
        }
    }

    @Test
    void anOfflinePlayerCanBeGranted(@TempDir Path root) {
        UUID id = UUID.randomUUID();
        try (PermitManager manager = new PermitManager(root.resolve("bypass.tsv"))) {
            manager.load();
            assertEquals("Tester2", manager.setBypass(id, true, "CONSOLE", "Tester2"),
                    "a grant must work for a player who is offline, that is when it matters most");
            assertTrue(manager.isBypassed(id));
        }
    }

    @Test
    void theFileIsWrittenWhereItIsReadFrom(@TempDir Path root) throws IOException {
        UUID id = UUID.randomUUID();
        Path file = root.resolve("nested").resolve("bypass.tsv");
        try (PermitManager manager = new PermitManager(file)) {
            manager.load();
            manager.setBypass(id, true, "Tester", "Tester2");
        }
        assertTrue(Files.isRegularFile(file),
                "the bypass file must be written to the configured path, otherwise nothing persists");
    }

    @Test
    void onePlayersBypassDoesNotLeakToAnother(@TempDir Path root) {
        UUID granted = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        try (PermitManager manager = new PermitManager(root.resolve("bypass.tsv"))) {
            manager.load();
            manager.setBypass(granted, true, "Tester", "Tester2");
            assertFalse(manager.isBypassed(other), "a bypass leaked to a different player");
            assertEquals(1, manager.count());
        }
    }
}
