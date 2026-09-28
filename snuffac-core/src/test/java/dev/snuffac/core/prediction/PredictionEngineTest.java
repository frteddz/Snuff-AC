package dev.snuffac.core.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.physics.MovementAttributes;
import dev.snuffac.core.physics.MovementEnvironment;
import dev.snuffac.core.physics.MovementInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PredictionEngineTest {

    @Test
    @DisplayName("an exact sprinting step resolves to near zero offset")
    void exactStepResolvesToZero() {
        MovementEnvironment environment = MovementEnvironment.GROUND.withSprinting(true);
        Vec3d start = Vec3d.ZERO;
        Vec3d predicted = PredictionEngine.applyTick(start,
                new MovementInput(1.0, 0.0, false, false), environment, MovementAttributes.DEFAULT);
        PredictionResult result = PredictionEngine.predict(start, predicted, environment, MovementAttributes.DEFAULT);
        assertTrue(result.offset().length() < 1.0E-6,
                "prediction should match an exact simulated step");
    }

    @Test
    @DisplayName("an impossibly fast step produces a large offset")
    void fastStepProducesOffset() {
        MovementEnvironment environment = MovementEnvironment.GROUND.withSprinting(true);
        Vec3d start = Vec3d.ZERO;
        Vec3d predicted = PredictionEngine.applyTick(start,
                new MovementInput(1.0, 0.0, false, false), environment, MovementAttributes.DEFAULT);
        Vec3d cheated = predicted.multiply(3.0);
        PredictionResult result = PredictionEngine.predict(start, cheated, environment, MovementAttributes.DEFAULT);
        assertTrue(result.offset().length() > 0.1,
                "a tripled step should be flagged as a large offset");
    }

    @Test
    @DisplayName("the predictor discovers the correct input direction without being told")
    void discoversInputDirection() {
        MovementEnvironment environment = MovementEnvironment.GROUND.withSprinting(true);
        Vec3d start = Vec3d.ZERO;
        MovementInput truth = new MovementInput(1.0, 0.0, false, false);
        Vec3d actual = PredictionEngine.applyTick(start, truth, environment, MovementAttributes.DEFAULT);

        PredictionResult result = PredictionEngine.predict(start, actual, environment, MovementAttributes.DEFAULT);
        assertTrue(result.offset().length() < 1.0E-6);
        assertTrue(result.matchedInput().forward() > 0.0,
                "the recovered input should point forward, matching the truth");
        assertEquals(0.0, result.matchedInput().strafe(), 1.0E-9,
                "the recovered input should not strafe, matching the truth");
    }

    @Test
    @DisplayName("a strafe only step is recovered as a strafe")
    void discoversStrafeInput() {
        MovementEnvironment environment = MovementEnvironment.GROUND;
        Vec3d start = Vec3d.ZERO;
        MovementInput truth = new MovementInput(0.0, -1.0, false, false);
        Vec3d actual = PredictionEngine.applyTick(start, truth, environment, MovementAttributes.DEFAULT);

        PredictionResult result = PredictionEngine.predict(start, actual, environment, MovementAttributes.DEFAULT);
        assertTrue(result.offset().length() < 1.0E-6);
        assertTrue(result.matchedInput().strafe() < 0.0,
                "the recovered input should strafe in the same direction as the truth");
    }

    @Test
    @DisplayName("backwards movement is discovered as well")
    void discoversReverseInput() {
        MovementEnvironment environment = MovementEnvironment.GROUND;
        Vec3d start = new Vec3d(0.0, 0.0, 0.05);
        MovementInput truth = new MovementInput(-1.0, 0.0, false, false);
        Vec3d actual = PredictionEngine.applyTick(start, truth, environment, MovementAttributes.DEFAULT);
        PredictionResult result = PredictionEngine.predict(start, actual, environment, MovementAttributes.DEFAULT);
        assertTrue(result.offset().length() < 1.0E-6);
        assertTrue(result.matchedInput().forward() < 0.0,
                "predicted input should be backwards when the truth is backwards");
    }

    @Test
    @DisplayName("a stationary step yields a zero offset")
    void stationaryStepIsClean() {
        MovementEnvironment environment = MovementEnvironment.GROUND;
        PredictionResult result = PredictionEngine.predict(
                Vec3d.ZERO, Vec3d.ZERO, environment, MovementAttributes.DEFAULT);
        assertTrue(result.offset().length() < 1.0E-9);
    }

    @Test
    @DisplayName("the maximum horizontal speed helper returns a finite bound for ground and air")
    void maximumSpeedIsFinite() {
        double ground = PredictionEngine.maximumHorizontalSpeed(
                MovementEnvironment.GROUND.withSprinting(true), MovementAttributes.DEFAULT);
        double air = PredictionEngine.maximumHorizontalSpeed(
                MovementEnvironment.AIR.withSprinting(true), MovementAttributes.DEFAULT);
        assertTrue(ground > 0.0 && Double.isFinite(ground));
        assertTrue(air > 0.0 && Double.isFinite(air));
    }

    @Test
    @DisplayName("gravity only steps are predicted accurately")
    void gravityStepIsAccurate() {
        MovementEnvironment environment = MovementEnvironment.AIR;
        Vec3d start = Vec3d.ZERO;
        Vec3d actual = PredictionEngine.applyTick(start, MovementInput.NONE, environment, MovementAttributes.DEFAULT);
        assertEquals(-0.0784, actual.y(), 1.0E-6,
                "first airborne step should apply gravity then vertical drag");
    }
}
