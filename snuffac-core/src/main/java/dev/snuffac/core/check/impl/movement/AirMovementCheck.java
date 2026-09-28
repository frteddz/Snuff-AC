package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.physics.MovementConstants;
import dev.snuffac.core.prediction.PredictionEngine;
import dev.snuffac.core.prediction.PredictionResult;
import java.util.Map;

public final class AirMovementCheck extends AbstractMovementCheck {

    private static final double MIN_EXCESS = 0.02;

    @Override
    public String key() {
        return "airmovement";
    }

    @Override
    public String name() {
        return "AirMovement";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Compares airborne movement against predicted kinematics instead of a fixed speed cap.";
    }

    @Override
    public Object createState() {
        return new AirState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (AirState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();
        var cache = player.worldCache();

        if (movement.onGround() || movement.ticksSinceGround() < 2) {
            state.excessTicks = 0;
            return;
        }
        if (movement.flying() || movement.gliding() || movement.riding() || movement.inVehicle()
                || movement.inWaterOrLava() || movement.onClimbable()) {
            state.excessTicks = 0;
            return;
        }
        if (!cache.chunkLoaded() || !canPredict(player)) {
            return;
        }

        var environment = environment(context);
        Vec3d actual = movement.delta();
        PredictionResult result = predict(context, actual, environment);

        Vec3d allowed = new Vec3d(
                movement.horizontalCollision() ? 0.35 : 0.06,
                MovementConstants.VERTICAL_LANDING_THRESHOLD + 0.02,
                movement.horizontalCollision() ? 0.35 : 0.06);

        double excessX = Math.max(0.0, Math.abs(result.offset().x()) - allowed.x());
        double excessZ = Math.max(0.0, Math.abs(result.offset().z()) - allowed.z());
        double total = Math.sqrt(excessX * excessX + excessZ * excessZ);

        if (total < MIN_EXCESS) {
            state.excessTicks = 0;
            return;
        }

        state.excessTicks++;
        if (state.excessTicks < 3) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("excess", round(total));
        evidence.put("excessX", round(excessX));
        evidence.put("excessZ", round(excessZ));
        evidence.put("airTicks", movement.ticksSinceGround());
        evidence.put("actualX", round(actual.x()));
        evidence.put("actualZ", round(actual.z()));
        evidence.put("predictedX", round(result.predictedDelta().x()));
        evidence.put("predictedZ", round(result.predictedDelta().z()));
        evidence.put("velocityX", round(movement.velocity().x()));
        evidence.put("velocityZ", round(movement.velocity().z()));
        evidence.put("horizontalCollision", movement.horizontalCollision());
        evidence.put("ice", movement.onIce());

        context.flag("airborne horizontal excess of " + round(total) + " blocks", evidence,
                Math.min(total * 10.0, 12.0));
        state.excessTicks = 0;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (AirState) state(context.player());
        if (state != null && context.player().movement().onGround()) {
            state.excessTicks = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class AirState {

        private int excessTicks;
    }
}
