package dev.snuffac.core.player;

public final class EquipmentState {

    private double miningSpeed = 1.0;
    private double blockBreakSpeed = 1.0;
    private boolean validTool;
    private int heldItemSlot = -1;
    private boolean holdingPlaceable;
    private boolean wearingElytra;
    private HeldKind held = HeldKind.OTHER;
    private String weaponType = "AIR";
    private boolean weaponAttributeActive;
    private double attackReach = 3.0;
    private double attackDamage = 1.0;
    private double attackSpeed = 4.0;
    private double observedAttackReach = 3.0;

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

    public void update(
            double miningSpeed,
            double blockBreakSpeed,
            boolean validTool,
            int heldItemSlot,
            boolean holdingPlaceable,
            boolean wearingElytra) {
        update(miningSpeed, blockBreakSpeed, validTool, heldItemSlot, holdingPlaceable);
        this.wearingElytra = wearingElytra;
    }

    public void update(
            double miningSpeed,
            double blockBreakSpeed,
            boolean validTool,
            int heldItemSlot,
            boolean holdingPlaceable,
            boolean wearingElytra,
            HeldKind held) {
        update(miningSpeed, blockBreakSpeed, validTool, heldItemSlot, holdingPlaceable, wearingElytra);
        this.held = held == null ? HeldKind.OTHER : held;
    }

    public HeldKind held() {
        return held;
    }

    public enum HeldKind {
        OTHER,
        FOOD,
        POTION,
        BLOCK
    }

    public boolean wearingElytra() {
        return wearingElytra;
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

    public double observedAttackReach() {
        return observedAttackReach;
    }

    public void observedAttackReach(double value) {
        this.observedAttackReach = value;
    }

    public double attackReach() {
        return attackReach;
    }

    public void attackReach(double value) {
        this.attackReach = Math.max(0.0, value);
    }

    public double attackDamage() {
        return attackDamage;
    }

    public void attackDamage(double value) {
        this.attackDamage = value;
    }

    public double attackSpeed() {
        return attackSpeed;
    }

    public void attackSpeed(double value) {
        this.attackSpeed = value;
    }

    public boolean reachObservable() {
        return observedAttackReach > 0.0;
    }

    public double reachDelta() {
        return observedAttackReach - attackReach;
    }

    public boolean reachInconsistent() {
        return observedAttackReach > 0.0
                && Math.abs(observedAttackReach - attackReach) > 0.05;
    }

    public void reset() {
        this.miningSpeed = 1.0;
        this.blockBreakSpeed = 1.0;
        this.validTool = false;
        this.heldItemSlot = -1;
        this.holdingPlaceable = false;
        this.wearingElytra = false;
        this.held = HeldKind.OTHER;
        this.weaponType = "AIR";
        this.weaponAttributeActive = false;
        this.attackReach = 3.0;
        this.attackDamage = 1.0;
        this.attackSpeed = 4.0;
        this.observedAttackReach = 3.0;
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
