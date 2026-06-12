package com.epixdevelopment.sellworth.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import project.seria.seriaBooster.SeriaBooster;

import java.util.UUID;

public class SeriaBoosterHook {
    private static boolean enabled = false;
    private static SeriaBooster seriaBooster;

    public static void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SeriaBooster");
        if (plugin != null && plugin.isEnabled() && plugin instanceof SeriaBooster) {
            try {
                seriaBooster = (SeriaBooster) plugin;
                enabled = true;
                Bukkit.getLogger().info("[SellWorth] Successfully hooked into SeriaBooster for sell multipliers.");
            } catch (Exception e) {
                Bukkit.getLogger().warning("[SellWorth] Failed to hook into SeriaBooster: " + e.getMessage());
            }
        }
    }

    public static double getSellMultiplier(UUID uuid) {
        if (uuid == null) {
            return 1.0;
        }
        if (seriaBooster == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("SeriaBooster");
            if (plugin != null && plugin.isEnabled() && plugin instanceof SeriaBooster) {
                seriaBooster = (SeriaBooster) plugin;
                enabled = true;
            }
        }
        if (!enabled || seriaBooster == null) {
            return 1.0;
        }
        try {
            return seriaBooster.boosters().getCombinedMultiplier(uuid, "sell");
        } catch (Exception e) {
            return 1.0;
        }
    }
}
