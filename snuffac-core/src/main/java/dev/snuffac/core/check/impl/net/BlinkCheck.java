package dev.snuffac.core.check.impl.net;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class BlinkCheck implements Check {

    public static final long GAP_NANOS = 350_000_000L;
    public static final int BURST_PACKETS = 5;
    public static final long BURST_WINDOW_NANOS = 250_000_000L;
    public static final int REQUIRED_BURSTS = 2;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "blink";
    }

    @Override
    public String name() {
        return "Blink";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.PACKET;
    }

    @Override
    public String description() {
        return "Detects movement packets held back and then released in a burst, which is the "
                + "Blink and LagSwitch cheat.";
    }

    @Override
    public Object createState() {
        return new BlinkState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (BlinkState) state(context.player());
        if (state == null) {
            return;
        }

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                context.player(), context.tps(), context.ping())) {
            return;
        }
        if (context.player().movement().ticksSinceTeleport() <= 3) {
            return;
        }

        long now = packet.arrivalNanos();
        long gap = state.lastNanos == 0L ? 0L : now - state.lastNanos;
        state.lastNanos = now;

        if (gap >= GAP_NANOS) {
            state.silenceTicks += (int) (gap / 50_000_000L);
            state.released = 0;
            state.releaseStartNanos = now;
            state.releaseCount = 1;
            return;
        }

        if (state.releaseCount == 0) {
            return;
        }
        if (now - state.releaseStartNanos > BURST_WINDOW_NANOS) {
            state.releaseCount = 0;
            state.released = 0;
            return;
        }

        state.releaseCount++;
        state.released++;
        if (state.released < BURST_PACKETS) {
            return;
        }

        state.bursts++;
        if (state.bursts < REQUIRED_BURSTS) {
            state.releaseCount = 0;
            state.released = 0;
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("silenceMillis", round(state.silenceTicks * 50.0));
        evidence.put("packetsInBurst", state.released);
        evidence.put("releaseMillis", round((now - state.releaseStartNanos) / 1_000_000.0));
        evidence.put("bursts", state.bursts);
        evidence.put("positionX", round(context.player().position().x()));
        evidence.put("positionZ", round(context.player().position().z()));
        evidence.put("ping", round(context.ping()));
        evidence.put("tps", round(context.tps()));

        context.flag("held movement packets for " + state.silenceTicks * 50
                + "ms then released " + state.released + " within "
                + round((now - state.releaseStartNanos) / 1_000_000.0) + "ms", evidence, 8.0);
        state.bursts = 0;
        state.silenceTicks = 0;
        state.releaseCount = 0;
        state.released = 0;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static final class BlinkState {

        private long lastNanos;
        private long releaseStartNanos;
        private int releaseCount;
        private int released;
        private int silenceTicks;
        private int bursts;
    }
}
