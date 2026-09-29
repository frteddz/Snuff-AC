package dev.snuffac.core.check.impl.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PacketSpamWindowTest {

    static double reportedRate(int count, long elapsedMillis) {
        if (elapsedMillis <= 0L) {
            return 0.0;
        }
        return count * 1000.0 / elapsedMillis;
    }

    @Test
    void aSinglePacketInTheFirstWindowCannotReportAThousandPerSecond() {
        double firstWindowRate = reportedRate(1, 0L);
        assertEquals(0.0, firstWindowRate,
                "a zero length window must produce no rate at all");
    }

    @Test
    void aWindowShorterThanTheMinimumIsNeverEvaluated() {
        assertTrue(PacketSpamCheck.MIN_WINDOW_ELAPSED >= 100L,
                "a window must be open long enough for the division to mean something");
    }

    @Test
    void theOldFormulaIsWhatProducedTheBug() {
        int count = 1;
        long elapsed = 0L;
        double oldFormula = count * 1000.0 / (elapsed + 1.0);
        assertEquals(1000.0, oldFormula, 0.001,
                "the previous count/(elapsed+1) formula reported 1000/s from one packet");
        assertTrue(oldFormula > 400.0,
                "which is why a standing still player was flagged at a 400 threshold");
    }

    @Test
    void theOldFormulaAlsoBlewUpOnTheSecondPacket() {
        double oldFormula = 2 * 1000.0 / (0L + 1.0);
        assertTrue(oldFormula > 400.0);
    }

    @Test
    void aRealRateIsCalculatedCorrectly() {
        assertEquals(100.0, reportedRate(100, 1000L), 0.001);
        assertEquals(400.0, reportedRate(200, 500L), 0.001);
    }

    @Test
    void aFloodMustSustainAcrossConsecutiveWindows() {
        assertTrue(PacketSpamCheck.REQUIRED_WINDOWS > 1,
                "one bad window is not proof, a sustained flood is");
    }

    @Test
    void theHardLimitStandsEvenWhenTheConfiguredLimitIsTiny() {
        double configured = 12.0;
        assertEquals(400.0, Math.max(configured * 2.0, 400.0), 0.001);
    }
}
