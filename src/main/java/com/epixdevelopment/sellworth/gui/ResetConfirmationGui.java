package com.epixdevelopment.sellworth.gui;


import com.epixdevelopment.sellworth.Sell;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import com.epixdevelopment.sellworth.util.Utils;

public class ResetConfirmationGui implements Listener {
   private final Sell plugin;
   private final Map<UUID, UUID> pendingReset = new HashMap();
   private static final String GUI_TITLE_PREFIX = "Confirm Reset: ";

   public ResetConfirmationGui(Sell plugin) {
      this.plugin = plugin;
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   public void open(Player admin, OfflinePlayer target) {
      this.pendingReset.put(admin.getUniqueId(), target.getUniqueId());
      String title = "Confirm Reset: " + target.getName();
      Inventory inv = Bukkit.createInventory((InventoryHolder)null, 36, title);
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta skullMeta = (SkullMeta)head.getItemMeta();
      if (skullMeta != null) {
         PlayerProfile profile = Bukkit.createPlayerProfile(target.getUniqueId());
         skullMeta.setOwnerProfile(profile);
         skullMeta.setDisplayName("§e" + target.getName());
         List<String> lore = new ArrayList();
         String totalStr = this.plugin.getFormattedTotalSold(target.getUniqueId());
         lore.add("§7Total sold: §b" + totalStr);
         skullMeta.setLore(lore);
         head.setItemMeta(skullMeta);
         inv.setItem(13, head);
      }

      ItemStack confirmPane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
      ItemMeta cMeta = confirmPane.getItemMeta();
      if (cMeta != null) {
         cMeta.setDisplayName("§aConfirm Reset");
         confirmPane.setItemMeta(cMeta);
         inv.setItem(15, confirmPane);
      }

      ItemStack declinePane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
      ItemMeta dMeta = declinePane.getItemMeta();
      if (dMeta != null) {
         dMeta.setDisplayName("§cCancel");
         declinePane.setItemMeta(dMeta);
         inv.setItem(11, declinePane);
      }

      List<String> categoryOrder = this.plugin.getConfigManager().getGuiConfig("sell-menu").getStringList("items");
      ConfigurationSection settingsSection = this.plugin.getConfigManager().getGuiConfig("sell-menu").getConfigurationSection("item-settings");

      for(int i = 0; i < categoryOrder.size() && i < 9; ++i) {
         String catKey = (String)categoryOrder.get(i);
         String catKeyLower = catKey.toLowerCase(Locale.ROOT);
         Material mat = Material.matchMaterial(catKey);
         String displayName = "§f" + catKey;
         if (settingsSection != null && settingsSection.isConfigurationSection(catKey)) {
            ConfigurationSection thisCatSec = settingsSection.getConfigurationSection(catKey);
            String dn;
            if (thisCatSec.contains("material")) {
               dn = thisCatSec.getString("material");
               if (dn != null) {
                  Material override = Material.matchMaterial(dn.toUpperCase(Locale.ROOT));
                  if (override != null) {
                     mat = override;
                  }
               }
            }

            if (thisCatSec.contains("displayname")) {
               dn = thisCatSec.getString("displayname");
               if (dn != null) {
                  displayName = Utils.formatColors(dn);
               }
            }
         }

         if (mat != null) {
            double rawCatSold = this.plugin.getRawTotalSold(target.getUniqueId(), catKeyLower);
            String formattedSold = Utils.abbreviateNumber(rawCatSold);
            ItemStack catItem = new ItemStack(mat);
            ItemMeta im = catItem.getItemMeta();
            if (im != null) {
               im.setDisplayName(displayName);
               List<String> lore = new ArrayList();
               lore.add("§7Sold: §b" + formattedSold);
               im.setLore(lore);
               catItem.setItemMeta(im);
            }

            inv.setItem(27 + i, catItem);
         }
      }

      admin.openInventory(inv);
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent e) {
      InventoryView view = e.getView();
      String title = view.getTitle();
      if (title != null && title.startsWith("Confirm Reset: ")) {
         e.setCancelled(true);
         if (e.getWhoClicked() instanceof Player) {
            Player admin = (Player)e.getWhoClicked();
            UUID adminUUID = admin.getUniqueId();
            if (!this.pendingReset.containsKey(adminUUID)) {
               admin.closeInventory();
            } else {
               int slot = e.getRawSlot();
               if (slot == 15) {
                  UUID targetUUID = (UUID)this.pendingReset.get(adminUUID);
                  OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetUUID);
                  this.plugin.resetPlayerData(targetUUID);
                  admin.playSound(admin.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
                  admin.sendMessage(Utils.formatColors("§aAll sell stats for §e" + targetOffline.getName() + " §ahas been reset."));
                  if (targetOffline.isOnline()) {
                     ((Player)targetOffline).sendMessage(Utils.formatColors("§cYour sell stats has been reset by an admin."));
                  }

                  this.pendingReset.remove(adminUUID);
                  admin.closeInventory();
               } else if (slot == 11) {
                  admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  admin.sendMessage(Utils.formatColors("§cReset cancelled."));
                  this.pendingReset.remove(adminUUID);
                  admin.closeInventory();
               }
            }
         }
      }
   }
}
