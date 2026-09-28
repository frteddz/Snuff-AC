package dev.snuffac.api.violation;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.SnuffPlatform;

public record CheckInfo(
        String key,
        String name,
        CheckCategory category,
        boolean enabled,
        double setbackThreshold,
        String punishmentAction,
        String description
) {
    public CheckInfo withEnabled(boolean newEnabled) {
        return new CheckInfo(key, name, category, newEnabled, setbackThreshold, punishmentAction, description);
    }
}
