package dev.snuffac.core.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KinematicsTest {

    private static MovementEnvironment sprintingGround() {
        return MovementEnvironment.GROUND.withSprinting(true);
    }

    private static Vec3d accelerateToTerminal(
            MovementEnvironment environment,
            MovementInput input,
            int ticks) {
        Vec3d velocity = Vec3d.ZERO;
        for (int i = 0; i < ticks; i++) {
            Vec3d next = Kinematics.predictHorizontal(velocity, input, environment, MovementAttributes.DEFAULT);
            velocity = new Vec3d(next.x(), velocity.y(), next.z());
        }
        return velocity;
    }

    @Test
    @DisplayName("sprinting on normal ground converges to the known terminal speed")
    void sprintTerminalSpeedOnGround() {
        Vec3d velocity = accelerateToTerminal(sprintingGround(), new MovementInput(1.0, 0.0, false, false), 400);
        assertEquals(0.2806, Math.abs(velocity.z()), 0.0005,
                "sprint terminal speed on default ground should be about 0.2806 blocks per tick");
    }

    @Test
    @DisplayName("walking and sneaking match the documented speeds")
    void walkingAndSneakingSpeeds() {
        Vec3d walk = accelerateToTerminal(
                MovementEnvironment.GROUND, new MovementInput(1.0, 0.0, false, false), 400);
        Vec3d sneak = accelerateToTerminal(
                MovementEnvironment.GROUND.withSneaking(true), new MovementInput(1.0, 0.0, false, false), 400);
        assertEquals(0.2159, Math.abs(walk.z()), 0.0005, "walking should be about 4.317 blocks per second");
        assertEquals(0.0648, Math.abs(sneak.z()), 0.0005, "sneaking should be about 1.295 blocks per second");
    }

    @Test
    @DisplayName("walking is slower than sprinting")
    void walkingIsSlowerThanSprinting() {
        Vec3d walk = accelerateToTerminal(
                MovementEnvironment.GROUND, new MovementInput(1.0, 0.0, false, false), 400);
        Vec3d sprint = accelerateToTerminal(sprintingGround(), new MovementInput(1.0, 0.0, false, false), 400);
        assertTrue(Math.abs(walk.z()) < Math.abs(sprint.z()),
                "walking speed should be lower than sprinting speed");
    }

    @Test
    @DisplayName("sneaking is slower than walking")
    void sneakingIsSlowerThanWalking() {
        Vec3d sneak = accelerateToTerminal(
                MovementEnvironment.GROUND.withSneaking(true), new MovementInput(1.0, 0.0, false, false), 400);
        Vec3d walk = accelerateToTerminal(
                MovementEnvironment.GROUND, new MovementInput(1.0, 0.0, false, false), 400);
        assertTrue(Math.abs(sneak.z()) < Math.abs(walk.z()),
                "sneaking speed should be lower than walking speed");
    }

    @Test
    @DisplayName("flat sprinting on ice builds more slowly than on normal ground")
    void iceAcceleratesMoreSlowly() {
        Vec3d ice = accelerateToTerminal(
                sprintingGround().withSlipperiness(0.98), new MovementInput(1.0, 0.0, false, false), 400);
        Vec3d ground = accelerateToTerminal(sprintingGround(), new MovementInput(1.0, 0.0, false, false), 400);
        assertTrue(Math.abs(ice.z()) < Math.abs(ground.z()),
                "ice has far less acceleration while flat, so the terminal speed is lower");
        assertEquals(0.2702, Math.abs(ice.z()), 0.001);
    }

    @Test
    @DisplayName("ice retains far more momentum between ticks than normal ground")
    void iceRetainsMomentum() {
        double iceDrag = Kinematics.horizontalDrag(
                sprintingGround().withSlipperiness(0.98));
        double groundDrag = Kinematics.horizontalDrag(sprintingGround());
        assertTrue(iceDrag > groundDrag,
                "ice must apply much less drag so speed is carried between jumps");
        assertEquals(0.8918, iceDrag, 1.0E-4);
        assertEquals(0.546, groundDrag, 1.0E-4);
    }

    @Test
    @DisplayName("sprint jumping eventually exceeds flat sprinting, matching the documented mechanic")
    void sprintJumpExceedsFlatSprint() {
        Vec3d sprintJump = accelerateToTerminal(
                MovementEnvironment.AIR.withSprinting(true), new MovementInput(1.0, 0.0, true, false), 400);
        Vec3d flat = accelerateToTerminal(sprintingGround(), new MovementInput(1.0, 0.0, false, false), 400);
        assertTrue(Math.abs(sprintJump.z()) > Math.abs(flat.z()),
                "airborne drag is low enough that continuous sprint jumping is faster than flat sprinting");
    }

    @Test
    @DisplayName("jump input while falling sets the launch velocity")
    void jumpSetsLaunchVelocity() {
        double beforeJump = -0.2;
        double afterJump = Kinematics.applyGravity(
                beforeJump, new MovementInput(0.0, 0.0, true, false),
                MovementEnvironment.AIR, MovementAttributes.DEFAULT);
        assertEquals(MovementConstants.JUMP_VELOCITY, afterJump, 1.0E-9);
    }

    @Test
    @DisplayName("jump boost raises the launch velocity by the documented amount")
    void jumpBoostIncreasesLaunch() {
        double boosted = Kinematics.applyGravity(
                -0.2, new MovementInput(0.0, 0.0, true, false),
                MovementEnvironment.AIR, MovementAttributes.DEFAULT.withJumpStrength(1.2));
        assertTrue(boosted > MovementConstants.JUMP_VELOCITY,
                "higher jump strength should produce a larger launch velocity");
    }

    @Test
    @DisplayName("gravity is applied every airborne tick without input")
    void gravityIsAppliedEachTick() {
        double first = Kinematics.applyGravity(0.0, MovementInput.NONE,
                MovementEnvironment.AIR, MovementAttributes.DEFAULT);
        double second = Kinematics.applyGravity(first, MovementInput.NONE,
                MovementEnvironment.AIR, MovementAttributes.DEFAULT);
        assertTrue(second < first, "vertical velocity should decrease under gravity");
    }

    @Test
    @DisplayName("terminal velocity is never exceeded")
    void terminalVelocityIsClamped() {
        double value = Kinematics.applyGravity(-100.0, MovementInput.NONE,
                MovementEnvironment.AIR, MovementAttributes.DEFAULT);
        assertEquals(MovementConstants.TERMINAL_VELOCITY, value, 1.0E-9);
    }

    @Test
    @DisplayName("empty input produces no horizontal acceleration")
    void emptyInputAcceleratesNothing() {
        Vec3d velocity = new Vec3d(0.3, 0.0, 0.3);
        Vec3d next = Kinematics.predictHorizontal(
                velocity, MovementInput.NONE, MovementEnvironment.GROUND, MovementAttributes.DEFAULT);
        assertEquals(velocity.x() * 0.546, next.x(), 1.0E-9);
        assertEquals(velocity.z() * 0.546, next.z(), 1.0E-9);
    }

    @Test
    @DisplayName("the speed effect increases acceleration")
    void speedEffectIncreasesAcceleration() {
        Vec3d plain = accelerateToTerminal(
                MovementEnvironment.GROUND.withEffects(0, 0, 0), new MovementInput(1.0, 0.0, false, false), 400);
        Vec3d hasted = accelerateToTerminal(
                MovementEnvironment.GROUND.withEffects(0, 2, 0), new MovementInput(1.0, 0.0, false, false), 400);
        assertTrue(Math.abs(hasted.z()) > Math.abs(plain.z()),
                "speed effect should raise the terminal speed");
    }

    @Test
    @DisplayName("the slowness effect reduces acceleration")
    void slownessEffectReducesAcceleration() {
        Vec3d plain = accelerateToTerminal(
                MovementEnvironment.GROUND.withEffects(0, 0, 0), new MovementInput(1.0, 0.0, false, false), 400);
        Vec3d slowed = accelerateToTerminal(
                MovementEnvironment.GROUND.withEffects(0, 0, 2), new MovementInput(1.0, 0.0, false, false), 400);
        assertTrue(Math.abs(slowed.z()) < Math.abs(plain.z()),
                "slowness should lower the terminal speed");
    }
}
