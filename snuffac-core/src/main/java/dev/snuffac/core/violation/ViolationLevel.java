package dev.snuffac.core.violation;

import dev.snuffac.core.util.MathUtil;

public final class ViolationLevel {

    private final double maximum;
    private final double decayPerTick;

    private double value;

    public ViolationLevel(double maximum, double decayPerTick) {
        this.maximum = maximum;
        this.decayPerTick = decayPerTick;
    }

    public static ViolationLevel standard() {
        return new ViolationLevel(1000.0, 0.05);
    }

    public double increase(double amount) {
        value = Math.min(value + amount, maximum);
        return value;
    }

    public void tick() {
        value = Math.max(value - decayPerTick, 0.0);
    }

    public void reset() {
        value = 0.0;
    }

    public void set(double amount) {
        value = MathUtil.clamp(amount, 0.0, maximum);
    }

    public double value() {
        return value;
    }

    public double maximum() {
        return maximum;
    }
}
