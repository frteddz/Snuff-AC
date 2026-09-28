package dev.snuffac.core.tolerance;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.MathUtil;
import java.util.EnumMap;
import java.util.Map;

public final class ToleranceModel {

    private final Map<ToleranceSource, Vec3d> contributions = new EnumMap<>(ToleranceSource.class);
    private final Map<ToleranceSource, Long> lastTriggeredTick = new EnumMap<>(ToleranceSource.class);

    private final double decayPerTick;
    private final double maxTotal;
    private final double carryOverCap;
    private final double carryOverRetention;

    private Vec3d carryOver = Vec3d.ZERO;
    private long currentTick;

    public ToleranceModel(
            double decayPerTick,
            double maxTotal,
            double carryOverCap,
            double carryOverRetention) {
        this.decayPerTick = decayPerTick;
        this.maxTotal = maxTotal;
        this.carryOverCap = carryOverCap;
        this.carryOverRetention = carryOverRetention;
    }

    public static ToleranceModel defaults() {
        return new ToleranceModel(0.35, 0.45, 1.0, 0.4);
    }

    public void advanceTick(long tick) {
        this.currentTick = tick;
        contributions.replaceAll((source, value) -> {
            if (value.isZero()) {
                return Vec3d.ZERO;
            }
            return value.multiply(decayPerTick);
        });
        contributions.entrySet().removeIf(entry -> entry.getValue().isZero());
        carryOver = carryOver.multiply(decayPerTick);
        if (carryOver.isZero()) {
            carryOver = Vec3d.ZERO;
        }
    }

    public void add(ToleranceSource source, Vec3d amount) {
        if (amount.isZero()) {
            return;
        }
        contributions.merge(source, amount, Vec3d::add);
        lastTriggeredTick.put(source, currentTick);
    }

    public void add(ToleranceSource source, double amount) {
        add(source, new Vec3d(amount, amount, amount));
    }

    public void addAxis(ToleranceSource source, double x, double y, double z) {
        add(source, new Vec3d(x, y, z));
    }

    public void clear(ToleranceSource source) {
        contributions.remove(source);
    }

    public void reset() {
        contributions.clear();
        lastTriggeredTick.clear();
        carryOver = Vec3d.ZERO;
    }

    public void beginLeniencyCarryOver(Vec3d previousOffset) {
        Vec3d scaled = previousOffset.multiply(carryOverRetention);
        double length = scaled.length();
        if (length > carryOverCap) {
            scaled = scaled.multiply(carryOverCap / length);
        }
        carryOver = carryOver.add(scaled);
        clampTotal();
    }

    public void consumeCarryOver() {
        carryOver = Vec3d.ZERO;
    }

    public Vec3d carryOver() {
        return carryOver;
    }

    public Vec3d total() {
        Vec3d sum = carryOver;
        for (Vec3d value : contributions.values()) {
            sum = sum.add(value);
        }
        return sum;
    }

    public Vec3d toleranceFor(ToleranceSource source) {
        Vec3d value = contributions.get(source);
        return value == null ? Vec3d.ZERO : value;
    }

    public long ticksSince(ToleranceSource source) {
        Long last = lastTriggeredTick.get(source);
        if (last == null) {
            return Long.MAX_VALUE;
        }
        return currentTick - last;
    }

    public boolean recentlyTriggered(ToleranceSource source, long window) {
        return ticksSince(source) <= window;
    }

    public void clampTotal() {
        if (contributions.isEmpty()) {
            return;
        }
        double length = total().length();
        if (length <= maxTotal || length < MathUtil.EPSILON) {
            return;
        }
        double factor = maxTotal / length;
        contributions.replaceAll((source, value) -> value.multiply(factor));
    }

    public Vec3d residual(Vec3d actual, Vec3d predicted) {
        Vec3d offset = actual.subtract(predicted);
        return offset.subtract(clampToBox(offset, toleranceBox()));
    }

    public static Vec3d clampToBox(Vec3d vector, ToleranceBox box) {
        return new Vec3d(
                MathUtil.clamp(vector.x(), -box.x(), box.x()),
                MathUtil.clamp(vector.y(), -box.y(), box.y()),
                MathUtil.clamp(vector.z(), -box.z(), box.z()));
    }

    public ToleranceBox toleranceBox() {
        Vec3d total = total();
        double x = Math.abs(total.x());
        double y = Math.abs(total.y());
        double z = Math.abs(total.z());
        return new ToleranceBox(x, y, z);
    }

    public record ToleranceBox(double x, double y, double z) {
    }
}
