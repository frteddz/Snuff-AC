package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class DriftCheck implements Check {

    public static final int MIN_SAMPLES = 40;
    public static final double MIN_DRIFT = 0.0025;
    public static final double MAX_SLOPE = 0.02;
    public static final int REQUIRED_CONSECUTIVE = 12;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "drift";
    }

    @Override
    public String name() {
        return "PositionDrift";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Detects a sustained constant per tick offset between the server prediction and the client position, which is how several cheats disguise their position edits.";
    }

    @Override
    public Object createState() {
        return new DriftState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement) || !movement.positionChanged()) {
            return;
        }
        var state = (DriftState) state(context.player());
        if (state == null) {
            return;
        }

        var graceMovement = context.player().movement();
        if (dev.snuffac.core.prediction.PredictionGraces.windChargeActive(graceMovement)) {
            return;
        }
        var player = context.player();
        var movementState = player.movement();

        if (!player.worldCache().chunkLoaded() || movementState.ticksSinceTeleport() <= 3
                || movementState.ticksSinceVehicle() <= 2
                || movementState.inWaterOrLava()
                || movementState.onClimbable()) {
            state.reset();
            return;
        }

        if (!movementState.onGround()) {
            state.reset();
            return;
        }
        Vec3d offset = movementState.observedOffset();
        if (offset.length() < 1.0E-6) {
            state.samples = 0;
            return;
        }

        state.samples++;
        state.driftSum = state.driftSum.add(offset);
        state.driftCount++;

        if (state.driftCount > 1) {
            Vec3d mean = state.driftSum.multiply(1.0 / state.driftCount);
            state.meanDrift = mean;
            state.meanSamples++;
            Vec3d delta = mean.subtract(state.lastMean);
            if (delta.length() < MAX_SLOPE) {
                state.stable++;
            } else {
                state.stable = 0;
            }
            state.lastMean = mean;
        }

        if (state.meanSamples < MIN_SAMPLES) {
            return;
        }
        if (state.meanDrift.length() < MIN_DRIFT) {
            state.stable = 0;
            return;
        }
        if (state.stable < REQUIRED_CONSECUTIVE) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("meanDriftX", Math.round(state.meanDrift.x() * 100000.0) / 100000.0);
        evidence.put("meanDriftY", Math.round(state.meanDrift.y() * 100000.0) / 100000.0);
        evidence.put("meanDriftZ", Math.round(state.meanDrift.z() * 100000.0) / 100000.0);
        evidence.put("driftMagnitude", Math.round(state.meanDrift.length() * 100000.0) / 100000.0);
        evidence.put("samples", state.meanSamples);
        evidence.put("stableSamples", state.stable);
        context.flag("sustained position drift of "
                + Math.round(state.meanDrift.length() * 100000.0) / 100000.0 + " blocks", evidence, 6.0);
        state.reset();
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (DriftState) state(context.player());
        if (state != null && context.player().movement().onGround()) {
            state.stable = 0;
        }
    }

    static final class DriftState {

        private int samples;
        private int driftCount;
        private int meanSamples;
        private int stable;
        private Vec3d driftSum = Vec3d.ZERO;
        private Vec3d meanDrift = Vec3d.ZERO;
        private Vec3d lastMean = Vec3d.ZERO;

        private void reset() {
            samples = 0;
            driftCount = 0;
            meanSamples = 0;
            stable = 0;
            driftSum = Vec3d.ZERO;
            meanDrift = Vec3d.ZERO;
            lastMean = Vec3d.ZERO;
        }
    }
}
