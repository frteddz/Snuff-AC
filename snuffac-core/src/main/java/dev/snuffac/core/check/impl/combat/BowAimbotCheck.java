package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.MathUtil;
import java.util.Map;
import java.util.Set;

public final class BowAimbotCheck implements Check {

    public static final double MIN_SWING_YAW = 8.0;
    public static final int REQUIRED_SWINGS = 4;
    public static final long WINDOW_NANOS = 1_500_000_000L;
    public static final double MAX_DEGREES_PER_TICK = 180.0;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ARM_ANIMATION, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "bowaimbot";
    }

    @Override
    public String name() {
        return "BowAimbot";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects a bow drawn and released repeatedly at a fixed aim correction, which is "
                + "the BowAimbot cheat.";
    }

    @Override
    public Object createState() {
        return new BowAimbotState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (BowAimbotState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        if (player.equipment().held() != dev.snuffac.core.player.EquipmentState.HeldKind.BOW) {
            state.reset();
            return;
        }
        if (player.movement().ticksSinceTeleport() <= 2) {
            state.reset();
            return;
        }

        if (packet instanceof MovementPacket movement) {
            if (movement.rotationChanged()) {
                state.lastRotationNanos = movement.arrivalNanos();
            }
            return;
        }
        if (!(packet instanceof dev.snuffac.core.packet.ArmAnimationPacket)) {
            return;
        }

        long now = packet.arrivalNanos();
        if (state.lastSwingNanos != 0L) {
            long gap = now - state.lastSwingNanos;
            if (gap > WINDOW_NANOS) {
                state.corrections.clear();
            }
        }

        double yaw = player.movement().yaw();
        if (state.lastYaw != 0.0f) {
            double delta = Math.abs(MathUtil.deltaDegrees(state.lastYaw, yaw));
            if (delta >= MIN_SWING_YAW && delta <= MAX_DEGREES_PER_TICK) {
                state.corrections.add(Math.round(delta * 100.0) / 100.0);
                if (state.corrections.size() > REQUIRED_SWINGS * 2) {
                    state.corrections.remove(0);
                }
                if (state.corrections.size() >= REQUIRED_SWINGS) {
                    double spread = spread(state.corrections);
                    if (spread < 0.75) {
                        Map<String, Object> evidence = context.newEvidence();
                        evidence.put("corrections", state.corrections);
                        evidence.put("spread", Math.round(spread * 100.0) / 100.0);
                        evidence.put("samples", state.corrections.size());
                        evidence.put("minimumCorrection", MIN_SWING_YAW);
                        evidence.put("attacks", player.combat().attacks());
                        evidence.put("rotationToSwing",
                                state.lastRotationNanos == 0L ? -1L
                                        : (now - state.lastRotationNanos) / 1_000_000L);
                        context.flag("bow aim correction of " + Math.round(delta * 100.0) / 100.0
                                + " degrees repeated " + state.corrections.size()
                                + " times with under a degree of spread", evidence, 6.0);
                        state.reset();
                        return;
                    }
                }
            } else if (delta < MIN_SWING_YAW) {
                state.corrections.clear();
            }
        }
        state.lastYaw = (float) yaw;
        state.lastSwingNanos = now;
    }

    public static double spread(java.util.List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double mean = 0.0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.size();
        double total = 0.0;
        for (double value : values) {
            total += Math.abs(value - mean);
        }
        return total / values.size();
    }

    static final class BowAimbotState {

        private final java.util.ArrayList<Double> corrections = new java.util.ArrayList<>();
        private float lastYaw;
        private long lastSwingNanos;
        private long lastRotationNanos;

        private void reset() {
            corrections.clear();
            lastYaw = 0.0f;
            lastSwingNanos = 0L;
            lastRotationNanos = 0L;
        }
    }
}