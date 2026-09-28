package dev.snuffac.core.enforcement;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class EnforcementService {

    private final Map<UUID, AtomicLong> actionCounts = new ConcurrentHashMap<>();
    private final Map<EnforcementType, AtomicLong> typeCounts = new EnumMap<>(EnforcementType.class);
    private volatile Handler handler = request -> {
    };
    private volatile boolean preventionEnabled = true;
    private volatile double minConfidenceForPrevention = 0.55;

    public void handler(Handler handler) {
        this.handler = handler == null ? request -> {
        } : handler;
    }

    public void preventionEnabled(boolean value) {
        this.preventionEnabled = value;
    }

    public boolean preventionEnabled() {
        return preventionEnabled;
    }

    public void minConfidenceForPrevention(double value) {
        this.minConfidenceForPrevention = Math.max(0.0, value);
    }

    public double minConfidenceForPrevention() {
        return minConfidenceForPrevention;
    }

    public boolean apply(EnforcementRequest request) {
        if (!request.preventsAnything()) {
            handler.onEnforcement(request);
            count(request);
            return false;
        }
        if (!preventionEnabled) {
            count(request);
            return false;
        }
        if (request.confidence() < minConfidenceForPrevention) {
            count(request);
            return false;
        }
        handler.onEnforcement(request);
        count(request);
        return true;
    }

    private void count(EnforcementRequest request) {
        actionCounts.computeIfAbsent(request.playerId(), ignored -> new AtomicLong()).incrementAndGet();
        typeCounts.computeIfAbsent(request.type(), ignored -> new AtomicLong()).incrementAndGet();
    }

    public long actionsFor(UUID playerId) {
        AtomicLong counter = actionCounts.get(playerId);
        return counter == null ? 0L : counter.get();
    }

    public long countOf(EnforcementType type) {
        AtomicLong counter = typeCounts.get(type);
        return counter == null ? 0L : counter.get();
    }

    public void forget(UUID playerId) {
        actionCounts.remove(playerId);
    }

    public interface Handler {

        void onEnforcement(EnforcementRequest request);
    }
}
