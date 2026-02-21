package com.epixdevelopment.sellworth.listeners;


import com.epixdevelopment.sellworth.Sell;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.epixdevelopment.sellworth.util.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class SellMenuClickListener implements Listener {
   private final Sell plugin;
   private boolean useNew;

   public SellMenuClickListener(Sell plugin) {
      this.plugin = plugin;
      this.useNew = plugin.getConfig().getBoolean("use-new-sell-menu", false);
   }
   
   
   public void updateMenuType() {
      boolean newUseNew = plugin.getConfig().getBoolean("use-new-sell-menu", false);
      if (this.useNew != newUseNew) {
         this.useNew = newUseNew;
         plugin.getLogger().info("Updated SellMenuClickListener to use " + (this.useNew ? "new" : "old") + " menu type");
      }
   }

   @EventHandler
   public void onClick(InventoryClickEvent e) {
      if (e.getWhoClicked() instanceof Player) {
         Player p = (Player)e.getWhoClicked();
         String title = e.getView().getTitle();
         String oldTitle = Utils.formatColors(this.plugin.getConfig().getString("sell-menu.title", ""));
         String newTitle = Utils.formatColors(this.plugin.getConfig().getString("new-sell-menu.title", ""));
         boolean isOld = title.equals(oldTitle);
         boolean isNew = title.equals(newTitle);
         if (isOld || isNew) {
            Inventory top = e.getInventory();
            int topSize = top.getSize();
            int slot = e.getRawSlot();
            if (!isNew) {
               if (!isOld || !this.useNew) {
                  int bottomStart = topSize - 9;
                  if (slot >= 0) {
                     if (slot >= bottomStart && slot < bottomStart + 9) {
                        e.setCancelled(true);
                        p.playSound(p.getLocation(), Sound.valueOf(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK").toUpperCase()), 1.0F, 1.0F);
                        List<String> items = this.plugin.getConfig().getStringList("sell-menu.items");
                        int idx = slot - bottomStart;
                        if (idx >= 0 && idx < items.size()) {
                           this.plugin.getProgressGui().open(p, (String)items.get(idx));
                        }

                     }
                  }
               }
            } else {
               Set<Integer> buttonSlots = new HashSet();
               Iterator var12;
               String cat;
               int s;
               if (this.plugin.getConfig().isConfigurationSection("new-sell-menu.item-settings")) {
                  var12 = this.plugin.getConfig().getConfigurationSection("new-sell-menu.item-settings").getKeys(false).iterator();

                  while(var12.hasNext()) {
                     cat = (String)var12.next();
                     s = this.plugin.getConfig().getInt("new-sell-menu.item-settings." + cat + ".slot", -1);
                     if (s >= 0) {
                        buttonSlots.add(s);
                     }
                  }
               }

               if (slot >= 0 && slot < topSize) {
                  if (!buttonSlots.contains(slot)) {
                     e.setCancelled(true);
                  } else {
                     e.setCancelled(true);
                     p.playSound(p.getLocation(), Sound.valueOf(this.plugin.getConfig().getString("sounds.click-sound", "UI_BUTTON_CLICK").toUpperCase()), 1.0F, 1.0F);
                     var12 = this.plugin.getConfig().getConfigurationSection("new-sell-menu.item-settings").getKeys(false).iterator();

                     while(var12.hasNext()) {
                        cat = (String)var12.next();
                        s = this.plugin.getConfig().getInt("new-sell-menu.item-settings." + cat + ".slot", -1);
                        if (s == slot) {
                           this.plugin.getProgressGui().open(p, cat);
                           break;
                        }
                     }

                  }
               } else if (e.isShiftClick()) {
                  e.setCancelled(true);
               } else if (e.getClick() == ClickType.DOUBLE_CLICK) {
                  e.setCancelled(true);
               } else if (e.getClick().isKeyboardClick()) {
                  e.setCancelled(true);
               } else {
                  InventoryAction a = e.getAction();
                  if (a == InventoryAction.MOVE_TO_OTHER_INVENTORY || a == InventoryAction.HOTBAR_SWAP || a == InventoryAction.HOTBAR_MOVE_AND_READD || a == InventoryAction.COLLECT_TO_CURSOR) {
                     e.setCancelled(true);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent e) {
      if (e.getWhoClicked() instanceof Player) {
         String title = e.getView().getTitle();
         String oldTitle = Utils.formatColors(this.plugin.getConfig().getString("sell-menu.title", ""));
         String newTitle = Utils.formatColors(this.plugin.getConfig().getString("new-sell-menu.title", ""));
         boolean isOld = title.equals(oldTitle);
         boolean isNew = title.equals(newTitle);
         if (isOld || isNew) {
            Inventory top = e.getInventory();
            int topSize = top.getSize();
            if (isNew) {
               Iterator var12 = e.getRawSlots().iterator();

               int raw;
               do {
                  if (!var12.hasNext()) {
                     return;
                  }

                  raw = (Integer)var12.next();
               } while(raw < 0 || raw >= topSize);

               e.setCancelled(true);
            } else if (!isOld || !this.useNew) {
               int bottomStart = topSize - 9;
               Iterator var10 = e.getRawSlots().iterator();

               int raw;
               do {
                  if (!var10.hasNext()) {
                     return;
                  }

                  raw = (Integer)var10.next();
               } while(raw < bottomStart || raw >= bottomStart + 9);

               e.setCancelled(true);
            }
         }
      }
   }
}

