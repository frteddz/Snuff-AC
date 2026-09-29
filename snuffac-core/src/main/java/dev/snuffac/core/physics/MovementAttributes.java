package dev.snuffac.core.physics;

public record MovementAttributes(
        double movementSpeed,
        double gravity,
        double jumpStrength,
        double stepHeight,
        double safeFallDistance,
        double scale,
        double sneakingSpeed,
        double waterMovementEfficiency
) {

    public static final double SERVER_JUMP_STRENGTH = 0.42;

    public static final MovementAttributes DEFAULT = new MovementAttributes(
            MovementConstants.BASE_MOVEMENT_SPEED,
            1.0,
            1.0,
            MovementConstants.STEP_HEIGHT_DEFAULT,
            MovementConstants.SAFE_FALL_DISTANCE_DEFAULT,
            1.0,
            1.0,
            1.0);

    public static double normaliseJumpStrength(double serverAttributeValue) {
        if (serverAttributeValue <= 0.0) {
            return 1.0;
        }
        return serverAttributeValue / SERVER_JUMP_STRENGTH;
    }

    public double effectiveGravity() {
        return MovementConstants.GRAVITY * gravity;
    }

    public MovementAttributes withMovementSpeed(double speed) {
        return new MovementAttributes(speed, gravity, jumpStrength, stepHeight, safeFallDistance, scale, sneakingSpeed, waterMovementEfficiency);
    }

    public MovementAttributes withJumpStrength(double strength) {
        return new MovementAttributes(movementSpeed, gravity, strength, stepHeight, safeFallDistance, scale, sneakingSpeed, waterMovementEfficiency);
    }

    public MovementAttributes withGravity(double newGravity) {
        return new MovementAttributes(movementSpeed, newGravity, jumpStrength, stepHeight, safeFallDistance, scale, sneakingSpeed, waterMovementEfficiency);
    }

    public MovementAttributes withStepHeight(double height) {
        return new MovementAttributes(movementSpeed, gravity, jumpStrength, height, safeFallDistance, scale, sneakingSpeed, waterMovementEfficiency);
    }
}
