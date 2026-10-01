package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.check.impl.movement.AbstractMovementCheck;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class StrafeCheck extends AbstractMovementCheck {

    public static final double MIN_AIR_TICKS = 2;
    public static final double MIN_ANGLE = 12.0;
    public static final int REQUIRED_TURNS = 6;
    public static final double MAX_AIR_TICKS = 200;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "strafe";
    }

    @Override
    public String name() {
        return "Strafe";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Compares airborne direction changes against the acceleration the vanilla input "
                + "model allows, catching clients that steer in mid air beyond what keys can do.";
    }

    @Override
    public Object createState() {
        return new StrafeState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (StrafeState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (dev.snuffac.core.prediction.PredictionGraces.windChargeActive(movement)) {
            return;
        }
        if (!canPredict(player) || movement.ticksSinceTeleport() <= 3
                || movement.pendingSetback()) {
            state.reset();
            return;
        }

        var environment = environment(context);
        if (environment.inWaterOrLava() || environment.onClimbable() || environment.gliding()
                || environment.flying() || environment.riding()
                || environment.onGroundFromClient()) {
            state.reset();
            return;
        }

        int airTicks = movement.ticksSinceGround();
        if (airTicks < MIN_AIR_TICKS || airTicks > MAX_AIR_TICKS) {
            state.reset();
            return;
        }

        Vec3d before = state.previousVelocity;
        Vec3d after = movement.clientVelocity().lengthSquared() > 1.0E-8
                ? movement.clientVelocity()
                : movement.delta();
        if (before.lengthSquared() < 1.0E-8 || after.lengthSquared() < 1.0E-8) {
            state.previousVelocity = after;
            return;
        }

        double turn = angleBetween(before, after);
        state.previousVelocity = after;
        if (turn < MIN_ANGLE) {
            state.cleanTicks++;
            if (state.cleanTicks >= REQUIRED_TURNS) {
                state.turns = 0;
                state.turnSum = 0.0;
            }
            return;
        }

        state.cleanTicks = 0;
        state.turns++;
        state.turnSum += turn;
        if (state.turns < REQUIRED_TURNS) {
            return;
        }

        double average = state.turnSum / state.turns;
        Map<String, Object> evidence = context.newEvidence();
        evidence.put("turns", state.turns);
        evidence.put("averageTurn", round(average));
        evidence.put("lastTurn", round(turn));
        evidence.put("limit", MIN_ANGLE);
        evidence.put("airTicks", airTicks);
        evidence.put("speedBefore", round(before.length()));
        evidence.put("speedAfter", round(after.length()));
        evidence.put("onIce", environment.onIce());
        evidence.put("onSoulSand", environment.onSoulSand());

        context.requestSetback("airborne direction change beyond the input model");
        context.flag("changed airborne direction by " + round(average) + " degrees on average "
                + "across " + state.turns + " turns", evidence, 8.0);
        state.turns = 0;
        state.turnSum = 0.0;
    }

    public static double angleBetween(Vec3d first, Vec3d second) {
        double ax = first.x();
        double az = first.z();
        double bx = second.x();
        double bz = second.z();
        double lengths = Math.sqrt(ax * ax + az * az) * Math.sqrt(bx * bx + bz * bz);
        if (lengths < 1.0E-9) {
            return 0.0;
        }
        double dot = (ax * bx + az * bz) / lengths;
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class StrafeState {

        private Vec3d previousVelocity = Vec3d.ZERO;
        private int turns;
        private int cleanTicks;
        private double turnSum;

        private void reset() {
            turns = 0;
            cleanTicks = 0;
            turnSum = 0.0;
            previousVelocity = Vec3d.ZERO;
        }
    }
}
