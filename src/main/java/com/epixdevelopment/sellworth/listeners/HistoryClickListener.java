package com.epixdevelopment.sellworth.listeners;


import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.gui.SellHistoryGui;
import com.epixdevelopment.sellworth.tracker.HistoryTracker;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class HistoryClickListener implements Listener {
   private final Sell plugin;
   private final Set<UUID> suppressClear = new HashSet();

   public HistoryClickListener(Sell plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onClick(InventoryClickEvent e) {
      if (e.getWhoClicked() instanceof Player) {
         Player p = (Player)e.getWhoClicked();
         HistoryTracker tracker = this.plugin.getHistoryTracker();
         SellHistoryGui gui = this.plugin.getSellHistoryGui();
         String title = e.getView().getTitle();
         if (tracker.isTracked(p.getUniqueId())) {
            if (gui.matchesTitle(title)) {
               e.setCancelled(true);
               int slot = e.getRawSlot();
               int page = tracker.getPage(p.getUniqueId());
               if (e.getCurrentItem() != null && !e.getCurrentItem().getType().isAir()) {
                  Sound clickSound = this.plugin.resolveSound(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
                  p.playSound(p.getLocation(), clickSound, 1.0F, 1.0F);
               }

               if (slot == gui.getSortSlot()) {
                  tracker.cycleOrder(p.getUniqueId());
                  this.suppressClear.add(p.getUniqueId());
                  gui.open(p, page);
               } else if (slot == gui.getRefreshSlot()) {
                  tracker.setFilter(p.getUniqueId(), (String)null);
                  this.suppressClear.add(p.getUniqueId());
                  gui.open(p, 1);
               } else {
                  int perPage = (gui.getRows() - 1) * 9;
                  int total = this.plugin.getHistory(p.getUniqueId()).size();
                  int maxPages = (total + perPage - 1) / perPage;
                  if (slot == gui.getBackSlot() && page > 1) {
                     this.suppressClear.add(p.getUniqueId());
                     gui.open(p, page - 1);
                  } else if (slot == gui.getNextSlot() && page < maxPages) {
                     this.suppressClear.add(p.getUniqueId());
                     gui.open(p, page + 1);
                  }

               }
            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent e) {
      if (e.getWhoClicked() instanceof Player) {
         Player p = (Player)e.getWhoClicked();
         HistoryTracker tracker = this.plugin.getHistoryTracker();
         SellHistoryGui gui = this.plugin.getSellHistoryGui();
         if (tracker.isTracked(p.getUniqueId())) {
            if (gui.matchesTitle(e.getView().getTitle())) {
               e.setCancelled(true);
            }
         }
      }
   }

   @EventHandler
   public void onClose(InventoryCloseEvent e) {
      if (e.getPlayer() instanceof Player) {
         Player p = (Player)e.getPlayer();
         UUID id = p.getUniqueId();
         if (!this.suppressClear.remove(id)) {
            this.plugin.getHistoryTracker().clear(id);
         }
      }
   }
}
