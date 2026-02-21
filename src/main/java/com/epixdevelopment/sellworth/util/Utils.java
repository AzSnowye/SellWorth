package com.epixdevelopment.sellworth.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;

public final class Utils {
   private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

   private Utils() {
   }

   public static String formatColors(String input) {
      if (input == null) {
         return null;
      } else {
         Matcher matcher = HEX_PATTERN.matcher(input);
         StringBuffer buffer = new StringBuffer(input.length() + 32);

         while(matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            char[] var5 = hex.toCharArray();
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               char c = var5[var7];
               replacement.append('§').append(c);
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
         }

         matcher.appendTail(buffer);
         return ChatColor.translateAlternateColorCodes('&', buffer.toString());
      }
   }

   public static List<String> formatColors(List<String> lines) {
      return (List)lines.stream().map(Utils::formatColors).collect(Collectors.toList());
   }

   public static String abbreviateNumber(double number) {
      if (number < 1000.0D) {
         return number == (double)((long)number) ? String.format("%d", (long)number) : String.format("%.1f", number);
      } else {
         String[] units = new String[]{"K", "M", "B", "T", "Q"};
         double value = number;

         int unitIndex;
         for(unitIndex = -1; value >= 1000.0D && unitIndex < units.length - 1; ++unitIndex) {
            value /= 1000.0D;
         }

         String formatted;
         if (value == (double)((long)value)) {
            formatted = String.format("%d", (long)value);
         } else {
            formatted = String.format("%.2f", value);
         }

         return formatted + units[unitIndex];
      }
   }
}
