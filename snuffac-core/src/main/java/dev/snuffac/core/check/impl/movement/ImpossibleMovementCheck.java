package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class ImpossibleMovementCheck extends AbstractMovementCheck {

    private static final double WORLD_BORDER = 30_000_000.0;
    private static final double MAX_COORDINATE_DELTA = 100.0;
    private static final double MIN_PLAUSIBLE_VERTICAL_DELTA = -100.0;
    private static final double MAX_PITCH = 90.0;
    private static final double MAX_YAW = 1.0E7;

    @Override
    public String key() {
        return "impossiblemovement";
    }

    @Override
    public String name() {
        return "ImpossibleMovement";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Rejects movement packets with impossible coordinates, deltas or rotations.";
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof MovementPacket movement)) {
            return;
        }

        var player = context.player();
        var movementState = player.movement();
        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        Vec3d position = movement.position();

        if (!validNumber(position.x()) || !validNumber(position.y()) || !validNumber(position.z())) {
            flagInvalid(context, "non finite position", Map.of(
                    "x", position.x(),
                    "y", position.y(),
                    "z", position.z()));
            return;
        }
        if (Math.abs(position.x()) > WORLD_BORDER || Math.abs(position.z()) > WORLD_BORDER
                || Math.abs(position.y()) > WORLD_BORDER) {
            flagInvalid(context, "position outside world border", Map.of(
                    "x", position.x(),
                    "y", position.y(),
                    "z", position.z()));
            return;
        }
        if (!validNumber(movement.yaw()) || !validNumber(movement.pitch())
                || Math.abs(movement.yaw()) > MAX_YAW || Math.abs(movement.pitch()) > MAX_PITCH) {
            flagInvalid(context, "invalid rotation", Map.of(
                    "yaw", movement.yaw(),
                    "pitch", movement.pitch(),
                    "maxYaw", MAX_YAW,
                    "maxPitch", MAX_PITCH));
            return;
        }
        if (movement.positionChanged() && movementState.lastPositionValid()) {
            Vec3d delta = movementState.delta();
            double horizontal = delta.horizontalLength();
            if (!validNumber(horizontal) || horizontal > MAX_COORDINATE_DELTA) {
                flagInvalid(context, "impossible position delta", Map.of(
                        "deltaX", delta.x(),
                        "deltaY", delta.y(),
                        "deltaZ", delta.z(),
                        "previousY", movementState.lastPosition().y(),
                        "currentY", position.y()));
                return;
            }
            if (delta.y() < MIN_PLAUSIBLE_VERTICAL_DELTA) {
                flagInvalid(context, "vertical delta beyond any plausible cause", Map.of(
                        "deltaY", delta.y(),
                        "minimum", MIN_PLAUSIBLE_VERTICAL_DELTA,
                        "fallDistance", movementState.fallDistance()));
            }
        }
    }

    private static void flagInvalid(CheckContext context, String reason, Map<String, Object> extra) {
        var evidence = context.newEvidence();
        evidence.put("reason", reason);
        evidence.putAll(extra);
        context.flagImmediately(reason, evidence);
    }

    private static boolean validNumber(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
