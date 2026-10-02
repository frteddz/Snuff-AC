package dev.snuffac.core.check.impl.world;

import dev.snuffac.api.CheckCategory;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.check.Check;
import dev.snuffac.core.check.CheckContext;
import dev.snuffac.core.combat.ReachResolver;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PacketType;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.EquipmentState.HeldKind;
import dev.snuffac.core.util.AxisAlignedBox;
import dev.snuffac.core.util.BlockFace;
import dev.snuffac.core.util.BlockPos;
import java.util.Map;
import java.util.Set;

public final class LiquidInteractCheck implements Check {

    public static final double REACH_LIMIT = 5.5;
    public static final int REQUIRED = 3;
    public static final double FACE_ANGLE_LIMIT = 75.0;

    @Override
    public Set<PacketType> packetInterests() {
        return Set.of(PacketType.BLOCK_PLACE, PacketType.MOVEMENT);
    }

    @Override
    public String key() {
        return "liquidinteract";
    }

    @Override
    public String name() {
        return "LiquidInteract";
    }

    @Override
    public CheckCategory category() {
        return CheckCategory.WORLD;
    }

    @Override
    public String description() {
        return "Reaches block use and placement through liquids and around corners from outside "
                + "the angle the player is looking from.";
    }

    @Override
    public Object createState() {
        return new LiquidState();
    }

    @Override
    public void onPacket(CheckContext context, SnuffPacket packet) {
        var state = (LiquidState) state(context.player());
        if (state == null) {
            return;
        }

        if (packet instanceof MovementPacket movement) {
            if (movement.rotationChanged()) {
                state.lastYaw = movement.yaw();
                state.lastPitch = movement.pitch();
            }
            return;
        }
        if (!(packet instanceof BlockPlacePacket place)) {
            return;
        }

        var player = context.player();
        var movementState = player.movement();
        if (movementState.ticksSinceTeleport() <= 1 || movementState.pendingSetback()) {
            state.reset();
            return;
        }
        if (!player.equipment().holdingPlaceable()
                && player.equipment().held() != HeldKind.OTHER) {
            state.reset();
            return;
        }

        BlockPos position = BlockPos.unpack(place.packedPosition());
        BlockFace face = BlockFace.byId(place.faceId());
        Vec3d target = new Vec3d(
                position.x() + 0.5 + face.stepX() * 0.5,
                position.y() + 0.5 + face.stepY() * 0.5,
                position.z() + 0.5 + face.stepZ() * 0.5);
        Vec3d eye = ReachResolver.eyePosition(player.position(), movementState.sneaking());

        double distance = eye.distanceTo(target);
        double angle = HitboxAngles.angleTo(
                eye, movementState.yaw(), movementState.pitch(), target);
        boolean throughLiquid = crossesLiquid(context, eye, target);

        if (distance <= REACH_LIMIT && angle <= FACE_ANGLE_LIMIT && !throughLiquid) {
            state.reset();
            return;
        }

        state.strikes++;
        if (state.strikes < REQUIRED) {
            return;
        }

        Map<String, Object> evidence = context.newEvidence();
        evidence.put("distance", Math.round(distance * 100.0) / 100.0);
        evidence.put("reachLimit", REACH_LIMIT);
        evidence.put("angle", Math.round(angle * 100.0) / 100.0);
        evidence.put("angleLimit", FACE_ANGLE_LIMIT);
        evidence.put("face", face.name());
        evidence.put("block", position.toString());
        evidence.put("throughLiquid", throughLiquid);
        evidence.put("consecutive", state.strikes);
        evidence.put("heldItem", player.equipment().held().name());
        evidence.put("sneaking", movementState.sneaking());

        context.preventInteraction("interacted with " + position + " from an impossible angle");
        context.flag("used " + position + " at " + Math.round(angle)
                + " degrees off the look direction", evidence, 7.0);
        state.reset();
    }

    public static boolean crossesLiquid(CheckContext context, Vec3d from, Vec3d to) {
        var cache = context.player().worldCache();
        if (!cache.chunkLoaded()) {
            return false;
        }
        int steps = (int) Math.ceil(from.distanceTo(to) / 0.3);
        if (steps < 1) {
            steps = 1;
        }
        Vec3d delta = to.subtract(from);
        for (int i = 1; i < steps; i++) {
            Vec3d sample = from.add(delta.multiply((double) i / steps));
            if (cache.isLiquid(BlockPos.of(sample))) {
                return true;
            }
        }
        return false;
    }

    static final class HitboxAngles {

        static double angleTo(Vec3d eye, double yaw, double pitch, Vec3d target) {
            double yawRadians = Math.toRadians(yaw);
            double pitchRadians = Math.toRadians(pitch);
            Vec3d facing = new Vec3d(
                    -Math.sin(yawRadians) * Math.cos(pitchRadians),
                    -Math.sin(pitchRadians),
                    Math.cos(yawRadians) * Math.cos(pitchRadians));
            Vec3d to = target.subtract(eye);
            double length = to.length();
            if (length < 1.0E-9) {
                return 0.0;
            }
            double dot = (facing.x() * to.x() + facing.y() * to.y() + facing.z() * to.z()) / length;
            dot = Math.max(-1.0, Math.min(1.0, dot));
            return Math.toDegrees(Math.acos(dot));
        }
    }

    static final class LiquidState {

        private int strikes;
        private float lastYaw;
        private float lastPitch;

        private void reset() {
            strikes = 0;
            lastYaw = 0.0f;
            lastPitch = 0.0f;
        }
    }
}