package dev.snuffac.velocity;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.snuffac.core.packet.KeepAlivePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.ServerKeepAlivePacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.packet.ServerVelocityPacket;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import dev.snuffac.api.Vec3d;

public final class VelocityPacketTranslator {

    public SnuffPacket inbound(User user, PacketTypeCommon type, PacketReceiveEvent event, long arrivalNanos) {
        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            var wrapper = new WrapperPlayClientPlayerFlying(event);
            var location = wrapper.getLocation();
            return new MovementPacket(
                    arrivalNanos,
                    wrapper.hasPositionChanged(),
                    wrapper.hasRotationChanged(),
                    new Vec3d(location.getX(), location.getY(), location.getZ()),
                    location.getYaw(),
                    location.getPitch(),
                    wrapper.isOnGround(),
                    wrapper.isHorizontalCollision());
        }
        if (type == PacketType.Play.Client.KEEP_ALIVE) {
            return new KeepAlivePacket(arrivalNanos, new WrapperPlayClientKeepAlive(event).getId());
        }
        return null;
    }

    public SnuffPacket outbound(User user, PacketTypeCommon type, PacketSendEvent event, long arrivalNanos) {
        if (type == PacketType.Play.Server.ENTITY_VELOCITY) {
            var wrapper = new WrapperPlayServerEntityVelocity(event);
            var velocity = wrapper.getVelocity();
            return new ServerVelocityPacket(
                    arrivalNanos,
                    wrapper.getEntityId(),
                    new Vec3d(velocity.getX(), velocity.getY(), velocity.getZ()));
        }
        if (type == PacketType.Play.Server.KEEP_ALIVE) {
            return new ServerKeepAlivePacket(arrivalNanos, new WrapperPlayServerKeepAlive(event).getId());
        }
        return null;
    }
}
