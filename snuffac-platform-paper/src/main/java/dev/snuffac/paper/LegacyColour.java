package dev.snuffac.paper;

import java.util.ArrayList;
import java.util.List;

public final class LegacyColour {

    private LegacyColour() {
    }

    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String text = input;
        StringBuilder out = new StringBuilder(text.length() + 16);
        int index = 0;
        int length = text.length();
        while (index < length) {
            char c = text.charAt(index);
            if (c != '&' && c != '§') {
                out.append(c);
                index++;
                continue;
            }
            if (index + 1 >= length) {
                out.append(c);
                index++;
                continue;
            }
            char code = Character.toLowerCase(text.charAt(index + 1));
            if (code == '#' && index + 8 <= length && isHex(text, index + 2)) {
                out.append("<color:#").append(text, index + 2, index + 8).append('>');
                index += 8;
                continue;
            }
            String tag = decoration(code);
            if (tag != null) {
                out.append(tag);
                index += 2;
                continue;
            }
            String named = namedColour(code);
            if (named != null) {
                out.append(named);
                index += 2;
                continue;
            }
            out.append(c);
            index++;
        }
        return out.toString();
    }

    public static boolean isHex(String text, int from) {
        for (int i = from; i < from + 6; i++) {
            char c = Character.toLowerCase(text.charAt(i));
            boolean digit = c >= '0' && c <= '9';
            boolean letter = c >= 'a' && c <= 'f';
            if (!digit && !letter) {
                return false;
            }
        }
        return true;
    }

    private static String decoration(char code) {
        return switch (code) {
            case 'l' -> "<bold>";
            case 'o' -> "<italic>";
            case 'n' -> "<underline>";
            case 'm' -> "<strikethrough>";
            case 'k' -> "<obfuscated>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    private static String namedColour(char code) {
        return switch (code) {
            case '0' -> "<color:#000000>";
            case '1' -> "<color:#0000AA>";
            case '2' -> "<color:#00AA00>";
            case '3' -> "<color:#00AAAA>";
            case '4' -> "<color:#AA0000>";
            case '5' -> "<color:#AA00AA>";
            case '6' -> "<color:#FFAA00>";
            case '7' -> "<color:#AAAAAA>";
            case '8' -> "<color:#555555>";
            case '9' -> "<color:#5555FF>";
            case 'a' -> "<color:#55FF55>";
            case 'b' -> "<color:#55FFFF>";
            case 'c' -> "<color:#FF5555>";
            case 'd' -> "<color:#FF55FF>";
            case 'e' -> "<color:#FFFF55>";
            case 'f' -> "<color:#FFFFFF>";
            default -> null;
        };
    }

    public static List<String> splitLines(String input) {
        List<String> lines = new ArrayList<>();
        if (input == null) {
            return lines;
        }
        for (String line : input.split("\n")) {
            lines.add(line);
        }
        return lines;
    }
}
