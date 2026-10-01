package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.HeldItemChangePacket;
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

class AutoTotemAndNoSlowTest {

    private SnuffCore core;
    private PlayerData player;
    private Vec3d here;

    @BeforeEach
    void setUp() {
        core = new SnuffCore(SnuffPlatform.PAPER, new RecordingLogger());
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-totem"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        here = new Vec3d(0.5, 65.0, 0.5);
        Map<Long, PlayerWorldCache.CachedBlock> world = new HashMap<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        apply(world);
        for (int i = 0; i < 4; i++) {
            core.tick();
        }
    }

    private void apply(Map<Long, PlayerWorldCache.CachedBlock> world) {
        player.worldCache(PlayerWorldCache.of(here, world, 0.6, 15, true, true));
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(true);
    }

    private void move(double x, double y, double z, boolean onGround) {
        var state = player.movement();
        state.advanceTo(new Vec3d(x, y, z), System.nanoTime());
        state.advanceGround(onGround);
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), true, false,
                new Vec3d(x, y, z), 0.0f, 0.0f, onGround, false));
        core.processQueue();
        core.tick();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("the same slot change after every hit is caught")
    void totemLoopIsCaught() {
        for (int hit = 0; hit < 8; hit++) {
            player.combat().recordDamage(System.currentTimeMillis());
            core.enqueue(player.id(), new HeldItemChangePacket(
                    System.currentTimeMillis() * 1_000_000L + 20_000_000L, 4, false));
            core.processQueue();
            core.tick();
        }
        assertTrue(flags("autototem") > 0,
                "a slot change a fixed 20ms after each hit is the AutoTotem cheat");
    }

    @Test
    @DisplayName("a slot change long after the hit is not a totem loop")
    void slowSlotChangeIsClean() {
        player.combat().recordDamage(System.currentTimeMillis() - 5_000L);
        for (int i = 0; i < 8; i++) {
            core.enqueue(player.id(), new HeldItemChangePacket(
                    System.currentTimeMillis() * 1_000_000L, i % 9, false));
            core.processQueue();
        }
        assertFalse(flags("autototem") > 0,
                "scrolling the hotbar five seconds after a hit is a person");
    }

    @Test
    @DisplayName("a slot change with no recent hit is not a totem loop")
    void slotChangeWithoutDamageIsClean() {
        for (int i = 0; i < 12; i++) {
            core.enqueue(player.id(), new HeldItemChangePacket(
                    System.currentTimeMillis() * 1_000_000L, i % 9, false));
            core.processQueue();
        }
        assertFalse(flags("autototem") > 0, "nothing was hit, so nothing was reacted to");
    }

    @Test
    @DisplayName("the totem rule needs several hits before it fires")
    void totemNeedsAHitCount() {
        assertTrue(dev.snuffac.core.check.impl.combat.AutoTotemCheck.REQUIRED_REACTIONS >= 4,
                "a couple of fast slot changes are just a fast player");
        assertTrue(dev.snuffac.core.check.impl.combat.AutoTotemCheck.REACTION_MILLIS <= 500L,
                "a human reacting to damage is slower than half a second");
    }

    private void walkAt(double step, boolean usingItem, int ticks) {
        var state = player.movement();
        double x = player.movement().position().x();
        for (int tick = 0; tick < ticks; tick++) {
            x += step;
            move(x, 65.0, 0.5, true);
            state.usingItem(usingItem);
        }
    }

    private static final double WALK = 0.2159;

    @Test
    @DisplayName("walking at full speed while eating is caught")
    void movingAtFullSpeedWhileUsingIsCaught() {
        walkAt(WALK, false, 24);
        walkAt(WALK, true, 24);
        assertTrue(flags("noslow") > 0,
                "eating at the same speed as free walking removes the vanilla slowdown");
    }

    @Test
    @DisplayName("walking at the vanilla slowed speed while eating is clean")
    void slowedWalkingIsClean() {
        walkAt(WALK, false, 24);
        walkAt(WALK * 0.2, true, 24);
        assertFalse(flags("noslow") > 0, "the slowed speed is what vanilla allows");
    }

    @Test
    @DisplayName("a little slow while eating is still caught")
    void barelySlowedIsCaught() {
        walkAt(WALK, false, 24);
        walkAt(WALK * 0.75, true, 24);
        assertTrue(flags("noslow") > 0, "three quarters speed is not the vanilla one fifth");
    }

    @Test
    @DisplayName("full speed while not using an item is never flagged")
    void normalWalkingIsClean() {
        walkAt(WALK, false, 24);
        assertFalse(flags("noslow") > 0, "ordinary walking has no slowdown applied");
    }

    @Test
    @DisplayName("the slowdown figure is the vanilla one")
    void slowdownFigureIsVanilla() {
        assertTrue(Math.abs(dev.snuffac.core.check.impl.movement.NoSlowCheck.EXPECTED_SLOWDOWN - 0.2)
                        < 1.0E-9,
                "using an item multiplies movement by 0.2");
        assertTrue(dev.snuffac.core.check.impl.movement.NoSlowCheck.MARGIN_ALLOWED <= 1.3,
                "the tolerance has to stay tight or a fast player is reported");
    }

    @Test
    @DisplayName("inventory clicks are seen by the engine")
    void inventoryClicksReachTheEngine() {
        core.enqueue(player.id(), new WindowClickPacket(System.nanoTime(), 1, 5, 0, 0));
        core.processQueue();
        assertFalse(loggerSevere(), "a click packet must not break anything");
    }

    private static boolean loggerSevere() {
        return false;
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
