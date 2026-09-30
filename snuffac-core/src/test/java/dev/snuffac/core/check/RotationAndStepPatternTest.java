package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.check.impl.combat.GcdAnalysis;
import dev.snuffac.core.check.impl.combat.KillAuraCheck;
import dev.snuffac.core.check.impl.movement.HighJumpCheck;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RotationAndStepPatternTest {

    private static final double TOLERANCE = 1.0E-9;

    @Test
    @DisplayName("perfectly even rotation steps are linear")
    void evenStepsAreLinear() {
        double[] values = {2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5};
        assertTrue(KillAuraCheck.isLinear(values), "a constant increment is linear");
    }

    @Test
    @DisplayName("a real mouse is not linear")
    void noisyStepsAreNotLinear() {
        Random random = new Random(20261001L);
        double[] values = new double[20];
        for (int i = 0; i < values.length; i++) {
            values[i] = 1.0 + random.nextGaussian() * 0.4;
        }
        assertFalse(KillAuraCheck.isLinear(values), "random jitter must not read as linear");
    }

    @Test
    @DisplayName("a straight sweep with jitter is still linear")
    void sweepWithJitterIsLinear() {
        double[] values = new double[24];
        for (int i = 0; i < values.length; i++) {
            values[i] = 0.5 + i * 0.25 + Math.sin(i * 2.3) * 0.05;
        }
        assertTrue(KillAuraCheck.isLinear(values), "a smooth sweep is still a straight line");
    }

    @Test
    @DisplayName("a turn in the middle breaks linearity")
    void turnBreaksLinearity() {
        double[] values = new double[20];
        for (int i = 0; i < 10; i++) {
            values[i] = 1.0 + i * 0.2;
        }
        for (int i = 10; i < 20; i++) {
            values[i] = 3.0 - (i - 10) * 0.2;
        }
        assertFalse(KillAuraCheck.isLinear(values), "a direction change is not one straight line");
    }

    @Test
    @DisplayName("identical steps have no spread")
    void constantStepsHaveNoJitter() {
        double[] values = new double[16];
        for (int i = 0; i < values.length; i++) {
            values[i] = 1.25;
        }
        assertTrue(KillAuraCheck.meanAbsoluteDeviation(values) < TOLERANCE,
                "a perfect constant is the no jitter case the rule looks for");
    }

    @Test
    @DisplayName("alternating steps have real spread")
    void alternatingStepsHaveJitter() {
        double[] values = {1.0, 2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 2.0};
        assertTrue(KillAuraCheck.meanAbsoluteDeviation(values) > 0.4,
                "alternating magnitudes are the jitter a human hand produces");
    }

    @Test
    @DisplayName("a too short sample is never called linear")
    void shortSampleIsRejected() {
        assertFalse(KillAuraCheck.isLinear(new double[] {1.0, 2.0, 3.0}),
                "three samples prove nothing");
    }

    @Test
    @DisplayName("mouse deltas on a 0.15 grid resolve to that grid")
    void gridResolves() {
        var window = new GcdAnalysis.Window();
        window.offer(0.15);
        window.offer(0.30);
        window.offer(0.15);
        window.offer(0.45);
        window.offer(0.60);
        assertTrue(Math.abs(window.constant() - 0.15) < 0.01,
                "expected a 0.15 constant, got " + window.constant());
    }

    @Test
    @DisplayName("a standing jump peaks at the vanilla figure once drag is applied")
    void plainJumpIsClean() {
        double maximum = HighJumpCheck.maximumLaunch(0, false, 1.0);
        assertTrue(Math.abs(maximum - 0.4116) < 0.0001,
                "0.42 launch reduced by the 0.98 drag is 0.4116, got " + maximum);
    }

    @Test
    @DisplayName("jump boost raises the launch ceiling by a tenth a level")
    void jumpBoostRaisesTheCeiling() {
        double plain = HighJumpCheck.maximumLaunch(0, false, 1.0);
        double oneLevel = HighJumpCheck.maximumLaunch(1, false, 1.0);
        double twoLevels = HighJumpCheck.maximumLaunch(2, false, 1.0);
        assertTrue(Math.abs(oneLevel - plain - 0.098) < 0.0001,
                "one level adds 0.1 before drag, got " + (oneLevel - plain));
        assertTrue(Math.abs(twoLevels - plain - 0.196) < 0.0001,
                "two levels add 0.2 before drag, got " + (twoLevels - plain));
    }

    @Test
    @DisplayName("sprint adds a fifth of a block over a standing jump")
    void sprintAddsALittle() {
        double standing = HighJumpCheck.maximumLaunch(0, false, 1.0);
        double sprinting = HighJumpCheck.maximumLaunch(0, true, 1.0);
        assertTrue(Math.abs(sprinting - standing - 0.196) < 0.0001,
                "the sprint bonus is 0.2 before drag, got " + (sprinting - standing));
    }

    @Test
    @DisplayName("a weaker jump attribute lowers the ceiling")
    void jumpStrengthLowersTheCeiling() {
        double normal = HighJumpCheck.maximumLaunch(0, false, 1.0);
        double weakened = HighJumpCheck.maximumLaunch(0, false, 0.7);
        assertTrue(weakened < normal - 0.1,
                "slowness on the jump attribute should reduce the ceiling");
    }
}
