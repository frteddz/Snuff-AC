package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.MathUtil;
import java.util.Map;
import java.util.Set;

public final class KillAuraCheck implements Check {

    public static final double MAX_OFF_ANGLE = 90.0;
    public static final int MULTI_TARGET_LIMIT = 3;
    private static final long WINDOW_MILLIS = 2000L;
    private static final int MIN_TARGETS = 4;
    private static final double MIN_SNAP_YAW = 12.0;
    private static final int REQUIRED_SNAPS = 6;
    public static final double MIN_SWITCH_ANGLE = 40.0;
    public static final int SWITCH_LIMIT = 3;
    public static final double FOV_LIMIT = 110.0;
    private static final int BEHIND_LIMIT = 3;
    private static final int LINEAR_MIN_SAMPLES = 12;
    private static final double LINEAR_TOLERANCE = 0.12;
    private static final double JITTER_FLOOR = 0.35;
    private static final int ROTATION_WINDOW = 24;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "killaura";
    }

    @Override
    public String name() {
        return "KillAura";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects attacking multiple entities in rotation, attacks on targets outside the field "
                + "of view or behind the player, and rotation that is linear or free of the jitter a "
                + "real mouse produces.";
    }

    @Override
    public Object createState() {
        return new AuraState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (packet instanceof dev.snuffac.core.packet.MovementPacket) {
            return;
        }
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }
        postAttack(context, attack);
        var state = (AuraState) state(context.player());
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        state.recent.removeIf(entry -> now - entry.timestamp > WINDOW_MILLIS);
        if (!state.recent.isEmpty() && state.recent.peekLast().targetId() != attack.targetId()) {
            long gap = now - state.recent.peekLast().timestamp;
            double separation = state.lastTargetAngle;
            if (gap <= 400L && separation >= MIN_SWITCH_ANGLE) {
                state.switches++;
                if (state.switches >= SWITCH_LIMIT) {
                    Map<String, Object> switchEvidence = context.newEvidence();
                    switchEvidence.put("switches", state.switches);
                    switchEvidence.put("separationDegrees", round(separation));
                    switchEvidence.put("gapMillis", gap);
                    switchEvidence.put("distinctTargets",
                            state.recent.stream().map(TargetEntry::targetId).distinct().count());
                    context.preventAttack("target switch of " + round(separation) + " degrees in "
                            + gap + "ms");
                    context.flag("switched between targets " + round(separation)
                            + " degrees apart within " + gap + "ms", switchEvidence, 7.0);
                    state.switches = 0;
                    state.recent.clear();
                    return;
                }
            } else {
                state.switches = 0;
            }
            state.lastTargetAngle = separation;
        } else if (state.recent.isEmpty()) {
            state.lastTargetAngle = 0.0;
        }
        state.recent.add(new TargetEntry(attack.targetId(), now));

        accountFieldOfView(context, attack, state);
        accountRotationPattern(context, attack, state);

        long distinct = state.recent.stream().map(TargetEntry::targetId).distinct().count();
        if (distinct >= MIN_TARGETS) {
            Map<String, Object> evidence = context.newEvidence();
            evidence.put("distinctTargets", distinct);
            evidence.put("window", WINDOW_MILLIS);
            evidence.put("recentTargets", state.recent.size());
            context.flag("attacked " + distinct + " distinct targets within " + WINDOW_MILLIS + "ms", evidence, 6.0);
            state.recent.clear();
        }

        double yawBefore = state.lastAttackYaw;
        double rotationDelta = Math.abs(MathUtil.deltaDegrees(yawBefore, attack.yaw()));
        if (rotationDelta >= MIN_SNAP_YAW) {
            state.snaps++;
        } else {
            state.snaps = 0;
        }
        state.lastAttackYaw = attack.yaw();

        if (state.snaps < REQUIRED_SNAPS) {
            return;
        }

        Map<String, Object> snapEvidence = context.newEvidence();
        snapEvidence.put("snaps", state.snaps);
        snapEvidence.put("lastRotationDelta", round(rotationDelta));
        snapEvidence.put("attackCps", context.player().combat().attacks());
        context.flag("repeated aim snapping before attacking", snapEvidence, 5.0);
        state.snaps = 0;
    }

    private void accountFieldOfView(CheckContext context, AttackPacket attack, AuraState state) {
        var environment = context.player().combatEnvironment();
        var target = environment.byId(attack.targetId());
        if (target == null) {
            return;
        }
        double offAngle = angleFromFacing(context, target.position());
        if (offAngle <= FOV_LIMIT) {
            state.behindTicks = 0;
            return;
        }

        state.behindTicks++;
        if (state.behindTicks < BEHIND_LIMIT) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "field of view");
        evidence.put("offAngle", round(offAngle));
        evidence.put("limit", FOV_LIMIT);
        evidence.put("behindTicks", state.behindTicks);
        evidence.put("targetId", attack.targetId());
        evidence.put("targetDistance", round(
                context.player().position().distanceTo(target.position())));

        context.preventAttack("attack " + round(offAngle) + " degrees off target");
        context.flag("attacked a target " + round(offAngle) + " degrees away from facing direction",
                evidence, 7.0);
        state.behindTicks = 0;
    }

    private void accountRotationPattern(CheckContext context, AttackPacket attack, AuraState state) {
        var window = state.rotation;
        window.offer(Math.abs(MathUtil.deltaDegrees(state.previousYaw, attack.yaw())));
        state.previousYaw = attack.yaw();

        if (window.size() < LINEAR_MIN_SAMPLES) {
            return;
        }

        double[] deltas = window.recentDeltas();
        double jitter = meanAbsoluteDeviation(deltas);
        if (jitter < JITTER_FLOOR) {
            return;
        }
        if (!isLinear(deltas)) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "rotation pattern");
        evidence.put("samples", deltas.length);
        evidence.put("jitter", round(jitter));
        evidence.put("gridConstant", round(window.constant()));
        evidence.put("snaps", state.snaps);

        context.flag("rotation between attacks is perfectly linear with no mouse jitter", evidence, 8.0);
        window.clear();
        state.snaps = 0;
    }

    public static double meanAbsoluteDeviation(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double mean = 0.0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.length;
        double total = 0.0;
        for (double value : values) {
            total += Math.abs(value - mean);
        }
        return total / values.length;
    }

    public static boolean isLinear(double[] values) {
        if (values.length < 4) {
            return false;
        }
        double first = values[0];
        double last = values[values.length - 1];
        if (Math.abs(last - first) < 1.0E-6) {
            return false;
        }
        double span = values.length - 1;
        for (int i = 0; i < values.length; i++) {
            double expected = first + (last - first) * (i / span);
            if (Math.abs(values[i] - expected) > LINEAR_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (AuraState) state(context.player());
        if (state != null) {
            state.snaps = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private record TargetEntry(int targetId, long timestamp) {
    }

    private void postAttack(CheckContext context, AttackPacket attack) {
        var combat = context.player().combat();
        var environment = context.player().combatEnvironment();
        var target = environment.byId(attack.targetId());
        double offAngle = 0.0;
        if (target != null) {
            offAngle = angleFromFacing(context, target.position());
        }
        long movementNanos = combat.lastMovementNanos();
        long gapNanos = movementNanos == 0L ? -1L : attack.arrivalNanos() - movementNanos;

        Map<String, Object> timing = context.newEvidence();
        timing.put("gapNanos", gapNanos);
        timing.put("movementFirst", gapNanos >= 0L && gapNanos <= 2_000_000L);
        timing.put("offAngle", round(offAngle));
        timing.put("distinctTargets", combat.distinctTargetsInWindow());
        combat.observeAttack(attack.arrivalNanos(), attack.targetId(), offAngle);

        boolean moveThenAttack = gapNanos >= 0L && gapNanos <= 2_000_000L;
        int targets = combat.distinctTargetsInWindow();
        if (moveThenAttack && targets >= MULTI_TARGET_LIMIT) {
            context.flag("attacked " + targets + " distinct targets within one second, "
                    + "each immediately after a movement packet", timing, 7.0);
            combat.clearWindow();
            return;
        }
        if (offAngle > MAX_OFF_ANGLE && target != null) {
            context.preventAttack("attack " + round(offAngle) + " degrees off target");
            context.flag("attacked a target " + round(offAngle)
                    + " degrees away from facing direction", timing, 6.0);
        }
    }

    private static double angleFromFacing(CheckContext context, dev.snuffac.api.Vec3d target) {
        var player = context.player();
        var eye = dev.snuffac.core.combat.ReachResolver.eyePosition(
                player.position(), player.movement().sneaking());
        double dx = target.x() - eye.x();
        double dy = target.y() - eye.y();
        double dz = target.z() - eye.z();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-6) {
            return 0.0;
        }
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double facingYaw = player.movement().yaw();
        double difference = Math.abs(normaliseAngle(targetYaw - facingYaw));
        return difference > 180.0 ? 360.0 - difference : difference;
    }

    private static double normaliseAngle(double degrees) {
        double value = degrees % 360.0;
        if (value < 0.0) {
            value += 360.0;
        }
        return value;
    }

    static final class AuraState {

        private int switches;
        private double lastTargetAngle;
        private final java.util.ArrayDeque<TargetEntry> recent = new java.util.ArrayDeque<>();
        private double lastAttackYaw;
        private int snaps;
        private int behindTicks;
        private double previousYaw;
        private final RotationWindow rotation = new RotationWindow();
    }

    static final class RotationWindow {

        private final double[] deltas = new double[ROTATION_WINDOW];
        private int count;
        private final GcdAnalysis.Window grid = new GcdAnalysis.Window();

        void offer(double delta) {
            grid.offer(delta);
            if (count < ROTATION_WINDOW) {
                deltas[count++] = delta;
            } else {
                System.arraycopy(deltas, 1, deltas, 0, ROTATION_WINDOW - 1);
                deltas[ROTATION_WINDOW - 1] = delta;
            }
        }

        double[] recentDeltas() {
            double[] result = new double[count];
            System.arraycopy(deltas, 0, result, 0, count);
            return result;
        }

        double constant() {
            return grid.constant();
        }

        int size() {
            return count;
        }

        void clear() {
            count = 0;
            grid.clear();
        }
    }
}
