package dev.snuffac.core.packet;

public record ServerRespawnPacket(long arrivalNanos) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.SERVER_RESPAWN;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.OUTBOUND;
    }
}
