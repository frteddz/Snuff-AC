package dev.snuffac.core.packet;

public record BlockPlacePacket(
        long arrivalNanos,
        long packedPosition,
        int faceId,
        float cursorX,
        float cursorY,
        float cursorZ,
        int handOrdinal,
        int sequence
) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.BLOCK_PLACE;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
