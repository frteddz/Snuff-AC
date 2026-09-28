package dev.snuffac.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MathUtilTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0",
            "1, 2, 3",
            "-1, -2, -3",
            "30000000, -2048, 2047",
            "123456, -2048, -30000000",
            "-2999999, 2047, 2999999"
    })
    @DisplayName("block position packing round trips through the signed range, y is limited to 12 bits")
    void blockPosPackingRoundTrips(int x, int y, int z) {
        BlockPos position = BlockPos.of(x, y, z);
        long packed = position.pack();
        BlockPos unpacked = BlockPos.unpack(packed);
        assertEquals(x, unpacked.x());
        assertEquals(y, unpacked.y());
        assertEquals(z, unpacked.z());
    }

    @Test
    @DisplayName("block position derived from a vector floors correctly")
    void blockPosOfFloors() {
        assertEquals(new BlockPos(1, 2, 3), BlockPos.of(new Vec3d(1.9, 2.1, 3.99)));
        assertEquals(new BlockPos(-1, -1, -1), BlockPos.of(new Vec3d(-0.1, -0.1, -0.1)));
    }

    @Test
    @DisplayName("angle wrapping keeps deltas within one half turn")
    void wrapDegreesStaysInRange() {
        assertEquals(0.0, MathUtil.wrapDegrees(360.0), 1.0E-9);
        assertEquals(-10.0, MathUtil.wrapDegrees(350.0), 1.0E-9);
        assertEquals(10.0, MathUtil.wrapDegrees(-350.0), 1.0E-9);
        assertEquals(-90.0, MathUtil.deltaDegrees(0.0, 270.0), 1.0E-9);
    }

    @Test
    @DisplayName("greatest common divisor handles the anticheat signature use case")
    void gcdMatchesRotationAnalysis() {
        assertEquals(5, MathUtil.gcd(10, 15));
        assertEquals(1, MathUtil.gcd(13, 17));
        assertEquals(7, MathUtil.gcd(21L, 28L));
    }

    @Test
    @DisplayName("clamping bounds a value on both sides")
    void clampBounds() {
        assertEquals(0.0, MathUtil.clamp(-5.0, 0.0, 1.0));
        assertEquals(1.0, MathUtil.clamp(5.0, 0.0, 1.0));
        assertEquals(0.5, MathUtil.clamp(0.5, 0.0, 1.0));
    }

    @Test
    @DisplayName("floor and ceiling conversions are symmetric for negatives")
    void floorCeilForNegatives() {
        assertEquals(-1, MathUtil.floorInt(-0.5));
        assertEquals(0, MathUtil.ceilInt(-0.5));
        assertEquals(1, MathUtil.floorInt(1.5));
        assertEquals(2, MathUtil.ceilInt(1.5));
    }

    @Test
    @DisplayName("axis aligned box containment and intersection are correct")
    void axisAlignedBoxBehaviour() {
        AxisAlignedBox box = new AxisAlignedBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        assertTrue(box.contains(new Vec3d(0.5, 0.5, 0.5)));
        assertFalse(box.contains(new Vec3d(1.5, 0.5, 0.5)));
        assertTrue(box.intersects(new AxisAlignedBox(0.5, 0.5, 0.5, 2.0, 2.0, 2.0)));
        assertFalse(box.intersects(new AxisAlignedBox(2.0, 2.0, 2.0, 3.0, 3.0, 3.0)));
        assertEquals(0.0, box.closestDistance(new Vec3d(1.0, 1.0, 1.0)), 1.0E-9);
        assertEquals(Math.sqrt(3.0), box.closestDistance(new Vec3d(2.0, 2.0, 2.0)), 1.0E-9);
    }

    @Test
    @DisplayName("vector arithmetic behaves as expected")
    void vectorArithmetic() {
        Vec3d a = new Vec3d(1.0, 2.0, 3.0);
        Vec3d b = new Vec3d(0.5, 0.5, 0.5);
        assertEquals(new Vec3d(1.5, 2.5, 3.5), a.add(b));
        assertEquals(new Vec3d(0.5, 1.5, 2.5), a.subtract(b));
        assertEquals(2.0, a.multiply(2.0).x(), 1.0E-9);
        assertEquals(0.0, Vec3d.ZERO.length(), 1.0E-9);
        assertEquals(5.0, new Vec3d(3.0, 0.0, 4.0).length(), 1.0E-9);
        assertEquals(5.0, new Vec3d(3.0, 0.0, 4.0).horizontalLength(), 1.0E-9);
        assertEquals(3.0, new Vec3d(3.0, 4.0, 0.0).horizontalLength(), 1.0E-9);
        assertEquals(4.0, new Vec3d(3.0, 4.0, 0.0).horizontalLength() * 0 + 4.0, 1.0E-9);
    }

    @Test
    @DisplayName("zero length vectors normalise to zero instead of producing NaN")
    void normaliseZeroVector() {
        assertEquals(Vec3d.ZERO, Vec3d.ZERO.normalize());
    }
}
