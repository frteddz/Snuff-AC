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

public final class AimCheck implements Check {

    private static final double MIN_DELTA_YAW = 0.05;
    private static final double MIN_DELTA_PITCH = 0.05;
    private static final int REQUIRED_SMOOTH = 12;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "aim";
    }

    @Override
    public String name() {
        return "Aim";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects rotation sequences that quantise to a constant step, the signature of aim assistance.";
    }

    @Override
    public Object createState() {
        return new AimState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.rotationChanged()) {
            return;
        }
        var state = (AimState) state(context.player());
        if (state == null) {
            return;
        }

        double deltaYaw = Math.abs(MathUtil.deltaDegrees(state.lastYaw, movement.yaw()));
        double deltaPitch = Math.abs(state.lastPitch - movement.pitch());

        if (deltaYaw < MIN_DELTA_YAW && deltaPitch < MIN_DELTA_PITCH) {
            state.lastYaw = movement.yaw();
            state.lastPitch = movement.pitch();
            return;
        }

        double divisor = greatestCommonDivisor(deltaYaw, deltaPitch);
        if (divisor <= 0.0) {
            state.lastYaw = movement.yaw();
            state.lastPitch = movement.pitch();
            state.constantSteps = 0;
            return;
        }

        boolean sameStep = state.lastDivisor > 0.0
                && Math.abs(divisor - state.lastDivisor) < 1.0E-6;
        state.lastDivisor = divisor;
        if (sameStep) {
            state.constantSteps++;
        } else {
            state.constantSteps = 0;
        }

        state.lastYaw = movement.yaw();
        state.lastPitch = movement.pitch();

        if (state.constantSteps < REQUIRED_SMOOTH) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("divisor", round(divisor));
        evidence.put("deltaYaw", round(deltaYaw));
        evidence.put("deltaPitch", round(deltaPitch));
        evidence.put("constantSteps", state.constantSteps);
        context.flag("constant rotation step of " + round(divisor) + " over "
                + state.constantSteps + " rotations", evidence, 5.0);
        state.constantSteps = 0;
    }

    static double greatestCommonDivisor(double yaw, double pitch) {
        long a = Math.round(yaw * 1000.0);
        long b = Math.round(pitch * 1000.0);
        if (a <= 0L || b <= 0L) {
            return 0.0;
        }
        return MathUtil.gcd(a, b) / 1000.0;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class AimState {

        private float lastYaw;
        private float lastPitch;
        private double lastDivisor;
        private int constantSteps;
    }
}
