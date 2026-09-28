package dev.snuffac.paper;

import java.util.Map;
import org.bukkit.Material;

public final class MiningSpeedResolver {

    private static final Map<String, Double> TOOL_SPEEDS = Map.ofEntries(
            Map.entry("WOODEN_PICKAXE", 2.0),
            Map.entry("STONE_PICKAXE", 3.0),
            Map.entry("IRON_PICKAXE", 4.0),
            Map.entry("GOLDEN_PICKAXE", 3.0),
            Map.entry("DIAMOND_PICKAXE", 5.0),
            Map.entry("NETHERITE_PICKAXE", 6.0),
            Map.entry("WOODEN_AXE", 2.0),
            Map.entry("STONE_AXE", 3.0),
            Map.entry("IRON_AXE", 4.0),
            Map.entry("GOLDEN_AXE", 3.0),
            Map.entry("DIAMOND_AXE", 5.0),
            Map.entry("NETHERITE_AXE", 6.0),
            Map.entry("WOODEN_SHOVEL", 1.5),
            Map.entry("STONE_SHOVEL", 1.5),
            Map.entry("IRON_SHOVEL", 1.5),
            Map.entry("GOLDEN_SHOVEL", 1.5),
            Map.entry("DIAMOND_SHOVEL", 1.5),
            Map.entry("NETHERITE_SHOVEL", 1.5),
            Map.entry("WOODEN_HOE", 1.0),
            Map.entry("STONE_HOE", 1.0),
            Map.entry("IRON_HOE", 1.0),
            Map.entry("GOLDEN_HOE", 1.0),
            Map.entry("DIAMOND_HOE", 1.0),
            Map.entry("NETHERITE_HOE", 1.0),
            Map.entry("SHEARS", 5.0),
            Map.entry("SHIELD", 1.0));

    private MiningSpeedResolver() {
    }

    public static double speedFor(Material material) {
        if (material == null || material == Material.AIR) {
            return 1.0;
        }
        Double speed = TOOL_SPEEDS.get(material.name());
        return speed == null ? 1.0 : speed;
    }

    public static boolean isTool(Material material) {
        if (material == null) {
            return false;
        }
        return TOOL_SPEEDS.containsKey(material.name());
    }

    public static boolean isPlaceable(Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }
        return material.isBlock() && !material.isAir();
    }
}
