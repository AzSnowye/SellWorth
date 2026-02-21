package com.epixdevelopment.sellworth.listeners;

import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.gui.CategoryGui;
import com.epixdevelopment.sellworth.gui.GuiHolder;
import com.epixdevelopment.sellworth.gui.ProgressGui;
import com.epixdevelopment.sellworth.util.SchedulerUtil;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiClickListener implements Listener {
   private final Sell plugin;

   public GuiClickListener(Sell plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onClick(InventoryClickEvent e) {
      if (e.getView().getTopInventory().getHolder() instanceof GuiHolder) {
         e.setCancelled(true);
         GuiHolder holder = (GuiHolder)e.getView().getTopInventory().getHolder();
         Player p = (Player)e.getWhoClicked();
         int slot = e.getRawSlot();
         FileConfiguration cfg = this.plugin.getConfig();
         String cat;
         int prevSlot;
         if (holder.getPage() == -1) {
            cat = holder.getCategoryKey();
            ProgressGui.CategoryIcon ico = (ProgressGui.CategoryIcon)this.plugin.getProgressGui().categoryIcons.get(cat);
            prevSlot = cfg.getInt("progress-menu.back-button.slot", 45);
            if (ico != null && slot == ico.slot) {
               this.playClickSound(p, cfg);
               SchedulerUtil.runTaskLater(this.plugin, p, () -> {
                  (new CategoryGui(this.plugin, cat)).open(p, 0);
               }, 1L);
            } else if (slot == prevSlot) {
               this.playClickSound(p, cfg);
               this.plugin.getSellGui().open(p);
            }
         } else {
            cat = holder.getCategoryKey();
            int page = holder.getPage();
            prevSlot = cfg.getInt("category-menu.previous-page-slot", 49);
            int nextSlot = cfg.getInt("category-menu.next-page-slot", 51);
            int backSlot = cfg.getInt("category-menu.back-button.slot", 45);
            int rows = cfg.getInt("category-menu.rows", 6);
            int perPage = (rows - 1) * 9;
            if (slot >= 0 && slot < perPage) {
               if (e.getView().getTopInventory().getItem(slot) != null) {
                  this.playClickSound(p, cfg);
               }

            } else if (slot == prevSlot && page > 0) {
               SchedulerUtil.runTaskLater(this.plugin, p, () -> {
                  (new CategoryGui(this.plugin, cat)).open(p, page - 1);
               }, 1L);
            } else if (slot != nextSlot) {
               if (slot == backSlot) {
                  this.playClickSound(p, cfg);
                  this.plugin.getProgressGui().open(p, cat);
               }

            } else {
               int totalEntries = 0;
               if (cfg.getList("categories." + cat) != null) {
                  Iterator var14 = cfg.getList("categories." + cat).iterator();

                  while(var14.hasNext()) {
                     Object element = var14.next();
                     if (element instanceof Map) {
                        Map<?, ?> m = (Map)element;
                        totalEntries += m.size();
                     }
                  }
               }

               int maxPage = (int)Math.ceil((double)totalEntries / (double)perPage) - 1;
               if (page < maxPage) {
                  SchedulerUtil.runTaskLater(this.plugin, p, () -> {
                     (new CategoryGui(this.plugin, cat)).open(p, page + 1);
                  }, 1L);
               }

            }
         }
      }
   }

   @EventHandler
   public void onDrag(InventoryDragEvent e) {
      if (e.getView().getTopInventory().getHolder() instanceof GuiHolder) {
         e.setCancelled(true);
      }

   }

   private void playClickSound(Player p, FileConfiguration cfg) {
      String raw = cfg.getString("sounds.click-sound", "UI_BUTTON_CLICK");

      Sound snd;
      try {
         snd = Sound.valueOf(raw.toUpperCase());
      } catch (Exception var6) {
         this.plugin.getLogger().warning("Invalid sounds.click-sound: '" + raw + "'. Using UI_BUTTON_CLICK instead.");
         snd = Sound.UI_BUTTON_CLICK;
      }

      p.playSound(p.getLocation(), snd, 1.0F, 1.0F);
   }
}
