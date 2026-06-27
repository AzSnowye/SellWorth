package com.epixdevelopment.sellworth.integration;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.util.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;

public class SellPacketListener implements PacketListener, Listener {
   private final Sell plugin;
   private final Map<Material, Double> values = new HashMap();
   private List<String> loreTemplate;
   private List<String> lorePlainPrefixes;
   private double defaultValue;
   private List<String> worthGuiNames;
   private boolean displayWorthLore;
   private Set<String> disabledItems;
   private final Map<UUID, Integer> openWindowId = new HashMap();
   private final Map<UUID, Boolean> worthOpen = new HashMap();
   private final Map<UUID, Boolean> lastClickCancelled = new HashMap();
   private final Map<UUID, Integer> containerSize = new HashMap();
   private final Map<String, String> itemCategoryCache = new HashMap();

   public SellPacketListener(Sell plugin) {
      this.plugin = plugin;
      this.loadConfigData();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void loadConfigData() {
      this.values.clear();
      this.plugin.getItemValues().forEach((key, price) -> {
         String matName = key.replaceFirst("(?i)-value$", "").toUpperCase(Locale.ROOT);

         try {
            this.values.put(Material.valueOf(matName), price);
         } catch (IllegalArgumentException var5) {
         }

      });
      this.defaultValue = this.plugin.getConfig().getDouble("default-value", 0.1D);
      this.loreTemplate = this.plugin.getConfig().getStringList("lore");
      this.lorePlainPrefixes = (List)this.loreTemplate.stream().map((line) -> {
         return ChatColor.stripColor(Utils.formatColors(line.replace("%amount%", ""))).toLowerCase(Locale.ROOT).trim();
      }).collect(Collectors.toList());
      this.displayWorthLore = this.plugin.getConfig().getBoolean("display-worth-lore", true);
      this.worthGuiNames = (List)this.plugin.getConfig().getStringList("worth-gui-names").stream().map(String::toLowerCase).collect(Collectors.toList());
      this.disabledItems = (Set)this.plugin.getConfig().getStringList("disabled-items").stream().map((s) -> {
         return s.toUpperCase(Locale.ROOT);
      }).collect(Collectors.toSet());
      this.itemCategoryCache.clear();
      for (Entry<String, List<String>> entry : this.plugin.categoryItems.entrySet()) {
         for (String itemName : entry.getValue()) {
            this.itemCategoryCache.put(itemName, entry.getKey());
         }
      }
   }

   @Override
   public void onPacketSend(PacketSendEvent event) {
      Player player = (Player) event.getPlayer();
      if (player == null) {
         return;
      }
      UUID uuid;
      try {
         uuid = player.getUniqueId();
      } catch (UnsupportedOperationException e) {
         return;
      }
      if (player.getGameMode() == GameMode.CREATIVE) {
         this.stripAll(event);
      } else {
         int wid;
         boolean allowTopWorth;
         if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow wrapper = new WrapperPlayServerOpenWindow(event);
            wid = wrapper.getContainerId();
            this.openWindowId.put(uuid, wid);
            String titleJson = GsonComponentSerializer.gson().serialize(wrapper.getTitle()).toLowerCase();
            boolean matchesConfigured = this.worthGuiNames.stream()
                .anyMatch(name -> titleJson.contains(name));
            allowTopWorth = matchesConfigured;
            this.worthOpen.put(uuid, allowTopWorth);
            this.containerSize.remove(uuid);
         } else if (!this.displayWorthLore) {
            this.stripAll(event);
         } else if (!this.plugin.isWorthEnabled(uuid)) {
            this.stripAll(event);
         } else {
            boolean invWindow;
            int contSlots;
            int i;
            if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
               WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
               wid = wrapper.getWindowId();
               invWindow = wid == 0;
               allowTopWorth = (Boolean)this.worthOpen.getOrDefault(uuid, false) && !invWindow;
               List<com.github.retrooper.packetevents.protocol.item.ItemStack> peItems = wrapper.getItems();
               List<com.github.retrooper.packetevents.protocol.item.ItemStack> newPeItems = new ArrayList<>();
               int total = peItems.size();
               int invSlots = 36;
               contSlots = Math.max(0, total - invSlots);
               this.containerSize.put(uuid, contSlots);

               for(i = 0; i < total; ++i) {
                  com.github.retrooper.packetevents.protocol.item.ItemStack peItem = peItems.get(i);
                  ItemStack orig = SpigotConversionUtil.toBukkitItemStack(peItem);
                  ItemStack modified;
                  if (i < contSlots) {
                     modified = allowTopWorth ? this.applyLore(orig, uuid) : this.stripWorthLore(orig);
                  } else {
                     modified = this.applyLore(orig, uuid);
                  }
                  newPeItems.add(SpigotConversionUtil.fromBukkitItemStack(modified));
               }

               wrapper.setItems(newPeItems);
            } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
               WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
               wid = wrapper.getWindowId();
               invWindow = wid == 0;
               allowTopWorth = (Boolean)this.worthOpen.getOrDefault(uuid, false) && !invWindow;
               boolean clickCancelled = (Boolean)this.lastClickCancelled.getOrDefault(uuid, false);
               com.github.retrooper.packetevents.protocol.item.ItemStack peItem = wrapper.getItem();
               ItemStack in = SpigotConversionUtil.toBukkitItemStack(peItem);
               contSlots = wrapper.getSlot();
               ItemStack out;
               if (invWindow) {
                  out = this.applyLore(in, uuid);
               } else {
                  i = (Integer)this.containerSize.getOrDefault(uuid, 0);
                  if (contSlots >= i) {
                     out = this.applyLore(in, uuid);
                  } else if (clickCancelled) {
                     out = this.stripWorthLore(in);
                  } else {
                     out = allowTopWorth ? this.applyLore(in, uuid) : this.stripWorthLore(in);
                  }
               }

               wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(out));
               this.lastClickCancelled.remove(uuid);
            }

         }
      }
   }

   private void stripAll(PacketSendEvent event) {
      if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
         WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
         List<com.github.retrooper.packetevents.protocol.item.ItemStack> peItems = wrapper.getItems();
         List<com.github.retrooper.packetevents.protocol.item.ItemStack> newPeItems = new ArrayList<>();
         for (com.github.retrooper.packetevents.protocol.item.ItemStack peItem : peItems) {
            ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
            newPeItems.add(SpigotConversionUtil.fromBukkitItemStack(this.stripWorthLore(bukkitItem)));
         }
         wrapper.setItems(newPeItems);
      } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
         WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
         ItemStack in = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
         wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(this.stripWorthLore(in)));
      }
   }

   public ItemStack stripWorthLore(ItemStack original) {
      if (original == null) {
         return null;
      } else {
         ItemStack item = original.clone();
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasLore()) {
            List<String> filtered = new ArrayList();
            Iterator var5 = meta.getLore().iterator();

            while(var5.hasNext()) {
               String line = (String)var5.next();
               String plain = ChatColor.stripColor(line).toLowerCase(Locale.ROOT).trim();
               if (this.lorePlainPrefixes.stream().noneMatch(prefix -> plain.startsWith(prefix))) {
                  filtered.add(line);
               }
            }

            meta.setLore(filtered.isEmpty() ? null : filtered);
            item.setItemMeta(meta);
            return item;
         } else {
            return item;
         }
      }
   }

   public ItemStack applyLore(ItemStack original, UUID playerId) {
      if (original != null && original.getType() != Material.AIR) {
         if (!this.displayWorthLore) {
            return original;
         } else if (!this.plugin.isWorthEnabled(playerId)) {
            return original;
         } else if (this.disabledItems.contains(original.getType().name())) {
            return original;
         } else {
            com.epixdevelopment.sellworth.integration.EGensHook egensHook = this.plugin.getEGensHook();
            if (egensHook != null && egensHook.isEnabled()) {
               if (egensHook.isGeneratorItem(original)) {
                  return original;
               }
               if (egensHook.isEgensDrop(original)) {
                  Double egensPrice = egensHook.getPrice(original);
                  if (egensPrice != null) {
                     double egensTotal = egensPrice * original.getAmount();
                     ItemStack item = original.clone();
                     ItemMeta meta = item.getItemMeta();
                     if (meta == null) return original;
                     String display = Utils.abbreviateNumber(egensTotal);
                     List<String> newLines = (List<String>) this.loreTemplate.stream().map((line) -> {
                        return Utils.formatColors(line.replace("%amount%", display));
                     }).collect(Collectors.toList());
                     List<String> existing = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                     existing.removeIf((line) -> {
                        String plain = ChatColor.stripColor(line).toLowerCase(Locale.ROOT).trim();
                        return this.lorePlainPrefixes.stream().anyMatch(prefix -> plain.startsWith(prefix));
                     });
                     for (String nl : newLines) {
                        if (!existing.contains(nl)) existing.add(nl);
                     }
                     meta.setLore(existing.isEmpty() ? null : existing);
                     item.setItemMeta(meta);
                     return item;
                  }
               }
            }
            // Check CustomFishing first — if this is a CF fish, use its market price
            org.bukkit.entity.Player cfPlayer = this.plugin.getServer().getPlayer(playerId);
            com.epixdevelopment.sellworth.integration.CustomFishingHook cfHook = this.plugin.getCustomFishingHook();
            if (cfPlayer != null && cfHook != null && cfHook.isEnabled() && cfHook.isCustomFishingItem(original)) {
               Double cfPrice = cfHook.getPrice(cfPlayer, original);
               if (cfPrice != null) {
                  double cfTotal = cfPrice * original.getAmount();
                  ItemStack item = original.clone();
                  ItemMeta meta = item.getItemMeta();
                  if (meta == null) return original;
                  String display = Utils.abbreviateNumber(cfTotal);
                  List<String> newLines = (List<String>) this.loreTemplate.stream().map((line) -> {
                     return Utils.formatColors(line.replace("%amount%", display));
                  }).collect(Collectors.toList());
                  List<String> existing = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                  existing.removeIf((line) -> {
                     String plain = ChatColor.stripColor(line).toLowerCase(Locale.ROOT).trim();
                     return this.lorePlainPrefixes.stream().anyMatch(prefix -> plain.startsWith(prefix));
                  });
                  for (String nl : newLines) {
                     if (!existing.contains(nl)) existing.add(nl);
                  }
                  meta.setLore(existing.isEmpty() ? null : existing);
                  item.setItemMeta(meta);
                  return item;
               }
            }

            ItemStack item = original.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
               return original;
            } else {
               double totalValue;
               label153: {
                  totalValue = 0.0D;
                  double insideRaw;
                  String spawnerKey;
                  double raw;
                  if (meta instanceof BlockStateMeta) {
                     BlockStateMeta bsm = (BlockStateMeta)meta;
                     BlockState var9 = bsm.getBlockState();
                     if (var9 instanceof ShulkerBox) {
                        ShulkerBox box = (ShulkerBox)var9;
                        String boxKey = item.getType().name().toLowerCase() + "-value";
                        double boxUnitPrice = this.plugin.getPrice(boxKey);
                        int boxCount = item.getAmount();
                        if (!this.disabledItems.contains(item.getType().name())) {
                           spawnerKey = this.itemCategoryCache.get(item.getType().name());
                           raw = spawnerKey != null ? this.plugin.getSellMultiplier(playerId, spawnerKey) : 1.0D;
                           totalValue += boxUnitPrice * (double)boxCount * raw;
                        }

                        ItemStack[] var38 = box.getInventory().getContents();
                        int var39 = var38.length;
                        int var40 = 0;

                        while(true) {
                           if (var40 >= var39) {
                              break label153;
                           }

                           ItemStack inside = var38[var40];
                           if (inside != null && inside.getType() != Material.AIR && !this.disabledItems.contains(inside.getType().name())) {
                              insideRaw = this.plugin.calculateItemWorth(inside);
                              String insideCat = this.itemCategoryCache.get(inside.getType().name());
                              double insideMult = insideCat != null ? this.plugin.getSellMultiplier(playerId, insideCat) : 1.0D;
                              totalValue += insideRaw * insideMult;
                           }

                           ++var40;
                        }
                     }
                  }

                  double baseVal;
                  if (item.getType() == Material.SPAWNER && meta instanceof BlockStateMeta) {
                     label110: {
                        BlockStateMeta bsm2 = (BlockStateMeta)meta;
                        BlockState var13 = bsm2.getBlockState();
                        if (var13 instanceof CreatureSpawner) {
                           CreatureSpawner cs = (CreatureSpawner)var13;
                           if (cs.getSpawnedType() != null) {
                              String var10000 = cs.getSpawnedType().name();
                              spawnerKey = var10000.toLowerCase(Locale.ROOT) + "_spawner-value";
                              baseVal = this.plugin.getPrice(spawnerKey);
                              break label110;
                           }
                        }

                        baseVal = (Double)this.values.getOrDefault(Material.SPAWNER, this.defaultValue);
                     }
                  } else {
                     String pKey = this.getPotionKey(item);
                     baseVal = pKey != null ? this.plugin.getPrice(pKey + "-value") : (Double)this.values.getOrDefault(item.getType(), this.defaultValue);
                  }

                  double enchVal = 0.0D;
                  Sell var10001;
                  String var10002;
                  if (meta instanceof EnchantmentStorageMeta) {
                     EnchantmentStorageMeta esm = (EnchantmentStorageMeta)meta;

                     Entry e;
                     for(Iterator var14 = esm.getStoredEnchants().entrySet().iterator(); var14.hasNext(); enchVal += var10001.getPrice(var10002 + String.valueOf(e.getValue()) + "-value")) {
                        e = (Entry)var14.next();
                        var10001 = this.plugin;
                        var10002 = ((Enchantment)e.getKey()).getKey().getKey().toLowerCase(Locale.ROOT);
                     }
                  }

                  Entry e;
                  for(Iterator var34 = meta.getEnchants().entrySet().iterator(); var34.hasNext(); enchVal += var10001.getPrice(var10002 + String.valueOf(e.getValue()) + "-value")) {
                     e = (Entry)var34.next();
                     var10001 = this.plugin;
                     var10002 = ((Enchantment)e.getKey()).getKey().getKey().toLowerCase(Locale.ROOT);
                  }

                  int amt = item.getAmount();
                  raw = (baseVal + enchVal) * (double)amt;
                  String cat = this.itemCategoryCache.get(item.getType().name());
                  insideRaw = cat != null ? this.plugin.getSellMultiplier(playerId, cat) : 1.0D;
                  totalValue = raw * insideRaw;
               }

               String display = Utils.abbreviateNumber(totalValue);
               List<String> newLines = (List)this.loreTemplate.stream().map((line) -> {
                  return Utils.formatColors(line.replace("%amount%", display));
               }).collect(Collectors.toList());
               List<String> existing = meta.hasLore() ? new ArrayList(meta.getLore()) : new ArrayList();
               existing.removeIf((line) -> {
                  String plain = ChatColor.stripColor(line).toLowerCase(Locale.ROOT).trim();
                  return this.lorePlainPrefixes.stream().anyMatch(prefix -> plain.startsWith(prefix));
               });
               Iterator var25 = newLines.iterator();

               while(var25.hasNext()) {
                  String nl = (String)var25.next();
                  if (!existing.contains(nl)) {
                     existing.add(nl);
                  }
               }

               meta.setLore(existing.isEmpty() ? null : existing);
               item.setItemMeta(meta);
               return item;
            }
         }
      } else {
         return original;
      }
   }

   private String getPotionKey(ItemStack item) {
      ItemMeta var3 = item.getItemMeta();
      if (var3 instanceof PotionMeta) {
         PotionMeta pm = (PotionMeta)var3;
         PotionData data = pm.getBasePotionData();
         if (data == null) {
            return null;
         } else {
            String base = data.getType().name().toLowerCase(Locale.ROOT);
            if (data.isExtended()) {
               base = "long_" + base;
            }

            if (data.isUpgraded()) {
               base = "strong_" + base;
            }

            if (item.getType() == Material.SPLASH_POTION) {
               base = "splash_" + base;
            } else if (item.getType() == Material.LINGERING_POTION) {
               base = "lingering_" + base;
            }

            return base;
         }
      } else {
         return null;
      }
   }

   public void reloadConfigData() {
      this.loadConfigData();
   }
}
