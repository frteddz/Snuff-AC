package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.SnuffPlatform;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.SnuffCore;
import dev.snuffac.core.config.ConfigSource;
import dev.snuffac.core.log.RecordingLogger;
import dev.snuffac.core.packet.ArmAnimationPacket;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.WindowClickPacket;
import dev.snuffac.core.player.EquipmentState.HeldKind;
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

class AutoConsumableTest {

    private SnuffCore core;
    private RecordingLogger logger;
    private PlayerData player;

    @BeforeEach
    void setUp() {
        logger = new RecordingLogger();
        core = new SnuffCore(SnuffPlatform.PAPER, logger);
        core.boot(new NoopMessenger(), new NoopPermissions(),
                ConfigSource.ofMap(new HashMap<>()),
                java.nio.file.Path.of("build", "test-logs-consumable"));
        player = core.addPlayer(UUID.randomUUID(), "Tester", System.currentTimeMillis() - 60_000L);
        Map<Long, PlayerWorldCache.CachedBlock> world = new HashMap<>();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                world.put(new BlockPos(dx, 64, dz).pack(),
                        new PlayerWorldCache.CachedBlock(BlockKind.SOLID, 1.0, "stone"));
            }
        }
        var here = new Vec3d(0.5, 65.0, 0.5);
        player.worldCache(PlayerWorldCache.of(here, world, 0.6, 15, true, true));
        player.movement().advanceTo(here, System.nanoTime());
        player.movement().advanceGround(true);
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

    private void hold(HeldKind kind) {
        player.equipment().update(1.0, 1.0, true, 0, false, false, kind);
    }

    private void swing(long millis) {
        core.enqueue(player.id(), new ArmAnimationPacket(millis * 1_000_000L, 1));
        core.processQueue();
    }

    private void eat(long millis) {
        core.enqueue(player.id(), new BlockPlacePacket(millis * 1_000_000L,
                new BlockPos(0, 64, 0).pack(), 1, 0.5f, 1.0f, 0.5f, 0, 0));
        core.processQueue();
    }

    private void click(long millis, int slot) {
        core.enqueue(player.id(), new WindowClickPacket(millis * 1_000_000L, 0, slot, 0, 0));
        core.processQueue();
    }

    private void tick() {
        core.enqueue(player.id(), new MovementPacket(System.nanoTime(), false, false,
                new Vec3d(0.5, 65.0, 0.5), 0.0f, 0.0f, true, false));
        core.processQueue();
        core.tick();
    }

    private int flags(String key) {
        return (int) player.history().stream()
                .filter(record -> key.equals(record.checkKey()))
                .count();
    }

    @Test
    @DisplayName("potions thrown on a fixed interval are caught")
    void fixedPotionCadenceIsCaught() {
        isolate("autopot");
        hold(HeldKind.POTION);
        long base = 2_000_000_000L;
        for (int i = 0; i < 8; i++) {
            swing(base + i * 100L);
        }
        assertTrue(flags("autopot") > 0, "a potion every 100ms with no spread is a machine");
    }

    @Test
    @DisplayName("potions thrown by hand are clean")
    void humanPotionCadenceIsClean() {
        isolate("autopot");
        hold(HeldKind.POTION);
        long[] times = {0L, 340L, 520L, 1180L, 1490L, 2100L, 3600L, 4100L};
        for (long time : times) {
            swing(2_000_000_000L + time);
        }
        assertFalse(flags("autopot") > 0, "varied potion timing is a person");
    }

    @Test
    @DisplayName("a swing with no potion in hand is never autopot")
    void swingWithoutPotionIsClean() {
        isolate("autopot");
        hold(HeldKind.OTHER);
        long base = 2_000_000_000L;
        for (int i = 0; i < 10; i++) {
            swing(base + i * 100L);
        }
        assertFalse(flags("autopot") > 0, "swinging a sword is not throwing potions");
    }

    @Test
    @DisplayName("food eaten on a fixed interval is caught")
    void fixedFoodCadenceIsCaught() {
        isolate("autosoup");
        hold(HeldKind.FOOD);
        long base = 2_000_000_000L;
        for (int i = 0; i < 8; i++) {
            eat(base + i * 150L);
        }
        assertTrue(flags("autosoup") > 0, "eating every 150ms is not how soup works");
    }

    @Test
    @DisplayName("food eaten at a human pace is clean")
    void humanFoodCadenceIsClean() {
        isolate("autosoup");
        hold(HeldKind.FOOD);
        long[] times = {0L, 800L, 1900L, 2600L, 4100L, 5200L, 6300L};
        for (long time : times) {
            eat(2_000_000_000L + time);
        }
        assertFalse(flags("autosoup") > 0, "eating a mushroom stew every second is normal");
    }

    @Test
    @DisplayName("armour equipped on a fixed delay after damage is caught")
    void fixedArmorReactionIsCaught() {
        isolate("autoarmor");
        long base = System.currentTimeMillis() + 60_000L;
        for (int hit = 0; hit < 8; hit++) {
            player.combat().recordDamage(base + hit * 1000L);
            click(base + hit * 1000L + 20L, 6);
        }
        assertTrue(flags("autoarmor") > 0,
                "armour swapped in 20ms after every hit is the AutoArmor cheat");
    }

    @Test
    @DisplayName("armour equipped by hand after a hit is clean")
    void manualArmorSwapIsClean() {
        isolate("autoarmor");
        long base = System.currentTimeMillis() + 60_000L;
        long[] delays = {900L, 1500L, 2400L, 3100L, 4800L};
        for (int i = 0; i < delays.length; i++) {
            long hitAt = base + i * 5000L;
            player.combat().recordDamage(hitAt);
            click(hitAt + delays[i], 6);
        }
        assertFalse(flags("autoarmor") > 0, "a player swapping armour a second later is fine");
    }

    @Test
    @DisplayName("clicking a non armour slot is never autoarmor")
    void nonArmorSlotIsClean() {
        isolate("autoarmor");
        long base = System.currentTimeMillis() + 60_000L;
        for (int i = 0; i < 10; i++) {
            player.combat().recordDamage(base + i * 500L);
            click(base + i * 500L + 20L, 30);
        }
        assertFalse(flags("autoarmor") > 0, "slot 30 is the hotbar, not the armour row");
    }

    @Test
    @DisplayName("the armour slots are the four pieces of the armour row")
    void armorSlotsAreCorrect() {
        for (int slot = 5; slot <= 8; slot++) {
            assertTrue(dev.snuffac.core.check.impl.combat.AutoArmorCheck.isArmorSlot(slot),
                    "slot " + slot + " is armour");
        }
        assertFalse(dev.snuffac.core.check.impl.combat.AutoArmorCheck.isArmorSlot(4));
        assertFalse(dev.snuffac.core.check.impl.combat.AutoArmorCheck.isArmorSlot(9));
    }

    @Test
    @DisplayName("the tick keeps the engine stable through all of this")
    void engineStaysStable() {
        isolate("autopot");
        hold(HeldKind.POTION);
        for (int i = 0; i < 30; i++) {
            swing(2_000_000_000L + i * 100L);
            tick();
        }
        assertTrue(player.checkState("autopot") != null, "the check keeps its state");
        assertTrue(logger.severeMessages().isEmpty(), "no severe errors during the run");
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
