package dev.snuffac.core.server;

public final class ServerHealth {

    private static final int WINDOW_TICKS = 100;

    private final long[] tickDurationsNanos = new long[WINDOW_TICKS];
    private int index;
    private int filled;

    private long lastTickNanos = System.nanoTime();
    private double currentTps = 20.0;
    private double averageTickMillis;
    private double minTps = 20.0;
    private int tickCounter;

    public synchronized void tick() {
        long now = System.nanoTime();
        long duration = now - lastTickNanos;
        lastTickNanos = now;
        tickDurationsNanos[index] = duration;
        index = (index + 1) % WINDOW_TICKS;
        if (filled < WINDOW_TICKS) {
            filled++;
        }
        tickCounter++;
        recompute();
    }

    private void recompute() {
        if (filled == 0) {
            currentTps = 20.0;
            return;
        }
        long total = 0L;
        long worst = 0L;
        for (int i = 0; i < filled; i++) {
            long duration = tickDurationsNanos[i];
            total += duration;
            if (duration > worst) {
                worst = duration;
            }
        }
        double averageNanos = total / (double) filled;
        averageTickMillis = averageNanos / 1_000_000.0;
        double worstMillis = worst / 1_000_000.0;
        currentTps = averageMillisToTps(averageTickMillis);
        minTps = averageMillisToTps(worstMillis);
    }

    private static double averageMillisToTps(double millis) {
        if (millis <= 0.0) {
            return 20.0;
        }
        return Math.min(20.0, 1000.0 / millis);
    }

    public synchronized double tps() {
        return currentTps;
    }

    public synchronized double minTps() {
        return minTps;
    }

    public synchronized double averageTickMillis() {
        return averageTickMillis;
    }

    public synchronized int tickCounter() {
        return tickCounter;
    }

    public synchronized boolean lagging(double threshold) {
        return currentTps < threshold;
    }

    public synchronized void reset() {
        index = 0;
        filled = 0;
        lastTickNanos = System.nanoTime();
        currentTps = 20.0;
        minTps = 20.0;
        averageTickMillis = 0.0;
    }
}
