package dev.snuffac.paper;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.function.BiConsumer;

public final class SnuffPacketListener extends PacketListenerAbstract {

    private final PacketTranslator translator;
    private final BiConsumer<User, SnuffPacket> sink;
    private final java.util.function.BiFunction<User, SnuffPacket, Boolean> gate;
    private final OutboundRewriter outbound;

    public interface OutboundRewriter {

        void rewrite(User user, PacketSendEvent event);
    }

    public SnuffPacketListener(PacketTranslator translator, BiConsumer<User, SnuffPacket> sink) {
        this(translator, sink, null, null);
    }

    public SnuffPacketListener(
            PacketTranslator translator,
            BiConsumer<User, SnuffPacket> sink,
            java.util.function.BiFunction<User, SnuffPacket, Boolean> gate) {
        this(translator, sink, gate, null);
    }

    public SnuffPacketListener(
            PacketTranslator translator,
            BiConsumer<User, SnuffPacket> sink,
            java.util.function.BiFunction<User, SnuffPacket, Boolean> gate,
            OutboundRewriter outbound) {
        this.translator = translator;
        this.sink = sink;
        this.gate = gate;
        this.outbound = outbound;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        if (user == null) {
            return;
        }
        SnuffPacket packet = translator.inbound(user, event.getPacketType(), event, System.nanoTime());
        if (packet == null) {
            return;
        }
        if (gate != null && Boolean.TRUE.equals(gate.apply(user, packet))) {
            event.setCancelled(true);
            return;
        }
        sink.accept(user, packet);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = event.getUser();
        if (user == null) {
            return;
        }
        if (outbound != null) {
            outbound.rewrite(user, event);
        }
        SnuffPacket packet = translator.outbound(user, event.getPacketType(), event, System.nanoTime());
        if (packet != null && gate != null) {
            gate.apply(user, packet);
        }
        if (packet != null) {
            sink.accept(user, packet);
        }
    }

    @Override
    public PacketListenerPriority getPriority() {
        return PacketListenerPriority.LOWEST;
    }
}
