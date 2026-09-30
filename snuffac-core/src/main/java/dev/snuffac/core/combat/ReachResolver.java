package dev.snuffac.core.combat;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.AxisAlignedBox;
import dev.snuffac.core.util.BlockPos;

public final class ReachResolver {

    public static final double SURVIVAL_MAX = 3.0;
    public static final double CREATIVE_MAX = 5.0;
    public static final double INTERACTION_SURVIVAL_MAX = 4.5;
    public static final double INTERACTION_CREATIVE_MAX = 5.0;
    public static final double INTERACTION_MARGIN = 0.0;
    public static final double LINE_OF_SIGHT_STEP = 0.2;

    private ReachResolver() {
    }

    public static double eyeHeight(boolean sneaking) {
        return sneaking ? 1.27 : 1.62;
    }

    public static Vec3d eyePosition(Vec3d feet, boolean sneaking) {
        return new Vec3d(feet.x(), feet.y() + eyeHeight(sneaking), feet.z());
    }

    public static double baseReach(boolean creative, boolean inVehicle) {
        if (inVehicle) {
            return creative ? 8.0 : 5.0;
        }
        return creative ? CREATIVE_MAX : SURVIVAL_MAX;
    }

    public static double interactionMaximum(boolean creative) {
        return creative ? INTERACTION_CREATIVE_MAX : INTERACTION_SURVIVAL_MAX;
    }

    public static AxisAlignedBox blockBox(BlockPos position) {
        return new AxisAlignedBox(
                position.x(), position.y(), position.z(),
                position.x() + 1.0, position.y() + 1.0, position.z() + 1.0);
    }

    public record InteractionResult(
            double distance,
            double allowed,
            double excess,
            boolean withinRange,
            Vec3d eye,
            Vec3d closest,
            AxisAlignedBox blockBox) {
    }

    public static InteractionResult resolveInteraction(
            Vec3d feet,
            boolean sneaking,
            BlockPos position,
            boolean creative) {

        Vec3d eye = eyePosition(feet, sneaking);
        AxisAlignedBox box = blockBox(position);
        Vec3d closest = box.closestPoint(eye);
        double distance = eye.distanceTo(closest);
        double allowed = interactionMaximum(creative);
        double excess = distance - allowed;
        return new InteractionResult(
                distance,
                allowed,
                excess,
                excess <= 0.0,
                eye,
                closest,
                box);
    }

    public static double verticalPadding() {
        return 0.2;
    }

    public record ReachResult(
            double distance,
            double allowed,
            double excess,
            boolean withinReach,
            boolean targetKnown,
            boolean lineOfSight,
            Vec3d eye,
            AxisAlignedBox targetBox) {
    }

    public static ReachResult resolve(
            Vec3d feet,
            boolean sneaking,
            EntitySnapshot target,
            boolean creative,
            boolean inVehicle,
            boolean lineOfSightObstructed) {

        Vec3d eye = eyePosition(feet, sneaking);
        if (target == null) {
            return new ReachResult(Double.MAX_VALUE, baseReach(creative, inVehicle), 0.0,
                    false, false, false, eye, AxisAlignedBox.ZERO);
        }
        double allowed = baseReach(creative, inVehicle);
        double distance = target.expandedDistanceFrom(eye, verticalPadding());
        double excess = distance - allowed;
        return new ReachResult(
                distance,
                allowed,
                excess,
                excess <= 0.0,
                true,
                !lineOfSightObstructed,
                eye,
                target.hitbox());
    }

    public static boolean segmentBlocked(
            Vec3d from,
            Vec3d to,
            BlockOcclusion occlusion) {

        double distance = from.distanceTo(to);
        if (distance < 1.0E-6) {
            return false;
        }
        int steps = (int) Math.ceil(distance / LINE_OF_SIGHT_STEP);
        Vec3d delta = to.subtract(from);
        for (int i = 1; i < steps; i++) {
            double progress = (double) i / steps;
            Vec3d sample = from.add(delta.multiply(progress));
            if (occlusion.opaqueAt(BlockPos.of(sample))) {
                return true;
            }
        }
        return false;
    }

    public interface BlockOcclusion {

        boolean opaqueAt(BlockPos position);
    }
}
