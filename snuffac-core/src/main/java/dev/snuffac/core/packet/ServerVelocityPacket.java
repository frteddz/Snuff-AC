package dev.snuffac.core.packet;

import dev.snuffac.api.Vec3d;

public record ServerVelocityPacket(long arrivalNanos, int entityId, Vec3d velocity) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.SERVER_VELOCITY;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.OUTBOUND;
    }
}
