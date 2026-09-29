package dev.snuffac.core.check.impl.packet;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class PacketRateCheck implements Check {

    public static final int MIN_SAMPLE_MILLIS = 4000;
    public static final double FAST_RATIO = 1.30;
    public static final double SLOW_RATIO = 0.55;
    public static final double MAX_PING_MILLIS = 320.0;
    public static final double MIN_TPS = 19.5;
    public static final int REQUIRED_CONSECUTIVE = 5;
    public static final double MIN_POSITION_MOVEMENT = 0.0001;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "packetrate";
    }

    @Override
    public String name() {
        return "PacketRate";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.PACKET;
    }

    @Override
    public String description() {
        return "Detects a sustained deviation of the movement packet rate from the server tick rate, which indicates timer manipulation.";
    }

    @Override
    public Object createState() {
        return new RateState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (RateState) state(context.player());
        if (state == null) {
            return;
        }
        if (state.windowStart == 0L) {
            state.windowStart = packet.arrivalNanos();
            state.packets = 1;
            return;
        }
        var state1 = context.player().movement();
        if (state1.ticksSinceTeleport() <= 1) {
            state.discard();
            return;
        }
        if (state.lastX != 0.0 || state.lastZ != 0.0) {
            double dx = state1.position().x() - state.lastX;
            double dz = state1.position().z() - state.lastZ;
            if (dx * dx + dz * dz > MIN_POSITION_MOVEMENT * MIN_POSITION_MOVEMENT) {
                state.moved = true;
            }
        }
        state.lastX = state1.position().x();
        state.lastZ = state1.position().z();

        long elapsed = packet.arrivalNanos() - state.windowStart;
        if (elapsed < MIN_SAMPLE_MILLIS * 1_000_000L) {
            state.packets++;
            return;
        }
        boolean moved = state.moved;
        int packets = state.packets;
        double seconds = elapsed / 1_000_000_000.0;
        state.discard();
        state.windowStart = packet.arrivalNanos();

        if (!moved || packets <= 0) {
            state.consecutive = 0;
            return;
        }
        if (context.ping() > MAX_PING_MILLIS || context.tps() < MIN_TPS) {
            state.consecutive = 0;
            return;
        }

        double rate = packets / seconds;
        state.samples++;

        if (state.samples < 4) {
            return;
        }

        double expected = ExtraPacketsCheck.SERVER_TICKS_PER_SECOND;
        boolean fast = rate > expected * FAST_RATIO;
        boolean slow = rate < expected * SLOW_RATIO;
        if (!fast && !slow) {
            state.consecutive = 0;
            return;
        }
        state.consecutive++;
        if (state.consecutive < REQUIRED_CONSECUTIVE) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("rate", Math.round(rate * 100.0) / 100.0);
        evidence.put("expected", expected);
        evidence.put("seconds", Math.round(seconds * 100.0) / 100.0);
        evidence.put("consecutive", state.consecutive);
        evidence.put("windowsRequired", REQUIRED_CONSECUTIVE);
        evidence.put("movedInWindow", true);
        evidence.put("ping", Math.round(context.ping() * 10.0) / 10.0);
        context.flag((fast ? "elevated" : "reduced") + " movement packet rate", evidence, 7.0);
        state.consecutive = 0;
    }

    static final class RateState {

        private long windowStart;
        private int packets;
        private int samples;
        private int consecutive;
        private boolean moved;
        private double lastX;
        private double lastZ;

        private void discard() {
            packets = 0;
            moved = false;
        }
    }
}
