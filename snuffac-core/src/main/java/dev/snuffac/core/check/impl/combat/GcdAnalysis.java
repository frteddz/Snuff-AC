package dev.snuffac.core.check.impl.combat;

import dev.snuffac.core.util.MathUtil;

public final class GcdAnalysis {

    public static final int WINDOW = 240;
    public static final double MIN_DELTA = 0.005;
    public static final double DEFAULT_CONSTANT = 0.15;

    private GcdAnalysis() {
    }

    public static double estimateConstant(double[] deltas, int count) {
        if (count < 4) {
            return 0.0;
        }
        double sum = 0.0;
        int used = 0;
        for (int i = 0; i < count; i++) {
            double value = Math.abs(deltas[i]);
            if (value < MIN_DELTA || value > 180.0 || Double.isNaN(value) || Double.isInfinite(value)) {
                continue;
            }
            sum += value;
            used++;
        }
        if (used < 4) {
            return 0.0;
        }
        double mean = sum / used;

        long accumulator = 0L;
        int counted = 0;
        for (int i = 0; i < count; i++) {
            double value = Math.abs(deltas[i]);
            if (value < MIN_DELTA || value > 180.0) {
                continue;
            }
            long scaled = Math.round(value * 10000.0);
            if (scaled == 0L) {
                continue;
            }
            accumulator = counted == 0 ? scaled : MathUtil.gcd(accumulator, scaled);
            counted++;
        }
        if (counted < 4 || accumulator == 0L) {
            return 0.0;
        }
        double candidate = accumulator / 10000.0;
        if (candidate < MIN_DELTA) {
            return 0.0;
        }
        double deviation = 0.0;
        for (int i = 0; i < count; i++) {
            double value = Math.abs(deltas[i]);
            if (value < MIN_DELTA) {
                continue;
            }
            double multiple = value / candidate;
            double rounded = Math.rint(multiple);
            if (rounded < 1.0) {
                continue;
            }
            deviation += Math.abs(multiple - rounded);
        }
        double average = deviation / Math.max(1, counted);
        if (average > 0.08) {
            return 0.0;
        }
        return candidate;
    }

    public static boolean isMultipleOf(double delta, double constant) {
        if (constant <= 0.0) {
            return false;
        }
        double multiple = Math.abs(delta) / constant;
        double rounded = Math.rint(multiple);
        if (rounded < 1.0) {
            return false;
        }
        return Math.abs(multiple - rounded) < 0.02;
    }

    public static final class Window {

        private final double[] deltas = new double[WINDOW];
        private int count;
        private double constant;

        public void offer(double delta) {
            if (count < WINDOW) {
                deltas[count++] = delta;
            } else {
                System.arraycopy(deltas, 1, deltas, 0, WINDOW - 1);
                deltas[WINDOW - 1] = delta;
            }
            constant = estimateConstant(deltas, count);
        }

        public double constant() {
            return constant;
        }

        public int size() {
            return count;
        }

        public void clear() {
            count = 0;
            constant = 0.0;
        }
    }
}
