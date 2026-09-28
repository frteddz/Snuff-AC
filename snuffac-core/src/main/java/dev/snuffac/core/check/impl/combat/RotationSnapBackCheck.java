package dev.snuffac.core.check.impl.combat;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.MathUtil;
import java.util.Map;
import java.util.Set;

public final class RotationSnapBackCheck implements Check {

    public static final double MIN_AIM_DELTA = 8.0;
    public static final double MIN_SNAP_BACK = 5.0;
    public static final long WINDOW_NANOS = 900_000_000L;
    public static final int REQUIRED = 2;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.ATTACK, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "rotationsnapback";
    }

    @Override
    public String name() {
        return "RotationSnapBack";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.COMBAT;
    }

    @Override
    public String description() {
        return "Detects a large aim rotation immediately before an attack followed by a reverse rotation immediately after, which no human input produces.";
    }

    @Override
    public Object createState() {
        return new SnapState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (SnapState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof MovementPacket movement) {
            if (!movement.rotationChanged()) {
                return;
            }
            float yaw = movement.yaw();
            float pitch = movement.pitch();
            long now = movement.arrivalNanos();

            if (state.attackNanos != 0L && now - state.attackNanos < WINDOW_NANOS && state.preAttackYawValid) {
                double aim = Math.abs(MathUtil.deltaDegrees(state.preAttackYaw, yaw));
                double back = Math.abs(MathUtil.deltaDegrees(state.attackYaw, yaw));
                if (aim >= MIN_AIM_DELTA && back >= MIN_SNAP_BACK) {
                    state.sequences++;
                    if (state.sequences >= REQUIRED) {
                        Map<String, Object> evidence = context.newEvidence();
                        evidence.put("aimDelta", Math.round(aim * 100.0) / 100.0);
                        evidence.put("snapBackDelta", Math.round(back * 100.0) / 100.0);
                        evidence.put("sequences", state.sequences);
                        evidence.put("afterAttackNanos", now - state.attackNanos);
                        context.flag("aim rotation and reverse snap back bracketing an attack", evidence, 9.0);
                        state.sequences = 0;
                    }
                }
            }
            state.lastYaw = yaw;
            state.lastPitch = pitch;
            state.preAttackYaw = state.lastYaw;
            state.preAttackYawValid = true;
            return;
        }

        if (packet instanceof AttackPacket) {
            state.attackNanos = packet.arrivalNanos();
            state.attackYaw = state.lastYaw;
            state.attackPitch = state.lastPitch;
            state.preAttackYaw = state.lastYaw;
            state.preAttackYawValid = true;
        }
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (SnapState) state(context.player());
        if (state != null && state.sequences > 0) {
            state.sequences--;
        }
    }

    static final class SnapState {

        private long attackNanos;
        private float attackYaw;
        private float attackPitch;
        private float preAttackYaw;
        private boolean preAttackYawValid;
        private float lastYaw;
        private float lastPitch;
        private int sequences;
    }
}
