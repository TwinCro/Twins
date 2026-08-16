package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuildCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public GuildCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use guild commands.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /guild create <name>");
                    return true;
                }
                plugin.getGuildManager().createGuild(player, args[1]);
            }
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /guild invite <player>");
                    return true;
                }
                plugin.getGuildManager().invitePlayer(player, args[1]);
            }
            case "accept" -> plugin.getGuildManager().acceptInvite(player);
            case "leave" -> plugin.getGuildManager().leaveGuild(player);
            case "disband" -> plugin.getGuildManager().disbandGuild(player);
            case "promote" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /guild promote <player>");
                    return true;
                }
                plugin.getGuildManager().promotePlayer(player, args[1]);
            }
            case "demote" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /guild demote <player>");
                    return true;
                }
                plugin.getGuildManager().demotePlayer(player, args[1]);
            }
            case "claim" -> plugin.getGuildManager().claimChunk(player);
            case "unclaim" -> plugin.getGuildManager().unclaimChunk(player);
            case "expand" -> plugin.getGuildManager().expandClaim(player);
            case "info" -> plugin.getGuildManager().showGuildInfo(player);
            case "sethome" -> plugin.getGuildManager().setGuildHome(player);
            case "home" -> plugin.getGuildManager().teleportGuildHome(player);
            case "delhome" -> plugin.getGuildManager().delGuildHome(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l======== Guild Commands ========");
        player.sendMessage("§e/guild create <name> §7- Create a new guild");
        player.sendMessage("§e/guild invite <player> §7- Invite a player (leader only)");
        player.sendMessage("§e/guild accept §7- Accept a pending invite");
        player.sendMessage("§e/guild leave §7- Leave your current guild");
        player.sendMessage("§e/guild disband §7- Disband your guild (leader only)");
        player.sendMessage("§e/guild promote <player> §7- Promote to officer (leader only)");
        player.sendMessage("§e/guild demote <player> §7- Demote to member (leader only)");
        player.sendMessage("§e/guild claim §7- Claim the chunk you're standing in");
        player.sendMessage("§e/guild unclaim §7- Unclaim the chunk you're standing in");
        player.sendMessage("§e/guild expand §7- Buy +1 claim slot (costs gold)");
        player.sendMessage("§e/guild info §7- View your guild info");
        player.sendMessage("§e/guild sethome §7- Set guild home (leader/officer)");
        player.sendMessage("§e/guild home §7- Teleport to guild home");
        player.sendMessage("§e/guild delhome §7- Remove guild home (leader/officer)");
        player.sendMessage("§6§l================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "invite", "accept", "leave", "disband",
                    "promote", "demote", "claim", "unclaim", "expand", "info",
                    "sethome", "home", "delhome");
        }
        return new ArrayList<>();
    }
}
