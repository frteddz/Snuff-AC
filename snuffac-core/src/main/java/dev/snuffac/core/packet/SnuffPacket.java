package dev.snuffac.core.packet;

public interface SnuffPacket {

    PacketType type();

    PacketDirection direction();

    long arrivalNanos();
}
