package com.epixdevelopment.sellworth.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SellWorthSellEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final double payout;
    private final long itemsSold;

    public SellWorthSellEvent(Player player, double payout, long itemsSold) {
        this.player = player;
        this.payout = payout;
        this.itemsSold = itemsSold;
    }

    public Player getPlayer() {
        return player;
    }

    public double getPayout() {
        return payout;
    }

    public long getItemsSold() {
        return itemsSold;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
