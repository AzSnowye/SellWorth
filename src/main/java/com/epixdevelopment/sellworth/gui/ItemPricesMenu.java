package com.epixdevelopment.sellworth.gui;


import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.tracker.ViewTracker;
import com.epixdevelopment.sellworth.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class ItemPricesMenu implements Listener {
   private final Sell plugin;
   private final double defaultValue;
   private List<Entry<ItemStack, Double>> masterEntries;
   private final Set<String> disabledSet;
   private final String titlePrefix;
   private final String titleTemplate;
   private final int rows;
   private final Material prevMat;
   private final int prevSlot;
   private final String prevName;
   private final List<String> prevLore;
   private final Material nextMat;
   private final int nextSlot;
   private final String nextName;
   private final List<String> nextLore;
   private final Material refreshMat;
   private final int refreshSlot;
   private final String refreshName;
   private final List<String> refreshLore;
   private final Material sortMat;
   private final int sortSlot;
   private final String sortName;
   private final String sortNotCurColor;
   private final String sortCurColor;
   private final List<String> sortOptions;
   private final Material filterMat;
   private final int filterSlot;
   private final String filterName;
   private final String filterNotCurColor;
   private final String filterCurColor;
   private final List<String> filterOptions;
   private final String itemDisplayTemplate;
   private final List<String> itemLoreTemplate;
   private final String pageSwitchSoundName;
   private static final Pattern ENCH_PATTERN = Pattern.compile("([a-z0-9_]+?)[_-]?(\\d+)-value");
   private static final Pattern POTION_PATTERN = Pattern.compile("(?:(splash|lingering)_)?(?:(long|strong)_)?([a-z_]+)$");
   private static final Pattern SPAWNER_PATTERN = Pattern.compile("([a-z_]+)_spawner-value");
   private final NamespacedKey PDC_CATEGORY;

   public ItemPricesMenu(Sell plugin) {
      this.plugin = plugin;
      this.defaultValue = plugin.getConfig().getDouble("default-value", 0.1D);
      Bukkit.getPluginManager().registerEvents(this, plugin);
      this.PDC_CATEGORY = new NamespacedKey(plugin, "category");
      this.disabledSet = new HashSet();
      Iterator var2 = plugin.getConfig().getStringList("disabled-items").iterator();

      while(var2.hasNext()) {
         String s = (String)var2.next();
         this.disabledSet.add(s.toUpperCase(Locale.ROOT));
      }

      ConfigurationSection menu = plugin.getConfigManager().getGuiConfig("item-prices-menu");
      this.titleTemplate = menu.getString("title");
      this.titlePrefix = this.titleTemplate.split("%page%", 2)[0];
      this.rows = menu.getInt("rows", 6);
      ConfigurationSection prev = menu.getConfigurationSection("previous");
      this.prevMat = Material.valueOf(prev.getString("previous-page-material").toUpperCase());
      this.prevSlot = prev.getInt("previous-page-slot");
      this.prevName = prev.getString("previous-page-displayname");
      this.prevLore = prev.getStringList("previous-page-lore");
      ConfigurationSection nxt = menu.getConfigurationSection("next");
      this.nextMat = Material.valueOf(nxt.getString("next-page-material").toUpperCase());
      this.nextSlot = nxt.getInt("next-page-slot");
      this.nextName = nxt.getString("next-page-displayname");
      this.nextLore = nxt.getStringList("next-page-lore");
      ConfigurationSection rf = menu.getConfigurationSection("refresh");
      this.refreshMat = Material.valueOf(rf.getString("material").toUpperCase());
      this.refreshSlot = rf.getInt("slot");
      this.refreshName = rf.getString("displayname");
      this.refreshLore = rf.getStringList("lore");
      ConfigurationSection st = menu.getConfigurationSection("sort");
      this.sortMat = Material.valueOf(st.getString("material").toUpperCase());
      this.sortSlot = st.getInt("slot");
      this.sortName = st.getString("displayname");
      this.sortNotCurColor = st.getString("NotCurrentColor");
      this.sortCurColor = st.getString("CurrentColor");
      this.sortOptions = st.getStringList("lore");
      ConfigurationSection fl = menu.getConfigurationSection("filter");
      this.filterMat = Material.valueOf(fl.getString("material").toUpperCase());
      this.filterSlot = fl.getInt("slot");
      this.filterName = fl.getString("displayname");
      this.filterNotCurColor = fl.getString("NotCurrentColor");
      this.filterCurColor = fl.getString("CurrentColor");
      this.filterOptions = fl.getStringList("lore");
      ConfigurationSection items = menu.getConfigurationSection("items");
      this.itemDisplayTemplate = items.getString("displayname");
      this.itemLoreTemplate = items.getStringList("lore");
      String configured = plugin.getConfig().getString("sounds.page-switch", "ITEM_BOOK_PAGE_TURN");
      this.pageSwitchSoundName = configured != null ? configured.toUpperCase(Locale.ROOT) : "ITEM_BOOK_PAGE_TURN";
      this.buildEntries();
   }

   private void buildEntries() {
      List<Entry<ItemStack, Double>> list = new ArrayList();
      Set<String> seen = new HashSet();
      boolean disableAllSpawnEggs = this.disabledSet.contains("SPAWN_EGG");
      Material[] var4 = Material.values();
      int var5 = var4.length;

      double val;
      String var10000;
      String splashOrLingering;
      for(int var6 = 0; var6 < var5; ++var6) {
         Material m = var4[var6];
         if (m.isItem() && m != Material.AIR && !this.disabledSet.contains(m.name()) && (!disableAllSpawnEggs || !m.name().endsWith("_SPAWN_EGG"))) {
            if (m == Material.SPAWNER) {
               double spawnerValue = this.plugin.getPrice("spawner-value");
               ItemStack spawner = new ItemStack(Material.SPAWNER);
               list.add(new SimpleEntry(spawner, spawnerValue));
               seen.add("spawner-value");
            } else {
               var10000 = m.name();
               splashOrLingering = var10000.toLowerCase(Locale.ROOT) + "-value";
               val = this.plugin.getPrice(splashOrLingering);
               list.add(new SimpleEntry(new ItemStack(m), val));
               seen.add(splashOrLingering);
            }
         }
      }

      Iterator var19 = this.plugin.getItemValues().keySet().iterator();

      String rawKey;
      Matcher sm;
      String entityName;
      while(var19.hasNext()) {
         rawKey = (String)var19.next();
         if (rawKey.endsWith("-value") && !seen.contains(rawKey)) {
            sm = ENCH_PATTERN.matcher(rawKey);
            if (sm.matches()) {
               entityName = sm.group(1);
               int lvl = Integer.parseInt(sm.group(2));
               val = this.plugin.getPrice(rawKey);
               final String finalEntityName = entityName;
               Enchantment ench = Arrays.stream(Enchantment.values())
                   .filter(e -> e.getKey().getKey().equalsIgnoreCase(finalEntityName))
                   .findFirst()
                   .orElse(null);
               if (ench != null) {
                  ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                  EnchantmentStorageMeta esm = (EnchantmentStorageMeta)book.getItemMeta();
                  if (esm != null) {
                     esm.addStoredEnchant(ench, lvl, true);
                     esm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "book");
                     book.setItemMeta(esm);
                     list.add(new SimpleEntry(book, val));
                     seen.add(rawKey);
                  }
               }
            }
         }
      }

      var19 = this.plugin.getItemValues().keySet().iterator();

      while(var19.hasNext()) {
         rawKey = (String)var19.next();
         if (rawKey.endsWith("-value") && !seen.contains(rawKey)) {
            String baseKey = rawKey.substring(0, rawKey.length() - 6);
            Matcher pm = POTION_PATTERN.matcher(baseKey);
            if (pm.matches()) {
               splashOrLingering = pm.group(1);
               String longOrStrong = pm.group(2);
               String effectName = pm.group(3);
               if (this.looksLikePotion(effectName)) {
                  boolean extended = "long".equalsIgnoreCase(longOrStrong);
                  boolean upgraded = "strong".equalsIgnoreCase(longOrStrong);
                  Material potMat = Material.POTION;
                  if ("splash".equalsIgnoreCase(splashOrLingering)) {
                     potMat = Material.SPLASH_POTION;
                  } else if ("lingering".equalsIgnoreCase(splashOrLingering)) {
                     potMat = Material.LINGERING_POTION;
                  }

                  ItemStack pot = new ItemStack(potMat);
                  PotionMeta pmMeta = (PotionMeta)pot.getItemMeta();
                  if (pmMeta != null) {
                     PotionType type = this.resolvePotionType(effectName);
                     if (type != null) {
                        pmMeta.setBasePotionData(new PotionData(type, extended, upgraded));
                     } else {
                        pmMeta.setBasePotionData(new PotionData(PotionType.AWKWARD, false, false));
                        var10000 = this.prettify(effectName);
                        String pretty = var10000 + " Potion";
                        pmMeta.setDisplayName(pretty);
                     }

                     pmMeta.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "brewing_stand");
                     pot.setItemMeta(pmMeta);
                     list.add(new SimpleEntry(pot, this.plugin.getPrice(rawKey)));
                     seen.add(rawKey);
                  }
               }
            }
         }
      }

      var19 = this.plugin.getItemValues().keySet().iterator();

      while(true) {
         do {
            do {
               do {
                  if (!var19.hasNext()) {
                     this.masterEntries = list;
                     return;
                  }

                  rawKey = (String)var19.next();
               } while(!rawKey.endsWith("-value"));
            } while(seen.contains(rawKey));

            sm = SPAWNER_PATTERN.matcher(rawKey);
         } while(!sm.matches());

         entityName = sm.group(1);

         EntityType et;
         try {
            et = EntityType.valueOf(entityName.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var18) {
            continue;
         }

         val = this.plugin.getPrice(rawKey);
         ItemStack sp = new ItemStack(Material.SPAWNER);
         BlockStateMeta bsm = (BlockStateMeta)sp.getItemMeta();
         if (bsm != null) {
            BlockState var35 = bsm.getBlockState();
            if (var35 instanceof CreatureSpawner) {
               CreatureSpawner cs = (CreatureSpawner)var35;
               cs.setSpawnedType(et);
               bsm.setBlockState(cs);
               sp.setItemMeta(bsm);
               list.add(new SimpleEntry(sp, val));
               seen.add(rawKey);
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent e) {
      if (e.getWhoClicked() instanceof Player) {
         Inventory top = e.getView().getTopInventory();
         String title = e.getView().getTitle();
         if (title.startsWith(this.titlePrefix)) {
            int slot = e.getRawSlot();
            if (slot >= 0 && slot < top.getSize()) {
               e.setCancelled(true);
               Player player = (Player)e.getWhoClicked();
               ViewTracker vt = this.plugin.getViewTracker();
               int currentPage = vt.getPage(player.getUniqueId());
               if (slot == this.filterSlot) {
                  String cur = vt.getFilter(player.getUniqueId());
                  int idx = this.filterOptions.indexOf(cur == null ? "all" : cur);
                  idx = (idx + 1) % this.filterOptions.size();
                  String nextCat = (String)this.filterOptions.get(idx);
                  vt.setFilter(player.getUniqueId(), nextCat.equalsIgnoreCase("all") ? null : nextCat);
                  this.open(player, currentPage);
               } else if (slot == this.prevSlot) {
                  this.open(player, currentPage - 1);
               } else if (slot == this.nextSlot) {
                  this.open(player, currentPage + 1);
               } else if (slot == this.refreshSlot) {
                  this.open(player, currentPage);
               }

            }
         }
      }
   }

   public void open(Player player, int reqPage) {
      ViewTracker vt = this.plugin.getViewTracker();
      String filterCategory = vt.getFilter(player.getUniqueId());
      List<Entry<ItemStack, Double>> sorted = new ArrayList(this.masterEntries);
      Comparator<Entry<ItemStack, Double>> byName = Comparator.comparing((e) -> {
         ItemMeta m = ((ItemStack)e.getKey()).getItemMeta();
         return m != null && m.hasDisplayName() ? m.getDisplayName() : this.prettify(((ItemStack)e.getKey()).getType());
      }, String.CASE_INSENSITIVE_ORDER);

      switch(vt.getOrder(player.getUniqueId())) {
      case HIGH_TO_LOW:
         sorted.sort(Comparator.comparingDouble((Entry<ItemStack, Double> e) -> e.getValue()).reversed().thenComparing(byName));
         break;
      case LOW_TO_HIGH:
         sorted.sort(Comparator.comparingDouble((Entry<ItemStack, Double> e) -> e.getValue()).thenComparing(byName));
         break;
      case NAME:
         sorted.sort(byName.thenComparingDouble(Entry::getValue));
         break;
      }

      if (filterCategory != null && !filterCategory.equalsIgnoreCase("all")) {
         List<String> allowed = (List)this.plugin.categoryItems.getOrDefault(filterCategory, Collections.emptyList());
         sorted.removeIf((e) -> {
            return !this.isAllowedInCategory((ItemStack)e.getKey(), filterCategory, allowed);
         });
      }

      int size = this.rows * 9;
      int per = size - 9;
      int maxPage = Math.max(1, (int)Math.ceil((double)sorted.size() / (double)per));
      int page = Math.min(maxPage, Math.max(1, reqPage));
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, size, Utils.formatColors(this.titleTemplate.replace("%page%", String.valueOf(page))));
      int start = (page - 1) * per;
      int end = Math.min(start + per, sorted.size());

      ItemStack is;
      ItemMeta m;
      Iterator var33;
      String line;
      for(int i = start; i < end; ++i) {
         Entry<ItemStack, Double> ent = (Entry)sorted.get(i);
         is = ((ItemStack)ent.getKey()).clone();
         m = is.getItemMeta();
         if (m != null) {
            String base;
            label138: {
               if (is.getType() == Material.SPAWNER && m instanceof BlockStateMeta) {
                  BlockStateMeta bsm = (BlockStateMeta)m;
                  BlockState var20 = bsm.getBlockState();
                  if (var20 instanceof CreatureSpawner) {
                     CreatureSpawner cs = (CreatureSpawner)var20;
                     EntityType spawned = cs.getSpawnedType();
                     base = spawned != null ? this.prettify(spawned.name() + "_SPAWNER") : this.prettify("SPAWNER");
                     break label138;
                  }
               }

               base = m.hasDisplayName() ? m.getDisplayName() : this.prettify(is.getType());
            }

            m.setDisplayName(Utils.formatColors(this.itemDisplayTemplate.replace("%ItemName%", base).replace("%amount%", Utils.abbreviateNumber((Double)ent.getValue()))));
            List<String> lore = new ArrayList();
            var33 = this.itemLoreTemplate.iterator();

            while(var33.hasNext()) {
               line = (String)var33.next();
               lore.add(Utils.formatColors(line.replace("%ItemName%", base).replace("%amount%", Utils.abbreviateNumber((Double)ent.getValue()))));
            }

            m.setLore(lore);
            is.setItemMeta(m);
            inv.setItem(i - start, is);
         }
      }

      this.place(inv, this.prevMat, this.prevSlot, this.prevName, this.prevLore);
      this.place(inv, this.nextMat, this.nextSlot, this.nextName, this.nextLore);
      this.place(inv, this.refreshMat, this.refreshSlot, this.refreshName, this.refreshLore);
      ItemStack sortItem = new ItemStack(this.sortMat);
      ItemMeta sm = sortItem.getItemMeta();
      String curFilt;
      if (sm != null) {
         sm.setDisplayName(Utils.formatColors(this.sortName));
         List<String> lore = new ArrayList();
         ViewTracker.SortOrder cur = vt.getOrder(player.getUniqueId());

         for(int i = 0; i < this.sortOptions.size(); ++i) {
            curFilt = (String)this.sortOptions.get(i);
            String col = (i != 0 || cur != ViewTracker.SortOrder.HIGH_TO_LOW) && (i != 1 || cur != ViewTracker.SortOrder.LOW_TO_HIGH) && (i != 2 || cur != ViewTracker.SortOrder.NAME) ? this.sortNotCurColor : this.sortCurColor;
            lore.add(Utils.formatColors(col + "• " + curFilt));
         }

         sm.setLore(lore);
         sortItem.setItemMeta(sm);
         inv.setItem(this.sortSlot, sortItem);
      }

      is = new ItemStack(this.filterMat);
      m = is.getItemMeta();
      if (m != null) {
         m.setDisplayName(Utils.formatColors(this.filterName));
         List<String> lore = new ArrayList();
         curFilt = filterCategory == null ? "all" : filterCategory;
         var33 = this.filterOptions.iterator();

         while(var33.hasNext()) {
            line = (String)var33.next();
            String col = line.equalsIgnoreCase(curFilt) ? this.filterCurColor : this.filterNotCurColor;
            lore.add(Utils.formatColors(col + "• " + this.prettify(line)));
         }

         m.setLore(lore);
         is.setItemMeta(m);
         inv.setItem(this.filterSlot, is);
      }

      player.openInventory(inv);
      vt.setPage(player.getUniqueId(), page);

      Sound sound = this.plugin.resolveSound(this.pageSwitchSoundName, Sound.ITEM_BOOK_PAGE_TURN);
      player.playSound(player.getLocation(), sound, 1.0F, 1.0F);

   }

   private void place(Inventory inv, Material mat, int slot, String name, List<String> lore) {
      ItemStack b = new ItemStack(mat);
      ItemMeta m = b.getItemMeta();
      if (m != null) {
         m.setDisplayName(Utils.formatColors(name));
         m.setLore(Utils.formatColors(lore));
         b.setItemMeta(m);
         inv.setItem(slot, b);
      }

   }

   private String prettify(Material m) {
      return this.prettify(m.name());
   }

   private String prettify(String raw) {
      String[] parts = raw.toLowerCase(Locale.ROOT).split("_");

      for(int i = 0; i < parts.length; ++i) {
         char var10002 = Character.toUpperCase(parts[i].charAt(0));
         parts[i] = var10002 + parts[i].substring(1);
      }

      return String.join(" ", parts);
   }

   private boolean isAllowedInCategory(ItemStack is, String filterCategory, List<String> allowed) {
      String type = is.getType().name();
      ItemMeta meta = is.getItemMeta();
      PersistentDataContainer pdc = meta != null ? meta.getPersistentDataContainer() : null;
      String catTag = pdc != null && pdc.has(this.PDC_CATEGORY, PersistentDataType.STRING) ? (String)pdc.get(this.PDC_CATEGORY, PersistentDataType.STRING) : null;
      if ("book".equalsIgnoreCase(filterCategory)) {
         if ("book".equalsIgnoreCase(catTag)) {
            return true;
         }

         if (meta instanceof EnchantmentStorageMeta) {
            return true;
         }

         if (type.endsWith("_BOOK")) {
            return true;
         }
      }

      if ("brewing_stand".equalsIgnoreCase(filterCategory)) {
         if ("brewing_stand".equalsIgnoreCase(catTag)) {
            return true;
         }

         if (type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION")) {
            return true;
         }
      }

      if (allowed != null && !allowed.isEmpty()) {
         if (allowed.contains(type)) {
            return true;
         }

         if ((this.containsIgnoreCase(allowed, "BOOKS") || this.containsIgnoreCase(allowed, "ANY_BOOK")) && (type.endsWith("_BOOK") || meta instanceof EnchantmentStorageMeta)) {
            return true;
         }

         if ((this.containsIgnoreCase(allowed, "POTIONS") || this.containsIgnoreCase(allowed, "ANY_POTION")) && (type.equals("POTION") || type.equals("SPLASH_POTION") || type.equals("LINGERING_POTION"))) {
            return true;
         }
      }

      return false;
   }

   private boolean containsIgnoreCase(List<String> list, String token) {
      Iterator var3 = list.iterator();

      String s;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         s = (String)var3.next();
      } while(!s.equalsIgnoreCase(token));

      return true;
   }

   private boolean looksLikePotion(String effectName) {
      return effectName.contains("potion") || effectName.contains("vision") || effectName.contains("invisibility") || effectName.contains("leaping") || effectName.contains("jump") || effectName.contains("fire_resistance") || effectName.contains("swiftness") || effectName.contains("speed") || effectName.contains("slowness") || effectName.contains("water_breathing") || effectName.contains("healing") || effectName.contains("harming") || effectName.contains("poison") || effectName.contains("regeneration") || effectName.contains("strength") || effectName.contains("weakness") || effectName.contains("turtle_master") || effectName.contains("slow_falling") || effectName.contains("mundane") || effectName.contains("thick") || effectName.contains("awkward") || effectName.contains("water") || effectName.contains("wind_charged") || effectName.contains("weaving") || effectName.contains("oozing") || effectName.contains("infested");
   }

   private PotionType resolvePotionType(String effectName) {
      String n = effectName.toLowerCase(Locale.ROOT);
      byte var4 = -1;
      switch(n.hashCode()) {
      case -982431341:
         if (n.equals("potion")) {
            var4 = 5;
         }
         break;
      case 50716538:
         if (n.equals("leaping")) {
            var4 = 0;
         }
         break;
      case 112903447:
         if (n.equals("water")) {
            var4 = 4;
         }
         break;
      case 696762990:
         if (n.equals("harming")) {
            var4 = 3;
         }
         break;
      case 795549946:
         if (n.equals("healing")) {
            var4 = 2;
         }
         break;
      case 1266452202:
         if (n.equals("swiftness")) {
            var4 = 1;
         }
      }

      switch(var4) {
      case 0:
         return PotionType.LEAPING;
      case 1:
         return PotionType.SWIFTNESS;
      case 2:
         return PotionType.HEALING;
      case 3:
         return PotionType.HARMING;
      case 4:
         return PotionType.WATER;
      case 5:
         return PotionType.WATER;
      default:
         String enumName = n.toUpperCase(Locale.ROOT);
         enumName = enumName.replace('-', '_');

         try {
            return PotionType.valueOf(enumName);
         } catch (IllegalArgumentException var7) {
            return null;
         }
      }
   }
}
