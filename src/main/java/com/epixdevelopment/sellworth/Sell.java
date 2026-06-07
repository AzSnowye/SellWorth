package com.epixdevelopment.sellworth;

import com.epixdevelopment.sellworth.commands.SellAxeCommand;
import com.epixdevelopment.sellworth.commands.SellCommand;
import com.epixdevelopment.sellworth.commands.SellHistoryCommand;
import com.epixdevelopment.sellworth.commands.ToggleWorthCommand;
import com.epixdevelopment.sellworth.commands.WorthCommand;
import com.epixdevelopment.sellworth.config.ConfigManager;
import com.epixdevelopment.sellworth.gui.CategoryGui;
import com.epixdevelopment.sellworth.gui.ItemPricesMenu;
import com.epixdevelopment.sellworth.gui.ProgressGui;
import com.epixdevelopment.sellworth.gui.ResetConfirmationGui;
import com.epixdevelopment.sellworth.gui.SellGui;
import com.epixdevelopment.sellworth.gui.SellHistoryGui;
import com.epixdevelopment.sellworth.integration.SellPacketListener;
import com.epixdevelopment.sellworth.integration.SellPlaceholderExpansion;
import com.epixdevelopment.sellworth.integration.VaultMoneyPlaceholder;
import com.epixdevelopment.sellworth.integration.MMOItemsHook;
import com.epixdevelopment.sellworth.integration.CustomFishingHook;
import com.epixdevelopment.sellworth.listeners.ChatInputListener;
import com.epixdevelopment.sellworth.listeners.CleanupListener;
import com.epixdevelopment.sellworth.listeners.GuiClickListener;
import com.epixdevelopment.sellworth.listeners.HistoryClickListener;
import com.epixdevelopment.sellworth.listeners.InventoryClickListener;
import com.epixdevelopment.sellworth.listeners.SellAxe;
import com.epixdevelopment.sellworth.listeners.SellListener;
import com.epixdevelopment.sellworth.listeners.SellMenuClickListener;
import com.epixdevelopment.sellworth.tracker.HistoryTracker;
import com.epixdevelopment.sellworth.tracker.ViewTracker;
import com.epixdevelopment.sellworth.util.SchedulerUtil;
import com.epixdevelopment.sellworth.util.Utils;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import com.epixdevelopment.sellworth.storage.SellDataStore;
import com.epixdevelopment.sellworth.storage.SqlSellDataStore;
import com.epixdevelopment.sellworth.storage.YamlSellDataStore;
import me.clip.placeholderapi.PlaceholderAPI;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.nio.file.Files;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Sell extends JavaPlugin implements Listener {
   public static final String RESET = "\u001B[0m";
   public static final String RED = "\u001B[31m";
   public static final String GREEN = "\u001B[32m";
   public static final String YELLOW = "\u001B[33m";
   public static final String BLUE = "\u001B[34m";
   public static final String PURPLE = "\u001B[35m";
   public static final String CYAN = "\u001B[36m";
   public static final String WHITE = "\u001B[37m";

   public static final String BOLD = "\u001B[1m";
   public static final String UNDERLINE = "\u001B[4m";

   private SellPacketListener packetListener;
   private CleanupListener cleanupListener;
   private ViewTracker viewTracker;
   private ItemPricesMenu itemPricesMenu;
   private SellGui sellGui;
   private ProgressGui progressGui;
   private HistoryTracker historyTracker;
   private SellHistoryGui sellHistoryGui;
   private ResetConfirmationGui resetConfirmationGui;
   private Economy econ;
   private final Map<UUID, Double> totalSold = new HashMap();
   private final Map<UUID, Map<String, Double>> soldByCategory = new HashMap();
   private final Map<UUID, Map<String, Sell.Stats>> itemHistory = new HashMap();
   public final Map<String, List<String>> categoryItems = new HashMap();
   private final Map<String, Double> itemValues = new HashMap();
   private SellDataStore dataStore;
   private NamespacedKey sellAxeKey;
   private NamespacedKey expiryKey;
   private final Set<UUID> toggleWorthDisabled = new HashSet();
   private final Map<UUID, Long> urgentModePlayers = new HashMap();
   private BukkitRunnable minuteTimer;
   private BukkitRunnable secondTimer;
   private SellMenuClickListener sellMenuClickListener;
   private ConfigManager configManager;
   private static Sell instance;
   private ExecutorService storageExecutor;
   private org.bukkit.command.CommandExecutor sellMultiExecutor;
   private com.epixdevelopment.sellworth.util.TransactionLog transactionLog;
   private MMOItemsHook mmoItemsHook;
   private CustomFishingHook customFishingHook;

   public MMOItemsHook getMMOItemsHook() {
      return this.mmoItemsHook;
   }

   public CustomFishingHook getCustomFishingHook() {
      return this.customFishingHook;
   }

   public com.epixdevelopment.sellworth.util.TransactionLog getTransactionLog() {
      return this.transactionLog;
   }

   private static final int NOTIFICATION_FADE_IN = 5; // 0.25 seconds
   private static final int NOTIFICATION_STAY = 40; // 2 seconds
   private static final int NOTIFICATION_FADE_OUT = 10; // 0.5 seconds

   public Sound resolveSound(String raw, Sound fallback) {
      if (raw == null || raw.isEmpty()) {
         return fallback;
      }

      String normalized = raw.trim();
      if (normalized.isEmpty()) {
         return fallback;
      }

      String upper = normalized.toUpperCase(Locale.ROOT);

      // Legacy Bukkit (Sound was an enum) - invoke reflectively to keep 1.21+
      // compiling.
      try {
         Method valueOf = Sound.class.getMethod("valueOf", String.class);
         Object out = valueOf.invoke(null, upper);
         if (out instanceof Sound) {
            return (Sound) out;
         }
      } catch (Throwable ignored) {
      }

      // Modern Bukkit/Paper (Sound is registry-backed)
      try {
         String key = normalized.toLowerCase(Locale.ROOT);
         if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
         }
         key = key.replace('_', '.');

         Sound reg = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
         if (reg != null) {
            return reg;
         }
      } catch (Throwable ignored) {
      }

      return fallback;
   }

   private void submitStorageTask(Runnable task) {
      ExecutorService executor = this.storageExecutor;
      if (executor != null && !executor.isShutdown()) {
         // shove storage work off main so u don't lag players
         executor.execute(task);
      } else {
         // fallback sync if executor died
         task.run();
      }
   }

   private void shutdownStorageExecutor() {
      ExecutorService executor = this.storageExecutor;
      if (executor == null) {
         return;
      }
      executor.shutdown();
      try {
         if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
         }
      } catch (InterruptedException e) {
         executor.shutdownNow();
         Thread.currentThread().interrupt();
      } finally {
         this.storageExecutor = null;
      }
   }

   public static Sell getInstance() {
      return instance;
   }

   public SellPacketListener getSellPacketListener() {
      return this.packetListener;
   }

   public CleanupListener getCleanupListener() {
      return this.cleanupListener;
   }

   public ResetConfirmationGui getResetConfirmationGui() {
      return this.resetConfirmationGui;
   }

   private EnumSet<Sell.SellNotifyMode> getNotifyModes() {
      EnumSet<Sell.SellNotifyMode> modes = EnumSet.noneOf(Sell.SellNotifyMode.class);
      Object raw = this.getConfig().get("sell-notify-mode");
      if (raw == null) {
         raw = this.getConfig().get("sell-shower");
      }

      if (raw instanceof String) {
         String s = (String) raw;
         this.addModesFromString(s, modes);
      } else if (raw instanceof List) {
         List<?> list = (List) raw;
         Iterator var5 = list.iterator();

         while (var5.hasNext()) {
            Object o = var5.next();
            this.addModesFromString(String.valueOf(o), modes);
         }
      } else if (raw != null) {
         this.addModesFromString(String.valueOf(raw), modes);
      }

      if (modes.isEmpty()) {
         modes.add(Sell.SellNotifyMode.ACTIONBAR);
      }

      return modes;
   }

   private void addModesFromString(String s, EnumSet<Sell.SellNotifyMode> modes) {
      if (s != null) {
         String[] parts = s.split("[,;\\s]+");
         String[] var4 = parts;
         int var5 = parts.length;

         for (int var6 = 0; var6 < var5; ++var6) {
            String p = var4[var6];
            String t = p.trim();
            if (!t.isEmpty()) {
               try {
                  modes.add(Sell.SellNotifyMode.valueOf(t.toUpperCase(Locale.ROOT)));
               } catch (IllegalArgumentException var10) {
               }
            }
         }

      }
   }

   private void notifySale(Player p, double moneyEarned, long itemsSold) {
      EnumSet<Sell.SellNotifyMode> modes = this.getNotifyModes();
      String amt = Utils.abbreviateNumber(moneyEarned);
      String itemsStr = String.valueOf(itemsSold);
      String title;
      if (modes.contains(Sell.SellNotifyMode.CHAT)) {
         title = Utils.formatColors(this.getConfig().getString("sell-menu.chat-message", "&#34ee80+$%amount%"))
               .replace("%amount%", amt).replace("%items%", itemsStr);
         p.sendMessage(title);
      }

      if (modes.contains(Sell.SellNotifyMode.ACTIONBAR)) {
         title = Utils.formatColors(this.getConfig().getString("sell-menu.actionbar-message", "&#34ee80+$%amount%"))
               .replace("%amount%", amt).replace("%items%", itemsStr);
         p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(title));
      }

      if (modes.contains(Sell.SellNotifyMode.TITLE)) {
         title = Utils.formatColors(this.getConfig().getString("sell-notify.screen.title", "&a+$%amount%"))
               .replace("%amount%", amt).replace("%items%", itemsStr);
         String subtitle = Utils
               .formatColors(this.getConfig().getString("sell-notify.screen.subtitle", "&7You sold %items% items"))
               .replace("%amount%", amt).replace("%items%", itemsStr);
         p.sendTitle(title, subtitle, NOTIFICATION_FADE_IN, NOTIFICATION_STAY, NOTIFICATION_FADE_OUT);
      }

   }

   private void displayWelcomeArt() {
      getLogger().info("");
      getLogger().info(RED + "   ███████╗███████╗██╗     ██╗    " + WHITE
            + "██╗    ██╗ ██████╗ ██████╗ ████████╗██╗  ██╗" + RESET);
      getLogger().info(RED + "   ██╔════╝██╔════╝██║     ██║    " + WHITE
            + "██║    ██║██╔═══██╗██╔══██╗╚══██╔══╝██║  ██║" + RESET);
      getLogger().info(RED + "   ███████╗█████╗  ██║     ██║    " + WHITE
            + "██║ █╗ ██║██║   ██║██████╔╝   ██║   ███████║" + RESET);
      getLogger().info(RED + "   ╚════██║██╔══╝  ██║     ██║    " + WHITE
            + "██║███╗██║██║   ██║██╔══██╗   ██║   ██╔══██║" + RESET);
      getLogger().info(RED + "   ███████║███████╗███████╗███████╗" + WHITE
            + "╚███╔███╔╝╚██████╔╝██║ ██║   ██║   ██║  ██║" + RESET);
      getLogger().info(RED + "   ╚══════╝╚══════╝╚══════╝╚══════╝ " + WHITE
            + "╚══╝╚══╝  ╚═════╝ ╚═╝ ╚═╝   ╚═╝   ╚═╝  ╚═╝" + RESET);
      getLogger().info("");
      getLogger().info(YELLOW + BOLD + "»»—————— [" + YELLOW + "CREATED AND MAINTAINED BY EPIX DEVELOPMENT" + YELLOW
            + BOLD + "] ——————««" + RESET);
      getLogger().info("");
   }

   @Override
   public void onLoad() {
      try {
         PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
         PacketEvents.getAPI().getSettings().checkForUpdates(false).bStats(true);
         PacketEvents.getAPI().load();
      } catch (Exception e) {
         getLogger().severe("Failed to load PacketEvents: " + e.getMessage());
      }
   }

   @Override
   public void onEnable() {
      if (instance != null) {
         if (getLogger() != null) {
            getLogger().info("");
            getLogger().info(RED + BOLD + "[!] " + RED + "Plugin already initialized!" + RESET);
            getLogger().info(WHITE + "This usually happens when the plugin is" + RESET);
            getLogger().info(WHITE + "reloaded instead of a proper restart." + RESET);
            getLogger().info("");
            getLogger().info(YELLOW + "» " + WHITE + "Please restart your server to apply changes" + RESET);
            getLogger().info(YELLOW + "» " + WHITE + "Get help: " + CYAN + "https://discord.gg/hPw84FhM3f" + RESET);
            getLogger().info("");
         }
         this.setEnabled(false);
         return;
      }

      instance = this;
      storageExecutor = Executors.newSingleThreadExecutor(r -> {
         Thread t = new Thread(r, "SellWorth-Storage");
         t.setDaemon(true);
         return t;
      });
      SchedulerUtil.init(this);
      displayWelcomeArt();

      try {
         if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
         }

         this.configManager = new ConfigManager(this);
         this.configManager.setup();

         if (!this.isEnabled()) {
            return;
         }

         this.configManager.reloadConfig();

         try {
            boolean needsRegeneration = false;
            String[] requiredSections = { "lore", "sell-notify", "sounds", "messages" };

            for (String section : requiredSections) {
               if (!getConfig().contains(section)) {
                  needsRegeneration = true;
                  break;
               }
            }

            if (needsRegeneration) {
               getLogger().info("");
               getLogger().info(YELLOW + "» " + WHITE + "Generating missing configuration sections..." + RESET);

               this.configManager.generateFullConfig();
               this.configManager.reloadConfig();

               getLogger().info(GREEN + "» " + WHITE + "Configuration updated successfully!" + RESET);
            } else {
               getLogger().info("");
               getLogger().info(GREEN + "» " + WHITE + "Configuration is up to date" + RESET);
            }

            getLogger().info(WHITE + "» " + WHITE + "Version: " + YELLOW + getDescription().getVersion() + RESET);
            getLogger().info("");
         } catch (Exception e) {
            getLogger().severe("Failed to verify configuration: " + e.getMessage());
            e.printStackTrace();
         }

         getLogger().info("");

         setupSaveFile();
         loadHistory();
         getLogger().info("Player data loaded successfully");

         this.reloadConfig();
         buildCategoryItems();

         try {
            PacketEvents.getAPI().init();
            this.packetListener = new SellPacketListener(this);
            PacketEvents.getAPI().getEventManager().registerListener(
               this.packetListener,
               com.github.retrooper.packetevents.event.PacketListenerPriority.NORMAL
            );
            this.getLogger().info("PacketEvents integration initialized");
         } catch (Exception e) {
            this.getLogger().severe("Failed to initialize PacketEvents integration: " + e.getMessage());
            this.getLogger().warning("Some features may not work without PacketEvents!");
         }

         this.mmoItemsHook = new MMOItemsHook(this);
         this.customFishingHook = new CustomFishingHook(this);

         this.viewTracker = new ViewTracker();
         this.historyTracker = new HistoryTracker();
         this.cleanupListener = new CleanupListener(this);

         if (this.configManager != null) {
            this.reloadConfig();

            this.itemPricesMenu = new ItemPricesMenu(this);
            this.sellGui = new SellGui(this);
            this.progressGui = new ProgressGui(this);
            this.sellHistoryGui = new SellHistoryGui(this);
            this.resetConfirmationGui = new ResetConfirmationGui(this);
            this.sellMenuClickListener = new SellMenuClickListener(this);
         } else {
            this.getLogger()
                  .warning("ConfigManager is not initialized, GUI components will be initialized on first use");
         }

         this.transactionLog = new com.epixdevelopment.sellworth.util.TransactionLog(this);

         this.sellAxeKey = new NamespacedKey(this, "sell_wand");
         this.expiryKey = new NamespacedKey(this, "sell_wand_expiry");

         this.getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
         this.getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);
         this.getServer().getPluginManager().registerEvents(this, this);
         this.getServer().getPluginManager().registerEvents(this.sellMenuClickListener, this);
         this.getServer().getPluginManager().registerEvents(new HistoryClickListener(this), this);
         this.getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);
         this.getServer().getPluginManager().registerEvents(this.cleanupListener, this);

         new SellHistoryCommand(this);
         new SellCommand(this);
         new WorthCommand(this);
         new ToggleWorthCommand(this);
         new SellAxeCommand(this, this.sellAxeKey, this.expiryKey);
         new SellAxe(this, this.sellAxeKey, this.expiryKey);

         setupSellMultiCommand();

         if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
               new SellPlaceholderExpansion(this).register();
               new VaultMoneyPlaceholder(this).register();
               getLogger().info("Successfully registered PlaceholderAPI placeholders");
            } catch (Exception e) {
               getLogger().warning("Failed to register PlaceholderAPI placeholders: " + e.getMessage());
            }
         }

         this.unregisterOtherSellCommands();

         for (Player p : this.getServer().getOnlinePlayers()) {
            this.cleanupListener.stripAllLore(p);
            p.updateInventory();
         }

         try {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
               if (!this.isEnabled())
                  return;

               Bukkit.getScheduler().runTaskLater(this, () -> {
                  try {
                     if (!this.setupVault()) {
                        this.getLogger().warning("Vault setup failed. Economy features will be disabled.");
                     }
                  } catch (Exception e) {
                     this.getLogger().warning("Error during Vault setup: " + e.getMessage());
                  }
               }, 10L);
            }, 20L); // 1 second delay to ensure server is ready
         } catch (Exception e) {
            this.getLogger().warning("Failed to schedule Vault setup, trying direct setup...");
            try {
               if (!this.setupVault()) {
                  this.getLogger().warning("Direct Vault setup failed. Economy features will be disabled.");
               }
            } catch (Exception ex) {
               this.getLogger().warning("Error during direct Vault setup: " + ex.getMessage());
            }
         }

         if (this.getConfig().getBoolean("sell-axe.use-countdown", true)) {
            this.setupSellAxeCountdown();
         }

         com.epixdevelopment.sellworth.integration.SeriaBoosterHook.init();

         if (getServer().getPluginManager().getPlugin("AxSellwands") != null) {
            getServer().getPluginManager().registerEvents(new com.epixdevelopment.sellworth.integration.AxSellwandsListener(this), this);
            getLogger().info("Successfully hooked into AxSellwands for multipliers.");
         }

         this.getLogger().info("SellWorth has been enabled successfully!");
      } catch (Exception e) {
         this.getLogger().severe("Failed to enable SellWorth: " + e.getMessage());
         e.printStackTrace();
         this.getServer().getPluginManager().disablePlugin(this);
      }
   }

   @Override
   public void onDisable() {
      shutdownStorageExecutor();
      try {
         saveHistory();
         getLogger().info("Player data saved successfully");

         if (minuteTimer != null) {
            try {
               minuteTimer.cancel();
            } catch (IllegalStateException e) {
            }
         }
         if (secondTimer != null) {
            try {
               secondTimer.cancel();
            } catch (IllegalStateException e) {
            }
         }

         if (this.dataStore != null) {
            try {
               this.dataStore.close();
            } catch (Exception ignored) {
            }
            this.dataStore = null;
         }

         try {
            PacketEvents.getAPI().terminate();
         } catch (Exception e) {
            getLogger().warning("Error terminating PacketEvents: " + e.getMessage());
         }

         getLogger().info("SellWorth has been disabled successfully!");
      } catch (Exception e) {
         getLogger().severe("Error during plugin shutdown: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private void setupSellAxeCountdown() {
      minuteTimer = new BukkitRunnable() {
         @Override
         public void run() {
            if (!isEnabled()) {
               this.cancel();
               return;
            }

            try {
               long now = System.currentTimeMillis();
               processAllPlayers(now, false); // false = minute mode
               checkUrgentMode(now);
            } catch (Exception e) {
               getLogger().warning("Error in minute timer: " + e.getMessage());
               e.printStackTrace();
            }
         }
      };

      secondTimer = new BukkitRunnable() {
         @Override
         public void run() {
            if (!isEnabled()) {
               this.cancel();
               return;
            }

            try {
               long now = System.currentTimeMillis();
               processAllPlayers(now, true); // true = urgent mode
               checkUrgentMode(now);
            } catch (Exception e) {
               getLogger().warning("Error in second timer: " + e.getMessage());
               e.printStackTrace();
            }
         }
      };

      minuteTimer.runTaskTimer(this, 0L, 1200L); // Every minute
   }

   private void processAllPlayers(long now, boolean urgentMode) {
      // sweep all players to refresh wand timers and lore; urgentMode=true when
      // countdown is low
      for (Player player : getServer().getOnlinePlayers()) {
         try {
            boolean inventoryUpdated = false;

            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand != null && itemInHand.getType() != Material.AIR) {
               if (updateSellAxeItem(player, itemInHand, now, urgentMode)) {
                  inventoryUpdated = true;
               }
            }

            Inventory inv = player.getInventory();
            for (int slot = 0; slot < inv.getSize(); slot++) {
               ItemStack item = inv.getItem(slot);
               if (item != null && !item.getType().isAir()) {
                  if (updateSellAxeItem(player, item, now, urgentMode)) {
                     inventoryUpdated = true;
                  }
               }
            }

            if (inventoryUpdated) {
               player.updateInventory();
            }
         } catch (Exception e) {
            getLogger()
                  .warning("Error processing player " + player.getName() + " in sell axe countdown: " + e.getMessage());
         }
      }
   }

   private void checkUrgentMode(long now) {
      Set<UUID> playersInUrgentMode = new HashSet<>();

      for (Player player : getServer().getOnlinePlayers()) {
         try {
            if (hasSellAxeWithLowTime(player, now)) {
               playersInUrgentMode.add(player.getUniqueId());
            }
         } catch (Exception e) {
            getLogger().warning("Error checking urgent mode for " + player.getName() + ": " + e.getMessage());
         }
      }

      if (!playersInUrgentMode.isEmpty()) {
         try {
            // spin up the fast timer only when someone is close to expiry
            if (secondTimer == null) {
               secondTimer.runTaskTimer(this, 0L, 20L); // Every second
            } else {
               try {
                  boolean isCancelled = secondTimer.isCancelled();
                  if (isCancelled) {
                     secondTimer.runTaskTimer(this, 0L, 20L); // Every second
                  }
               } catch (IllegalStateException e) {
                  secondTimer.runTaskTimer(this, 0L, 20L);
               }
            }
         } catch (IllegalStateException e) {
         }
      } else if (secondTimer != null) {
         try {
            // nobody in urgent mode, kill the fast timer to save ticks
            if (!secondTimer.isCancelled()) {
               secondTimer.cancel();
            }
         } catch (IllegalStateException e) {
         }
      }

      urgentModePlayers.clear();
      for (UUID uuid : playersInUrgentMode) {
         urgentModePlayers.put(uuid, now);
      }
   }

   private boolean hasSellAxeWithLowTime(Player player, long now) {
      ItemStack itemInHand = player.getInventory().getItemInMainHand();
      if (itemInHand != null && itemInHand.getType() != Material.AIR) {
         if (isSellAxeWithLowTime(itemInHand, now)) {
            return true;
         }
      }

      Inventory inv = player.getInventory();
      for (int slot = 0; slot < inv.getSize(); slot++) {
         ItemStack item = inv.getItem(slot);
         if (item != null && !item.getType().isAir()) {
            if (isSellAxeWithLowTime(item, now)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isSellAxeWithLowTime(ItemStack item, long now) {
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         PersistentDataContainer pdc = meta.getPersistentDataContainer();
         Byte marker = pdc.get(sellAxeKey, PersistentDataType.BYTE);

         if (marker != null && marker == 1) {
            Long expiry = pdc.get(expiryKey, PersistentDataType.LONG);
            if (expiry != null) {
               long remainingMillis = expiry - now;
               long remainingSeconds = remainingMillis / 1000L;
               return remainingSeconds <= 59L; // Urgent mode: 59 seconds or less
            }
         }
      }
      return false;
   }

   private boolean updateSellAxeItem(Player player, ItemStack item, long now, boolean urgentMode) {
      try {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            Byte marker = pdc.get(sellAxeKey, PersistentDataType.BYTE);

            if (marker != null && marker == 1) {
               Long expiry = pdc.get(expiryKey, PersistentDataType.LONG);
               if (expiry != null) {
                  if (now >= expiry) {
                     player.getInventory().removeItem(item);
                     player.sendMessage(Utils.formatColors("&cYour Sell Wand has expired and been removed."));
                     return true;
                  } else {
                     return updateSellAxeLore(player, item, now, urgentMode);
                  }
               }
            }
         }
         return false;
      } catch (Exception e) {
         getLogger().warning("Error updating sell axe item: " + e.getMessage());
         return false;
      }
   }

   @EventHandler(ignoreCancelled = true)
   public void onPlayerCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
      String message = event.getMessage().toLowerCase(Locale.ROOT);
      if (message.equals("/sell") || message.equals("/sellmenu")) {
         if (!event.getPlayer().hasPermission("sell.use")) {
            return; // Let Bukkit handle the permission denied message
         }
         event.setCancelled(true);
         if (this.sellGui != null) {
            this.sellGui.open(event.getPlayer());
         }
      } else if (message.startsWith("/sell ") || message.startsWith("/sellmenu ")) {
         event.setMessage("/sellworth:" + event.getMessage().substring(1));
      }
   }

   private void unregisterOtherSellCommands() {
      // Intentionally left empty as the old reflection logic broke the plugin's own
      // commands
   }

   private boolean setupVault() {
      if (getServer().getPluginManager().getPlugin("Vault") == null) {
         getLogger().warning("Vault not found! Economy features will be disabled.");
         return false;
      }

      RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp == null) {
         getLogger().warning("No economy plugin found! Economy features will be disabled.");
         return false;
      }

      econ = rsp.getProvider();
      return econ != null;
   }

   private void setupSellMultiCommand() {
      org.bukkit.command.PluginCommand sellMultiCommand = getCommand("sellmulti");
      if (sellMultiCommand != null) {
         sellMultiCommand.setExecutor(null);

         if (this.getConfig().getBoolean("use-new-sell-menu", false)) {
            sellMultiExecutor = (sender, cmd, lbl, args) -> {
               if (!(sender instanceof org.bukkit.entity.Player)) {
                  sender.sendMessage("§cOnly players can use this command.");
                  return true;
               } else {
                  this.getSellGui().openNew((org.bukkit.entity.Player) sender);
                  return true;
               }
            };
            sellMultiCommand.setExecutor(sellMultiExecutor);
         }
      }
   }

   public void reloadPlugin() {
      try {
         configManager.reloadConfig();
         reloadConfig(); // Reload Bukkit's config

         if (mmoItemsHook != null) {
            mmoItemsHook.checkPlugin();
            if (mmoItemsHook.isEnabled()) {
               mmoItemsHook.loadConfig();
            }
         }
         if (customFishingHook != null) {
            customFishingHook.checkPlugin();
         }

         setupSaveFile();
         loadHistory();
         buildCategoryItems(); // Rebuild category items

         if (packetListener != null) {
            packetListener.loadConfigData();
         }

         if (sellGui != null) {
            sellGui.loadConfig(); // Reload GUI configuration
            sellGui.updateMenuType();
         }

         if (sellMenuClickListener != null) {
            sellMenuClickListener.updateMenuType();
         }

         setupSellMultiCommand();

         progressGui = new ProgressGui(this);
         sellHistoryGui = new SellHistoryGui(this);
         itemPricesMenu = new ItemPricesMenu(this); // Rebuild item prices menu

         getLogger().info("Configuration and data reloaded successfully!");
      } catch (Exception e) {
         getLogger().severe("Failed to reload plugin: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private void setupSaveFile() {
      if (this.dataStore != null) {
         try {
            this.dataStore.close();
         } catch (Exception ignored) {
         }
         this.dataStore = null;
      }

      String typeRaw = this.getConfig().getString("storage.type", "yaml");
      String type = typeRaw == null ? "yaml" : typeRaw.trim().toLowerCase(Locale.ROOT);

      try {
         if (type.equals("sqlite")) {
            String dbName = this.getConfig().getString("storage.sqlite.file", "sellworth.db");
            File dbFile = new File(this.getDataFolder(), dbName);
            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            this.dataStore = new SqlSellDataStore(this, jdbcUrl, null, null);
         } else if (type.equals("mysql")) {
            String host = this.getConfig().getString("storage.mysql.host", "localhost");
            int port = this.getConfig().getInt("storage.mysql.port", 3306);
            String database = this.getConfig().getString("storage.mysql.database", "sellworth");
            String user = this.getConfig().getString("storage.mysql.username", "root");
            String pass = this.getConfig().getString("storage.mysql.password", "");
            boolean useSsl = this.getConfig().getBoolean("storage.mysql.useSSL", false);
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSsl
                  + "&characterEncoding=utf8";
            this.dataStore = new SqlSellDataStore(this, jdbcUrl, user, pass);
         } else {
            this.dataStore = new YamlSellDataStore(this);
         }

         this.dataStore.init();
      } catch (Exception e) {
         getLogger().severe("Failed to initialize data store (" + type + "): " + e.getMessage());
         try {
            this.dataStore = new YamlSellDataStore(this);
            this.dataStore.init();
         } catch (Exception e2) {
            getLogger().severe("Failed to initialize YAML fallback data store: " + e2.getMessage());
         }
      }
   }

   private void loadHistory() {
      if (this.dataStore == null) {
         getLogger().warning("Cannot load history: data store not initialized");
         return;
      }

      try {
         this.dataStore.loadAll(this.totalSold, this.soldByCategory, this.itemHistory, this.toggleWorthDisabled);
      } catch (Exception e) {
         getLogger().severe("Failed to load player data: " + e.getMessage());
         e.printStackTrace();
         return;
      }

      String typeRaw = this.getConfig().getString("storage.type", "yaml");
      String type = typeRaw == null ? "yaml" : typeRaw.trim().toLowerCase(Locale.ROOT);
      boolean migrate = this.getConfig().getBoolean("storage.migrate-from-yaml", true);

      if (migrate && !type.equals("yaml")) {
         boolean empty = this.totalSold.isEmpty() && this.soldByCategory.isEmpty() && this.itemHistory.isEmpty()
               && this.toggleWorthDisabled.isEmpty();
         if (empty) {
            try {
               YamlSellDataStore yaml = new YamlSellDataStore(this);
               yaml.init();
               yaml.loadAll(this.totalSold, this.soldByCategory, this.itemHistory, this.toggleWorthDisabled);
               boolean after = this.totalSold.isEmpty() && this.soldByCategory.isEmpty() && this.itemHistory.isEmpty()
                     && this.toggleWorthDisabled.isEmpty();
               if (!after) {
                  this.dataStore.saveAll(this.totalSold, this.soldByCategory, this.itemHistory,
                        this.toggleWorthDisabled);
               }
            } catch (Exception ignored) {
            }
         }
      }
   }

   private void saveHistory() {
      try {
         if (this.dataStore == null) {
            getLogger().warning("Cannot save history: data store not initialized");
            return;
         }
         this.dataStore.saveAll(this.totalSold, this.soldByCategory, this.itemHistory, this.toggleWorthDisabled);
      } catch (Exception e) {
         getLogger().severe("Could not save player data: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private void saveToggleWorthToSave() {
      if (this.dataStore == null) {
         getLogger().warning("Cannot save toggle worth: data store not initialized");
         return;
      }
      Set<UUID> snapshot = new HashSet<>(this.toggleWorthDisabled);
      submitStorageTask(() -> {
         try {
            if (this.dataStore != null) {
               this.dataStore.saveToggleWorth(snapshot);
            }
         } catch (Exception e) {
            getLogger().severe("Could not save toggle worth: " + e.getMessage());
            e.printStackTrace();
         }
      });
   }

   public boolean isWorthEnabled(UUID id) {
      return !this.toggleWorthDisabled.contains(id);
   }

   public void setWorthEnabled(UUID id, boolean enabled) {
      if (enabled) {
         this.toggleWorthDisabled.remove(id);
      } else {
         this.toggleWorthDisabled.add(id);
      }

      this.saveToggleWorthToSave();

      Player p = Bukkit.getPlayer(id);
      if (p != null) {
         if (!enabled) {
            if (this.cleanupListener != null) {
               this.cleanupListener.stripAllLore(p);
            }
         } else {
            p.updateInventory();
         }
      }
   }

   private void buildCategoryItems() {
      this.itemValues.clear();
      this.categoryItems.clear();
      ConfigurationSection cats = this.getConfig().getConfigurationSection("categories");
      if (cats != null) {
         String cat;
         ArrayList mats;
         label49: for (Iterator var2 = cats.getKeys(false).iterator(); var2.hasNext(); this.categoryItems.put(cat,
               mats)) {
            cat = (String) var2.next();
            List<?> raw = this.getConfig().getList("categories." + cat);
            mats = new ArrayList();
            if (raw != null) {
               Iterator var6 = raw.iterator();

               while (true) {
                  Object o;
                  do {
                     if (!var6.hasNext()) {
                        continue label49;
                     }

                     o = var6.next();
                  } while (!(o instanceof Map));

                  Map<?, ?> map = (Map) o;
                  Iterator var9 = map.entrySet().iterator();

                  while (var9.hasNext()) {
                     Entry<?, ?> me = (Entry) var9.next();
                     String entryKey = me.getKey().toString().trim();

                     double price;
                     try {
                        price = Double.parseDouble(me.getValue().toString());
                     } catch (NumberFormatException var16) {
                        this.getLogger().warning("Invalid price for '" + entryKey + "' in category '" + cat + "'");
                        continue;
                     }

                     String lowerKey = entryKey.toLowerCase(Locale.ROOT);
                     this.itemValues.put(lowerKey, price);
                     String matName = lowerKey.replaceAll("(?i)-value$", "").toUpperCase(Locale.ROOT);
                     mats.add(matName);
                  }
               }
            }
         }

      }
   }

   public double getPrice(String key) {
      return (Double) this.itemValues.getOrDefault(key.toLowerCase(Locale.ROOT),
            this.getConfig().getDouble("default-value", 0.1D));
   }

   public Map<String, Double> getItemValues() {
      return Collections.unmodifiableMap(this.itemValues);
   }

   public void recordSale(Player p, Map<String, Sell.Stats> sold) {
      UUID id = p.getUniqueId();
      this.itemHistory.computeIfAbsent(id, (k) -> {
         return new HashMap();
      });
      sold.forEach((k, st) -> {
         Sell.Stats old = (Sell.Stats) ((Map) this.itemHistory.get(id)).getOrDefault(k, new Sell.Stats(0.0D, 0.0D));
         ((Map) this.itemHistory.get(id)).put(k, new Sell.Stats(old.count + st.count, old.revenue + st.revenue));
         if (this.transactionLog != null) {
            this.transactionLog.logSell(p, k, st.count, st.revenue, false);
         }
      });
      double sum = sold.values().stream().mapToDouble((s) -> {
         return s.revenue;
      }).sum();
      this.totalSold.merge(id, sum, Double::sum);
      Map<String, Double> cmap = (Map) this.soldByCategory.computeIfAbsent(id, (k) -> {
         return new HashMap();
      });
      Iterator var7 = this.categoryItems.entrySet().iterator();

      while (var7.hasNext()) {
         Entry<String, List<String>> entry = (Entry) var7.next();
         double catSum = sold.entrySet().stream().filter((e2) -> {
            return ((List) entry.getValue()).contains(((String) e2.getKey()).toUpperCase(Locale.ROOT));
         }).mapToDouble((e2) -> {
            return ((Sell.Stats) e2.getValue()).revenue;
         }).sum();
         if (catSum > 0.0D) {
            cmap.merge((String) entry.getKey(), catSum, Double::sum);
         }
      }

   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      Player p = (Player) event.getPlayer();
      String openTitle = event.getView().getTitle();
      String classicTitle = Utils.formatColors(this.getConfig().getString("sell-menu.title", "&aSell Items"));
      String newTitle = Utils.formatColors(this.getConfig().getString("new-sell-menu.title", "&aSell Items"));
      if (!openTitle.equals(newTitle)) {
         if (openTitle.equals(classicTitle)) {
            Inventory inv = event.getInventory();
            boolean useNewFlag = this.getConfig().getBoolean("use-new-sell-menu", false);
            boolean excludeBottomRow = !useNewFlag && this.isUseMultipliers();
            int sellableSlots = inv.getSize() - (excludeBottomRow ? 9 : 0);
            List<String> disabledList = this.getConfig().getStringList("disabled-items");
            Set<String> disabledSet = new HashSet();
            Iterator var12 = disabledList.iterator();

            String declineMsg;
            while (var12.hasNext()) {
               declineMsg = (String) var12.next();
               disabledSet.add(declineMsg.toUpperCase(Locale.ROOT));
            }

            Sound declineSound = this.resolveSound(this.getConfig().getString("sounds.declined", "ENTITY_VILLAGER_NO"),
                  Sound.ENTITY_VILLAGER_NO);
            declineMsg = Utils
                  .formatColors(this.getConfig().getString("messages.cannot-sell", "&cYou cannot sell that item!"));
            Map<String, Sell.Stats> sold = new HashMap();
            Map<String, Double> revCats = new HashMap();
            boolean notifiedDecline = false;

            int i;
            ItemStack item;
            String mat;
            boolean isShulkerBox;
            for (i = 0; i < sellableSlots; ++i) {
               item = inv.getItem(i);
               if (item != null && !item.getType().isAir()) {
                  mat = item.getType().name();
                  isShulkerBox = mat.endsWith("_SHULKER_BOX") || item.getType() == Material.SHULKER_BOX;
                  if (disabledSet.contains(mat) && !isShulkerBox) {
                     Map<Integer, ItemStack> leftover = p.getInventory().addItem(new ItemStack[] { item });
                     leftover.values().forEach((d) -> {
                        p.getWorld().dropItemNaturally(p.getLocation(), d);
                     });
                     inv.setItem(i, (ItemStack) null);
                     if (!notifiedDecline) {
                        p.playSound(p.getLocation(), declineSound, 1.0F, 1.0F);
                        p.sendMessage(declineMsg);
                        notifiedDecline = true;
                     }
                  }
               }
            }

            for (i = 0; i < sellableSlots; ++i) {
               item = inv.getItem(i);
               if (item != null && !item.getType().isAir()) {
                  mat = item.getType().name();
                  isShulkerBox = mat.endsWith("_SHULKER_BOX") || item.getType() == Material.SHULKER_BOX;
                  String var10000;
                  Iterator var32;
                  Entry eEntry;
                  double totalRev;
                  ItemMeta im;
                  BlockStateMeta bsm;
                  ShulkerBox boxState;
                  BlockState var56;
                  int lvl;
                  if (isShulkerBox && disabledSet.contains(mat)) {
                     im = item.getItemMeta();
                     if (im instanceof BlockStateMeta) {
                        bsm = (BlockStateMeta) im;
                        var56 = bsm.getBlockState();
                        if (var56 instanceof ShulkerBox) {
                           boxState = (ShulkerBox) var56;
                           Inventory var58 = boxState.getInventory();
                           ItemStack[] var61 = var58.getContents();
                           lvl = var61.length;

                           for (int var67 = 0; var67 < lvl; ++var67) {
                              ItemStack inside = var61[var67];
                              if (inside != null && !inside.getType().isAir()) {
                                 String insideMat = inside.getType().name();
                                 if (!disabledSet.contains(insideMat)) {
                                    ItemMeta insideMeta = inside.getItemMeta();
                                    if (inside.getType() == Material.ENCHANTED_BOOK
                                          && insideMeta instanceof EnchantmentStorageMeta) {
                                       EnchantmentStorageMeta innerESM = (EnchantmentStorageMeta) insideMeta;
                                       var32 = innerESM.getStoredEnchants().entrySet().iterator();

                                       while (var32.hasNext()) {
                                          Entry<Enchantment, Integer> innerEntry = (Entry) var32.next();
                                          Enchantment enc = innerEntry.getKey();
                                          int enchantLevel = innerEntry.getValue();
                                          var10000 = enc.getKey().getKey().toLowerCase(Locale.ROOT);
                                          String keyName = var10000 + enchantLevel;
                                          double singlePrice = this.getPrice(keyName + "-value");
                                          double itemTotalRev = singlePrice * (double) inside.getAmount();
                                          sold.merge(keyName, new Sell.Stats((double) inside.getAmount(), itemTotalRev),
                                                (a, b) -> {
                                                   return new Sell.Stats(a.count + b.count, a.revenue + b.revenue);
                                                });
                                          Iterator var89 = this.categoryItems.entrySet().iterator();

                                          while (var89.hasNext()) {
                                             Entry<String, List<String>> cat = (Entry) var89.next();
                                             if (((List) cat.getValue()).contains(keyName.toUpperCase(Locale.ROOT))) {
                                                revCats.merge(cat.getKey(), itemTotalRev, Double::sum);
                                             }
                                          }
                                       }
                                    } else {
                                        String insideKey = this.getItemKey(inside);
                                        double insideValue = this.calculateItemWorth(p, inside);
                                        sold.merge(insideKey, new Sell.Stats((double) inside.getAmount(), insideValue),
                                              (a, b) -> {
                                                 return new Sell.Stats(a.count + b.count, a.revenue + b.revenue);
                                              });
                                        boolean isCfItem = this.customFishingHook != null && this.customFishingHook.isCustomFishingItem(inside);
                                        Iterator var82 = this.categoryItems.entrySet().iterator();
 
                                        while (var82.hasNext()) {
                                           Entry<String, List<String>> cat = (Entry) var82.next();
                                           if (isCfItem) {
                                               if (cat.getKey().equalsIgnoreCase("tropical_fish")) {
                                                  revCats.merge((String) cat.getKey(), insideValue, Double::sum);
                                               }
                                           } else if (((List) cat.getValue()).contains(inside.getType().name())) {
                                              revCats.merge((String) cat.getKey(), insideValue, Double::sum);
                                           }
                                        }
                                    }
                                 }
                              }
                           }

                           ItemStack emptyBox = new ItemStack(item.getType(), item.getAmount());
                           inv.setItem(i, (ItemStack) null);
                           Map<Integer, ItemStack> returned = p.getInventory().addItem(new ItemStack[] { emptyBox });
                           returned.values().forEach((d) -> {
                              p.getWorld().dropItemNaturally(p.getLocation(), d);
                           });
                           continue;
                        }
                     }

                     inv.setItem(i, (ItemStack) null);
                  } else if (isShulkerBox) {
                     im = item.getItemMeta();
                     if (im instanceof BlockStateMeta) {
                        bsm = (BlockStateMeta) im;
                        var56 = bsm.getBlockState();
                        if (var56 instanceof ShulkerBox) {
                           boxState = (ShulkerBox) var56;
                           String boxKey = item.getType().name().toLowerCase(Locale.ROOT);
                           double boxValue = this.getPrice(boxKey + "-value") * (double) item.getAmount();
                           sold.merge(boxKey, new Sell.Stats((double) item.getAmount(), boxValue), (a, b) -> {
                              return new Sell.Stats(a.count + b.count, a.revenue + b.revenue);
                           });
                           Iterator var65 = this.categoryItems.entrySet().iterator();

                           while (var65.hasNext()) {
                              Entry<String, List<String>> cat = (Entry) var65.next();
                              if (((List) cat.getValue()).contains(item.getType().name())) {
                                 revCats.merge((String) cat.getKey(), boxValue, Double::sum);
                              }
                           }

                           Inventory boxInv = boxState.getInventory();
                           ItemStack[] var69 = boxInv.getContents();
                           int var29 = var69.length;

                           for (int var72 = 0; var72 < var29; ++var72) {
                              ItemStack inside = var69[var72];
                              if (inside != null && !inside.getType().isAir()) {
                                 ItemMeta insideMeta = inside.getItemMeta();
                                 if (inside.getType() == Material.ENCHANTED_BOOK
                                       && insideMeta instanceof EnchantmentStorageMeta) {
                                    EnchantmentStorageMeta innerESM = (EnchantmentStorageMeta) insideMeta;
                                    Iterator var79 = innerESM.getStoredEnchants().entrySet().iterator();

                                    while (var79.hasNext()) {
                                       Entry<Enchantment, Integer> enchantEntry = (Entry) var79.next();
                                       Enchantment enc = enchantEntry.getKey();
                                       int enchantLevel = enchantEntry.getValue();
                                       var10000 = enc.getKey().getKey().toLowerCase(Locale.ROOT);
                                       String keyName = var10000 + enchantLevel;
                                       double itemTotalRev = this.getPrice(keyName + "-value")
                                             * (double) inside.getAmount();
                                       sold.merge(keyName, new Sell.Stats((double) inside.getAmount(), itemTotalRev),
                                             (a, b) -> {
                                                return new Sell.Stats(a.count + b.count, a.revenue + b.revenue);
                                             });
                                       Iterator var43 = this.categoryItems.entrySet().iterator();

                                       while (var43.hasNext()) {
                                          Entry<String, List<String>> cat = (Entry) var43.next();
                                          if (cat.getValue().contains(keyName.toUpperCase(Locale.ROOT))) {
                                             revCats.merge(cat.getKey(), itemTotalRev, Double::sum);
                                          }
                                       }
                                    }
                                 } else {
                                    String insideKey = this.getItemKey(inside);
                                    double insideValue = this.calculateItemWorth(p, inside);
                                    sold.merge(insideKey, new Sell.Stats((double) inside.getAmount(), insideValue),
                                          (a, b) -> {
                                             return new Sell.Stats(a.count + b.count, a.revenue + b.revenue);
                                          });
                                    boolean isCfItem = this.customFishingHook != null && this.customFishingHook.isCustomFishingItem(inside);
                                    Iterator var37 = this.categoryItems.entrySet().iterator();

                                    while (var37.hasNext()) {
                                       Entry<String, List<String>> cat = (Entry) var37.next();
                                       if (isCfItem) {
                                           if (cat.getKey().equalsIgnoreCase("tropical_fish")) {
                                              revCats.merge((String) cat.getKey(), insideValue, Double::sum);
                                           }
                                       } else if (((List) cat.getValue()).contains(inside.getType().name())) {
                                          revCats.merge((String) cat.getKey(), insideValue, Double::sum);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } else if (!disabledSet.contains(mat)) {
                     im = item.getItemMeta();
                     if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta) {
                        EnchantmentStorageMeta esm = (EnchantmentStorageMeta) im;
                        Iterator var54 = esm.getStoredEnchants().entrySet().iterator();

                        while (var54.hasNext()) {
                           Entry<Enchantment, Integer> enchantEntry = (Entry) var54.next();
                           Enchantment enc = enchantEntry.getKey();
                           int enchantLevel = enchantEntry.getValue();
                           var10000 = enc.getKey().getKey().toLowerCase(Locale.ROOT);
                           String keyName = var10000 + enchantLevel;
                           double singlePrice = this.getPrice(keyName + "-value");
                           double itemTotalRev = singlePrice * (double) item.getAmount();
                           sold.merge(keyName, new Sell.Stats((double) item.getAmount(), itemTotalRev), (a, b) -> {
                              return new Sell.Stats(a.count + b.count, a.revenue + b.revenue);
                           });
                           var32 = this.categoryItems.entrySet().iterator();

                           while (var32.hasNext()) {
                              Entry<String, List<String>> categoryEntry = (Entry) var32.next();
                              if (categoryEntry.getValue().contains(keyName.toUpperCase(Locale.ROOT))) {
                                 revCats.merge(categoryEntry.getKey(), itemTotalRev, Double::sum);
                              }
                           }
                        }
                     } else {
                         String key = this.getItemKey(item);
                         double raw = this.calculateItemWorth(p, item);
                         sold.merge(key, new Sell.Stats((double) item.getAmount(), raw), (a, b) -> {
                            return new Sell.Stats(a.count + b.count, a.revenue + b.revenue);
                         });
                         boolean isCfItem = this.customFishingHook != null && this.customFishingHook.isCustomFishingItem(item);
                         Iterator var25 = this.categoryItems.entrySet().iterator();
 
                         while (var25.hasNext()) {
                            Entry<String, List<String>> cat = (Entry) var25.next();
                            if (isCfItem) {
                                if (cat.getKey().equalsIgnoreCase("tropical_fish")) {
                                   revCats.merge((String) cat.getKey(), raw, Double::sum);
                                }
                            } else if (((List) cat.getValue()).contains(item.getType().name())) {
                               revCats.merge((String) cat.getKey(), raw, Double::sum);
                            }
                         }
                     }
                  }
               }
            }

            if (!sold.isEmpty()) {
               this.recordSale(p, sold);
               double payout;
               if (this.isUseMultipliers()) {
                  double categorized = revCats.entrySet().stream().mapToDouble((e) -> {
                     return (Double) e.getValue() * this.getSellMultiplier(p.getUniqueId(), (String) e.getKey());
                  }).sum();
                  double uncategorized = sold.entrySet().stream().filter((e) -> {
                     String k = e.getKey();
                     if (k.startsWith("customfishing_")) {
                        return false;
                     }
                     return this.categoryItems.values().stream().noneMatch((list) -> {
                        return list.contains(k.toUpperCase(Locale.ROOT));
                      });
                  }).mapToDouble((e) -> {
                     return ((Sell.Stats) e.getValue()).revenue;
                  }).sum();
                  payout = categorized + uncategorized;
               } else {
                  payout = sold.values().stream().mapToDouble((s) -> {
                     return s.revenue;
                  }).sum();
               }

               Economy economy = this.getEconomy();
               if (economy != null) {
                  economy.depositPlayer(p, payout);
               } else {
                  this.getLogger().warning(
                        "Economy provider not available; skipping payout for " + p.getName() + " ($" + payout + ")");
               }

               Sound soundOnClose = this.resolveSound(
                     this.getConfig().getString("sell-menu.sound-on-close", "ENTITY_EXPERIENCE_ORB_PICKUP"),
                     Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
               p.playSound(p.getLocation(), soundOnClose, 1.0F, 1.0F);
               long itemsSold = Math.round(sold.values().stream().mapToDouble((s) -> {
                  return s.count;
               }).sum());
               this.getServer().getPluginManager().callEvent(new com.epixdevelopment.sellworth.api.events.SellWorthSellEvent(p, payout, itemsSold));
               this.notifySale(p, payout, itemsSold);
            }
          }
       }
    }

   private String getPotionKey(ItemStack item) {
      ItemMeta var3 = item.getItemMeta();
      if (var3 instanceof PotionMeta) {
         PotionMeta pm = (PotionMeta) var3;
         String base = pm.getBasePotionData().getType().name().toLowerCase(Locale.ROOT);
         if (pm.getBasePotionData().isExtended()) {
            base = "long_" + base;
         }

         if (pm.getBasePotionData().isUpgraded()) {
            base = "strong_" + base;
         }

         Material mat = item.getType();
         if (mat == Material.SPLASH_POTION) {
            base = "splash_" + base;
         } else if (mat == Material.LINGERING_POTION) {
            base = "lingering_" + base;
         }

         return base;
      } else {
         return null;
      }
   }

   public double calculateItemWorth(ItemStack item) {
      return this.calculateItemWorth((org.bukkit.entity.Player) null, item);
   }

   public double calculateItemWorth(org.bukkit.entity.Player player, ItemStack item) {
      if (this.mmoItemsHook != null && this.mmoItemsHook.isEnabled()) {
         Double mmoPrice = this.mmoItemsHook.getPrice(item);
         if (mmoPrice != null) {
            return mmoPrice * (double) item.getAmount();
         }
      }
      if (this.customFishingHook != null && this.customFishingHook.isEnabled()) {
         Double cfPrice = this.customFishingHook.getPrice(player, item);
         if (cfPrice != null) {
            return cfPrice * (double) item.getAmount();
         }
      }

      ItemMeta im = item.getItemMeta();
      double base;
      String baseKey;
      String var10000;
      if (item.getType() == Material.SPAWNER && im instanceof BlockStateMeta) {
         BlockStateMeta bsm = (BlockStateMeta) im;
         BlockState var17 = bsm.getBlockState();
         if (var17 instanceof CreatureSpawner) {
            CreatureSpawner cs = (CreatureSpawner) var17;
            baseKey = cs.getSpawnedType().name().toLowerCase(Locale.ROOT);
            String spawnerKey = baseKey + "_spawner-value";
            base = this.getPrice(spawnerKey);
         } else {
            base = this.getPrice("spawner-value");
         }
      } else {
         String potionKey = this.getPotionKey(item);
         if (potionKey != null) {
            base = this.getPrice(potionKey + "-value");
         } else {
            var10000 = item.getType().name();
            baseKey = var10000.toLowerCase(Locale.ROOT) + "-value";
            base = this.getPrice(baseKey);
         }
      }

      double ench = 0.0D;
      if (im instanceof EnchantmentStorageMeta) {
         EnchantmentStorageMeta esm = (EnchantmentStorageMeta) im;

         String k;
         for (Iterator var19 = esm.getStoredEnchants().entrySet().iterator(); var19
               .hasNext(); ench += this.getPrice(k)) {
            Entry<Enchantment, Integer> e = (Entry) var19.next();
            var10000 = ((Enchantment) e.getKey()).getKey().getKey().toLowerCase(Locale.ROOT);
            k = var10000 + String.valueOf(e.getValue()) + "-value";
         }
      }

      String k;
      if (im != null) {
         for (Iterator var20 = im.getEnchants().entrySet().iterator(); var20.hasNext(); ench += this.getPrice(k)) {
            Entry<Enchantment, Integer> e = (Entry) var20.next();
            var10000 = ((Enchantment) e.getKey()).getKey().getKey().toLowerCase(Locale.ROOT);
            k = var10000 + String.valueOf(e.getValue()) + "-value";
         }
      }

      double total = (base + ench) * (double) item.getAmount();
      if (im instanceof BlockStateMeta) {
         BlockStateMeta bsm = (BlockStateMeta) im;
         BlockState var11 = bsm.getBlockState();
         if (var11 instanceof ShulkerBox) {
            ShulkerBox box = (ShulkerBox) var11;
            ItemStack[] var26 = box.getInventory().getContents();
            int var12 = var26.length;

            for (int var13 = 0; var13 < var12; ++var13) {
               ItemStack inside = var26[var13];
               if (inside != null && inside.getType() != Material.AIR) {
                  total += this.calculateItemWorth(player, inside);
               }
            }
         }
      }

      return total;
   }

   public String getItemKey(ItemStack item) {
      if (this.customFishingHook != null && this.customFishingHook.isCustomFishingItem(item)) {
         try {
            String cfId = net.momirealms.customfishing.api.BukkitCustomFishingPlugin.getInstance().getItemManager().getCustomFishingItemID(item);
            if (cfId != null) {
               return "customfishing_" + cfId.toLowerCase(Locale.ROOT);
            }
         } catch (Throwable ignored) {}
      }
      if (this.mmoItemsHook != null && this.mmoItemsHook.isEnabled()) {
         try {
            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
            if (nbt != null && nbt.hasType()) {
               return "mmoitems_" + nbt.getType().toLowerCase(Locale.ROOT) + "_" + nbt.getString("MMOITEMS_ITEM_ID").toLowerCase(Locale.ROOT);
            }
         } catch (Throwable ignored) {}
      }
      return item.getType().name().toLowerCase(Locale.ROOT);
   }

   public void resetPlayerData(UUID uuid) {
      this.totalSold.remove(uuid);
      this.soldByCategory.remove(uuid);
      this.itemHistory.remove(uuid);

      try {
         if (this.dataStore != null) {
            this.dataStore.resetPlayer(uuid);
         }
      } catch (Exception e) {
         Logger var10000 = this.getLogger();
         String var10001 = String.valueOf(uuid);
         var10000.severe("Could not save resets for " + var10001 + ": " + e.getMessage());
      }

   }

   public double getSellMultiplier(UUID u, String cat) {
      double multiplier = 1.0D;
      ConfigurationSection levels = this.getConfig().getConfigurationSection("progress-menu.levels");
      if (levels != null) {
         double soldInCategory = (Double) ((Map) this.soldByCategory.getOrDefault(u, Collections.emptyMap()))
               .getOrDefault(cat, 0.0D);
         Iterator var8 = levels.getKeys(false).iterator();

         while (var8.hasNext()) {
            String key = (String) var8.next();
            ConfigurationSection lvlSec = levels.getConfigurationSection(key);
            if (lvlSec != null) {
               double needed = lvlSec.getDouble("amountNeeded", Double.MAX_VALUE);
               double multi = lvlSec.getDouble("multi", 1.0D);
               if (soldInCategory >= needed) {
                  multiplier = multi;
               }
            }
         }
      }

      return multiplier * com.epixdevelopment.sellworth.integration.SeriaBoosterHook.getSellMultiplier(u);
   }

   public double getMaxMilestoneMultiplier(UUID u) {
      double maxMultiplier = 1.0D;
      ConfigurationSection levels = this.getConfig().getConfigurationSection("progress-menu.levels");
      if (levels != null) {
         for (String cat : this.categoryItems.keySet()) {
            double soldInCategory = (Double) ((Map) this.soldByCategory.getOrDefault(u, Collections.emptyMap()))
                  .getOrDefault(cat, 0.0D);
            for (String key : levels.getKeys(false)) {
               ConfigurationSection lvlSec = levels.getConfigurationSection(key);
               if (lvlSec != null) {
                  double needed = lvlSec.getDouble("amountNeeded", Double.MAX_VALUE);
                  double multi = lvlSec.getDouble("multi", 1.0D);
                  if (soldInCategory >= needed && multi > maxMultiplier) {
                     maxMultiplier = multi;
                  }
               }
            }
         }
      }
      return maxMultiplier;
   }

   public double getItemMultiplier(Player p, ItemStack item) {
      if (!isUseMultipliers())
         return 1.0;
      double totalMult = 0;
      boolean found = false;

      List<String> keysToCheck = new ArrayList<>();
      ItemMeta im = item.getItemMeta();
      if (item.getType() == Material.ENCHANTED_BOOK && im instanceof EnchantmentStorageMeta) {
         EnchantmentStorageMeta esm = (EnchantmentStorageMeta) im;
         for (Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
            keysToCheck.add((entry.getKey().getKey().getKey() + entry.getValue()).toUpperCase(Locale.ROOT));
         }
      } else {
         keysToCheck.add(item.getType().name());
      }

      for (Entry<String, List<String>> entry : categoryItems.entrySet()) {
         for (String key : keysToCheck) {
            if (entry.getValue().contains(key)
                || (this.customFishingHook != null && this.customFishingHook.isCustomFishingItem(item) && entry.getKey().equalsIgnoreCase("tropical_fish"))) {
               totalMult += getSellMultiplier(p.getUniqueId(), entry.getKey());
               found = true;
               break;
            }
         }
      }
      return found ? totalMult : 1.0;
   }

   public ViewTracker getViewTracker() {
      return this.viewTracker;
   }

   public ItemPricesMenu getItemPricesMenu() {
      if (this.itemPricesMenu == null) {
         if (this.configManager != null) {
            this.reloadConfig();
            this.itemPricesMenu = new ItemPricesMenu(this);
         } else {
            throw new IllegalStateException("Cannot initialize ItemPricesMenu: ConfigManager is not available");
         }
      }
      return this.itemPricesMenu;
   }

   public SellGui getSellGui() {
      return this.sellGui;
   }

   public ProgressGui getProgressGui() {
      return this.progressGui;
   }

   public Economy getEconomy() {
      return this.econ;
   }

   public HistoryTracker getHistoryTracker() {
      return this.historyTracker;
   }

   public SellHistoryGui getSellHistoryGui() {
      return this.sellHistoryGui;
   }

   public Map<String, Sell.Stats> getHistory(UUID u) {
      return (Map) this.itemHistory.getOrDefault(u, Collections.emptyMap());
   }

   public String getFormattedTotalSold(UUID u) {
      return Utils.abbreviateNumber((Double) this.totalSold.getOrDefault(u, 0.0D));
   }

   public double getRawTotalSold(UUID u, String cat) {
      if (cat == null || cat.isEmpty()) {
         return totalSold.getOrDefault(u, 0.0D);
      }
      return (Double) ((Map) this.soldByCategory.getOrDefault(u, Collections.emptyMap())).getOrDefault(cat, 0.0D);
   }

   public double sumInventory(Inventory inv) {
      double t = 0.0D;
      ItemStack[] var4 = inv.getContents();
      int var5 = var4.length;

      for (int var6 = 0; var6 < var5; ++var6) {
         ItemStack i = var4[var6];
         if (i != null && !i.getType().isAir()) {
            t += this.calculateItemWorth(i);
         }
      }

      return t;
   }

   private String formatDuration(long ms) {
      return formatDuration(ms, false);
   }

   private String formatDuration(long ms, boolean urgentMode) {
      long totalSeconds = ms / 1000L;
      long days = totalSeconds / 86400L;
      long hours = totalSeconds % 86400L / 3600L;
      long minutes = totalSeconds % 3600L / 60L;
      long seconds = totalSeconds % 60L;

      if (urgentMode || totalSeconds <= 59) {
         if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
         } else if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
         } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
         } else {
            return seconds + "s";
         }
      } else {
         if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
         } else if (hours > 0) {
            return hours + "h " + minutes + "m";
         } else {
            return minutes + "m";
         }
      }
   }

   public boolean isUseMultipliers() {
      return this.getConfig().getBoolean("use-multipliers", true);
   }

   private static enum SellNotifyMode {
      TITLE,
      ACTIONBAR,
      CHAT;

      private static Sell.SellNotifyMode[] $values() {
         return new Sell.SellNotifyMode[] { TITLE, ACTIONBAR, CHAT };
      }
   }

   private boolean updateSellAxeLore(Player player, ItemStack item, long currentTime, boolean urgentMode) {
      if (item == null || item.getType() == Material.AIR) {
         return false;
      }

      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return false;
      }

      PersistentDataContainer pdc = meta.getPersistentDataContainer();
      if (!pdc.has(sellAxeKey, PersistentDataType.BYTE)) {
         return false;
      }

      Long expiry = pdc.get(expiryKey, PersistentDataType.LONG);
      if (expiry == null) {
         return false;
      }

      if (currentTime >= expiry) {
         return false; // Item has expired, will be handled by the caller
      }

      long remainingMillis = expiry - currentTime;
      String formatted = formatDuration(remainingMillis, urgentMode);
      List<String> template = getConfig().getStringList("sell-axe.lore");
      List<String> newLore = new ArrayList<>();

      for (String line : template) {
         String replaced = line.replace("%countdown%", formatted);
         newLore.add(Utils.formatColors(replaced));
      }

      if (!newLore.equals(meta.getLore())) {
         meta.setLore(newLore);
         item.setItemMeta(meta);
         return true;
      }

      return false;
   }

   public static class Stats {
      public double count;
      public double revenue;

      public Stats(double count, double revenue) {
         this.count = count;
         this.revenue = revenue;
      }
   }
}
