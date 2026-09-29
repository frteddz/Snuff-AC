package dev.snuffac.core.check.impl.movement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.physics.MovementAttributes;
import dev.snuffac.core.physics.MovementConstants;
import org.junit.jupiter.api.Test;

class JumpLaunchTest {

    private static final double VANILLA_LAUNCH = MovementConstants.JUMP_VELOCITY
            * MovementConstants.VERTICAL_DRAG;

    @Test
    void aPlainJumpIsNotAboveTheLegalMaximum() {
        double maximum = HighJumpCheck.maximumLaunch(0, false, 1.0);
        assertEquals(VANILLA_LAUNCH, maximum, 1.0E-9);
        assertTrue(maximum > 0.4,
                "the ceiling for a vanilla jump must be near 0.4116, got " + maximum);
    }

    @Test
    void theServerAttributeIsTheImpulseNotAMultiplier() {
        assertEquals(0.42, MovementAttributes.SERVER_JUMP_STRENGTH, 1.0E-9,
                "the JUMP_STRENGTH attribute on 1.21.11 defaults to the impulse itself");
        assertEquals(1.0, MovementAttributes.normaliseJumpStrength(0.42), 1.0E-9,
                "a default attribute value must normalise back to the neutral multiplier");
    }

    @Test
    void theAttributeIsNoLongerDoubleCounted() {
        double doubleCounted = HighJumpCheck.maximumLaunch(0, false,
                MovementAttributes.SERVER_JUMP_STRENGTH);
        assertTrue(doubleCounted < VANILLA_LAUNCH * 0.5,
                "passing the raw attribute must not be possible, it gave " + doubleCounted);
    }

    @Test
    void aRealisticVanillaLaunchIsAccepted() {
        double maximum = HighJumpCheck.maximumLaunch(0, false, 1.0);
        double observed = (MovementConstants.JUMP_VELOCITY
                - MovementConstants.GRAVITY) * MovementConstants.VERTICAL_DRAG;
        assertTrue(observed <= maximum + HighJumpCheck.MIN_EXCESS,
                "the first airborne delta " + observed + " must not exceed " + maximum);
    }

    @Test
    void aSprintJumpGetsTheSprintBonus() {
        double walking = HighJumpCheck.maximumLaunch(0, false, 1.0);
        double sprinting = HighJumpCheck.maximumLaunch(0, true, 1.0);
        assertTrue(sprinting > walking, "a sprint jump is higher than a walk jump");
        assertEquals(MovementConstants.SPRINT_JUMP_BOOST * MovementConstants.VERTICAL_DRAG,
                sprinting - walking, 1.0E-9);
    }

    @Test
    void jumpBoostRaisesTheCeiling() {
        double plain = HighJumpCheck.maximumLaunch(0, false, 1.0);
        double boosted = HighJumpCheck.maximumLaunch(2, false, 1.0);
        assertTrue(boosted > plain, "jump boost must raise the ceiling");
    }

    @Test
    void aGenuineHighJumpIsStillCaught() {
        double maximum = HighJumpCheck.maximumLaunch(0, false, 1.0);
        double cheated = 0.75;
        assertTrue(cheated - maximum > HighJumpCheck.MIN_EXCESS,
                "a launch of " + cheated + " must still be well over the ceiling " + maximum);
    }
}
