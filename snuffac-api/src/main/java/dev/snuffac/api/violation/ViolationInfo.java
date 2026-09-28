package dev.snuffac.api.violation;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.SnuffPlatform;
import java.util.UUID;

public record ViolationInfo(
        UUID playerId,
        String playerName,
        CheckCategory category,
        String checkKey,
        String checkName,
        String detail,
        double violationLevel,
        double buffer,
        double confidence,
        double pingMillis,
        double tps,
        long timestampMillis,
        SnuffPlatform platform
) {
}
