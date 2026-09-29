package dev.snuffac.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.AxisAlignedBox;
import org.junit.jupiter.api.Test;

class HitboxVerifierTest {

    private static final double PLAYER_HITBOX_TOLERANCE = 0.001;

    private static EntitySnapshot targetAt(Vec3d feet) {
        return new EntitySnapshot(
                7,
                "player",
                feet,
                HitboxVerifier.playerBox(feet, false),
                true,
                true,
                1.62,
                Vec3d.ZERO,
                -1.0);
    }

    @Test
    void buildsTheVanillaPlayerBox() {
        AxisAlignedBox box = HitboxVerifier.playerBox(new Vec3d(10.0, 64.0, 10.0), false);
        assertEquals(0.6, box.sizeX(), PLAYER_HITBOX_TOLERANCE);
        assertEquals(0.6, box.sizeZ(), PLAYER_HITBOX_TOLERANCE);
        assertEquals(1.8, box.sizeY(), PLAYER_HITBOX_TOLERANCE);
        assertEquals(9.7, box.minX(), PLAYER_HITBOX_TOLERANCE);
        assertEquals(10.3, box.maxX(), PLAYER_HITBOX_TOLERANCE);
        assertEquals(64.0, box.minY(), PLAYER_HITBOX_TOLERANCE);
        assertEquals(65.8, box.maxY(), PLAYER_HITBOX_TOLERANCE);
    }

    @Test
    void buildsTheSneakingPlayerBox() {
        AxisAlignedBox box = HitboxVerifier.playerBox(new Vec3d(0.0, 0.0, 0.0), true);
        assertEquals(1.5, box.sizeY(), PLAYER_HITBOX_TOLERANCE);
    }

    @Test
    void rayStraightDownTheMiddleOfTheBoxHits() {
        AxisAlignedBox box = HitboxVerifier.playerBox(new Vec3d(5.0, 0.0, 5.0), false);
        Vec3d eye = new Vec3d(5.0, 1.62, 5.0);
        Vec3d forward = new Vec3d(0.0, 0.0, 1.0);
        assertTrue(HitboxVerifier.intersects(box, eye, forward));
    }

    @Test
    void rayPastTheEdgeOfTheBoxMisses() {
        AxisAlignedBox box = HitboxVerifier.playerBox(new Vec3d(5.0, 0.0, 5.0), false);
        Vec3d eye = new Vec3d(40.0, 1.62, 5.0);
        Vec3d forward = new Vec3d(0.0, 0.0, 1.0);
        assertFalse(HitboxVerifier.intersects(box, eye, forward));
    }

    @Test
    void rayPointedAwayFromTheBoxMisses() {
        AxisAlignedBox box = HitboxVerifier.playerBox(new Vec3d(0.0, 0.0, 0.0), false);
        Vec3d eye = new Vec3d(0.0, 1.62, -6.0);
        Vec3d backwards = new Vec3d(0.0, 0.0, -1.0);
        assertFalse(HitboxVerifier.intersects(box, eye, backwards));
    }

    @Test
    void aRayStartingInsideTheBoxStillIntersects() {
        AxisAlignedBox box = HitboxVerifier.playerBox(new Vec3d(0.0, 0.0, 0.0), false);
        Vec3d inside = new Vec3d(0.0, 1.62, 0.0);
        assertTrue(HitboxVerifier.intersects(box, inside, new Vec3d(0.0, 0.0, -1.0)));
    }

    @Test
    void aZeroDirectionNeverIntersects() {
        AxisAlignedBox box = HitboxVerifier.playerBox(new Vec3d(0.0, 0.0, 0.0), false);
        assertFalse(HitboxVerifier.intersects(box, new Vec3d(0.0, 1.0, 0.0), Vec3d.ZERO));
    }

    @Test
    void aLegalCloseHitIsAccepted() {
        Vec3d feet = new Vec3d(0.0, 64.0, 0.0);
        Vec3d targetFeet = new Vec3d(0.0, 64.0, 2.0);
        var result = HitboxVerifier.verify(
                feet, false, 0.0f, 0.0f, targetAt(targetFeet), false, false, 0.1, true);
        assertTrue(result.rayHits(), "looking straight at a target two blocks away must hit");
        assertTrue(result.withinReach());
        assertTrue(result.lineOfSightClear());
        assertFalse(result.shouldReject());
    }

    @Test
    void anAttackBeyondVanillaReachIsRejected() {
        Vec3d feet = new Vec3d(0.0, 64.0, 0.0);
        Vec3d targetFeet = new Vec3d(0.0, 64.0, 4.5);
        var result = HitboxVerifier.verify(
                feet, false, 0.0f, 0.0f, targetAt(targetFeet), false, false, 0.1, true);
        assertFalse(result.withinReach());
        assertTrue(result.shouldReject());
    }

    @Test
    void creativeReachIsLongerThanSurvivalReach() {
        Vec3d feet = new Vec3d(0.0, 64.0, 0.0);
        Vec3d targetFeet = new Vec3d(0.0, 64.0, 4.0);
        var survival = HitboxVerifier.verify(
                feet, false, 0.0f, 0.0f, targetAt(targetFeet), false, false, 0.1, true);
        var creative = HitboxVerifier.verify(
                feet, false, 0.0f, 0.0f, targetAt(targetFeet), true, false, 0.1, true);
        assertFalse(survival.withinReach());
        assertTrue(creative.withinReach());
        assertTrue(creative.maximum() > survival.maximum());
    }

    @Test
    void aHitThroughAWallIsRejectedEvenInsideReach() {
        Vec3d feet = new Vec3d(0.0, 64.0, 0.0);
        Vec3d targetFeet = new Vec3d(0.0, 64.0, 2.0);
        var result = HitboxVerifier.verify(
                feet, false, 0.0f, 0.0f, targetAt(targetFeet), false, false, 0.1, false);
        assertTrue(result.withinReach());
        assertFalse(result.lineOfSightClear());
        assertTrue(result.shouldReject(), "a wall between the attacker and the target must block the hit");
    }

    @Test
    void aLookRayThatMissesTheBoxIsRejected() {
        Vec3d feet = new Vec3d(0.0, 64.0, 0.0);
        Vec3d targetFeet = new Vec3d(3.0, 64.0, 3.0);
        var result = HitboxVerifier.verify(
                feet, false, 180.0f, 0.0f, targetAt(targetFeet), false, false, 0.5, true);
        assertFalse(result.rayHits(), "facing away from the target must miss the box");
        assertTrue(result.shouldReject(), "an expanded hitbox attack must be rejected");
    }

    @Test
    void aMissingTargetIsNotRejected() {
        var result = HitboxVerifier.verify(
                new Vec3d(0.0, 64.0, 0.0), false, 0.0f, 0.0f, null, false, false, 0.1, true);
        assertFalse(result.targetKnown());
        assertFalse(result.shouldReject(), "an unknown target must never be punished");
    }

    @Test
    void aZeroLengthHitboxFallsBackToTheVanillaBox() {
        var degenerate = new EntitySnapshot(
                3, "mystery", new Vec3d(0.0, 0.0, 0.0), AxisAlignedBox.ZERO,
                false, false, 0.0, Vec3d.ZERO, -1.0);
        AxisAlignedBox box = HitboxVerifier.canonical(degenerate);
        assertEquals(0.6, box.sizeX(), PLAYER_HITBOX_TOLERANCE);
        assertEquals(1.8, box.sizeY(), PLAYER_HITBOX_TOLERANCE);
    }

    @Test
    void theAngleToTheTargetCentreIsZeroWhenLookingStraightAtIt() {
        Vec3d eye = new Vec3d(0.0, 65.62, 0.0);
        double angle = HitboxVerifier.angleToCenterDegrees(
                eye, 0.0f, 0.0f, new Vec3d(0.0, 65.62, 3.0));
        assertEquals(0.0, angle, 0.01);
    }

    @Test
    void theAngleToTheTargetCentreIsLargeWhenLookingTheOtherWay() {
        Vec3d eye = new Vec3d(0.0, 65.62, 0.0);
        double angle = HitboxVerifier.angleToCenterDegrees(
                eye, 180.0f, 0.0f, new Vec3d(0.0, 65.62, 3.0));
        assertEquals(180.0, angle, 0.5);
    }

    @Test
    void theAngleNeverExceedsOneHundredEighty() {
        Vec3d eye = new Vec3d(0.0, 65.62, 0.0);
        double angle = HitboxVerifier.angleToCenterDegrees(
                eye, 91.0f, -37.0f, new Vec3d(5.0, 64.0, -2.0));
        assertTrue(angle >= 0.0 && angle <= 180.0, "got " + angle);
    }

    @Test
    void aTargetAtTheEyePositionDoesNotDivideByZero() {
        Vec3d eye = new Vec3d(0.0, 65.62, 0.0);
        double angle = HitboxVerifier.angleToCenterDegrees(eye, 0.0f, 0.0f, eye);
        assertEquals(0.0, angle, 0.0);
    }
}
