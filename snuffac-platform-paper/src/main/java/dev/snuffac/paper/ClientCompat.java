package dev.snuffac.paper;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class ClientCompat {

    public static final int LEGACY_PROTOCOL_BOUNDARY = 47;
    public static final int SUPPORTED_PROTOCOL_FLOOR = 47;

    private static Method floodgateCheck;
    private static Object floodgateApi;
    private static boolean floodgateProbed;

    private ClientCompat() {
    }

    public static boolean isBedrock(Player player) {
        if (!probeFloodgate()) {
            return false;
        }
        try {
            Object result = floodgateCheck.invoke(floodgateApi, player.getUniqueId());
            return result instanceof Boolean booleanResult && booleanResult;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    public static boolean isFloodgateInstalled() {
        return probeFloodgate();
    }

    private static synchronized boolean probeFloodgate() {
        if (floodgateProbed) {
            return floodgateCheck != null;
        }
        floodgateProbed = true;
        try {
            ClassLoader loader = ClientCompat.class.getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi", false, loader);
            floodgateApi = apiClass.getMethod("getInstance").invoke(null);
            floodgateCheck = apiClass.getMethod("isFloodgatePlayer", UUID.class);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            floodgateCheck = null;
        }
        return floodgateCheck != null;
    }

    public static boolean isLegacyProtocol(int protocolVersion) {
        return protocolVersion > 0 && protocolVersion < LEGACY_PROTOCOL_BOUNDARY;
    }

    public static boolean isSupportedProtocol(int protocolVersion) {
        return protocolVersion <= 0 || protocolVersion >= LEGACY_PROTOCOL_BOUNDARY;
    }
}
