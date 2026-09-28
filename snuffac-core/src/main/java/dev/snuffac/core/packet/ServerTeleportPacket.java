package dev.snuffac.core.packet;

import dev.snuffac.api.Vec3d;

public record ServerTeleportPacket(
        long arrivalNanos,
        Vec3d position,
        float yaw,
        float pitch,
        TeleportCause cause
) implements SnuffPacket {

    public enum TeleportCause {
        SPAWN,
        PORTAL,
        COMMAND,
        PLUGIN,
        SETBACK,
        UNKNOWN
    }

    @Override
    public PacketType type() {
        return PacketType.SERVER_TELEPORT;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.OUTBOUND;
    }

    public boolean isSetback() {
        return cause == TeleportCause.SETBACK;
    }
}
