package dev.snuffac.core.check.impl.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.util.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhaseSweepTest {

    @Test
    @DisplayName("a move through open air is allowed")
    void openAirIsAllowed() {
        Vec3d from = new Vec3d(0.5, 65.0, 0.5);
        Vec3d to = new Vec3d(2.5, 65.0, 0.5);
        assertFalse(PhaseSweep.crossesSolid(from, to, position -> false),
                "there is nothing in the way");
    }

    @Test
    @DisplayName("a move straight through a wall is caught")
    void throughAWallIsCaught() {
        Vec3d from = new Vec3d(0.5, 65.5, 0.5);
        Vec3d to = new Vec3d(4.5, 65.5, 0.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(2, 65, 0));
        assertTrue(PhaseSweep.crossesSolid(from, to, solid),
                "the wall between the two points should be found");
    }

    @Test
    @DisplayName("a wall the move does not pass through is ignored")
    void wallOffThePathIsIgnored() {
        Vec3d from = new Vec3d(0.5, 65.0, 0.5);
        Vec3d to = new Vec3d(3.5, 65.0, 0.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(2, 65, 5));
        assertFalse(PhaseSweep.crossesSolid(from, to, solid),
                "a block to the side is not in the way");
    }

    @Test
    @DisplayName("a move no further than a block does not sample a wall beside it")
    void ordinaryStepIsClean() {
        Vec3d from = new Vec3d(0.5, 65.0, 0.5);
        Vec3d to = new Vec3d(1.1, 65.0, 0.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(1, 64, 0));
        assertFalse(PhaseSweep.crossesSolid(from, to, solid),
                "the floor under a normal step is not a wall");
    }

    @Test
    @DisplayName("a move that starts inside a block is not reported")
    void startingInsideIsNotReported() {
        Vec3d from = new Vec3d(2.5, 65.5, 0.5);
        Vec3d to = new Vec3d(4.5, 65.5, 0.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(2, 65, 0));
        assertFalse(PhaseSweep.crossesSolid(from, to, solid),
                "a player already overlapping a block, such as one pushed into it, is not phasing");
    }

    @Test
    @DisplayName("a long move is sampled finely enough to catch a thin wall")
    void longMoveIsSampledFinely() {
        Vec3d from = new Vec3d(0.5, 65.5, 0.5);
        Vec3d to = new Vec3d(24.5, 65.5, 0.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(12, 65, 0));
        assertTrue(PhaseSweep.crossesSolid(from, to, solid),
                "a wall in the middle of a long move must not be stepped over");
    }

    @Test
    @DisplayName("a diagonal move through a corner is caught")
    void diagonalThroughCornerIsCaught() {
        Vec3d from = new Vec3d(0.5, 65.5, 0.5);
        Vec3d to = new Vec3d(3.5, 65.5, 3.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(2, 65, 2));
        assertTrue(PhaseSweep.crossesSolid(from, to, solid),
                "diagonal phasing is the same cheat");
    }

    @Test
    @DisplayName("a vertical rise past the step height through a block is caught")
    void verticalThroughCeilingIsCaught() {
        Vec3d from = new Vec3d(0.5, 65.0, 0.5);
        Vec3d to = new Vec3d(0.5, 67.0, 0.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(0, 66, 0));
        assertTrue(PhaseSweep.crossesSolid(from, to, solid),
                "going straight up through a block is phasing");
    }

    @Test
    @DisplayName("a jump that clears a one block step is not phasing")
    void jumpOverOneBlockIsClean() {
        Vec3d from = new Vec3d(0.5, 65.0, 0.5);
        Vec3d to = new Vec3d(0.5, 66.4, 0.5);
        PhaseSweep.Solidity solid = position -> position.equals(new BlockPos(0, 65, 0));
        assertFalse(PhaseSweep.crossesSolid(from, to, solid),
                "rising over the block you are standing next to is a normal jump");
    }
}
