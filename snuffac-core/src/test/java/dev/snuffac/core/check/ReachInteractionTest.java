package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.check.impl.movement.FlyCheck;
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

class ReachInteractionTest {

    private SnuffCore core;
    private RecordingLogger logger;
    private PlayerData player;
    private Vec3d here;
    private Map<Long, PlayerWorldCache.CachedBlock> floor;

    @BeforeEach
    void setUp() {
        logger = new RecordingLogger();
        core = new SnuffCore(SnuffPlatform.PAPER, logger);
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-reach"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        here = new Vec3d(0.0, 65.0, 0.0);
        solidFloor();
        move(here, true);
        for (int i = 0; i < 4; i++) {
            core.tick();
        }
    }

    private void solidFloor() {
        Map<Long, PlayerWorldCache.CachedBlock> blocks = new HashMap<>();
        floor = blocks;
        for (int dx = -14; dx <= 14; dx++) {
            for (int dy = -1; dy <= 4; dy++) {
                for (int dz = -14; dz <= 14; dz++) {
                    blocks.put(new BlockPos(dx, 64 + dy, dz).pack(),
                            new PlayerWorldCache.CachedBlock(
                                    dy == 0 ? BlockKind.SOLID : BlockKind.AIR, 1.0, "stone"));
                }
            }
        }
        player.worldCache(PlayerWorldCache.of(here, blocks, 0.6, 15, true, true));
    }

    private void move(Vec3d target, boolean onGround) {
        var state = player.movement();
        state.advanceTo(target, System.nanoTime());
        state.advanceGround(onGround);
        state.horizontalCollision(false);
        player.worldCache(PlayerWorldCache.of(target, floor, 0.6, 15, true, onGround));
        send(new MovementPacket(System.nanoTime(), true, false, target, 0.0f, 0.0f, onGround, false));
    }

    private void send(SnuffPacket packet) {
        core.enqueue(player.id(), packet);
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (core.processQueue() == 0) {
                return;
            }
        }
    }

    private boolean flagged(String key) {
        return player.history().stream().anyMatch(record -> key.equals(record.checkKey()));
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    private void place(int x, int y, int z) {
        send(new BlockPlacePacket(System.nanoTime(),
                new BlockPos(x, y, z).pack(), 1, 0.5f, 0.5f, 0.5f, 0, 0));
    }

    @Test
    @DisplayName("using a block one step away is not flagged")
    void nearbyInteractionIsClean() {
        for (int i = 0; i < 6; i++) {
            place(1, 64, 0);
        }
        assertFalse(flagged("reach"), "a block inside the vanilla interaction range must be allowed");
    }

    @Test
    @DisplayName("using a block diagonally just over the eye is not flagged")
    void diagonalInteractionIsClean() {
        for (int i = 0; i < 6; i++) {
            place(3, 64, 3);
        }
        assertFalse(flagged("reach"), "the distance is measured to the nearest face, not the centre");
    }

    @Test
    @DisplayName("using a block at the vanilla survival limit is not flagged")
    void limitInteractionIsClean() {
        for (int i = 0; i < 6; i++) {
            place(4, 64, 0);
        }
        assertFalse(flagged("reach"), "the edge of the interaction range must be allowed");
    }

    @Test
    @DisplayName("using a block across the map is flagged")
    void distantInteractionIsFlagged() {
        for (int i = 0; i < 6; i++) {
            place(12, 64, 0);
        }
        assertTrue(flagged("reach"), "a block far outside the interaction range must be flagged");
    }

    @Test
    @DisplayName("a single stray distant interaction is forgiven")
    void oneStrayInteractionIsForgiven() {
        place(12, 64, 0);
        assertFalse(flagged("reach"), "one out of range interaction should be forgiven");
    }

    @Test
    @DisplayName("interacting with air is never a reach violation")
    void airInteractionIsIgnored() {
        for (int i = 0; i < 8; i++) {
            place(12, 68, 12);
        }
        assertFalse(flagged("reach"), "there is no block there to reach for");
    }

    @Test
    @DisplayName("the interaction report names the interaction mode in its evidence")
    void evidenceNamesTheMode() {
        for (int i = 0; i < 6; i++) {
            place(12, 64, 0);
        }
        var evidence = lastEvidence("reach");
        assertTrue(String.valueOf(evidence.get("mode")).contains("interaction"),
                "the evidence should say the interaction rule fired: " + evidence);
        assertEqualsMaterial(evidence);
    }

    private void assertEqualsMaterial(Map<String, Object> evidence) {
        assertTrue(String.valueOf(evidence.get("material")).contains("stone"),
                "the evidence should name the block that was reached for: " + evidence);
    }

    @Test
    @DisplayName("distant interactions are reported in pairs rather than flooding")
    void distantInteractionIsRateLimited() {
        for (int i = 0; i < 40; i++) {
            place(12, 64, 0);
        }
        assertTrue(flags("reach") > 0, "it should still be caught");
        assertTrue(flags("reach") <= 20,
                "one report per pair of packets should not flood the history, got " + flags("reach"));
    }

    @Test
    @DisplayName("hovering in the air for a long time runs the air budget out")
    void endlessAirIsFlagged() {
        double y = 65.0;
        for (int tick = 0; tick < 320; tick++) {
            y += 0.42;
            move(new Vec3d(0.0, y, 0.0), false);
            core.tick();
        }
        assertTrue(flagged("fly"), "hovering for 320 ticks should run the air budget out");
    }

    @Test
    @DisplayName("standing still on solid ground never runs the budget out")
    void groundRefillsTheBudget() {
        for (int cycle = 0; cycle < 8; cycle++) {
            for (int tick = 0; tick < 40; tick++) {
                move(here, true);
                core.tick();
            }
        }
        assertFalse(flagged("fly"), "standing still on solid ground must never be flagged");
    }

    @Test
    @DisplayName("falling from the build limit is not flight")
    void longFallIsClean() {
        double y = 320.0;
        for (int tick = 0; tick < 110; tick++) {
            move(new Vec3d(0.0, y, 0.0), false);
            core.tick();
            y -= 2.2;
        }
        assertFalse(flagged("fly"), "falling from the build limit is legal and must stay clean");
    }

    @Test
    @DisplayName("the air budget report names its mode and counts the unsupported ticks")
    void budgetEvidenceNamesTheMode() {
        double y = 65.0;
        for (int tick = 0; tick < 320; tick++) {
            y += 0.42;
            move(new Vec3d(0.0, y, 0.0), false);
            core.tick();
        }
        var budget = player.history().stream()
                .filter(record -> "fly".equals(record.checkKey()))
                .map(record -> record.evidence())
                .filter(evidence -> String.valueOf(evidence.get("mode")).contains("air budget"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("no air budget record"));
        assertTrue(budget.containsKey("unsupportedTicks"),
                "the evidence should count the unsupported ticks: " + budget);
        assertTrue(budget.containsKey("airTicks"),
                "the evidence should report the total air time: " + budget);
        assertTrue(budget.containsKey("height"),
                "the evidence should report the height the player was at: " + budget);
    }

    @Test
    @DisplayName("climbing support keeps the air budget topped up")
    void climbingKeepsTheBudget() {
        for (int cycle = 0; cycle < 6; cycle++) {
            for (int tick = 0; tick < 60; tick++) {
                player.movement().onClimbable(true);
                move(new Vec3d(0.0, 65.0 + tick, 0.0), false);
                core.tick();
            }
        }
        assertFalse(flagged("fly"), "a ladder is legitimate support and must not be flagged");
    }

    @Test
    @DisplayName("fractional upward noise while falling is not climbing")
    void fractionalRiseIsNotClimbing() {
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        state.velocity(new Vec3d(0.0, -0.0468, 0.0));
        assertFalse(FlyCheck.isRising(0.003, state),
                "a few thousandths upward while the velocity is downward is rounding noise");
    }

    @Test
    @DisplayName("a real upward move with upward velocity is climbing")
    void realRiseIsClimbing() {
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        state.velocity(new Vec3d(0.0, 0.42, 0.0));
        assertTrue(FlyCheck.isRising(0.42, state), "a jump launch is real upward movement");
    }

    @Test
    @DisplayName("a big position rise with downward velocity is not climbing")
    void bigRiseWithDownwardVelocityIsNotClimbing() {
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        state.velocity(new Vec3d(0.0, -0.62, 0.0));
        assertFalse(FlyCheck.isRising(0.5, state),
                "the server velocity is falling even if the position moved up that tick");
    }

    @Test
    @DisplayName("an upward velocity with no upward position change is not climbing")
    void upwardVelocityWithoutRiseIsNotClimbing() {
        var state = player.movement();
        state.advanceTo(here, System.nanoTime());
        state.advanceGround(false);
        state.velocity(new Vec3d(0.0, 0.42, 0.0));
        assertFalse(FlyCheck.isRising(0.01, state),
                "the position has to actually rise by a real amount");
    }

    private Map<String, Object> lastEvidence(String key) {
        return player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("no " + key + " record"))
                .evidence();
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
