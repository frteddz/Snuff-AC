package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;

public final class AirJumpCheck extends AbstractMovementCheck {

    public static final double MIN_RISE = 0.12;
    public static final int REQUIRED_TICKS = 3;
    public static final double VOID_GAP = 1.2;

    @Override
    public String key() {
        return "airjump";
    }

    @Override
    public String name() {
        return "AirJump";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects a jump event landing with no block underneath to jump from, which is the "
                + "AirJump cheat.";
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

        var player = context.player();
        var movement = player.movement();
        var cache = player.worldCache();

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (!cache.chunkLoaded() || movement.ticksSinceTeleport() <= 2
                || movement.pendingSetback()) {
            state.reset();
            return;
        }
        if (movement.inVehicle() || movement.riding() || movement.inWaterOrLava()
                || movement.onClimbable() || movement.gliding() || movement.flying()
                || movement.hasLevitation() || movement.hasSlowFalling()) {
            state.reset();
            return;
        }

        double rise = movement.delta().y();
        if (rise < MIN_RISE) {
            state.reset();
            return;
        }
        if (movement.ticksSinceGround() <= 1) {
            state.reset();
            return;
        }
        if (hasGroundBelow(cache, movement.position())) {
            state.reset();
            return;
        }

        state.rises++;
        if (state.rises < REQUIRED_TICKS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("rises", state.rises);
        evidence.put("rise", round(rise));
        evidence.put("minimumRise", MIN_RISE);
        evidence.put("airTicks", movement.ticksSinceGround());
        evidence.put("velocityY", round(movement.velocity().y()));
        evidence.put("positionY", round(movement.position().y()));
        evidence.put("sprinting", movement.sprinting());
        evidence.put("groundBelow", false);

        context.requestSetback("jumped with nothing underneath");
        context.flag("rose " + round(rise) + " blocks per tick for " + state.rises
                + " ticks with no ground underneath", evidence, 9.0);
        state.reset();
    }

    public static boolean hasGroundBelow(
            dev.snuffac.core.player.PlayerWorldCache cache, Vec3d position) {
        BlockPos feet = BlockPos.of(position);
        for (int depth = 1; depth <= 3; depth++) {
            BlockPos below = feet.offset(0, -depth, 0);
            if (cache.blocksMovement(below)) {
                return true;
            }
        }
        return false;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class JumpState {

        private int rises;

        private void reset() {
            rises = 0;
        }
    }
}