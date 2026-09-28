package dev.snuffac.core.confidence;

import dev.snuffac.core.util.MathUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class ConfidenceModel {

    public static final double WINDOW_TICKS = 200.0;
    public static final double PEAK_RETAINED_TICKS = 40.0;

    private final Deque<Signal> signals = new ArrayDeque<>();
    private double current;
    private double peak;
    private long peakTick = -1L;
    private long tick;
    private int signalCount;

    public void add(String checkKey, double weight, long nowMillis) {
        double contribution = MathUtil.clamp(weight, 0.0, 1.0);
        current += contribution;
        peak = Math.max(peak, current);
        peakTick = nowMillis;
        signalCount++;
        signals.addLast(new Signal(checkKey, contribution, nowMillis));
        evict(nowMillis);
    }

    public void decay(double amount) {
        current = Math.max(0.0, current - Math.max(0.0, amount));
        evict(System.currentTimeMillis());
    }

    public void tick(long nowMillis) {
        this.tick++;
        current *= 0.995;
        if (current < 0.01) {
            current = 0.0;
        }
        evict(nowMillis);
    }

    private void evict(long nowMillis) {
        while (!signals.isEmpty() && nowMillis - signals.peekFirst().millis > PEAK_RETAINED_TICKS) {
            signals.removeFirst();
        }
    }

    public double current() {
        return current;
    }

    public double peak() {
        long now = System.currentTimeMillis();
        if (peakTick > 0 && now - peakTick > PEAK_RETAINED_TICKS * 50L) {
            return current;
        }
        return peak;
    }

    public int signalCount() {
        return signalCount;
    }

    public long tick() {
        return tick;
    }

    public List<String> recentCheckKeys() {
        List<String> keys = new ArrayList<>();
        for (Signal signal : signals) {
            keys.add(signal.checkKey);
        }
        return keys;
    }

    public void reset() {
        signals.clear();
        current = 0.0;
        peak = 0.0;
        peakTick = -1L;
    }

    public record Signal(String checkKey, double weight, long millis) {
    }
}
