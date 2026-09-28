package dev.snuffac.paper;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.snuffac.core.packet.SnuffPacket;

public interface PacketTranslator {

    SnuffPacket inbound(User user, PacketTypeCommon type, PacketReceiveEvent event, long arrivalNanos);

    SnuffPacket outbound(User user, PacketTypeCommon type, PacketSendEvent event, long arrivalNanos);
}
