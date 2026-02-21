package com.epixdevelopment.sellworth.listeners;

import com.epixdevelopment.sellworth.Sell;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class CleanupListener implements Listener {
   private final Sell plugin;
   private final List<String> lorePrefixes;

   public CleanupListener(Sell plugin) {
      this.plugin = plugin;
      this.lorePrefixes = plugin.getConfig().getStringList("lore").stream().map((line) -> {
         return ChatColor.stripColor(line.replace("%amount%", "")).toLowerCase(Locale.ROOT).trim();
      }).toList();
   }

   public void stripAllLore(Player p) {
      ItemStack[] var2 = p.getInventory().getContents();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ItemStack it = var2[var4];
         if (it != null && it.hasItemMeta()) {
            ItemMeta meta = it.getItemMeta();
            if (meta.hasLore()) {
               List<String> filtered = new ArrayList();
               Iterator var8 = meta.getLore().iterator();

               while(var8.hasNext()) {
                  String line = (String)var8.next();
                  String plain = ChatColor.stripColor(line).toLowerCase(Locale.ROOT).trim();
                  if (this.lorePrefixes.stream().noneMatch(prefix -> plain.startsWith(prefix))) {
                     filtered.add(line);
                  }
               }

               meta.setLore(filtered.isEmpty() ? null : filtered);
               it.setItemMeta(meta);
            }
         }
      }

      p.updateInventory();
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent e) {
      Player p = e.getPlayer();
      if (p.getGameMode() == GameMode.CREATIVE) {
         this.stripAllLore(p);
      }

   }

   @EventHandler
   public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
      Player p = event.getPlayer();
      GameMode newMode = event.getNewGameMode();
      if (newMode == GameMode.CREATIVE) {
         this.stripAllLore(p);
      } else if (newMode == GameMode.SURVIVAL) {
         p.updateInventory();
      }

   }

   @EventHandler
   public void onPickup(EntityPickupItemEvent e) {
      LivingEntity var3 = e.getEntity();
      if (var3 instanceof Player) {
         Player p = (Player)var3;
         if (p.getGameMode() == GameMode.CREATIVE) {
            e.getItem().setItemStack(this.stripLoreFromStack(e.getItem().getItemStack()));
         }
      }

   }

   @EventHandler
   public void onItemMerge(ItemMergeEvent e) {
      e.getTarget().setItemStack(this.stripLoreFromStack(e.getTarget().getItemStack()));
   }

   private ItemStack stripLoreFromStack(ItemStack original) {
      if (original != null && original.hasItemMeta()) {
         ItemStack copy = original.clone();
         ItemMeta meta = copy.getItemMeta();
         if (!meta.hasLore()) {
            return copy;
         } else {
            List<String> keep = new ArrayList();
            Iterator var5 = meta.getLore().iterator();

            while(var5.hasNext()) {
               String line = (String)var5.next();
               String plain = ChatColor.stripColor(line).toLowerCase(Locale.ROOT).trim();
               if (this.lorePrefixes.stream().noneMatch(prefix -> plain.startsWith(prefix))) {
                  keep.add(line);
               }
            }

            meta.setLore(keep.isEmpty() ? null : keep);
            copy.setItemMeta(meta);
            return copy;
         }
      } else {
         return original;
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent e) {
      final Player p = (Player)e.getWhoClicked();
      if (p.getGameMode() != GameMode.CREATIVE) {
         (new BukkitRunnable() {
            public void run() {
               p.updateInventory();
            }
         }).runTaskLater(this.plugin, 1L);
      }

   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent e) {
      final Player p = (Player)e.getWhoClicked();
      if (p.getGameMode() != GameMode.CREATIVE) {
         (new BukkitRunnable() {
            public void run() {
               p.updateInventory();
            }
         }).runTaskLater(this.plugin, 1L);
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent e) {
      Player p = (Player)e.getPlayer();
      if (p.getGameMode() == GameMode.CREATIVE) {
         this.stripAllLore(p);
      }

   }
}
