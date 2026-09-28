package dev.snuffac.core.packet;

import dev.snuffac.api.Vec3d;

public record MovementPacket(
        long arrivalNanos,
        boolean positionChanged,
        boolean rotationChanged,
        Vec3d position,
        float yaw,
        float pitch,
        boolean onGround,
        boolean horizontalCollision
) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.MOVEMENT;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }

    public boolean rotationOnly() {
        return !positionChanged && rotationChanged;
    }

    public boolean flagsOnly() {
        return !positionChanged && !rotationChanged;
    }
}
