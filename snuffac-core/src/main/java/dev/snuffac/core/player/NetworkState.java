package dev.snuffac.core.player;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NetworkState {

    private static final int MAX_PENDING_KEEP_ALIVES = 64;

    private final Map<Long, Long> pendingKeepAlives = new LinkedHashMap<>(16, 0.75f, false) {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest) {
            return size() > MAX_PENDING_KEEP_ALIVES;
        }
    };

    private double pingMillis = -1.0;
    private double smoothedPingMillis = -1.0;
    private double minPingMillis = -1.0;
    private double maxPingMillis = -1.0;
    private int keepAliveId;
    private int keepAlivesSent;
    private int keepAlivesReceived;
    private int keepAlivesMismatched;

    private long packetsReceived;
    private long packetsSent;
    private long packetsReceivedWindowStart;
    private long packetsSentWindowStart;
    private int packetsPerSecond;

    private long lastPacketNanos;
    private long lastMovementNanos;
    private long firstPacketNanos;

    private int invalidPackets;
    private int cancelledPackets;
    private int badPacketsThisSecond;
    private long badPacketWindowStart;

    private long tickCounter;
    private double ticksPerSecond = 20.0;
    private long firstMovementTick = -1L;
    private long lastMovementTick = -1L;
    private double movementTicks;

    public void sentKeepAlive(long id, long sendMillis) {
        pendingKeepAlives.put(id, sendMillis);
        keepAlivesSent++;
        this.keepAliveId = (int) id;
    }

    public long receivedKeepAlive(long id, long nowMillis) {
        keepAlivesReceived++;
        Long sent = pendingKeepAlives.remove(id);
        if (sent == null) {
            keepAlivesMismatched++;
            return -1L;
        }
        long roundTrip = nowMillis - sent;
        recordPing(roundTrip);
        return roundTrip;
    }

    public void recordPing(long roundTripMillis) {
        this.pingMillis = roundTripMillis;
        if (smoothedPingMillis < 0.0) {
            smoothedPingMillis = roundTripMillis;
        } else {
            smoothedPingMillis += (roundTripMillis - smoothedPingMillis) * 0.1;
        }
        if (minPingMillis < 0.0 || roundTripMillis < minPingMillis) {
            minPingMillis = roundTripMillis;
        }
        if (roundTripMillis > maxPingMillis) {
            maxPingMillis = roundTripMillis;
        }
    }

    public double ping() {
        return smoothedPingMillis < 0.0 ? 0.0 : smoothedPingMillis;
    }

    public double rawPing() {
        return pingMillis;
    }

    public double smoothedPing() {
        return smoothedPingMillis;
    }

    public double minPing() {
        return minPingMillis;
    }

    public double maxPing() {
        return maxPingMillis;
    }

    public int keepAliveId() {
        return keepAliveId;
    }

    public int keepAlivesSent() {
        return keepAlivesSent;
    }

    public int keepAlivesReceived() {
        return keepAlivesReceived;
    }

    public int keepAlivesMismatched() {
        return keepAlivesMismatched;
    }

    public long packetsReceived() {
        return packetsReceived;
    }

    public long packetsSent() {
        return packetsSent;
    }

    public int packetsPerSecond() {
        return packetsPerSecond;
    }

    public void packetReceived(long nanos) {
        packetsReceived++;
        if (firstPacketNanos == 0L) {
            firstPacketNanos = nanos;
        }
        lastPacketNanos = nanos;
    }

    public void packetSent() {
        packetsSent++;
    }

    public void movementReceived(long nanos, long tick) {
        lastMovementNanos = nanos;
        if (firstMovementTick < 0L) {
            firstMovementTick = tick;
        }
        lastMovementTick = tick;
    }

    public void countBadPacket(long nowMillis) {
        if (badPacketWindowStart == 0L) {
            badPacketWindowStart = nowMillis;
        }
        badPacketsThisSecond++;
    }

    public int badPacketsThisSecond(long nowMillis) {
        if (badPacketWindowStart != 0L && nowMillis - badPacketWindowStart >= 1000L) {
            badPacketWindowStart = nowMillis;
            badPacketsThisSecond = 0;
        }
        return badPacketsThisSecond;
    }

    public int invalidPackets() {
        return invalidPackets;
    }

    public void invalidPacket() {
        invalidPackets++;
    }

    public int cancelledPackets() {
        return cancelledPackets;
    }

    public void cancelledPacket() {
        cancelledPackets++;
    }

    public long lastPacketNanos() {
        return lastPacketNanos;
    }

    public long lastMovementNanos() {
        return lastMovementNanos;
    }

    public long firstPacketNanos() {
        return firstPacketNanos;
    }

    public long tickCounter() {
        return tickCounter;
    }

    public void tickCounter(long value) {
        this.tickCounter = value;
    }

    public double ticksPerSecond() {
        return ticksPerSecond;
    }

    public void ticksPerSecond(double value) {
        this.ticksPerSecond = value;
    }

    public long firstMovementTick() {
        return firstMovementTick;
    }

    public long lastMovementTick() {
        return lastMovementTick;
    }

    public double movementTicks() {
        return movementTicks;
    }

    public void tick(double serverTps, long nowMillis) {
        if (packetsReceivedWindowStart == 0L) {
            packetsReceivedWindowStart = nowMillis;
            packetsSentWindowStart = nowMillis;
        }
        if (nowMillis - packetsReceivedWindowStart >= 1000L) {
            packetsPerSecond = (int) Math.min(
                    Integer.MAX_VALUE, packetsReceived / Math.max(1.0, (nowMillis - packetsReceivedWindowStart) / 1000.0));
            packetsReceivedWindowStart = nowMillis;
            packetsSentWindowStart = nowMillis;
            packetsReceived = 0L;
            packetsSent = 0L;
        }
        this.ticksPerSecond = serverTps;
    }

    public void reset() {
        pingMillis = -1.0;
        smoothedPingMillis = -1.0;
        keepAlivesSent = 0;
        keepAlivesReceived = 0;
        keepAlivesMismatched = 0;
        invalidPackets = 0;
        cancelledPackets = 0;
        firstMovementTick = -1L;
        lastMovementTick = -1L;
        for (Iterator<Long> iterator = pendingKeepAlives.keySet().iterator(); iterator.hasNext(); ) {
            iterator.next();
            iterator.remove();
        }
    }
}
