package dev.snuffac.core.packet;

public record PlayerLoadedPacket(long arrivalNanos) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.PLAYER_LOADED;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
