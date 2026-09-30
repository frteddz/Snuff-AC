package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class AutoClickerCheck implements Check {

    private static final long WINDOW_MILLIS = 1000L;
    private static final double MIN_VARIANCE = 0.0;
    private static final int BURST_TICKS = 3;
    public static final int CADENCE_MIN_SAMPLES = 14;
    public static final double SPREAD_MIN = 0.06;
    public static final double SPREAD_RATIO = 0.04;
    public static final int REPEAT_RUN = 6;
    public static final double REPEAT_SPREAD = 1.5;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK, PacketType.ARM_ANIMATION);
    }

    @Override
    public String key() {
        return "autoclicker";
    }

    @Override
    public String name() {
        return "AutoClicker";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Analyses attack and swing cadence for impossible rates and machine perfect timing "
                + "variance, including the distribution shape of the intervals, repeated identical "
                + "delays and a perfectly even cadence.";
    }

    @Override
    public Object createState() {
        return new ClickerState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (ClickerState) state(context.player());
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        var combat = context.player().combat();

        if (packet instanceof AttackPacket) {
            combat.recordAttackTime(now);
        } else if (packet instanceof dev.snuffac.core.packet.ArmAnimationPacket) {
            combat.recordSwing(now);
        } else {
            return;
        }

        int window = Math.max(4, context.config().clickerWindowSize());
        long span = (long) (window * 1000.0 / 20.0);
        int count = combat.attacksInWindow(now, Math.max(span, WINDOW_MILLIS));

        double cps = count * 1000.0 / (double) Math.max(1L, Math.max(span, WINDOW_MILLIS));
        double maximum = context.config().clickerMaxCps();

        state.samples.add(now);
        while (state.samples.size() > window) {
            state.samples.remove(0);
        }
        if (state.samples.size() < 6) {
            return;
        }

        long[] intervals = new long[state.samples.size() - 1];
        for (int i = 0; i < intervals.length; i++) {
            intervals[i] = state.samples.get(i + 1) - state.samples.get(i);
        }
        double mean = mean(intervals);
        double deviation = standardDeviation(intervals, mean);
        double varianceRatio = mean <= 0.0 ? 0.0 : deviation / mean;

        if (cps <= maximum || varianceRatio > MIN_VARIANCE) {
            state.perfectTicks = 0;
            return;
        }

        if (deviation < 1.0) {
            state.perfectTicks++;
        } else {
            state.perfectTicks = 0;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("cps", round(cps));
        evidence.put("maximumCps", maximum);
        evidence.put("meanInterval", round(mean));
        evidence.put("deviation", round(deviation));
        evidence.put("varianceRatio", round(varianceRatio));
        evidence.put("samples", intervals.length);
        evidence.put("perfectTicks", state.perfectTicks);
        evidence.put("swings", combat.swings());

        state.cadenceSamples++;
        double spread = standardDeviation(intervals, mean);
        double longestRepeat = longestRepeatRun(intervals);

        evidence.put("intervalSpread", round(spread));
        evidence.put("longestRepeat", longestRepeat);
        evidence.put("cadenceSamples", state.cadenceSamples);

        boolean machineEven = state.cadenceSamples >= CADENCE_MIN_SAMPLES
                && mean > 0.0
                && spread < SPREAD_MIN
                && spread / mean < SPREAD_RATIO;
        boolean repeatedDelay = state.cadenceSamples >= CADENCE_MIN_SAMPLES
                && longestRepeat >= REPEAT_RUN
                && spread < REPEAT_SPREAD;

        if (machineEven || repeatedDelay) {
            String shape = machineEven
                    ? "perfectly even"
                    : "the same delay " + (int) longestRepeat + " times running";
            context.flag("click cadence with " + shape + " at " + round(cps) + " cps", evidence, 6.0);
            state.perfectTicks = 0;
            state.cadenceSamples = 0;
            return;
        }

        if (state.perfectTicks >= BURST_TICKS) {
            context.flag("machine perfect click cadence with cps " + round(cps), evidence, 6.0);
            state.perfectTicks = 0;
        } else {
            context.flag("click rate of " + round(cps) + " cps exceeds " + maximum, evidence,
                    Math.min((cps - maximum) * 2.0, 8.0));
        }
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (ClickerState) state(context.player());
        if (state != null) {
            state.perfectTicks = 0;
        }
    }

    public static int longestRepeatRun(long[] values) {
        int best = 0;
        int run = 0;
        long previous = Long.MIN_VALUE;
        for (long value : values) {
            if (value == previous) {
                run++;
            } else {
                run = 1;
                previous = value;
            }
            if (run > best) {
                best = run;
            }
        }
        return best;
    }

    private static double mean(long[] values) {
        long total = 0L;
        for (long value : values) {
            total += value;
        }
        return values.length == 0 ? 0.0 : (double) total / values.length;
    }

    private static double standardDeviation(long[] values, double mean) {
        if (values.length < 2) {
            return 0.0;
        }
        double sum = 0.0;
        for (long value : values) {
            double delta = value - mean;
            sum += delta * delta;
        }
        return Math.sqrt(sum / (values.length - 1));
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class ClickerState {

        private final java.util.ArrayList<Long> samples = new java.util.ArrayList<>(64);
        private int perfectTicks;
        private int cadenceSamples;
    }
}
