package dev.snuffac.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TuningProfileTest {

    private static SnuffConfig loaded(Map<String, Object> values) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.putAll(values);
        SnuffConfig config = new SnuffConfig();
        config.load(ConfigSource.ofMap(merged));
        return config;
    }

    @Test
    void strictIsTheDefaultProfile() {
        assertEquals("strict", new SnuffConfig().tuningProfile());
    }

    @Test
    void strictIsTheProfileShippedInConfig() {
        SnuffConfig config = new SnuffConfig();
        config.load(ConfigSource.ofMap(new LinkedHashMap<>()));
        assertEquals("strict", config.tuningProfile());
    }

    @Test
    void strictHalvesTheToleranceSoLessSlopIsAllowed() {
        SnuffConfig config = loaded(Map.of("tuning.profile", "strict"));
        assertEquals(0.5, config.profileToleranceScale());
    }

    @Test
    void balancedLeavesToleranceUnchanged() {
        SnuffConfig config = loaded(Map.of("tuning.profile", "balanced"));
        assertEquals(1.0, config.profileToleranceScale());
        assertEquals(1.0, config.profileReachScale());
    }

    @Test
    void lenientDoublesTheTolerance() {
        SnuffConfig config = loaded(Map.of("tuning.profile", "lenient"));
        assertEquals(2.0, config.profileToleranceScale());
    }

    @Test
    void strictKeepsARealReachMarginAboveVanilla() {
        double base = new SnuffConfig().reachMaximum();
        double allowed = loaded(Map.of("tuning.profile", "strict")).reachToleranceFor(0.0, false);
        assertTrue(allowed > 0.0, "strict must not remove the lag margin entirely");
        assertTrue(base + allowed > base, "a margin of " + allowed + " remains on " + base);
    }

    @Test
    void strictScalesReachAndMovementIndependently() {
        SnuffConfig strict = loaded(Map.of("tuning.profile", "strict"));
        assertTrue(strict.profileReachScale() > 0.0);
        assertTrue(strict.profileToleranceScale() > 0.0);
    }

    @Test
    void anUnknownProfileFallsBackToStrict() {
        SnuffConfig config = loaded(Map.of("tuning.profile", "banana"));
        assertEquals(0.5, config.profileToleranceScale());
    }

    @Test
    void theProfileNameIsCaseInsensitive() {
        SnuffConfig config = loaded(Map.of("tuning.profile", "LENIENT"));
        assertEquals(2.0, config.profileToleranceScale());
    }

    @Test
    void strictProducesSmallerToleranceThanLenientForTheSamePing() {
        double ping = 120.0;
        double tps = 20.0;
        double strict = loaded(Map.of("tuning.profile", "strict")).toleranceFor(ping, tps, 1.0);
        double lenient = loaded(Map.of("tuning.profile", "lenient")).toleranceFor(ping, tps, 1.0);
        assertTrue(strict < lenient, "strict " + strict + " must be below lenient " + lenient);
    }

    @Test
    void strictProducesSmallerReachToleranceThanBalanced() {
        double ping = 80.0;
        double strict = loaded(Map.of("tuning.profile", "strict")).reachToleranceFor(ping, false);
        double balanced = loaded(Map.of("tuning.profile", "balanced")).reachToleranceFor(ping, false);
        assertTrue(strict < balanced, "strict " + strict + " must be below balanced " + balanced);
    }

    @Test
    void reachToleranceStaysPositiveUnderEveryProfile() {
        for (String profile : new String[] {"strict", "balanced", "lenient"}) {
            double value = loaded(Map.of("tuning.profile", profile)).reachToleranceFor(0.0, false);
            assertTrue(value > 0.0, profile + " reach tolerance must stay positive, got " + value);
        }
    }

    @Test
    void visualDefaultsAreSafeAndEnabled() {
        SnuffConfig config = new SnuffConfig();
        assertTrue(config.visualEntityHiding());
        assertTrue(config.visualSoundFuzzing());
        assertTrue(config.visualRevealRadius() > 0.0);
        assertTrue(config.visualRevealPadding() >= config.visualRevealRadius());
        assertTrue(config.visualSoundJitter() > 0.0);
        assertTrue(config.visualIntervalTicks() >= 1);
    }

    @Test
    void visualValuesLoadFromConfig() {
        SnuffConfig config = loaded(Map.of(
                "visual.entity-hiding", false,
                "visual.sound-fuzzing", false,
                "visual.reveal-radius", 8.0,
                "visual.reveal-padding", 12.0,
                "visual.sound-jitter", 0.5,
                "visual.interval-ticks", 10));
        assertFalse(config.visualEntityHiding());
        assertFalse(config.visualSoundFuzzing());
        assertEquals(8.0, config.visualRevealRadius());
        assertEquals(12.0, config.visualRevealPadding());
        assertEquals(0.5, config.visualSoundJitter());
        assertEquals(10, config.visualIntervalTicks());
    }

    @Test
    void aZeroVisualIntervalIsClampedToOneTick() {
        assertEquals(1, loaded(Map.of("visual.interval-ticks", 0)).visualIntervalTicks());
    }

    @Test
    void defaultCheckThresholdsAreStrictEnoughToActuallyReport() {
        CheckConfig check = CheckConfig.defaults(dev.snuffac.api.CheckCategory.COMBAT);
        assertEquals(1.0, check.effectiveBufferThreshold());
        assertEquals(1.0, check.alertThreshold());
        assertEquals(1.0, check.setbackThreshold());
        assertTrue(check.setbacksEnabled(), "a default check must be able to prevent");
    }
}
