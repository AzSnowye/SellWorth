package com.epixdevelopment.sellworth.integration;

import com.epixdevelopment.sellworth.Sell;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import io.lumine.mythic.lib.api.item.NBTItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MMOItemsHook {

    private final Sell plugin;
    private final Map<String, Map<String, Double>> prices = new HashMap<>();
    private boolean enabled = false;

    public MMOItemsHook(Sell plugin) {
        this.plugin = plugin;
        checkPlugin();
        if (enabled) {
            loadConfig();
        }
    }

    public void checkPlugin() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("MMOItems");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void loadConfig() {
        prices.clear();
        File configFile = new File(plugin.getDataFolder(), "mmoitems.yml");
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("mmoitems.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create mmoitems.yml: " + e.getMessage());
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Check if config has list format (Format 2)
        if (config.isList("items")) {
            List<?> list = config.getList("items");
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) obj;
                        Object typeObj = map.get("type");
                        Object idObj = map.get("id");
                        Object baseObj = map.get("base");
                        if (typeObj != null && idObj != null && baseObj != null) {
                            String type = typeObj.toString().toUpperCase(Locale.ROOT);
                            String id = idObj.toString().toUpperCase(Locale.ROOT);
                            double price = Double.parseDouble(baseObj.toString());
                            prices.computeIfAbsent(type, k -> new HashMap<>()).put(id, price);
                        }
                    }
                }
            }
        }

        // Also check keys directly (Format 1 or root level)
        for (String type : config.getKeys(false)) {
            if (type.equalsIgnoreCase("items")) continue;
            if (config.isConfigurationSection(type)) {
                ConfigurationSection sec = config.getConfigurationSection(type);
                if (sec != null) {
                    for (String id : sec.getKeys(false)) {
                        double price = sec.getDouble(id);
                        prices.computeIfAbsent(type.toUpperCase(Locale.ROOT), k -> new HashMap<>())
                              .put(id.toUpperCase(Locale.ROOT), price);
                    }
                }
            } else if (config.isList(type)) {
                // If the root level is a list (some users might do this)
                List<?> list = config.getList(type);
                if (list != null) {
                    for (Object obj : list) {
                        if (obj instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) obj;
                            Object typeObj = map.get("type");
                            Object idObj = map.get("id");
                            Object baseObj = map.get("base");
                            if (typeObj != null && idObj != null && baseObj != null) {
                                String t = typeObj.toString().toUpperCase(Locale.ROOT);
                                String id = idObj.toString().toUpperCase(Locale.ROOT);
                                double price = Double.parseDouble(baseObj.toString());
                                prices.computeIfAbsent(t, k -> new HashMap<>()).put(id, price);
                            }
                        }
                    }
                }
            }
        }
    }

    public Double getPrice(ItemStack item) {
        if (!enabled || item == null) {
            return null;
        }

        try {
            NBTItem nbtItem = NBTItem.get(item);
            if (nbtItem != null && nbtItem.hasType()) {
                String type = nbtItem.getType().toUpperCase(Locale.ROOT);
                String id = nbtItem.getString("MMOITEMS_ITEM_ID");
                if (id != null) {
                    id = id.toUpperCase(Locale.ROOT);
                    Map<String, Double> idMap = prices.get(type);
                    if (idMap != null) {
                        return idMap.get(id);
                    }
                }
            }
        } catch (Throwable t) {
            // MMOItems or MythicLib not loaded properly, or version incompatibility
        }
        return null;
    }
}
