package dev.snuffac.core.check.impl.world;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.util.BlockPos;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;

public final class OreRatioCheck implements Check {

    public static final int WINDOW = 40;
    public static final int MIN_SAMPLES = 20;
    public static final double VALUABLE_RATIO = 0.25;
    public static final int REQUIRED_WINDOWS = 3;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_BREAK);
    }

    @Override
    public String key() {
        return "oreratio";
    }

    @Override
    public String name() {
        return "OreRatio";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Compares how much valuable ore a player's mining path targets against ordinary "
                + "stone, which is the statistical signature of an xray client.";
    }

    @Override
    public Object createState() {
        return new RatioState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!(packet instanceof BlockBreakPacket dig)
                || dig.action() != BlockBreakPacket.BlockBreakAction.START) {
            return;
        }
        var state = (RatioState) state(context.player());
        if (state == null) {
            return;
        }

        var cache = context.player().worldCache();
        if (!cache.chunkLoaded()) {
            return;
        }

        BlockPos position = BlockPos.unpack(dig.packedPosition());
        boolean valuable = cache.isValuableOre(position);
        state.digs++;
        if (valuable) {
            state.ores++;
        }

        state.window.add(valuable ? 1 : 0);
        if (state.window.size() > WINDOW) {
            state.window.removeFirst();
        }
        if (state.window.size() < MIN_SAMPLES) {
            return;
        }

        long ore = 0L;
        for (int value : state.window) {
            ore += value;
        }
        double ratio = (double) ore / (double) state.window.size();
        if (ratio < VALUABLE_RATIO) {
            state.cleanWindows++;
            if (state.cleanWindows >= REQUIRED_WINDOWS) {
                state.reset();
            }
            return;
        }

        state.dirtyWindows++;
        if (state.dirtyWindows < REQUIRED_WINDOWS) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("windowRatio", round(ratio));
        evidence.put("ratioLimit", VALUABLE_RATIO);
        evidence.put("window", state.window.size());
        evidence.put("totalDigs", state.digs);
        evidence.put("totalOres", state.ores);
        evidence.put("dirtyWindows", state.dirtyWindows);
        evidence.put("lastBlock", position.toString());
        evidence.put("lastMaterial", cache.materialAt(position));

        context.flag("targeted valuable ore in " + round(ratio) + " of the last "
                + state.window.size() + " blocks mined", evidence, 6.0);
        state.reset();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class RatioState {

        private final ArrayDeque<Integer> window = new ArrayDeque<>(WINDOW);
        private int digs;
        private int ores;
        private int dirtyWindows;
        private int cleanWindows;

        private void reset() {
            window.clear();
            digs = 0;
            ores = 0;
            dirtyWindows = 0;
            cleanWindows = 0;
        }
    }
}