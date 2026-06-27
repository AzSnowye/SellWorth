package com.epixdevelopment.sellworth.integration;

import com.epixdevelopment.sellworth.Sell;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import com.epromite.egens.EGensPlugin;
import com.epromite.egens.core.drops.DropEntry;

public class EGensHook {
    private final Sell plugin;
    private boolean enabled = false;

    public EGensHook(Sell plugin) {
        this.plugin = plugin;
        checkPlugin();
    }

    public void checkPlugin() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("eGens");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isGeneratorItem(ItemStack item) {
        if (!enabled || item == null) {
            return false;
        }
        try {
            EGensPlugin egens = EGensPlugin.getInstance();
            if (egens != null) {
                return egens.generatorItems().isGeneratorItem(item);
            }
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }

    public boolean isEgensDrop(ItemStack item) {
        if (!enabled || item == null) {
            return false;
        }
        try {
            EGensPlugin egens = EGensPlugin.getInstance();
            if (egens != null) {
                if (egens.generatorItems().isGeneratorItem(item)) {
                    return false;
                }
                return egens.dropsItems().resolve(item) != null;
            }
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }

    public Double getPrice(ItemStack item) {
        if (!enabled || item == null) {
            return null;
        }
        try {
            EGensPlugin egens = EGensPlugin.getInstance();
            if (egens != null) {
                if (egens.generatorItems().isGeneratorItem(item)) {
                    return null;
                }
                DropEntry entry = egens.dropsItems().resolve(item);
                if (entry != null) {
                    return entry.price();
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
