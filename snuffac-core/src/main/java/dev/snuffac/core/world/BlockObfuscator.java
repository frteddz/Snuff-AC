package dev.snuffac.core.world;

import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.ArrayList;
import java.util.List;

public final class BlockObfuscator {

    private final ObfuscationPolicy policy;
    private final int bandStart;
    private final int bandEnd;
    private final boolean hideContainers;

    private long obfuscatedCount;
    private long inspectedCount;

    public BlockObfuscator(
            ObfuscationPolicy policy,
            int bandStart,
            int bandEnd,
            boolean hideContainers) {
        this.policy = policy == null ? ObfuscationPolicy.OFF : policy;
        this.bandStart = bandStart;
        this.bandEnd = bandEnd;
        this.hideContainers = hideContainers;
    }

    public static BlockObfuscator disabled() {
        return new BlockObfuscator(ObfuscationPolicy.OFF, 0, 0, false);
    }

    public ObfuscationPolicy policy() {
        return policy;
    }

    public boolean active() {
        return policy != ObfuscationPolicy.OFF;
    }

    public boolean shouldObfuscate(BlockPos position, String materialName) {
        if (!active()) {
            return false;
        }
        if (!OreClassifier.isYInHiddenBand(position, bandStart, bandEnd)) {
            return false;
        }
        if (OreClassifier.isValuableOre(BlockKind.UNKNOWN, materialName)) {
            return true;
        }
        return policy == ObfuscationPolicy.HIDDEN_ORES_AND_DEEPSLATE
                && OreClassifier.isDeepslate(materialName);
    }

    public boolean shouldHideContainer(BlockPos position, String materialName) {
        return hideContainers
                && OreClassifier.isContainer(materialName)
                && OreClassifier.isYInHiddenBand(position, bandStart, bandEnd);
    }

    public record SectionResult(
            List<BlockPos> obfuscated,
            List<BlockPos> hiddenContainers,
            int inspected) {
    }

    public SectionResult process(List<BlockPos> positions, java.util.function.BiFunction<BlockPos, String, String> lookup) {
        List<BlockPos> obfuscated = new ArrayList<>();
        List<BlockPos> hidden = new ArrayList<>();
        int inspected = 0;
        for (BlockPos position : positions) {
            inspected++;
            String material = lookup.apply(position, "air");
            if (material == null) {
                continue;
            }
            if (shouldObfuscate(position, material)) {
                obfuscated.add(position);
            } else if (shouldHideContainer(position, material)) {
                hidden.add(position);
            }
        }
        obfuscatedCount += obfuscated.size();
        inspectedCount += inspected;
        return new SectionResult(obfuscated, hidden, inspected);
    }

    public long obfuscatedCount() {
        return obfuscatedCount;
    }

    public long inspectedCount() {
        return inspectedCount;
    }

    public double obfuscationRatio() {
        return inspectedCount == 0L ? 0.0 : (double) obfuscatedCount / inspectedCount;
    }
}
