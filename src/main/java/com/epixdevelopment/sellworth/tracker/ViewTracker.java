package com.epixdevelopment.sellworth.tracker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ViewTracker {
   private final Map<UUID, Integer> pages = new HashMap();
   private final Map<UUID, ViewTracker.SortOrder> order = new HashMap();
   private final Map<UUID, String> filters = new HashMap();

   public void setPage(UUID player, int page) {
      this.pages.put(player, page);
   }

   public int getPage(UUID player) {
      return (Integer)this.pages.getOrDefault(player, 1);
   }

   public ViewTracker.SortOrder getOrder(UUID player) {
      return (ViewTracker.SortOrder)this.order.getOrDefault(player, ViewTracker.SortOrder.HIGH_TO_LOW);
   }

   public void setOrder(UUID player, ViewTracker.SortOrder sortOrder) {
      this.order.put(player, sortOrder);
   }

   public void cycleOrder(UUID player) {
      ViewTracker.SortOrder cur = this.getOrder(player);
      ViewTracker.SortOrder var10000;
      switch(cur.ordinal()) {
      case 0:
         var10000 = ViewTracker.SortOrder.LOW_TO_HIGH;
         break;
      case 1:
         var10000 = ViewTracker.SortOrder.NAME;
         break;
      case 2:
         var10000 = ViewTracker.SortOrder.HIGH_TO_LOW;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      ViewTracker.SortOrder next = var10000;
      this.order.put(player, next);
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

   public void clear(UUID player) {
      this.pages.remove(player);
      this.order.remove(player);
      this.filters.remove(player);
   }

   public boolean isTracked(UUID player) {
      return this.pages.containsKey(player);
   }

   public static enum SortOrder {
      HIGH_TO_LOW,
      LOW_TO_HIGH,
      NAME;

      private static ViewTracker.SortOrder[] $values() {
         return new ViewTracker.SortOrder[]{HIGH_TO_LOW, LOW_TO_HIGH, NAME};
      }
   }
}

