package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.packet.ServerVelocityPacket;
import dev.snuffac.core.tolerance.ToleranceSource;
import java.util.Map;
import java.util.Set;

public final class VelocityCheck extends AbstractMovementCheck {

    private static final int EXPIRY_TICKS = 40;
    private static final double MIN_MOTION_RATIO = 0.02;
    private static final double MIN_ABSOLUTE_DROPPED = 0.06;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT, PacketType.SERVER_VELOCITY);
    }

    @Override
    public String key() {
        return "velocity";
    }

    @Override
    public String name() {
        return "Velocity";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects clients that discard or ignore knockback that the server has just applied.";
    }

    @Override
    public Object createState() {
        return new VelocityState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (VelocityState) state(context.player());
        if (state == null) {
            return;
        }

        var graceMovement = context.player().movement();
        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                context.player(), context.tps(), context.ping())) {
            return;
        }
        if (dev.snuffac.core.prediction.PredictionGraces.windChargeActive(graceMovement)) {
            return;
        }

        if (packet instanceof ServerVelocityPacket velocity) {
            if (velocity.entityId() != context.player().entityId()) {
                return;
            }
            if (velocity.velocity().lengthSquared() < 1.0E-6) {
                return;
            }
            state.pending = velocity.velocity();
            state.expiry = EXPIRY_TICKS;
            return;
        }

        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }

        if (state.pending == null) {
            return;
        }

        state.expiry--;
        if (state.expiry <= 0) {
            state.pending = null;
            return;
        }
        if (context.player().movement().ticksSinceTeleport() <= 1) {
            state.pending = null;
            return;
        }
        if (state.graceTicks > 0) {
            state.graceTicks--;
            return;
        }

        Vec3d applied = context.player().movement().delta();
        Vec3d expected = state.pending;

        double expectedLength = expected.length();
        double appliedLength = applied.length();

        Vec3d forgiveness = context.player().movement().tolerance()
                .toleranceFor(ToleranceSource.SERVER_KNOCKBACK);
        double forgivenX = Math.abs(forgiveness.x());
        double forgivenY = Math.abs(forgiveness.y());
        double forgivenZ = Math.abs(forgiveness.z());

        boolean droppedX = Math.abs(applied.x()) < expectedLength * MIN_MOTION_RATIO
                && Math.abs(applied.x()) < MIN_ABSOLUTE_DROPPED
                && Math.abs(expected.x()) - forgivenX > MIN_ABSOLUTE_DROPPED;
        boolean droppedY = Math.abs(applied.y()) < expectedLength * MIN_MOTION_RATIO
                && Math.abs(applied.y()) < MIN_ABSOLUTE_DROPPED
                && Math.abs(expected.y()) - forgivenY > MIN_ABSOLUTE_DROPPED;
        boolean droppedZ = Math.abs(applied.z()) < expectedLength * MIN_MOTION_RATIO
                && Math.abs(applied.z()) < MIN_ABSOLUTE_DROPPED
                && Math.abs(expected.z()) - forgivenZ > MIN_ABSOLUTE_DROPPED;

        if (!droppedX && !droppedY && !droppedZ) {
            state.graceTicks = 3;
            return;
        }

        state.droppedTicks++;
        if (state.droppedTicks < 2) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("expectedX", round(expected.x()));
        evidence.put("expectedY", round(expected.y()));
        evidence.put("expectedZ", round(expected.z()));
        evidence.put("appliedX", round(applied.x()));
        evidence.put("appliedY", round(applied.y()));
        evidence.put("appliedZ", round(applied.z()));
        evidence.put("axis", droppedX ? "x" : droppedY ? "y" : "z");
        evidence.put("droppedTicks", state.droppedTicks);

        context.flag("knockback dropped on " + (droppedX ? "x" : droppedY ? "y" : "z") + " axis", evidence, 7.0);
        state.droppedTicks = 0;
        state.pending = null;
        state.graceTicks = 3;
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (VelocityState) state(context.player());
        if (state == null) {
            return;
        }
        if (state.pending != null && state.expiry > 0) {
            state.expiry--;
            if (state.expiry <= 0) {
                state.pending = null;
                state.droppedTicks = 0;
            }
        }
        if (context.player().movement().ticksSinceTeleport() <= 1) {
            state.pending = null;
            state.droppedTicks = 0;
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class VelocityState {

        private Vec3d pending;
        private int expiry;
        private int droppedTicks;
        private int graceTicks;
    }
}
