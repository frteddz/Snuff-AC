package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.impl.combat.HitboxCheck;
import dev.snuffac.core.check.impl.combat.TriggerBotCheck;
import dev.snuffac.core.combat.EntitySnapshot;
import dev.snuffac.core.combat.HitboxVerifier;
import dev.snuffac.core.util.AxisAlignedBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HitboxAndTriggerTest {

    private static final Vec3d EYE = new Vec3d(0.0, 65.62, 0.0);

    @Test
    @DisplayName("a ray straight at a target needs no expansion")
    void directHitNeedsNoExpansion() {
        AxisAlignedBox box = box(0.0, 65.0, 3.0);
        Vec3d direction = HitboxCheck.facingDirection(0.0f, 0.0f);
        assertEquals(0.0, HitboxCheck.smallestExpansion(box, EYE, direction),
                "a ray dead centre should hit the real box outright");
    }

    @Test
    @DisplayName("a ray at the edge of the box needs no expansion either")
    void edgeHitNeedsNoExpansion() {
        AxisAlignedBox box = box(0.28, 65.0, 3.0);
        Vec3d direction = HitboxCheck.facingDirection(0.0f, 0.0f);
        assertTrue(HitboxCheck.smallestExpansion(box, EYE, direction) < HitboxCheck.MIN_EXPANSION,
                "a ray that clips the real box is a legitimate hit");
    }

    @Test
    @DisplayName("a ray just past the box needs a real expansion to land")
    void nearMissNeedsExpansion() {
        AxisAlignedBox box = box(0.45, 65.0, 3.0);
        Vec3d direction = HitboxCheck.facingDirection(0.0f, 0.0f);
        double expansion = HitboxCheck.smallestExpansion(box, EYE, direction);
        assertTrue(expansion > 0.05,
                "a ray that misses the real box should report how far out it was, got " + expansion);
    }

    @Test
    @DisplayName("a ray well past the box needs a large expansion")
    void clearMissNeedsALot() {
        AxisAlignedBox box = box(1.2, 65.0, 3.0);
        Vec3d direction = HitboxCheck.facingDirection(0.0f, 0.0f);
        assertTrue(HitboxCheck.smallestExpansion(box, EYE, direction) >= 0.4,
                "a hit that far out means a substantially larger box");
    }

    @Test
    @DisplayName("the expansion search is bounded")
    void expansionIsBounded() {
        AxisAlignedBox box = box(40.0, 65.0, 3.0);
        Vec3d direction = HitboxCheck.facingDirection(0.0f, 0.0f);
        assertTrue(HitboxCheck.smallestExpansion(box, EYE, direction) <= 1.0,
                "the search must terminate rather than run forever");
    }

    @Test
    @DisplayName("the vanilla box is the documented player size")
    void vanillaBoxIsCorrect() {
        var snapshot = new EntitySnapshot(1, "player", new Vec3d(10.0, 70.0, 10.0),
                AxisAlignedBox.ZERO, true, true, 1.62, Vec3d.ZERO, -1.0);
        AxisAlignedBox box = HitboxCheck.vanillaBox(snapshot, false);
        assertTrue(Math.abs(box.sizeX() - 0.6) < 1.0E-9, "a player box is 0.6 wide");
        assertTrue(Math.abs(box.sizeZ() - 0.6) < 1.0E-9, "a player box is 0.6 deep");
        assertTrue(Math.abs(box.sizeY() - 1.8) < 1.0E-9, "a standing player box is 1.8 tall");
        assertTrue(Math.abs(box.minY() - 70.0) < 1.0E-9, "the box starts at the feet");
    }

    @Test
    @DisplayName("a crouching box is shorter and the same width")
    void crouchBoxIsShorter() {
        var snapshot = new EntitySnapshot(1, "player", new Vec3d(10.0, 70.0, 10.0),
                AxisAlignedBox.ZERO, true, true, 1.27, Vec3d.ZERO, -1.0);
        AxisAlignedBox box = HitboxCheck.vanillaBox(snapshot, true);
        assertTrue(Math.abs(box.sizeY() - 1.5) < 1.0E-9, "a crouching player box is 1.5 tall");
        assertTrue(Math.abs(box.sizeX() - 0.6) < 1.0E-9, "and still 0.6 wide");
    }

    @Test
    @DisplayName("a ray aimed past the target is not an expanded hitbox")
    void rayPastTheTargetMisses() {
        AxisAlignedBox box = box(0.0, 65.0, 3.0);
        Vec3d direction = HitboxCheck.facingDirection(0.0f, 0.0f);
        assertEquals(0.0, HitboxCheck.smallestExpansion(box, EYE, direction),
                "the ray passes through the box, so no expansion is needed");
        AxisAlignedBox beside = box(0.5, 65.0, 3.0);
        double sideways = HitboxCheck.smallestExpansion(beside, EYE, direction);
        assertTrue(sideways > HitboxCheck.MIN_EXPANSION,
                "a target half a block to the side only connects with a bigger box, got " + sideways);
        assertTrue(sideways <= 1.0, "and the search still terminates, got " + sideways);
    }

    @Test
    @DisplayName("the raycast agrees with the box it was given")
    void raycastMatchesBox() {
        AxisAlignedBox box = box(0.0, 65.0, 3.0);
        assertTrue(HitboxVerifier.intersects(box, EYE, HitboxCheck.facingDirection(0.0f, 0.0f)));
    }

    @Test
    @DisplayName("the trigger window is between one and three ticks")
    void triggerWindowIsSane() {
        assertTrue(TriggerBotCheck.MIN_REACTION_NANOS > 0L,
                "there has to be a floor so the same tick is not a reaction");
        assertTrue(TriggerBotCheck.REACTION_NANOS < 100_000_000L,
                "and a ceiling, since a human reaction is longer than three ticks");
        assertTrue(TriggerBotCheck.REACTION_NANOS > 50_000_000L,
                "a full tick is the natural lower bound for a settled crosshair");
    }

    @Test
    @DisplayName("the trigger rule needs a run of samples before it fires")
    void triggerNeedsSamples() {
        assertTrue(TriggerBotCheck.SAMPLE_TARGETS >= 6,
                "a couple of fast reactions are just a fast player");
        assertTrue(TriggerBotCheck.REQUIRED_CONSISTENT >= 3,
                "and the run has to repeat before it counts");
    }

    private static AxisAlignedBox box(double x, double y, double z) {
        return new AxisAlignedBox(
                x - 0.3, y, z - 0.3,
                x + 0.3, y + 1.8, z + 0.3);
    }
}
