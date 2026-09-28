package dev.snuffac.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.util.BlockPos;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlockObfuscatorTest {

    private static BlockObfuscator obfuscator(ObfuscationPolicy policy) {
        return new BlockObfuscator(policy, -16, 320, false);
    }

    @Test
    @DisplayName("a disabled obfuscator never hides anything")
    void disabledIsInert() {
        BlockObfuscator obfuscator = obfuscator(ObfuscationPolicy.OFF);
        assertFalse(obfuscator.active());
        assertFalse(obfuscator.shouldObfuscate(new BlockPos(0, 12, 0), "DIAMOND_ORE"));
    }

    @Test
    @DisplayName("valuable ores inside the hidden band are obfuscated")
    void oresInBandHidden() {
        BlockObfuscator obfuscator = obfuscator(ObfuscationPolicy.HIDDEN_ORES);
        assertTrue(obfuscator.shouldObfuscate(new BlockPos(10, 12, 10), "DIAMOND_ORE"));
        assertTrue(obfuscator.shouldObfuscate(new BlockPos(10, -14, 10), "GOLD_ORE"));
        assertTrue(obfuscator.shouldObfuscate(new BlockPos(10, 100, 10), "ANCIENT_DEBRIS"));
    }

    @Test
    @DisplayName("ores outside the hidden band are sent to the client")
    void oresOutsideBandVisible() {
        BlockObfuscator obfuscator = obfuscator(ObfuscationPolicy.HIDDEN_ORES);
        assertFalse(obfuscator.shouldObfuscate(new BlockPos(10, 400, 10), "DIAMOND_ORE"));
        assertFalse(obfuscator.shouldObfuscate(new BlockPos(10, -60, 10), "DIAMOND_ORE"));
    }

    @Test
    @DisplayName("stone is never obfuscated even inside the band")
    void stoneNeverHidden() {
        BlockObfuscator obfuscator = obfuscator(ObfuscationPolicy.HIDDEN_ORES_AND_DEEPSLATE);
        assertFalse(obfuscator.shouldObfuscate(new BlockPos(10, 12, 10), "STONE"));
        assertFalse(obfuscator.shouldObfuscate(new BlockPos(10, 12, 10), "DIRT"));
    }

    @Test
    @DisplayName("deepslate is only hidden under the stricter policy")
    void deepslatePolicyDependent() {
        BlockPos position = new BlockPos(10, 12, 10);
        assertFalse(obfuscator(ObfuscationPolicy.HIDDEN_ORES).shouldObfuscate(position, "DEEPSLATE"));
        assertTrue(obfuscator(ObfuscationPolicy.HIDDEN_ORES_AND_DEEPSLATE)
                .shouldObfuscate(position, "DEEPSLATE"));
    }

    @Test
    @DisplayName("containers are only hidden when that is explicitly enabled")
    void containersOptional() {
        BlockPos position = new BlockPos(0, 40, 0);
        assertFalse(new BlockObfuscator(ObfuscationPolicy.HIDDEN_ORES, -16, 320, false)
                .shouldHideContainer(position, "CHEST"));
        assertTrue(new BlockObfuscator(ObfuscationPolicy.HIDDEN_ORES, -16, 320, true)
                .shouldHideContainer(position, "CHEST"));
    }

    @Test
    @DisplayName("section processing reports which positions were obfuscated")
    void sectionProcessing() {
        BlockObfuscator obfuscator = obfuscator(ObfuscationPolicy.HIDDEN_ORES);
        Map<BlockPos, String> world = Map.of(
                new BlockPos(0, 12, 0), "DIAMOND_ORE",
                new BlockPos(1, 12, 0), "STONE",
                new BlockPos(2, 400, 0), "GOLD_ORE");
        BlockObfuscator.SectionResult result = obfuscator.process(
                List.of(new BlockPos(0, 12, 0), new BlockPos(1, 12, 0), new BlockPos(2, 400, 0)),
                (position, ignored) -> world.get(position));
        assertEquals(3, result.inspected());
        assertEquals(1, result.obfuscated().size());
        assertEquals(new BlockPos(0, 12, 0), result.obfuscated().get(0));
        assertTrue(obfuscator.obfuscatedCount() >= 1);
    }

    @Test
    @DisplayName("ore tiers rank the ores that matter most")
    void oreTiers() {
        assertEquals(3, OreClassifier.oreTier("DEEPSLATE_DIAMOND_ORE"));
        assertEquals(3, OreClassifier.oreTier("EMERALD_ORE"));
        assertEquals(2, OreClassifier.oreTier("ANCIENT_DEBRIS"));
        assertEquals(1, OreClassifier.oreTier("IRON_ORE"));
        assertEquals(0, OreClassifier.oreTier("STONE"));
    }
}
