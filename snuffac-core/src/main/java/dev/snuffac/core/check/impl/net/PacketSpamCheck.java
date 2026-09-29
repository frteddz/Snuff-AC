package dev.snuffac.core.check.impl.net;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class PacketSpamCheck implements Check {

    public static final long WINDOW_MILLIS = 1000L;
    public static final long MIN_WINDOW_ELAPSED = 250L;
    public static final int REQUIRED_WINDOWS = 3;
    private static final double HARD_LIMIT = 400.0;

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
        } else if (now - state.windowStart >= WINDOW_MILLIS) {
            close(context, state, now);
        }
        state.count++;
        state.byType.merge(packet.type(), 1, Integer::sum);
    }

    @Override
    public void onTick(CheckContext context) {
        var state = (SpamState) state(context.player());
        if (state == null || state.windowStart == 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - state.windowStart >= WINDOW_MILLIS) {
            close(context, state, now);
        }
    }

    @Override
    public void onPlayerQuit(CheckContext context) {
        var state = (SpamState) state(context.player());
        if (state != null) {
            state.reset(System.currentTimeMillis());
        }
    }

    private void close(CheckContext context, SpamState state, long now) {
        long elapsed = now - state.windowStart;
        int count = state.count;
        Map<PacketType, Integer> breakdown = new EnumMap<>(state.byType);
        state.reset(now);

        if (elapsed < MIN_WINDOW_ELAPSED || count <= 0) {
            return;
        }

        double perSecond = count * 1000.0 / elapsed;
        double limit = context.config().badPacketMaxPerSecond();
        double threshold = Math.max(limit * 2.0, HARD_LIMIT);
        if (perSecond < threshold) {
            state.breaches = 0;
            return;
        }

        state.breaches++;
        if (state.breaches < REQUIRED_WINDOWS) {
            return;
        }
        state.breaches = 0;

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("packetsPerSecond", round(perSecond));
        evidence.put("threshold", round(threshold));
        evidence.put("count", count);
        evidence.put("elapsedMillis", elapsed);
        evidence.put("byType", breakdown);
        evidence.put("windowsRequired", REQUIRED_WINDOWS);
        evidence.put("ping", round(context.ping()));
        evidence.put("tps", round(context.tps()));
        context.flag("packet flood of " + round(perSecond) + " packets per second", evidence, 8.0);
    }

    static final class SpamState {

        private long windowStart;
        private int count;
        private int breaches;
        private final Map<PacketType, Integer> byType = new EnumMap<>(PacketType.class);

        private void reset(long now) {
            windowStart = now;
            count = 0;
            byType.clear();
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
