package com.epixdevelopment.sellworth.gui;


import com.epixdevelopment.sellworth.Sell;
import com.epixdevelopment.sellworth.util.Utils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SellGui {
   private final Sell plugin;
   private String title;
   private int rows;
   private int size;
   private String actionbarFmt;
   private String chatFmt;
   private int barLength;
   private String barSymbol;
   private String loadingColor;
   private String completeLoadingColor;
   private Sound closeSound;
   private boolean useNewMenuFlag;

   public SellGui(Sell plugin) {
      this.plugin = plugin;
      this.useNewMenuFlag = plugin.getConfig().getBoolean("use-new-sell-menu", false);
      this.loadConfig();
   }

   private void loadSection(String section) {
      ConfigurationSection cfg = this.plugin.getConfig().getConfigurationSection(section);
      if (cfg == null) {
         throw new IllegalStateException("Missing section '" + section + "' in config.yml");
      } else {
         this.title = Utils.formatColors(cfg.getString("title", "&aSell Items"));
         this.rows = cfg.getInt("rows", 5);
         this.size = this.rows * 9;
         this.actionbarFmt = Utils.formatColors(cfg.getString("actionbar-message", ""));
         this.chatFmt = Utils.formatColors(cfg.getString("chat-message", ""));
         this.barLength = cfg.getInt("bar-length", 10);
         this.barSymbol = Utils.formatColors(cfg.getString("bar-symbol", "#"));
         this.loadingColor = Utils.formatColors(cfg.getString("loading-color", ""));
         this.completeLoadingColor = Utils.formatColors(cfg.getString("complete-loading-color", this.loadingColor));
         this.closeSound = null;
         ConfigurationSection sounds = cfg.getConfigurationSection("sounds");
         if (sounds != null) {
            String rawCloseSound = sounds.getString("close-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            Sound resolved = this.plugin.resolveSound(rawCloseSound, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            this.closeSound = resolved;
            if (resolved == Sound.ENTITY_EXPERIENCE_ORB_PICKUP && rawCloseSound != null && !rawCloseSound.equalsIgnoreCase("ENTITY_EXPERIENCE_ORB_PICKUP")) {
               this.plugin.getLogger().warning("Invalid close-sound in " + section + ": " + rawCloseSound);
            }
         }

      }
   }

   public void loadConfig() {
      this.loadSection("sell-menu");
   }
   
   
   public void updateMenuType() {
      boolean newUseNew = plugin.getConfig().getBoolean("use-new-sell-menu", false);
      if (this.useNewMenuFlag != newUseNew) {
         this.useNewMenuFlag = newUseNew;
         plugin.getLogger().info("Updated SellGui to use " + (this.useNewMenuFlag ? "new" : "old") + " menu type");
      }
   }

   public Inventory open(Player p) {
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, this.size, this.title);
      List<SellGui.LevelData> lvlList = this.loadLevels();
      if (!this.useNewMenuFlag && this.plugin.isUseMultipliers()) {
         this.populateBottomRow("sell-menu", inv, lvlList, p);
      }

      p.openInventory(inv);
      return inv;
   }

   public Inventory openNew(Player p) {
      this.loadSection("new-sell-menu");
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, this.size, this.title);
      List<SellGui.LevelData> lvlList = this.loadLevels();
      if (this.plugin.isUseMultipliers()) {
         this.populateBottomRow("new-sell-menu", inv, lvlList, p);
      }

      p.openInventory(inv);
      this.loadConfig();
      return inv;
   }

   private List<SellGui.LevelData> loadLevels() {
      List<SellGui.LevelData> lvlList = new ArrayList();
      ConfigurationSection lvlSec = this.plugin.getConfig().getConfigurationSection("progress-menu.levels");
      if (lvlSec != null) {
         Iterator var3 = lvlSec.getKeys(false).iterator();

         while(var3.hasNext()) {
            String key = (String)var3.next();
            ConfigurationSection ls = lvlSec.getConfigurationSection(key);
            if (ls != null) {
               lvlList.add(new SellGui.LevelData(ls.getLong("amountNeeded", 0L), ls.getDouble("multi", 1.0D)));
            }
         }

         lvlList.sort(Comparator.comparingLong((ld) -> {
            return ld.amountNeeded;
         }));
      }

      return lvlList;
   }

   private void populateBottomRow(String section, Inventory inv, List<SellGui.LevelData> lvlList, Player p) {
      if (this.plugin.isUseMultipliers()) {
         List<String> items = this.plugin.getConfig().getStringList(section + ".items");
         ConfigurationSection settings = this.plugin.getConfig().getConfigurationSection(section + ".item-settings");
         int defaultStart = (this.rows - 1) * 9;

         for(int i = 0; i < items.size(); ++i) {
            String catKey = (String)items.get(i);
            ConfigurationSection is = settings != null ? settings.getConfigurationSection(catKey) : null;
            int slot = defaultStart + i;
            if (is != null && is.isInt("slot")) {
               slot = is.getInt("slot");
            }

            String matName = is != null && is.getString("material") != null ? is.getString("material") : catKey;

            Material mat;
            try {
               mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException var23) {
               continue;
            }

            ItemStack button = new ItemStack(mat, 1);
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
               SellGui.LevelData next = this.findNextLevel(lvlList, p, catKey);
               double pct = this.computePct(next, p, catKey);
               String bar = this.buildBar(pct);
               if (is != null) {
                  meta.setDisplayName(Utils.formatColors(is.getString("displayname", catKey)));
                  List<String> lore = new ArrayList();
                  Iterator var21 = is.getStringList("lore").iterator();

                  while(var21.hasNext()) {
                     String line = (String)var21.next();
                     lore.add(Utils.formatColors(line.replace("%next-multi%", String.format("%.1f", next.multi)).replace("%progress%", String.format("%.1f", pct)).replace("%progress-bar%", bar)));
                  }

                  meta.setLore(lore);
               } else {
                  meta.setDisplayName(Utils.formatColors("&e" + catKey.toLowerCase(Locale.ROOT)));
               }

               button.setItemMeta(meta);
               inv.setItem(slot, button);
            }
         }

      }
   }

   private SellGui.LevelData findNextLevel(List<SellGui.LevelData> lvlList, Player p, String key) {
      double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key.toLowerCase(Locale.ROOT));
      Iterator var6 = lvlList.iterator();

      SellGui.LevelData ld;
      do {
         if (!var6.hasNext()) {
            return lvlList.isEmpty() ? new SellGui.LevelData(1L, 1.0D) : (SellGui.LevelData)lvlList.get(lvlList.size() - 1);
         }

         ld = (SellGui.LevelData)var6.next();
      } while(!(sold < (double)ld.amountNeeded));

      return ld;
   }

   private double computePct(SellGui.LevelData ld, Player p, String key) {
      double sold = this.plugin.getRawTotalSold(p.getUniqueId(), key.toLowerCase(Locale.ROOT));
      return ld.amountNeeded > 0L ? Math.min(sold / (double)ld.amountNeeded * 100.0D, 100.0D) : 100.0D;
   }

   private String buildBar(double pct) {
      int filled = (int)Math.round((double)this.barLength * (pct / 100.0D));
      StringBuilder b = new StringBuilder();
      b.append(this.loadingColor);

      int i;
      for(i = 0; i < filled; ++i) {
         b.append(this.barSymbol);
      }

      b.append("&f");

      for(i = filled; i < this.barLength; ++i) {
         b.append(this.barSymbol);
      }

      if (pct >= 100.0D) {
         b = new StringBuilder(this.completeLoadingColor);

         for(i = 0; i < this.barLength; ++i) {
            b.append(this.barSymbol);
         }
      }

      return b.toString();
   }

   public void handleClose(Player p) {
      if (this.closeSound != null) {
         p.playSound(p.getLocation(), this.closeSound, 1.0F, 1.0F);
      }

   }

   public boolean matchesTitle(String openTitle) {
      return ChatColor.stripColor(openTitle).equals(ChatColor.stripColor(this.title));
   }

   public int getSize() {
      return this.size;
   }

   private static class LevelData {
      final long amountNeeded;
      final double multi;

      LevelData(long a, double m) {
         this.amountNeeded = a;
         this.multi = m;
      }
   }
}

