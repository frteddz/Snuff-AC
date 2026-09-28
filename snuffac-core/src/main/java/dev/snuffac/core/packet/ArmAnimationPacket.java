package dev.snuffac.core.packet;

public record ArmAnimationPacket(long arrivalNanos, int handOrdinal) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.ARM_ANIMATION;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }
}
