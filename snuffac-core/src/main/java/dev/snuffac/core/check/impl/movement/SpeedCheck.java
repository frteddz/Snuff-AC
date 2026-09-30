package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.prediction.PredictionResult;
import java.util.Map;

public final class SpeedCheck extends AbstractMovementCheck {

    private static final double MIN_EXCESS = 0.005;
    private static final double LENIENCY_CARRY_THRESHOLD = 0.05;
    public static final double CREEP_FLOOR = 0.004;
    public static final double CREEP_CEILING = 0.06;
    public static final int CREEP_WINDOW = 100;
    public static final double CREEP_FILL = 1.0;
    public static final double CREEP_DECAY = 0.06;

    @Override
    public String key() {
        return "speed";
    }

    @Override
    public String name() {
        return "Speed";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Prediction based speed detection that enumerates client input and forgives known "
                + "external influences, plus an accumulator that catches a client that is only "
                + "slightly fast for a long time.";
    }

    @Override
    public Object createState() {
        return new SpeedState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (SpeedState) state(context.player());
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
        var movement = player.movement();
        var toleranceModel = tolerance(context);

        if (!canPredict(player)) {
            movement.velocity(Vec3d.ZERO);
            state.lastOffset = Vec3d.ZERO;
            return;
        }

        var environment = environment(context);
        Vec3d actualDelta = movement.delta();
        PredictionResult result = predict(context, actualDelta, environment);

        Vec3d forgiven = toleranceModel.total();
        Vec3d offset = result.offset();
        Vec3d residual = dev.snuffac.core.tolerance.ToleranceModel.clampToBox(
                offset, toleranceModel.toleranceBox());
        double residualLength = residual.length();

        applyPrediction(player, result);
        movement.clientVelocity(actualDelta);
        state.lastOffset = residual;
        toleranceModel.consumeCarryOver();

        if (accountCreep(context, state, residualLength)) {
            return;
        }

        if (residualLength < MIN_EXCESS) {
            return;
        }

        double tolerance = context.config().toleranceFor(context.ping(), context.tps(), 1.0);
        double excess = residualLength - tolerance;
        if (excess <= 0.0) {
            state.excessTicks = 0;
            return;
        }

        state.excessTicks++;
        if (state.excessTicks < 2) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("excess", round(excess));
        evidence.put("tolerance", round(tolerance));
        evidence.put("offsetX", round(residual.x()));
        evidence.put("offsetY", round(residual.y()));
        evidence.put("offsetZ", round(residual.z()));
        evidence.put("predictedX", round(result.predictedDelta().x()));
        evidence.put("predictedZ", round(result.predictedDelta().z()));
        evidence.put("actualX", round(actualDelta.x()));
        evidence.put("actualZ", round(actualDelta.z()));
        evidence.put("forgiven", round(forgiven.length()));
        evidence.put("slipperiness", round(environment.slipperiness()));
        evidence.put("onGround", environment.onGround());
        evidence.put("sprinting", environment.sprinting());
        evidence.put("excessTicks", state.excessTicks);

        double weight = Math.min(excess * 12.0, 12.0);
        context.requestSetback("speed excess of " + round(excess) + " blocks");
        context.flag("speed excess of " + round(excess) + " blocks", evidence, weight);

        if (residualLength > LENIENCY_CARRY_THRESHOLD) {
            toleranceModel.beginLeniencyCarryOver(residual);
        }
        state.excessTicks = 0;
    }

    private boolean accountCreep(CheckContext context, SpeedState state, double residualLength) {
        if (residualLength < CREEP_FLOOR) {
            state.creepDecay++;
            if (state.creepDecay >= CREEP_DECAY_TICKS) {
                state.creep = Math.max(0.0, state.creep - CREEP_DECAY);
                state.creepWindow = 0;
                state.creepDecay = 0;
            }
            return false;
        }

        if (residualLength > CREEP_CEILING) {
            state.creep = 0.0;
            state.creepWindow = 0;
            state.creepDecay = 0;
            return false;
        }

        state.creepDecay = 0;
        state.creepWindow++;
        if (state.creepWindow > CREEP_WINDOW) {
            state.creepWindow = CREEP_WINDOW;
        }
        state.creep += CREEP_FILL * (residualLength - CREEP_FLOOR) / (CREEP_CEILING - CREEP_FLOOR);

        if (state.creep < CREEP_FILL * 12.0 || state.creepWindow < CREEP_WINDOW / 2) {
            return false;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "creep");
        evidence.put("accumulated", round(state.creep));
        evidence.put("residual", round(residualLength));
        evidence.put("floor", CREEP_FLOOR);
        evidence.put("ceiling", CREEP_CEILING);
        evidence.put("window", state.creepWindow);
        evidence.put("ping", round(context.ping()));
        evidence.put("tps", round(context.tps()));

        context.requestSetback("sustained small speed excess of " + round(residualLength) + " blocks");
        context.flag("sustained small speed excess of " + round(residualLength)
                + " blocks per tick over " + state.creepWindow + " ticks", evidence, 9.0);
        state.creep = 0.0;
        state.creepWindow = 0;
        return true;
    }

    private static final int CREEP_DECAY_TICKS = 4;

    @Override
    public void onTick(CheckContext context) {
        var state = (SpeedState) state(context.player());
        if (state != null && context.player().movement().onGround()) {
            state.excessTicks = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class SpeedState {

        private int excessTicks;
        private Vec3d lastOffset = Vec3d.ZERO;
        private double creep;
        private int creepWindow;
        private int creepDecay;
    }
}
