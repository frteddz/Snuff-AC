package dev.snuffac.core.platform;

import dev.snuffac.core.util.BlockKind;
import dev.snuffac.core.util.BlockPos;
import java.util.List;
import java.util.UUID;

public interface SnuffWorldAccess {

    BlockKind blockAt(BlockPos position);

    double slipperinessAt(BlockPos position);

    boolean isPassable(BlockPos position);

    boolean isClimbable(BlockPos position);

    boolean isLiquid(BlockPos position);

    int lightLevel(BlockPos position);

    List<UUID> nearbyEntities(UUID viewer, double radius);

    double[] entityPosition(UUID entityId);

    double[] entityBox(UUID entityId);

    boolean isChunkLoaded(BlockPos position);

    int worldHeight();
}
