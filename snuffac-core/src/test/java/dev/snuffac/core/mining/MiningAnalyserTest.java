package dev.snuffac.core.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.util.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MiningAnalyserTest {

    private static MiningEvidence evidence(boolean within, int tier, long millis, double gap) {
        return new MiningEvidence(0, new BlockPos(tier, tier, tier), millis, within, 4, tier,
                "ORE", gap);
    }

    @Test
    @DisplayName("targets outside the sent chunk radius are counted separately")
    void outsideCounted() {
        MiningAnalyser analyser = new MiningAnalyser(2);
        analyser.record(evidence(true, 3, 1000L, 0.0));
        analyser.record(evidence(false, 3, 2000L, 1.0));
        analyser.record(evidence(false, 3, 3000L, 1.0));
        assertEquals(2, analyser.outsideSentChunks());
        assertEquals(2, analyser.valuableOutsideSentChunks());
    }

    @Test
    @DisplayName("low tier ores outside the radius do not count as valuable")
    void lowTierExcluded() {
        MiningAnalyser analyser = new MiningAnalyser(2);
        analyser.record(evidence(false, 1, 1000L, 0.0));
        analyser.record(evidence(false, 1, 2000L, 1.0));
        assertEquals(2, analyser.outsideSentChunks());
        assertEquals(0, analyser.valuableOutsideSentChunks());
    }

    @Test
    @DisplayName("history is bounded so a long session cannot leak memory")
    void historyBounded() {
        MiningAnalyser analyser = new MiningAnalyser(2);
        for (int i = 0; i < MiningAnalyser.HISTORY_LIMIT * 3; i++) {
            analyser.record(evidence(true, 1, i, 0.0));
        }
        assertEquals(MiningAnalyser.HISTORY_LIMIT, analyser.size());
    }

    @Test
    @DisplayName("average ore tier and target cadence are reported")
    void averagesReported() {
        MiningAnalyser analyser = new MiningAnalyser(2);
        analyser.record(evidence(true, 3, 1000L, 0.0));
        analyser.record(evidence(true, 1, 3000L, 2.0));
        assertEquals(2.0, analyser.averageOreTier(), 1.0E-9);
        assertEquals(2.0, analyser.averageSecondsBetweenTargets(), 1.0E-9);
    }

    @Test
    @DisplayName("clearing removes all history")
    void clearResets() {
        MiningAnalyser analyser = new MiningAnalyser(2);
        analyser.record(evidence(false, 3, 1000L, 0.0));
        analyser.clear();
        assertEquals(0, analyser.size());
        assertEquals(0, analyser.outsideSentChunks());
        assertTrue(analyser.targetsOutsideSentChunks().isEmpty());
    }
}
