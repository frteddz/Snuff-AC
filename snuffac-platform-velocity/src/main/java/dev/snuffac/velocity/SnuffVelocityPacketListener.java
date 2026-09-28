package dev.snuffac.velocity;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.function.BiConsumer;

public final class SnuffVelocityPacketListener extends PacketListenerAbstract {

    private final VelocityPacketTranslator translator;
    private final BiConsumer<User, SnuffPacket> sink;

    public SnuffVelocityPacketListener(
            VelocityPacketTranslator translator,
            BiConsumer<User, SnuffPacket> sink) {
        this.translator = translator;
        this.sink = sink;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        if (user == null) {
            return;
        }
        SnuffPacket packet = translator.inbound(user, event.getPacketType(), event, System.nanoTime());
        if (packet != null) {
            sink.accept(user, packet);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = event.getUser();
        if (user == null) {
            return;
        }
        SnuffPacket packet = translator.outbound(user, event.getPacketType(), event, System.nanoTime());
        if (packet != null) {
            sink.accept(user, packet);
        }
    }

    @Override
    public PacketListenerPriority getPriority() {
        return PacketListenerPriority.LOW;
    }
}
