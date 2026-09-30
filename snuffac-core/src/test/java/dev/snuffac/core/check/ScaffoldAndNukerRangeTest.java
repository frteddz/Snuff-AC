package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.check.impl.world.NukerCheck;
import dev.snuffac.core.check.impl.world.ScaffoldCheck;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.BlockPlacePacket;
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

class ScaffoldAndNukerRangeTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;
    private Vec3d here;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-scaffold"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        here = new Vec3d(0.5, 65.0, 0.5);
        world = new HashMap<>();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -1; dy <= 4; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    world.put(new BlockPos(dx, 64 + dy, dz).pack(),
                            new PlayerWorldCache.CachedBlock(
                                    dy == 0 ? BlockKind.SOLID : BlockKind.AIR, 1.0, "stone"));
                }
            }
        }
        applyWorld(true, 0.0f, 0.0f);
    }

    private void applyWorld(boolean onGroundBelow, float yaw, float pitch) {
        player.worldCache(PlayerWorldCache.of(here, world, 0.6, 15, true, onGroundBelow));
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(true);
        state.advanceRotation(yaw, pitch, System.nanoTime());
    }

    private void place(int x, int y, int z, int face) {
        core.enqueue(player.id(), new BlockPlacePacket(System.nanoTime(),
                new BlockPos(x, y, z).pack(), face, 0.5f, 0.5f, 0.5f, 0, 0));
        core.processQueue();
    }

    private void dig(int x, int y, int z) {
        core.enqueue(player.id(), new BlockBreakPacket(System.nanoTime(),
                BlockBreakPacket.BlockBreakAction.START,
                new BlockPos(x, y, z).pack(), 0, 0));
        core.processQueue();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("the facing limit leaves room to place what the player is aiming at")
    void facingLimitIsForgiving() {
        assertTrue(ScaffoldCheck.FACING_LIMIT > 20.0,
                "a real player places within a few degrees of where they look");
        assertTrue(ScaffoldCheck.FACING_LIMIT < 90.0,
                "and a placement behind them should still be caught");
    }

    @Test
    @DisplayName("placing with nothing in hand is flagged and blocked")
    void emptyHandPlacementIsFlagged() {
        for (int i = 0; i < 4; i++) {
            place(0, 64, 0, 0);
        }
        assertTrue(flags("scaffold") > 0, "three placements with an empty hand in a row should be caught");
    }

    @Test
    @DisplayName("the empty hand report names the mode and the empty slot")
    void emptyHandEvidenceNamesTheMode() {
        for (int i = 0; i < 4; i++) {
            place(0, 64, 0, 0);
        }
        var evidence = player.history().stream()
                .filter(record -> "scaffold".equals(record.checkKey()))
                .reduce((first, second) -> second)
                .orElseThrow()
                .evidence();
        assertEquals("empty hand", evidence.get("mode"));
        assertTrue(evidence.containsKey("heldItemSlot"),
                "the evidence should record what was in hand: " + evidence);
    }

    @Test
    @DisplayName("a single empty hand placement is forgiven")
    void oneEmptyHandIsForgiven() {
        place(0, 64, 0, 0);
        assertEquals(0, flags("scaffold"), "one mistake should not be a report");
    }

    @Test
    @DisplayName("digging a block behind a wall is flagged")
    void diggingBehindAWallIsFlagged() {
        world.put(new BlockPos(0, 65, 2).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        world.put(new BlockPos(0, 65, 3).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        world.put(new BlockPos(0, 65, 4).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        applyWorld(true, 0.0f, 0.0f);
        for (int i = 0; i < 3; i++) {
            dig(0, 65, 6);
        }
        assertTrue(flags("nuker") > 0, "digging behind a solid block should be caught");
    }

    @Test
    @DisplayName("digging the block in front is never flagged")
    void diggingInFrontIsClean() {
        world.put(new BlockPos(0, 65, 3).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        applyWorld(true, 0.0f, 0.0f);
        for (int i = 0; i < 2; i++) {
            dig(0, 65, 3);
        }
        assertEquals(0, flags("nuker"), "the block in front of the player is in sight");
    }

    @Test
    @DisplayName("the out of sight report names the mode and the block")
    void outOfSightEvidenceNamesTheMode() {
        world.put(new BlockPos(0, 65, 2).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        world.put(new BlockPos(0, 65, 3).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        world.put(new BlockPos(0, 65, 4).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        applyWorld(true, 0.0f, 0.0f);
        for (int i = 0; i < 3; i++) {
            dig(0, 65, 6);
        }
        var evidence = player.history().stream()
                .filter(record -> "nuker".equals(record.checkKey()))
                .reduce((first, second) -> second)
                .orElseThrow()
                .evidence();
        assertEquals("out of sight", evidence.get("mode"));
        assertTrue(String.valueOf(evidence.get("block")).contains("6"),
                "the evidence should name the block: " + evidence);
    }

    @Test
    @DisplayName("digging a block the server has never sent is not judged on sight")
    void unknownBlockIsNotJudged() {
        applyWorld(true, 0.0f, 0.0f);
        world.remove(new BlockPos(0, 65, 7).pack());
        applyWorld(true, 0.0f, 0.0f);
        for (int i = 0; i < 3; i++) {
            dig(0, 65, 7);
        }
        assertEquals(0, flags("nuker"),
                "the server cannot judge sight to a block it has no data for");
    }

    @Test
    @DisplayName("the category of both checks is unchanged")
    void categoriesUnchanged() {
        assertEquals(CheckCategory.WORLD,
                new ScaffoldCheck().category());
        assertEquals(CheckCategory.WORLD,
                new NukerCheck().category());
        assertFalse(new ScaffoldCheck().description().isEmpty());
        assertFalse(new NukerCheck().description().isEmpty());
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
