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
    public static final int ACCELERATION_WINDOW = 10;
    public static final double ACCELERATION_TOLERANCE = 0.06;
    public static final double MIN_ACCELERATION = 0.12;
    public static final int REQUIRED_ACCELERATION = 4;

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
        return "Detects rotation sequences that quantise to a constant step, and rotation whose "
                + "acceleration between packets is identical, which a hand cannot produce.";
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

        state.pitchWindow.offer(deltaPitch);
        state.yawWindow.offer(deltaYaw);

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

        accountAcceleration(context, state, deltaYaw);

        if (state.constantSteps >= REQUIRED_SMOOTH) {
            Map<String, Object> smooth = context.newEvidence();
            smooth.put("divisor", round(divisor));
            smooth.put("deltaYaw", round(deltaYaw));
            smooth.put("deltaPitch", round(deltaPitch));
            smooth.put("constantSteps", state.constantSteps);
            context.flag("constant rotation step of " + round(divisor) + " over "
                    + state.constantSteps + " rotations", smooth, 5.0);
            state.constantSteps = 0;
        }

        double constant = state.pitchWindow.constant();
        if (constant > 0.0 && state.pitchWindow.size() >= 30
                && !GcdAnalysis.isMultipleOf(deltaPitch, constant)
                && deltaPitch > GcdAnalysis.MIN_DELTA * 4.0) {
            state.breaks++;
            if (state.breaks >= 3) {
                Map<String, Object> evidence = context.newEvidence();
                evidence.put("sensitivityConstant", round(constant));
                evidence.put("deltaPitch", round(deltaPitch));
                evidence.put("deltaYaw", round(deltaYaw));
                evidence.put("expectedMultiple", round(deltaPitch / constant));
                evidence.put("breaks", state.breaks);
                evidence.put("samples", state.pitchWindow.size());
                evidence.put("mouseConstant", GcdAnalysis.DEFAULT_CONSTANT);
                context.flag("pitch delta of " + round(deltaPitch)
                        + " is not a multiple of the mouse constant " + round(constant), evidence, 6.0);
                state.breaks = 0;
                state.pitchWindow.clear();
                state.yawWindow.clear();
            }
        } else if (GcdAnalysis.isMultipleOf(deltaPitch, constant)) {
            state.breaks = 0;
        }
    }

    private static void accountAcceleration(CheckContext context, AimState state, double deltaYaw) {
        if (deltaYaw < MIN_DELTA_YAW) {
            state.accelerations.clear();
            state.lastDelta = 0.0;
            state.constantAccelerations = 0;
            return;
        }

        if (state.lastDelta > 0.0) {
            double acceleration = Math.abs(deltaYaw - state.lastDelta);
            state.accelerations.add(acceleration);
            if (state.accelerations.size() > ACCELERATION_WINDOW) {
                state.accelerations.remove(0);
            }
            if (acceleration < ACCELERATION_TOLERANCE && deltaYaw > MIN_ACCELERATION) {
                state.constantAccelerations++;
                if (state.constantAccelerations >= REQUIRED_ACCELERATION) {
                    Map<String, Object> evidence = context.newEvidence();
                    evidence.put("mode", "acceleration");
                    evidence.put("accelerations", state.accelerations);
                    evidence.put("constantAccelerations", state.constantAccelerations);
                    evidence.put("tolerance", ACCELERATION_TOLERANCE);
                    evidence.put("lastDeltaYaw", Math.round(deltaYaw * 10000.0) / 10000.0);
                    context.flag("rotation acceleration identical across "
                            + state.constantAccelerations + " packets", evidence, 6.0);
                    state.accelerations.clear();
                    state.constantAccelerations = 0;
                }
            } else if (acceleration >= ACCELERATION_TOLERANCE) {
                state.constantAccelerations = 0;
            }
        }
        state.lastDelta = deltaYaw;
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
        private int breaks;
        private final java.util.ArrayList<Double> accelerations =
                new java.util.ArrayList<>(ACCELERATION_WINDOW);
        private double lastDelta;
        private int constantAccelerations;
        private final GcdAnalysis.Window pitchWindow = new GcdAnalysis.Window();
        private final GcdAnalysis.Window yawWindow = new GcdAnalysis.Window();
    }
}
