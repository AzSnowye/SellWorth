package com.epixdevelopment.sellworth.commands;


import com.epixdevelopment.sellworth.Sell;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import com.epixdevelopment.sellworth.util.Utils;

public class SellAxeCommand implements CommandExecutor, TabCompleter {
   private final Sell plugin;
   private final NamespacedKey sellAxeKey;
   private final NamespacedKey expiryKey;

   public SellAxeCommand(Sell plugin, NamespacedKey sellAxeKey, NamespacedKey expiryKey) {
      this.plugin = plugin;
      this.sellAxeKey = sellAxeKey;
      this.expiryKey = expiryKey;
      plugin.getCommand("sellwand").setExecutor(this);
      plugin.getCommand("sellwand").setTabCompleter(this);
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!sender.hasPermission("sell.admin")) {
         sender.sendMessage(Utils.formatColors("&cYou do not have permission."));
         return true;
      } else if ((args.length == 2 || args.length == 3) && args[0].equalsIgnoreCase("givesellwand")) {
         Player target = Bukkit.getPlayerExact(args[1]);
         if (target == null) {
            sender.sendMessage(Utils.formatColors("&cPlayer not found: " + args[1]));
            return true;
         } else {
            ItemStack sellAxe = new ItemStack(Material.NETHERITE_AXE, 1);
            ItemMeta meta = sellAxe.getItemMeta();
            if (meta == null) {
               this.plugin.getLogger().warning("Failed to create Sell Wand item!");
               return true;
            } else {
               String rawName = this.plugin.getConfig().getString("sell-axe.display-name", "&aSell Wand");
               meta.setDisplayName(Utils.formatColors(rawName));
               List<String> loreTemplate = this.plugin.getConfig().getStringList("sell-axe.lore");
               boolean useCountdown = this.plugin.getConfig().getBoolean("sell-axe.use-countdown", true);
               long expiryMillis = 0L;
               String enchEntry;
               String enchName;
               if (useCountdown) {
                  long durationSeconds;
                  if (args.length == 3) {
                     durationSeconds = parseCustomTime(args[2]);
                     if (durationSeconds <= 0) {
                        sender.sendMessage(Utils.formatColors("&cInvalid time format. Use formats like: 3d, 2h, 30m, 45s"));
                        return true;
                     }
                  } else {
                     durationSeconds = this.plugin.getConfig().getLong("sell-axe.duration-seconds", 259200L);
                  }
                  expiryMillis = System.currentTimeMillis() + durationSeconds * 1000L;
                  enchEntry = this.formatDuration(durationSeconds * 1000L);

                  for(int i = 0; i < loreTemplate.size(); ++i) {
                     enchName = ((String)loreTemplate.get(i)).replace("%countdown%", enchEntry);
                     loreTemplate.set(i, Utils.formatColors(enchName));
                  }
               } else {
                  for(int i = 0; i < loreTemplate.size(); ++i) {
                     loreTemplate.set(i, Utils.formatColors((String)loreTemplate.get(i)));
                  }
               }

               meta.setLore(loreTemplate);
               List<String> enchList = this.plugin.getConfig().getStringList("sell-axe.enchantments");
               if (enchList != null) {
                  Iterator var14 = enchList.iterator();

                  while(var14.hasNext()) {
                     enchEntry = (String)var14.next();
                     String[] parts = enchEntry.split(":");
                     if (parts.length == 2) {
                        try {
                           enchName = parts[0].toUpperCase().trim();
                           int level = Integer.parseInt(parts[1].trim());
                           Enchantment ench = Enchantment.getByName(enchName);
                           if (ench != null) {
                              meta.addEnchant(ench, level, true);
                           } else {
                              this.plugin.getLogger().warning("Unknown enchantment: " + enchName);
                           }
                        } catch (NumberFormatException var20) {
                           this.plugin.getLogger().warning("Invalid enchant level: " + enchEntry);
                        }
                     }
                  }
               }

               PersistentDataContainer pdc = meta.getPersistentDataContainer();
               pdc.set(this.sellAxeKey, PersistentDataType.BYTE, (byte)1);
               if (useCountdown) {
                  pdc.set(this.expiryKey, PersistentDataType.LONG, expiryMillis);
               }

               sellAxe.setItemMeta(meta);
               target.getInventory().addItem(new ItemStack[]{sellAxe});
               return true;
            }
         }
      } else {
         sender.sendMessage(Utils.formatColors("&cUsage: /sellwand givesellwand <player> [time]"));
         sender.sendMessage(Utils.formatColors("&7Time formats: 3d, 2h, 30m, 45s"));
         return true;
      }
   }

   private String formatDuration(long ms) {
      long totalSeconds = ms / 1000L;
      long days = totalSeconds / 86400L;
      long hours = totalSeconds % 86400L / 3600L;
      long minutes = totalSeconds % 3600L / 60L;
      long seconds = totalSeconds % 60L;
      
      if (totalSeconds > 59) {
         if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
         } else if (hours > 0) {
            return hours + "h " + minutes + "m";
         } else {
            return minutes + "m";
         }
      } else {
         if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
         } else if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
         } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
         } else {
            return seconds + "s";
         }
      }
   }

   private long parseCustomTime(String timeStr) {
      timeStr = timeStr.toLowerCase().trim();
      
      if (timeStr.isEmpty() || timeStr.length() < 2) return -1;
      
      char unit = timeStr.charAt(timeStr.length() - 1);
      String numberStr = timeStr.substring(0, timeStr.length() - 1);
      
      try {
         long value = Long.parseLong(numberStr);
         if (value < 0) return -1;
         
         switch (unit) {
            case 'd':
               return value * 86400;
            case 'h':
               return value * 3600;
            case 'm':
               return value * 60;
            case 's':
               return value;
            default:
               return -1;
         }
      } catch (NumberFormatException e) {
         return -1;
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      if (!sender.hasPermission("sell.admin")) {
         return Collections.emptyList();
      } else if (args.length == 1) {
         return "givesellwand".startsWith(args[0].toLowerCase()) ? Collections.singletonList("givesellwand") : Collections.emptyList();
      } else if (args.length == 2) {
         String prefix = args[1].toLowerCase();
         return (List)Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((name) -> {
            return name.toLowerCase().startsWith(prefix);
         }).collect(Collectors.toList());
      } else {
         return Collections.emptyList();
      }
   }
}

