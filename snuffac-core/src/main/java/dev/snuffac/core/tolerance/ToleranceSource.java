package dev.snuffac.core.tolerance;

public enum ToleranceSource {

    EXTERNAL_PUSH(true),
    PISTON(true),
    BOUNCY_BLOCK(true),
    ITEM_USE_SLOWDOWN(true),
    ATTACK_SLOWDOWN(true),
    VEHICLE(true),
    SERVER_KNOCKBACK(true),
    EXPLOSION(true),
    RIPTIDE(true),
    TELEPORT(false),
    SETBACK(false),
    BLOCK_CHANGE(true),
    CHUNK_LOAD(true),
    SERVER_TICK_DESYNC(false),
    HIGH_PING(false),
    LOW_TPS(false),
    GAME_MODE(false),
    POSITION_EPSILON(false),
    LENIENCY_CARRYOVER(false);

    private final boolean expiring;

    ToleranceSource(boolean expiring) {
        this.expiring = expiring;
    }

    public boolean expiring() {
        return expiring;
    }
}
