package dev.snuffac.core.check.impl.net;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class TimerCheck implements Check {

    private static final int WARMUP_TICKS = 40;
    private static final int MEASURE_TICKS = 200;
    private static final double EXPECTED_PER_TICK = 1.0;
    private static final double POSITIVE_THRESHOLD = 1.15;
    private static final double NEGATIVE_THRESHOLD = 0.82;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "timer";
    }

    @Override
    public String name() {
        return "Timer";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.PACKET;
    }

    @Override
    public String description() {
        return "Compares the client movement packet rate against server ticks to detect timer manipulation.";
    }

    @Override
    public Object createState() {
        return new TimerState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (TimerState) state(context.player());
        if (state == null) {
            return;
        }
        state.packetsThisTick++;
        context.player().network().movementReceived(packet.arrivalNanos(), 0L);
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (TimerState) state(context.player());
        if (state == null) {
            return;
        }

        if (context.player().movement().ticksSinceTeleport() <= 0) {
            state.reset();
            return;
        }

        state.measuredTicks++;
        state.totalPackets += state.packetsThisTick;
        state.packetsThisTick = 0;

        if (state.measuredTicks < WARMUP_TICKS) {
            return;
        }
        if (state.measuredTicks - WARMUP_TICKS < MEASURE_TICKS) {
            return;
        }

        double windowTicks = state.measuredTicks - WARMUP_TICKS;
        double perTick = state.totalPackets / windowTicks;

        boolean positive = perTick > POSITIVE_THRESHOLD;
        boolean negative = perTick < NEGATIVE_THRESHOLD;

        if (positive || negative) {
            if (exempt(context)) {
                state.reset();
                return;
            }
            Map<String, Object> evidence = context.newEvidence();
            evidence.put("packetsPerTick", round(perTick));
            evidence.put("expected", EXPECTED_PER_TICK);
            evidence.put("totalPackets", state.totalPackets);
            evidence.put("windowTicks", (int) windowTicks);
            evidence.put("ping", round(context.ping()));
            evidence.put("tps", round(context.tps()));
            context.flag((positive ? "positive" : "negative") + " timer at "
                    + round(perTick) + " movement packets per tick", evidence, 7.0);
        }
        state.reset();
    }

    private static boolean exempt(CheckContext context) {
        if (context.ping() > context.config().timerExemptionPing()) {
            return true;
        }
        if (context.tps() < context.config().timerMinimumTps()) {
            return true;
        }
        var movement = context.player().movement();
        return movement.riding() || movement.inVehicle() || movement.gliding() || movement.flying();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class TimerState {

        private int packetsThisTick;
        private int measuredTicks;
        private long totalPackets;

        private void reset() {
            packetsThisTick = 0;
            measuredTicks = 0;
            totalPackets = 0L;
        }
    }
}
