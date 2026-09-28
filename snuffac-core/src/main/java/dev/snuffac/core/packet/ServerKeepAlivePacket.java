package dev.snuffac.core.packet;

public record ServerKeepAlivePacket(long arrivalNanos, long payload) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.SERVER_KEEP_ALIVE;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.OUTBOUND;
    }
}
