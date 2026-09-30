package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.player.PlayerWorldCache;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NukerRetryTest {

    private SnuffCore core;
    private PlayerData player;
    private long packed;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-nuker"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        Map<Long, PlayerWorldCache.CachedBlock> blocks = new HashMap<>();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    blocks.put(new BlockPos(dx, 64 + dy, dz).pack(),
                            new PlayerWorldCache.CachedBlock(
                                    dy == 0 ? BlockKind.SOLID : BlockKind.AIR, 1.0, "stone"));
                }
            }
        }
        player.worldCache(PlayerWorldCache.of(
                new Vec3d(0.0, 65.0, 0.0), blocks, 0.6, 15, true, true));
        packed = new BlockPos(0, 64, 0).pack();
        var state = player.movement();
        state.advanceTo(new Vec3d(0.0, 65.0, 0.0), System.nanoTime());
        state.advanceGround(true);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                new Vec3d(0.0, 65.0, 0.0), 0.0f, 0.0f, true, false));
        for (int i = 0; i < 3; i++) {
            core.tick();
        }
    }

    private void startDig(long position) {
        core.enqueue(player.id(), new BlockBreakPacket(
                System.nanoTime(), BlockBreakPacket.BlockBreakAction.START, position, 0, 0));
        core.processQueue();
    }

    private boolean nukerFlagged() {
        return player.history().stream().anyMatch(record -> "nuker".equals(record.checkKey()));
    }

    @Test
    @DisplayName("retrying the same block many times is not nuker")
    void retryingOneBlockIsClean() {
        for (int i = 0; i < 40; i++) {
            startDig(packed);
        }
        assertFalse(nukerFlagged(),
                "a player who keeps clicking one block they cannot break is not nuker");
    }

    @Test
    @DisplayName("starting a burst across many blocks is still caught")
    void burstAcrossBlocksIsFlagged() {
        for (int i = 0; i < 5; i++) {
            startDig(new BlockPos(i, 64, 0).pack());
        }
        assertTrue(nukerFlagged(), "digging five different blocks in a burst should be caught");
    }

    @Test
    @DisplayName("digging a row of blocks one after another stays clean")
    void steadyDiggingIsClean() {
        for (int i = 0; i < 6; i++) {
            startDig(new BlockPos(i, 64, 0).pack());
            sleep(500);
        }
        assertFalse(nukerFlagged(), "ordinary digging at a human pace must stay clean");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class NoopMessenger implements dev.snuffac.core.platform.SnuffMessenger {

        @Override
        public void sendMessage(Object player, String message) {
        }

        @Override
        public void broadcast(String message, String permission) {
        }

        @Override
        public void sendConsole(String message) {
        }
    }

    private static final class NoopPermissions implements dev.snuffac.core.platform.SnuffPermissionChecker {

        @Override
        public boolean hasPermission(Object player, String permission) {
            return false;
        }

        @Override
        public List<String> playersWithPermission(String permission) {
            return List.of();
        }
    }
}
