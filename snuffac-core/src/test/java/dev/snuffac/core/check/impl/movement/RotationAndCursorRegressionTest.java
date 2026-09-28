package dev.snuffac.core.check.impl.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.packet.MovementPacket;
import org.junit.jupiter.api.Test;

class RotationAndCursorRegressionTest {

    private static MovementPacket movement(Vec3d position, float yaw, float pitch) {
        return new MovementPacket(System.nanoTime(), true, true, position, yaw, pitch, false, false);
    }

    @Test
    void yawBeyondOneFullTurnIsNotTreatedAsImpossible() {
        MovementPacket packet = movement(new Vec3d(0.0, 64.0, 0.0), 374.25693f, 8.405056f);
        assertTrue(Float.isFinite(packet.yaw()));
        assertTrue(Math.abs(packet.yaw()) > 360.0,
                "this reproduces the observed yaw of 374 which the old check wrongly rejected");
    }

    @Test
    void pitchStaysWithinNinetyDegrees() {
        assertTrue(Math.abs(19.072132f) < 90.0, "observed pitch of 19 is legitimate");
    }

    @Test
    void aFallingPlayerFarFromOriginIsNotAWorldViolation() {
        Vec3d position = new Vec3d(12.0, 64.0, -40.0);
        Vec3d cursor = new Vec3d(12.0, 64.0, 2280.0);
        double distance = position.distanceTo(cursor);
        assertTrue(distance > 2000.0, "observed attack cursor distance was over 2000 blocks");
        assertFalse(distance > 1.0E6, "and it is still well inside world bounds, so it must not flag");
    }

}
