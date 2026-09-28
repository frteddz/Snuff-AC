package dev.snuffac.core.platform;

public interface SnuffScheduler {

    void runOnMainThread(Runnable task);

    void runAsync(Runnable task);

    void runTimerOnMainThread(Runnable task, long delayMillis, long periodMillis);

    void runTimerAsync(Runnable task, long delayMillis, long periodMillis);

    void cancelAll();
}
