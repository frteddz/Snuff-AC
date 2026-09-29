package dev.snuffac.core.check.impl.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GcdAnalysisTest {

    @Test
    void findsTheConstantWhenEveryDeltaIsAMultiple() {
        double constant = 0.1527;
        double[] deltas = new double[40];
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = constant * (1 + (i % 5));
        }
        assertEquals(constant, GcdAnalysis.estimateConstant(deltas, deltas.length), 0.0005);
    }

    @Test
    void returnsZeroWhenThereAreTooFewSamples() {
        double[] deltas = {0.15, 0.30, 0.45};
        assertEquals(0.0, GcdAnalysis.estimateConstant(deltas, deltas.length));
    }

    @Test
    void returnsZeroForRandomNoiseThatIsNotQuantised() {
        double[] deltas = new double[40];
        java.util.Random random = new java.util.Random(7L);
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = 0.11 + random.nextDouble() * 9.7;
        }
        assertEquals(0.0, GcdAnalysis.estimateConstant(deltas, deltas.length));
    }

    @Test
    void ignoresSubMouseMovementNoise() {
        double[] deltas = new double[40];
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = i % 2 == 0 ? 0.0001 : 0.0;
        }
        assertEquals(0.0, GcdAnalysis.estimateConstant(deltas, deltas.length));
    }

    @Test
    void ignoresNonFiniteDeltas() {
        double[] deltas = new double[40];
        for (int i = 0; i < deltas.length; i++) {
            deltas[i] = 0.15 * (1 + (i % 3));
        }
        deltas[3] = Double.NaN;
        deltas[9] = Double.POSITIVE_INFINITY;
        assertTrue(GcdAnalysis.estimateConstant(deltas, deltas.length) > 0.0);
    }

    @Test
    void recognisesAMultipleOfTheConstant() {
        assertTrue(GcdAnalysis.isMultipleOf(0.15, 0.15));
        assertTrue(GcdAnalysis.isMultipleOf(0.30, 0.15));
        assertTrue(GcdAnalysis.isMultipleOf(0.45, 0.15));
        assertTrue(GcdAnalysis.isMultipleOf(1.50, 0.15));
    }

    @Test
    void rejectsAnOffGridDelta() {
        assertFalse(GcdAnalysis.isMultipleOf(0.37, 0.15));
        assertFalse(GcdAnalysis.isMultipleOf(1.03, 0.15));
    }

    @Test
    void rejectsADeltaSmallerThanTheConstant() {
        assertFalse(GcdAnalysis.isMultipleOf(0.05, 0.15));
    }

    @Test
    void rejectsEverythingWhenTheConstantIsUnknown() {
        assertFalse(GcdAnalysis.isMultipleOf(0.15, 0.0));
    }

    @Test
    void theWindowLearnsTheConstantAsSamplesArrive() {
        GcdAnalysis.Window window = new GcdAnalysis.Window();
        assertEquals(0.0, window.constant());
        for (int i = 0; i < 30; i++) {
            window.offer(0.15 * (1 + (i % 4)));
        }
        assertEquals(30, window.size());
        assertEquals(0.15, window.constant(), 0.0005);
    }

    @Test
    void clearingTheWindowResetsTheLearnedConstant() {
        GcdAnalysis.Window window = new GcdAnalysis.Window();
        for (int i = 0; i < 30; i++) {
            window.offer(0.15 * (1 + (i % 4)));
        }
        assertTrue(window.constant() > 0.0);
        window.clear();
        assertEquals(0.0, window.constant());
        assertEquals(0, window.size());
    }

    @Test
    void theWindowStaysBoundedWhenOverfed() {
        GcdAnalysis.Window window = new GcdAnalysis.Window();
        for (int i = 0; i < GcdAnalysis.WINDOW + 200; i++) {
            window.offer(0.15 * (1 + (i % 4)));
        }
        assertEquals(GcdAnalysis.WINDOW, window.size());
        assertTrue(window.constant() > 0.0);
    }

    @Test
    void aHumanLikePitchDeltaIsStillAMultiple() {
        double constant = 0.1527;
        assertTrue(GcdAnalysis.isMultipleOf(constant * 12.0, constant));
    }
}
