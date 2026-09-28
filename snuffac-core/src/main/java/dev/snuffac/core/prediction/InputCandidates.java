package dev.snuffac.core.prediction;

import dev.snuffac.core.physics.MovementInput;
import java.util.ArrayList;
import java.util.List;

public final class InputCandidates {

    private static final double[] AXIS = {0.0, 1.0, -1.0};

    private InputCandidates() {
    }

    public static List<MovementInput> full() {
        List<MovementInput> candidates = new ArrayList<>(48);
        for (double forward : AXIS) {
            for (double strafe : AXIS) {
                for (int flipIndex = 0; flipIndex < 2; flipIndex++) {
                    boolean flip = flipIndex == 1;
                    candidates.add(new MovementInput(forward, strafe, false, flip));
                    candidates.add(new MovementInput(forward, strafe, true, flip));
                }
            }
        }
        return candidates;
    }

    public static List<MovementInput> forwardOnly(boolean jumping) {
        List<MovementInput> candidates = new ArrayList<>(8);
        for (int flipIndex = 0; flipIndex < 2; flipIndex++) {
            boolean flip = flipIndex == 1;
            candidates.add(new MovementInput(1.0, 0.0, jumping, flip));
            candidates.add(new MovementInput(1.0, 1.0, jumping, flip));
            candidates.add(new MovementInput(1.0, -1.0, jumping, flip));
        }
        return candidates;
    }

    public static List<MovementInput> withFlipOnly(MovementInput base) {
        List<MovementInput> candidates = new ArrayList<>(4);
        candidates.add(base.withoutFlip());
        candidates.add(new MovementInput(base.forward(), base.strafe(), base.jump(), true));
        return candidates;
    }
}
