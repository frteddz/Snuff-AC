package dev.snuffac.core.mining;

import dev.snuffac.core.util.BlockPos;

public record MiningEvidence(
        int entityId,
        BlockPos position,
        long timestampMillis,
        boolean withinSentChunkRadius,
        int distanceFromPlayer,
        int oreTier,
        String materialName,
        double secondsSincePreviousTarget) {
}
