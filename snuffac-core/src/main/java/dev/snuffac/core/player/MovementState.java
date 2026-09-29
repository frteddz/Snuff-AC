package dev.snuffac.core.player;

import dev.snuffac.api.Vec3d;
import dev.snuffac.core.physics.MovementAttributes;
import dev.snuffac.core.physics.MovementEnvironment;
import dev.snuffac.core.tolerance.ToleranceModel;

public final class MovementState {

    private final ToleranceModel tolerance;

    private Vec3d position = Vec3d.ZERO;
    private Vec3d lastPosition = Vec3d.ZERO;
    private Vec3d delta = Vec3d.ZERO;
    private Vec3d velocity = Vec3d.ZERO;
    private Vec3d clientVelocity = Vec3d.ZERO;
    private Vec3d predictedVelocity = Vec3d.ZERO;
    private Vec3d lastOffset = Vec3d.ZERO;
    private Vec3d observedOffset = Vec3d.ZERO;
    private Vec3d observedPosition;
    private int ticksSincePlace = 9999;
    private int ticksSinceBreak = 9999;
    private long movementTick = Long.MIN_VALUE;
    private int movementPacketsThisTick;
    private int ticksSinceWindCharge = 9999;
    private int ticksSinceWindChargeHit = 9999;
    private boolean riptiding;
    private int ticksSinceKnockback = 9999;

    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;

    private boolean onGround;
    private boolean lastOnGround;
    private boolean serverOnGround;
    private boolean horizontalCollision;
    private boolean lastPositionValid;

    private long movementCount;
    private long lastPositionNanos;
    private long lastRotationNanos;

    private MovementAttributes attributes = MovementAttributes.DEFAULT;
    private MovementEnvironment environment = MovementEnvironment.AIR;

    private double fallDistance;
    private double lastFallDistance;
    private double distanceFallen;

    private int ticksSinceGround;
    private int ticksSinceLiquid;
    private int ticksSinceClimbable;
    private int ticksSinceGlide;
    private int ticksSinceTeleport;
    private int ticksSinceVehicle;
    private int ticksSinceBlockChange;
    private int ticksSinceSlime;
    private int ticksOnIce;
    private int ticksUsingItem;
    private int ticksSprinting;
    private int ticksSneaking;
    private int ticksOnFire;
    private int ticksInLiquid;

    private double speedMultiplier = 1.0;
    private double slownessMultiplier = 1.0;
    private int jumpBoostLevel;
    private int speedLevel;
    private int slownessLevel;
    private boolean hasSlowFalling;
    private boolean hasLevitation;
    private int levitationAmplifier;
    private boolean swimming;
    private boolean inWater;
    private boolean inLava;
    private boolean onClimbable;
    private boolean onHoney;
    private boolean onSlime;
    private boolean onIce;
    private boolean onSoulSand;
    private boolean gliding;
    private boolean flying;
    private boolean riding;
    private boolean sprinting;
    private boolean sneaking;
    private boolean usingItem;
    private boolean inVehicle;

    private boolean teleportedThisTick;
    private boolean serverTeleportPending;
    private boolean pendingSetback;
    private Vec3d setbackTarget = Vec3d.ZERO;
    private long setbackTick = -1L;

    public MovementState(ToleranceModel tolerance) {
        this.tolerance = tolerance;
    }

    public ToleranceModel tolerance() {
        return tolerance;
    }

    public Vec3d position() {
        return position;
    }

    public void position(Vec3d value) {
        this.position = value;
    }

    public Vec3d lastPosition() {
        return lastPosition;
    }

    public void lastPosition(Vec3d value) {
        this.lastPosition = value;
    }

    public Vec3d delta() {
        return delta;
    }

    public void advanceTo(Vec3d newPosition, long arrivalNanos) {
        this.lastPosition = this.position;
        this.position = newPosition;
        this.delta = newPosition.subtract(this.lastPosition);
        this.lastPositionNanos = arrivalNanos;
        this.movementCount++;
        this.lastPositionValid = true;
    }

    public Vec3d velocity() {
        return velocity;
    }

    public void velocity(Vec3d value) {
        this.velocity = value;
    }

    public Vec3d clientVelocity() {
        return clientVelocity;
    }

    public void clientVelocity(Vec3d value) {
        this.clientVelocity = value;
    }

    public Vec3d predictedVelocity() {
        return predictedVelocity;
    }

    public void predictedVelocity(Vec3d value) {
        this.predictedVelocity = value;
    }

    public Vec3d lastOffset() {
        return lastOffset;
    }

    public void lastOffset(Vec3d value) {
        this.lastOffset = value;
    }

    public Vec3d observedOffset() {
        return observedOffset;
    }

    public int movementPacketsThisTick() {
        return movementPacketsThisTick;
    }

    public void observeMovementPacket(long tickCounter) {
        if (tickCounter != movementTick) {
            movementTick = tickCounter;
            movementPacketsThisTick = 0;
        }
        movementPacketsThisTick++;
    }

    public void observePositionDelta(dev.snuffac.core.packet.MovementPacket packet) {
        if (!packet.positionChanged()) {
            return;
        }
        Vec3d reported = packet.position();
        if (reported == null) {
            return;
        }
        if (observedPosition != null && !teleportedThisTick) {
            observedOffset = reported.subtract(observedPosition);
        }
        observedPosition = reported;
    }

    public int ticksSincePlace() {
        return ticksSincePlace;
    }

    public int ticksSinceBreak() {
        return ticksSinceBreak;
    }

    public int ticksSinceWindCharge() {
        return ticksSinceWindCharge;
    }

    public int ticksSinceWindChargeHit() {
        return ticksSinceWindChargeHit;
    }

    public boolean riptiding() {
        return riptiding;
    }

    public void riptiding(boolean value) {
        this.riptiding = value;
    }

    public int ticksSinceKnockback() {
        return ticksSinceKnockback;
    }

    public void markKnockback() {
        ticksSinceKnockback = 0;
    }

    public void markWindCharge() {
        ticksSinceWindCharge = 0;
    }

    public void markWindChargeHit() {
        ticksSinceWindChargeHit = 0;
    }

    public void tickWindChargeCounters() {
        if (ticksSinceKnockback < 10000) {
            ticksSinceKnockback++;
        }
        if (ticksSinceWindCharge < 10000) {
            ticksSinceWindCharge++;
        }
        if (ticksSinceWindChargeHit < 10000) {
            ticksSinceWindChargeHit++;
        }
    }

    public void markPlaced() {
        ticksSincePlace = 0;
    }

    public void markBroke() {
        ticksSinceBreak = 0;
    }

    public void tickPlaceCounters() {
        if (ticksSincePlace < 10000) {
            ticksSincePlace++;
        }
        if (ticksSinceBreak < 10000) {
            ticksSinceBreak++;
        }
    }

    public float yaw() {
        return yaw;
    }

    public void yaw(float value) {
        this.yaw = value;
    }

    public float pitch() {
        return pitch;
    }

    public void pitch(float value) {
        this.pitch = value;
    }

    public float lastYaw() {
        return lastYaw;
    }

    public float lastPitch() {
        return lastPitch;
    }

    public void advanceRotation(float newYaw, float newPitch, long arrivalNanos) {
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.yaw = newYaw;
        this.pitch = newPitch;
        this.lastRotationNanos = arrivalNanos;
    }

    public boolean onGround() {
        return onGround;
    }

    public void onGround(boolean value) {
        this.onGround = value;
    }

    public boolean lastOnGround() {
        return lastOnGround;
    }

    public void advanceGround(boolean value) {
        this.lastOnGround = this.onGround;
        this.onGround = value;
    }

    public boolean serverOnGround() {
        return serverOnGround;
    }

    public void serverOnGround(boolean value) {
        this.serverOnGround = value;
    }

    public boolean horizontalCollision() {
        return horizontalCollision;
    }

    public void horizontalCollision(boolean value) {
        this.horizontalCollision = value;
    }

    public boolean lastPositionValid() {
        return lastPositionValid;
    }

    public long movementCount() {
        return movementCount;
    }

    public long lastPositionNanos() {
        return lastPositionNanos;
    }

    public long lastRotationNanos() {
        return lastRotationNanos;
    }

    public MovementAttributes attributes() {
        return attributes;
    }

    public void attributes(MovementAttributes value) {
        this.attributes = value;
    }

    public MovementEnvironment environment() {
        return environment;
    }

    public void environment(MovementEnvironment value) {
        this.environment = value;
    }

    public double fallDistance() {
        return fallDistance;
    }

    public void fallDistance(double value) {
        this.fallDistance = value;
    }

    public double lastFallDistance() {
        return lastFallDistance;
    }

    public void advanceFallDistance(double value) {
        this.lastFallDistance = this.fallDistance;
        this.fallDistance = value;
    }

    public double distanceFallen() {
        return distanceFallen;
    }

    public void distanceFallen(double value) {
        this.distanceFallen = value;
    }

    public int ticksSinceGround() {
        return ticksSinceGround;
    }

    public int ticksSinceLiquid() {
        return ticksSinceLiquid;
    }

    public int ticksSinceClimbable() {
        return ticksSinceClimbable;
    }

    public int ticksSinceGlide() {
        return ticksSinceGlide;
    }

    public int ticksSinceTeleport() {
        return ticksSinceTeleport;
    }

    public int ticksSinceVehicle() {
        return ticksSinceVehicle;
    }

    public int ticksSinceBlockChange() {
        return ticksSinceBlockChange;
    }

    public int ticksSinceSlime() {
        return ticksSinceSlime;
    }

    public int ticksOnIce() {
        return ticksOnIce;
    }

    public int ticksUsingItem() {
        return ticksUsingItem;
    }

    public int ticksSprinting() {
        return ticksSprinting;
    }

    public int ticksSneaking() {
        return ticksSneaking;
    }

    public int ticksOnFire() {
        return ticksOnFire;
    }

    public int ticksInLiquid() {
        return ticksInLiquid;
    }

    public void tickCounters() {
        tickPlaceCounters();
        tickWindChargeCounters();
        ticksSinceGround = onGround ? 0 : ticksSinceGround + 1;
        ticksSinceLiquid = inWaterOrLava() ? 0 : ticksSinceLiquid + 1;
        ticksSinceClimbable = onClimbable ? 0 : ticksSinceClimbable + 1;
        ticksSinceGlide = gliding ? 0 : ticksSinceGlide + 1;
        ticksSinceTeleport = teleportedThisTick ? 0 : ticksSinceTeleport + 1;
        ticksSinceVehicle = inVehicle ? 0 : ticksSinceVehicle + 1;
        ticksSinceBlockChange++;
        ticksSinceSlime++;
        ticksOnIce = onIce ? ticksOnIce + 1 : 0;
        ticksUsingItem = usingItem ? ticksUsingItem + 1 : 0;
        ticksSprinting = sprinting ? ticksSprinting + 1 : 0;
        ticksSneaking = sneaking ? ticksSneaking + 1 : 0;
        ticksOnFire = ticksOnFire > 0 ? ticksOnFire - 1 : 0;
        ticksInLiquid = inWaterOrLava() ? ticksInLiquid + 1 : 0;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public void speedMultiplier(double value) {
        this.speedMultiplier = value;
    }

    public double slownessMultiplier() {
        return slownessMultiplier;
    }

    public void slownessMultiplier(double value) {
        this.slownessMultiplier = value;
    }

    public int jumpBoostLevel() {
        return jumpBoostLevel;
    }

    public int speedLevel() {
        return speedLevel;
    }

    public int slownessLevel() {
        return slownessLevel;
    }

    public void effects(int jumpBoost, int speed, int slowness) {
        this.jumpBoostLevel = jumpBoost;
        this.speedLevel = speed;
        this.slownessLevel = slowness;
    }

    public boolean hasSlowFalling() {
        return hasSlowFalling;
    }

    public void hasSlowFalling(boolean value) {
        this.hasSlowFalling = value;
    }

    public boolean hasLevitation() {
        return hasLevitation;
    }

    public void hasLevitation(boolean value) {
        this.hasLevitation = value;
    }

    public int levitationAmplifier() {
        return levitationAmplifier;
    }

    public void levitation(boolean present, int amplifier) {
        this.hasLevitation = present;
        this.levitationAmplifier = amplifier;
    }

    public boolean swimming() {
        return swimming;
    }

    public void swimming(boolean value) {
        this.swimming = value;
    }

    public boolean inWater() {
        return inWater;
    }

    public void inWater(boolean value) {
        this.inWater = value;
    }

    public boolean inLava() {
        return inLava;
    }

    public void inLava(boolean value) {
        this.inLava = value;
    }

    public boolean inWaterOrLava() {
        return inWater || inLava;
    }

    public boolean onClimbable() {
        return onClimbable;
    }

    public void onClimbable(boolean value) {
        this.onClimbable = value;
    }

    public boolean onHoney() {
        return onHoney;
    }

    public void onHoney(boolean value) {
        this.onHoney = value;
    }

    public boolean onSlime() {
        return onSlime;
    }

    public void onSlime(boolean value) {
        this.onSlime = value;
    }

    public boolean onIce() {
        return onIce;
    }

    public void onIce(boolean value) {
        this.onIce = value;
    }

    public boolean onSoulSand() {
        return onSoulSand;
    }

    public void onSoulSand(boolean value) {
        this.onSoulSand = value;
    }

    public boolean gliding() {
        return gliding;
    }

    public void gliding(boolean value) {
        this.gliding = value;
    }

    public boolean flying() {
        return flying;
    }

    public void flying(boolean value) {
        this.flying = value;
    }

    public boolean riding() {
        return riding;
    }

    public void riding(boolean value) {
        this.riding = value;
    }

    public boolean sprinting() {
        return sprinting;
    }

    public void sprinting(boolean value) {
        this.sprinting = value;
    }

    public boolean sneaking() {
        return sneaking;
    }

    public void sneaking(boolean value) {
        this.sneaking = value;
    }

    public boolean usingItem() {
        return usingItem;
    }

    public void usingItem(boolean value) {
        this.usingItem = value;
    }

    public boolean inVehicle() {
        return inVehicle;
    }

    public void inVehicle(boolean value) {
        this.inVehicle = value;
    }

    public boolean teleportedThisTick() {
        return teleportedThisTick;
    }

    public void teleportedThisTick(boolean value) {
        this.teleportedThisTick = value;
    }

    public boolean serverTeleportPending() {
        return serverTeleportPending;
    }

    public void serverTeleportPending(boolean value) {
        this.serverTeleportPending = value;
    }

    public boolean pendingSetback() {
        return pendingSetback;
    }

    public Vec3d setbackTarget() {
        return setbackTarget;
    }

    public void requestSetback(Vec3d target) {
        this.pendingSetback = true;
        this.setbackTarget = target;
    }

    public long setbackTick() {
        return setbackTick;
    }

    public void setbackTick(long value) {
        this.setbackTick = value;
    }

    public void reset() {
        lastPosition = position;
        delta = Vec3d.ZERO;
        velocity = Vec3d.ZERO;
        clientVelocity = Vec3d.ZERO;
        predictedVelocity = Vec3d.ZERO;
        lastOffset = Vec3d.ZERO;
        lastPositionValid = false;
        tolerance.reset();
    }
}
