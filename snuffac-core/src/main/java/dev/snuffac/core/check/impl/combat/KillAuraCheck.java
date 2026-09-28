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

    private static final long WINDOW_MILLIS = 2000L;
    private static final int MIN_TARGETS = 4;
    private static final double MIN_SNAP_YAW = 12.0;
    private static final int REQUIRED_SNAPS = 6;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK);
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
        return "Detects attacking multiple entities in rotation and repeated instant aim snaps before attacking.";
    }

    @Override
    public Object createState() {
        return new AuraState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof AttackPacket attack)) {
            return;
        }
        var state = (AuraState) state(context.player());
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        state.recent.removeIf(entry -> now - entry.timestamp > WINDOW_MILLIS);
        state.recent.add(new TargetEntry(attack.targetId(), now));

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

    static final class AuraState {

        private final java.util.ArrayDeque<TargetEntry> recent = new java.util.ArrayDeque<>();
        private double lastAttackYaw;
        private int snaps;
    }
}
