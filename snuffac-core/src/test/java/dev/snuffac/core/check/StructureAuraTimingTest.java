package dev.snuffac.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.core.check.impl.combat.AutoCrystalCheck;
import dev.snuffac.core.check.impl.combat.BedAuraCheck;
import dev.snuffac.core.check.impl.combat.ReactionTiming;
import java.util.ArrayDeque;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StructureAuraTimingTest {

    @Test
    @DisplayName("identical reaction times are mechanical")
    void identicalGapsAreFlagged() {
        ArrayDeque<Long> gaps = new ArrayDeque<>();
        for (int i = 0; i < 8; i++) {
            gaps.add(40L);
        }
        var verdict = ReactionTiming.judge(gaps, 5, 220L);
        assertTrue(verdict.flagged(), "eight identical 40ms gaps are a machine");
    }

    @Test
    @DisplayName("a human spread is not mechanical")
    void variedGapsAreClean() {
        Random random = new Random(20261007L);
        ArrayDeque<Long> gaps = new ArrayDeque<>();
        for (int i = 0; i < 8; i++) {
            gaps.add(40L + Math.round(random.nextGaussian() * 18.0));
        }
        var verdict = ReactionTiming.judge(gaps, 5, 220L);
        assertFalse(verdict.flagged(), "a hand does not hit the same delay every time");
    }

    @Test
    @DisplayName("too few samples never flag")
    void shortWindowIsClean() {
        ArrayDeque<Long> gaps = new ArrayDeque<>();
        gaps.add(40L);
        gaps.add(40L);
        assertFalse(ReactionTiming.judge(gaps, 5, 220L).flagged(),
                "two swings prove nothing");
    }

    @Test
    @DisplayName("a slow reaction is not a cheat")
    void slowReactionIsClean() {
        ArrayDeque<Long> gaps = new ArrayDeque<>();
        for (int i = 0; i < 8; i++) {
            gaps.add(400L);
        }
        assertFalse(ReactionTiming.judge(gaps, 5, 220L).flagged(),
                "quarter of a second per swing is a person");
    }

    @Test
    @DisplayName("the verdict reports the mean and the spread")
    void verdictReportsNumbers() {
        ArrayDeque<Long> gaps = new ArrayDeque<>();
        for (int i = 0; i < 6; i++) {
            gaps.add(50L);
        }
        var verdict = ReactionTiming.judge(gaps, 5, 220L);
        assertTrue(verdict.flagged());
        assertTrue(verdict.mean() == 50L, "mean should be 50, got " + verdict.mean());
        assertTrue(verdict.jitter() == 0L, "identical gaps have no spread, got " + verdict.jitter());
        assertTrue(verdict.samples() == 6, "six samples were judged");
    }

    @Test
    @DisplayName("the crystal check only reads end crystals")
    void crystalTargetsOnly() {
        assertTrue(AutoCrystalCheck.isTarget("END_CRYSTAL"));
        assertFalse(AutoCrystalCheck.isTarget("ZOMBIE"));
        assertFalse(AutoCrystalCheck.isTarget(null));
    }

    @Test
    @DisplayName("the bed check reads beds and anchors and nothing else")
    void bedTargetsOnly() {
        assertTrue(BedAuraCheck.isTarget("BED"));
        assertTrue(BedAuraCheck.isTarget("ANCHOR"));
        assertFalse(BedAuraCheck.isTarget("PLAYER"));
        assertFalse(BedAuraCheck.isTarget("ITEM_FRAME"));
    }

    @Test
    @DisplayName("the reaction ceiling is a quarter of a second")
    void ceilingIsSane() {
        assertTrue(AutoCrystalCheck.MAX_REACTION_MILLIS <= 250L,
                "anything slower is a person reacting");
        assertTrue(BedAuraCheck.REQUIRED >= 4,
                "a few fast swings are just fast clicking");
    }
}
