package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlinkAndTimerBalanceTest {

    private SnuffCore core;
    private PlayerData player;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-blink"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        Map<Long, PlayerWorldCache.CachedBlock> world = new HashMap<>();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        player.worldCache(PlayerWorldCache.of(
                new Vec3d(0.5, 65.0, 0.5), world, 0.6, 15, true, true));
        var state = player.movement();
        state.advanceTo(new Vec3d(0.5, 65.0, 0.5), System.nanoTime());
        state.advanceGround(true);
        for (int i = 0; i < 5; i++) {
            core.tick();
        }
    }

    private void send(Vec3d position, long arrivalNanos) {
        var state = player.movement();
        state.advanceTo(position, arrivalNanos);
        state.advanceGround(true);
        core.enqueue(player.id(), new MovementPacket(arrivalNanos, true, false,
                position, 0.0f, 0.0f, true, false));
        core.processQueue();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("packets held back then released in a burst are caught")
    void blinkBurstIsCaught() {
        long base = System.nanoTime() + 1_000_000_000L;
        int index = 0;
        for (int round = 0; round < 3; round++) {
            long gap = base + round * 2_000_000_000L;
            for (int i = 0; i < 6; i++) {
                send(new Vec3d(0.5 + index * 0.2, 65.0, 0.5), gap + i * 1_000_000L);
                index++;
            }
        }
        assertTrue(flags("blink") > 0,
                "a long silence followed by many packets at once is the Blink cheat");
    }

    @Test
    @DisplayName("an even packet rate is never blink")
    void evenRateIsClean() {
        long base = System.nanoTime() + 1_000_000_000L;
        for (int i = 0; i < 60; i++) {
            send(new Vec3d(0.5 + i * 0.1, 65.0, 0.5), base + i * 50_000_000L);
        }
        assertEquals(0, flags("blink"), "a client sending on every tick is not blinking");
    }

    @Test
    @DisplayName("the blink gap is longer than one vanilla tick")
    void gapThresholdIsSane() {
        assertTrue(dev.snuffac.core.check.impl.net.BlinkCheck.GAP_NANOS > 50_000_000L,
                "a single dropped tick is not a blink");
    }

    @Test
    @DisplayName("the blink burst needs several packets, not one late arrival")
    void burstThresholdIsSane() {
        assertTrue(dev.snuffac.core.check.impl.net.BlinkCheck.BURST_PACKETS >= 4,
                "one late packet after lag is not a burst");
    }

    @Test
    @DisplayName("the timer balance window is shorter than the hard threshold allows")
    void balanceRunsBeforeTheHardLimit() {
        assertTrue(dev.snuffac.core.check.impl.net.TimerCheck.BALANCE_RATIO
                        < dev.snuffac.core.check.impl.net.TimerCheck.POSITIVE_THRESHOLD,
                "the balance has to fire before the existing hard threshold");
        assertTrue(dev.snuffac.core.check.impl.net.TimerCheck.BALANCE_WINDOWS >= 2,
                "one window is not enough evidence");
    }

    @Test
    @DisplayName("the timer balance only counts a rate above the tick rate")
    void balanceOnlyCountsAboveOne() {
        assertTrue(dev.snuffac.core.check.impl.net.TimerCheck.BALANCE_RATIO > 1.0,
                "a client sending exactly one packet per tick is not cheating");
        assertTrue(dev.snuffac.core.check.impl.net.TimerCheck.BALANCE_RATIO < 1.1,
                "but a small margin above one is still worth watching");
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
