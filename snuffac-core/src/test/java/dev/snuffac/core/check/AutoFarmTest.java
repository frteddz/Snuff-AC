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
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoFarmTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;
    private Vec3d here;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-farm"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        here = new Vec3d(0.5, 65.0, 0.5);
        world = new HashMap<>();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    world.put(new BlockPos(dx, 64 + dy, dz).pack(),
                            new PlayerWorldCache.CachedBlock(
                                    dy == 0 ? BlockKind.SOLID : BlockKind.AIR, 1.0, "stone"));
                }
            }
        }
        apply();
        for (int i = 0; i < 5; i++) {
            core.tick();
        }
    }

    private void apply() {
        moveTo(here);
    }

    private void moveTo(Vec3d position) {
        player.worldCache(PlayerWorldCache.of(position, world, 0.6, 15, true, true));
        var state = player.movement();
        state.advanceTo(position, System.nanoTime());
        state.advanceGround(true);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                position, 0.0f, 0.0f, true, false));
        core.processQueue();
        core.tick();
    }

    private void isolate(String key) {
        for (var check : core.registry().all()) {
            if (!check.key().equals(key)) {
                core.registry().config(check).enabled(false);
            }
        }
    }

    private void finishDig(int x, int y, int z, long millis) {
        core.enqueue(player.id(), new BlockBreakPacket(millis * 1_000_000L,
                BlockBreakPacket.BlockBreakAction.FINISH,
                new BlockPos(x, y, z).pack(), 0, 0));
        core.processQueue();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("mining at an even pace in one spot is caught")
    void rigidMiningIsCaught() {
        isolate("autofarm");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 60; i++) {
            apply();
            finishDig(i % 2, 65, 0, base + i * 500L);
        }
        assertTrue(flags("autofarm") > 0,
                "sixty blocks at exactly the same interval in the same spot is automation");
    }

    @Test
    @DisplayName("mining at a human pace is clean")
    void humanMiningIsClean() {
        isolate("autofarm");
        Random random = new Random(20261004L);
        long base = System.currentTimeMillis() + 60_000L;
        long at = base;
        for (int i = 0; i < 60; i++) {
            apply();
            at += 300L + Math.round(random.nextGaussian() * 260.0);
            finishDig(i % 3 - 1, 65, 0, at);
        }
        assertFalse(flags("autofarm") > 0, "a hand varies its own timing");
    }

    @Test
    @DisplayName("a wanderer working many blocks is clean")
    void variedPathIsClean() {
        isolate("autofarm");
        Random random = new Random(20261005L);
        long base = System.currentTimeMillis() + 60_000L;
        long at = base;
        for (int i = 0; i < 60; i++) {
            int x = random.nextInt(7) - 3;
            int z = random.nextInt(7) - 3;
            moveTo(new Vec3d(x + 0.5, 65.0, z + 0.5));
            at += 500L;
            finishDig(x, 65, z, at);
        }
        assertFalse(flags("autofarm") > 0, "moving around and mining is a person");
    }

    @Test
    @DisplayName("a tight path has a small spread")
    void tightPathIsTight() {
        ArrayDeque<Vec3d> tight = new ArrayDeque<>();
        for (int i = 0; i < 24; i++) {
            tight.add(new Vec3d(0.5, 0.0, 0.5));
        }
        assertTrue(dev.snuffac.core.check.impl.world.AutoFarmCheck.pathIsTight(tight),
                "standing in one place is a tight path");

        Random scatter = new Random(20261007L);
        ArrayDeque<Vec3d> loose = new ArrayDeque<>();
        for (int i = 0; i < 24; i++) {
            loose.add(new Vec3d(scatter.nextInt(16) - 8, 0.0, scatter.nextInt(16) - 8));
        }
        assertFalse(dev.snuffac.core.check.impl.world.AutoFarmCheck.pathIsTight(loose),
                "walking all over the place is not a tight path");
    }

    @Test
    @DisplayName("interval jitter is the spread around the mean")
    void intervalJitterIsMeasured() {
        ArrayDeque<Long> even = new ArrayDeque<>();
        for (int i = 0; i < 12; i++) {
            even.add(500L);
        }
        assertTrue(dev.snuffac.core.check.impl.world.AutoFarmCheck.intervalJitter(even) == 0.0,
                "a constant interval has no jitter");

        ArrayDeque<Long> varied = new ArrayDeque<>();
        Random random = new Random(20261006L);
        for (int i = 0; i < 12; i++) {
            varied.add(500L + Math.round(random.nextGaussian() * 300.0));
        }
        assertTrue(dev.snuffac.core.check.impl.world.AutoFarmCheck.intervalJitter(varied) > 0.2,
                "a hand's timing has real spread");
    }

    @Test
    @DisplayName("the window needs enough events before it judges")
    void windowNeedsEvents() {
        assertTrue(dev.snuffac.core.check.impl.world.AutoFarmCheck.MIN_EVENTS >= 20,
                "a handful of blocks is not a farming session");
        assertTrue(dev.snuffac.core.check.impl.world.AutoFarmCheck.REQUIRED_WINDOWS >= 2,
                "one rigid window could be a coincidence");
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