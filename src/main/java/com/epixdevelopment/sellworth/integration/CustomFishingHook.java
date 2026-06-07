package com.epixdevelopment.sellworth.integration;

import com.epixdevelopment.sellworth.Sell;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.mechanic.context.Context;

public class CustomFishingHook {
    private final Sell plugin;
    private boolean enabled = false;

    public CustomFishingHook(Sell plugin) {
        this.plugin = plugin;
        checkPlugin();
    }

    public void checkPlugin() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("CustomFishing");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isCustomFishingItem(ItemStack item) {
        if (!enabled || item == null) {
            return false;
        }
        try {
            String cfId = BukkitCustomFishingPlugin.getInstance().getItemManager().getCustomFishingItemID(item);
            return cfId != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public Double getPrice(Player player, ItemStack item) {
        if (!enabled || item == null) {
            return null;
        }
        try {
            String cfId = BukkitCustomFishingPlugin.getInstance().getItemManager().getCustomFishingItemID(item);
            if (cfId != null) {
                Context context = Context.player(player);
                Double basePrice = BukkitCustomFishingPlugin.getInstance().getMarketManager().getItemPrice(context, item);
                if (basePrice != null && item.hasItemMeta()) {
                    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("customfishingexpansion", "mutation_multiplier");
                    Double multiplier = item.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.DOUBLE);
                    if (multiplier != null && multiplier > 0) {
                        return basePrice * multiplier;
                    }
                }
                return basePrice;
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
