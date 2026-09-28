package dev.snuffac.core.physics;

public record MovementEnvironment(
        boolean onGround,
        boolean inWater,
        boolean inLava,
        boolean inWaterOrLava,
        boolean submerged,
        boolean onClimbable,
        boolean onHoney,
        boolean onIce,
        boolean onSlime,
        boolean onSoulSand,
        boolean gliding,
        boolean flying,
        boolean riding,
        boolean sneaking,
        boolean sprinting,
        boolean usingItem,
        boolean onGroundFromClient,
        boolean horizontalCollision,
        boolean levitating,
        boolean slowFalling,
        boolean riptiding,
        int jumpBoostLevel,
        int speedLevel,
        int slownessLevel,
        double slipperiness,
        double blockFriction,
        double fallDistance,
        int poseHeight
) {

    public static final MovementEnvironment GROUND = new MovementEnvironment(
            true, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, true, false, false, false, false,
            0, 0, 0,
            MovementConstants.DEFAULT_SLIPPERINESS,
            MovementConstants.DEFAULT_SLIPPERINESS,
            0.0,
            0);

    public static final MovementEnvironment AIR = GROUND.withOnGround(false);

    public MovementEnvironment withOnGround(boolean value) {
        return new MovementEnvironment(
                value, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withClientGround(boolean value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, sprinting, usingItem, value, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withSlipperiness(double value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                value, value, fallDistance, poseHeight);
    }

    public MovementEnvironment withSneaking(boolean value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, value, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withSprinting(boolean value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, value, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withUsingItem(boolean value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, sprinting, value, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withWater(boolean value) {
        return new MovementEnvironment(
                onGround, value, inLava, value || inLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withFallDistance(double value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, value, poseHeight);
    }

    public MovementEnvironment withEffects(int jump, int speed, int slowness) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, riding, sneaking, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jump, speed, slowness,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withGliding(boolean value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                value, flying, riding, sneaking, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public MovementEnvironment withRiding(boolean value) {
        return new MovementEnvironment(
                onGround, inWater, inLava, inWaterOrLava, submerged, onClimbable, onHoney, onIce, onSlime, onSoulSand,
                gliding, flying, value, sneaking, sprinting, usingItem, onGroundFromClient, horizontalCollision,
                levitating, slowFalling, riptiding, jumpBoostLevel, speedLevel, slownessLevel,
                slipperiness, blockFriction, fallDistance, poseHeight);
    }

    public double effectsMultiplier() {
        double value = (1.0 + MovementConstants.SPEED_EFFECT_PER_LEVEL * speedLevel)
                * (1.0 - MovementConstants.SLOWNESS_PER_LEVEL * slownessLevel);
        return Math.max(value, 0.0);
    }

    public boolean isSubmergedOrInFluid() {
        return inWater || inLava;
    }

    public boolean canFly() {
        return flying || gliding;
    }
}
