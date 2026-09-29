package dev.snuffac.core.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlertPrefixTest {

    private static final String GRADIENT =
            "&#EE6832&l&o[&#F18154&l&oS&#F49A76&l&oN&#F7B499&l&oU&#F9CDBB&l&oF"
            + "&#FCE6DD&l&oF&#FFFFFF&l&o] ";

    @Test
    void thePrefixIsPrependedToTheRenderedAlert() {
        String chat = AlertFormatter.withPrefix("frteddz failed Reach", GRADIENT);
        assertTrue(chat.startsWith(GRADIENT), "got: " + chat);
        assertTrue(chat.endsWith("frteddz failed Reach"));
    }

    @Test
    void anEmptyPrefixLeavesTheLineAlone() {
        assertEquals("plain", AlertFormatter.withPrefix("plain", ""));
        assertEquals("plain", AlertFormatter.withPrefix("plain", null));
    }

    @Test
    void theConsoleLineIsPlainTextWithNoMarkup() {
        String chat = AlertFormatter.withPrefix("frteddz failed Reach", GRADIENT);
        String console = AlertFormatter.stripMarkup(chat);
        assertFalse(console.contains("<"), "console must not receive MiniMessage tags: " + console);
        assertFalse(console.contains("&"), "console must not receive legacy codes: " + console);
        assertFalse(console.contains("§"), "console must not receive section signs: " + console);
        assertTrue(console.contains("frteddz failed Reach"));
    }

    @Test
    void theConsoleLineStaysReadable() {
        String chat = AlertFormatter.withPrefix("frteddz failed Reach | VL: 1.0", GRADIENT);
        String console = AlertFormatter.stripMarkup(chat);
        assertTrue(console.contains("[SNUFF]"), "the bracket word survives, got: " + console);
        assertTrue(console.trim().endsWith("frteddz failed Reach | VL: 1.0"), "got: " + console);
        assertEquals("[SNUFF] frteddz failed Reach | VL: 1.0", console.trim());
    }

    @Test
    void aTruncatedHexDoesNotCorruptTheConsoleLine() {
        String chat = AlertFormatter.withPrefix("detail &#FF00 broken", GRADIENT);
        String console = AlertFormatter.stripMarkup(chat);
        assertFalse(console.contains("#FF00"), "a partial code must not linger: " + console);
    }

    @Test
    void strippingANullLineIsSafe() {
        assertEquals("", AlertFormatter.stripMarkup(null));
        assertEquals("", AlertFormatter.stripMarkup(""));
    }
}
