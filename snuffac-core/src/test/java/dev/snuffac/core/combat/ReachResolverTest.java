package dev.snuffac.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.AxisAlignedBox;
import dev.snuffac.core.util.BlockPos;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReachResolverTest {

    private static EntitySnapshot target(Vec3d position, double width, double height) {
        AxisAlignedBox box = new AxisAlignedBox(
                position.x() - width / 2.0, position.y(), position.z() - width / 2.0,
                position.x() + width / 2.0, position.y() + height, position.z() + width / 2.0);
        return new EntitySnapshot(7, "zombie", position, box, true, false, 1.62, Vec3d.ZERO, -1.0);
    }

    @Test
    @DisplayName("eye height matches vanilla standing and crouching values")
    void eyeHeights() {
        assertEquals(1.62, ReachResolver.eyePosition(Vec3d.ZERO, false).y(), 1.0E-9);
        assertEquals(1.27, ReachResolver.eyePosition(Vec3d.ZERO, true).y(), 1.0E-9);
    }

    @Test
    @DisplayName("survival reach is three blocks and creative is five")
    void baseReach() {
        assertEquals(3.0, ReachResolver.baseReach(false, false), 1.0E-9);
        assertEquals(5.0, ReachResolver.baseReach(true, false), 1.0E-9);
        assertEquals(5.0, ReachResolver.baseReach(false, true), 1.0E-9);
    }

    @Test
    @DisplayName("a target inside survival reach resolves as within reach")
    void targetWithinReach() {
        EntitySnapshot target = target(new Vec3d(2.5, 64.0, 0.0), 0.6, 1.95);
        ReachResolver.ReachResult result = ReachResolver.resolve(
                new Vec3d(0.0, 64.0, 0.0), false, target, false, false, false);
        assertTrue(result.withinReach(), "distance was " + result.distance());
        assertTrue(result.excess() < 0.0);
        assertTrue(result.targetKnown());
        assertTrue(result.lineOfSight());
    }

    @Test
    @DisplayName("a target beyond survival reach produces a positive excess")
    void targetBeyondReach() {
        EntitySnapshot target = target(new Vec3d(6.0, 64.0, 0.0), 0.6, 1.95);
        ReachResolver.ReachResult result = ReachResolver.resolve(
                new Vec3d(0.0, 64.0, 0.0), false, target, false, false, false);
        assertFalse(result.withinReach());
        assertTrue(result.excess() > 2.0, "excess was " + result.excess());
    }

    @Test
    @DisplayName("creative reach is larger than survival reach for the same target")
    void creativeReachesFurther() {
        EntitySnapshot target = target(new Vec3d(4.0, 64.0, 0.0), 0.6, 1.95);
        ReachResolver.ReachResult survival = ReachResolver.resolve(
                new Vec3d(0.0, 64.0, 0.0), false, target, false, false, false);
        ReachResolver.ReachResult creative = ReachResolver.resolve(
                new Vec3d(0.0, 64.0, 0.0), false, target, true, false, false);
        assertFalse(survival.withinReach());
        assertTrue(creative.withinReach(), "creative should reach a target survival cannot");
    }

    @Test
    @DisplayName("an unknown target reports as unknown rather than guessing")
    void unknownTarget() {
        ReachResolver.ReachResult result = ReachResolver.resolve(
                new Vec3d(0.0, 64.0, 0.0), false, null, false, false, false);
        assertFalse(result.targetKnown());
        assertTrue(result.distance() > 1.0E300, "unknown targets report an unusable distance");
    }

    @Test
    @DisplayName("line of sight detects a block between the eye and the target")
    void lineOfSightBlocked() {
        Map<BlockPos, Boolean> solid = new HashMap<>();
        solid.put(BlockPos.of(2, 65, 0), true);
        ReachResolver.BlockOcclusion occlusion = position -> Boolean.TRUE.equals(solid.get(position));
        assertTrue(ReachResolver.segmentBlocked(
                new Vec3d(0.0, 65.62, 0.0), new Vec3d(4.0, 65.62, 0.0), occlusion));
    }

    @Test
    @DisplayName("line of sight is clear when nothing intervenes")
    void lineOfSightClear() {
        ReachResolver.BlockOcclusion occlusion = position -> false;
        assertFalse(ReachResolver.segmentBlocked(
                new Vec3d(0.0, 65.62, 0.0), new Vec3d(4.0, 65.62, 0.0), occlusion));
    }

    @Test
    @DisplayName("the combat environment reports the closest entity distance")
    void environmentNearest() {
        Map<Integer, EntitySnapshot> entities = new HashMap<>();
        entities.put(1, target(new Vec3d(2.0, 64.0, 0.0), 0.6, 1.95));
        entities.put(2, target(new Vec3d(9.0, 64.0, 0.0), 0.6, 1.95));
        CombatEnvironment environment = CombatEnvironment.of(1000L, entities, 12, 10);
        assertEquals(2, environment.count());
        assertTrue(environment.known(1));
        assertFalse(environment.known(99));
        assertTrue(environment.nearestDistance(new Vec3d(0.0, 64.0, 0.0)) < 3.0);
        assertEquals(10, environment.sentChunkRadius());
    }
}
