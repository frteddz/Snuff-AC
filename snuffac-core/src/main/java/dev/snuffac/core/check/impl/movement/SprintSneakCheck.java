package dev.snuffac.core.check.impl.movement;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.SnuffPacket;
import java.util.Map;

public final class SprintSneakCheck extends AbstractMovementCheck {

    public static final double DIAGONAL_YAW = 45.0;
    public static final double STRAFE_LIMIT = 2.5;
    public static final double BACK_LIMIT = 1.0;
    public static final int REQUIRED_TICKS = 5;
    public static final double MIN_HORIZONTAL = 0.05;
    private static final double INPUT_SCALE = 0.98;

    @Override
    public String key() {
        return "sprintsneak";
    }

    @Override
    public String name() {
        return "SprintSneak";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.MOVEMENT;
    }

    @Override
    public String description() {
        return "Validates the sprint and sneak state the client reports: sprinting while moving "
                + "backwards or strafing at full speed, and sneak speed while not crouching.";
    }

    @Override
    public Object createState() {
        return new SprintState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        if (!isPositionUpdate(packet)) {
            return;
        }
        var state = (SprintState) state(context.player());
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
                || environment.riding() || environment.onClimbable()
                || environment.usingItem()) {
            state.reset();
            return;
        }

        double horizontal = movement.delta().horizontalLength();
        if (horizontal < MIN_HORIZONTAL) {
            state.reset();
            return;
        }

        double speed = horizontal / Math.max(0.05, environment.slipperiness() * INPUT_SCALE);
        double facing = facingAngle(movement.yaw(), movement.delta());

        if (movement.sprinting() && movement.ticksSprinting() > 2 && !environment.sneaking()) {
            if (facing >= 180.0 - BACK_LIMIT) {
                state.report(context, "sprinting backwards", facing, speed);
                return;
            }
            if (facing >= DIAGONAL_YAW && horizontal > STRAFE_LIMIT) {
                state.report(context, "sprinting sideways", facing, speed);
                return;
            }
        }

        state.ticks++;
        if (state.ticks < REQUIRED_TICKS) {
            return;
        }
        state.reset();

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("sprinting", movement.sprinting());
        evidence.put("sneaking", movement.sneaking());
        evidence.put("ticksSprinting", movement.ticksSprinting());
        evidence.put("ticksSneaking", movement.ticksSneaking());
        evidence.put("facingOffset", round(facing));
        evidence.put("horizontal", round(horizontal));
        evidence.put("speed", round(speed));
        evidence.put("slipperiness", round(environment.slipperiness()));
        evidence.put("onGround", environment.onGroundFromClient());
    }

    public static double facingAngle(double yaw, Vec3d delta) {
        double dx = delta.x();
        double dz = delta.z();
        if (Math.abs(dx) < 1.0E-9 && Math.abs(dz) < 1.0E-9) {
            return 0.0;
        }
        double moveYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double difference = Math.abs(moveYaw - yaw) % 360.0;
        return difference > 180.0 ? 360.0 - difference : difference;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    static final class SprintState {

        private int ticks;

        private void report(CheckContext context, String what, double facing, double speed) {
            Map<String, Object> evidence = context.newEvidence();
            evidence.put("mode", what);
            evidence.put("facingOffset", round(facing));
            evidence.put("speed", round(speed));
            evidence.put("sprinting", true);
            evidence.put("ticksSprinting", context.player().movement().ticksSprinting());
            context.flag(what + ", which the vanilla client does not allow", evidence, 6.0);
            reset();
        }

        private void reset() {
            ticks = 0;
        }
    }

}