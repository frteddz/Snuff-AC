package dev.snuffac.paper;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public final class StaffSounds {

    public static final String MENU_OPEN = "menu_open";
    public static final String SUCCESS = "success";
    public static final String REJECT = "reject";
    public static final String PUNISH = "punish";

    private StaffSounds() {
    }

    public static void play(Player player, String cue) {
        try {
            if (!enabledFor(player)) {
                return;
            }
            Sound sound = resolve(cue);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, 0.6f, 1.0f);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean enabledFor(Player player) {
        try {
            return player.hasPermission("snuffac.sounds")
                    && SnuffSounds.enabled();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Sound resolve(String cue) {
        if (cue == null) {
            return null;
        }
        return switch (cue) {
            case MENU_OPEN -> Sound.UI_BUTTON_CLICK;
            case SUCCESS -> Sound.ENTITY_PLAYER_LEVELUP;
            case REJECT -> Sound.BLOCK_NOTE_BLOCK_BASS;
            case PUNISH -> Sound.BLOCK_ANVIL_LAND;
            default -> null;
        };
    }
}
