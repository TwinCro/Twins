package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LevelCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public LevelCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && sender.hasPermission("dungeonrealms.admin")) {
            Player target = sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
            showLevel(sender, target);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        showLevel(player, player);
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

    private void showLevel(CommandSender sender, Player target) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null || !data.hasClass()) {
            sender.sendMessage("§cNo class data found.");
            return;
        }

        sender.sendMessage("§6§l=== " + target.getName() + "'s Level ===");
        sender.sendMessage("§eLevel: " + data.getLevel() + "/" + plugin.getLevelManager().getMaxLevel());
        sender.sendMessage("§eXP: " + data.getXp() + " / " + plugin.getLevelManager().getXpForLevel(data.getLevel()));
        sender.sendMessage("§eMana: " + data.getMana() + "/" + data.getMaxMana());
        sender.sendMessage("§eAwakened: " + (data.isAwakened() ? "§aYes" : "§cNo"));
    }
}
