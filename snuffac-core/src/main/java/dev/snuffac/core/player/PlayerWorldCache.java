package dev.snuffac.core.player;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.physics.MovementEnvironment;
import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.Collections;
import java.util.Map;

public final class PlayerWorldCache {

    private final Vec3d center;
    private final Map<Long, CachedBlock> blocks;
    private final double slipperinessBelow;
    private final int lightLevel;
    private final boolean chunkLoaded;
    private final boolean onGroundBelow;

    private PlayerWorldCache(
            Vec3d center,
            Map<Long, CachedBlock> blocks,
            double slipperinessBelow,
            int lightLevel,
            boolean chunkLoaded,
            boolean onGroundBelow) {
        this.center = center;
        this.blocks = blocks;
        this.slipperinessBelow = slipperinessBelow;
        this.lightLevel = lightLevel;
        this.chunkLoaded = chunkLoaded;
        this.onGroundBelow = onGroundBelow;
    }

    public static PlayerWorldCache empty(Vec3d center) {
        return new PlayerWorldCache(center, Collections.emptyMap(), 0.6, 0, false, false);
    }

    public static PlayerWorldCache of(
            Vec3d center,
            Map<Long, CachedBlock> blocks,
            double slipperinessBelow,
            int lightLevel,
            boolean chunkLoaded,
            boolean onGroundBelow) {
        return new PlayerWorldCache(
                center, Map.copyOf(blocks), slipperinessBelow, lightLevel, chunkLoaded, onGroundBelow);
    }

    public Vec3d center() {
        return center;
    }

    public double slipperinessBelow() {
        return slipperinessBelow;
    }

    public int lightLevel() {
        return lightLevel;
    }

    public boolean chunkLoaded() {
        return chunkLoaded;
    }

    public boolean onGroundBelow() {
        return onGroundBelow;
    }

    public CachedBlock blockAt(BlockPos position) {
        return blocks.get(position.pack());
    }

    public BlockKind kindAt(BlockPos position) {
        CachedBlock block = blocks.get(position.pack());
        return block == null ? BlockKind.UNKNOWN : block.kind();
    }

    public String materialAt(BlockPos position) {
        CachedBlock block = blocks.get(position.pack());
        return block == null ? null : block.materialName();
    }

    public double hardnessAt(BlockPos position) {
        CachedBlock block = blocks.get(position.pack());
        return block == null ? 1.0 : block.hardness();
    }

    public boolean isValuableOre(BlockPos position) {
        return dev.snuffac.core.world.OreClassifier.isValuableOre(kindAt(position), materialAt(position));
    }

    public int oreTierAt(BlockPos position) {
        return dev.snuffac.core.world.OreClassifier.oreTier(materialAt(position));
    }

    public boolean isContainer(BlockPos position) {
        return dev.snuffac.core.world.OreClassifier.isContainer(materialAt(position));
    }

    public boolean isPassable(BlockPos position) {
        return kindAt(position).passable();
    }

    public boolean isClimbable(BlockPos position) {
        return kindAt(position).climbable();
    }

    public boolean isLiquid(BlockPos position) {
        return kindAt(position).liquid();
    }

    public boolean isIce(BlockPos position) {
        return kindAt(position).ice();
    }

    public boolean isSlime(BlockPos position) {
        return kindAt(position).slime();
    }

    public int size() {
        return blocks.size();
    }

    public MovementEnvironment apply(MovementEnvironment base) {
        return base.withSlipperiness(slipperinessBelow);
    }

    public record CachedBlock(BlockKind kind, double hardness, String materialName) {

        public CachedBlock(BlockKind kind, double hardness) {
            this(kind, hardness, null);
        }
    }
}
