package dev.snuffac.core.packet;

public record ChunkPacket(long arrivalNanos, int chunkX, int chunkZ) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.CHUNK;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.OUTBOUND;
    }
}
