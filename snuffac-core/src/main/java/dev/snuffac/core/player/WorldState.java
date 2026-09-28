package dev.snuffac.core.player;

public final class WorldState {

    private long digStartMillis;
    private long lastDigMillis;
    private long digCount;
    private int lastDigX;
    private int lastDigY;
    private int lastDigZ;
    private boolean digActive;
    private int digSequence;

    private long placeMillis;
    private long placeCount;
    private int lastPlaceX;
    private int lastPlaceY;
    private int lastPlaceZ;
    private long consecutiveSamePlace;

    private final long[] digTimestamps = new long[64];
    private final long[] placeTimestamps = new long[64];

    public void digStart(long millis, int x, int y, int z, int sequence) {
        this.digStartMillis = millis;
        this.lastDigMillis = millis;
        this.digCount++;
        this.lastDigX = x;
        this.lastDigY = y;
        this.lastDigZ = z;
        this.digActive = true;
        this.digSequence = sequence;
        int slot = (int) (digCount & 0x3F);
        digTimestamps[slot] = millis;
    }

    public void digStop(long millis) {
        this.lastDigMillis = millis;
        this.digActive = false;
    }

    public long digStartMillis() {
        return digStartMillis;
    }

    public long lastDigMillis() {
        return lastDigMillis;
    }

    public long digCount() {
        return digCount;
    }

    public int lastDigX() {
        return lastDigX;
    }

    public int lastDigY() {
        return lastDigY;
    }

    public int lastDigZ() {
        return lastDigZ;
    }

    public boolean digActive() {
        return digActive;
    }

    public int digSequence() {
        return digSequence;
    }

    public void place(long millis, int x, int y, int z) {
        if (x == lastPlaceX && y == lastPlaceY && z == lastPlaceZ && placeMillis > 0L) {
            consecutiveSamePlace++;
        } else {
            consecutiveSamePlace = 0;
        }
        this.placeMillis = millis;
        this.lastPlaceX = x;
        this.lastPlaceY = y;
        this.lastPlaceZ = z;
        this.placeCount++;
        int slot = (int) (placeCount & 0x3F);
        placeTimestamps[slot] = millis;
    }

    public long placeMillis() {
        return placeMillis;
    }

    public long placeCount() {
        return placeCount;
    }

    public int lastPlaceX() {
        return lastPlaceX;
    }

    public int lastPlaceY() {
        return lastPlaceY;
    }

    public int lastPlaceZ() {
        return lastPlaceZ;
    }

    public long consecutiveSamePlace() {
        return consecutiveSamePlace;
    }

    public int digsInWindow(long now, long windowMillis) {
        int count = 0;
        long threshold = now - windowMillis;
        for (long timestamp : digTimestamps) {
            if (timestamp > threshold && timestamp > 0L) {
                count++;
            }
        }
        return count;
    }

    public int placesInWindow(long now, long windowMillis) {
        int count = 0;
        long threshold = now - windowMillis;
        for (long timestamp : placeTimestamps) {
            if (timestamp > threshold && timestamp > 0L) {
                count++;
            }
        }
        return count;
    }

    public long digDuration(long now) {
        return digActive ? now - digStartMillis : lastDigMillis - digStartMillis;
    }

    public void reset() {
        digCount = 0;
        placeCount = 0;
        digActive = false;
        consecutiveSamePlace = 0;
        java.util.Arrays.fill(digTimestamps, 0L);
        java.util.Arrays.fill(placeTimestamps, 0L);
    }
}
