package dev.snuffac.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LegacyColourJoinTest {

    private static String joined(String body) {
        return LegacyColour.joined(body);
    }

    @Test
    void noLegacyAmpersandCodeSurvivesTheJoin() {
        String rendered = joined("hello");
        assertFalse(rendered.indexOf('&') >= 0,
                "a legacy ampersand survived the join: " + rendered);
    }

    @Test
    void thePrefixIsRenderedAsColourTags() {
        String rendered = joined("hello");
        assertTrue(rendered.startsWith("<color:#EE6832>"),
                "prefix must open with its first colour, got: " + rendered);
        assertTrue(rendered.contains("S") && rendered.contains("N") && rendered.contains("U")
                && rendered.contains("F"), "prefix letters were dropped: " + rendered);
        assertTrue(rendered.contains("<color:#FFFFFF>"), "prefix must close in white: " + rendered);
    }

    @Test
    void thePrefixCarriesExactlySevenGradientStops() {
        assertEquals(7, countOccurrences(LegacyColour.PREFIX, "&#"),
                "the supplied prefix has seven stops");
        assertEquals(7, countOccurrences(joined("x"), "<color:#"));
    }

    @Test
    void thePrefixDoesNotBleedColourIntoTheBody() {
        String rendered = joined("Muted Steve for 1h 10s.");
        int reset = rendered.indexOf("<reset>");
        assertTrue(reset >= 0, "the prefix must be closed with a reset: " + rendered);
        String tail = rendered.substring(reset);
        assertTrue(tail.contains("Muted Steve for 1h 10s."), "body was lost: " + rendered);
        assertFalse(tail.substring(0, tail.indexOf("Muted")).contains("<bold>"),
                "bold must not leak past the prefix: " + rendered);
        assertFalse(tail.substring(0, tail.indexOf("Muted")).contains("<italic>"),
                "italic must not leak past the prefix: " + rendered);
        assertFalse(tail.substring(0, tail.indexOf("Muted")).contains("<color:"),
                "a colour tag must not leak past the prefix: " + rendered);
    }

    @Test
    void theBodyIsConvertedToo() {
        String rendered = joined("&#FF0000error&#66FF00okay");
        assertTrue(rendered.contains("<color:#FF0000>"), "red stop missing: " + rendered);
        assertTrue(rendered.contains("<color:#66FF00>"), "green stop missing: " + rendered);
        assertFalse(rendered.indexOf('&') >= 0, "a legacy ampersand survived in the body: " + rendered);
    }

    @Test
    void theBodyTextIsPreserved() {
        assertTrue(joined("Muted Steve for 1h 10s.").contains("Muted Steve for 1h 10s."));
    }

    @Test
    void aTruncatedHexDoesNotBecomeABrokenTag() {
        String rendered = joined("&#FF00 broken");
        assertFalse(rendered.contains("<color:#FF00>"), "broken tag produced: " + rendered);
    }

    @Test
    void anEmptyBodyStillCarriesThePrefix() {
        assertTrue(joined("").contains("<color:#EE6832>"));
    }

    @Test
    void aNullBodyIsSafe() {
        assertTrue(joined(null).contains("<color:#EE6832>"));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
