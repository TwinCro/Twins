package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.leveling.AwakeningManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AwakenCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public AwakenCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        AwakeningManager am = new AwakeningManager(plugin);
        if (!am.canAwaken(player)) {
            player.sendMessage("§cYou must be level 60 and not yet awakened to use this command.");
            return true;
        }

        player.sendMessage("§6§lAre you sure you want to awaken?");
        player.sendMessage("§eThis will reset your level to 1 and transform your class.");
        player.sendMessage("§eType §6/awaken confirm §eto proceed.");

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            am.awaken(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && "confirm".startsWith(args[0].toLowerCase())) {
            completions.add("confirm");
        }
        return completions;
    }
}
