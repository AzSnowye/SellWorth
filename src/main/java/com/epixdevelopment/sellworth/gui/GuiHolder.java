package com.epixdevelopment.sellworth.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {
   private final String categoryKey;
   private final int page;
   private final CategoryGui.SortOrder sortOrder;

   public GuiHolder(String categoryKey, int page) {
      this(categoryKey, page, CategoryGui.SortOrder.NAME);
   }

   public GuiHolder(String categoryKey, int page, CategoryGui.SortOrder sortOrder) {
      this.categoryKey = categoryKey;
      this.page = page;
      this.sortOrder = sortOrder == null ? CategoryGui.SortOrder.NAME : sortOrder;
   }

   public String getCategoryKey() {
      return this.categoryKey;
   }

   public int getPage() {
      return this.page;
   }

   public CategoryGui.SortOrder getSortOrder() {
      return this.sortOrder;
   }

   public Inventory getInventory() {
      return null;
   }
}
