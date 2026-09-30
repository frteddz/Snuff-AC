package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class CriticalCheck implements Check {

    public static final int EXTRA_PACKET_ALLOWANCE = 1;
    public static final long WINDOW_NANOS = 1_200_000_000L;
    public static final double LEGIT_Y_DELTA = 0.0035;
    public static final double LEGIT_DISTANCE = 1.35;
    public static final double LEGIT_REACH = 4.2;
    public static final int REQUIRED = 3;
    public static final int FALL_STATE_REQUIRED = 2;
    public static final double MIN_FALL_SPEED = 0.05;
    public static final double GROUND_SLACK = 0.02;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "critical";
    }

    @Override
    public String name() {
        return "Criticals";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects forced criticals produced by sending extra position packets with a small "
                + "vertical lift immediately before an attack, and reports a critical landed while "
                + "the server knows the player is not falling.";
    }

    @Override
    public Object createState() {
        return new CriticalState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (CriticalState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof MovementPacket movement && movement.positionChanged()) {
            if (context.player().movement().movementPacketsThisTick() > EXTRA_PACKET_ALLOWANCE) {
                record(context, state, movement.arrivalNanos());
            }
            return;
        }
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }

        prune(context, state, attack.arrivalNanos());
        boolean falling = isGenuineFall(context);
        state.landingTicks = falling ? 0 : state.landingTicks + 1;

        Map<String, Object> landing = null;
        if (state.lifts < REQUIRED && !isGenuineFall(context)) {
            if (state.landingTicks < FALL_STATE_REQUIRED) {
                return;
            }
            landing = context.newEvidence();
            landing.put("mode", "not falling");
            landing.put("consecutive", state.landingTicks);
            landing.put("onGround", context.player().movement().onGround());
            landing.put("velocityY", round(context.player().movement().velocity().y()));
            landing.put("inWater", context.player().movement().inWaterOrLava());
            landing.put("onClimbable", context.player().movement().onClimbable());
            landing.put("fallDistance", round(context.player().movement().fallDistance()));
            context.flag("critical landed while not falling", landing, 8.0);
            state.landingTicks = 0;
        }

        if (state.lifts < REQUIRED) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "extra packets");
        evidence.put("lifts", state.lifts);
        evidence.put("windowNanos", WINDOW_NANOS);
        evidence.put("lastLiftY", state.lastLift);
        evidence.put("onGround", context.player().movement().onGround());
        evidence.put("distance", Math.round(attack.cursorPosition().distanceTo(context.position()) * 100.0) / 100.0);
        context.flag("forced critical through extra movement packets", evidence, 9.0);
        state.lifts = 0;
        state.landingTicks = 0;
        return;
    }

    public static boolean isGenuineFall(CheckContext context) {
        var movement = context.player().movement();
        if (movement.onGround()) {
            return false;
        }
        if (movement.inWaterOrLava() || movement.onClimbable() || movement.riding()
                || movement.inVehicle() || movement.gliding() || movement.flying()) {
            return false;
        }
        if (movement.hasLevitation() || movement.hasSlowFalling()) {
            return false;
        }
        return -movement.velocity().y() >= MIN_FALL_SPEED
                || movement.fallDistance() > GROUND_SLACK;
    }

    private static void record(CheckContext context, CriticalState state, long arrivalNanos) {
        var movement = context.player().movement();
        double deltaY = movement.delta().y();
        double distance = movement.delta().horizontalLength();

        if (movement.onGround()) {
            state.groundTicks++;
        } else {
            state.groundTicks = 0;
        }

        if (state.lastNanos == 0L) {
            state.lastNanos = arrivalNanos;
            state.lastLift = deltaY;
            return;
        }
        long gap = arrivalNanos - state.lastNanos;
        state.lastNanos = arrivalNanos;
        state.lastLift = deltaY;

        if (gap > WINDOW_NANOS) {
            return;
        }
        if (distance > LEGIT_DISTANCE) {
            return;
        }
        if (Math.abs(deltaY) > LEGIT_Y_DELTA) {
            return;
        }
        if (movement.onGround()) {
            return;
        }
        state.lifts++;
    }

    private static void prune(CheckContext context, CriticalState state, long arrivalNanos) {
        if (state.lastNanos == 0L) {
            return;
        }
        if (arrivalNanos - state.lastNanos > WINDOW_NANOS) {
            state.lifts = 0;
        }
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (CriticalState) state(context.player());
        if (state != null && state.lifts > 0) {
            state.lifts--;
        }
    }

    static final class CriticalState {

        private long lastNanos;
        private double lastLift;
        private int lifts;
        private int groundTicks;
        private int landingTicks;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
