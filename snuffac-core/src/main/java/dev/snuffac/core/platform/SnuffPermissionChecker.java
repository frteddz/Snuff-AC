package dev.snuffac.core.platform;

import java.util.List;

public interface SnuffPermissionChecker {

    boolean hasPermission(Object player, String permission);

    List<String> playersWithPermission(String permission);
}
