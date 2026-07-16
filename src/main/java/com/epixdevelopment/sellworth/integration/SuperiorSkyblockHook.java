package com.epixdevelopment.sellworth.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

public class SuperiorSkyblockHook {
    private static boolean enabled = false;

    public static void init() {
        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            enabled = true;
            Bukkit.getLogger().info("[SellWorth] Successfully hooked into SuperiorSkyblock2 for island sell multipliers.");
        }
    }

    public static double getSellMultiplier(UUID uuid) {
        if (!enabled || uuid == null) {
            return 1.0;
        }
        try {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return 1.0;
            }
            Class<?> apiClass = Class.forName("com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI");
            Object superiorPlayer = apiClass.getMethod("getPlayer", Player.class).invoke(null, player);
            if (superiorPlayer != null) {
                Object island = superiorPlayer.getClass().getMethod("getIsland").invoke(superiorPlayer);
                if (island != null) {
                    Object upgradesManager = apiClass.getMethod("getUpgrades").invoke(null);
                    if (upgradesManager != null) {
                        Object upgrade = upgradesManager.getClass().getMethod("getUpgrade", String.class).invoke(upgradesManager, "sell-multiplier");
                        if (upgrade == null) {
                            upgrade = upgradesManager.getClass().getMethod("getUpgrade", String.class).invoke(upgradesManager, "sell");
                        }
                        if (upgrade != null) {
                            Class<?> upgradeClass = Class.forName("com.bgsoftware.superiorskyblock.api.upgrades.Upgrade");
                            Object upgradeLevel = island.getClass().getMethod("getUpgradeLevel", upgradeClass).invoke(island, upgrade);
                            if (upgradeLevel != null) {
                                return (double) upgradeLevel.getClass().getMethod("getSellMultiplier").invoke(upgradeLevel);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // Fallback
        }
        return 1.0;
    }
}
