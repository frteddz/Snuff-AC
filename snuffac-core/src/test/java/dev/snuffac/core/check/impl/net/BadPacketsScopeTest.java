package dev.snuffac.core.check.impl.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class BadPacketsScopeTest {

    @Test
    void theDigMismatchRuleIsGone() throws Exception {
        Method validate = Class.forName("dev.snuffac.core.check.impl.net.BadPacketsCheck")
                .getDeclaredMethod("validateBlockPosition",
                        dev.snuffac.core.check.CheckContext.class, long.class, String.class);
        String body = readBody(validate);
        assertFalse(body.contains("digActive"),
                "badpackets must not look at dig state, mining behaviour belongs to FastBreak and Nuker");
        assertFalse(body.contains("sameAsActiveDig"),
                "the dig position comparison must not exist at all");
    }

    @Test
    void theDigComparisonHelperNoLongerExists() throws Exception {
        for (Method method : Class.forName("dev.snuffac.core.check.impl.net.BadPacketsCheck")
                .getDeclaredMethods()) {
            assertFalse(method.getName().equals("sameAsActiveDig"),
                    "sameAsActiveDig was the source of the bridging false positive");
        }
    }

    @Test
    void onlyNonFiniteCoordinatesAreStillImmediate() throws Exception {
        Method classify = Class.forName("dev.snuffac.core.check.impl.net.BadPacketsCheck")
                .getDeclaredMethod("structurallyImpossible", String.class);
        classify.setAccessible(true);
        assertTrue((boolean) classify.invoke(null, "non finite movement coordinates"));
        assertTrue((boolean) classify.invoke(null, "non finite attack cursor"));
        assertTrue((boolean) classify.invoke(null, "non finite rotation"));
        assertFalse((boolean) classify.invoke(null, "movement outside world border"));
        assertFalse((boolean) classify.invoke(null, "digging position outside build limits"),
                "an out of bounds position is buffered, not immediate");
    }

    @Test
    void theWorldBorderMatchesTheVanillaMaximum() {
        assertTrue(BadPacketsCheck.WORLD_BORDER <= 29_999_984.0,
                "the border must not exceed the vanilla maximum of 29999984");
    }

    private static String readBody(Method method) throws Exception {
        String file = "src/main/java/" + method.getDeclaringClass().getName()
                .replace('.', '/') + ".java";
        java.nio.file.Path path = java.nio.file.Path.of(file);
        if (!java.nio.file.Files.isRegularFile(path)) {
            return "";
        }
        String source = java.nio.file.Files.readString(path);
        int start = source.indexOf("private static void validateBlockPosition");
        if (start < 0) {
            start = source.indexOf("validateBlockPosition");
        }
        if (start < 0) {
            return "";
        }
        int end = source.indexOf("\n    }", start);
        return end < 0 ? source.substring(start) : source.substring(start, end);
    }
}
