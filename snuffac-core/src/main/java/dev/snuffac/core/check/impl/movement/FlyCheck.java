package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class FlyCheck extends AbstractMovementCheck {

    private static final int MAX_AIR_TICKS = 220;
    private static final int HOVER_TICKS = 40;
    private static final int RISE_GRACE_TICKS = 4;
    private static final int RISES_BEFORE_FLAG = 3;
    private static final int PARTIAL_SUPPORT_REFILL = 10;
    private static final int BUDGET_CAP = MAX_AIR_TICKS + 60;
    private static final int BUDGETS_BEFORE_FLAG = 2;

    @Override
    public String key() {
        return "fly";
    }

    @Override
    public String name() {
        return "Fly";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects sustained unsupported flight using the server block view instead of client claims, "
                + "and runs an air time budget that only legitimate support can refill.";
    }

    @Override
    public Object createState() {
        return new FlyState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement)) {
            return;
        }
        if (!movement.positionChanged()) {
            countIdleTicks(context, movement);
            return;
        }

        var state = (FlyState) state(context.player());
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

        if (movementState.onGround() || movementState.flying() || movementState.gliding()
                || movementState.riding() || movementState.inVehicle()
                || movementState.inWaterOrLava() || movementState.onClimbable()
                || movementState.hasSlowFalling() || movementState.hasLevitation()
                || movementState.riptiding()
                || movementState.ticksSinceKnockback() < 12
                || movementState.ticksSinceBlockChange() < 3) {
            state.reset();
            return;
        }

        if (!cache.chunkLoaded() || movementState.ticksSinceVehicle() <= 5 || movementState.ticksOnFire() > 0) {
            return;
        }

        int airTicks = movementState.ticksSinceGround();
        if (spendAirBudget(context, state, airTicks, movementState)) {
            return;
        }
        if (cache.onGroundBelow()) {
            return;
        }

        double deltaY = movementState.delta().y();

        if (Math.abs(deltaY) < 1.0E-6) {
            state.sameLevelTicks++;
        } else {
            state.sameLevelTicks = 0;
        }

        if (deltaY > 0.0 && airTicks > RISE_GRACE_TICKS) {
            state.rises++;
            if (state.rises >= RISES_BEFORE_FLAG) {
                Map<String, Object> evidence = context.newEvidence();
                evidence.put("mode", "rising");
                evidence.put("airTicks", airTicks);
                evidence.put("deltaY", round(deltaY));
                evidence.put("velocityY", round(movementState.velocity().y()));
                evidence.put("rises", state.rises);
                context.requestSetback("rising while airborne for " + airTicks + " ticks");
            context.flag("rising while airborne for " + airTicks + " ticks", evidence, 8.0);
                state.rises = 0;
            }
            return;
        }

        if (state.sameLevelTicks >= HOVER_TICKS) {
            Map<String, Object> evidence = context.newEvidence();
            evidence.put("mode", "hovering");
            evidence.put("airTicks", airTicks);
            evidence.put("hoverTicks", state.sameLevelTicks);
            evidence.put("velocityY", round(movementState.velocity().y()));
            evidence.put("fallDistance", round(movementState.fallDistance()));
            context.requestSetback("hovering without support");
            context.flag("hovering without support for " + state.sameLevelTicks + " ticks", evidence, 6.0);
            state.sameLevelTicks = 0;
        }
    }

    private void countIdleTicks(CheckContext context, MovementPacket movement) {
        var state = (FlyState) state(context.player());
        if (state == null) {
            return;
        }
        var movementState = context.player().movement();
        if (movementState.onGround() || movementState.flying() || movementState.gliding()
                || movementState.inVehicle() || movementState.inWaterOrLava()
                || movementState.onClimbable() || movementState.hasSlowFalling()
                || movementState.hasLevitation() || movementState.riptiding()) {
            state.reset();
            return;
        }
        if (!context.player().worldCache().chunkLoaded()
                || context.player().worldCache().onGroundBelow()) {
            return;
        }
        int airTicks = movementState.ticksSinceGround();
        if (spendAirBudget(context, state, airTicks, movementState)) {
            return;
        }
        if (airTicks < RISE_GRACE_TICKS) {
            return;
        }

        state.sameLevelTicks++;
        if (state.sameLevelTicks < HOVER_TICKS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "hovering");
        evidence.put("airTicks", airTicks);
        evidence.put("hoverTicks", state.sameLevelTicks);
        evidence.put("velocityY", round(movementState.velocity().y()));
        evidence.put("positionChanged", false);
        context.requestSetback("hovering without support");
        context.flag("hovering without support for " + state.sameLevelTicks + " packets", evidence, 6.0);
        state.sameLevelTicks = 0;
    }

    private boolean spendAirBudget(
            CheckContext context,
            FlyState state,
            int airTicks,
            dev.snuffac.core.player.MovementState movement) {

        if (airTicks < 2) {
            state.refill(MAX_AIR_TICKS);
            state.unsupportedTicks = 0;
            return false;
        }

        if (partialSupport(movement)) {
            state.refill(MAX_AIR_TICKS);
            state.unsupportedTicks = 0;
            return false;
        }

        state.unsupportedTicks++;
        state.budget = Math.max(0, state.budget - 1);

        if (state.budget > 0) {
            return false;
        }
        if (state.budgetFlags >= BUDGETS_BEFORE_FLAG) {
            return true;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "air budget");
        evidence.put("airTicks", airTicks);
        evidence.put("unsupportedTicks", state.unsupportedTicks);
        evidence.put("budget", state.budget);
        evidence.put("velocityY", round(movement.velocity().y()));
        evidence.put("fallDistance", round(movement.fallDistance()));
        evidence.put("ticksSinceKnockback", movement.ticksSinceKnockback());
        evidence.put("height", round(movement.position().y()));

        context.requestSetback("air time budget exhausted after " + state.unsupportedTicks + " ticks");
        context.flag("unsupported for " + state.unsupportedTicks
                + " ticks with no valid support", evidence, 10.0);

        state.budgetFlags++;
        state.budget = PARTIAL_SUPPORT_REFILL * 4;
        return true;
    }

    private static boolean partialSupport(dev.snuffac.core.player.MovementState movement) {
        return movement.flying() || movement.gliding() || movement.riding()
                || movement.inVehicle() || movement.inWaterOrLava() || movement.swimming()
                || movement.onClimbable() || movement.onHoney() || movement.onSoulSand()
                || movement.hasSlowFalling() || movement.hasLevitation() || movement.riptiding()
                || movement.ticksSinceKnockback() < 12
                || movement.ticksSinceBlockChange() < 3
                || movement.ticksOnFire() > 0;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (FlyState) state(context.player());
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

    static final class FlyState {

        private int sameLevelTicks;
        private int rises;
        private int budget = MAX_AIR_TICKS;
        private int unsupportedTicks;
        private int budgetFlags;

        private void refill(int target) {
            budget = Math.min(BUDGET_CAP, Math.max(budget, target));
        }

        private void reset() {
            sameLevelTicks = 0;
            rises = 0;
            budget = MAX_AIR_TICKS;
            unsupportedTicks = 0;
            budgetFlags = 0;
        }
    }
}
