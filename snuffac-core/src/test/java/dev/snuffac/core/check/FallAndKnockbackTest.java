package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.ServerVelocityPacket;
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

class FallAndKnockbackTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;
    private Vec3d here;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-fall"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        here = new Vec3d(0.5, 65.0, 0.5);
        world = new HashMap<>();
        world.put(new BlockPos(0, 64, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        apply(true);
        for (int i = 0; i < 4; i++) {
            core.tick();
        }
    }

    private void apply(boolean onGroundBelow) {
        player.worldCache(PlayerWorldCache.of(here, world, 0.6, 15, true, onGroundBelow));
    }

    private void step(Vec3d position, boolean onGround) {
        apply(onGround);
        var state = player.movement();
        state.advanceTo(position, System.nanoTime());
        state.advanceGround(onGround);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                position, 0.0f, 0.0f, onGround, false));
        core.processQueue();
        core.tick();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("a real fall from height is reported once it lands")
    void serverSideFallIsTracked() {
        double y = 90.0;
        for (int tick = 0; tick < 40; tick++) {
            step(new Vec3d(0.5, y, 0.5), false);
            y -= 0.6;
        }
        step(new Vec3d(0.5, 65.0, 0.5), true);
        assertTrue(flags("nofall") > 0,
                "a long fall the server watched the whole way is real fall damage");
    }

    @Test
    @DisplayName("a short hop is never reported as a server side fall")
    void shortHopIsClean() {
        for (int cycle = 0; cycle < 4; cycle++) {
            for (int tick = 0; tick < 3; tick++) {
                step(new Vec3d(0.5, 65.0 + tick * 0.4, 0.5), false);
            }
            step(new Vec3d(0.5, 65.0, 0.5), true);
        }
        assertFalse(flags("nofall") > 0, "a hop is not a fall");
    }

    @Test
    @DisplayName("standing on the ground never accumulates fall distance")
    void standingStillIsClean() {
        for (int tick = 0; tick < 60; tick++) {
            step(new Vec3d(0.5, 65.0, 0.5), true);
        }
        assertFalse(flags("nofall") > 0, "standing still is not falling");
    }

    @Test
    @DisplayName("a block under the feet stops the server fall from building")
    void landingOnGroundResets() {
        for (int tick = 0; tick < 10; tick++) {
            step(new Vec3d(0.5, 66.0 + tick * 0.1, 0.5), false);
        }
        apply(true);
        step(new Vec3d(0.5, 66.0, 0.5), true);
        for (int cycle = 0; cycle < 3; cycle++) {
            step(new Vec3d(0.5, 66.0, 0.5), true);
        }
        assertFalse(flags("nofall") > 0, "the ground is right there, nothing to report");
    }

    @Test
    @DisplayName("cobweb above the head absorbs knockback rather than being a cheat")
    void cobwebAbsorbsKnockback() {
        world.put(new BlockPos(0, 67, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.COBWEB, 0.6, "cobweb"));
        apply(false);
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        assertTrue(dev.snuffac.core.check.impl.movement.VelocityCheck
                        .absorbedByTerrain(context(), state),
                "a cobweb over the head is one of the listed causes that absorbs knockback");
    }

    @Test
    @DisplayName("water absorbs knockback rather than being a cheat")
    void waterAbsorbsKnockback() {
        world.put(new BlockPos(0, 65, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.WATER, 0.6, "water"));
        apply(false);
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        assertTrue(dev.snuffac.core.check.impl.movement.VelocityCheck
                        .absorbedByTerrain(context(), state),
                "water is one of the listed causes that absorbs knockback");
    }

    @Test
    @DisplayName("a ladder absorbs knockback rather than being a cheat")
    void ladderAbsorbsKnockback() {
        world.put(new BlockPos(0, 65, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.LADDER, 0.6, "ladder"));
        apply(false);
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        assertTrue(dev.snuffac.core.check.impl.movement.VelocityCheck
                        .absorbedByTerrain(context(), state),
                "a ladder is one of the listed causes that absorbs knockback");
    }

    @Test
    @DisplayName("open ground does not absorb knockback")
    void openGroundDoesNotAbsorb() {
        apply(false);
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        assertFalse(dev.snuffac.core.check.impl.movement.VelocityCheck
                        .absorbedByTerrain(context(), state),
                "knockback on flat ground should be measurable");
    }

    @Test
    @DisplayName("a knockback the client discards is still reported")
    void discardedKnockbackIsFlagged() {
        apply(false);
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        for (int i = 0; i < 4; i++) {
            core.enqueue(player.id(), new ServerVelocityPacket(
                    System.nanoTime(), player.entityId(), new Vec3d(0.9, 0.0, 0.0)));
            step(new Vec3d(0.5, 65.0, 0.5), false);
        }
        assertTrue(flags("velocity") > 0, "sending nothing back for a knockback is the cheat");
    }

    private dev.snuffac.core.check.CheckContext context() {
        return core.dispatcher().context(player, new dev.snuffac.core.check.impl.movement.VelocityCheck());
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
