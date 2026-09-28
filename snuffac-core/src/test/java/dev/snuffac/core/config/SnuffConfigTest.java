package dev.snuffac.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnuffConfigTest {

    private static ConfigSource source(Object... pathValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pathValuePairs.length; i += 2) {
            map.put((String) pathValuePairs[i], pathValuePairs[i + 1]);
        }
        return ConfigSource.ofMap(map);
    }

    @Test
    @DisplayName("defaults are applied when the config is empty")
    void defaultsApplied() {
        SnuffConfig config = new SnuffConfig();
        config.load(source());
        assertTrue(config.enabled());
        assertFalse(config.debug());
        assertEquals("Snuff", config.alertPrefix());
        assertEquals(3.35, config.reachMaximum(), 1.0E-9);
    }

    @Test
    @DisplayName("values from the config override the defaults")
    void overridesDefaults() {
        SnuffConfig config = new SnuffConfig();
        config.load(source(
                "general.enabled", false,
                "general.debug", true,
                "general.alert-prefix", "Custom",
                "general.alert-cooldown-ms", 2500));
        assertFalse(config.enabled());
        assertTrue(config.debug());
        assertEquals("Custom", config.alertPrefix());
        assertEquals(2500, config.alertCooldownMillis());
    }

    @Test
    @DisplayName("string values are coerced from numbers when needed")
    void coercesTypes() {
        SnuffConfig config = new SnuffConfig();
        config.load(source(
                "general.alert-prefix", 42,
                "general.debug", "true",
                "combat.reach.maximum", "4.2"));
        assertEquals("42", config.alertPrefix());
        assertTrue(config.debug());
        assertEquals(4.2, config.reachMaximum(), 1.0E-9);
    }

    @Test
    @DisplayName("ping tolerance grows with latency but is capped")
    void pingToleranceGrowsAndCaps() {
        SnuffConfig config = new SnuffConfig();
        config.load(source(
                "tolerance.base", 0.001,
                "tolerance.ping-floor", 0.001,
                "tolerance.ping-per-ms", 0.00002,
                "tolerance.ping-maximum", 0.06,
                "tolerance.tps-floor", 0.0,
                "tolerance.tps-maximum", 0.0));
        double low = config.toleranceFor(0.0, 20.0, 1.0);
        double mid = config.toleranceFor(200.0, 20.0, 1.0);
        double extreme = config.toleranceFor(5000.0, 20.0, 1.0);
        assertTrue(mid > low, "tolerance should grow with ping");
        assertTrue(extreme <= 0.0611, "tolerance must be capped even at extreme ping");
    }

    @Test
    @DisplayName("low tps widens tolerance to avoid punishing laggy servers")
    void tpsToleranceGrowsWhenLagging() {
        SnuffConfig config = new SnuffConfig();
        config.load(source(
                "tolerance.base", 0.0,
                "tolerance.ping-floor", 0.0,
                "tolerance.ping-per-ms", 0.0,
                "tolerance.ping-maximum", 0.0,
                "tolerance.tps-floor", 0.0,
                "tolerance.tps-per-miss", 0.004,
                "tolerance.tps-maximum", 0.05,
                "tolerance.safe-tps", 19.0));
        double healthy = config.toleranceFor(0.0, 20.0, 1.0);
        double lagging = config.toleranceFor(0.0, 15.0, 1.0);
        assertTrue(lagging > healthy, "tolerance should widen when the server is below safe tps");
    }

    @Test
    @DisplayName("reach tolerance accounts for latency and vehicles")
    void reachToleranceScales() {
        SnuffConfig config = new SnuffConfig();
        config.load(source(
                "combat.reach.maximum", 3.35,
                "combat.reach.tolerance", 0.10,
                "combat.reach.vehicle-tolerance", 0.50,
                "combat.reach.latency-multiplier", 0.002,
                "combat.reach.max-latency-bonus", 0.35));
        double lowPing = config.reachToleranceFor(0.0, false);
        double highPing = config.reachToleranceFor(300.0, false);
        double vehicle = config.reachToleranceFor(0.0, true);
        assertTrue(highPing > lowPing, "latency should widen the reach allowance");
        assertTrue(vehicle > lowPing, "being in a vehicle should widen the reach allowance");
        assertTrue(config.reachToleranceFor(10_000.0, false) <= 0.4501,
                "the latency bonus must be capped");
    }

    @Test
    @DisplayName("check configuration reads its own section from the source")
    void checkConfigLoadsSection() {
        CheckConfig check = CheckConfig.defaults(dev.snuffac.api.CheckCategory.MOVEMENT);
        check.applyFrom(source(
                "enabled", false,
                "buffer-threshold", 42.5,
                "buffer-decay", 1.5,
                "violation-increment", 2.0,
                "setback-threshold", 9.0,
                "action", "command",
                "command", "say %player% flagged",
                "command-threshold", 5.0,
                "command-cooldown-ms", 3000), "");
        assertFalse(check.enabled());
        assertEquals(42.5, check.bufferThreshold(), 1.0E-9);
        assertEquals(1.5, check.bufferDecay(), 1.0E-9);
        assertEquals(2.0, check.violationIncrement(), 1.0E-9);
        assertEquals(9.0, check.setbackThreshold(), 1.0E-9);
        assertEquals(CheckConfig.ACTION_COMMAND, check.action());
        assertTrue(check.commandsEnabled());
        assertEquals(3000, check.commandCooldownMillis());
    }

    @Test
    @DisplayName("an unset setback threshold of zero disables setbacks")
    void zeroSetbackDisables() {
        CheckConfig check = CheckConfig.defaults(dev.snuffac.api.CheckCategory.COMBAT);
        check.applyFrom(source("setback-threshold", 0.0), "");
        assertFalse(check.setbacksEnabled());
    }

    @Test
    @DisplayName("nested map paths resolve through the source")
    void nestedPathsResolve() {
        ConfigSource nested = source();
        nested.set("checks.movement.fly.enabled", false);
        assertEquals(Boolean.FALSE, nested.raw("checks.movement.fly.enabled"));
        assertEquals(List.of("checks"), List.copyOf(new java.util.ArrayList<>(List.of("checks"))));
        assertTrue(nested.contains("checks.movement.fly.enabled"));
    }
}
