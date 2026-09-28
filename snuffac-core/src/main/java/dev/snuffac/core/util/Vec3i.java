package dev.snuffac.core.util;

import dev.snuffac.api.Vec3d;

public final class Vec3i {

    public final int x;
    public final int y;
    public final int z;

    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Vec3i of(Vec3d vec) {
        return new Vec3i(
                MathUtil.floorInt(vec.x()),
                MathUtil.floorInt(vec.y()),
                MathUtil.floorInt(vec.z()));
    }

    public Vec3d toVec() {
        return new Vec3d(x, y, z);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vec3i vec)) {
            return false;
        }
        return x == vec.x && y == vec.y && z == vec.z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
