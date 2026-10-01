package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

class StrafeAndSafewalkTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-strafe"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        world = new HashMap<>();
        start();
        for (int i = 0; i < 5; i++) {
            core.tick();
        }
    }

    private void start() {
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        place(new Vec3d(0.5, 65.0, 0.5), true);
    }

    private void place(Vec3d position, boolean onGround) {
        player.worldCache(PlayerWorldCache.of(position, world, 0.6, 15, true, onGround));
        var state = player.movement();
        state.advanceTo(position, System.nanoTime());
        state.advanceGround(onGround);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                position, 0.0f, 0.0f, onGround, false));
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

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("the angle between two vectors is measured correctly")
    void angleBetweenIsCorrect() {
        double straight = dev.snuffac.core.check.impl.movement.StrafeCheck
                .angleBetween(new Vec3d(1, 0, 0), new Vec3d(1, 0, 0));
        assertTrue(straight < 1.0E-6, "the same direction is no turn");
        double right = dev.snuffac.core.check.impl.movement.StrafeCheck
                .angleBetween(new Vec3d(1, 0, 0), new Vec3d(0, 0, 1));
        assertTrue(Math.abs(right - 90.0) < 1.0E-6, "a right angle is 90, got " + right);
        double opposite = dev.snuffac.core.check.impl.movement.StrafeCheck
                .angleBetween(new Vec3d(1, 0, 0), new Vec3d(-1, 0, 0));
        assertTrue(Math.abs(opposite - 180.0) < 1.0E-6, "reversing is 180, got " + opposite);
    }

    @Test
    @DisplayName("a zero vector is not counted as a turn")
    void zeroVectorIsNotATurn() {
        double turn = dev.snuffac.core.check.impl.movement.StrafeCheck
                .angleBetween(Vec3d.ZERO, new Vec3d(1, 0, 0));
        assertTrue(turn == 0.0, "a standing still player has no direction");
    }

    @Test
    @DisplayName("steering hard back and forth in mid air is caught")
    void hardStrafeIsCaught() {
        isolate("strafe");
        place(new Vec3d(0.5, 80.0, 0.5), false);
        for (int cycle = 0; cycle < 12; cycle++) {
            double dx = cycle % 2 == 0 ? 0.4 : -0.4;
            place(new Vec3d(0.5 + dx, 80.0 + cycle * 0.1, 0.5), false);
        }
        assertTrue(flags("strafe") > 0,
                "reversing horizontal direction every tick is not something keys do");
    }

    @Test
    @DisplayName("a steady glide in one direction is clean")
    void steadyAirIsClean() {
        isolate("strafe");
        place(new Vec3d(0.5, 80.0, 0.5), false);
        double z = 0.5;
        for (int tick = 0; tick < 40; tick++) {
            z += 0.2;
            place(new Vec3d(0.5, 80.0 + tick * 0.05, z), false);
        }
        assertFalse(flags("strafe") > 0, "flying straight ahead is not cheating");
    }

    @Test
    @DisplayName("standing on solid ground away from an edge is not safewalk")
    void flatGroundIsClean() {
        isolate("safewalk");
        for (int cycle = 0; cycle < 10; cycle++) {
            for (int tick = 0; tick < 6; tick++) {
                place(new Vec3d(0.5, 65.0, 0.5), true);
            }
            for (int tick = 0; tick < 6; tick++) {
                place(new Vec3d(0.5 + tick * 0.2, 65.0, 0.5), true);
            }
        }
        assertFalse(flags("safewalk") > 0,
                "standing still in the middle of a floor is just standing still");
    }

    @Test
    @DisplayName("a plain floor block is never an edge")
    void solidFootIsNotAnEdge() {
        assertFalse(dev.snuffac.core.check.impl.movement.SafewalkCheck.atEdge(
                cache(new Vec3d(0.5, 65.0, 0.5)),
                new Vec3d(0.5, 65.0, 0.5)),
                "there is floor under and beside the player");
    }

    @Test
    @DisplayName("a gap in the floor beside the player is an edge")
    void gapIsAnEdge() {
        Map<Long, PlayerWorldCache.CachedBlock> gapped = new HashMap<>(world);
        for (int dz = -12; dz <= 12; dz++) {
            gapped.remove(new BlockPos(2, 64, dz).pack());
        }
        var here = new Vec3d(1.5, 65.0, 0.5);
        var gappedCache = PlayerWorldCache.of(here, gapped, 0.6, 15, true, true);
        assertTrue(dev.snuffac.core.check.impl.movement.SafewalkCheck.atEdge(gappedCache, here),
                "the floor beside the player has been removed");
    }

    private PlayerWorldCache cache(Vec3d centre) {
        return PlayerWorldCache.of(centre, world, 0.6, 15, true, true);
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
