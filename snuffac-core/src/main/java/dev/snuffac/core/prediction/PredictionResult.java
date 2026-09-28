package dev.snuffac.core.prediction;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.physics.MovementInput;

public record PredictionResult(
        Vec3d predictedVelocity,
        Vec3d predictedDelta,
        Vec3d offset,
        double offsetMagnitude,
        double error,
        MovementInput matchedInput,
        int candidateIndex
) {

    public static final PredictionResult EMPTY = new PredictionResult(
            Vec3d.ZERO, Vec3d.ZERO, Vec3d.ZERO, Double.MAX_VALUE, Double.MAX_VALUE, MovementInput.NONE, -1);

    public PredictionResult withOffset(Vec3d newOffset, double newError) {
        return new PredictionResult(
                predictedVelocity, predictedDelta, newOffset, newOffset.length(), newError, matchedInput, candidateIndex);
    }
}
