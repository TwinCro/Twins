package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.listeners.ClassSwitchListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClassSwitchCommand implements CommandExecutor {

    private final DungeonRealms plugin;

    public ClassSwitchCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        var data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage("§cYou need a class first. Use §e/class choose <class>");
            return true;
        }

        player.getInventory().addItem(ClassSwitchListener.createSwitcherItem());
        player.sendMessage("§aYou received a §6Class Switcher§a! Right-click it to switch classes.");
        return true;
    }
}
