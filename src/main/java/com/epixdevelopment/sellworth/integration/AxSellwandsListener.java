package com.epixdevelopment.sellworth.integration;

import com.artillexstudios.axsellwands.api.events.AxSellwandsSellEvent;
import com.epixdevelopment.sellworth.Sell;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import java.util.UUID;

public class AxSellwandsListener implements Listener {

    private final Sell plugin;

    public AxSellwandsListener(Sell plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAxSellwandSell(AxSellwandsSellEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        double originalMoney = event.getMoneyMade();
        
        double boosterMultiplier = SeriaBoosterHook.getSellMultiplier(uuid) * SuperiorSkyblockHook.getSellMultiplier(uuid);
        double maxMilestone = plugin.getMaxMilestoneMultiplier(uuid);
        
        double newMoney = originalMoney * boosterMultiplier * maxMilestone;
        
        event.setMoneyMade(newMoney);
    }
}
