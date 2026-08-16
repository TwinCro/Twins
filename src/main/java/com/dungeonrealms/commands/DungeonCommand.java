package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.dungeon.DungeonConfig;
import com.dungeonrealms.dungeon.DungeonRank;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DungeonCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public DungeonCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            listDungeons(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listDungeons(player);
            case "enter" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /dungeon enter <name>");
                    return true;
                }
                plugin.getDungeonManager().enterDungeon(player, args[1].toLowerCase());
            }
            case "leave" -> plugin.getDungeonManager().leaveDungeon(player);
            default -> player.sendMessage("§cUsage: /dungeon [list | enter <name> | leave]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : Arrays.asList("list", "enter", "leave")) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("enter")) {
            String prefix = args[1].toLowerCase();
            for (DungeonConfig dg : plugin.getDungeonManager().getAllDungeons()) {
                if (dg.getId().toLowerCase().startsWith(prefix)) completions.add(dg.getId());
            }
        }
        return completions;
    }

    private void listDungeons(Player player) {
        player.sendMessage("§6§l=== Available Dungeons ===");
        for (DungeonConfig dg : plugin.getDungeonManager().getAllDungeons()) {
            DungeonRank rank = dg.getRank();
            player.sendMessage(rank.getColor() + ChatColor.translateAlternateColorCodes('&', dg.getDisplayName())
                    + " §7(" + rank.getDisplayName() + ") §eMin Level: " + dg.getMinLevel() + " §7Max Players: " + dg.getMaxPlayers());
        }
        player.sendMessage("§eUse §6/dungeon enter <name> §eor use a dungeon key to enter.");
    }
}
