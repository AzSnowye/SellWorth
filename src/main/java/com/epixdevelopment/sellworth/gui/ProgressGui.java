package com.epixdevelopment.sellworth.gui;


import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.util.Utils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ProgressGui {
   private final Sell plugin;
   private String defaultTitle;
   private final Map<String, String> customTitles = new HashMap();
   private int size;
   private String loadingColor;
   private String completeLoadingColor;
   private String barSymbol;
   private int barLength;
   private Material incompleteMat;
   private String incompleteName;
   private List<String> incompleteLore;
   private Material workingMat;
   private String workingName;
   private List<String> workingLore;
   private Material completeMat;
   private String completeName;
   private List<String> completeLore;
   private boolean fillerEnabled;
   private Material fillerMat;
   private String fillerName;
   private List<String> fillerLore;
   private boolean backEnabled;
   private int backSlot;
   private Material backMat;
   private String backName;
   private List<String> backLore;
   private final List<ProgressGui.Level> levels = new ArrayList();
   public final Map<String, ProgressGui.CategoryIcon> categoryIcons = new HashMap();

   public ProgressGui(Sell plugin) {
      this.plugin = plugin;
      this.loadConfig();
   }

   public void loadConfig() {
      ConfigurationSection cfg = this.plugin.getConfig().getConfigurationSection("progress-menu");
      if (cfg != null) {
         this.defaultTitle = Utils.formatColors(cfg.getString("title", "%item% Progress"));
         this.customTitles.clear();
         ConfigurationSection titles = cfg.getConfigurationSection("titles");
         if (titles != null) {
            Iterator var3 = titles.getKeys(false).iterator();

            while(var3.hasNext()) {
               String cat = (String)var3.next();
               this.customTitles.put(cat.toLowerCase(), Utils.formatColors(titles.getString(cat)));
            }
         }

         this.size = cfg.getInt("rows", 3) * 9;
         this.loadingColor = Utils.formatColors(cfg.getString("loading-color", "&#20f706"));
         this.completeLoadingColor = Utils.formatColors(cfg.getString("complete-loading-color", this.loadingColor));
         this.barLength = cfg.getInt("bar-length", 10);
         this.barSymbol = cfg.getString("bar-symbol", "━");
         ConfigurationSection inc = cfg.getConfigurationSection("incomplete");
         this.incompleteMat = Material.valueOf(inc.getString("material"));
         this.incompleteName = Utils.formatColors(inc.getString("displayname"));
         this.incompleteLore = Utils.formatColors(inc.getStringList("lore"));
         ConfigurationSection wk = cfg.getConfigurationSection("working");
         this.workingMat = Material.valueOf(wk.getString("material"));
         this.workingName = Utils.formatColors(wk.getString("displayname"));
         this.workingLore = Utils.formatColors(wk.getStringList("lore"));
         ConfigurationSection com = cfg.getConfigurationSection("complete");
         this.completeMat = Material.valueOf(com.getString("material"));
         this.completeName = Utils.formatColors(com.getString("displayname"));
         this.completeLore = Utils.formatColors(com.getStringList("lore"));
         ConfigurationSection fill = cfg.getConfigurationSection("filler");
         this.fillerEnabled = fill.getBoolean("enabled", false);
         this.fillerMat = Material.valueOf(fill.getString("material"));
         this.fillerName = Utils.formatColors(fill.getString("displayname"));
         this.fillerLore = Utils.formatColors(fill.getStringList("lore"));
         ConfigurationSection back = cfg.getConfigurationSection("back-button");
         this.backEnabled = back.getBoolean("enabled", false);
         this.backSlot = back.getInt("slot", -1);
         this.backMat = Material.valueOf(back.getString("material"));
         this.backName = Utils.formatColors(back.getString("displayname"));
         this.backLore = Utils.formatColors(back.getStringList("lore"));
         this.levels.clear();
         ConfigurationSection lvlSec = cfg.getConfigurationSection("levels");
         if (lvlSec != null) {
            Iterator var9 = lvlSec.getKeys(false).iterator();

            while(var9.hasNext()) {
               String key = (String)var9.next();
               ConfigurationSection ls = lvlSec.getConfigurationSection(key);
               long amount = ls.getLong("amountNeeded");
               double multi = ls.getDouble("multi");
               int slot = ls.getInt("slot");
               this.levels.add(new ProgressGui.Level(amount, multi, slot));
            }
         }

         this.levels.sort(Comparator.comparingLong((l) -> {
            return l.amountNeeded;
         }));
         this.categoryIcons.clear();
         ConfigurationSection cats = cfg.getConfigurationSection("categories");
         if (cats != null) {
            Iterator var20 = cats.getKeys(false).iterator();

            while(var20.hasNext()) {
               String cat = (String)var20.next();
               ConfigurationSection ico = cats.getConfigurationSection(cat + ".icon");
               if (ico != null) {
                  Material mat = Material.valueOf(ico.getString("material"));
                  int slot = ico.getInt("slot");
                  String dn = Utils.formatColors(ico.getString("displayname"));
                  List<String> lore = Utils.formatColors(ico.getStringList("lore"));
                  this.categoryIcons.put(cat.toLowerCase(), new ProgressGui.CategoryIcon(mat, slot, dn, lore));
               }
            }
         }

      }
   }

   public Inventory open(Player p, String categoryKey) {
      String key = categoryKey.toLowerCase();
      String raw = (String)this.customTitles.getOrDefault(key, this.defaultTitle);
      String title = raw.replace("%item%", this.capitalize(key));
      Inventory inv = Bukkit.createInventory(new GuiHolder(key, -1), this.size, title);
      double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key);

      int workingIndex;
      for(workingIndex = 0; workingIndex < this.levels.size() && sold >= (double)((ProgressGui.Level)this.levels.get(workingIndex)).amountNeeded; ++workingIndex) {
      }

      for(int i = 0; i < this.levels.size(); ++i) {
         ProgressGui.Level lvl = (ProgressGui.Level)this.levels.get(i);
         boolean isComplete = i < workingIndex;
         boolean isWorking = i == workingIndex;
         Material mat = isComplete ? this.completeMat : (isWorking ? this.workingMat : this.incompleteMat);
         ItemStack pane = new ItemStack(mat);
         ItemMeta meta = pane.getItemMeta();
         meta.setDisplayName(isComplete ? this.completeName : (isWorking ? this.workingName : this.incompleteName));
         double progress = Math.min(sold, (double)lvl.amountNeeded);
         double frac = lvl.amountNeeded > 0L ? progress / (double)lvl.amountNeeded : 0.0D;
         String needed = Utils.abbreviateNumber((double)lvl.amountNeeded);
         String done = Utils.abbreviateNumber(progress);
         String amtStr = isComplete ? needed + "/" + needed : done + "/" + needed;
         String var10000;
         String bar;
         if (isComplete) {
            var10000 = this.completeLoadingColor;
            bar = var10000 + this.barSymbol.repeat(this.barLength);
         } else if (isWorking) {
            int filled = (int)Math.round(frac * (double)this.barLength);
            var10000 = this.loadingColor;
            bar = var10000 + this.barSymbol.repeat(filled) + "&f" + this.barSymbol.repeat(this.barLength - filled);
         } else {
            bar = "&f" + this.barSymbol.repeat(this.barLength);
         }

         List<String> template = isComplete ? this.completeLore : (isWorking ? this.workingLore : this.incompleteLore);
         List<String> finalLore = new ArrayList();
         Iterator var27 = template.iterator();

         while(var27.hasNext()) {
            String line = (String)var27.next();
            finalLore.add(Utils.formatColors(line.replace("%loading-bar%", bar).replace("%multi%", String.valueOf(lvl.multi)).replace("%progress%", String.format("%.1f", frac * 100.0D)).replace("%amount-needed%", amtStr)));
         }

         meta.setLore(finalLore);
         pane.setItemMeta(meta);
         inv.setItem(lvl.slot, pane);
      }

      ProgressGui.CategoryIcon ci = (ProgressGui.CategoryIcon)this.categoryIcons.get(key);
      ItemStack b;
      ItemMeta bm;
      if (ci != null) {
         b = new ItemStack(ci.material);
         bm = b.getItemMeta();
         bm.setDisplayName(ci.displayName.replace("%item%", this.capitalize(key)).replace("%sold%", Utils.abbreviateNumber(sold)));
         List<String> lore = new ArrayList();
         Iterator var36 = ci.lore.iterator();

         while(var36.hasNext()) {
            String l = (String)var36.next();
            lore.add(Utils.formatColors(l.replace("%item%", this.capitalize(key)).replace("%sold%", Utils.abbreviateNumber(sold))));
         }

         bm.setLore(lore);
         b.setItemMeta(bm);
         inv.setItem(ci.slot, b);
      }

      if (this.fillerEnabled) {
         for(int i = 0; i < this.size; ++i) {
            if (inv.getItem(i) == null) {
               ItemStack f = new ItemStack(this.fillerMat);
               ItemMeta fm = f.getItemMeta();
               fm.setDisplayName(this.fillerName);
               fm.setLore(this.fillerLore);
               f.setItemMeta(fm);
               inv.setItem(i, f);
            }
         }
      }

      if (this.backEnabled && this.backSlot >= 0 && this.backSlot < this.size) {
         b = new ItemStack(this.backMat);
         bm = b.getItemMeta();
         bm.setDisplayName(this.backName);
         bm.setLore(this.backLore);
         b.setItemMeta(bm);
         inv.setItem(this.backSlot, b);
      }

      p.openInventory(inv);
      return inv;
   }

   private String capitalize(String s) {
      if (s != null && !s.isEmpty()) {
         char var10000 = Character.toUpperCase(s.charAt(0));
         return var10000 + s.substring(1);
      } else {
         return s;
      }
   }

   private static class Level {
      final long amountNeeded;
      final double multi;
      final int slot;

      Level(long a, double m, int s) {
         this.amountNeeded = a;
         this.multi = m;
         this.slot = s;
      }
   }

   public static class CategoryIcon {
      public final Material material;
      public final int slot;
      public final String displayName;
      public final List<String> lore;

      public CategoryIcon(Material m, int s, String dn, List<String> l) {
         this.material = m;
         this.slot = s;
         this.displayName = dn;
         this.lore = l;
      }
   }
}
