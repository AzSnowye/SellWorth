package com.epixdevelopment.sellworth.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class SchedulerUtil {
    private static final boolean FOLIA;
    private static Method getRegionSchedulerMethod;
    private static Method getSchedulerMethod;
    private static volatile boolean initialized = false;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static void init(Plugin plugin) {
        if (!FOLIA) {
            initialized = true;
            return;
        }
        if (initialized) {
            return;
        }
        if (plugin == null) {
            return;
        }

        try {
            getRegionSchedulerMethod = Bukkit.getServer().getClass().getMethod("getRegionScheduler");
            getSchedulerMethod = Player.class.getMethod("getScheduler");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize Folia scheduler: " + e.getMessage());
        } finally {
            initialized = true;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runTask(Plugin plugin, Runnable task) {
        if (!initialized) {
            init(plugin);
        }
        if (FOLIA && initialized) {
            try {
                Object scheduler = getRegionSchedulerMethod.invoke(Bukkit.getServer());
                scheduler.getClass().getMethod("run", Plugin.class, Runnable.class)
                        .invoke(scheduler, plugin, task);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to run Folia task, falling back to sync: " + e.getMessage());
            }
        }
        try {
            Bukkit.getScheduler().runTask(plugin, task);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule sync task: " + e.getMessage());
        }
    }

    public static void runTaskLater(Plugin plugin, Runnable task, long delay) {
        if (!initialized) {
            init(plugin);
        }
        // Use Bukkit scheduler for delayed tasks - works on both Bukkit and Folia
        try {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule delayed sync task: " + e.getMessage());
        }
    }

    public static void runTask(Plugin plugin, Player player, Runnable task) {
        if (!initialized) {
            init(plugin);
        }
        if (FOLIA && player != null && initialized) {
            try {
                Object scheduler = getSchedulerMethod.invoke(player);
                scheduler.getClass().getMethod("run", Plugin.class, Runnable.class)
                        .invoke(scheduler, plugin, task);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to run Folia player task, falling back to sync: " + e.getMessage());
            }
        }
        try {
            Bukkit.getScheduler().runTask(plugin, task);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule player sync task: " + e.getMessage());
        }
    }

    public static void runTaskLater(Plugin plugin, Player player, Runnable task, long delay) {
        if (!initialized) {
            init(plugin);
        }
        // Use Bukkit scheduler for player delayed tasks - works on both Bukkit and Folia
        // For short delays like 1 tick, this is safe and avoids Folia player scheduler complexity
        try {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule delayed player sync task: " + e.getMessage());
        }
    }

    public static void runTaskAsync(Plugin plugin, Runnable task) {
        if (!initialized) {
            init(plugin);
        }
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to schedule async task: " + e.getMessage());
        }
    }
}

