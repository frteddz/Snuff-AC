package dev.snuffac.core.violation;

import java.util.UUID;

public final class HistoryClearRequest {

    private final UUID playerId;
    private final String playerName;
    private final String requestedBy;
    private final long requestedMillis;
    private final boolean confirmed;

    public HistoryClearRequest(UUID playerId, String playerName, String requestedBy,
            long requestedMillis, boolean confirmed) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.requestedBy = requestedBy;
        this.requestedMillis = requestedMillis;
        this.confirmed = confirmed;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public String requestedBy() {
        return requestedBy;
    }

    public long requestedMillis() {
        return requestedMillis;
    }

    public boolean confirmed() {
        return confirmed;
    }

    public String describe() {
        return "clear flags for " + playerName + " by " + requestedBy;
    }
}
