package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public PartyCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            plugin.getPartyManager().listParty(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found or offline.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage("§cYou cannot invite yourself.");
                    return true;
                }
                plugin.getPartyManager().invitePlayer(player, target);
            }
            case "accept" -> plugin.getPartyManager().acceptInvite(player);
            case "leave" -> plugin.getPartyManager().leaveParty(player);
            case "list" -> plugin.getPartyManager().listParty(player);
            case "disband" -> {
                if (plugin.getPartyManager().getParty(player) != null) {
                    plugin.getPartyManager().disbandParty(player);
                } else {
                    player.sendMessage("§cYou are not a party leader.");
                }
            }
            default -> player.sendMessage("§cUsage: /party [invite <player> | accept | leave | list | disband]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : Arrays.asList("invite", "accept", "leave", "list", "disband")) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            String prefix = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) completions.add(p.getName());
            }
        }
        return completions;
    }
}
