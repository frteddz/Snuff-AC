package dev.snuffac.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snuffac.api.CheckCategory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvidenceKindTest {

    private static CheckConfig loaded(String key, Map<String, Object> values) {
        CheckConfig check = CheckConfig.defaults(CheckCategory.COMBAT);
        check.applyFrom(ConfigSource.ofMap(new LinkedHashMap<>(values)), "checks." + key);
        return check;
    }

    @Test
    void aCheckIsDerivedUnlessItSaysOtherwise() {
        assertFalse(CheckConfig.defaults(CheckCategory.COMBAT).structural());
        assertEquals(CheckConfig.EVIDENCE_DERIVED,
                CheckConfig.defaults(CheckCategory.COMBAT).evidenceKind());
    }

    @Test
    void structuralCanBeSetInConfig() {
        CheckConfig check = loaded("badpackets", Map.of("checks.badpackets.evidence", "STRUCTURAL"));
        assertTrue(check.structural());
    }

    @Test
    void evidenceIsCaseInsensitive() {
        CheckConfig check = loaded("badpackets", Map.of("checks.badpackets.evidence", "structural"));
        assertTrue(check.structural());
    }

    @Test
    void aDerivedCheckCannotSetbackOnItsFirstFlag() {
        CheckConfig check = loaded("packetspam", Map.of(
                "checks.packetspam.alert-threshold", 1.0,
                "checks.packetspam.setback-threshold", 1.0,
                "checks.packetspam.evidence", "DERIVED"));
        assertEquals(1.0, check.alertThreshold());
        assertEquals(1.0, check.setbackThreshold());
        assertEquals(2.0, check.effectiveSetbackThreshold(),
                "a derived check must be pushed one level past its alert threshold before it moves anyone");
    }

    @Test
    void aStructuralCheckCanActAtItsOwnThreshold() {
        CheckConfig check = loaded("badpackets", Map.of(
                "checks.badpackets.alert-threshold", 1.0,
                "checks.badpackets.setback-threshold", 1.0,
                "checks.badpackets.evidence", "STRUCTURAL"));
        assertEquals(1.0, check.effectiveSetbackThreshold());
    }

    @Test
    void aDerivedCheckNeverActsBelowItsAlertThresholdPlusOne() {
        for (int alert = 1; alert <= 5; alert++) {
            CheckConfig check = loaded("x", Map.of(
                    "checks.x.alert-threshold", (double) alert,
                    "checks.x.setback-threshold", 1.0,
                    "checks.x.evidence", "DERIVED"));
            assertEquals(alert + 1.0, check.effectiveSetbackThreshold(),
                    "alert " + alert);
        }
    }

    @Test
    void aDerivedCheckWithNoSetbackStaysDisabled() {
        CheckConfig check = loaded("x", Map.of(
                "checks.x.alert-threshold", 1.0,
                "checks.x.setback-threshold", 0.0,
                "checks.x.evidence", "DERIVED"));
        assertFalse(check.setbacksEnabled());
    }
}
