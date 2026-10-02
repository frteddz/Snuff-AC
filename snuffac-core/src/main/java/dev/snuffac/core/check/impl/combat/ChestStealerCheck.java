package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.packet.WindowClickPacket;
import java.util.Map;
import java.util.Set;

public final class ChestStealerCheck implements Check {

    public static final long MIN_CLICK_MILLIS = 12L;
    public static final int REQUIRED_CLICKS = 8;
    public static final int WINDOW = 16;
    public static final double JITTER_RATIO = 0.4;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.INVENTORY_CLICK);
    }

    @Override
    public String key() {
        return "cheststealer";
    }

    @Override
    public String name() {
        return "ChestStealer";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects emptying a container on a fixed click interval, and clicks outside the "
                + "visible slot range, which is the ChestStealer and AutoDrop cheat.";
    }

    @Override
    public Object createState() {
        return new StealState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (StealState) state(context.player());
        if (state == null) {
            return;
        }

        if (!(packet instanceof WindowClickPacket click)) {
            return;
        }
        if (click.windowId() == 0) {
            return;
        }

        if (!inVisibleRange(click.slot())) {
            state.outside++;
            state.intervals.clear();
            if (state.outside < REQUIRED_CLICKS) {
                return;
            }
            Map<String, Object> blind = context.newEvidence();
            blind.put("mode", "outside visible range");
            blind.put("clicks", state.outside);
            blind.put("lastSlot", click.slot());
            blind.put("window", click.windowId());
            context.flag("clicked slot " + click.slot() + " outside the visible range of a container",
                    blind, 6.0);
            state.reset();
            return;
        }
        state.outside = 0;

        long now = packet.arrivalNanos() / 1_000_000L;
        if (state.lastMillis != 0L) {
            long interval = now - state.lastMillis;
            if (interval >= MIN_CLICK_MILLIS && interval <= 500L) {
                state.intervals.add(interval);
                if (state.intervals.size() > WINDOW) {
                    state.intervals.removeFirst();
                }
            } else {
                state.intervals.clear();
            }
        }
        state.lastMillis = now;

        if (state.intervals.size() < REQUIRED_CLICKS) {
            return;
        }

        long mean = mean(state.intervals);
        if (mean <= 0L) {
            return;
        }
        long jitter = jitter(state.intervals, mean);
        if ((double) jitter / (double) mean > JITTER_RATIO) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "fixed cadence");
        evidence.put("meanIntervalMs", mean);
        evidence.put("jitterMs", jitter);
        evidence.put("samples", state.intervals.size());
        evidence.put("intervals", state.intervals);
        evidence.put("window", click.windowId());
        evidence.put("lastSlot", click.slot());

        context.flag("emptied a container on a " + mean + "ms click interval with only "
                + jitter + "ms of spread", evidence, 6.0);
        state.intervals.clear();
    }

    public static boolean inVisibleRange(int slot) {
        return slot >= 0 && slot <= 53;
    }

    private static long mean(java.util.ArrayDeque<Long> values) {
        long total = 0L;
        for (long value : values) {
            total += value;
        }
        return values.isEmpty() ? 0L : total / values.size();
    }

    private static long jitter(java.util.ArrayDeque<Long> values, long mean) {
        long total = 0L;
        for (long value : values) {
            total += Math.abs(value - mean);
        }
        return values.isEmpty() ? 0L : total / values.size();
    }

    static final class StealState {

        private final java.util.ArrayDeque<Long> intervals = new java.util.ArrayDeque<>(WINDOW);
        private long lastMillis;
        private int outside;

        private void reset() {
            intervals.clear();
            lastMillis = 0L;
            outside = 0;
        }
    }
}