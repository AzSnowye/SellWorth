package com.epixdevelopment.sellworth.commands;

import com.epixdevelopment.sellworth.Sell;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellHistoryCommand implements CommandExecutor {
   private final Sell plugin;

   public SellHistoryCommand(Sell plugin) {
      this.plugin = plugin;
      plugin.getCommand("sellhistory").setExecutor(this);
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "Only players can view sell history.");
         return true;
      } else {
         Player p = (Player)sender;
         this.plugin.getSellHistoryGui().open(p, 1);
         return true;
      }
   }
}
