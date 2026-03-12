package com.epixdevelopment.sellworth.listeners;


import com.epixdevelopment.sellworth.Sell;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.epixdevelopment.sellworth.tracker.ViewTracker;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryClickListener implements Listener {
   private final Sell plugin;
   private final Set<UUID> suppressClear = new HashSet();

   public InventoryClickListener(Sell plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onClick(InventoryClickEvent e) {
      if (e.getWhoClicked() instanceof Player) {
         Player p = (Player)e.getWhoClicked();
         UUID uuid = p.getUniqueId();
         ViewTracker vt = this.plugin.getViewTracker();
         if (vt.isTracked(uuid)) {
            Inventory top = e.getView().getTopInventory();
            if (e.getClickedInventory() == top) {
               e.setCancelled(true);
               Sound clickSound = this.plugin.resolveSound(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
               p.playSound(p.getLocation(), clickSound, 1.0F, 1.0F);
               int slot = e.getRawSlot();
               int page = vt.getPage(uuid);
               int prev = this.plugin.getConfig().getInt("item-prices-menu.previous.previous-page-slot");
               int next = this.plugin.getConfig().getInt("item-prices-menu.next.next-page-slot");
               int filter = this.plugin.getConfig().getInt("item-prices-menu.filter.slot");
               int refresh = this.plugin.getConfig().getInt("item-prices-menu.refresh.slot");
               int sort = this.plugin.getConfig().getInt("item-prices-menu.sort.slot");
               if (slot == prev && page > 1) {
                  this.suppressClear.add(uuid);
                  this.plugin.getItemPricesMenu().open(p, page - 1);
               } else if (slot == next) {
                  this.suppressClear.add(uuid);
                  this.plugin.getItemPricesMenu().open(p, page + 1);
               } else if (slot == sort) {
                  vt.cycleOrder(uuid);
                  this.suppressClear.add(uuid);
                  this.plugin.getItemPricesMenu().open(p, page);
               } else if (slot == filter) {
                  List<String> options = this.plugin.getConfig().getStringList("item-prices-menu.filter.lore");
                  String cur = vt.getFilter(uuid);
                  int idx = options.indexOf(cur == null ? "all" : cur);
                  idx = (idx + 1) % options.size();
                  String nextCat = (String)options.get(idx);
                  vt.setFilter(uuid, nextCat.equalsIgnoreCase("all") ? null : nextCat);
                  this.suppressClear.add(uuid);
                  this.plugin.getItemPricesMenu().open(p, page);
               } else if (slot == refresh) {
                  vt.setFilter(uuid, (String)null);
                  this.suppressClear.add(uuid);
                  this.plugin.getItemPricesMenu().open(p, 1);
               }

            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent e) {
      if (e.getWhoClicked() instanceof Player) {
         Player p = (Player)e.getWhoClicked();
         UUID uuid = p.getUniqueId();
         ViewTracker vt = this.plugin.getViewTracker();
         if (vt.isTracked(uuid)) {
            Inventory top = e.getView().getTopInventory();
            int size = top.getSize();
            Iterator var7 = e.getRawSlots().iterator();

            int raw;
            do {
               if (!var7.hasNext()) {
                  return;
               }

               raw = (Integer)var7.next();
            } while(raw < 0 || raw >= size);

            e.setCancelled(true);
            Sound clickSound = this.plugin.resolveSound(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK"), Sound.UI_BUTTON_CLICK);
            p.playSound(p.getLocation(), clickSound, 1.0F, 1.0F);
         }
      }
   }

   @EventHandler
   public void onClose(InventoryCloseEvent e) {
      if (e.getPlayer() instanceof Player) {
         Player p = (Player)e.getPlayer();
         UUID uuid = p.getUniqueId();
         ViewTracker vt = this.plugin.getViewTracker();
         if (vt.isTracked(uuid)) {
            if (!this.suppressClear.remove(uuid)) {
               String filter = vt.getFilter(uuid);
               if (filter == null || !filter.isEmpty()) {
                  vt.clear(uuid);
               }
            }
         }
      }
   }
}
