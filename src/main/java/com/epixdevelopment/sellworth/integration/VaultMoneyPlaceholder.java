package com.epixdevelopment.sellworth.integration;

import com.epixdevelopment.sellworth.Sell;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class VaultMoneyPlaceholder extends PlaceholderExpansion {
    private final Sell plugin;

    public VaultMoneyPlaceholder(Sell plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vaultmoney";
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
        
        if (params.equalsIgnoreCase("sell_money") && plugin.getEconomy() != null) {
            return String.format("%.2f", plugin.getEconomy().getBalance(player));
        }
        
        return null;
    }
}
