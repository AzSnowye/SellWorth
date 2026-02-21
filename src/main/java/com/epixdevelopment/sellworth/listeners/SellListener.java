package com.epixdevelopment.sellworth.listeners;

import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.util.Utils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class SellListener implements Listener {
   private final Sell plugin;
   private final Map<Material, Integer> values = new HashMap();
   private final List<String> loreTemplate;

   public SellListener(Sell plugin) {
      this.plugin = plugin;
      this.loadValues();
      this.loreTemplate = plugin.getConfig().getStringList("lore");
   }

   private void loadValues() {
      ConfigurationSection config = this.plugin.getConfig();
      Iterator var2 = config.getKeys(false).iterator();

      while(var2.hasNext()) {
         String key = (String)var2.next();
         if (key.endsWith("-value")) {
            String matName = key.substring(0, key.length() - 6).toUpperCase(Locale.ROOT);

            try {
               Material mat = Material.valueOf(matName);
               this.values.put(mat, config.getInt(key));
            } catch (IllegalArgumentException var6) {
               this.plugin.getLogger().warning("Unknown material in config: " + matName);
            }
         }
      }

   }

   @EventHandler
   public void onInventoryOpen(InventoryOpenEvent event) {
      if (event.getPlayer() instanceof Player) {
         Inventory inv = event.getInventory();
         if (event.getView().getType() == InventoryType.CRAFTING) {
            inv = event.getView().getBottomInventory();
         }

         for(int slot = 0; slot < inv.getSize(); ++slot) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
               int baseValue = (Integer)this.values.getOrDefault(item.getType(), 0);
               int amount = item.getAmount();
               int totalValue = baseValue * amount;
               ItemMeta meta = item.getItemMeta();
               if (meta instanceof BlockStateMeta) {
                  BlockStateMeta bsm = (BlockStateMeta)meta;
                  BlockState var11 = bsm.getBlockState();
                  if (var11 instanceof ShulkerBox) {
                     ShulkerBox box = (ShulkerBox)var11;
                     ItemStack[] var18 = box.getInventory().getContents();
                     int var12 = var18.length;

                     for(int var13 = 0; var13 < var12; ++var13) {
                        ItemStack inside = var18[var13];
                        if (inside != null && inside.getType() != Material.AIR) {
                           int insideBase = (Integer)this.values.getOrDefault(inside.getType(), 0);
                           int insideAmount = inside.getAmount();
                           totalValue += insideBase * insideAmount;
                        }
                     }
                  }
               }

               final int finalTotalValue = totalValue;
               List<String> newLore = (List)this.loreTemplate.stream().map((line) -> {
                  return line.replace("%amount%", String.valueOf(finalTotalValue));
               }).collect(Collectors.toList());
               if (meta != null) {
                  meta.setLore(newLore);
                  item.setItemMeta(meta);
                  inv.setItem(slot, item);
               }
            }
         }

      }
   }
}
