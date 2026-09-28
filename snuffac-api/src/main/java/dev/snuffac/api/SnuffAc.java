package dev.snuffac.api;

import dev.snuffac.api.violation.CheckInfo;
import dev.snuffac.api.violation.ViolationInfo;
import dev.snuffac.api.violation.ViolationListener;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SnuffAc {

    static Optional<SnuffAc> get() {
        return Optional.ofNullable(Holder.instance);
    }

    String version();

    SnuffPlatform platform();

    boolean isEnabled();

    List<CheckInfo> checks();

    Optional<CheckInfo> check(String key);

    void setCheckEnabled(String key, boolean enabled) throws CheckNotFoundException;

    void addListener(ViolationListener listener);

    void removeListener(ViolationListener listener);

    void reload();

    List<ViolationInfo> recentViolations(UUID playerId, int limit);

    final class Holder {

        private static volatile SnuffAc instance;

        private Holder() {
        }

        static SnuffAc getInstance() {
            return instance;
        }

        public static void set(SnuffAc ac) {
            if (instance != null) {
                throw new IllegalStateException("Snuff AC is already installed");
            }
            instance = ac;
        }

        public static void clear() {
            instance = null;
        }
    }

    final class CheckNotFoundException extends Exception {

        private static final long serialVersionUID = 1L;

        public CheckNotFoundException(String key) {
            super("Unknown check: " + key);
        }
    }
}
