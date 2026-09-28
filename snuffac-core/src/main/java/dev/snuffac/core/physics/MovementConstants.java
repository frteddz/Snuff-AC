package dev.snuffac.core.physics;

public final class MovementConstants {

    public static final double GRAVITY = 0.08;
    public static final double VERTICAL_DRAG = 0.98;
    public static final double AIR_DRAG = 0.91;
    public static final double TERMINAL_VELOCITY = -3.92;
    public static final double JUMP_VELOCITY = 0.42;
    public static final double JUMP_BOOST_PER_LEVEL = 0.1;
    public static final double SPRINT_JUMP_BOOST = 0.2;
    public static final double VERTICAL_MOMENTUM_THRESHOLD = 0.003;
    public static final double VERTICAL_LANDING_THRESHOLD = 0.05;

    public static final double BASE_MOVEMENT_SPEED = 0.1;
    public static final double SPRINT_MULTIPLIER = 1.3;
    public static final double SNEAK_MULTIPLIER = 0.3;
    public static final double USE_ITEM_SLOWDOWN = 0.2;
    public static final double RIDING_INPUT_SCALE = 0.98;
    public static final double INPUT_SCALE = 0.98;
    public static final double DIAGONAL_INPUT_SCALE = 0.98 * Math.sqrt(2.0);

    public static final double DEFAULT_SLIPPERINESS = 0.6;
    public static final double SLIME_SLIPPERINESS = 0.8;
    public static final double ICE_SLIPPERINESS = 0.98;
    public static final double PACKED_ICE_SLIPPERINESS = 0.98;
    public static final double BLUE_ICE_SLIPPERINESS = 0.989;
    public static final double HONEY_SLIPPERINESS = 0.8;
    public static final double SOUL_SAND_SLIPPERINESS = 0.48;

    public static final double AIR_ACCELERATION = 0.02;
    public static final double WATER_DRAG = 0.8;
    public static final double LAVA_DRAG = 0.5;
    public static final double WATER_SURFACE_ACCELERATION = 0.02;
    public static final double LAVA_ACCELERATION = 0.02;
    public static final double UNDERWATER_DRAG = 0.8;
    public static final double SWIM_UP_ACCELERATION = 0.04;
    public static final double HONEY_FALL_SPEED = -0.05;
    public static final double HONEY_ACCELERATION_SCALE = 0.8;

    public static final double STEP_HEIGHT_DEFAULT = 0.6;
    public static final double SAFE_FALL_DISTANCE_DEFAULT = 3.0;
    public static final double FALL_DAMAGE_DISTANCE_DEFAULT = 3.0;
    public static final double PLAYER_WIDTH = 0.6;
    public static final double PLAYER_HEIGHT = 1.8;
    public static final double PLAYER_EYE_HEIGHT = 1.62;
    public static final double SNEAKING_HEIGHT = 1.5;
    public static final double PLAYER_SNEAKING_EYE_HEIGHT = 1.27;

    public static final double SPEED_EFFECT_PER_LEVEL = 0.2;
    public static final double SLOWNESS_PER_LEVEL = 0.15;
    public static final double SLOW_FALLING_GRAVITY = 0.01;
    public static final double LEVITATION_VERTICAL_SPEED = 0.05;

    public static final double ELYTRA_DRAG = 0.99;
    public static final double MIN_ELYTRA_SPEED = 0.01;
    public static final double ELYTRA_LIFT_COEFFICIENT = 0.02;

    public static final double EXPLOSION_KNOCKBACK_BASE = 0.3;
    public static final double KNOCKBACK_RESISTANCE_MAX = 1.0;

    public static final double MOVEMENT_EPSILON_LEGACY = 0.03;
    public static final double MOVEMENT_EPSILON_MODERN = 0.0002;

    public static final double POSITION_ERROR_LIMIT = 0.05;
    public static final double MINIMUM_MOVEMENT_EPSILON = 1.0E-9;

    private MovementConstants() {
    }

    public static double movementEpsilon(int protocolVersion) {
        return protocolVersion >= 764 ? MOVEMENT_EPSILON_MODERN : MOVEMENT_EPSILON_LEGACY;
    }
}
