package com.epixdevelopment.sellworth.util;

import com.epixdevelopment.sellworth.Sell;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransactionLog {

    private final Sell plugin;
    private boolean toConsole;
    private boolean toFile;
    private String formatDate;
    private String formatSell;
    private String formatSellAll;

    public TransactionLog(Sell plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection logSection = plugin.getConfig().getConfigurationSection("log");
        if (logSection != null) {
            toConsole = logSection.getBoolean("toConsole", true);
            toFile = logSection.getBoolean("toFile", true);
            formatDate = logSection.getString("formatDate", "yyyy/MM/dd HH:mm:ss");
            formatSell = logSection.getString("formatSell", "%player% sold %amount% x %item% for %price% to %shop% shop");
            formatSellAll = logSection.getString("formatSellAll", "%player% sold all %amount% x %item% for %price% to %shop% shop");
        } else {
            toConsole = false;
            toFile = false;
        }
    }

    public void logSell(Player player, String item, double amount, double price, boolean isSellAll) {
        if (!toConsole && !toFile) return;

        String format = isSellAll ? formatSellAll : formatSell;
        if (format == null || format.isEmpty()) return;

        DecimalFormat df = new DecimalFormat("#,##0.00");

        String dateStr = new SimpleDateFormat(formatDate).format(new Date());
        
        String message = format
                .replace("%player%", player.getName())
                .replace("%amount%", String.valueOf((int) amount))
                .replace("%item%", item)
                .replace("%price%", df.format(price))
                .replace("%shop%", "SellWorth");

        String fullMessage = dateStr + " " + message;

        if (toConsole) {
            plugin.getLogger().info("Transaction: " + message);
        }

        if (toFile) {
            File logFile = new File(plugin.getDataFolder(), "transactions.log");
            try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                out.println(fullMessage);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not write to transactions.log: " + e.getMessage());
            }
        }
    }
}
