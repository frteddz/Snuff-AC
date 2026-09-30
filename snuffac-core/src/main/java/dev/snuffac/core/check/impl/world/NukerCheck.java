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
    public static final double LINE_OF_SIGHT_LIMIT = 6.0;
    private static final int SIGHT_LIMIT_RUN = 2;

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
        return "Detects starting to dig an abnormal number of distinct blocks per burst and per "
                + "second, and dig targets the server block view says are out of sight.";
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

        if (accountLineOfSight(context, dig, state)) {
            return;
        }

        long now = System.currentTimeMillis();
        state.recent.removeIf(event -> now - event.atMillis() >= WINDOW_MILLIS);
        state.recent.add(new DigEvent(now, dig.packedPosition()));

        long burst = state.recent.stream()
                .filter(event -> now - event.atMillis() < BURST_WINDOW_MILLIS)
                .map(DigEvent::packed)
                .distinct()
                .count();
        long distinct = state.recent.stream().map(DigEvent::packed).distinct().count();

        if (burst < BURST_LIMIT && distinct <= MAX_DISTINCT_PER_SECOND) {
            return;
        }

        BlockPos sample = BlockPos.unpack(state.recent.peekLast().packed());
        Map<String, Object> evidence = context.newEvidence();
        evidence.put("distinctBlocksInBurst", burst);
        evidence.put("distinctBlocks", distinct);
        evidence.put("maximum", MAX_DISTINCT_PER_SECOND);
        evidence.put("window", WINDOW_MILLIS);
        evidence.put("sampleX", sample.x());
        evidence.put("sampleY", sample.y());
        evidence.put("sampleZ", sample.z());
        evidence.put("distance", round(context.player().position().distanceTo(sample.toVec())));
        context.flag("started digging " + burst + " distinct blocks within " + BURST_WINDOW_MILLIS
                + "ms, " + distinct + " in the last second", evidence, 6.0);
        state.recent.clear();
    }

    private boolean accountLineOfSight(CheckContext context, BlockBreakPacket dig, NukerState state) {
        var cache = context.player().worldCache();
        if (!cache.chunkLoaded()) {
            return false;
        }
        BlockPos position = digPosition(dig);
        if (cache.materialAt(position) == null) {
            return false;
        }
        var eye = dev.snuffac.core.combat.ReachResolver.eyePosition(
                context.player().position(), context.player().movement().sneaking());
        var target = new dev.snuffac.api.Vec3d(
                position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);
        if (eye.distanceTo(target) <= LINE_OF_SIGHT_LIMIT) {
            state.blindDigs = 0;
            return false;
        }
        boolean blocked = dev.snuffac.core.combat.ReachResolver.segmentBlocked(
                eye, target, cache::blocksMovement);
        if (!blocked) {
            state.blindDigs = 0;
            return false;
        }

        state.blindDigs++;
        if (state.blindDigs < SIGHT_LIMIT_RUN) {
            return true;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("mode", "out of sight");
        evidence.put("distance", round(eye.distanceTo(target)));
        evidence.put("limit", LINE_OF_SIGHT_LIMIT);
        evidence.put("consecutive", state.blindDigs);
        evidence.put("block", position.toString());
        evidence.put("material", cache.materialAt(position));
        context.flag("started digging a block at " + position + " which is behind solid blocks",
                evidence, 7.0);
        state.blindDigs = 0;
        state.recent.clear();
        return true;
    }

    private static BlockPos digPosition(BlockBreakPacket dig) {
        return BlockPos.unpack(dig.packedPosition());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    record DigEvent(long atMillis, long packed) {
    }

    static final class NukerState {

        private final java.util.ArrayDeque<DigEvent> recent = new java.util.ArrayDeque<>();
        private int blindDigs;
    }
}
