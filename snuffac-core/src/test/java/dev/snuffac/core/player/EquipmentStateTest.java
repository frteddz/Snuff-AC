package dev.snuffac.core.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EquipmentStateTest {

    @Test
    @DisplayName("break time follows the vanilla hardness and tool speed formula")
    void breakTimeMatchesVanilla() {
        double ticks = EquipmentState.breakTicks(1.5, 8.0, 1.0);
        assertEquals(5.625, ticks, 1.0E-6,
                "stone at mining speed 8 should take about 5.6 ticks to break");
        assertEquals(281.25, EquipmentState.breakMillis(1.5, 8.0, 1.0), 1.0E-6,
                "5.625 ticks should be 281.25 milliseconds");
    }

    @Test
    @DisplayName("bare hands are slower than a diamond pickaxe")
    void bareHandsAreSlower() {
        double bare = EquipmentState.breakMillis(1.5, 1.0, 1.0);
        double diamond = EquipmentState.breakMillis(1.5, 8.0, 1.0);
        assertTrue(bare > diamond * 4.0, "bare hands should be several times slower than a pickaxe");
    }

    @Test
    @DisplayName("higher hardness takes longer to break")
    void hardnessIncreasesTime() {
        double soft = EquipmentState.breakMillis(0.5, 4.0, 1.0);
        double hard = EquipmentState.breakMillis(5.0, 4.0, 1.0);
        assertTrue(hard > soft * 9.0, "obsidian should take far longer than wool");
    }

    @Test
    @DisplayName("instakill blocks break instantly")
    void instakillBlocksAreInstant() {
        assertEquals(0.0, EquipmentState.breakMillis(0.0, 1.0, 1.0), 1.0E-9);
    }

    @Test
    @DisplayName("a zero mining speed never divides by zero")
    void guardsAgainstZeroSpeed() {
        double value = EquipmentState.breakMillis(1.0, 0.0, 0.0);
        assertTrue(Double.isFinite(value) && value > 0.0);
    }

    @Test
    @DisplayName("update stores the values the platform provides")
    void updateStoresValues() {
        EquipmentState equipment = new EquipmentState();
        equipment.update(6.0, 1.0, true, 3, true);
        assertEquals(6.0, equipment.miningSpeed(), 1.0E-9);
        assertTrue(equipment.validTool());
        assertEquals(3, equipment.heldItemSlot());
        assertTrue(equipment.holdingPlaceable());
        equipment.reset();
        assertEquals(1.0, equipment.miningSpeed(), 1.0E-9);
        assertFalse(equipment.validTool());
    }

    @Test
    @DisplayName("world cache reports block kinds and hardness from the filled snapshot")
    void worldCacheReportsBlocks() {
        PlayerWorldCache cache = PlayerWorldCache.of(
                Vec3d.ZERO,
                java.util.Map.of(
                        new dev.snuffac.core.util.BlockPos(0, 0, 0).pack(),
                        new PlayerWorldCache.CachedBlock(dev.snuffac.core.util.BlockKind.ICE, 0.5)),
                0.98, 15, true, true);
        var position = new dev.snuffac.core.util.BlockPos(0, 0, 0);
        assertEquals(dev.snuffac.core.util.BlockKind.ICE, cache.kindAt(position));
        assertTrue(cache.isIce(position));
        assertEquals(0.5, cache.hardnessAt(position), 1.0E-9);
        assertTrue(cache.chunkLoaded());
        assertTrue(cache.onGroundBelow());
        assertFalse(cache.isPassable(position));
        assertEquals(dev.snuffac.core.util.BlockKind.UNKNOWN,
                cache.kindAt(new dev.snuffac.core.util.BlockPos(5, 5, 5)));
    }
}
