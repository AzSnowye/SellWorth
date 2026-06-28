package com.epixdevelopment.sellworth.commands;


import com.epixdevelopment.sellworth.Sell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import com.epixdevelopment.sellworth.util.Utils;

public class SellCommand implements CommandExecutor, TabCompleter {
   private final Sell plugin;

   public SellCommand(Sell plugin) {
      this.plugin = plugin;
      plugin.getCommand("sell").setExecutor(this);
      plugin.getCommand("sell").setTabCompleter(this);
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
         if (!sender.hasPermission("sell.admin")) {
            sender.sendMessage(Utils.formatColors("&cYou do not have permission to reload the config."));
            return true;
         } else {
            this.plugin.reloadPlugin();
            sender.sendMessage(Utils.formatColors("&aSell config reloaded."));
            return true;
         }
      } else {
         String category;
         if (args.length == 2 && args[0].equalsIgnoreCase("resetall")) {
            if (!sender.hasPermission("sell.admin")) {
               sender.sendMessage(Utils.formatColors("&cYou do not have permission to reset player data."));
               return true;
            } else {
               category = args[1];
               OfflinePlayer offline = Bukkit.getOfflinePlayer(category);
               UUID targetUUID = offline.getUniqueId();
               if (!offline.hasPlayedBefore() && Bukkit.getPlayerExact(category) == null) {
                  sender.sendMessage(Utils.formatColors("&cPlayer &e" + category + " &cnot found."));
                  return true;
               } else {
                  if (sender instanceof Player) {
                     Player adminPlayer = (Player)sender;
                     this.plugin.getResetConfirmationGui().open(adminPlayer, offline);
                  } else {
                     sender.sendMessage(Utils.formatColors("&cOnly in‐game players can confirm a reset."));
                  }

                  return true;
               }
            }
         } else if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("sell.admin")) {
               sender.sendMessage(Utils.formatColors("&cYou do not have permission to reset player data."));
               return true;
            }
            String playerName = args[1];
            String cat = args[2].toLowerCase(Locale.ROOT);
            if (!this.plugin.categoryItems.containsKey(cat)) {
               sender.sendMessage(Utils.formatColors("&cCategory &e" + cat + " &cnot found."));
               return true;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
            if (!offline.hasPlayedBefore() && Bukkit.getPlayerExact(playerName) == null) {
               sender.sendMessage(Utils.formatColors("&cPlayer &e" + playerName + " &cnot found."));
               return true;
            }
            this.plugin.resetCategoryProgress(offline.getUniqueId(), cat);
            sender.sendMessage(Utils.formatColors("&aSuccessfully reset progress of &e" + offline.getName() + " &afor category &e" + cat + "&a."));
            return true;
         } else if (args.length == 4 && args[0].equalsIgnoreCase("setlevel")) {
            if (!sender.hasPermission("sell.admin")) {
               sender.sendMessage(Utils.formatColors("&cYou do not have permission to modify player data."));
               return true;
            }
            String playerName = args[1];
            String cat = args[2].toLowerCase(Locale.ROOT);
            String level = args[3];
            if (!this.plugin.categoryItems.containsKey(cat)) {
               sender.sendMessage(Utils.formatColors("&cCategory &e" + cat + " &cnot found."));
               return true;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
            if (!offline.hasPlayedBefore() && Bukkit.getPlayerExact(playerName) == null) {
               sender.sendMessage(Utils.formatColors("&cPlayer &e" + playerName + " &cnot found."));
               return true;
            }
            boolean success = this.plugin.setCategoryLevelProgress(offline.getUniqueId(), cat, level);
            if (success) {
               sender.sendMessage(Utils.formatColors("&aSuccessfully set milestone level of &e" + offline.getName() + " &ain category &e" + cat + " &ato &e" + level + "&a."));
            } else {
               sender.sendMessage(Utils.formatColors("&cLevel &e" + level + " &cnot found."));
            }
            return true;
         } else if (args.length == 3 && args[0].equalsIgnoreCase("resetmilestone")) {
            if (!sender.hasPermission("sell.admin")) {
               sender.sendMessage(Utils.formatColors("&cYou do not have permission to modify milestones."));
               return true;
            }
            String playerName = args[1];
            String cat = args[2].toLowerCase(Locale.ROOT);
            if (!cat.equals("all") && !this.plugin.categoryItems.containsKey(cat)) {
               sender.sendMessage(Utils.formatColors("&cCategory &e" + cat + " &cnot found."));
               return true;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
            if (!offline.hasPlayedBefore() && Bukkit.getPlayerExact(playerName) == null) {
               sender.sendMessage(Utils.formatColors("&cPlayer &e" + playerName + " &cnot found."));
               return true;
            }
            this.plugin.resetCategoryLevelProgress(offline.getUniqueId(), cat);
            sender.sendMessage(Utils.formatColors("&aSuccessfully reset milestone of &e" + offline.getName() + " &ain category &e" + cat + "&a."));
            return true;
         } else if (args.length == 3 && args[0].equalsIgnoreCase("removeitem")) {
            if (!sender.hasPermission("sell.admin")) {
               sender.sendMessage(Utils.formatColors("&cYou do not have permission to modify prices."));
               return true;
            } else {
               category = args[1].toLowerCase(Locale.ROOT);
               org.bukkit.configuration.file.FileConfiguration catCfg = this.plugin.getConfigManager().getCategoriesConfig();
               ConfigurationSection catsSection = catCfg.getConfigurationSection("categories");
               if (catsSection != null && catsSection.getKeys(false).contains(category)) {
                  String entryKey = args[2].toLowerCase(Locale.ROOT).replace(' ', '_');
                  if (!entryKey.endsWith("-value")) {
                     entryKey = entryKey + "-value";
                  }

                  String path = "categories." + category;
                  List<Map<?, ?>> rawList = catCfg.getMapList(path);
                  List<Map<String, Object>> newList = new ArrayList();
                  Iterator varRaw = rawList.iterator();

                  while(varRaw.hasNext()) {
                     Map<?, ?> m = (Map)varRaw.next();
                     Map<String, Object> copy = new HashMap(m);
                     newList.add(copy);
                  }

                  boolean removed = false;
                  Iterator<Map<String, Object>> it = newList.iterator();

                  while(it.hasNext()) {
                     Map<String, Object> m = it.next();
                     if (m.containsKey(entryKey)) {
                        it.remove();
                        removed = true;
                     }
                  }

                  if (!removed) {
                     sender.sendMessage(Utils.formatColors("&cItem &e" + entryKey + " &cnot found in category &e" + category + "&c."));
                     return true;
                  }

                  catCfg.set(path, newList);
                  this.plugin.getConfigManager().saveCategoriesConfig();
                  this.plugin.reloadPlugin();
                  sender.sendMessage(Utils.formatColors("&aRemoved &e" + entryKey + " &afrom category &e" + category));
                  return true;
               } else {
                  sender.sendMessage(Utils.formatColors("&cUnknown category: " + category));
                  return true;
               }
            }
         } else if (args.length == 3 && args[0].equalsIgnoreCase("addhanditem")) {
            if (!sender.hasPermission("sell.admin")) {
               sender.sendMessage(Utils.formatColors("&cYou do not have permission to modify prices."));
               return true;
            } else if (!(sender instanceof Player)) {
               sender.sendMessage(Utils.formatColors("&cOnly players can use this."));
               return true;
            } else {
               category = args[1].toLowerCase(Locale.ROOT);
               org.bukkit.configuration.file.FileConfiguration catCfg = this.plugin.getConfigManager().getCategoriesConfig();
               ConfigurationSection catsSection = catCfg.getConfigurationSection("categories");
               if (catsSection != null && catsSection.getKeys(false).contains(category)) {
                  double price;
                  try {
                     price = Double.parseDouble(args[2]);
                     if (price < 0.0D) {
                        throw new NumberFormatException();
                     }
                  } catch (NumberFormatException var19) {
                     sender.sendMessage(Utils.formatColors("&cPlease specify a valid non-negative number for price."));
                     return true;
                  }

                  Player player = (Player)sender;
                  ItemStack hand = player.getInventory().getItemInMainHand();
                  if (hand != null && !hand.getType().isAir()) {
                     String entryKey;
                     label148: {
                        ItemMeta var16;
                        if (hand.getType() == Material.SPAWNER) {
                           var16 = hand.getItemMeta();
                           if (var16 instanceof BlockStateMeta) {
                              BlockStateMeta bsm = (BlockStateMeta)var16;
                              BlockState var27 = bsm.getBlockState();
                              if (var27 instanceof CreatureSpawner) {
                                 CreatureSpawner cs = (CreatureSpawner)var27;
                                 String spawned = cs.getSpawnedType().name().toLowerCase(Locale.ROOT);
                                 entryKey = spawned + "_spawner-value";
                              } else {
                                 entryKey = "spawner-value";
                              }
                              break label148;
                           }
                        }

                        String var10000;
                        if (hand.getType() == Material.ENCHANTED_BOOK) {
                           var16 = hand.getItemMeta();
                           if (var16 instanceof EnchantmentStorageMeta) {
                              EnchantmentStorageMeta esm = (EnchantmentStorageMeta)var16;
                              if (esm.getStoredEnchants().size() != 1) {
                                 sender.sendMessage(Utils.formatColors("&cHold an enchanted book with exactly one enchantment."));
                                 return true;
                              }

                              Entry<Enchantment, Integer> e = (Entry)esm.getStoredEnchants().entrySet().iterator().next();
                              var10000 = ((Enchantment)e.getKey()).getKey().getKey().toLowerCase(Locale.ROOT);
                              entryKey = var10000 + String.valueOf(e.getValue()) + "-value";
                              break label148;
                           }
                        }

                        var16 = hand.getItemMeta();
                        if (var16 instanceof PotionMeta) {
                           PotionMeta pm = (PotionMeta)var16;
                           String base = pm.getBasePotionData().getType().name().toLowerCase(Locale.ROOT);
                           if (pm.getBasePotionData().isExtended()) {
                              base = "long_" + base;
                           }

                           if (pm.getBasePotionData().isUpgraded()) {
                              base = "strong_" + base;
                           }

                           Material mat = hand.getType();
                           if (mat == Material.SPLASH_POTION) {
                              base = "splash_" + base;
                           } else if (mat == Material.LINGERING_POTION) {
                              base = "lingering_" + base;
                           }

                           entryKey = base + "-value";
                        } else {
                           var10000 = hand.getType().name();
                           entryKey = var10000.toLowerCase(Locale.ROOT) + "-value";
                        }
                     }

                     String path = "categories." + category;
                     List<Map<?, ?>> rawList = catCfg.getMapList(path);
                     List<Map<String, Object>> newList = new ArrayList();
                     Iterator var35 = rawList.iterator();

                     while(var35.hasNext()) {
                        Map<?, ?> m = (Map)var35.next();
                        Map<String, Object> copy = new HashMap(m);
                        newList.add(copy);
                     }

                     boolean replaced = false;
                     Iterator var30 = newList.iterator();

                     while(var30.hasNext()) {
                        Map<String, Object> m = (Map)var30.next();
                        if (m.containsKey(entryKey)) {
                           m.put(entryKey, price);
                           replaced = true;
                           break;
                        }
                     }

                     if (!replaced) {
                        Map<String, Object> toAdd = new HashMap();
                        toAdd.put(entryKey, price);
                        newList.add(toAdd);
                     }

                     catCfg.set(path, newList);
                     this.plugin.getConfigManager().saveCategoriesConfig();
                     this.plugin.reloadPlugin();
                     sender.sendMessage(Utils.formatColors("&aSet &e" + entryKey + " &ain category &e" + category + " &ato &e" + price));
                     return true;
                  } else {
                     sender.sendMessage(Utils.formatColors("&cHold an item to set its price."));
                     return true;
                  }
               } else {
                  sender.sendMessage(Utils.formatColors("&cUnknown category: " + category));
                  return true;
               }
            }
         } else {
            if (sender instanceof Player) {
               this.plugin.getSellGui().open((Player)sender);
            } else {
               sender.sendMessage(Utils.formatColors("&cOnly players may open the sell menu."));
            }

            return true;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      if (!sender.hasPermission("sell.admin")) {
         return Collections.emptyList();
      } else if (args.length == 1) {
         return this.filter(Arrays.asList("reload", "addhanditem", "removeitem", "resetall", "reset", "setlevel", "resetmilestone"), args[0]);
      } else {
         if (args.length == 2) {
            if (args[0].equalsIgnoreCase("addhanditem")) {
               ConfigurationSection cats = this.plugin.getConfigManager().getCategoriesConfig().getConfigurationSection("categories");
               if (cats == null) {
                  return Collections.emptyList();
               }

               return this.filter(new ArrayList(cats.getKeys(false)), args[1]);
            }

            if (args[0].equalsIgnoreCase("removeitem")) {
               ConfigurationSection cats = this.plugin.getConfigManager().getCategoriesConfig().getConfigurationSection("categories");
               if (cats == null) {
                  return Collections.emptyList();
               }

               return this.filter(new ArrayList(cats.getKeys(false)), args[1]);
            }

            if (args[0].equalsIgnoreCase("resetall") || args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("resetmilestone")) {
               List<String> onlineNames = new ArrayList();
               Iterator var6 = Bukkit.getOnlinePlayers().iterator();

               while(var6.hasNext()) {
                  Player p = (Player)var6.next();
                  onlineNames.add(p.getName());
               }

               return this.filter(onlineNames, args[1]);
            }
         }

         if (args.length == 3) {
            if (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("resetmilestone")) {
               List<String> list = new ArrayList<>(this.plugin.categoryItems.keySet());
               if (args[0].equalsIgnoreCase("resetmilestone")) {
                   list.add("all");
               }
               return this.filter(list, args[2]);
            }
            if (args[0].equalsIgnoreCase("removeitem")) {
               String category = args[1].toLowerCase(Locale.ROOT);
               List<Map<?, ?>> rawList = this.plugin.getConfigManager().getCategoriesConfig().getMapList("categories." + category);
               List<String> keys = new ArrayList();
               Iterator var6 = rawList.iterator();

               while(var6.hasNext()) {
                  Map<?, ?> m = (Map)var6.next();
                  Iterator var8 = m.keySet().iterator();

                  while(var8.hasNext()) {
                     Object k = var8.next();
                     if (k != null) {
                        keys.add(String.valueOf(k));
                     }
                  }
               }

               return this.filter(keys, args[2]);
            }
         }

         if (args.length == 4) {
            if (args[0].equalsIgnoreCase("setlevel")) {
               ConfigurationSection levels = this.plugin.getConfigManager().getGuiConfig("progress-menu").getConfigurationSection("levels");
               if (levels != null) {
                  List<String> levelKeys = new ArrayList<>(levels.getKeys(false));
                  List<String> numericLevels = new ArrayList<>();
                  for (String k : levelKeys) {
                     if (k.startsWith("level")) {
                        numericLevels.add(k.substring(5));
                     }
                  }
                  levelKeys.addAll(numericLevels);
                  return this.filter(levelKeys, args[3]);
               }
            }
         }

         return Collections.emptyList();
      }
   }

   private List<String> filter(List<String> options, String prefix) {
      if (prefix.isEmpty()) {
         return options;
      } else {
         String lower = prefix.toLowerCase(Locale.ROOT);
         List<String> out = new ArrayList();
         Iterator var5 = options.iterator();

         while(var5.hasNext()) {
            String o = (String)var5.next();
            if (o.toLowerCase(Locale.ROOT).startsWith(lower)) {
               out.add(o);
            }
         }

         return out;
      }
   }
}
