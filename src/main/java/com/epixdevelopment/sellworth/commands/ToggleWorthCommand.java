package com.epixdevelopment.sellworth.commands;


import com.epixdevelopment.sellworth.Sell;
import java.util.Objects;
import java.util.UUID;
import com.epixdevelopment.sellworth.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class ToggleWorthCommand implements CommandExecutor {
   private final Sell plugin;

   public ToggleWorthCommand(Sell plugin) {
      this.plugin = plugin;
      ((PluginCommand)Objects.requireNonNull(plugin.getCommand("toggleworth"), "Command 'toggleworth' not found in plugin.yml")).setExecutor(this);
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (sender instanceof Player) {
         Player p = (Player)sender;
         UUID id = p.getUniqueId();
         boolean newEnabled = !this.plugin.isWorthEnabled(id);
         this.plugin.setWorthEnabled(id, !newEnabled);
         boolean currentlyEnabled = this.plugin.isWorthEnabled(id);
         boolean targetEnabled = !currentlyEnabled;
         this.plugin.setWorthEnabled(id, targetEnabled);
         if (!targetEnabled) {
            this.plugin.getCleanupListener().stripAllLore(p);
         } else {
            p.updateInventory();
         }

         String path = targetEnabled ? "messages.worth-enabled" : "messages.worth-disabled";
         String fallback = targetEnabled ? "&#34ee80Worth lore: &aENABLED" : "&#34ee80Worth lore: &cDISABLED";
         p.sendMessage(Utils.formatColors(this.plugin.getConfig().getString(path, fallback)));
         return true;
      } else {
         sender.sendMessage("§cOnly players can use this command.");
         return true;
      }
   }
}
