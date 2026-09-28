package dev.snuffac.core.util;

import dev.snuffac.api.Vec3d;

public record BlockPos(int x, int y, int z) {

    public static BlockPos of(Vec3d vec) {
        return new BlockPos((int) Math.floor(vec.x()), (int) Math.floor(vec.y()), (int) Math.floor(vec.z()));
    }

    public static BlockPos of(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    public static BlockPos unpack(long packed) {
        return new BlockPos(unpackX(packed), unpackY(packed), unpackZ(packed));
    }

    public static long pack(int x, int y, int z) {
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed << 0 >> 38);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    public long pack() {
        return pack(x, y, z);
    }

    public Vec3d toVec() {
        return new Vec3d(x, y, z);
    }

    public Vec3d center() {
        return new Vec3d(x + 0.5, y + 0.5, z + 0.5);
    }

    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }

    public BlockPos relative(BlockFace face) {
        return offset(face.stepX(), face.stepY(), face.stepZ());
    }

    public double distanceTo(Vec3d other) {
        double dx = x + 0.5 - other.x();
        double dy = y + 0.5 - other.y();
        double dz = z + 0.5 - other.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double horizontalDistanceTo(Vec3d other) {
        double dx = x + 0.5 - other.x();
        double dz = z + 0.5 - other.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
