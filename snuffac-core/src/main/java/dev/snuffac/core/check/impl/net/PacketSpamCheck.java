package dev.snuffac.core.check.impl.net;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;
import java.util.Set;

public final class PacketSpamCheck implements Check {

    private static final double HARD_LIMIT = 400.0;
    private static final long WINDOW_MILLIS = 1000L;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.MOVEMENT, PacketType.ATTACK, PacketType.BLOCK_BREAK,
                PacketType.BLOCK_PLACE, PacketType.KEEP_ALIVE);
    }

    @Override
    public String key() {
        return "packetspam";
    }

    @Override
    public String name() {
        return "PacketSpam";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.PACKET;
    }

    @Override
    public String description() {
        return "Detects clients flooding the server with packets beyond any legitimate play pattern.";
    }

    @Override
    public Object createState() {
        return new SpamState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (SpamState) state(context.player());
        if (state == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (state.windowStart == 0L) {
            state.windowStart = now;
        }
        if (now - state.windowStart >= WINDOW_MILLIS) {
            state.windowStart = now;
            state.count = 0;
        }
        state.count++;

        double perSecond = state.count * 1000.0 / (double) Math.max(1L, now - state.windowStart + 1L);
        double limit = context.config().badPacketMaxPerSecond();
        double threshold = Math.max(limit * 2.0, HARD_LIMIT);

        if (perSecond < threshold) {
            state.flagged = false;
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("packetsPerSecond", round(perSecond));
        evidence.put("threshold", round(threshold));
        evidence.put("count", state.count);
        evidence.put("packetType", packet.type().name());
        evidence.put("ping", round(context.ping()));

        if (state.flagged) {
            return;
        }
        state.flagged = true;
        context.flag("packet flood of " + round(perSecond) + " packets per second", evidence, 8.0);
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (SpamState) state(context.player());
        if (state != null) {
            state.flagged = false;
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static final class SpamState {

        private long windowStart;
        private int count;
        private boolean flagged;
    }
}
