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

class CooldownAndInstamineTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-cooldown"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        world = new HashMap<>();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    world.put(new BlockPos(dx, 64 + dy, dz).pack(),
                            new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 3.0, "stone"));
                }
            }
        }
        var here = new Vec3d(0.5, 65.0, 0.5);
        player.worldCache(PlayerWorldCache.of(here, world, 0.6, 15, true, true));
        player.movement().advanceTo(here, System.nanoTime());
        player.movement().advanceGround(true);
        for (int i = 0; i < 5; i++) {
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

    private void placeAt(long millis, int x, int y, int z) {
        core.enqueue(player.id(), new BlockPlacePacket(millis * 1_000_000L,
                new BlockPos(x, y, z).pack(), 1, 0.5f, 0.5f, 0.5f, 0, 0));
        core.processQueue();
    }

    private void finishAt(long millis, int x, int y, int z) {
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
    @DisplayName("the vanilla placement cooldown is the documented four ticks")
    void cooldownFigureIsVanilla() {
        assertTrue(dev.snuffac.core.check.impl.world.FastPlaceCheck.VANILLA_COOLDOWN_MILLIS == 50L,
                "four ticks at twenty per second");
        assertTrue(dev.snuffac.core.check.impl.world.FastPlaceCheck.REQUIRED_FAST >= 4,
                "a couple of fast placements is a person in a hurry");
    }

    @Test
    @DisplayName("placing faster than the cooldown is caught")
    void fastPlacingIsCaught() {
        isolate("fastplace");
        long base = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            placeAt(base + i * 5L, i % 3, 64, 0);
        }
        assertTrue(flags("fastplace") > 0,
                "eight placements five milliseconds apart ignores the cooldown");
    }

    @Test
    @DisplayName("placing at the cooldown is clean")
    void cooldownPlacingIsClean() {
        isolate("fastplace");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 6; i++) {
            placeAt(base + i * 120L, i % 3, 64, 0);
        }
        assertFalse(flags("fastplace") > 0, "a little over the cooldown every time is normal");
    }

    @Test
    @DisplayName("several hard blocks finished instantly is caught")
    void instantMiningIsCaught() {
        isolate("instamine");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 4; i++) {
            finishAt(base + i * 3L, i, 64, 0);
        }
        assertTrue(flags("instamine") > 0,
                "four stone blocks finished three milliseconds apart is an InstaMine");
    }

    @Test
    @DisplayName("mining at a real pace is clean")
    void pacedMiningIsClean() {
        isolate("instamine");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 4; i++) {
            finishAt(base + i * 400L, i, 64, 0);
        }
        assertFalse(flags("instamine") > 0, "taking most of a second a block is a person");
    }

    @Test
    @DisplayName("one block finished repeatedly is not insta mining")
    void repeatingOneBlockIsClean() {
        isolate("instamine");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 8; i++) {
            finishAt(base + i * 3L, 0, 64, 0);
        }
        assertFalse(flags("instamine") > 0,
                "the same block counted once is a client retrying, not insta mining");
    }

    @Test
    @DisplayName("soft blocks finished quickly are not insta mining")
    void softBlocksAreClean() {
        isolate("instamine");
        for (int dx = -3; dx <= 3; dx++) {
            world.put(new BlockPos(dx, 64, 1).pack(),
                    new PlayerWorldCache.CachedBlock(BlockKind.AIR, 0.1, "tall_grass"));
        }
        player.worldCache(PlayerWorldCache.of(
                new Vec3d(0.5, 65.0, 0.5), world, 0.6, 15, true, true));
        long base = System.currentTimeMillis() + 60_000L;
        for (int dx = -3; dx <= 3; dx++) {
            finishAt(base, dx, 64, 1);
        }
        assertFalse(flags("instamine") > 0,
                "breaking grass takes no time at all, which is not suspicious");
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