package dev.snuffac.core.util;

import dev.snuffac.api.Vec3d;

public final class MathUtil {

    public static final double EPSILON = 1.0E-7;
    public static final double DEGREES_TO_RADIANS = Math.PI / 180.0;
    public static final double RADIANS_TO_DEGREES = 180.0 / Math.PI;

    private MathUtil() {
    }

    public static double square(double value) {
        return value * value;
    }

    public static int floorInt(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    public static int ceilInt(double value) {
        int truncated = (int) value;
        return value > truncated ? truncated + 1 : truncated;
    }

    public static long floorLong(double value) {
        long truncated = (long) value;
        return value < truncated ? truncated - 1L : truncated;
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int clampInt(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clampFloat(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        }
        if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    public static float wrapDegrees(float degrees) {
        return (float) wrapDegrees((double) degrees);
    }

    public static double deltaDegrees(double from, double to) {
        return wrapDegrees(to - from);
    }

    public static double fractionalPart(double value) {
        return value - Math.floor(value);
    }

    public static int floorMod(int value, int modulus) {
        int result = value % modulus;
        return result < 0 ? result + modulus : result;
    }

    public static double interpolate(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    public static boolean between(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public static boolean betweenInclusive(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public static int gcd(int a, int b) {
        int x = Math.abs(a);
        int y = Math.abs(b);
        while (y != 0) {
            int remainder = x % y;
            x = y;
            y = remainder;
        }
        return x;
    }

    public static double gcd(int a, int b, int c) {
        return gcd(gcd(a, b), c);
    }

    public static int gcd(long a, long b) {
        long x = Math.abs(a);
        long y = Math.abs(b);
        while (y != 0) {
            long remainder = x % y;
            x = y;
            y = remainder;
        }
        return (int) x;
    }

    public static double gcd(double a, double b) {
        return gcd((long) a, (long) b);
    }

    public static double horizontalDistance(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static Vec3d horizontalScale(Vec3d vec, double factor) {
        return new Vec3d(vec.x() * factor, vec.y(), vec.z() * factor);
    }

    public static double roundTo(double value, int decimals) {
        double factor = Math.pow(10.0, decimals);
        return Math.round(value * factor) / factor;
    }

    public static double roundToIncrement(double value, double increment) {
        if (increment <= 0.0) {
            return value;
        }
        return Math.round(value / increment) * increment;
    }

    public static double signum(double value) {
        if (value > 0.0) {
            return 1.0;
        }
        return value < 0.0 ? -1.0 : 0.0;
    }

    public static int signumInt(double value) {
        if (value > 0.0) {
            return 1;
        }
        return value < 0.0 ? -1 : 0;
    }

    public static double average(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double total = 0.0;
        for (double value : values) {
            total += value;
        }
        return total / values.length;
    }

    public static double stdDev(double[] values) {
        if (values.length < 2) {
            return 0.0;
        }
        double mean = average(values);
        double sum = 0.0;
        for (double value : values) {
            double delta = value - mean;
            sum += delta * delta;
        }
        return Math.sqrt(sum / (values.length - 1));
    }

    public static int mcd(int a, int b) {
        return a == 0 ? b == 0 ? 0 : 1 : b == 0 ? 1 : a == b ? 0 : 2;
    }

    public static int mcd(long a, long b) {
        return mcd((int) a, (int) b);
    }
}
