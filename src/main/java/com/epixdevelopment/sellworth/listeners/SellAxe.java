package com.epixdevelopment.sellworth.listeners;

import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.util.SchedulerUtil;
import com.epixdevelopment.sellworth.util.Utils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SellAxe implements Listener {
   private final Sell plugin;
   private final NamespacedKey sellAxeKey;
   private final NamespacedKey expiryKey;

   public SellAxe(Sell plugin, NamespacedKey sellAxeKey, NamespacedKey expiryKey) {
      this.plugin = plugin;
      this.sellAxeKey = sellAxeKey;
      this.expiryKey = expiryKey;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onBlockBreak(BlockBreakEvent event) {
      if (!event.isCancelled()) {
         Block block = event.getBlock();
         BlockState state = block.getState();
         boolean isChest = state instanceof Chest;
         boolean isShulker = state instanceof ShulkerBox;
         if (isChest || isShulker) {
            Player player = event.getPlayer();
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand != null && !inHand.getType().isAir()) {
               ItemMeta meta = inHand.getItemMeta();
               if (meta != null) {
                  PersistentDataContainer pdc = meta.getPersistentDataContainer();
                  Byte marker = (Byte)pdc.get(this.sellAxeKey, PersistentDataType.BYTE);
                  if (marker != null && marker == 1) {
                     event.setCancelled(true);
                     if (this.plugin.getConfig().getBoolean("sell-axe.use-countdown", true)) {
                        Long expiry = (Long)pdc.get(this.expiryKey, PersistentDataType.LONG);
                        if (expiry == null || System.currentTimeMillis() >= expiry) {
                           player.getInventory().remove(inHand);
                           String msg = this.plugin.getConfig().getString("messages.expired-wand", "&cYour Sell Wand has expired and been removed.");
                           player.sendMessage(Utils.formatColors(msg));
                           return;
                        }
                     }

                     Inventory containerInv = isChest ? ((Chest)state).getInventory() : ((ShulkerBox)state).getInventory();
                     // grab a safe copy so u don't touch live bukkit inv off main
                     ItemStack[] snapshot = Arrays.stream(containerInv.getContents())
                         .map(item -> item == null ? null : item.clone())
                         .toArray(ItemStack[]::new);

                     // crunch values async, then hop back to main to pay/notify
                     SchedulerUtil.runTaskAsync(this.plugin, () -> {
                        Map<String, Sell.Stats> sold = new HashMap<>();
                        Map<String, Double> revCats = new HashMap<>();

                        for (ItemStack item : snapshot) {
                           if (item == null || item.getType().isAir()) {
                              continue;
                           }

                           ItemMeta im = item.getItemMeta();
                           if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta) {
                              EnchantmentStorageMeta esm = (EnchantmentStorageMeta)im;
                              for (Entry<Enchantment, Integer> enchEntry : esm.getStoredEnchants().entrySet()) {
                                 String enchKey = enchEntry.getKey().getKey().getKey().toLowerCase() + enchEntry.getValue();
                                 double total = this.plugin.getPrice(enchKey + "-value") * item.getAmount();
                                 sold.merge(enchKey, new Sell.Stats((double)item.getAmount(), total), (a, b) -> new Sell.Stats(a.count + b.count, a.revenue + b.revenue));
                                 for (Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                                    if (cat.getValue().contains(enchKey.toUpperCase(Locale.ROOT))) {
                                       revCats.merge(cat.getKey(), total, Double::sum);
                                    }
                                 }
                              }
                              continue;
                           }

                           if (im instanceof BlockStateMeta) {
                              BlockStateMeta bsm = (BlockStateMeta)im;
                              BlockState bs = bsm.getBlockState();
                              if (bs instanceof ShulkerBox) {
                                 ShulkerBox nested = (ShulkerBox)bs;
                                 String boxKey = item.getType().name().toLowerCase();
                                 double boxValue = this.plugin.getPrice(boxKey + "-value") * item.getAmount();
                                 sold.merge(boxKey, new Sell.Stats((double)item.getAmount(), boxValue), (a, b) -> new Sell.Stats(a.count + b.count, a.revenue + b.revenue));
                                 for (Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                                    if (cat.getValue().contains(item.getType().name())) {
                                       revCats.merge(cat.getKey(), boxValue, Double::sum);
                                    }
                                 }

                                 for (ItemStack inside : nested.getInventory().getContents()) {
                                    if (inside == null || inside.getType().isAir()) {
                                       continue;
                                    }
                                    ItemMeta innerMeta = inside.getItemMeta();
                                    if (inside.getType() == Material.ENCHANTED_BOOK && innerMeta instanceof EnchantmentStorageMeta) {
                                       EnchantmentStorageMeta esm = (EnchantmentStorageMeta)innerMeta;
                                       for (Entry<Enchantment, Integer> nestedEnchEntry : esm.getStoredEnchants().entrySet()) {
                                          String nestedKey = nestedEnchEntry.getKey().getKey().getKey().toLowerCase() + nestedEnchEntry.getValue();
                                          double val = this.plugin.getPrice(nestedKey + "-value") * inside.getAmount();
                                          sold.merge(nestedKey, new Sell.Stats((double)inside.getAmount(), val), (a, b) -> new Sell.Stats(a.count + b.count, a.revenue + b.revenue));
                                          for (Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                                             if (cat.getValue().contains(nestedKey.toUpperCase(Locale.ROOT))) {
                                                revCats.merge(cat.getKey(), val, Double::sum);
                                             }
                                          }
                                       }
                                    } else {
                                       String innerKey = inside.getType().name().toLowerCase();
                                       double val = this.plugin.calculateItemWorth(inside);
                                       sold.merge(innerKey, new Sell.Stats((double)inside.getAmount(), val), (a, b) -> new Sell.Stats(a.count + b.count, a.revenue + b.revenue));
                                       for (Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                                          if (cat.getValue().contains(inside.getType().name())) {
                                             revCats.merge(cat.getKey(), val, Double::sum);
                                          }
                                       }
                                    }
                                 }
                                 continue;
                              }
                           }

                           String key = item.getType().name().toLowerCase();
                           double value = this.plugin.calculateItemWorth(item);
                           sold.merge(key, new Sell.Stats((double)item.getAmount(), value), (a, b) -> new Sell.Stats(a.count + b.count, a.revenue + b.revenue));
                           for (Entry<String, List<String>> cat : this.plugin.categoryItems.entrySet()) {
                              if (cat.getValue().contains(item.getType().name())) {
                                 revCats.merge(cat.getKey(), value, Double::sum);
                              }
                           }
                        }

                        SchedulerUtil.runTask(this.plugin, player, () -> {
                           if (sold.isEmpty()) {
                              player.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.empty-chest", "&7Chest is empty – nothing to sell.")));
                              return;
                           }

                           this.plugin.recordSale(player, sold);
                           double payout = revCats.entrySet().stream()
                               .mapToDouble(ex -> ex.getValue() * this.plugin.getSellMultiplier(player.getUniqueId(), ex.getKey()))
                               .sum();
                           double uncategorized = sold.entrySet().stream()
                               .filter(ex -> this.plugin.categoryItems.values().stream().noneMatch(list -> list.contains(ex.getKey().toUpperCase(Locale.ROOT))))
                               .mapToDouble(ex -> ex.getValue().revenue)
                               .sum();
                           payout += uncategorized;

                           if (this.plugin.getEconomy() != null) {
                              this.plugin.getEconomy().depositPlayer(player, payout);
                           } else {
                              this.plugin.getLogger().warning("Economy provider not available; skipping payout for " + player.getName() + " ($" + payout + ")");
                           }
                           Sound closeSound = this.plugin.resolveSound(this.plugin.getConfig().getString("sell-menu.sound-on-close", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                           player.playSound(player.getLocation(), closeSound, 1.0F, 1.0F);
                           String actionbar = Utils.formatColors(this.plugin.getConfig().getString("sell-menu.actionbar-message", "&aSold $%amount%"))
                               .replace("%amount%", Utils.abbreviateNumber(payout));
                           player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionbar));
                           String chatMsg = Utils.formatColors(this.plugin.getConfig().getString("sell-menu.chat-message", "&7[Sell]&r $%amount%"))
                               .replace("%amount%", Utils.abbreviateNumber(payout));
                           player.sendMessage(chatMsg);
                           containerInv.clear();
                        });
                     });
                  }
               }
            }
         }
      }
   }
}
