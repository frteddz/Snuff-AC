package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.player.PlayerWorldCache;
import dev.snuffac.core.tolerance.ToleranceModel;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EngineIntegrationTest {

    private SnuffCore core;
    private RecordingLogger logger;
    private PlayerData player;

    @BeforeEach
    void setUp() {
        logger = new RecordingLogger();
        core = new SnuffCore(SnuffPlatform.PAPER, logger);
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new java.util.HashMap<>()),
                java.nio.file.Path.of("build", "test-logs"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis());
        player.worldCache(PlayerWorldCache.of(
                player.position(),
                Map.of(),
                0.6, 15, true, true));
    }

    @Test
    @DisplayName("a player is registered with a check state for every check")
    void playerHasStateForEveryCheck() {
        for (String key : core.checkKeys()) {
            assertNotNull(player.checkState(key), "missing state for " + key);
        }
    }

    @Test
    @DisplayName("every check creates the per player data it asked for")
    void perPlayerCheckDataIsCreated() {
        for (var check : core.registry().all()) {
            if (check.createState() == null) {
                continue;
            }
            assertNotNull(player.checkData(check.key()), "missing per player data for " + check.key());
        }
    }

    @Test
    @DisplayName("a movement packet flows through the engine without error")
    void movementPacketIsProcessed() {
        MovementPacket packet = new MovementPacket(
                System.nanoTime(), true, true, new Vec3d(1.0, 0.0, 1.0), 90.0f, 0.0f, true, false);
        core.enqueue(player.id(), packet);
        drain();
        assertTrue(logger.warnings().isEmpty(),
                "processing a normal movement packet should not log warnings: " + logger.warnings());
    }

    @Test
    @DisplayName("an impossible coordinate produces an immediate violation")
    void impossiblePositionFlags() {
        int before = countViolations();
        MovementPacket packet = new MovementPacket(
                System.nanoTime(), true, false, new Vec3d(Double.NaN, 0.0, 0.0), 0.0f, 0.0f, true, false);
        core.enqueue(player.id(), packet);
        drain();
        assertTrue(countViolations() > before,
                "a non finite coordinate should be reported as a violation");
    }

    @Test
    @DisplayName("an attack with a cursor outside the world is flagged")
    void farAttackIsFlagged() {
        int before = countViolations();
        AttackPacket attack = new AttackPacket(
                System.nanoTime(), 42, new Vec3d(5.0E6, 0.0, 5.0E6), false, 0.0f, 0.0f);
        for (int i = 0; i < 6; i++) {
            core.enqueue(player.id(), attack);
            drain();
        }
        assertTrue(countViolations() > before, "an out of range attack should be flagged");
    }

    @Test
    @DisplayName("ticking the engine decays buffers and stays stable over many ticks")
    void ticksRemainStable() {
        for (int i = 0; i < 500; i++) {
            core.tick();
        }
        assertTrue(logger.severeMessages().isEmpty(), "no severe errors during ticking");
        assertEquals(500L, core.tickCounter());
        assertNotNull(player.checkState("fly").buffer());
    }

    @Test
    @DisplayName("removing a player clears them from the registry")
    void removalClearsPlayer() {
        UUID id = player.id();
        core.removePlayer(id);
        assertEquals(0, core.player(id) == null ? 0 : 1);
    }

    @Test
    @DisplayName("an exempt player is never processed")
    void exemptPlayersAreIgnored() {
        player.exempt(true);
        int before = countViolations();
        AttackPacket attack = new AttackPacket(
                System.nanoTime(), 42, new Vec3d(5.0E6, 0.0, 5.0E6), false, 0.0f, 0.0f);
        for (int i = 0; i < 6; i++) {
            core.enqueue(player.id(), attack);
            drain();
        }
        assertEquals(before, countViolations(), "an exempt player must not accrue violations");
    }

    @Test
    @DisplayName("a disabled check never fires while other checks still report the same packet")
    void disabledChecksAreSkipped() {
        var config = core.registry().config("reach");
        config.enabled(false);
        int before = countViolations();
        AttackPacket attack = new AttackPacket(
                System.nanoTime(), 42, new Vec3d(5.0E6, 0.0, 5.0E6), false, 0.0f, 0.0f);
        for (int i = 0; i < 8; i++) {
            core.enqueue(player.id(), attack);
            drain();
        }
        boolean reachFired = player.history().stream()
                .anyMatch(record -> "reach".equals(record.checkKey()));
        assertFalse(reachFired, "a disabled check must not report violations");
        assertTrue(countViolations() > before,
                "other enabled checks should still be able to report the same packet");
        config.enabled(true);
    }

    @Test
    @DisplayName("check categories cover movement combat world and packet")
    void categoriesAreCovered() {
        for (CheckCategory category : CheckCategory.values()) {
            assertFalse(core.registry().byCategory(category).isEmpty(),
                    "expected at least one check in " + category);
        }
    }

    @Test
    @DisplayName("digging the same block instantly is flagged as fast breaking")
    void instantDigIsFlagged() {
        player.worldCache(PlayerWorldCache.of(
                player.position(),
                Map.of(new BlockPos(0, 0, 0).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 5.0)),
                0.6, 15, true, true));
        int before = countViolations();
        long packed = new BlockPos(0, 0, 0).pack();
        for (int attempt = 0; attempt < 4; attempt++) {
            core.enqueue(player.id(), new BlockBreakPacket(
                    System.nanoTime(), BlockBreakPacket.BlockBreakAction.START, packed, 0, 0));
            drain();
            core.enqueue(player.id(), new BlockBreakPacket(
                    System.nanoTime(), BlockBreakPacket.BlockBreakAction.FINISH, packed, 0, 0));
            drain();
        }
        assertTrue(countViolations() > before, "repeated instant breaks of a hard block should be flagged");
    }

    @Test
    @DisplayName("alerts format into the configured template")
    void alertsFormat() {
        core.alerts().clearCooldowns();
        var formatter = new dev.snuffac.core.alert.AlertFormatter(
                "Snuff", "[{prefix}] {player} failed {check} | VL: {vl} | ping: {ping}ms", null);
        var info = new dev.snuffac.api.violation.ViolationInfo(
                player.id(), "Tester", CheckCategory.MOVEMENT, "fly", "Fly",
                "test", 4.7, 12.0, 3.0, 42.0, 20.0, System.currentTimeMillis(), SnuffPlatform.PAPER);
        String rendered = formatter.format(info, Map.of("airTicks", 100));
        assertTrue(rendered.contains("Tester"), rendered);
        assertTrue(rendered.contains("Fly"), rendered);
        assertTrue(rendered.contains("4.7"), rendered);
        assertTrue(rendered.contains("42.0ms") || rendered.contains("42ms"), rendered);
    }

    private void drain() {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (core.processQueue() == 0) {
                return;
            }
        }
    }

    private int countViolations() {
        return player.history().size();
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
