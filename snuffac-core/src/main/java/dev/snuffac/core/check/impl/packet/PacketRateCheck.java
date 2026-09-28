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
    public static final double FAST_RATIO = 1.18;
    public static final double SLOW_RATIO = 0.80;
    public static final double MAX_PING_MILLIS = 320.0;
    public static final double MIN_TPS = 19.5;

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
        long elapsed = packet.arrivalNanos() - state.windowStart;
        if (elapsed < MIN_SAMPLE_MILLIS * 1_000_000L) {
            state.packets++;
            return;
        }
        double seconds = elapsed / 1_000_000_000.0;
        double rate = state.packets / seconds;
        state.packets = 1;
        state.windowStart = packet.arrivalNanos();
        state.samples++;

        if (state.samples < 2 || context.ping() > MAX_PING_MILLIS || context.tps() < MIN_TPS) {
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
        if (state.consecutive < 3) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("rate", Math.round(rate * 100.0) / 100.0);
        evidence.put("expected", expected);
        evidence.put("seconds", Math.round(seconds * 100.0) / 100.0);
        evidence.put("consecutive", state.consecutive);
        evidence.put("ping", Math.round(context.ping() * 10.0) / 10.0);
        context.flag((fast ? "elevated" : "reduced") + " movement packet rate", evidence, 7.0);
        state.consecutive = 0;
    }

    static final class RateState {

        private long windowStart;
        private int packets;
        private int samples;
        private int consecutive;
    }
}
