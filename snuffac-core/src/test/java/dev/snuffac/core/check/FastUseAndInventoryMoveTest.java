package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.WindowClickPacket;
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

class FastUseAndInventoryMoveTest {

    private SnuffCore core;
    private PlayerData player;
    private Map<Long, PlayerWorldCache.CachedBlock> world;
    private double x;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-use"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        world = new HashMap<>();
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        x = 0.5;
        step(0.0, true);
        for (int i = 0; i < 4; i++) {
            core.tick();
        }
    }

    private void step(double deltaX, boolean onGround) {
        x += deltaX;
        var position = new Vec3d(x, 65.0, 0.5);
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

    private void use() {
        core.enqueue(player.id(), new BlockPlacePacket(System.nanoTime(),
                new BlockPos(0, 64, 0).pack(), 1, 0.5f, 1.0f, 0.5f, 0, 0));
        core.processQueue();
    }

    private void click(int window) {
        core.enqueue(player.id(), new WindowClickPacket(System.nanoTime(), window, 5, 0, 0));
        core.processQueue();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("restarting an item use before the vanilla duration is caught")
    void earlyUseRestartIsCaught() {
        isolate("fastuse");
        player.movement().usingItem(true);
        for (int round = 0; round < 6; round++) {
            for (int tick = 0; tick < 5; tick++) {
                step(0.0, true);
                player.movement().usingItem(true);
            }
            use();
        }
        assertTrue(flags("fastuse") > 0,
                "restarting a 32 tick food use every 5 ticks completes it far too fast");
    }

    @Test
    @DisplayName("a use that runs its full duration is clean")
    void fullDurationUseIsClean() {
        isolate("fastuse");
        player.movement().usingItem(true);
        for (int tick = 0; tick < 80; tick++) {
            step(0.0, true);
            player.movement().usingItem(true);
        }
        assertFalse(flags("fastuse") > 0, "letting food finish is not cheating");
    }

    @Test
    @DisplayName("the vanilla food duration is the documented figure")
    void durationIsVanilla() {
        assertTrue(dev.snuffac.core.check.impl.combat.FastUseCheck.VANILLA_FOOD_TICKS == 32,
                "food and drink take 32 ticks");
        assertTrue(dev.snuffac.core.check.impl.combat.FastUseCheck.REQUIRED_RESTARTS >= 3,
                "one early restart is a misclick");
    }

    @Test
    @DisplayName("moving at full speed with an inventory open is caught")
    void movingWithInventoryOpenIsCaught() {
        isolate("inventorymove");
        click(9);
        for (int tick = 0; tick < 14; tick++) {
            step(0.28, true);
        }
        assertTrue(flags("inventorymove") > 0,
                "vanilla does not let a player move at walking speed with a container open");
    }

    @Test
    @DisplayName("standing still with an inventory open is clean")
    void standingStillWithInventoryOpenIsClean() {
        isolate("inventorymove");
        click(9);
        for (int tick = 0; tick < 30; tick++) {
            step(0.0, true);
        }
        assertFalse(flags("inventorymove") > 0, "looking through a chest while standing is normal");
    }

    @Test
    @DisplayName("moving at full speed with no inventory open is clean")
    void movingWithNoInventoryIsClean() {
        isolate("inventorymove");
        for (int tick = 0; tick < 30; tick++) {
            step(0.28, true);
        }
        assertFalse(flags("inventorymove") > 0, "ordinary walking needs no inventory open");
    }

    @Test
    @DisplayName("the player inventory itself does not count as a container")
    void playerInventoryIsNotAContainer() {
        isolate("inventorymove");
        click(0);
        for (int tick = 0; tick < 30; tick++) {
            step(0.28, true);
        }
        assertFalse(flags("inventorymove") > 0,
                "moving while rearranging your own hotbar is something the game allows");
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
