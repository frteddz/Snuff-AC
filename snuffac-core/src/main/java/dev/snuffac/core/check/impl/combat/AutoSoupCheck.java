package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.EquipmentState.HeldKind;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;

public final class AutoSoupCheck implements Check {

    public static final long MIN_INTERVAL_MILLIS = 20L;
    public static final long MAX_INTERVAL_MILLIS = 400L;
    public static final int REQUIRED = 4;
    public static final double JITTER_RATIO = 0.2;
    private static final int WINDOW = 8;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_PLACE);
    }

    @Override
    public String key() {
        return "autosoup";
    }

    @Override
    public String name() {
        return "AutoSoup";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects food and soup eaten at a fixed interval, which is the AutoSoup and AutoEat cheat.";
    }

    @Override
    public Object createState() {
        return new Cadence();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (Cadence) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        if (player.equipment().held() != HeldKind.FOOD) {
            state.clear();
            return;
        }
        if (player.movement().ticksSinceTeleport() <= 2 || player.movement().inVehicle()) {
            state.clear();
            return;
        }

        long now = packet.arrivalNanos() / 1_000_000L;
        if (state.lastMillis == 0L) {
            state.lastMillis = now;
            return;
        }

        long interval = now - state.lastMillis;
        state.lastMillis = now;
        if (interval < MIN_INTERVAL_MILLIS) {
            return;
        }

        state.intervals.add(interval);
        if (state.intervals.size() > WINDOW) {
            state.intervals.removeFirst();
        }
        if (state.intervals.size() < REQUIRED) {
            return;
        }

        long mean = mean(state.intervals);
        if (mean <= 0L || mean > MAX_INTERVAL_MILLIS) {
            return;
        }
        long jitter = jitter(state.intervals, mean);
        if ((double) jitter / (double) mean > JITTER_RATIO) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("meanIntervalMs", mean);
        evidence.put("jitterMs", jitter);
        evidence.put("samples", state.intervals.size());
        evidence.put("intervals", state.intervals);
        evidence.put("maxIntervalMs", MAX_INTERVAL_MILLIS);
        evidence.put("heldItem", player.equipment().held().name());
        evidence.put("attacks", player.combat().attacks());
        evidence.put("positionX", round(player.position().x()));
        evidence.put("positionZ", round(player.position().z()));

        context.flag("ate at a fixed interval of " + mean
                + "ms with only " + jitter + "ms of spread", evidence, 6.0);
        state.intervals.clear();
    }

    private static long mean(ArrayDeque<Long> values) {
        long total = 0L;
        for (long value : values) {
            total += value;
        }
        return values.isEmpty() ? 0L : total / values.size();
    }

    private static long jitter(ArrayDeque<Long> values, long mean) {
        long total = 0L;
        for (long value : values) {
            total += Math.abs(value - mean);
        }
        return values.isEmpty() ? 0L : total / values.size();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class Cadence {

        private final ArrayDeque<Long> intervals = new ArrayDeque<>(WINDOW);
        private long lastMillis;

        private void clear() {
            intervals.clear();
            lastMillis = 0L;
        }
    }
}
