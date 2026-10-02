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

class HungerJumpSprintTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-hunger"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        world = new HashMap<>();
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        step(0.0, 0.0, 65.0, true);
        for (int i = 0; i < 5; i++) {
            core.tick();
        }
    }

    private void step(double dx, double dz, double y, boolean onGround) {
        var current = player.movement().position();
        var next = new Vec3d(current.x() + dx, y, current.z() + dz);
        player.worldCache(PlayerWorldCache.of(next, world, 0.6, 15, true, onGround));
        var state = player.movement();
        state.advanceTo(next, System.nanoTime());
        state.advanceGround(onGround);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, true,
                next, state.yaw(), 0.0f, onGround, false));
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
    @DisplayName("the facing offset between look direction and travel is measured")
    void facingOffsetIsMeasured() {
        assertTrue(dev.snuffac.core.check.impl.movement.SprintSneakCheck.facingAngle(
                0.0, new Vec3d(0, 0, 1)) < 1.0E-6, "walking forward with yaw 0 is no offset");
        assertTrue(Math.abs(dev.snuffac.core.check.impl.movement.SprintSneakCheck.facingAngle(
                0.0, new Vec3d(-1, 0, 0)) - 90.0) < 1.0E-6, "left is ninety degrees");
        assertTrue(Math.abs(dev.snuffac.core.check.impl.movement.SprintSneakCheck.facingAngle(
                0.0, new Vec3d(0, 0, -1)) - 180.0) < 1.0E-6, "backwards is a hundred and eighty");
    }

    @Test
    @DisplayName("a player sprinting forwards is not omnidirectional sprint")
    void forwardSprintIsClean() {
        isolate("sprintsneak");
        var state = player.movement();
        state.sprinting(true);
        for (int tick = 0; tick < 20; tick++) {
            step(0.0, 0.28, 65.0, true);
            state.sprinting(true);
        }
        assertFalse(flags("sprintsneak") > 0, "sprinting forwards is what sprinting is");
    }

    @Test
    @DisplayName("sprinting backwards at full speed is caught")
    void backwardSprintIsCaught() {
        isolate("sprintsneak");
        var state = player.movement();
        state.sprinting(true);
        for (int tick = 0; tick < 20; tick++) {
            step(0.0, -0.28, 65.0, true);
            state.sprinting(true);
        }
        assertTrue(flags("sprintsneak") > 0,
                "the vanilla client cannot sprint backwards at full speed");
    }

    @Test
    @DisplayName("a block underneath means a jump has something to jump from")
    void groundBelowIsFound() {
        assertTrue(dev.snuffac.core.check.impl.movement.AirJumpCheck.hasGroundBelow(
                player.worldCache(), new Vec3d(0.5, 65.0, 0.5)),
                "there is a floor right there");
    }

    @Test
    @DisplayName("rising in the air with nothing underneath is caught")
    void airRiseIsCaught() {
        isolate("airjump");
        for (int tick = 0; tick < 20; tick++) {
            step(0.0, 0.0, 70.0 + tick * 0.3, false);
        }
        assertTrue(flags("airjump") > 0,
                "three ticks of rising with no floor is not a jump");
    }

    @Test
    @DisplayName("rising off a solid floor is a normal jump")
    void jumpFromGroundIsClean() {
        isolate("airjump");
        for (int cycle = 0; cycle < 4; cycle++) {
            step(0.0, 0.0, 65.0, true);
            for (int tick = 0; tick < 8; tick++) {
                step(0.0, 0.0, 65.4 + tick * 0.2, false);
            }
        }
        assertFalse(flags("airjump") > 0, "jumping off the ground is the game working");
    }

    @Test
    @DisplayName("sprinting without losing hunger is clean")
    void normalHungerLossIsClean() {
        isolate("antihunger");
        var state = player.movement();
        int food = 20;
        state.foodLevel(food);
        for (int tick = 0; tick < 120; tick++) {
            step(0.0, 0.28, 65.0, true);
            state.sprinting(true);
            if (tick % 2 == 1) {
                food = Math.max(0, food - 1);
                state.foodLevel(food);
            }
        }
        assertFalse(flags("antihunger") > 0,
                "losing hunger while sprinting is what exhaustion is for");
    }

    @Test
    @DisplayName("sprinting without losing any hunger is caught")
    void noHungerLossIsCaught() {
        isolate("antihunger");
        var state = player.movement();
        state.foodLevel(20);
        for (int tick = 0; tick < 120; tick++) {
            step(0.0, 0.28, 65.0, true);
            state.sprinting(true);
            state.foodLevel(20);
        }
        assertTrue(flags("antihunger") > 0,
                "earning exhaustion for a hundred ticks and losing no hunger is not vanilla");
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