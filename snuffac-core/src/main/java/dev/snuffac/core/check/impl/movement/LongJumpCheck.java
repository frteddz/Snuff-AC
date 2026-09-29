package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class LongJumpCheck extends AbstractMovementCheck {

    private static final double MAX_AIR_DISTANCE = 12.5;
    private static final int MIN_AIR_TICKS = 3;

    @Override
    public String key() {
        return "longjump";
    }

    @Override
    public String name() {
        return "LongJump";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects airborne horizontal distance beyond what a full sprint jump can cover.";
    }

    @Override
    public Object createState() {
        return new LongJumpState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (LongJumpState) state(context.player());
        if (state == null) {
            return;
        }

        var graceMovement = context.player().movement();
        if (dev.snuffac.core.prediction.PredictionGraces.windChargeActive(graceMovement)) {
            return;
        }

        var movement = movement(context);
        if (movement.onGround() || movement.ticksSinceGround() < MIN_AIR_TICKS) {
            state.reset();
            return;
        }
        if (movement.ticksSinceTeleport() <= 1 || movement.flying() || movement.gliding()
                || movement.riding() || movement.inVehicle() || movement.inWaterOrLava()
                || movement.onClimbable() || movement.horizontalCollision()) {
            return;
        }

        state.distance += movement.delta().horizontalLength();
        state.ticks++;

        if (movement.ticksSinceGround() < 24 || state.distance <= MAX_AIR_DISTANCE) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("distance", round(state.distance));
        evidence.put("maximum", MAX_AIR_DISTANCE);
        evidence.put("ticks", state.ticks);
        evidence.put("airTicks", movement.ticksSinceGround());
        evidence.put("velocity", round(movement.velocity().horizontalLength()));
        context.flag("airborne distance of " + round(state.distance) + " blocks", evidence, 8.0);
        state.reset();
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (LongJumpState) state(context.player());
        if (state != null && movement(context).ticksSinceGround() == 0) {
            state.reset();
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class LongJumpState {

        private double distance;
        private int ticks;

        private void reset() {
            distance = 0.0;
            ticks = 0;
        }
    }
}
