package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class NoFallCheck extends AbstractMovementCheck {

    private static final int MIN_FALL_TICKS = 3;
    private static final double MAX_LEGIT_FALL_DISTANCE = 3.0;

    @Override
    public String key() {
        return "nofall";
    }

    @Override
    public String name() {
        return "NoFall";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects resetting fall damage by claiming ground contact while the server still sees air.";
    }

    @Override
    public Object createState() {
        return new NoFallState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement)) {
            return;
        }

        var state = (NoFallState) state(context.player());
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

        var player = context.player();
        var movementState = player.movement();
        var cache = player.worldCache();

        if (movementState.ticksSinceTeleport() <= 1 || movementState.inWaterOrLava()
                || movementState.onClimbable() || movementState.gliding() || movementState.flying()
                || movementState.riding() || movementState.inVehicle()) {
            state.reset();
            return;
        }
        if (!cache.chunkLoaded()) {
            return;
        }

        double fallDistance = movementState.fallDistance();

        if (movementState.onGround()) {
            if (state.airTicksBefore > MIN_FALL_TICKS && !cache.onGroundBelow() && fallDistance > MAX_LEGIT_FALL_DISTANCE) {
                Map<String, Object> evidence = context.newEvidence();
                evidence.put("airTicks", state.airTicksBefore);
                evidence.put("fallDistance", round(fallDistance));
                evidence.put("groundBelow", cache.onGroundBelow());
                evidence.put("onGroundClaim", true);
                context.flag("claimed ground after " + state.airTicksBefore + " airborne ticks", evidence, 10.0);
            }
            state.reset();
            return;
        }

        if (!movement.positionChanged() && !movement.rotationChanged()) {
            return;
        }

        state.airTicksBefore = movementState.ticksSinceGround();
        state.airTicks++;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (NoFallState) state(context.player());
        if (state == null) {
            return;
        }
        if (context.player().movement().ticksSinceGround() == 0) {
            state.reset();
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class NoFallState {

        private int airTicksBefore;
        private int airTicks;

        private void reset() {
            airTicksBefore = 0;
            airTicks = 0;
        }
    }
}
