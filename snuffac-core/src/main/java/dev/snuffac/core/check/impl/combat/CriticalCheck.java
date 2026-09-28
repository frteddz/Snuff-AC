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

    public static final long WINDOW_NANOS = 1_200_000_000L;
    public static final double LEGIT_Y_DELTA = 0.0035;
    public static final double LEGIT_DISTANCE = 1.35;
    public static final double LEGIT_REACH = 4.2;
    public static final int REQUIRED = 3;

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
        return "Detects forced criticals produced by sending extra position packets with a small vertical lift immediately before an attack.";
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
            record(context, state, movement.arrivalNanos());
            return;
        }
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }

        prune(context, state, attack.arrivalNanos());
        if (state.lifts < REQUIRED) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("lifts", state.lifts);
        evidence.put("windowNanos", WINDOW_NANOS);
        evidence.put("lastLiftY", state.lastLift);
        evidence.put("onGround", context.player().movement().onGround());
        evidence.put("distance", Math.round(attack.cursorPosition().distanceTo(context.position()) * 100.0) / 100.0);
        context.flag("forced critical through extra movement packets", evidence, 9.0);
        state.lifts = 0;
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
    }
}
