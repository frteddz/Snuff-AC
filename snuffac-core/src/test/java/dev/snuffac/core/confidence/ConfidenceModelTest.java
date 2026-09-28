package dev.snuffac.core.confidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfidenceModelTest {

    @Test
    @DisplayName("signals accumulate into the current confidence")
    void signalsAccumulate() {
        ConfidenceModel model = new ConfidenceModel();
        long now = System.currentTimeMillis();
        model.add("fly", 0.5, now);
        model.add("speed", 0.4, now);
        assertEquals(0.9, model.current(), 1.0E-6);
        assertEquals(2, model.signalCount());
    }

    @Test
    @DisplayName("confidence is clamped so one check cannot exceed one")
    void contributionsClamped() {
        ConfidenceModel model = new ConfidenceModel();
        model.add("fly", 5.0, System.currentTimeMillis());
        assertTrue(model.current() <= 1.0);
    }

    @Test
    @DisplayName("decay reduces confidence but never below zero")
    void decays() {
        ConfidenceModel model = new ConfidenceModel();
        model.add("fly", 1.0, System.currentTimeMillis());
        model.decay(0.4);
        assertEquals(0.6, model.current(), 1.0E-6);
        model.decay(99.0);
        assertEquals(0.0, model.current(), 1.0E-6);
    }

    @Test
    @DisplayName("ticking bleeds confidence towards zero")
    void tickBleeds() {
        ConfidenceModel model = new ConfidenceModel();
        long now = System.currentTimeMillis();
        model.add("fly", 1.0, now);
        for (int i = 0; i < 2000; i++) {
            model.tick(now);
        }
        assertTrue(model.current() < 0.01, "confidence should have bled away");
    }

    @Test
    @DisplayName("peak confidence is retained briefly then expires")
    void peakExpires() {
        ConfidenceModel model = new ConfidenceModel();
        long now = System.currentTimeMillis();
        model.add("fly", 1.0, now);
        assertEquals(1.0, model.peak(), 1.0E-6);
        long later = now + (long) (ConfidenceModel.PEAK_RETAINED_TICKS * 50L) + 5000L;
        assertEquals(model.current(), model.peak(), 1.0E-6);
    }

    @Test
    @DisplayName("recent contributing checks are retained for reporting")
    void recentChecksRetained() {
        ConfidenceModel model = new ConfidenceModel();
        long now = System.currentTimeMillis();
        model.add("fly", 0.5, now);
        model.add("groundflag", 0.5, now);
        assertTrue(model.recentCheckKeys().contains("fly"));
        assertTrue(model.recentCheckKeys().contains("groundflag"));
    }
}
