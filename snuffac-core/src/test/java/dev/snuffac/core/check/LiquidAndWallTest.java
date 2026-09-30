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

class LiquidAndWallTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-liquid"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        world = new HashMap<>();
        world.put(new BlockPos(0, 63, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        world.put(new BlockPos(0, 64, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        apply(new Vec3d(0.5, 65.0, 0.5), true);
        for (int i = 0; i < 4; i++) {
            core.tick();
        }
    }

    private void apply(Vec3d position, boolean onGroundBelow) {
        player.worldCache(PlayerWorldCache.of(position, world, 0.6, 15, true, onGroundBelow));
        var state = player.movement();
        state.advanceTo(position, System.nanoTime());
        state.advanceGround(true);
    }

    private void step(Vec3d position, boolean onGround) {
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
    @DisplayName("standing in deep water is not walking on it")
    void deepWaterIsClean() {
        for (int dy = 0; dy >= -3; dy--) {
            world.put(new BlockPos(0, 65 + dy, 0).pack(),
                    new PlayerWorldCache.CachedBlock(BlockKind.WATER, 0.6, "water"));
        }
        for (int tick = 0; tick < 40; tick++) {
            step(new Vec3d(0.5, 65.0, 0.5 + tick * 0.1), false);
        }
        assertFalse(flags("jesus") > 0, "a swimmer at the surface is not Jesus");
    }

    @Test
    @DisplayName("standing on ice over water is not walking on it")
    void iceIsSupport() {
        world.put(new BlockPos(0, 64, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.ICE, 0.98, "ice"));
        world.put(new BlockPos(0, 65, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.WATER, 0.6, "water"));
        apply(new Vec3d(0.5, 65.0, 0.5), true);
        for (int tick = 0; tick < 40; tick++) {
            step(new Vec3d(0.5, 65.0, 0.5 + tick * 0.1), false);
        }
        assertFalse(flags("jesus") > 0, "frosted ice is a valid surface");
    }

    @Test
    @DisplayName("the jesus limit requires a real horizontal move")
    void stillWaterIsForgiven() {
        assertTrue(dev.snuffac.core.check.impl.movement.JesusCheck.MIN_HORIZONTAL > 0.05,
                "a player standing still in water must be forgiven");
    }

    @Test
    @DisplayName("rising against a ladder is not wall climbing")
    void ladderIsClean() {
        world.put(new BlockPos(1, 65, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        world.put(new BlockPos(1, 66, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.LADDER, 0.6, "ladder"));
        var state = player.movement();
        state.onClimbable(true);
        for (int tick = 0; tick < 20; tick++) {
            state.onClimbable(true);
            step(new Vec3d(0.5, 65.0 + tick * 0.2, 0.5), false);
        }
        assertFalse(flags("spider") > 0, "a ladder is the legitimate way up a wall");
    }

    @Test
    @DisplayName("rising against honey or soul sand is not wall climbing")
    void honeyIsClean() {
        world.put(new BlockPos(0, 65, 0).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.HONEY, 0.8, "honey_block"));
        var state = player.movement();
        for (int tick = 0; tick < 20; tick++) {
            state.onHoney(true);
            step(new Vec3d(0.5, 65.0 + tick * 0.2, 0.5), false);
        }
        assertFalse(flags("spider") > 0, "honey slows a rise without being a cheat");
    }

    @Test
    @DisplayName("a small rise with no wall is not wall climbing")
    void openAirRiseIsClean() {
        for (int tick = 0; tick < 20; tick++) {
            step(new Vec3d(0.5 + tick * 0.3, 65.0 + tick * 0.2, 0.5), false);
        }
        assertFalse(flags("spider") > 0, "rising in open space is a jump");
    }

    @Test
    @DisplayName("the spider rise threshold is above ordinary movement")
    void riseThresholdIsSensible() {
        assertTrue(dev.snuffac.core.check.impl.movement.SpiderCheck.MIN_RISE >= 0.1,
                "ordinary air control must stay under the threshold");
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
