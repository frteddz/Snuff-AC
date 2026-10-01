package dev.snuffac.core.packet;

public record WindowClickPacket(
        long arrivalNanos,
        int windowId,
        int slot,
        int button,
        int mode) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.INVENTORY_CLICK;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
