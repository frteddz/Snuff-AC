package dev.snuffac.core.player;

public final class EquipmentState {

    private double miningSpeed = 1.0;
    private double blockBreakSpeed = 1.0;
    private boolean validTool;
    private int heldItemSlot = -1;
    private boolean holdingPlaceable;
    private String weaponType = "AIR";
    private boolean weaponAttributeActive;

    public static final EquipmentState DEFAULT = new EquipmentState();

    public double miningSpeed() {
        return miningSpeed;
    }

    public double blockBreakSpeed() {
        return blockBreakSpeed;
    }

    public boolean validTool() {
        return validTool;
    }

    public int heldItemSlot() {
        return heldItemSlot;
    }

    public boolean holdingPlaceable() {
        return holdingPlaceable;
    }

    public void update(
            double miningSpeed,
            double blockBreakSpeed,
            boolean validTool,
            int heldItemSlot,
            boolean holdingPlaceable) {
        this.miningSpeed = Math.max(miningSpeed, 0.0);
        this.blockBreakSpeed = blockBreakSpeed;
        this.validTool = validTool;
        this.heldItemSlot = heldItemSlot;
        this.holdingPlaceable = holdingPlaceable;
    }

    public String weaponType() {
        return weaponType;
    }

    public void weaponType(String value) {
        this.weaponType = value == null || value.isEmpty() ? "AIR" : value;
    }

    public boolean weaponAttributeActive() {
        return weaponAttributeActive;
    }

    public void weaponAttributeActive(boolean value) {
        this.weaponAttributeActive = value;
    }

    public void reset() {
        this.miningSpeed = 1.0;
        this.blockBreakSpeed = 1.0;
        this.validTool = false;
        this.heldItemSlot = -1;
        this.holdingPlaceable = false;
        this.weaponType = "AIR";
        this.weaponAttributeActive = false;
    }

    public static double breakTicks(double hardness, double miningSpeed, double blockBreakSpeed) {
        if (hardness <= 0.0) {
            return 0.0;
        }
        double speed = Math.max(miningSpeed, 0.05) * Math.max(blockBreakSpeed, 0.05);
        return hardness * 30.0 / speed;
    }

    public static double breakMillis(double hardness, double miningSpeed, double blockBreakSpeed) {
        return breakTicks(hardness, miningSpeed, blockBreakSpeed) * 50.0;
    }
}
