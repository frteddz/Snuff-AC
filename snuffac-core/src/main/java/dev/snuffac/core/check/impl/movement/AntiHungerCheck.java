package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class AntiHungerCheck extends AbstractMovementCheck {

    public static final double EXHAUSTION_PER_BLOCK_SPRINTING = 0.1;
    public static final double EXHAUSTION_PER_JUMP = 0.2;
    public static final double EXHAUSTION_PER_HUNGER_POINT = 4.0;
    public static final double REQUIRED_RATIO = 0.35;
    public static final int REQUIRED_TICKS = 80;
    public static final double MIN_EXPECTED_HUNGER = 0.5;

    @Override
    public String key() {
        return "antihunger";
    }

    @Override
    public String name() {
        return "AntiHunger";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Compares the exhaustion a player should be accumulating from sprinting and "
                + "jumping against the hunger the server actually observed, which is the "
                + "AntiHunger cheat.";
    }

    @Override
    public Object createState() {
        return new HungerState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (HungerState) state(context.player());
        if (state == null) {
            return;
        }

        var player = context.player();
        var movement = player.movement();
        var environment = environment(context);

        if (!dev.snuffac.core.check.CheckDispatcher.globalGate(
                player, context.tps(), context.ping())) {
            return;
        }
        if (!canPredict(player) || movement.ticksSinceTeleport() <= 3
                || movement.pendingSetback()) {
            state.reset();
            return;
        }
        if (!environment.onGroundFromClient() || environment.inWaterOrLava()
                || environment.riding() || environment.onClimbable()) {
            state.reset();
            return;
        }

        double horizontal = movement.delta().horizontalLength();
        if (environment.sprinting() && horizontal > 0.05) {
            state.expectedExhaustion += horizontal * EXHAUSTION_PER_BLOCK_SPRINTING;
        }
        if (movement.ticksSinceGround() == 1 && movement.delta().y() > 0.2) {
            state.expectedExhaustion += EXHAUSTION_PER_JUMP;
        }
        state.ticks++;

        int food = player.movement().foodLevel();
        if (state.lastFood < 0) {
            state.lastFood = food;
            return;
        }
        int foodDrop = Math.max(0, state.lastFood - food);
        state.lastFood = food;
        state.observedDrop += foodDrop;

        double expectedHunger = state.expectedExhaustion / EXHAUSTION_PER_HUNGER_POINT;
        if (state.ticks < REQUIRED_TICKS || expectedHunger < MIN_EXPECTED_HUNGER) {
            return;
        }

        double ratio = (double) state.observedDrop / expectedHunger;
        if (ratio >= REQUIRED_RATIO) {
            state.forget();
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("expectedExhaustion",
                Math.round(state.expectedExhaustion * 10000.0) / 10000.0);
        evidence.put("expectedHungerDrop", Math.round(expectedHunger * 10000.0) / 10000.0);
        evidence.put("observedHungerDrop", state.observedDrop);
        evidence.put("ratio", Math.round(ratio * 10000.0) / 10000.0);
        evidence.put("requiredRatio", REQUIRED_RATIO);
        evidence.put("ticks", state.ticks);
        evidence.put("foodLevel", food);
        evidence.put("sprinting", environment.sprinting());

        context.flag("lost " + state.observedDrop + " hunger while sprinting should have cost "
                + Math.round(expectedHunger * 100.0) / 100.0 + ", a ratio of "
                + Math.round(ratio * 100.0) / 100.0, evidence, 8.0);
        state.forget();
    }

    static final class HungerState {

        private int ticks;
        private double expectedExhaustion;
        private int observedDrop;
        private int lastFood = -1;

        private void reset() {
            ticks = 0;
            expectedExhaustion = 0.0;
            observedDrop = 0;
        }

        private void forget() {
            reset();
            lastFood = -1;
        }
    }
}