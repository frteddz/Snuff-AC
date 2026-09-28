package dev.snuffac.core.prediction;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.physics.Kinematics;
import dev.snuffac.core.physics.MovementAttributes;
import dev.snuffac.core.physics.MovementEnvironment;
import dev.snuffac.core.physics.MovementInput;
import java.util.List;

public final class PredictionEngine {

    private static final double ERROR_PRUNE_THRESHOLD = 1.0E-4;

    private PredictionEngine() {
    }

    public static PredictionResult predict(
            Vec3d currentVelocity,
            Vec3d actualDelta,
            MovementEnvironment environment,
            MovementAttributes attributes) {

        List<MovementInput> candidates = InputCandidates.full();

        PredictionResult best = PredictionResult.EMPTY;
        double bestError = Double.MAX_VALUE;

        for (int index = 0; index < candidates.size(); index++) {
            MovementInput input = candidates.get(index);
            Vec3d predicted = applyTick(currentVelocity, input, environment, attributes);
            Vec3d offset = actualDelta.subtract(predicted);
            double error = offset.lengthSquared();

            if (error < bestError) {
                bestError = error;
                best = new PredictionResult(predicted, predicted, offset, offset.length(), error, input, index);
                if (error < ERROR_PRUNE_THRESHOLD) {
                    break;
                }
            }
        }
        return best;
    }

    public static Vec3d applyTick(
            Vec3d velocity,
            MovementInput input,
            MovementEnvironment environment,
            MovementAttributes attributes) {

        Vec3d horizontal = Kinematics.predictHorizontal(velocity, input, environment, attributes);
        double vertical = Kinematics.predictVerticalY(velocity.y(), input, environment, attributes);
        return new Vec3d(horizontal.x(), vertical, horizontal.z());
    }

    public static PredictionResult predictWithInput(
            Vec3d currentVelocity,
            Vec3d actualDelta,
            MovementInput input,
            MovementEnvironment environment,
            MovementAttributes attributes) {

        Vec3d predicted = applyTick(currentVelocity, input, environment, attributes);
        Vec3d offset = actualDelta.subtract(predicted);
        return new PredictionResult(
                predicted, predicted, offset, offset.length(), offset.lengthSquared(), input, -1);
    }

    public static double maximumHorizontalSpeed(MovementEnvironment environment, MovementAttributes attributes) {
        double drag = Kinematics.horizontalDrag(environment);
        double acceleration = Kinematics.horizontalAcceleration(
                new MovementInput(1.0, 0.0, false, false), environment);
        if (drag >= 1.0) {
            return acceleration * 64.0;
        }
        return acceleration / (1.0 - drag);
    }
}
