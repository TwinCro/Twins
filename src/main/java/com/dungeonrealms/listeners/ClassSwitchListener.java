package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.classes.GameClass;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.api.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ClassSwitchListener implements Listener {

    private final DungeonRealms plugin;

    public ClassSwitchListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    public void openSwitchMenu(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage("§cYou need a class first. Use §e/class choose <class>");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6§lSwitch Class");

        int slot = 0;
        for (GameClass gc : plugin.getClassManager().getBaseClasses()) {
            ItemStack item = new ItemStack(getClassMaterial(gc.getId()));
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            String name = ChatColor.translateAlternateColorCodes('&', gc.getDisplayName());
            boolean isCurrent = gc.getId().equals(data.getClassId());

            meta.setDisplayName((isCurrent ? "§a§l[ACTIVE] " : "§e") + name);
            List<String> lore = new ArrayList<>();
            lore.add("§7" + gc.getDescription());
            lore.add("");
            lore.add("§7HP: §c" + gc.getMaxHealth() + " §7| DMG: §c" + gc.getBaseDamage()
                    + " §7| DEF: §9" + gc.getBaseDefense() + " §7| MP: §b" + gc.getBaseMana());
            lore.add("");

            if (isCurrent) {
                lore.add("§aThis is your current class.");
            } else {
                lore.add("§eClick to switch to this class.");
                lore.add("§7Your current class progress will be saved.");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lSwitch Class")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClick() != ClickType.LEFT) return;

        int clickedSlot = event.getSlot();
        List<GameClass> baseClasses = new ArrayList<>(plugin.getClassManager().getBaseClasses());
        if (clickedSlot < 0 || clickedSlot >= baseClasses.size()) return;

        GameClass targetClass = baseClasses.get(clickedSlot);
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return;

        if (targetClass.getId().equals(data.getClassId())) {
            player.sendMessage("§eYou are already this class.");
            player.closeInventory();
            return;
        }

        switchClass(player, data, targetClass.getId());
        player.closeInventory();
    }

    private void switchClass(Player player, PlayerDataManager.PlayerData data, String newClassId) {
        GameClass newGc = plugin.getClassManager().getClass(newClassId);
        if (newGc == null) {
            player.sendMessage("§cClass not found.");
            return;
        }

        String oldClassId = data.getClassId();
        int oldLevel = data.getLevel();
        long oldXp = data.getXp();

        JsonArray oldSkillsJson = new JsonArray();
        for (String skillId : data.getUnlockedSkills()) {
            JsonObject skillObj = new JsonObject();
            skillObj.addProperty("skill_id", skillId);
            skillObj.addProperty("equipped", data.getEquippedSkills().contains(skillId));
            Integer slot = null;
            for (var entry : data.getSkillSlots().entrySet()) {
                if (entry.getValue().equals(skillId)) {
                    slot = entry.getKey();
                    break;
                }
            }
            if (slot != null) {
                skillObj.addProperty("skill_slot", slot);
            }
            oldSkillsJson.add(skillObj);
        }

        SupabaseClient supabase = plugin.getSupabaseClient();
        supabase.upsertClassProgress(player.getUniqueId(), oldClassId, oldLevel, oldXp, oldSkillsJson.toString());

        supabase.getClassProgress(player.getUniqueId(), newClassId).thenAccept(progressJson -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (progressJson != null) {
                    int savedLevel = progressJson.has("level") ? progressJson.get("level").getAsInt() : 1;
                    long savedXp = progressJson.has("xp") ? progressJson.get("xp").getAsLong() : 0;

                    data.setLevel(savedLevel);
                    data.setXp(savedXp);

                    data.getUnlockedSkills().clear();
                    data.getEquippedSkills().clear();
                    data.getSkillSlots().clear();

                    if (progressJson.has("skills") && !progressJson.get("skills").isJsonNull()) {
                        JsonArray skillsArray = JsonParser.parseString(progressJson.get("skills").getAsString()).getAsJsonArray();
                        for (int i = 0; i < skillsArray.size(); i++) {
                            JsonObject skillObj = skillsArray.get(i).getAsJsonObject();
                            String skillId = skillObj.get("skill_id").getAsString();
                            boolean equipped = skillObj.has("equipped") && skillObj.get("equipped").getAsBoolean();
                            data.getUnlockedSkills().add(skillId);
                            if (equipped) data.getEquippedSkills().add(skillId);
                            if (skillObj.has("skill_slot") && !skillObj.get("skill_slot").isJsonNull()) {
                                int slot = skillObj.get("skill_slot").getAsInt();
                                if (slot >= 1 && slot <= 6) {
                                    data.getSkillSlots().put(slot, skillId);
                                }
                            }
                        }
                    }
                } else {
                    data.setLevel(1);
                    data.setXp(0);
                    data.getUnlockedSkills().clear();
                    data.getEquippedSkills().clear();
                    data.getSkillSlots().clear();

                    for (GameClass.ClassSkill cs : newGc.getSkillsUpToLevel(1)) {
                        data.getUnlockedSkills().add(cs.getSkillId());
                    }
                }

                data.setClassId(newClassId);
                plugin.getClassManager().applyClassStats(player, data);
                data.setSkillBarMode(true);
                plugin.getPlayerDataManager().savePlayer(player.getUniqueId());

                player.sendMessage("§a§lClass switched to " + ChatColor.translateAlternateColorCodes('&', newGc.getDisplayName()) + "!");
                player.sendMessage("§7Your §e" + oldClassId + " §7progress (level " + oldLevel + ") has been saved.");
                player.sendMessage("§7Use the class switcher item again to switch back anytime.");
            });
        });
    }

    public static ItemStack createSwitcherItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lClass Switcher");
            List<String> lore = new ArrayList<>();
            lore.add("§7Right-click to open the class switcher menu.");
            lore.add("§7Switch classes without losing progress!");
            lore.add("");
            lore.add("§8dr_class_switcher");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSwitcherItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return false;
        return lore.get(lore.size() - 1).equals("§8dr_class_switcher");
    }

    private Material getClassMaterial(String classId) {
        return switch (classId.toLowerCase()) {
            case "warrior" -> Material.IRON_SWORD;
            case "mage" -> Material.BLAZE_ROD;
            case "archer" -> Material.BOW;
            case "tank" -> Material.IRON_CHESTPLATE;
            case "assassin" -> Material.IRON_SWORD;
            default -> Material.NETHER_STAR;
        };
    }
}
