package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.player.PlayerWorldCache;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LiquidAndWallDetectionTest {

    @Test
    @DisplayName("walking across open water with no block beneath is caught")
    void jesusOnOpenWaterIsCaught() {
        SnuffCore core = boot();
        PlayerData player = core.addPlayer(UUID.randomUUID(), "Tester",
                System.currentTimeMillis() - 60_000L);
        Map<Long, PlayerWorldCache.CachedBlock> world = new HashMap<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 14; dz++) {
                world.put(new BlockPos(dx, 59, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
                for (int depth = 60; depth <= 64; depth++) {
                    world.put(new BlockPos(dx, depth, dz).pack(),
                            new PlayerWorldCache.CachedBlock(BlockKind.WATER, 0.6, "water"));
                }
            }
        }
        for (int tick = 0; tick < 60; tick++) {
            step(core, player, world, new Vec3d(0.5, 64.05, 0.5 + tick * 0.2), tick > 0);
        }
        assertTrue(flagged(player, "jesus"),
                "moving across water with nothing underfoot is the WaterWalk cheat");
    }

    @Test
    @DisplayName("climbing a bare wall with nothing to climb is caught")
    void spiderOnBareWallIsCaught() {
        SnuffCore core = boot();
        PlayerData player = core.addPlayer(UUID.randomUUID(), "Tester",
                System.currentTimeMillis() - 60_000L);
        Map<Long, PlayerWorldCache.CachedBlock> world = new HashMap<>();
        for (int dy = 0; dy < 8; dy++) {
            world.put(new BlockPos(0, 64 + dy, 0).pack(),
                    new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        }
        for (int tick = 0; tick < 40; tick++) {
            step(core, player, world, new Vec3d(0.5, 65.0 + tick * 0.25, 0.5), tick > 0);
        }
        assertTrue(flagged(player, "spider"),
                "rising beside solid blocks with no ladder is the WallClimb cheat");
    }

    private static SnuffCore boot() {
        SnuffCore core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-detect"));
        return core;
    }

    private static void step(
            SnuffCore core, PlayerData player,
            Map<Long, PlayerWorldCache.CachedBlock> world, Vec3d position, boolean onGround) {

        player.worldCache(PlayerWorldCache.of(position, world, 0.6, 15, true, onGround));
        var state = player.movement();
        state.advanceTo(position, System.nanoTime());
        state.advanceGround(onGround);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                position, 0.0f, 0.0f, onGround, false));
        core.processQueue();
        core.tick();
    }

    private static boolean flagged(PlayerData player, String key) {
        return player.history().stream().anyMatch(record -> key.equals(record.checkKey()));
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
