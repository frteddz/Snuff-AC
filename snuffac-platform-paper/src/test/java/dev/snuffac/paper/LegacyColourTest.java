package dev.snuffac.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LegacyColourTest {

    @Test
    void convertsHexColours() {
        assertEquals("<color:#FF0000>", LegacyColour.toMiniMessage("&#FF0000"));
        assertEquals("<color:#66FF00>", LegacyColour.toMiniMessage("&#66FF00"));
    }

    @Test
    void convertsDecorations() {
        assertEquals("<bold>", LegacyColour.toMiniMessage("&l"));
        assertEquals("<italic>", LegacyColour.toMiniMessage("&o"));
        assertEquals("<underline>", LegacyColour.toMiniMessage("&n"));
        assertEquals("<strikethrough>", LegacyColour.toMiniMessage("&m"));
        assertEquals("<reset>", LegacyColour.toMiniMessage("&r"));
    }

    @Test
    void convertsTheExactPrefixTheUserSupplied() {
        String prefix = LegacyColour.PREFIX;
        String converted = LegacyColour.toMiniMessage(prefix);
        assertTrue(converted.startsWith("<color:#EE6832><bold><italic>["),
                "got: " + converted);
        assertTrue(converted.endsWith("<color:#FFFFFF><bold><italic>]"),
                "got: " + converted);
        assertFalse(converted.contains("&"), "no legacy ampersand code may survive");
        assertEquals(7, countOccurrences(converted, "<color:#"), "the prefix has seven gradient stops");
    }

    @Test
    void convertsNamedLegacyColours() {
        assertEquals("<color:#FF5555>", LegacyColour.toMiniMessage("&c"));
        assertEquals("<color:#55FF55>", LegacyColour.toMiniMessage("&a"));
        assertEquals("<color:#AAAAAA>", LegacyColour.toMiniMessage("&7"));
    }

    @Test
    void acceptsSectionSignAsWellAsAmpersand() {
        assertEquals("<color:#FF0000>", LegacyColour.toMiniMessage("§#FF0000"));
        assertEquals("<bold>", LegacyColour.toMiniMessage("§l"));
    }

    @Test
    void leavesUnknownCodesAsLiteralText() {
        assertEquals("&z", LegacyColour.toMiniMessage("&z"));
        assertEquals("&", LegacyColour.toMiniMessage("&"));
        assertEquals("plain", LegacyColour.toMiniMessage("plain"));
    }

    @Test
    void rejectsTruncatedHexWithoutThrowing() {
        assertEquals("&#FF00", LegacyColour.toMiniMessage("&#FF00"));
        assertEquals("&#ZZZZZZ", LegacyColour.toMiniMessage("&#ZZZZZZ"));
    }

    @Test
    void handlesNullAndEmpty() {
        assertEquals("", LegacyColour.toMiniMessage(null));
        assertEquals("", LegacyColour.toMiniMessage(""));
    }

    @Test
    void composesAdjacentColourAndDecorationTags() {
        String input = "&#FF0000&l&oText";
        assertEquals("<color:#FF0000><bold><italic>Text", LegacyColour.toMiniMessage(input));
    }

    @Test
    void keepsOrdinaryTextAndPunctuationIntact() {
        assertEquals("Snuff banned Steve for 1h 10s.",
                LegacyColour.toMiniMessage("Snuff banned Steve for 1h 10s."));
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
