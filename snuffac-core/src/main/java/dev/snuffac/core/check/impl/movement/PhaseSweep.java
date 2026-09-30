package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.BlockPos;

public final class PhaseSweep {

    public static final double SAMPLE_STEP = 0.2;
    public static final double VERTICAL_SAMPLE_STEP = 0.05;
    public static final double SKIN = 0.001;

    private PhaseSweep() {
    }

    public interface Solidity {

        boolean solidAt(BlockPos position);
    }

    public static boolean crossesSolid(Vec3d from, Vec3d to, Solidity solidity) {
        BlockPos hit = findSolid(from, to, solidity);
        return hit != null;
    }

    public static BlockPos findSolid(Vec3d from, Vec3d to, Solidity solidity) {
        if (from == null || to == null) {
            return null;
        }
        double vertical = Math.abs(to.y() - from.y());
        double step = vertical > 1.0 ? VERTICAL_SAMPLE_STEP : SAMPLE_STEP;
        double distance = from.distanceTo(to);
        if (distance < SKIN) {
            return null;
        }
        int samples = (int) Math.ceil(distance / step);
        if (samples < 1) {
            samples = 1;
        }
        Vec3d delta = to.subtract(from);
        for (int i = 0; i <= samples; i++) {
            double progress = (double) i / samples;
            Vec3d sample = from.add(delta.multiply(progress));
            BlockPos position = BlockPos.of(sample);
            if (sameBlock(position, from) || sameBlock(position, to)) {
                continue;
            }
            if (solidity.solidAt(position)) {
                return position;
            }
        }
        return null;
    }

    private static boolean sameBlock(BlockPos position, Vec3d point) {
        return position.x() == Math.floor(point.x())
                && position.y() == Math.floor(point.y())
                && position.z() == Math.floor(point.z());
    }
}
