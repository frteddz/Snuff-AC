package dev.snuffac.api.violation;

@FunctionalInterface
public interface ViolationListener {

    void onViolation(ViolationInfo info);
}
