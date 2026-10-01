package dev.snuffac.core.packet;

public record HeldItemChangePacket(
        long arrivalNanos,
        int slot,
        boolean offhand) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.HELD_ITEM_CHANGE;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
