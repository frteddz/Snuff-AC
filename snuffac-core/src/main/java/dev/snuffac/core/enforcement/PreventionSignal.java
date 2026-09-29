package dev.snuffac.core.enforcement;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class PreventionSignal {

    private final AtomicReference<Verdict> pending = new AtomicReference<>();
    private final AtomicLong attackBlocks = new AtomicLong();
    private final AtomicLong placeBlocks = new AtomicLong();
    private final AtomicLong interactionBlocks = new AtomicLong();
    private final AtomicLong setbacks = new AtomicLong();

    public record Verdict(
            boolean cancelAttack,
            boolean cancelPlacement,
            boolean cancelInteraction,
            boolean requestSetback,
            String checkKey,
            String reason) {

        public static Verdict none() {
            return new Verdict(false, false, false, false, "", "");
        }
    }

    public void clear() {
        pending.set(null);
    }

    public void cancelAttack(String checkKey, String reason) {
        offer(new Verdict(true, false, false, false, checkKey, reason));
    }

    public void cancelPlacement(String checkKey, String reason) {
        offer(new Verdict(false, true, false, false, checkKey, reason));
    }

    public void cancelInteraction(String checkKey, String reason) {
        offer(new Verdict(false, false, true, false, checkKey, reason));
    }

    public void requestSetback(String checkKey, String reason) {
        offer(new Verdict(false, false, false, true, checkKey, reason));
    }

    public void offer(Verdict verdict) {
        Verdict current = pending.get();
        if (current == null) {
            pending.compareAndSet(null, verdict);
            return;
        }
        pending.set(merge(current, verdict));
    }

    private static Verdict merge(Verdict current, Verdict next) {
        return new Verdict(
                current.cancelAttack() || next.cancelAttack(),
                current.cancelPlacement() || next.cancelPlacement(),
                current.cancelInteraction() || next.cancelInteraction(),
                current.requestSetback() || next.requestSetback(),
                current.checkKey().isEmpty() ? next.checkKey() : current.checkKey(),
                current.reason().isEmpty() ? next.reason() : current.reason());
    }

    public Verdict take() {
        Verdict verdict = pending.getAndSet(null);
        return verdict == null ? Verdict.none() : verdict;
    }

    public Verdict peek() {
        Verdict verdict = pending.get();
        return verdict == null ? Verdict.none() : verdict;
    }

    public void recordAttackBlock() {
        attackBlocks.incrementAndGet();
    }

    public void recordPlacementBlock() {
        placeBlocks.incrementAndGet();
    }

    public void recordInteractionBlock() {
        interactionBlocks.incrementAndGet();
    }

    public void recordSetback() {
        setbacks.incrementAndGet();
    }

    public long attackBlocks() {
        return attackBlocks.get();
    }

    public long placementBlocks() {
        return placeBlocks.get();
    }

    public long interactionBlocks() {
        return interactionBlocks.get();
    }

    public long setbacks() {
        return setbacks.get();
    }
}
