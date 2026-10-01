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

class ElytraAndVehicleTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-elytra"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        world = new HashMap<>();
        for (int dx = -30; dx <= 30; dx++) {
            for (int dz = -30; dz <= 30; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        start(new Vec3d(0.5, 90.0, 0.5));
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

    private void start(Vec3d position) {
        player.worldCache(PlayerWorldCache.of(position, world, 0.6, 15, true, false));
        var state = player.movement();
        state.advanceTo(position, System.nanoTime());
        state.advanceGround(false);
    }

    private void glide(Vec3d position, double step, int ticks) {
        var state = player.movement();
        state.gliding(true);
        double x = position.x();
        double z = position.z();
        for (int tick = 0; tick < ticks; tick++) {
            x += step;
            Vec3d next = new Vec3d(x, position.y() - 0.05, z);
            player.worldCache(PlayerWorldCache.of(next, world, 0.6, 15, true, false));
            state.advanceTo(next, System.nanoTime());
            state.advanceGround(false);
            state.gliding(true);
            core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                    next, 0.0f, 0.0f, false, false));
            core.processQueue();
            core.tick();
        }
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("gliding with no elytra equipped is caught")
    void glidingWithNoElytraIsCaught() {
        isolate("elytrafly");
        glide(new Vec3d(0.5, 90.0, 0.5), 0.4, 30);
        assertTrue(flags("elytrafly") > 0,
                "the server sees the player gliding and no elytra on their chest");
    }

    @Test
    @DisplayName("gliding at the vanilla speed with an elytra is clean")
    void normalGlideWithElytraIsClean() {
        isolate("elytrafly");
        player.equipment().update(1.0, 1.0, true, 0, false, true);
        glide(new Vec3d(0.5, 90.0, 0.5), 0.4, 60);
        assertFalse(flags("elytrafly") > 0,
                "a real elytra glide is a little over 3 blocks per tick");
    }

    @Test
    @DisplayName("gliding faster than the unboosted limit is caught")
    void overUnboostedLimitIsCaught() {
        isolate("elytrafly");
        player.equipment().update(1.0, 1.0, true, 0, false, true);
        glide(new Vec3d(0.5, 90.0, 0.5), 4.5, 60);
        assertTrue(flags("elytrafly") > 0,
                "4.5 blocks a tick is above the unboosted figure and no rocket was used");
    }

    @Test
    @DisplayName("gliding fast with a fresh rocket is not caught")
    void boostedGlideIsClean() {
        isolate("elytrafly");
        player.equipment().update(1.0, 1.0, true, 0, false, true);
        player.movement().markBoost();
        glide(new Vec3d(0.5, 90.0, 0.5), 4.5, 40);
        assertFalse(flags("elytrafly") > 0, "a boosted glide is allowed to be fast");
    }

    @Test
    @DisplayName("a vehicle carrying the player at vanilla speed is clean")
    void normalVehicleSpeedIsClean() {
        isolate("vehicle");
        var state = player.movement();
        state.inVehicle(true);
        state.riding(true);
        double x = 0.5;
        for (int tick = 0; tick < 40; tick++) {
            x += 0.5;
            Vec3d next = new Vec3d(x, 65.0, 0.5);
            player.worldCache(PlayerWorldCache.of(next, world, 0.6, 15, true, false));
            state.advanceTo(next, System.nanoTime());
            state.advanceGround(false);
            state.inVehicle(true);
            state.riding(true);
            core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                    next, 0.0f, 0.0f, false, false));
            core.processQueue();
            core.tick();
        }
        assertFalse(flags("vehicle") > 0, "half a block a tick in a boat is ordinary");
    }

    @Test
    @DisplayName("a vehicle carrying the player far too fast is caught")
    void absurdVehicleSpeedIsCaught() {
        isolate("vehicle");
        var state = player.movement();
        state.inVehicle(true);
        state.riding(true);
        double x = 0.5;
        for (int tick = 0; tick < 40; tick++) {
            x += 2.5;
            Vec3d next = new Vec3d(x, 65.0, 0.5);
            player.worldCache(PlayerWorldCache.of(next, world, 0.6, 15, true, false));
            state.advanceTo(next, System.nanoTime());
            state.advanceGround(false);
            state.inVehicle(true);
            state.riding(true);
            core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                    next, 0.0f, 0.0f, false, false));
            core.processQueue();
            core.tick();
        }
        assertTrue(flags("vehicle") > 0, "two and a half blocks a tick in a boat is not vanilla");
    }

    @Test
    @DisplayName("the elytra limits are the documented vanilla figures")
    void elytraLimitsAreVanilla() {
        assertTrue(dev.snuffac.core.check.impl.movement.ElytraCheck.MAX_UNBOOSTED > 3.0,
                "an unboosted glide is a little over 3 blocks per tick");
        assertTrue(dev.snuffac.core.check.impl.movement.ElytraCheck.MAX_UNBOOSTED < 3.5,
                "and well under the boosted figure");
        assertTrue(dev.snuffac.core.check.impl.movement.ElytraCheck.BOOSTED >= 7.0,
                "a rocket boosted glide is much faster");
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
