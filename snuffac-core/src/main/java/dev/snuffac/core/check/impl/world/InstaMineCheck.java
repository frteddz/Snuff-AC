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

public final class InstaMineCheck implements Check {

    public static final long INSTANT_WINDOW_NANOS = 600_000_000L;
    public static final int REQUIRED = 3;
    public static final double MIN_HARDNESS = 0.5;
    public static final double MIN_NANOS = 25_000_000.0;
    public static final int REQUIRED_DISTINCT = 3;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_BREAK);
    }

    @Override
    public String key() {
        return "instamine";
    }

    @Override
    public String name() {
        return "InstaMine";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Detects several different hard blocks finished inside the same instant window, "
                + "which is the InstaMine cheat.";
    }

    @Override
    public Object createState() {
        return new MineState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof BlockBreakPacket dig)
                || dig.action() != BlockBreakPacket.BlockBreakAction.FINISH) {
            return;
        }
        var state = (MineState) state(context.player());
        if (state == null) {
            return;
        }

        var cache = context.player().worldCache();
        if (!cache.chunkLoaded()) {
            return;
        }

        long now = packet.arrivalNanos();
        state.finishes.removeIf(entry -> now - entry.nanos > INSTANT_WINDOW_NANOS);

        BlockPos position = BlockPos.unpack(dig.packedPosition());
        double hardness = cache.hardnessAt(position);
        if (hardness < MIN_HARDNESS) {
            return;
        }

        state.finishes.add(new Finish(now, position));
        if (state.finishes.size() < REQUIRED) {
            return;
        }

        long distinct = state.finishes.stream()
                .map(finish -> finish.position().toString())
                .distinct()
                .count();
        if (distinct < REQUIRED_DISTINCT) {
            return;
        }

        long span = now - state.finishes.peekFirst().nanos();
        double average = span / (double) state.finishes.size();
        if (average >= MIN_NANOS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("finishes", state.finishes.size());
        evidence.put("distinctBlocks", distinct);
        evidence.put("spanMillis", span / 1_000_000L);
        evidence.put("averageMillis", Math.round(average / 1_000_000.0 * 100.0) / 100.0);
        evidence.put("floorMillis", MIN_NANOS / 1_000_000.0);
        evidence.put("hardness", Math.round(hardness * 100.0) / 100.0);
        evidence.put("hardnessFloor", MIN_HARDNESS);
        evidence.put("lastBlock", position.toString());
        evidence.put("miningSpeed",
                Math.round(context.player().equipment().miningSpeed() * 100.0) / 100.0);

        context.flag("finished " + distinct + " hard blocks in " + span / 1_000_000L
                + "ms, about " + Math.round(average / 1_000_000.0) + "ms each", evidence, 7.0);
        state.finishes.clear();
    }

    record Finish(long nanos, BlockPos position) {
    }

    static final class MineState {

        private final java.util.ArrayDeque<Finish> finishes = new java.util.ArrayDeque<>(8);
    }
}