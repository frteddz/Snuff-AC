package dev.snuffac.core.packet;

public record BlockBreakPacket(
        long arrivalNanos,
        BlockBreakAction action,
        long packedPosition,
        int blockFaceId,
        int sequence
) implements SnuffPacket {

    public enum BlockBreakAction {
        START,
        FINISH,
        CANCEL,
        ABORT
    }

    @Override
    public PacketType type() {
        return PacketType.BLOCK_BREAK;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
