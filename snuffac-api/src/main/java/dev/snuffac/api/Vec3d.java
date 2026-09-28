package dev.snuffac.api;

public record Vec3d(double x, double y, double z) {

    public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);

    public Vec3d add(Vec3d other) {
        return new Vec3d(x + other.x, y + other.y, z + other.z);
    }

    public Vec3d subtract(Vec3d other) {
        return new Vec3d(x - other.x, y - other.y, z - other.z);
    }

    public Vec3d multiply(double factor) {
        return new Vec3d(x * factor, y * factor, z * factor);
    }

    public Vec3d multiply(Vec3d other) {
        return new Vec3d(x * other.x, y * other.y, z * other.z);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double horizontalLength() {
        return Math.sqrt(x * x + z * z);
    }

    public double horizontalLengthSquared() {
        return x * x + z * z;
    }

    public Vec3d normalize() {
        double len = length();
        return len < 1.0E-9 ? ZERO : multiply(1.0 / len);
    }

    public Vec3d withY(double newY) {
        return new Vec3d(x, newY, z);
    }

    public Vec3d setX(double newX) {
        return new Vec3d(newX, y, z);
    }

    public Vec3d setY(double newY) {
        return new Vec3d(x, newY, z);
    }

    public Vec3d setZ(double newZ) {
        return new Vec3d(x, y, newZ);
    }

    public double distanceTo(Vec3d other) {
        return subtract(other).length();
    }

    public double distanceSquaredTo(Vec3d other) {
        return subtract(other).lengthSquared();
    }

    public double horizontalDistanceTo(Vec3d other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double horizontalDistanceSquaredTo(Vec3d other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return dx * dx + dz * dz;
    }

    public double dot(Vec3d other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public boolean isZero() {
        return x == 0.0 && y == 0.0 && z == 0.0;
    }

    public double floorX() {
        return Math.floor(x);
    }

    public double floorY() {
        return Math.floor(y);
    }

    public double floorZ() {
        return Math.floor(z);
    }
}
