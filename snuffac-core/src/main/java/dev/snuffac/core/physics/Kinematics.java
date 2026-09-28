package dev.snuffac.core.physics;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.MathUtil;

public final class Kinematics {

    private Kinematics() {
    }

    public static double horizontalDrag(MovementEnvironment environment) {
        if (environment.inWater()) {
            return MovementConstants.WATER_DRAG;
        }
        if (environment.inLava()) {
            return MovementConstants.LAVA_DRAG;
        }
        if (environment.onClimbable() && !environment.onGround()) {
            return 0.6;
        }
        if (environment.onGround()) {
            return environment.slipperiness() * MovementConstants.AIR_DRAG;
        }
        return MovementConstants.AIR_DRAG;
    }

    public static double horizontalAcceleration(MovementInput input, MovementEnvironment environment) {
        double effects = environment.effectsMultiplier();
        double speed = movementMultiplier(input, environment);
        if (input.empty()) {
            return 0.0;
        }
        if (environment.inWater()) {
            return MovementConstants.WATER_SURFACE_ACCELERATION * speed * effects;
        }
        if (environment.inLava()) {
            return MovementConstants.LAVA_ACCELERATION * speed * effects;
        }
        if (environment.onClimbable() && !environment.onGround()) {
            return 0.15 * speed;
        }
        if (environment.onGround()) {
            double slipperiness = Math.max(environment.slipperiness(), 0.05);
            double factor = MovementConstants.DEFAULT_SLIPPERINESS / slipperiness;
            return MovementConstants.BASE_MOVEMENT_SPEED * speed * effects * factor * factor * factor;
        }
        return MovementConstants.AIR_ACCELERATION * speed;
    }

    public static double movementMultiplier(MovementInput input, MovementEnvironment environment) {
        double stateMultiplier;
        if (environment.sneaking()) {
            stateMultiplier = MovementConstants.SNEAK_MULTIPLIER;
        } else if (environment.sprinting()) {
            stateMultiplier = MovementConstants.SPRINT_MULTIPLIER;
        } else {
            stateMultiplier = 1.0;
        }
        if (environment.usingItem()) {
            stateMultiplier *= MovementConstants.USE_ITEM_SLOWDOWN;
        }
        return stateMultiplier * input.inputScale();
    }

    public static Vec3d predictHorizontal(
            Vec3d velocity,
            MovementInput input,
            MovementEnvironment environment,
            MovementAttributes attributes) {

        double drag = horizontalDrag(environment);
        double acceleration = horizontalAcceleration(input, environment);

        double accelX = input.direction().x() * acceleration;
        double accelZ = input.direction().z() * acceleration;

        double newX = velocity.x() * drag + accelX;
        double newZ = velocity.z() * drag + accelZ;

        if (environment.riding()) {
            newX = MathUtil.roundToIncrement(newX, MovementConstants.MINIMUM_MOVEMENT_EPSILON);
            newZ = MathUtil.roundToIncrement(newZ, MovementConstants.MINIMUM_MOVEMENT_EPSILON);
        }
        return new Vec3d(newX, velocity.y(), newZ);
    }

    public static double predictVerticalY(
            double currentY,
            MovementInput input,
            MovementEnvironment environment,
            MovementAttributes attributes) {

        if (environment.flying()) {
            return 0.0;
        }
        if (environment.gliding()) {
            return predictGlideY(currentY, environment);
        }
        if (environment.onClimbable() && !environment.onGround() && !environment.inWaterOrLava()) {
            return climbVelocity(input);
        }
        if (environment.inWater() || environment.inLava()) {
            return predictFluidY(currentY, input, environment, attributes);
        }
        if (environment.onHoney() && currentY < 0.0) {
            return MovementConstants.HONEY_FALL_SPEED;
        }
        if (environment.onGround()) {
            return groundedVertical(currentY, input, environment, attributes);
        }
        if (environment.levitating()) {
            return levitationVelocity(currentY, input);
        }
        return applyGravity(currentY, input, environment, attributes);
    }

    public static double applyGravity(
            double currentY,
            MovementInput input,
            MovementEnvironment environment,
            MovementAttributes attributes) {

        double value = currentY - attributes.effectiveGravity();
        value *= MovementConstants.VERTICAL_DRAG;
        if (input.jump()) {
            double jumpPower = MovementConstants.JUMP_VELOCITY
                    * attributes.jumpStrength()
                    + MovementConstants.JUMP_BOOST_PER_LEVEL * environment.jumpBoostLevel()
                    + (environment.sprinting() ? MovementConstants.SPRINT_JUMP_BOOST : 0.0);
            if (value < 0.0) {
                value = jumpPower;
            }
        }
        if (environment.slowFalling()) {
            value = Math.max(value, -MovementConstants.SLOW_FALLING_GRAVITY * 4.0);
        }
        if (value < MovementConstants.TERMINAL_VELOCITY) {
            value = MovementConstants.TERMINAL_VELOCITY;
        }
        return value;
    }

    private static double groundedVertical(
            double currentY,
            MovementInput input,
            MovementEnvironment environment,
            MovementAttributes attributes) {
        if (input.jump()) {
            double jumpPower = MovementConstants.JUMP_VELOCITY
                    * attributes.jumpStrength()
                    + MovementConstants.JUMP_BOOST_PER_LEVEL * environment.jumpBoostLevel()
                    + (environment.sprinting() ? MovementConstants.SPRINT_JUMP_BOOST : 0.0);
            return jumpPower * MovementConstants.VERTICAL_DRAG;
        }
        return 0.0;
    }

    private static double predictGlideY(double currentY, MovementEnvironment environment) {
        return currentY * MovementConstants.ELYTRA_DRAG;
    }

    private static double climbVelocity(MovementInput input) {
        if (input.forward() > 0.0) {
            return 0.2;
        }
        if (input.forward() < 0.0) {
            return -0.15;
        }
        return 0.0;
    }

    private static double predictFluidY(
            double currentY,
            MovementInput input,
            MovementEnvironment environment,
            MovementAttributes attributes) {

        boolean lava = environment.inLava();
        double drag = lava ? MovementConstants.LAVA_DRAG : MovementConstants.WATER_DRAG;
        double drift = (currentY - attributes.effectiveGravity()) * drag;

        if (lava) {
            if (input.jump() && currentY <= 0.0) {
                return currentY + MovementConstants.SWIM_UP_ACCELERATION;
            }
            return drift;
        }

        if (input.jump()) {
            if (currentY < MovementConstants.SWIM_UP_ACCELERATION) {
                return currentY + MovementConstants.SWIM_UP_ACCELERATION;
            }
            return currentY - MovementConstants.SWIM_UP_ACCELERATION;
        }
        if (input.flip() && currentY > -MovementConstants.SWIM_UP_ACCELERATION) {
            return currentY - MovementConstants.SWIM_UP_ACCELERATION;
        }
        return drift;
    }

    private static double levitationVelocity(double currentY, MovementInput input) {
        if (input.jump()) {
            return currentY + MovementConstants.LEVITATION_VERTICAL_SPEED;
        }
        if (input.flip()) {
            return currentY - MovementConstants.LEVITATION_VERTICAL_SPEED;
        }
        return currentY;
    }

    public static Vec3d applyGravityAndDrag(Vec3d velocity, MovementAttributes attributes) {
        return new Vec3d(
                velocity.x() * MovementConstants.AIR_DRAG,
                applyGravity(velocity.y(), MovementInput.NONE, MovementEnvironment.AIR, attributes),
                velocity.z() * MovementConstants.AIR_DRAG);
    }

    public static boolean isLandingVelocity(double velocityY) {
        return velocityY < -MovementConstants.VERTICAL_MOMENTUM_THRESHOLD
                && velocityY > MovementConstants.TERMINAL_VELOCITY;
    }

    public static double clampedMovement(double value, double epsilon) {
        if (Math.abs(value) < epsilon) {
            return 0.0;
        }
        return value;
    }
}
