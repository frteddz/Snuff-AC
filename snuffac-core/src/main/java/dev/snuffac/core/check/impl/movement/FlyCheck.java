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
        return "Detects sustained unsupported flight using the server block view instead of client claims.";
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
        if (airTicks > MAX_AIR_TICKS) {
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
        if (airTicks > MAX_AIR_TICKS) {
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
        context.flag("hovering without support for " + state.sameLevelTicks + " packets", evidence, 6.0);
        state.sameLevelTicks = 0;
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

        private void reset() {
            sameLevelTicks = 0;
            rises = 0;
        }
    }
}
