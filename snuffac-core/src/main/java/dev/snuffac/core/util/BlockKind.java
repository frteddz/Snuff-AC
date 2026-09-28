package dev.snuffac.core.util;

public enum BlockKind {

    AIR(true, 0.6, false, false, false),
    SOLID(false, 0.6, false, false, false),
    SLIME(false, 0.8, false, false, false),
    ICE(false, 0.98, false, false, false),
    PACKED_ICE(false, 0.98, false, false, false),
    BLUE_ICE(false, 0.989, false, false, false),
    HONEY(false, 0.8, false, false, false),
    SOUL_SAND(false, 0.48, false, false, false),
    LADDER(true, 0.6, true, false, false),
    VINE(true, 0.6, true, false, false),
    SCAFFOLDING(true, 0.6, true, false, false),
    COBWEB(true, 0.6, false, false, true),
    WATER(true, 0.6, false, true, false),
    LAVA(true, 0.6, false, true, false),
    CACTUS(true, 0.6, false, false, true),
    POWDER_SNOW(true, 0.6, false, false, true),
    BEDROCK(false, 0.6, false, false, false),
    BARRIER(true, 0.6, false, false, false),
    UNKNOWN(false, 0.6, false, false, false);

    private final boolean passable;
    private final double slipperiness;
    private final boolean climbable;
    private final boolean liquid;
    private final boolean damageSource;

    BlockKind(boolean passable, double slipperiness, boolean climbable, boolean liquid, boolean damageSource) {
        this.passable = passable;
        this.slipperiness = slipperiness;
        this.climbable = climbable;
        this.liquid = liquid;
        this.damageSource = damageSource;
    }

    public boolean passable() {
        return passable;
    }

    public double slipperiness() {
        return slipperiness;
    }

    public boolean climbable() {
        return climbable;
    }

    public boolean liquid() {
        return liquid;
    }

    public boolean damageSource() {
        return damageSource;
    }

    public boolean ice() {
        return this == ICE || this == PACKED_ICE || this == BLUE_ICE;
    }

    public boolean bouncy() {
        return this == SLIME || this == HONEY;
    }

    public boolean slime() {
        return this == SLIME;
    }

    public boolean honey() {
        return this == HONEY;
    }
}
