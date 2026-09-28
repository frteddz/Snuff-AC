package dev.snuffac.core.world;

import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.Locale;
import java.util.Set;

public final class OreClassifier {

    public static final Set<String> VALUABLE_ORES = Set.of(
            "DIAMOND_ORE",
            "DEEPSLATE_DIAMOND_ORE",
            "EMERALD_ORE",
            "DEEPSLATE_EMERALD_ORE",
            "GOLD_ORE",
            "DEEPSLATE_GOLD_ORE",
            "IRON_ORE",
            "DEEPSLATE_IRON_ORE",
            "COPPER_ORE",
            "DEEPSLATE_COPPER_ORE",
            "REDSTONE_ORE",
            "DEEPSLATE_REDSTONE_ORE",
            "LAPIS_ORE",
            "DEEPSLATE_LAPIS_ORE",
            "ANCIENT_DEBRIS",
            "NETHER_QUARTZ_ORE",
            "NETHER_GOLD_ORE");

    public static final Set<String> HIDDEN_CONTAINERS = Set.of(
            "CHEST",
            "BARREL",
            "SHULKER_BOX",
            "ENDER_CHEST");

    private OreClassifier() {
    }

    public static boolean isValuableOre(BlockKind kind, String materialName) {
        if (kind == BlockKind.UNKNOWN && materialName == null) {
            return false;
        }
        return materialName != null && VALUABLE_ORES.contains(materialName.toUpperCase(Locale.ROOT));
    }

    public static boolean isContainer(String materialName) {
        return materialName != null && HIDDEN_CONTAINERS.contains(materialName.toUpperCase(Locale.ROOT));
    }

    public static boolean isDeepslate(String materialName) {
        return materialName != null
                && materialName.toLowerCase(Locale.ROOT).contains("deepslate");
    }

    public static int oreTier(String materialName) {
        if (materialName == null) {
            return 0;
        }
        String name = materialName.toUpperCase(Locale.ROOT);
        if (name.contains("DIAMOND") || name.contains("EMERALD")) {
            return 3;
        }
        if (name.contains("GOLD") || name.contains("ANCIENT_DEBRIS") || name.contains("REDSTONE")) {
            return 2;
        }
        if (name.contains("IRON") || name.contains("COPPER") || name.contains("LAPIS")
                || name.contains("QUARTZ")) {
            return 1;
        }
        return 0;
    }

    public static boolean isYInHiddenBand(BlockPos position, int bandStart, int bandEnd) {
        return position.y() >= bandStart && position.y() <= bandEnd;
    }
}
