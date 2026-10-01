package dev.snuffac.core.check.impl.combat;

import java.util.ArrayDeque;

public final class ReactionTiming {

    public static final int WINDOW = 12;
    public static final double JITTER_RATIO = 0.25;

    private ReactionTiming() {
    }

    public record Verdict(boolean flagged, long mean, long jitter, int samples, String reason) {

        public static Verdict notFlagged(int samples) {
            return new Verdict(false, 0L, 0L, samples, "reaction spread too wide to be mechanical");
        }
    }

    public static long mean(ArrayDeque<Long> samples) {
        if (samples.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (long value : samples) {
            total += value;
        }
        return total / samples.size();
    }

    public static long jitter(ArrayDeque<Long> samples, long mean) {
        if (samples.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (long value : samples) {
            total += Math.abs(value - mean);
        }
        return total / samples.size();
    }

    public static Verdict judge(ArrayDeque<Long> samples, int required, long maximumMillis) {
        if (samples.size() < required) {
            return Verdict.notFlagged(samples.size());
        }
        long mean = mean(samples);
        if (mean <= 0L) {
            return Verdict.notFlagged(samples.size());
        }
        if (mean > maximumMillis) {
            return Verdict.notFlagged(samples.size());
        }
        long jitter = jitter(samples, mean);
        if ((double) jitter / (double) mean > JITTER_RATIO) {
            return Verdict.notFlagged(samples.size());
        }
        return new Verdict(true, mean, jitter, samples.size(), null);
    }
}
