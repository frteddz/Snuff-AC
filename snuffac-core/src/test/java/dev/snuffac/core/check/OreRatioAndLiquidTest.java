package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.BlockPlacePacket;
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

class OreRatioAndLiquidTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;
    private Vec3d here;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-ore"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        here = new Vec3d(0.5, 65.0, 0.5);
        world = new HashMap<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        apply();
        for (int i = 0; i < 5; i++) {
            core.tick();
        }
    }

    private void apply() {
        player.worldCache(PlayerWorldCache.of(here, world, 0.6, 15, true, true));
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(true);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                here, 0.0f, 0.0f, true, false));
        core.processQueue();
    }

    private void isolate(String key) {
        for (var check : core.registry().all()) {
            if (!check.key().equals(key)) {
                core.registry().config(check).enabled(false);
            }
        }
    }

    private void dig(int x, int y, int z) {
        core.enqueue(player.id(), new BlockBreakPacket(System.nanoTime(),
                BlockBreakPacket.BlockBreakAction.START,
                new BlockPos(x, y, z).pack(), 0, 0));
        core.processQueue();
    }

    private void place(int x, int y, int z, int face) {
        core.enqueue(player.id(), new BlockPlacePacket(System.nanoTime(),
                new BlockPos(x, y, z).pack(), face, 0.5f, 0.5f, 0.5f, 0, 0));
        core.processQueue();
    }

    private void ore(int x, int z) {
        world.put(new BlockPos(x, 64, z).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.AIR, 3.0, "diamond_ore"));
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("mining mostly stone is clean")
    void stoneMiningIsClean() {
        isolate("oreratio");
        for (int i = 0; i < 60; i++) {
            dig(i % 5 - 2, 64, (i / 5) % 5 - 2);
        }
        assertFalse(flags("oreratio") > 0, "digging stone is what a player does");
    }

    @Test
    @DisplayName("a mining path that is mostly ore is caught")
    void oreHeavyMiningIsCaught() {
        isolate("oreratio");
        apply();
        for (int i = 0; i < 60; i++) {
            ore(i % 5 - 2, (i / 5) % 5 - 2);
            apply();
            dig(i % 5 - 2, 64, (i / 5) % 5 - 2);
        }
        assertTrue(flags("oreratio") > 0,
                "digging valuable ore in a quarter of your blocks is not ordinary mining");
    }

    @Test
    @DisplayName("the ore ratio limit is a quarter of the path")
    void ratioLimitIsSane() {
        assertTrue(dev.snuffac.core.check.impl.world.OreRatioCheck.VALUABLE_RATIO >= 0.2,
                "a fifth is generous enough for a lucky player");
        assertTrue(dev.snuffac.core.check.impl.world.OreRatioCheck.VALUABLE_RATIO <= 0.3,
                "and tight enough that ordinary mining never reaches it");
        assertTrue(dev.snuffac.core.check.impl.world.OreRatioCheck.MIN_SAMPLES >= 20,
                "a handful of blocks proves nothing");
    }

    @Test
    @DisplayName("using a block straight in front is clean")
    void facingABlockIsClean() {
        isolate("liquidinteract");
        world.put(new BlockPos(0, 65, 3).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        apply();
        for (int i = 0; i < 6; i++) {
            place(0, 65, 3, 2);
        }
        assertFalse(flags("liquidinteract") > 0,
                "a block straight ahead at eye level is where the player is looking");
    }

    @Test
    @DisplayName("reaching through a wall is caught")
    void reachingThroughAWallIsCaught() {
        isolate("liquidinteract");
        for (int dz = 1; dz <= 3; dz++) {
            world.put(new BlockPos(0, 65, dz).pack(),
                    new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        }
        apply();
        for (int i = 0; i < 8; i++) {
            place(0, 65, 7, 2);
        }
        assertTrue(flags("liquidinteract") > 0,
                "seven blocks away is past what a client can reach, wall or not");
    }

    @Test
    @DisplayName("reaching behind the player is caught")
    void reachingBehindIsCaught() {
        isolate("liquidinteract");
        world.put(new BlockPos(0, 65, -2).pack(),
                new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
        apply();
        for (int i = 0; i < 8; i++) {
            place(0, 65, -2, 2);
        }
        assertTrue(flags("liquidinteract") > 0,
                "the player is looking the other way entirely");
    }

    @Test
    @DisplayName("the liquid crossing test finds water between two points")
    void liquidCrossingIsDetected() {
        Map<Long, PlayerWorldCache.CachedBlock> wet = new HashMap<>(world);
        for (int dz = 2; dz <= 4; dz++) {
            wet.put(new BlockPos(0, 65, dz).pack(),
                    new PlayerWorldCache.CachedBlock(BlockKind.WATER, 0.6, "water"));
        }
        player.worldCache(PlayerWorldCache.of(here, wet, 0.6, 15, true, true));
        var context = core.dispatcher().context(player,
                new dev.snuffac.core.check.impl.world.LiquidInteractCheck());
        boolean through = dev.snuffac.core.check.impl.world.LiquidInteractCheck
                .crossesLiquid(context, new Vec3d(0.5, 65.6, 0.5), new Vec3d(0.5, 65.6, 6.5));
        assertTrue(through, "water between the eye and the target should be found");

        boolean clear = dev.snuffac.core.check.impl.world.LiquidInteractCheck
                .crossesLiquid(context, new Vec3d(0.5, 65.6, 0.5), new Vec3d(3.5, 65.6, 0.5));
        assertFalse(clear, "a path with no water in it should not be reported");
    }

    @Test
    @DisplayName("the printer needs several evenly spaced placements")
    void printerNeedsARun() {
        assertTrue(dev.snuffac.core.check.impl.world.ScaffoldCheck.PRINTER_RUN >= 4,
                "three placements in a row is a person building");
        assertTrue(dev.snuffac.core.check.impl.world.ScaffoldCheck.PRINTER_INTERVAL >= 0.5,
                "and the spacing floor has to exclude fast human building");
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