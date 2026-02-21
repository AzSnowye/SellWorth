package com.epixdevelopment.sellworth.storage;

import com.epixdevelopment.sellworth.Sell;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class YamlSellDataStore implements SellDataStore {
    private final Sell plugin;
    private final File file;
    private FileConfiguration config;

    public YamlSellDataStore(Sell plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "databases.yml");
    }

    @Override
    public void init() throws Exception {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new IOException("Could not create databases.yml: " + e.getMessage(), e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void loadAll(
            Map<UUID, Double> totalSold,
            Map<UUID, Map<String, Double>> soldByCategory,
            Map<UUID, Map<String, Sell.Stats>> itemHistory,
            Set<UUID> toggleWorthDisabled
    ) throws Exception {
        ensureInit();

        totalSold.clear();
        soldByCategory.clear();
        itemHistory.clear();
        toggleWorthDisabled.clear();

        if (config.isConfigurationSection("players")) {
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            if (playersSection != null) {
                for (String playerId : playersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(playerId);
                        String playerPath = "players." + playerId;

                        double total = config.getDouble(playerPath + ".total", 0.0D);
                        totalSold.put(uuid, total);

                        if (config.isConfigurationSection(playerPath + ".categories")) {
                            ConfigurationSection catSection = config.getConfigurationSection(playerPath + ".categories");
                            if (catSection != null) {
                                Map<String, Double> categories = new HashMap<>();
                                for (String category : catSection.getKeys(false)) {
                                    categories.put(category, catSection.getDouble(category));
                                }
                                soldByCategory.put(uuid, categories);
                            }
                        }

                        if (config.isConfigurationSection(playerPath + ".items")) {
                            ConfigurationSection itemsSection = config.getConfigurationSection(playerPath + ".items");
                            if (itemsSection != null) {
                                Map<String, Sell.Stats> items = new HashMap<>();
                                for (String itemKey : itemsSection.getKeys(false)) {
                                    String itemPath = playerPath + ".items." + itemKey;
                                    long count = config.getLong(itemPath + ".count", 0L);
                                    double revenue = config.getDouble(itemPath + ".revenue", 0.0D);
                                    items.put(itemKey, new Sell.Stats(count, revenue));
                                }
                                itemHistory.put(uuid, items);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in databases.yml: " + playerId);
                    }
                }
            }
        }

        List<String> raw = config.getStringList("toggleworth-disabled");
        if (raw != null) {
            for (String uuidStr : raw) {
                try {
                    toggleWorthDisabled.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in toggleworth-disabled: " + uuidStr);
                }
            }
        }
    }

    @Override
    public void saveAll(
            Map<UUID, Double> totalSold,
            Map<UUID, Map<String, Double>> soldByCategory,
            Map<UUID, Map<String, Sell.Stats>> itemHistory,
            Set<UUID> toggleWorthDisabled
    ) throws Exception {
        ensureInit();

        for (Map.Entry<UUID, Double> entry : totalSold.entrySet()) {
            String playerPath = "players." + entry.getKey();
            config.set(playerPath + ".total", entry.getValue());

            Map<String, Double> categories = soldByCategory.get(entry.getKey());
            if (categories != null) {
                for (Map.Entry<String, Double> category : categories.entrySet()) {
                    config.set(playerPath + ".categories." + category.getKey(), category.getValue());
                }
            }

            Map<String, Sell.Stats> items = itemHistory.get(entry.getKey());
            if (items != null) {
                for (Map.Entry<String, Sell.Stats> item : items.entrySet()) {
                    String itemPath = playerPath + ".items." + item.getKey();
                    config.set(itemPath + ".count", item.getValue().count);
                    config.set(itemPath + ".revenue", item.getValue().revenue);
                }
            }
        }

        config.set("toggleworth-disabled", new ArrayList<>(toggleWorthDisabled));
        config.save(file);
    }

    @Override
    public void saveToggleWorth(Set<UUID> toggleWorthDisabled) throws Exception {
        ensureInit();
        List<String> raw = toggleWorthDisabled.stream().map(UUID::toString).toList();
        config.set("toggleworth-disabled", raw);
        config.save(file);
    }

    @Override
    public void resetPlayer(UUID uuid) throws Exception {
        ensureInit();
        config.set("players." + uuid, null);
        config.save(file);
    }

    @Override
    public void close() {
    }

    private void ensureInit() throws Exception {
        if (config == null) {
            init();
        }
    }
}
