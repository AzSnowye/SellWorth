package com.epixdevelopment.sellworth.integration;

import com.epixdevelopment.sellworth.Sell;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class EGensHook {
    private final Sell plugin;
    private boolean enabled = false;

    public EGensHook(Sell plugin) {
        this.plugin = plugin;
        checkPlugin();
    }

    public void checkPlugin() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("eGens");
    }

    public boolean isEnabled() {
        return enabled;
    }

    private Object getField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private Object getEGensPlugin() {
        try {
            Class<?> egensPluginClass = Class.forName("com.epromite.egens.EGensPlugin");
            return JavaPlugin.getPlugin(egensPluginClass.asSubclass(JavaPlugin.class));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isGeneratorItem(ItemStack item) {
        if (!enabled || item == null) {
            return false;
        }
        try {
            Object egens = getEGensPlugin();
            if (egens != null) {
                Object generatorItems = getField(egens, "generatorItems");
                if (generatorItems != null) {
                    return (boolean) generatorItems.getClass().getMethod("isGeneratorItem", ItemStack.class).invoke(generatorItems, item);
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }

    public boolean isEgensDrop(ItemStack item) {
        if (!enabled || item == null) {
            return false;
        }
        try {
            Object egens = getEGensPlugin();
            if (egens != null) {
                Object generatorItems = getField(egens, "generatorItems");
                if (generatorItems != null) {
                    boolean isGen = (boolean) generatorItems.getClass().getMethod("isGeneratorItem", ItemStack.class).invoke(generatorItems, item);
                    if (isGen) {
                        return false;
                    }
                }
                
                // 1. Try PDC stamp resolution first
                Object dropsItems = getField(egens, "dropsItems");
                if (dropsItems != null) {
                    boolean resolved = dropsItems.getClass().getMethod("resolve", ItemStack.class).invoke(dropsItems, item) != null;
                    if (resolved) {
                        return true;
                    }
                }

                // 2. Fallback: match by properties against registered drops
                Object dropsRegistry = getField(egens, "dropsRegistry");
                if (dropsRegistry != null) {
                    java.util.Collection<?> allDrops = (java.util.Collection<?>) dropsRegistry.getClass().getMethod("all").invoke(dropsRegistry);
                    if (allDrops != null) {
                        for (Object entry : allDrops) {
                            if (matchDrop(entry, item)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }

    public Double getPrice(ItemStack item) {
        if (!enabled || item == null) {
            return null;
        }
        try {
            Object egens = getEGensPlugin();
            if (egens != null) {
                Object generatorItems = getField(egens, "generatorItems");
                if (generatorItems != null) {
                    boolean isGen = (boolean) generatorItems.getClass().getMethod("isGeneratorItem", ItemStack.class).invoke(generatorItems, item);
                    if (isGen) {
                        return null;
                    }
                }

                // 1. Try PDC stamp resolution first
                Object dropsItems = getField(egens, "dropsItems");
                if (dropsItems != null) {
                    Object entry = dropsItems.getClass().getMethod("resolve", ItemStack.class).invoke(dropsItems, item);
                    if (entry != null) {
                        return (Double) entry.getClass().getMethod("price").invoke(entry);
                    }
                }

                // 2. Fallback: match by properties against registered drops
                Object dropsRegistry = getField(egens, "dropsRegistry");
                if (dropsRegistry != null) {
                    java.util.Collection<?> allDrops = (java.util.Collection<?>) dropsRegistry.getClass().getMethod("all").invoke(dropsRegistry);
                    if (allDrops != null) {
                        for (Object entry : allDrops) {
                            if (matchDrop(entry, item)) {
                                return (Double) entry.getClass().getMethod("price").invoke(entry);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    private boolean matchDrop(Object dropEntry, ItemStack item) {
        if (dropEntry == null || item == null) {
            return false;
        }
        try {
            Object itemKey = dropEntry.getClass().getMethod("itemKey").invoke(dropEntry);
            if (itemKey == null) {
                return false;
            }

            // Check MMOItem
            boolean isMmo = (boolean) itemKey.getClass().getMethod("mmoitems").invoke(itemKey);
            if (isMmo) {
                String type = (String) itemKey.getClass().getMethod("mmoitemType").invoke(itemKey);
                String id = (String) itemKey.getClass().getMethod("mmoitemId").invoke(itemKey);
                try {
                    Class<?> nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
                    Object nbtItem = nbtItemClass.getMethod("get", ItemStack.class).invoke(null, item);
                    if (nbtItem != null) {
                        boolean hasType = (boolean) nbtItemClass.getMethod("hasType").invoke(nbtItem);
                        if (hasType) {
                            String mmoType = (String) nbtItemClass.getMethod("getType").invoke(nbtItem);
                            String mmoId = (String) nbtItemClass.getMethod("getString", String.class).invoke(nbtItem, "MMOITEMS_ITEM_ID");
                            if (type.equalsIgnoreCase(mmoType) && id.equalsIgnoreCase(mmoId)) {
                                return true;
                            }
                        }
                    }
                } catch (Throwable t) {
                    // ignore
                }
                return false;
            }

            // Check custom item (not fully implemented in EGens, but check name/metadata if set)
            boolean isCustom = (boolean) itemKey.getClass().getMethod("custom").invoke(itemKey);
            if (isCustom) {
                // Return true if fallback name matches
            }

            // Check vanilla Material
            org.bukkit.Material dropMat = (org.bukkit.Material) itemKey.getClass().getMethod("material").invoke(itemKey);
            if (item.getType() != dropMat) {
                return false;
            }

            // Check display name if defined
            String displayName = (String) dropEntry.getClass().getMethod("displayName").invoke(dropEntry);
            if (displayName != null) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasDisplayName()) {
                    return false;
                }
                String normItem = normalizeText(meta.getDisplayName());
                String normDrop = normalizeText(displayName);
                if (!normItem.equals(normDrop)) {
                    return false;
                }
            }

            // Check head texture for PLAYER_HEAD
            if (item.getType() == org.bukkit.Material.PLAYER_HEAD) {
                String headTexture = (String) dropEntry.getClass().getMethod("headTexture").invoke(dropEntry);
                if (headTexture != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    if (meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                        String texture = getSkullTexture(skullMeta);
                        if (texture == null || !texture.equals(headTexture)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }

            return true;
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        text = org.bukkit.ChatColor.stripColor(text);
        text = text.replaceAll("<[^>]*>", "");
        text = text.toLowerCase(java.util.Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case 'ᴀ': sb.append('a'); break;
                case 'ʙ': sb.append('b'); break;
                case 'ᴄ': sb.append('c'); break;
                case 'ᴅ': sb.append('d'); break;
                case 'ᴇ': sb.append('e'); break;
                case 'ғ': sb.append('f'); break;
                case 'ɢ': sb.append('g'); break;
                case 'ʜ': sb.append('h'); break;
                case 'ɪ': sb.append('i'); break;
                case 'ᴊ': sb.append('j'); break;
                case 'ᴋ': sb.append('k'); break;
                case 'ʟ': sb.append('l'); break;
                case 'ᴍ': sb.append('m'); break;
                case 'ɴ': sb.append('n'); break;
                case 'ᴏ': sb.append('o'); break;
                case 'ᴘ': sb.append('p'); break;
                case 'ǫ': sb.append('q'); break;
                case 'ʀ': sb.append('r'); break;
                case 'ᴛ': sb.append('t'); break;
                case 'ᴜ': sb.append('u'); break;
                case 'ᴠ': sb.append('v'); break;
                case 'ᴡ': sb.append('w'); break;
                case 'ʏ': sb.append('y'); break;
                case 'ᴢ': sb.append('z'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString().trim();
    }

    private String getSkullTexture(org.bukkit.inventory.meta.SkullMeta skull) {
        try {
            Object profile = skull.getClass().getMethod("getPlayerProfile").invoke(skull);
            if (profile != null) {
                java.util.Collection<?> properties = (java.util.Collection<?>) profile.getClass().getMethod("getProperties").invoke(profile);
                if (properties != null) {
                    for (Object prop : properties) {
                        String name = (String) prop.getClass().getMethod("getName").invoke(prop);
                        if ("textures".equals(name)) {
                            return (String) prop.getClass().getMethod("getValue").invoke(prop);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
