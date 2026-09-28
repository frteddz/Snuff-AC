package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.physics.MovementConstants;
import java.util.Map;

public final class StepCheck extends AbstractMovementCheck {

    private static final double MIN_EXCESS = 0.01;

    @Override
    public String key() {
        return "step";
    }

    @Override
    public String name() {
        return "Step";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects vertical gains above the player's step height without a valid cause.";
    }

    @Override
    public Object createState() {
        return new StepState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (StepState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();

        if (!movement.onGround() || movement.ticksSinceTeleport() <= 1
                || movement.riding() || movement.inVehicle() || movement.inWaterOrLava()) {
            state.excessTicks = 0;
            return;
        }

        double stepHeight = movement.attributes().stepHeight();
        double maxRise = stepHeight + MovementConstants.POSITION_ERROR_LIMIT;
        double rise = movement.delta().y();

        if (rise <= maxRise) {
            state.excessTicks = 0;
            state.lastRise = rise;
            return;
        }

        state.excessTicks++;
        double excess = rise - maxRise;
        if (excess < MIN_EXCESS) {
            return;
        }
        if (state.excessTicks < 2) {
            state.lastRise = rise;
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("excess", round(excess));
        evidence.put("rise", round(rise));
        evidence.put("stepHeight", round(stepHeight));
        evidence.put("lastRise", round(state.lastRise));
        evidence.put("onClimbable", movement.onClimbable());
        evidence.put("sprinting", movement.sprinting());
        context.flag("step of " + round(rise) + " blocks exceeds step height", evidence,
                Math.min(excess * 10.0, 10.0));
        state.excessTicks = 0;
        state.lastRise = rise;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (StepState) state(context.player());
        if (state != null && !context.player().movement().onGround()) {
            state.excessTicks = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class StepState {

        private int excessTicks;
        private double lastRise;
    }
}
