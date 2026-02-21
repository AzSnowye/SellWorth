package com.epixdevelopment.sellworth.integration;

import com.epixdevelopment.sellworth.Sell;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.UUID;

public class SellPlaceholderExpansion extends PlaceholderExpansion {
    private final Sell plugin;

    public SellPlaceholderExpansion(Sell plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sellworth";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();
        
        if (params.equalsIgnoreCase("total") || params.equalsIgnoreCase("totalsold")) {
            return String.format("%.2f", plugin.getRawTotalSold(uuid, ""));
        }
        
        if (params.endsWith("_total")) {
            String category = params.substring(0, params.length() - 6); // Remove "_total"
            return String.format("%.2f", plugin.getRawTotalSold(uuid, category));
        }
        
        return null;
    }
}

