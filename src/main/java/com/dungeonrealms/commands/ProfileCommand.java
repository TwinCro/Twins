package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.classes.GameClass;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ProfileCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public ProfileCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length > 0 && sender.hasPermission("dungeonrealms.admin")) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            target = player;
        }

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null || !data.hasClass()) {
            sender.sendMessage("§cNo class data found. Use §e/class choose <class> §cfirst.");
            return true;
        }

        GameClass gc = plugin.getClassManager().getClass(data.getClassId());
        if (gc == null) {
            sender.sendMessage("§cClass not found: " + data.getClassId());
            return true;
        }

        String displayName = ChatColor.translateAlternateColorCodes('&', gc.getDisplayName());

        sender.sendMessage("§6§l╔══════════════════════════╗");
        sender.sendMessage("§6§l║   §e§l" + target.getName() + "'s Profile   §6§l║");
        sender.sendMessage("§6§l╚══════════════════════════╝");
        sender.sendMessage("§eClass: §f" + displayName);
        sender.sendMessage("§eLevel: §f" + data.getLevel() + "§7/" + plugin.getLevelManager().getMaxLevel());
        sender.sendMessage("§eXP: §f" + data.getXp() + "§7/" + plugin.getLevelManager().getXpForLevel(data.getLevel()));
        sender.sendMessage("§eMana: §f" + data.getMana() + "§7/" + data.getMaxMana());
        sender.sendMessage("§eGold: §6" + data.getGold());
        sender.sendMessage("§eAwakened: " + (data.isAwakened() ? "§aYes" : "§cNo"));
        if (data.isAwakened()) {
            sender.sendMessage("§eAwakening Count: §f" + data.getAwakeningCount());
        }
        sender.sendMessage("§eUnlocked Skills: §f" + data.getUnlockedSkills().size());
        sender.sendMessage("§eEquipped Skills: §f" + data.getEquippedSkills().size());
        sender.sendMessage("§eHomes: §f" + data.getHomeCount() + "§7/3");
        sender.sendMessage("§6§l═══════════════════════════");
        sender.sendMessage("§7Base Stats:");
        sender.sendMessage("§7  HP: §c" + (int) target.getHealth() + "§7/" + (int) target.getMaxHealth());
        sender.sendMessage("§7  Base Damage: §c" + (int) plugin.getClassManager().getBaseDamage(data));
        sender.sendMessage("§7  Base Defense: §c" + (int) plugin.getClassManager().getBaseDefense(data));
        sender.sendMessage("§6§l═══════════════════════════");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("dungeonrealms.admin")) {
            String prefix = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) completions.add(p.getName());
            }
        }
        return completions;
    }
}
