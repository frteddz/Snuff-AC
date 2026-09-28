package dev.snuffac.core.check.impl.movement;

import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.physics.MovementEnvironment;
import dev.snuffac.core.player.MovementState;
import dev.snuffac.core.player.PlayerData;
import dev.snuffac.core.player.PlayerWorldCache;
import dev.snuffac.core.prediction.PredictionEngine;
import dev.snuffac.core.prediction.PredictionResult;
import dev.snuffac.core.tolerance.ToleranceModel;
import java.util.Set;

public abstract class AbstractMovementCheck implements Check {

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT);
    }

    protected static MovementState movement(CheckContext context) {
        return context.player().movement();
    }

    protected static PlayerWorldCache cache(CheckContext context) {
        return context.player().worldCache();
    }

    protected static MovementEnvironment environment(CheckContext context) {
        PlayerData player = context.player();
        return dev.snuffac.core.player.EnvironmentMapper.from(player.movement(), player.worldCache());
    }

    protected static ToleranceModel tolerance(CheckContext context) {
        return context.player().movement().tolerance();
    }

    protected static PredictionResult predict(
            CheckContext context,
            dev.snuffac.api.Vec3d actualDelta,
            MovementEnvironment environment) {
        PlayerData player = context.player();
        return PredictionEngine.predict(
                player.movement().velocity(), actualDelta, environment, player.movement().attributes());
    }

    protected static boolean canPredict(PlayerData player) {
        MovementState state = player.movement();
        PlayerWorldCache cache = player.worldCache();
        if (!cache.chunkLoaded()) {
            return false;
        }
        if (state.ticksSinceTeleport() <= 1) {
            return false;
        }
        if (state.ticksSinceVehicle() <= 2) {
            return false;
        }
        if (state.serverTeleportPending()) {
            return false;
        }
        return state.lastPositionValid();
    }

    protected static void applyPrediction(PlayerData player, PredictionResult result) {
        player.movement().predictedVelocity(result.predictedVelocity());
        player.movement().velocity(result.predictedVelocity());
        player.movement().lastOffset(result.offset());
    }

    protected static boolean isPositionUpdate(SnuffPacket packet) {
        return packet instanceof dev.snuffac.core.packet.MovementPacket movement && movement.positionChanged();
    }
}
