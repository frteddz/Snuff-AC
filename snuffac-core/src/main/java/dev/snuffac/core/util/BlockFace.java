package dev.snuffac.core.util;

public enum BlockFace {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private static final BlockFace[] BY_ID = values();

    private final int stepX;
    private final int stepY;
    private final int stepZ;

    BlockFace(int stepX, int stepY, int stepZ) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
    }

    public static BlockFace byId(int id) {
        if (id < 0 || id >= BY_ID.length) {
            return DOWN;
        }
        return BY_ID[id];
    }

    public int stepX() {
        return stepX;
    }

    public int stepY() {
        return stepY;
    }

    public int stepZ() {
        return stepZ;
    }

    public boolean vertical() {
        return stepY != 0;
    }

    public boolean horizontal() {
        return stepY == 0;
    }
}
