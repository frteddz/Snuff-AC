package dev.snuffac.core.platform;

public interface SnuffMessenger {

    void sendMessage(Object player, String message);

    void broadcast(String message, String permission);

    void sendConsole(String message);
}
