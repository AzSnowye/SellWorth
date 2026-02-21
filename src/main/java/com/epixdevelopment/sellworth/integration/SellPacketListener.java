package com.epixdevelopment.sellworth.integration;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
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

public class SellPacketListener extends PacketAdapter implements Listener {
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

   public SellPacketListener(Sell plugin) {
      super(plugin, ListenerPriority.NORMAL, new PacketType[]{Server.OPEN_WINDOW, Server.WINDOW_ITEMS, Server.SET_SLOT});
      this.plugin = plugin;
      this.loadConfigData();
      ProtocolManager mgr = ProtocolLibrary.getProtocolManager();
      mgr.addPacketListener(this);
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
   }

   public void onPacketSending(PacketEvent event) {
      PacketContainer packet = event.getPacket();
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (player.getGameMode() == GameMode.CREATIVE) {
         this.stripAll(packet);
      } else {
         int wid;
         boolean allowTopWorth;
         if (packet.getType() == Server.OPEN_WINDOW) {
            wid = (Integer)packet.getIntegers().read(0);
            this.openWindowId.put(uuid, wid);
            String titleJson = ((WrappedChatComponent)packet.getChatComponents().read(0)).getJson().toLowerCase();
            boolean matchesConfigured = this.worthGuiNames.stream()
                .anyMatch(name -> titleJson.contains(name));
            allowTopWorth = matchesConfigured;
            this.worthOpen.put(uuid, allowTopWorth);
            this.containerSize.remove(uuid);
         } else if (!this.displayWorthLore) {
            this.stripAll(packet);
         } else if (!this.plugin.isWorthEnabled(uuid)) {
            this.stripAll(packet);
         } else {
            boolean invWindow;
            int contSlots;
            int i;
            if (packet.getType() == Server.WINDOW_ITEMS) {
               wid = (Integer)packet.getIntegers().read(0);
               invWindow = wid == 0;
               allowTopWorth = (Boolean)this.worthOpen.getOrDefault(uuid, false) && !invWindow;
               List<ItemStack> items = new ArrayList((Collection)packet.getItemListModifier().read(0));
               int total;
               if (invWindow) {
                  for(total = 0; total < items.size(); ++total) {
                     items.set(total, this.applyLore((ItemStack)items.get(total), uuid));
                  }
               } else {
                  total = items.size();
                  int invSlots = 36;
                  contSlots = Math.max(0, total - invSlots);
                  this.containerSize.put(uuid, contSlots);

                  for(i = 0; i < total; ++i) {
                     ItemStack orig = (ItemStack)items.get(i);
                     if (i < contSlots) {
                        items.set(i, allowTopWorth ? this.applyLore(orig, uuid) : this.stripWorthLore(orig));
                     } else {
                        items.set(i, this.applyLore(orig, uuid));
                     }
                  }
               }

               packet.getItemListModifier().write(0, items);
            } else if (packet.getType() == Server.SET_SLOT) {
               wid = (Integer)packet.getIntegers().read(0);
               invWindow = wid == 0;
               allowTopWorth = (Boolean)this.worthOpen.getOrDefault(uuid, false) && !invWindow;
               boolean clickCancelled = (Boolean)this.lastClickCancelled.getOrDefault(uuid, false);
               ItemStack in = (ItemStack)packet.getItemModifier().read(0);
               contSlots = (Integer)packet.getIntegers().read(1);
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

               packet.getItemModifier().write(0, out);
               this.lastClickCancelled.remove(uuid);
            }

         }
      }
   }

   private void stripAll(PacketContainer packet) {
      if (packet.getType() == Server.WINDOW_ITEMS) {
         List<ItemStack> items = new ArrayList((Collection)packet.getItemListModifier().read(0));

         for(int i = 0; i < items.size(); ++i) {
            items.set(i, this.stripWorthLore((ItemStack)items.get(i)));
         }

         packet.getItemListModifier().write(0, items);
      } else if (packet.getType() == Server.SET_SLOT) {
         ItemStack in = (ItemStack)packet.getItemModifier().read(0);
         packet.getItemModifier().write(0, this.stripWorthLore(in));
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
                           spawnerKey = this.plugin.categoryItems.entrySet().stream()
                              .filter(ex -> ex.getValue().contains(item.getType().name()))
                              .map(Entry::getKey)
                              .findFirst()
                              .orElse(null);
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
                              String insideCat = this.plugin.categoryItems.entrySet().stream()
                                  .filter(ex -> ex.getValue().contains(inside.getType().name()))
                                  .map(Entry::getKey)
                                  .findFirst()
                                  .orElse(null);
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
                  String cat = this.plugin.categoryItems.entrySet().stream()
                      .filter(ex -> ex.getValue().contains(item.getType().name()))
                      .map(Entry::getKey)
                      .findFirst()
                      .orElse(null);
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
