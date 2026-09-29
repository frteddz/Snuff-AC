package dev.snuffac.core.combat;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.AxisAlignedBox;

public final class HitboxVerifier {

    public static final double PLAYER_WIDTH = 0.6;
    public static final double PLAYER_HEIGHT = 1.8;
    public static final double SNEAK_HEIGHT = 1.5;
    public static final double EPSILON = 1.0E-7;

    private HitboxVerifier() {
    }

    public static AxisAlignedBox playerBox(Vec3d feet, boolean sneaking) {
        double height = sneaking ? SNEAK_HEIGHT : PLAYER_HEIGHT;
        return new AxisAlignedBox(
                feet.x() - PLAYER_WIDTH / 2.0, feet.y(), feet.z() - PLAYER_WIDTH / 2.0,
                feet.x() + PLAYER_WIDTH / 2.0, feet.y() + height, feet.z() + PLAYER_WIDTH / 2.0);
    }

    public static AxisAlignedBox canonical(EntitySnapshot snapshot) {
        if (snapshot.hitbox() != null && snapshot.hitbox().volume() > 0.0) {
            return snapshot.hitbox();
        }
        return playerBox(snapshot.position(), false);
    }

    public static boolean intersects(AxisAlignedBox box, Vec3d origin, Vec3d direction) {
        double dx = direction.x();
        double dy = direction.y();
        double dz = direction.z();
        if (Math.abs(dx) < EPSILON && Math.abs(dy) < EPSILON && Math.abs(dz) < EPSILON) {
            return false;
        }
        double minX = box.minX();
        double minY = box.minY();
        double minZ = box.minZ();
        double maxX = box.maxX();
        double maxY = box.maxY();
        double maxZ = box.maxZ();

        double tMin;
        double tMax;

        if (Math.abs(dx) < EPSILON) {
            if (origin.x() < minX || origin.x() > maxX) {
                return false;
            }
            tMin = Double.NEGATIVE_INFINITY;
            tMax = Double.POSITIVE_INFINITY;
        } else {
            double inverse = 1.0 / dx;
            double t1 = (minX - origin.x()) * inverse;
            double t2 = (maxX - origin.x()) * inverse;
            tMin = Math.min(t1, t2);
            tMax = Math.max(t1, t2);
        }

        if (Math.abs(dy) < EPSILON) {
            if (origin.y() < minY || origin.y() > maxY) {
                return false;
            }
        } else {
            double inverse = 1.0 / dy;
            double t1 = (minY - origin.y()) * inverse;
            double t2 = (maxY - origin.y()) * inverse;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }

        if (Math.abs(dz) < EPSILON) {
            if (origin.z() < minZ || origin.z() > maxZ) {
                return false;
            }
        } else {
            double inverse = 1.0 / dz;
            double t1 = (minZ - origin.z()) * inverse;
            double t2 = (maxZ - origin.z()) * inverse;
            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }

        return tMax >= Math.max(tMin, 0.0);
    }

    public static double angleToCenterDegrees(Vec3d eye, double yaw, double pitch, Vec3d target) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double fx = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double fy = -Math.sin(pitchRadians);
        double fz = Math.cos(yawRadians) * Math.cos(pitchRadians);

        double dx = target.x() - eye.x();
        double dy = target.y() - eye.y();
        double dz = target.z() - eye.z();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < EPSILON) {
            return 0.0;
        }
        double dot = (fx * dx + fy * dy + fz * dz) / length;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    public record Result(
            boolean targetKnown,
            boolean rayHits,
            boolean withinReach,
            boolean lineOfSightClear,
            double reach,
            double maximum,
            double angleDegrees,
            Vec3d eye,
            String reason) {

        public boolean shouldReject() {
            return targetKnown && (!withinReach || !rayHits || !lineOfSightClear);
        }

        public static Result unknown(String reason) {
            return new Result(false, false, true, true, 0.0, 0.0, 0.0, Vec3d.ZERO, reason);
        }
    }

    public static Result verify(
            Vec3d feet,
            boolean sneaking,
            float yaw,
            float pitch,
            EntitySnapshot target,
            boolean creative,
            boolean inVehicle,
            double tolerance,
            boolean lineOfSightClear) {

        Vec3d eye = ReachResolver.eyePosition(feet, sneaking);
        if (target == null) {
            return Result.unknown("target snapshot missing");
        }
        AxisAlignedBox box = canonical(target);
        double maximum = ReachResolver.baseReach(creative, inVehicle) + tolerance;
        double reach = box.expanded(ReachResolver.verticalPadding()).closestDistance(eye);

        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        Vec3d direction = new Vec3d(
                -Math.sin(yawRadians) * Math.cos(pitchRadians),
                -Math.sin(pitchRadians),
                Math.cos(yawRadians) * Math.cos(pitchRadians));

        boolean rayHits = intersects(box, eye, direction);
        double angle = angleToCenterDegrees(eye, yaw, pitch, box.center());

        String reason;
        if (reach > maximum) {
            reason = "reach " + round(reach) + " over " + round(maximum);
        } else if (!lineOfSightClear) {
            reason = "no line of sight";
        } else if (!rayHits) {
            reason = "look ray misses the hitbox, angle " + round(angle) + " degrees";
        } else {
            reason = "legal";
        }

        return new Result(true, rayHits, reach <= maximum, lineOfSightClear,
                reach, maximum, angle, eye, reason);
    }

    public static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
