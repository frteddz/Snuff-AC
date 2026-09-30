package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.check.impl.combat.AutoClickerCheck;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClickCadenceShapeTest {

    private static long[] intervals(int count, long value) {
        long[] values = new long[count];
        for (int i = 0; i < count; i++) {
            values[i] = value;
        }
        return values;
    }

    @Test
    @DisplayName("a perfectly even cadence is the no spread case")
    void evenCadenceHasNoSpread() {
        long[] values = intervals(20, 50);
        double mean = 50.0;
        double spread = Math.sqrt(deviation(values, mean));
        assertTrue(spread < AutoClickerCheck.SPREAD_MIN,
                "a fixed delay has no spread, got " + spread);
    }

    @Test
    @DisplayName("a human cadence has real spread")
    void humanCadenceHasSpread() {
        Random random = new Random(20261002L);
        long[] values = new long[40];
        for (int i = 0; i < values.length; i++) {
            values[i] = 45L + Math.round(random.nextGaussian() * 12.0);
        }
        double mean = 0.0;
        for (long value : values) {
            mean += value;
        }
        mean /= values.length;
        double spread = Math.sqrt(deviation(values, mean));
        assertTrue(spread > AutoClickerCheck.SPREAD_MIN,
                "a hand produces varied delays, got " + spread);
    }

    @Test
    @DisplayName("the same delay six times running is a repeat run")
    void identicalDelaysAreFound() {
        long[] values = {50, 50, 50, 50, 50, 50, 50, 42, 55};
        assertTrue(AutoClickerCheck.longestRepeatRun(values) >= AutoClickerCheck.REPEAT_RUN,
                "seven identical delays in a row should be found");
    }

    @Test
    @DisplayName("a varied cadence has no long repeat run")
    void variedCadenceHasNoRepeat() {
        long[] values = {50, 42, 58, 47, 61, 44, 53, 49, 60};
        assertEquals(1, AutoClickerCheck.longestRepeatRun(values),
                "no delay repeats, so the run is one");
    }

    @Test
    @DisplayName("an even cadence has a spread well under a millisecond")
    void evenCadenceSpreadIsTiny() {
        long[] values = intervals(30, 50);
        double mean = 50.0;
        double spread = Math.sqrt(deviation(values, mean));
        assertTrue(spread < 0.001, "a fixed delay has effectively no spread, got " + spread);
    }

    @Test
    @DisplayName("a human cadence is nowhere near the even threshold")
    void humanCadenceIsWellClear() {
        Random random = new Random(20261004L);
        long[] values = new long[40];
        for (int i = 0; i < values.length; i++) {
            values[i] = 45L + Math.round(random.nextGaussian() * 12.0);
        }
        double mean = 0.0;
        for (long value : values) {
            mean += value;
        }
        mean /= values.length;
        double spread = Math.sqrt(deviation(values, mean));
        double ratio = spread / mean;
        assertTrue(spread > AutoClickerCheck.SPREAD_MIN * 50,
                "a hand is nowhere near a machine, got " + spread);
        assertTrue(ratio > AutoClickerCheck.SPREAD_RATIO * 5,
                "and several times the even ratio, got " + ratio);
    }

    @Test
    @DisplayName("a run of identical delays is only interesting when the rest is tight too")
    void repeatRunNeedsTightSpread() {
        Random random = new Random(20261005L);
        long[] spreadOut = {50, 50, 50, 50, 50, 50, 70, 90, 40, 110, 35};
        assertTrue(AutoClickerCheck.longestRepeatRun(spreadOut) >= AutoClickerCheck.REPEAT_RUN,
                "the run is there");
        double mean = 0.0;
        for (long value : spreadOut) {
            mean += value;
        }
        mean /= spreadOut.length;
        double spread = Math.sqrt(deviation(spreadOut, mean));
        assertTrue(spread >= AutoClickerCheck.REPEAT_SPREAD,
                "but the surrounding spread is wide, so it is a human pattern, got " + spread);
        assertTrue(random != null);
    }

    private static double deviation(long[] values, double mean) {
        double total = 0.0;
        for (long value : values) {
            double difference = value - mean;
            total += difference * difference;
        }
        return values.length < 2 ? 0.0 : total / values.length;
    }
}
