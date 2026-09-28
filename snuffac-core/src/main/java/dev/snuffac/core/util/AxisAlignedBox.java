package dev.snuffac.core.util;

import dev.snuffac.api.Vec3d;

public final class AxisAlignedBox {

    public static final AxisAlignedBox ZERO = new AxisAlignedBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    public AxisAlignedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public static AxisAlignedBox of(Vec3d min, Vec3d max) {
        return new AxisAlignedBox(
                Math.min(min.x(), max.x()),
                Math.min(min.y(), max.y()),
                Math.min(min.z(), max.z()),
                Math.max(min.x(), max.x()),
                Math.max(min.y(), max.y()),
                Math.max(min.z(), max.z()));
    }

    public AxisAlignedBox expanded(double amount) {
        return new AxisAlignedBox(
                minX - amount, minY - amount, minZ - amount,
                maxX + amount, maxY + amount, maxZ + amount);
    }

    public AxisAlignedBox offset(double dx, double dy, double dz) {
        return new AxisAlignedBox(
                minX + dx, minY + dy, minZ + dz,
                maxX + dx, maxY + dy, maxZ + dz);
    }

    public AxisAlignedBox offset(Vec3d delta) {
        return offset(delta.x(), delta.y(), delta.z());
    }

    public boolean contains(Vec3d point) {
        return point.x() >= minX && point.x() <= maxX
                && point.y() >= minY && point.y() <= maxY
                && point.z() >= minZ && point.z() <= maxZ;
    }

    public boolean intersects(AxisAlignedBox other) {
        return maxX >= other.minX && minX <= other.maxX
                && maxY >= other.minY && minY <= other.maxY
                && maxZ >= other.minZ && minZ <= other.maxZ;
    }

    public Vec3d center() {
        return new Vec3d((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5);
    }

    public double sizeX() {
        return maxX - minX;
    }

    public double sizeY() {
        return maxY - minY;
    }

    public double sizeZ() {
        return maxZ - minZ;
    }

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double minZ() {
        return minZ;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public double maxZ() {
        return maxZ;
    }

    public double volume() {
        return sizeX() * sizeY() * sizeZ();
    }

    public double closestDistance(Vec3d point) {
        double dx = Math.max(minX - point.x(), 0.0) + Math.max(point.x() - maxX, 0.0);
        double dy = Math.max(minY - point.y(), 0.0) + Math.max(point.y() - maxY, 0.0);
        double dz = Math.max(minZ - point.z(), 0.0) + Math.max(point.z() - maxZ, 0.0);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double closestDistanceSquared(Vec3d point) {
        double dx = Math.max(minX - point.x(), 0.0) + Math.max(point.x() - maxX, 0.0);
        double dy = Math.max(minY - point.y(), 0.0) + Math.max(point.y() - maxY, 0.0);
        double dz = Math.max(minZ - point.z(), 0.0) + Math.max(point.z() - maxZ, 0.0);
        return dx * dx + dy * dy + dz * dz;
    }

    public boolean withinDistance(Vec3d point, double distance) {
        return closestDistanceSquared(point) <= distance * distance;
    }

    @Override
    public String toString() {
        return "[" + minX + ", " + minY + ", " + minZ + " -> " + maxX + ", " + maxY + ", " + maxZ + "]";
    }
}
