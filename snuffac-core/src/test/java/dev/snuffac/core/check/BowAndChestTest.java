package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.ArmAnimationPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.WindowClickPacket;
import dev.snuffac.core.player.EquipmentState.HeldKind;
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

class BowAndChestTest {

    private SnuffCore core;
    private PlayerData player;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-bow"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        Map<Long, PlayerWorldCache.CachedBlock> world = new HashMap<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        var here = new Vec3d(0.5, 65.0, 0.5);
        player.worldCache(PlayerWorldCache.of(here, world, 0.6, 15, true, true));
        player.movement().advanceTo(here, System.nanoTime());
        player.movement().advanceGround(true);
        for (int i = 0; i < 4; i++) {
            core.tick();
        }
    }

    private void isolate(String key) {
        for (var check : core.registry().all()) {
            if (!check.key().equals(key)) {
                core.registry().config(check).enabled(false);
            }
        }
    }

    private void hold(HeldKind kind) {
        player.equipment().update(1.0, 1.0, true, 0, false, false, kind);
    }

    private void aim(float yaw, long nanos) {
        var state = player.movement();
        state.advanceRotation(yaw, 0.0f, nanos);
        core.enqueue(player.id(), new MovementPacket(nanos, false, true,
                state.position(), yaw, 0.0f, true, false));
        core.processQueue();
    }

    private void release(long nanos) {
        core.enqueue(player.id(), new ArmAnimationPacket(nanos, 0));
        core.processQueue();
    }

    private void click(long nanos, int window, int slot) {
        core.enqueue(player.id(), new WindowClickPacket(nanos, window, slot, 0, 0));
        core.processQueue();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("a bow released on the vanilla draw time is clean")
    void vanillaDrawIsClean() {
        isolate("fastbow");
        hold(HeldKind.BOW);
        long base = System.currentTimeMillis() + 60_000L;
        for (int round = 0; round < 5; round++) {
            long at = base + round * 2000L;
            release(at * 1_000_000L);
            release((at + 400L) * 1_000_000L);
        }
        assertFalse(flags("fastbow") > 0, "no round here is shorter than the draw time");
    }

    @Test
    @DisplayName("a bow released almost instantly is caught")
    void instantDrawIsCaught() {
        isolate("fastbow");
        hold(HeldKind.BOW);
        long base = System.currentTimeMillis() + 60_000L;
        for (int round = 0; round < 6; round++) {
            long at = base + round * 100L;
            release(at * 1_000_000L);
            release((at + 60L) * 1_000_000L);
        }
        assertTrue(flags("fastbow") > 0,
                "releasing 60ms after drawing means the arrow has no charge");
    }

    @Test
    @DisplayName("a swing with no bow in hand is never fastbow")
    void noBowIsClean() {
        isolate("fastbow");
        hold(HeldKind.OTHER);
        long base = System.currentTimeMillis() + 60_000L;
        for (int round = 0; round < 8; round++) {
            release((base + round * 500L) * 1_000_000L);
        }
        assertFalse(flags("fastbow") > 0, "melee swings are not bow shots");
    }

    @Test
    @DisplayName("a chest emptied on a fixed cadence is caught")
    void fixedChestCadenceIsCaught() {
        isolate("cheststealer");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 14; i++) {
            click((base + i * 40L) * 1_000_000L, 9, i);
        }
        assertTrue(flags("cheststealer") > 0, "a click every 40ms is not a person looting");
    }

    @Test
    @DisplayName("a chest looted by hand is clean")
    void humanChestLootIsClean() {
        isolate("cheststealer");
        long base = System.currentTimeMillis() + 60_000L;
        long[] times = {0L, 600L, 1500L, 2100L, 3400L, 4200L, 5800L, 6600L, 7400L};
        for (int i = 0; i < times.length; i++) {
            click((base + times[i]) * 1_000_000L, 9, i);
        }
        assertFalse(flags("cheststealer") > 0, "picking items up one at a time is normal");
    }

    @Test
    @DisplayName("clicking the player inventory is never cheststealer")
    void playerInventoryClicksAreClean() {
        isolate("cheststealer");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 14; i++) {
            click((base + i * 30L) * 1_000_000L, 0, i);
        }
        assertFalse(flags("cheststealer") > 0, "rearranging your own inventory is allowed");
    }

    @Test
    @DisplayName("the visible container range is the real one")
    void visibleRangeIsCorrect() {
        assertTrue(dev.snuffac.core.check.impl.combat.ChestStealerCheck.inVisibleRange(0));
        assertTrue(dev.snuffac.core.check.impl.combat.ChestStealerCheck.inVisibleRange(53));
        assertFalse(dev.snuffac.core.check.impl.combat.ChestStealerCheck.inVisibleRange(54));
        assertFalse(dev.snuffac.core.check.impl.combat.ChestStealerCheck.inVisibleRange(-1));
    }

    @Test
    @DisplayName("identical bow corrections are flagged and varied ones are not")
    void bowCorrectionSpread() {
        var identical = new java.util.ArrayList<Double>();
        for (int i = 0; i < 4; i++) {
            identical.add(20.0);
        }
        assertTrue(dev.snuffac.core.check.impl.combat.BowAimbotCheck.spread(identical) < 0.75,
                "the same correction four times has no spread");

        var varied = new java.util.ArrayList<Double>();
        varied.add(8.0);
        varied.add(31.0);
        varied.add(12.5);
        varied.add(44.0);
        assertTrue(dev.snuffac.core.check.impl.combat.BowAimbotCheck.spread(varied) > 5.0,
                "a human aim correction varies widely");
    }

    @Test
    @DisplayName("a bow snapped to the same correction each time is caught")
    void repeatedCorrectionIsCaught() {
        isolate("bowaimbot");
        hold(HeldKind.BOW);
        long base = System.currentTimeMillis() + 60_000L;
        float yaw = 0.0f;
        for (int round = 0; round < 6; round++) {
            long at = base + round * 700L;
            aim(yaw, at * 1_000_000L);
            yaw += 20.0f;
            aim(yaw, (at + 10L) * 1_000_000L);
            release((at + 20L) * 1_000_000L);
        }
        assertTrue(flags("bowaimbot") > 0,
                "exactly twenty degrees of correction before every shot is not a hand");
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