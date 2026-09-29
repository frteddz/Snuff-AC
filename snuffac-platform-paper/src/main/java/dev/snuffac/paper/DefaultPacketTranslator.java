package dev.snuffac.paper;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import dev.snuffac.api.Vec3d;
import dev.snuffac.core.packet.AttackPacket;
import dev.snuffac.core.packet.BlockBreakPacket;
import dev.snuffac.core.packet.BlockPlacePacket;
import dev.snuffac.core.packet.EntityActionPacket;
import dev.snuffac.core.packet.KeepAlivePacket;
import dev.snuffac.core.packet.MovementPacket;
import dev.snuffac.core.packet.PlayerLoadedPacket;
import dev.snuffac.core.packet.ServerKeepAlivePacket;
import dev.snuffac.core.packet.SoundPacket;
import dev.snuffac.core.packet.ServerTeleportPacket;
import dev.snuffac.core.packet.ServerVelocityPacket;
import dev.snuffac.core.packet.SnuffPacket;
import dev.snuffac.core.player.MovementState;
import dev.snuffac.core.util.BlockPos;
import java.util.function.Function;

public final class DefaultPacketTranslator implements PacketTranslator {

    private final Function<User, MovementState> movementLookup;

    public DefaultPacketTranslator(Function<User, MovementState> movementLookup) {
        this.movementLookup = movementLookup;
    }

    @Override
    public SnuffPacket inbound(User user, PacketTypeCommon type, PacketReceiveEvent event, long arrivalNanos) {

        if (WrapperPlayClientPlayerFlying.isFlying(type)) {
            var wrapper = new WrapperPlayClientPlayerFlying(event);
            var location = wrapper.getLocation();
            return new MovementPacket(
                    arrivalNanos,
                    wrapper.hasPositionChanged(),
                    wrapper.hasRotationChanged(),
                    new Vec3d(location.getX(), location.getY(), location.getZ()),
                    location.getYaw(),
                    location.getPitch(),
                    wrapper.isOnGround(),
                    wrapper.isHorizontalCollision());
        }
        if (type == PacketType.Play.Client.INTERACT_ENTITY) {
            var wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return null;
            }
            var cursor = wrapper.getLocation();
            MovementState state = movementLookup.apply(user);
            float yaw = state == null ? 0.0f : state.yaw();
            float pitch = state == null ? 0.0f : state.pitch();
            return new AttackPacket(
                    arrivalNanos,
                    wrapper.getEntityId(),
                    new Vec3d(cursor.getX(), cursor.getY(), cursor.getZ()),
                    wrapper.isSneaking().orElse(Boolean.FALSE),
                    yaw,
                    pitch);
        }
        if (type == PacketType.Play.Client.PLAYER_DIGGING) {
            var wrapper = new WrapperPlayClientPlayerDigging(event);
            var position = wrapper.getBlockPosition();
            return new BlockBreakPacket(
                    arrivalNanos,
                    diggingAction(wrapper.getAction()),
                    BlockPos.pack(position.getX(), position.getY(), position.getZ()),
                    wrapper.getBlockFaceId(),
                    wrapper.getSequence());
        }
        if (type == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            var wrapper = new WrapperPlayClientPlayerBlockPlacement(event);
            var position = wrapper.getBlockPosition();
            var cursor = wrapper.getCursorPosition();
            return new BlockPlacePacket(
                    arrivalNanos,
                    BlockPos.pack(position.getX(), position.getY(), position.getZ()),
                    wrapper.getFaceId(),
                    cursor == null ? 0.0f : cursor.getX(),
                    cursor == null ? 0.0f : cursor.getY(),
                    cursor == null ? 0.0f : cursor.getZ(),
                    wrapper.getHand() == null ? 0 : wrapper.getHand().ordinal(),
                    wrapper.getSequence());
        }
        if (type == PacketType.Play.Client.KEEP_ALIVE) {
            return new KeepAlivePacket(arrivalNanos, new WrapperPlayClientKeepAlive(event).getId());
        }
        if (type == PacketType.Play.Client.ENTITY_ACTION) {
            return new EntityActionPacket(arrivalNanos, entityAction(new WrapperPlayClientEntityAction(event).getAction()));
        }
        if (type == PacketType.Play.Client.PLAYER_LOADED) {
            return new PlayerLoadedPacket(arrivalNanos);
        }
        return null;
    }

    @Override
    public SnuffPacket outbound(User user, PacketTypeCommon type, PacketSendEvent event, long arrivalNanos) {
        if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            var wrapper = new WrapperPlayServerPlayerPositionAndLook(event);
            var position = wrapper.getPosition();
            return new ServerTeleportPacket(
                    arrivalNanos,
                    new Vec3d(position.getX(), position.getY(), position.getZ()),
                    wrapper.getYaw(),
                    wrapper.getPitch(),
                    ServerTeleportPacket.TeleportCause.UNKNOWN);
        }
        if (type == PacketType.Play.Server.ENTITY_VELOCITY) {
            var wrapper = new WrapperPlayServerEntityVelocity(event);
            var velocity = wrapper.getVelocity();
            return new ServerVelocityPacket(
                    arrivalNanos,
                    wrapper.getEntityId(),
                    new Vec3d(velocity.getX(), velocity.getY(), velocity.getZ()));
        }
        if (type == PacketType.Play.Server.KEEP_ALIVE) {
            return new ServerKeepAlivePacket(arrivalNanos, new WrapperPlayServerKeepAlive(event).getId());
        }
        if (type == PacketType.Play.Server.SOUND_EFFECT
                || type == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
            var wrapper = new WrapperPlayServerSoundEffect(event);
            var position = wrapper.getPosition();
            if (position == null) {
                return null;
            }
            return new SoundPacket(
                    arrivalNanos,
                    new Vec3d(position.getX(), position.getY(), position.getZ()),
                    wrapper.getSound() == null ? "" : wrapper.getSound().getName().toString(),
                    wrapper.getVolume(),
                    wrapper.getPitch());
        }
        return null;
    }

    private static BlockBreakPacket.BlockBreakAction diggingAction(
            com.github.retrooper.packetevents.protocol.player.DiggingAction action) {
        return switch (action) {
            case START_DIGGING -> BlockBreakPacket.BlockBreakAction.START;
            case FINISHED_DIGGING -> BlockBreakPacket.BlockBreakAction.FINISH;
            case CANCELLED_DIGGING -> BlockBreakPacket.BlockBreakAction.CANCEL;
            default -> BlockBreakPacket.BlockBreakAction.ABORT;
        };
    }

    private static EntityActionPacket.EntityAction entityAction(
            WrapperPlayClientEntityAction.Action action) {
        if (action == null) {
            return EntityActionPacket.EntityAction.LEAVE_BED;
        }
        if (action == WrapperPlayClientEntityAction.Action.START_SNEAKING) {
            return EntityActionPacket.EntityAction.START_SNEAKING;
        }
        if (action == WrapperPlayClientEntityAction.Action.STOP_SNEAKING) {
            return EntityActionPacket.EntityAction.STOP_SNEAKING;
        }
        if (action == WrapperPlayClientEntityAction.Action.LEAVE_BED) {
            return EntityActionPacket.EntityAction.LEAVE_BED;
        }
        if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING) {
            return EntityActionPacket.EntityAction.START_SPRINTING;
        }
        if (action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
            return EntityActionPacket.EntityAction.STOP_SPRINTING;
        }
        if (action == WrapperPlayClientEntityAction.Action.OPEN_HORSE_INVENTORY) {
            return EntityActionPacket.EntityAction.OPEN_HORSE_INVENTORY;
        }
        if (action == WrapperPlayClientEntityAction.Action.START_JUMPING_WITH_HORSE) {
            return EntityActionPacket.EntityAction.START_HORSE_JUMP;
        }
        if (action == WrapperPlayClientEntityAction.Action.START_FLYING_WITH_ELYTRA) {
            return EntityActionPacket.EntityAction.START_ELYTRA_FLYING;
        }
        return EntityActionPacket.EntityAction.STOP_HORSE_JUMP;
    }
}
