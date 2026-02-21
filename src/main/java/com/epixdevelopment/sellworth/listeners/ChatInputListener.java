package com.epixdevelopment.sellworth.listeners;

import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.tracker.ViewTracker;
import com.epixdevelopment.sellworth.util.SchedulerUtil;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputListener implements Listener {
   private final Sell plugin;

   public ChatInputListener(Sell plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onChat(AsyncPlayerChatEvent e) {
      Player p = e.getPlayer();
      UUID uuid = p.getUniqueId();
      ViewTracker vt = this.plugin.getViewTracker();
      String worthFilter = vt.getFilter(uuid);
      if (worthFilter != null && worthFilter.isEmpty()) {
         e.setCancelled(true);
         e.getRecipients().clear();
         String input = e.getMessage().trim();
         vt.setFilter(uuid, input);
         SchedulerUtil.runTask(this.plugin, p, () -> {
            this.plugin.getItemPricesMenu().open(p, 1);
         });
      }

   }
}
