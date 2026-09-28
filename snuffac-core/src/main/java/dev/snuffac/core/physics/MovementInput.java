package dev.snuffac.core.physics;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.MathUtil;

public record MovementInput(double forward, double strafe, boolean jump, boolean flip) {

    public static final MovementInput NONE = new MovementInput(0.0, 0.0, false, false);

    private static final double IMPULSE_SCALE = 0.98;
    private static final double JUMP_FLIP_SCALE = 0.98;

    public MovementInput negated() {
        return new MovementInput(-forward, -strafe, jump, !flip);
    }

    public MovementInput withoutFlip() {
        return new MovementInput(forward, strafe, jump, false);
    }

    public boolean empty() {
        return forward == 0.0 && strafe == 0.0;
    }

    public double rawMagnitude() {
        return Math.sqrt(forward * forward + strafe * strafe);
    }

    public double inputScale() {
        double magnitude = rawMagnitude();
        if (magnitude < MathUtil.EPSILON) {
            return 0.0;
        }
        if (flip) {
            return magnitude * JUMP_FLIP_SCALE;
        }
        if (magnitude > 1.0) {
            return IMPULSE_SCALE;
        }
        return magnitude * IMPULSE_SCALE;
    }

    public Vec3d direction() {
        double magnitude = rawMagnitude();
        if (magnitude < MathUtil.EPSILON) {
            return Vec3d.ZERO;
        }
        return new Vec3d(strafe / magnitude, 0.0, forward / magnitude);
    }

    public Vec3d facing() {
        return direction();
    }
}
