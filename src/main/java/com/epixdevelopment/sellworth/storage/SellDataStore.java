package com.epixdevelopment.sellworth.storage;

import com.epixdevelopment.sellworth.Sell;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface SellDataStore extends AutoCloseable {
    void init() throws Exception;

    void loadAll(
            Map<UUID, Double> totalSold,
            Map<UUID, Map<String, Double>> soldByCategory,
            Map<UUID, Map<String, Sell.Stats>> itemHistory,
            Set<UUID> toggleWorthDisabled
    ) throws Exception;

    void saveAll(
            Map<UUID, Double> totalSold,
            Map<UUID, Map<String, Double>> soldByCategory,
            Map<UUID, Map<String, Sell.Stats>> itemHistory,
            Set<UUID> toggleWorthDisabled
    ) throws Exception;

    void saveToggleWorth(Set<UUID> toggleWorthDisabled) throws Exception;

    void resetPlayer(UUID uuid) throws Exception;

    @Override
    void close() throws Exception;
}
