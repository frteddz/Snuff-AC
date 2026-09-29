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
        SnuffPlatform platform,
        String worldName,
        double x,
        double y,
        double z
) {

    public ViolationInfo(
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
            SnuffPlatform platform) {
        this(playerId, playerName, category, checkKey, checkName, detail, violationLevel,
                buffer, confidence, pingMillis, tps, timestampMillis, platform, "", 0.0, 0.0, 0.0);
    }
}
