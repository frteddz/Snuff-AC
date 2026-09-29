package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.physics.MovementConstants;
import java.util.Map;

public final class HighJumpCheck extends AbstractMovementCheck {

    public static final double MIN_EXCESS = 0.08;
    public static final int REQUIRED_CONSECUTIVE = 3;

    @Override
    public String key() {
        return "highjump";
    }

    @Override
    public String name() {
        return "HighJump";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects launch velocities above the maximum produced by jumping with the current effects.";
    }

    @Override
    public Object createState() {
        return new JumpState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (JumpState) state(context.player());
        if (state == null) {
            return;
        }

        var graceMovement = context.player().movement();
        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                context.player(), context.tps(), context.ping())) {
            return;
        }
        if (dev.snuffac.core.prediction.PredictionGraces.windChargeActive(graceMovement)) {
            return;
        }

        var movement = movement(context);
        if (movement.ticksSinceTeleport() <= 1 || movement.riding() || movement.inVehicle()
                || movement.inWaterOrLava() || movement.onClimbable() || movement.gliding() || movement.flying()) {
            state.excessTicks = 0;
            return;
        }
        if (movement.onGround() || movement.ticksSinceGround() > 1) {
            state.excessTicks = 0;
            return;
        }

        double maximum = maximumLaunch(movement.jumpBoostLevel(), movement.sprinting(),
                movement.attributes().jumpStrength());
        double observed = movement.delta().y();
        double excess = observed - maximum;

        if (excess < MIN_EXCESS) {
            state.badJumps = 0;
            return;
        }

        state.badJumps++;
        if (state.badJumps < REQUIRED_CONSECUTIVE) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("excess", round(excess));
        evidence.put("observed", round(observed));
        evidence.put("maximum", round(maximum));
        evidence.put("jumpBoost", movement.jumpBoostLevel());
        evidence.put("sprinting", movement.sprinting());
        evidence.put("airTicks", movement.ticksSinceGround());
        evidence.put("jumpStrength", round(movement.attributes().jumpStrength()));
        evidence.put("tolerance", MIN_EXCESS);
        evidence.put("consecutiveBadJumps", state.badJumps);
        context.flag("launch velocity " + round(observed) + " above maximum " + round(maximum)
                + " on " + state.badJumps + " consecutive jumps", evidence, 10.0);
        state.badJumps = 0;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (JumpState) state(context.player());
        if (state != null && movement(context).onGround()) {
            state.excessTicks = 0;
        }
    }

    static double maximumLaunch(int jumpBoost, boolean sprinting, double jumpStrength) {
        double value = MovementConstants.JUMP_VELOCITY * jumpStrength
                + MovementConstants.JUMP_BOOST_PER_LEVEL * jumpBoost
                + (sprinting ? MovementConstants.SPRINT_JUMP_BOOST : 0.0);
        return value * MovementConstants.VERTICAL_DRAG;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class JumpState {

        private int excessTicks;
        private int badJumps;
    }
}
