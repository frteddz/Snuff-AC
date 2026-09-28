package dev.snuffac.core.violation;

import dev.snuffac.core.util.MathUtil;

public final class ViolationBuffer {

    private final double threshold;
    private final double decay;
    private final double maximum;

    private double value;

    public ViolationBuffer(double threshold, double decay, double maximum) {
        this.threshold = threshold;
        this.decay = decay;
        this.maximum = maximum;
    }

    public static ViolationBuffer standard() {
        return new ViolationBuffer(20.0, 0.5, 100.0);
    }

    public double add(double amount) {
        if (amount <= 0.0) {
            return value;
        }
        value = Math.min(value + amount, maximum);
        return value;
    }

    public double addProportional(double excess, double weight) {
        return add(excess * weight);
    }

    public boolean crossed() {
        return value >= threshold;
    }

    public void drain() {
        value = 0.0;
    }

    public void reduce(double amount) {
        value = Math.max(value - amount, 0.0);
    }

    public void tick() {
        value = Math.max(value - decay, 0.0);
    }

    public double progress() {
        if (threshold <= 0.0) {
            return 0.0;
        }
        return MathUtil.clamp(value / threshold, 0.0, 1.0);
    }

    public double value() {
        return value;
    }

    public double threshold() {
        return threshold;
    }

    public double decay() {
        return decay;
    }

    public boolean isEmpty() {
        return value <= 0.0;
    }

    public void reset() {
        value = 0.0;
    }
}
