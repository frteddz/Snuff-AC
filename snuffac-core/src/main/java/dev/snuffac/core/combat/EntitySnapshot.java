package dev.snuffac.core.combat;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.AxisAlignedBox;

public record EntitySnapshot(
        int entityId,
        String typeName,
        Vec3d position,
        AxisAlignedBox hitbox,
        boolean living,
        boolean player,
        double eyeHeight,
        Vec3d velocity,
        double lastDamageMillis
) {

    public static EntitySnapshot unknown(int entityId, Vec3d position) {
        return new EntitySnapshot(entityId, "unknown", position, AxisAlignedBox.ZERO,
                false, false, 0.0, Vec3d.ZERO, -1.0);
    }

    public Vec3d center() {
        return hitbox.center();
    }

    public double distanceFrom(Vec3d eye) {
        return eye.distanceTo(center());
    }

    public double closestDistanceFrom(Vec3d eye) {
        return hitbox.closestDistance(eye);
    }

    public double expandedDistanceFrom(Vec3d eye, double margin) {
        return hitbox.expanded(margin).closestDistance(eye);
    }
}
