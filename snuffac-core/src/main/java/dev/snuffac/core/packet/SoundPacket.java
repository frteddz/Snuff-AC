package dev.snuffac.core.packet;

import dev.snuffac.api.Vec3d;

public record SoundPacket(
        long arrivalNanos,
        Vec3d origin,
        String soundName,
        float volume,
        float pitch
) implements SnuffPacket {

    @Override
    public PacketType type() {
        return PacketType.SOUND;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.OUTBOUND;
    }
}
