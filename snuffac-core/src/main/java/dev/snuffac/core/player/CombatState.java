package dev.snuffac.core.player;

import dev.snuffac.api.Vec3d;

public final class CombatState {

    private long attacks;
    private long lastAttackMillis;
    private int lastTargetId = -1;
    private Vec3d lastAttackPosition = Vec3d.ZERO;
    private Vec3d lastAttackCursor = Vec3d.ZERO;
    private float lastAttackYaw;
    private float lastAttackPitch;
    private int lastAttackHand = -1;
    private long lastMovementNanos;
    private long lastAttackNanos;
    private int distinctTargetsInWindow;
    private int lastTargetIdWindow = -1;
    private long windowStartNanos;
    private double worstOffAngle;
    private int distinctTargetsTotal;

    private long swings;
    private long lastSwingMillis;

    private double lastReach;
    private double bestReach;
    private long reachSamples;
    private double reachAverage;
    private double reachDeviation;
    private double lastReachMomentum;
    private boolean attacking;

    private double lastGcd;
    private long lastRotationMillis;
    private float lastAimYaw;
    private float lastAimPitch;
    private int rotationSamples;
    private double rotationVariance;
    private double lastDeltaYaw;
    private double lastDeltaPitch;

    private double bestAttackCps = 0.0;
    private int currentAttackCps;

    private final long[] attackTimestamps = new long[64];

    public long lastDamageMillis() {
        return lastDamageMillis;
    }

    public void recordDamage(long millis) {
        lastDamageMillis = millis;
        damageEvents++;
    }

    public long damageEvents() {
        return damageEvents;
    }

    public long attacks() {
        return attacks;
    }

    public long lastAttackMillis() {
        return lastAttackMillis;
    }

    public void recordAttack(long millis, int targetId, Vec3d position, Vec3d cursor, float yaw, float pitch, int hand) {
        this.attacks++;
        this.lastAttackMillis = millis;
        this.lastTargetId = targetId;
        this.lastAttackPosition = position;
        this.lastAttackCursor = cursor;
        this.lastAttackYaw = yaw;
        this.lastAttackPitch = pitch;
        this.lastAttackHand = hand;
    }

    public int lastTargetId() {
        return lastTargetId;
    }

    public Vec3d lastAttackPosition() {
        return lastAttackPosition;
    }

    public Vec3d lastAttackCursor() {
        return lastAttackCursor;
    }

    public float lastAttackYaw() {
        return lastAttackYaw;
    }

    public float lastAttackPitch() {
        return lastAttackPitch;
    }

    public int lastAttackHand() {
        return lastAttackHand;
    }

    private long lastDamageMillis;
    private long damageEvents;

    public long swings() {
        return swings;
    }

    public void recordSwing(long millis) {
        this.swings++;
        this.lastSwingMillis = millis;
    }

    public long lastSwingMillis() {
        return lastSwingMillis;
    }

    public double lastReach() {
        return lastReach;
    }

    public double bestReach() {
        return bestReach;
    }

    public long reachSamples() {
        return reachSamples;
    }

    public double reachAverage() {
        return reachAverage;
    }

    public double reachDeviation() {
        return reachDeviation;
    }

    public double lastReachMomentum() {
        return lastReachMomentum;
    }

    public void recordReach(double reach) {
        this.lastReach = reach;
        this.reachSamples++;
        if (reach > bestReach) {
            bestReach = reach;
        }
        double delta = reach - reachAverage;
        reachAverage += delta / reachSamples;
        double delta2 = reach - reachAverage;
        reachDeviation = Math.sqrt(((reachSamples - 1) * reachDeviation * reachDeviation + delta * delta2) / reachSamples);
        this.lastReachMomentum = reach - lastReach;
    }

    public boolean attacking() {
        return attacking;
    }

    public void attacking(boolean value) {
        this.attacking = value;
    }

    public double lastGcd() {
        return lastGcd;
    }

    public long lastRotationMillis() {
        return lastRotationMillis;
    }

    public void recordRotation(long millis, float yaw, float pitch, double gcd, double deltaYaw, double deltaPitch) {
        this.lastRotationMillis = millis;
        this.lastAimYaw = yaw;
        this.lastAimPitch = pitch;
        this.lastGcd = gcd;
        this.rotationSamples++;
        if (rotationSamples > 2) {
            rotationVariance *= 0.9;
            rotationVariance += Math.abs(gcd - lastGcd) * 0.1;
        }
        this.lastDeltaYaw = deltaYaw;
        this.lastDeltaPitch = deltaPitch;
    }

    public float lastAimYaw() {
        return lastAimYaw;
    }

    public float lastAimPitch() {
        return lastAimPitch;
    }

    public int rotationSamples() {
        return rotationSamples;
    }

    public double rotationVariance() {
        return rotationVariance;
    }

    public double lastDeltaYaw() {
        return lastDeltaYaw;
    }

    public double lastDeltaPitch() {
        return lastDeltaPitch;
    }

    public double bestAttackCps() {
        return bestAttackCps;
    }

    public int currentAttackCps() {
        return currentAttackCps;
    }

    public void recordAttackTime(long millis) {
        int slot = (int) (attacks & 0x3F);
        attackTimestamps[slot] = millis;
    }

    public int attacksInWindow(long now, long windowMillis) {
        int count = 0;
        long threshold = now - windowMillis;
        for (long timestamp : attackTimestamps) {
            if (timestamp > threshold && timestamp > 0L) {
                count++;
            }
        }
        return count;
    }

    public long[] attackTimestamps() {
        return attackTimestamps;
    }

    public void observeMovement(long arrivalNanos) {
        this.lastMovementNanos = arrivalNanos;
    }

    public long lastMovementNanos() {
        return lastMovementNanos;
    }

    public void observeAttack(long arrivalNanos, int targetId, double offAngle) {
        this.lastAttackNanos = arrivalNanos;
        this.worstOffAngle = Math.max(this.worstOffAngle, offAngle);
        this.distinctTargetsTotal++;
        if (windowStartNanos == 0L || arrivalNanos - windowStartNanos > 1_000_000_000L) {
            windowStartNanos = arrivalNanos;
            distinctTargetsInWindow = 1;
            lastTargetIdWindow = targetId;
            return;
        }
        if (targetId != lastTargetIdWindow) {
            lastTargetIdWindow = targetId;
            distinctTargetsInWindow++;
        }
    }

    public long lastAttackNanos() {
        return lastAttackNanos;
    }

    public int distinctTargetsInWindow() {
        return distinctTargetsInWindow;
    }

    public double worstOffAngle() {
        return worstOffAngle;
    }

    public void clearWindow() {
        this.distinctTargetsInWindow = 0;
        this.worstOffAngle = 0.0;
        this.lastTargetIdWindow = -1;
    }

    public void reset() {
        attacks = 0;
        swings = 0;
        lastTargetId = -1;
        bestReach = 0.0;
        reachSamples = 0;
        reachAverage = 0.0;
        reachDeviation = 0.0;
        rotationSamples = 0;
        rotationVariance = 0.0;
        java.util.Arrays.fill(attackTimestamps, 0L);
    }
}
