package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class VehicleCheck extends AbstractMovementCheck {

    public static final int REQUIRED_TICKS = 8;
    public static final double MAX_AVERAGE = 1.6;
    public static final double MAX_PEAK = 2.4;
    public static final double MAX_RISE = 1.5;
    public static final double MARGIN = 0.25;

    @Override
    public String key() {
        return "vehicle";
    }

    @Override
    public String name() {
        return "Vehicle";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Limits boat, minecart and horse movement to the vanilla figures while the player "
                + "is riding, which is the BoatFly and vehicle hack.";
    }

    @Override
    public Object createState() {
        return new VehicleState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (VehicleState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movementState = player.movement();

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (movementState.ticksSinceTeleport() <= 2 || movementState.pendingSetback()) {
            state.reset();
            return;
        }
        if (!movementState.inVehicle() && !movementState.riding()) {
            state.reset();
            return;
        }

        double horizontal = movementState.delta().horizontalLength();
        if (horizontal < 0.01) {
            return;
        }
        state.horizontalSum += horizontal;
        state.horizontalTicks++;
        state.peak = Math.max(state.peak, horizontal);
        if (state.horizontalTicks < REQUIRED_TICKS) {
            return;
        }
        double average = state.horizontalSum / state.horizontalTicks;

        boolean tooFast = average > MAX_AVERAGE + MARGIN || state.peak > MAX_PEAK + MARGIN;
        double rise = movementState.delta().y();

        if (!tooFast && rise <= MAX_RISE) {
            state.cleanTicks++;
            if (state.cleanTicks >= REQUIRED_TICKS) {
                state.forget();
            }
            return;
        }

        state.overTicks++;
        if (state.overTicks < REQUIRED_TICKS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("averageHorizontal", round(average));
        evidence.put("peakHorizontal", round(state.peak));
        evidence.put("maxAverage", MAX_AVERAGE);
        evidence.put("maxPeak", MAX_PEAK);
        evidence.put("rise", round(rise));
        evidence.put("maxRise", MAX_RISE);
        evidence.put("ticks", state.horizontalTicks);
        evidence.put("inVehicle", movementState.inVehicle());
        evidence.put("ticksSinceVehicle", movementState.ticksSinceVehicle());
        evidence.put("onClimbable", movementState.onClimbable());

        context.requestSetback("vehicle moving faster than vanilla allows");
        context.flag("vehicle carried the player at " + round(average) + " blocks per tick, "
                + "peak " + round(state.peak) + ", the vanilla figures are "
                + MAX_AVERAGE + " and " + MAX_PEAK, evidence, 9.0);
        state.forget();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class VehicleState {

        private int horizontalTicks;
        private int overTicks;
        private int cleanTicks;
        private double horizontalSum;
        private double peak;

        private void reset() {
            horizontalTicks = 0;
            overTicks = 0;
            cleanTicks = 0;
            horizontalSum = 0.0;
            peak = 0.0;
        }

        private void forget() {
            reset();
        }
    }
}
