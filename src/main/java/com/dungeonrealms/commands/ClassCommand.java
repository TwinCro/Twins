package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.classes.GameClass;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public ClassCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            showInfo(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listClasses(player);
            case "choose" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /class choose <classname>");
                    return true;
                }
                chooseClass(player, args[1].toLowerCase());
            }
            case "info" -> showInfo(player);
            default -> player.sendMessage("§cUsage: /class [list | choose <class> | info]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : Arrays.asList("list", "choose", "info")) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("choose")) {
            String prefix = args[1].toLowerCase();
            for (GameClass gc : plugin.getClassManager().getBaseClasses()) {
                if (gc.getId().startsWith(prefix)) completions.add(gc.getId());
            }
        }
        return completions;
    }

    private void listClasses(Player player) {
        player.sendMessage("§6§l=== Available Classes ===");
        for (GameClass gc : plugin.getClassManager().getBaseClasses()) {
            player.sendMessage("§e" + gc.getId() + " §7- " + ChatColor.translateAlternateColorCodes('&', gc.getDisplayName()));
            player.sendMessage("§7  " + gc.getDescription());
            player.sendMessage("§7  HP: " + gc.getMaxHealth() + " | DMG: " + gc.getBaseDamage() + " | DEF: " + gc.getBaseDefense() + " | MP: " + gc.getBaseMana());
        }
    }

    private void chooseClass(Player player, String classId) {
        GameClass gc = plugin.getClassManager().getClass(classId);
        if (gc == null) {
            player.sendMessage("§cClass not found: " + classId);
            player.sendMessage("§eUse /class list to see available classes.");
            return;
        }
        if (gc.isAwakened()) {
            player.sendMessage("§cYou cannot choose an awakened class directly. You must awaken at level 60.");
            return;
        }

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getOrCreatePlayerData(player);
        if (data == null) {
            player.sendMessage("§cLoading your data, please try again.");
            return;
        }
        if (data.hasClass() && !data.isAwakened()) {
            player.sendMessage("§cYou already have a class: " + data.getClassId());
            player.sendMessage("§eUse the §6Class Switcher §eitem to switch classes, or §e/awaken §eat level 60 to evolve.");
            return;
        }

        data.setClassId(classId);
        data.setLevel(1);
        data.setXp(0);

        data.getUnlockedSkills().clear();
        data.getEquippedSkills().clear();
        for (GameClass.ClassSkill cs : gc.getSkillsUpToLevel(1)) {
            data.getUnlockedSkills().add(cs.getSkillId());
        }

        plugin.getClassManager().applyClassStats(player, data);
        data.setSkillBarMode(true);
        plugin.getPlayerDataManager().savePlayer(player.getUniqueId());

        player.getInventory().addItem(com.dungeonrealms.listeners.ClassSwitchListener.createSwitcherItem());

        player.sendMessage("§a§lYou have chosen: " + ChatColor.translateAlternateColorCodes('&', gc.getDisplayName()));
        player.sendMessage("§7" + gc.getDescription());
        player.sendMessage("§7You received a §6Class Switcher§7! Right-click it to switch classes anytime.");
    }

    private void showInfo(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getOrCreatePlayerData(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage("§cYou haven't chosen a class yet. Use §e/class list §cto see available classes.");
            return;
        }

        GameClass gc = plugin.getClassManager().getClass(data.getClassId());
        if (gc == null) return;

        player.sendMessage("§6§l=== Class Info ===");
        player.sendMessage("§eClass: " + ChatColor.translateAlternateColorCodes('&', gc.getDisplayName()));
        player.sendMessage("§eLevel: " + data.getLevel() + "/" + plugin.getLevelManager().getMaxLevel());
        player.sendMessage("§eXP: " + data.getXp() + "/" + plugin.getLevelManager().getXpForLevel(data.getLevel()));
        player.sendMessage("§eMana: " + data.getMana() + "/" + data.getMaxMana());
        player.sendMessage("§eAwakened: " + (data.isAwakened() ? "§aYes" : "§cNo"));
        if (data.isAwakened()) {
            player.sendMessage("§eAwakening Count: " + data.getAwakeningCount());
        }
        player.sendMessage("§eUnlocked Skills: " + data.getUnlockedSkills().size());
    }
}
