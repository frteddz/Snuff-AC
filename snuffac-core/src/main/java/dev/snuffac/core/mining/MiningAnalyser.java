package dev.snuffac.core.mining;

import dev.snuffac.core.util.BlockPos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class MiningAnalyser {

    public static final int HISTORY_LIMIT = 64;

    private final Deque<MiningEvidence> history = new ArrayDeque<>();
    private final int oreTierThreshold;

    public MiningAnalyser(int oreTierThreshold) {
        this.oreTierThreshold = Math.max(0, oreTierThreshold);
    }

    public void record(MiningEvidence evidence) {
        history.addLast(evidence);
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    public List<MiningEvidence> history() {
        return new ArrayList<>(history);
    }

    public List<MiningEvidence> targetsOutsideSentChunks() {
        List<MiningEvidence> result = new ArrayList<>();
        for (MiningEvidence evidence : history) {
            if (!evidence.withinSentChunkRadius()) {
                result.add(evidence);
            }
        }
        return result;
    }

    public long valuableOutsideSentChunks() {
        long count = 0L;
        for (MiningEvidence evidence : history) {
            if (!evidence.withinSentChunkRadius() && evidence.oreTier() >= oreTierThreshold) {
                count++;
            }
        }
        return count;
    }

    public long outsideSentChunks() {
        return targetsOutsideSentChunks().size();
    }

    public double averageOreTier() {
        if (history.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (MiningEvidence evidence : history) {
            total += evidence.oreTier();
        }
        return total / history.size();
    }

    public double averageSecondsBetweenTargets() {
        if (history.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (MiningEvidence evidence : history) {
            total += evidence.secondsSincePreviousTarget();
        }
        return total / (history.size() - 1);
    }

    public void clear() {
        history.clear();
    }

    public int size() {
        return history.size();
    }
}
