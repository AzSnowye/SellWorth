package com.epixdevelopment.sellworth.tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HistoryTracker {
   private final Map<UUID, Integer> pages = new HashMap();
   private final Map<UUID, HistoryTracker.SortOrder> orders = new HashMap();
   private final Map<UUID, String> filters = new HashMap();
   private final Map<UUID, UUID> targets = new HashMap();

   public void setTarget(UUID viewer, UUID target) {
      this.targets.put(viewer, target);
   }

   public UUID getTarget(UUID viewer) {
      return this.targets.getOrDefault(viewer, viewer);
   }

   public void setPage(UUID player, int page) {
      this.pages.put(player, page);
   }

   public int getPage(UUID player) {
      return (Integer)this.pages.getOrDefault(player, 1);
   }

   public void setOrder(UUID player, HistoryTracker.SortOrder order) {
      this.orders.put(player, order);
   }

   public HistoryTracker.SortOrder getOrder(UUID player) {
      return (HistoryTracker.SortOrder)this.orders.getOrDefault(player, HistoryTracker.SortOrder.HIGH);
   }

   public void cycleOrder(UUID player) {
      HistoryTracker.SortOrder cur = this.getOrder(player);
      HistoryTracker.SortOrder next;
      switch(cur.ordinal()) {
      case 0:
         next = HistoryTracker.SortOrder.LOW;
         break;
      case 1:
         next = HistoryTracker.SortOrder.NAME;
         break;
      case 2:
         next = HistoryTracker.SortOrder.HIGH;
         break;
      default:
         next = HistoryTracker.SortOrder.HIGH;
      }

      this.orders.put(player, next);
   }

   public void setFilter(UUID player, String filter) {
      if (filter == null) {
         this.filters.remove(player);
      } else {
         this.filters.put(player, filter);
      }

   }

   public String getFilter(UUID player) {
      return (String)this.filters.get(player);
   }

   public boolean isTracked(UUID player) {
      return this.pages.containsKey(player);
   }

   public void clear(UUID player) {
      this.pages.remove(player);
      this.orders.remove(player);
      this.filters.remove(player);
      this.targets.remove(player);
   }

   public static enum SortOrder {
      HIGH,
      LOW,
      NAME;

      private static HistoryTracker.SortOrder[] $values() {
         return new HistoryTracker.SortOrder[]{HIGH, LOW, NAME};
      }
   }
}

