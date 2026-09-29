package dev.snuffac.core.prediction;

import dev.snuffac.core.player.MovementState;

public final class PredictionGraces {

    public static final int WIND_CHARGE_TICKS = 60;
    public static final int WIND_CHARGE_HIT_TICKS = 20;
    public static final int ATTACK_IMPULSE_TICKS = 6;

    private PredictionGraces() {
    }

    public static boolean windChargeActive(MovementState movement) {
        return movement.ticksSinceWindCharge() < WIND_CHARGE_TICKS
                || movement.ticksSinceWindChargeHit() < WIND_CHARGE_HIT_TICKS;
    }

    public static String windChargeReason(MovementState movement) {
        if (movement.ticksSinceWindCharge() < WIND_CHARGE_TICKS) {
            return "wind charge used " + movement.ticksSinceWindCharge() + " tick(s) ago";
        }
        if (movement.ticksSinceWindChargeHit() < WIND_CHARGE_HIT_TICKS) {
            return "wind charged " + movement.ticksSinceWindChargeHit() + " tick(s) ago";
        }
        return "";
    }
}
