package dev.snuffac.paper;

import dev.snuffac.core.util.BlockKind;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class BlockClassifier {

    private BlockClassifier() {
    }

    public static BlockKind classify(Material material) {
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR
                || material == Material.VOID_AIR) {
            return BlockKind.AIR;
        }
        if (material.isAir()) {
            return BlockKind.AIR;
        }
        String name = material.name();

        if (isWater(material)) {
            return BlockKind.WATER;
        }
        if (material == Material.LAVA) {
            return BlockKind.LAVA;
        }
        if (material == Material.BEDROCK) {
            return BlockKind.BEDROCK;
        }
        if (material == Material.BARRIER) {
            return BlockKind.BARRIER;
        }
        if (material == Material.ICE || material == Material.FROSTED_ICE) {
            return BlockKind.ICE;
        }
        if (material == Material.PACKED_ICE) {
            return BlockKind.PACKED_ICE;
        }
        if (material == Material.BLUE_ICE) {
            return BlockKind.BLUE_ICE;
        }
        if (material == Material.SLIME_BLOCK) {
            return BlockKind.SLIME;
        }
        if (material == Material.HONEY_BLOCK) {
            return BlockKind.HONEY;
        }
        if (material == Material.SOUL_SAND) {
            return BlockKind.SOUL_SAND;
        }
        if (material == Material.LADDER || material == Material.VINE
                || material == Material.SCAFFOLDING || material == Material.TWISTING_VINES
                || material == Material.TWISTING_VINES_PLANT || material == Material.GLOW_LICHEN
                || material == Material.CAVE_VINES || material == Material.CAVE_VINES_PLANT) {
            return BlockKind.LADDER;
        }
        if (material == Material.COBWEB) {
            return BlockKind.COBWEB;
        }
        if (material == Material.CACTUS || material == Material.POWDER_SNOW) {
            return BlockKind.CACTUS;
        }
        if (!material.isSolid()) {
            return BlockKind.AIR;
        }
        if (name.startsWith("LEGACY_")) {
            return BlockKind.UNKNOWN;
        }
        return BlockKind.SOLID;
    }

    public static BlockKind classify(Block block) {
        if (block == null) {
            return BlockKind.UNKNOWN;
        }
        return classify(block.getType());
    }

    private static boolean isWater(Material material) {
        return material == Material.WATER || material == Material.BUBBLE_COLUMN;
    }

    public static double hardness(Block block) {
        if (block == null) {
            return 1.0;
        }
        BlockKind kind = classify(block);
        if (kind == BlockKind.AIR || kind == BlockKind.BEDROCK || kind == BlockKind.BARRIER
                || kind == BlockKind.WATER || kind == BlockKind.LAVA) {
            return 0.0;
        }
        try {
            float hardness = block.getType().getHardness();
            if (Float.isNaN(hardness) || hardness < 0.0F) {
                return 1.0;
            }
            return hardness;
        } catch (RuntimeException exception) {
            return 1.0;
        }
    }
}
