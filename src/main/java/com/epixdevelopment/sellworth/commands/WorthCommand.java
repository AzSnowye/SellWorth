package com.epixdevelopment.sellworth.commands;


import com.epixdevelopment.sellworth.Sell;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.epixdevelopment.sellworth.util.Utils;
import com.epixdevelopment.sellworth.tracker.ViewTracker;

public class WorthCommand implements CommandExecutor, TabCompleter {
   private final Sell plugin;
   private final List<String> materialKeys;

   public WorthCommand(Sell plugin) {
      this.plugin = plugin;
      this.materialKeys = (List)Arrays.stream(Material.values()).filter(Material::isItem).map((m) -> {
         return m.name().toLowerCase();
      }).sorted().collect(Collectors.toList());
      plugin.getCommand("worth").setExecutor(this);
      plugin.getCommand("worth").setTabCompleter(this);
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "Only players can use /worth.");
         return true;
      } else {
         Player p = (Player)sender;
         if (args.length == 0) {
            ViewTracker vt = this.plugin.getViewTracker();
            vt.setFilter(p.getUniqueId(), (String)null);
            this.plugin.getItemPricesMenu().open(p, 1);
            return true;
         } else {
            String joined = String.join("_", args).toUpperCase();
            Material mat = Material.matchMaterial(joined);
            if (mat != null && mat.isItem()) {
               ItemStack lookup = new ItemStack(mat);
               double baseValue = this.plugin.calculateItemWorth(lookup);
               double multiplier = 1.0D;
               String categoryFound = null;
               Iterator var14 = this.plugin.categoryItems.entrySet().iterator();

               while(var14.hasNext()) {
                  Entry<String, List<String>> entry = (Entry)var14.next();
                  if (((List)entry.getValue()).contains(mat.name())) {
                     multiplier = this.plugin.getSellMultiplier(p.getUniqueId(), (String)entry.getKey());
                     categoryFound = (String)entry.getKey();
                     break;
                  }
               }

               double finalValue = baseValue * multiplier;
               String pretty = this.prettify(mat);
               String amount = Utils.abbreviateNumber(finalValue);
               String template = this.plugin.getConfig().getString("messages.worth", "&e1 %item% is worth %amount%");
               String msg = Utils.formatColors(template.replace("%item%", pretty).replace("%amount%", amount).replace("%mult%", String.valueOf(multiplier)));
               p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
               p.sendMessage(msg);
               return true;
            } else {
               p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString("messages.worth-invalid", "&cUnknown item: %input%").replace("%input%", String.join(" ", args))));
               return true;
            }
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      if (args.length >= 1) {
         String pre = String.join("_", args).toLowerCase().replace(' ', '_');
         return (List)this.materialKeys.stream().filter((key) -> {
            return key.startsWith(pre);
         }).map((key) -> {
            Material m = Material.matchMaterial(key.toUpperCase());
            return m != null && m.isItem() ? this.prettify(m) : key.replace('_', ' ');
         }).collect(Collectors.toList());
      } else {
         return Collections.emptyList();
      }
   }

   private String prettify(Material m) {
      String[] parts = m.name().toLowerCase().split("_");

      for(int i = 0; i < parts.length; ++i) {
         char var10002 = Character.toUpperCase(parts[i].charAt(0));
         parts[i] = var10002 + parts[i].substring(1);
      }

      return String.join(" ", parts);
   }
}
