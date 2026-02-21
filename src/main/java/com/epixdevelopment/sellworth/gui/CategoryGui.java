package com.epixdevelopment.sellworth.gui;


import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class CategoryGui {
   private final Sell plugin;
   private final String categoryKey;
   private final List<ItemStack> items = new ArrayList();
   private final int rows;
   private final String defaultTitleTpl;
   private final Map<String, String> customTitles = new HashMap();
   private final int prevSlot;
   private final int nextSlot;
   private final int backSlot;
   private final Material prevMat;
   private final Material nextMat;
   private final Material backMat;
   private final String prevName;
   private final String nextName;
   private final String backName;
   private final List<String> prevLore;
   private final List<String> nextLore;
   private final List<String> backLore;
   private final String itemNameTpl;
   private final List<String> itemLoreTpl;
   private final String pageSwitchSoundName;
   private static final Pattern ENCH_PATTERN = Pattern.compile("([a-z0-9_]+?)[_-]?(\\d+)-value");
   private static final Pattern POTION_PATTERN = Pattern.compile("(?:(splash|lingering)_)?(?:(long|strong)_)?([a-z_]+)-value");
   private static final Pattern SPAWNER_PATTERN = Pattern.compile("([a-z_]+)_spawner-value");
   private final NamespacedKey PDC_CATEGORY;
   private static final TreeMap<Integer, String> ROMAN = new TreeMap();

   public CategoryGui(Sell plugin, String categoryKey) {
      this.plugin = plugin;
      this.categoryKey = categoryKey.toLowerCase(Locale.ROOT);
      this.PDC_CATEGORY = new NamespacedKey(plugin, "category");
      ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("category-menu");
      this.rows = cfg.getInt("rows", 6);
      this.defaultTitleTpl = Utils.formatColors(cfg.getString("title", "%item% Items"));
      ConfigurationSection tsec = cfg.getConfigurationSection("titles");
      if (tsec != null) {
         Iterator var5 = tsec.getKeys(false).iterator();

         while(var5.hasNext()) {
            String cat = (String)var5.next();
            this.customTitles.put(cat.toLowerCase(), Utils.formatColors(tsec.getString(cat)));
         }
      }

      this.prevSlot = cfg.getInt("previous-page-slot", 49);
      this.prevMat = Material.matchMaterial(cfg.getString("previous-page.material"));
      this.prevName = Utils.formatColors(cfg.getString("previous-page.displayname", "&aPrevious"));
      this.prevLore = Utils.formatColors(cfg.getStringList("previous-page.lore"));
      this.nextSlot = cfg.getInt("next-page-slot", 51);
      this.nextMat = Material.matchMaterial(cfg.getString("next-page.material"));
      this.nextName = Utils.formatColors(cfg.getString("next-page.displayname", "&aNext"));
      this.nextLore = Utils.formatColors(cfg.getStringList("next-page.lore"));
      this.backSlot = cfg.getInt("back-button.slot", 45);
      this.backMat = Material.matchMaterial(cfg.getString("back-button.material"));
      this.backName = Utils.formatColors(cfg.getString("back-button.displayname", "&cBack"));
      this.backLore = Utils.formatColors(cfg.getStringList("back-button.lore"));
      this.itemNameTpl = Utils.formatColors(cfg.getString("item.displayname", "%item%"));
      this.itemLoreTpl = cfg.getStringList("item.lore");
      String configured = plugin.getConfig().getString("sounds.page-switch", "ITEM_BOOK_PAGE_TURN");
      this.pageSwitchSoundName = configured != null ? configured.toUpperCase(Locale.ROOT) : "ITEM_BOOK_PAGE_TURN";
      Object rawNode = plugin.getConfig().get("categories." + this.categoryKey);
      Iterator var9;
      if (rawNode instanceof ConfigurationSection) {
         ConfigurationSection catSec = (ConfigurationSection)rawNode;
         var9 = catSec.getKeys(false).iterator();

         while(var9.hasNext()) {
            String entryKey = (String)var9.next();
            double price = catSec.getDouble(entryKey, -1.0D);
            if (price < 0.0D) {
               plugin.getLogger().warning("Invalid price for '" + entryKey + "' in categories." + categoryKey);
            } else {
               this.handleEntry(entryKey, price);
            }
         }
      } else if (rawNode instanceof List) {
         List<?> rawList = (List)rawNode;
         var9 = rawList.iterator();

         while(true) {
            Object o;
            do {
               if (!var9.hasNext()) {
                  return;
               }

               o = var9.next();
            } while(!(o instanceof Map));

            Map<?, ?> map0 = (Map)o;
            Iterator var13 = map0.entrySet().iterator();

            while(var13.hasNext()) {
               Entry<String, Object> e = (Entry)var13.next();
               String entryKey = (String)e.getKey();

               double price;
               try {
                  price = Double.parseDouble(e.getValue().toString());
               } catch (NumberFormatException var19) {
                  plugin.getLogger().warning("Invalid price for '" + entryKey + "' in categories." + categoryKey);
                  continue;
               }

               this.handleEntry(entryKey, price);
            }
         }
      } else {
         plugin.getLogger().warning("No entries found for categories." + categoryKey);
      }

   }

   private void handleEntry(String entryKey, double price) {
      Matcher spm = SPAWNER_PATTERN.matcher(entryKey);
      if (spm.matches()) {
         try {
            EntityType type = EntityType.valueOf(spm.group(1).toUpperCase(Locale.ROOT));
            ItemStack stk = new ItemStack(Material.SPAWNER);
            BlockStateMeta bsm = (BlockStateMeta)stk.getItemMeta();
            if (bsm != null) {
               BlockState var27 = bsm.getBlockState();
               if (var27 instanceof CreatureSpawner) {
                  CreatureSpawner cs = (CreatureSpawner)var27;
                  cs.setSpawnedType(type);
                  bsm.setBlockState(cs);
                  stk.setItemMeta(bsm);
               }
            }

            this.applyDisplayAndLore(stk, entryKey, price, (String)null);
            this.items.add(stk);
         } catch (IllegalArgumentException var18) {
            Logger var10000 = this.plugin.getLogger();
            String var10001 = spm.group(1);
            var10000.warning("Unknown spawner entity '" + var10001 + "' in categories." + this.categoryKey);
         }

      } else {
         Matcher enchM = ENCH_PATTERN.matcher(entryKey);
         String matName;
         if (enchM.matches() && entryKey.endsWith("-value")) {
            matName = enchM.group(1);
            int lvl = Integer.parseInt(enchM.group(2));
            String prettyEn = this.prettyName(matName);
            Enchantment found = Arrays.stream(Enchantment.values())
                .filter(e -> e.getKey().getKey().equalsIgnoreCase(matName))
                .findFirst()
                .orElse(null);
            ItemStack stk = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta esm = (EnchantmentStorageMeta)stk.getItemMeta();
            if (esm != null) {
               if (found != null) {
                  esm.addStoredEnchant(found, lvl, true);
               } else {
                  esm.setDisplayName("Enchanted Book (" + prettyEn + " " + this.toRoman(lvl) + ")");
               }

               esm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "book");
               stk.setItemMeta(esm);
            }

            this.applyDisplayAndLore(stk, entryKey, price, found == null ? "Enchanted Book (" + prettyEn + " " + this.toRoman(lvl) + ")" : null);
            this.items.add(stk);
         } else {
            matName = entryKey.replace("-value", "");
            Material mat = Material.matchMaterial(matName);
            if (mat != null) {
               ItemStack stk = new ItemStack(mat);
               this.applyDisplayAndLore(stk, entryKey, price, (String)null);
               this.items.add(stk);
            } else {
               Matcher potM = POTION_PATTERN.matcher(entryKey);
               if (potM.matches()) {
                  String splashOrLingering = potM.group(1);
                  String longOrStrong = potM.group(2);
                  String effectName = potM.group(3);
                  boolean extended = "long".equalsIgnoreCase(longOrStrong);
                  boolean upgraded = "strong".equalsIgnoreCase(longOrStrong);
                  PotionType type = this.resolvePotionType(effectName);
                  if (type == null) {
                     this.plugin.getLogger().warning("Unknown potion effect '" + effectName + "' for key '" + entryKey + "' in categories." + this.categoryKey + " — skipping potion item.");
                  } else {
                     Material potMat = Material.POTION;
                     if ("splash".equalsIgnoreCase(splashOrLingering)) {
                        potMat = Material.SPLASH_POTION;
                     } else if ("lingering".equalsIgnoreCase(splashOrLingering)) {
                        potMat = Material.LINGERING_POTION;
                     }

                     ItemStack stk = new ItemStack(potMat);
                     PotionMeta pm = (PotionMeta)stk.getItemMeta();
                     if (pm != null) {
                        pm.setBasePotionData(new PotionData(type, extended, upgraded));
                        pm.getPersistentDataContainer().set(this.PDC_CATEGORY, PersistentDataType.STRING, "brewing_stand");
                        stk.setItemMeta(pm);
                     }

                     this.applyDisplayAndLore(stk, entryKey, price, (String)null);
                     this.items.add(stk);
                  }
               } else {
                  this.plugin.getLogger().warning("Unrecognized entry key '" + entryKey + "' in categories." + this.categoryKey + " — not a spawner, book, material, or known potion.");
               }
            }
         }
      }
   }

   public void open(Player p, int page) {
      int perPage = (this.rows - 1) * 9;
      int start = page * perPage;
      int end = Math.min(start + perPage, this.items.size());
      String titleTpl = (String)this.customTitles.getOrDefault(this.categoryKey, this.defaultTitleTpl);
      String title = titleTpl.replace("%item%", this.prettyName(this.categoryKey));
      Inventory inv = Bukkit.createInventory(new GuiHolder(this.categoryKey, page), this.rows * 9, title);

      for(int i = start; i < end; ++i) {
         inv.setItem(i - start, (ItemStack)this.items.get(i));
      }

      if (this.prevMat != null) {
         inv.setItem(this.prevSlot, this.buildButton(this.prevMat, this.prevName, this.prevLore));
      }

      if (this.nextMat != null) {
         inv.setItem(this.nextSlot, this.buildButton(this.nextMat, this.nextName, this.nextLore));
      }

      if (this.backMat != null) {
         inv.setItem(this.backSlot, this.buildButton(this.backMat, this.backName, this.backLore));
      }

      p.openInventory(inv);

      try {
         Sound sound = Sound.valueOf(this.pageSwitchSoundName);
         p.playSound(p.getLocation(), sound, 1.0F, 1.0F);
      } catch (IllegalArgumentException var10) {
         this.plugin.getLogger().warning("Invalid sound '" + this.pageSwitchSoundName + "', defaulting to ITEM_BOOK_PAGE_TURN");
         p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
      }

   }

   private void applyDisplayAndLore(ItemStack stk, String entryKey, double price, String customName) {
      ItemMeta im = stk.getItemMeta();
      if (im != null) {
         if (customName != null) {
            im.setDisplayName(customName);
         } else if (!im.hasDisplayName()) {
            String base = this.prettyName(entryKey.replace("-value", ""));
            im.setDisplayName(this.itemNameTpl.replace("%item%", base));
         }

         List<String> lore = new ArrayList();
         Iterator var8 = this.itemLoreTpl.iterator();

         while(var8.hasNext()) {
            String line = (String)var8.next();
            lore.add(Utils.formatColors(line.replace("%item-value%", Utils.abbreviateNumber(price))));
         }

         im.setLore(lore);
         stk.setItemMeta(im);
      }
   }

   private ItemStack buildButton(Material mat, String name, List<String> lore) {
      ItemStack b = new ItemStack(mat);
      ItemMeta m = b.getItemMeta();
      if (m != null) {
         m.setDisplayName(name);
         m.setLore(lore);
         b.setItemMeta(m);
      }

      return b;
   }

   private String prettyName(String raw) {
      String s = raw.replace('-', '_');
      StringBuilder out = new StringBuilder();
      String[] var4 = s.split("_");
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String part = var4[var6];
         if (!part.isEmpty()) {
            String low = part.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(low.charAt(0))).append(low.substring(1)).append(" ");
         }
      }

      return out.toString().trim();
   }

   private PotionType safePotionType(String... names) {
      String[] var2 = names;
      int var3 = names.length;
      int var4 = 0;

      while(var4 < var3) {
         String s = var2[var4];

         try {
            return PotionType.valueOf(s);
         } catch (IllegalArgumentException var7) {
            ++var4;
         }
      }

      return null;
   }

   private PotionType resolvePotionType(String effectName) {
      String n = effectName.toLowerCase(Locale.ROOT).replace('-', '_');
      byte var4 = -1;
      switch(n.hashCode()) {
      case -982431341:
         if (n.equals("potion")) {
            var4 = 7;
         }
         break;
      case 50716538:
         if (n.equals("leaping")) {
            var4 = 0;
         }
         break;
      case 109641799:
         if (n.equals("speed")) {
            var4 = 2;
         }
         break;
      case 112903447:
         if (n.equals("water")) {
            var4 = 6;
         }
         break;
      case 696762990:
         if (n.equals("harming")) {
            var4 = 4;
         }
         break;
      case 795549946:
         if (n.equals("healing")) {
            var4 = 3;
         }
         break;
      case 1032770443:
         if (n.equals("regeneration")) {
            var4 = 5;
         }
         break;
      case 1266452202:
         if (n.equals("swiftness")) {
            var4 = 1;
         }
      }

      switch(var4) {
      case 0:
         return this.safePotionType("JUMP", "LEAPING");
      case 1:
      case 2:
         return this.safePotionType("SPEED", "SWIFTNESS");
      case 3:
         return this.safePotionType("INSTANT_HEAL", "HEALING");
      case 4:
         return this.safePotionType("INSTANT_DAMAGE", "HARMING");
      case 5:
         return this.safePotionType("REGENERATION", "REGEN");
      case 6:
      case 7:
         return this.safePotionType("WATER");
      default:
         PotionType direct = this.safePotionType(n.toUpperCase(Locale.ROOT));
         if (direct != null) {
            return direct;
         } else if (n.equals("regen")) {
            return this.safePotionType("REGENERATION");
         } else if (n.equals("instant_heal")) {
            return this.safePotionType("HEALING");
         } else if (n.equals("instant_damage")) {
            return this.safePotionType("HARMING");
         } else {
            return n.equals("jump_boost") ? this.safePotionType("JUMP", "LEAPING") : null;
         }
      }
   }

   private String toRoman(int number) {
      if (number <= 0) {
         return String.valueOf(number);
      } else {
         int l = (Integer)ROMAN.floorKey(number);
         if (number == l) {
            return (String)ROMAN.get(number);
         } else {
            String var10000 = (String)ROMAN.get(l);
            return var10000 + this.toRoman(number - l);
         }
      }
   }

   static {
      ROMAN.put(1000, "M");
      ROMAN.put(900, "CM");
      ROMAN.put(500, "D");
      ROMAN.put(400, "CD");
      ROMAN.put(100, "C");
      ROMAN.put(90, "XC");
      ROMAN.put(50, "L");
      ROMAN.put(40, "XL");
      ROMAN.put(10, "X");
      ROMAN.put(9, "IX");
      ROMAN.put(5, "V");
      ROMAN.put(4, "IV");
      ROMAN.put(1, "I");
   }
}
