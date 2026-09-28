package dev.snuffac.core.packet;

public record KeepAlivePacket(long arrivalNanos, long payload) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.KEEP_ALIVE;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
