package dev.snuffac.core.packet;

public record EntityActionPacket(long arrivalNanos, EntityAction action) implements SnuffPacket {

    public enum EntityAction {
        START_SNEAKING,
        STOP_SNEAKING,
        LEAVE_BED,
        START_SPRINTING,
        STOP_SPRINTING,
        START_HORSE_JUMP,
        STOP_HORSE_JUMP,
        OPEN_HORSE_INVENTORY,
        START_ELYTRA_FLYING
    }

    @Override
    public PacketType type() {
        return PacketType.ENTITY_ACTION;
    }

    @Override
    public PacketDirection direction() {
        return PacketDirection.INBOUND;
    }

    public boolean togglesSneak() {
        return action == EntityAction.START_SNEAKING || action == EntityAction.STOP_SNEAKING;
    }

    public boolean togglesSprint() {
        return action == EntityAction.START_SPRINTING || action == EntityAction.STOP_SPRINTING;
    }
}
