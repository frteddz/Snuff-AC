package dev.snuffac.core.check.impl.world;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class NukerCheck implements Check {

    private static final long WINDOW_MILLIS = 1000L;
    public static final double MAX_DIG_REACH = 6.0;
    private static final long BURST_WINDOW_MILLIS = 700L;
    private static final int BURST_LIMIT = 3;
    private static final int MAX_DISTINCT_PER_SECOND = 12;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_BREAK);
    }

    @Override
    public String key() {
        return "nuker";
    }

    @Override
    public String name() {
        return "Nuker";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Detects starting to dig an abnormal number of distinct blocks per second.";
    }

    @Override
    public Object createState() {
        return new NukerState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof BlockBreakPacket dig) || dig.action() != BlockBreakPacket.BlockBreakAction.START) {
            return;
        }
        var state = (NukerState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var cache = player.worldCache();
        var environment = player.combatEnvironment();
        if (environment.count() > 0 || cache.chunkLoaded()) {
            double reach = context.player().position().distanceTo(digPosition(dig).toVec());
            if (reach > MAX_DIG_REACH && cache.blockAt(digPosition(dig)) != null) {
                Map<String, Object> far = context.newEvidence();
                far.put("distance", round(reach));
                far.put("maximum", MAX_DIG_REACH);
                context.flag("started digging a block " + round(reach) + " blocks away", far, 7.0);
            }
        }

        long now = System.currentTimeMillis();
        state.recent.removeIf(entry -> now - entry >= WINDOW_MILLIS);
        state.recent.add(dig.packedPosition());

        long burst = state.recent.stream().filter(entry -> now - entry < BURST_WINDOW_MILLIS).count();
        long distinct = state.recent.stream().distinct().count();

        if (burst < BURST_LIMIT && distinct <= MAX_DISTINCT_PER_SECOND) {
            return;
        }

        BlockPos sample = BlockPos.unpack(state.recent.peekLast());
        Map<String, Object> evidence = context.newEvidence();
        evidence.put("digPacketsInBurst", burst);
        evidence.put("distinctBlocks", distinct);
        evidence.put("maximum", MAX_DISTINCT_PER_SECOND);
        evidence.put("window", WINDOW_MILLIS);
        evidence.put("sampleX", sample.x());
        evidence.put("sampleY", sample.y());
        evidence.put("sampleZ", sample.z());
        evidence.put("distance", round(context.player().position().distanceTo(sample.toVec())));
        context.flag("sent " + burst + " dig packets within " + BURST_WINDOW_MILLIS
                + "ms across " + distinct + " distinct block(s)", evidence, 6.0);
        state.recent.clear();
    }

    private static BlockPos digPosition(BlockBreakPacket dig) {
        return BlockPos.unpack(dig.packedPosition());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    static final class NukerState {

        private final java.util.ArrayDeque<Long> recent = new java.util.ArrayDeque<>();
    }
}
