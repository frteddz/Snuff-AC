package dev.snuffac.core.packet;

import dev.snuffac.api.Vec3d;

public record AttackPacket(
        long arrivalNanos,
        int targetId,
        Vec3d cursorPosition,
        boolean sneaking,
        float yaw,
        float pitch
) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.ATTACK;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
