package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.items.GameItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemsCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public ItemsCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /items [info | give <player> <itemid>]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                GameItem gi = plugin.getItemManager().getGameItemFromStack(hand);
                if (gi == null) {
                    player.sendMessage("§cHold a DungeonRealms item in your hand.");
                    return true;
                }
                player.sendMessage("§6§l=== Item Info ===");
                player.sendMessage("§eID: " + gi.getId());
                player.sendMessage("§eName: " + ChatColor.translateAlternateColorCodes('&', gi.getDisplayName()));
                player.sendMessage("§eRarity: " + gi.getRarity().getDisplayName());
                player.sendMessage("§eType: " + gi.getType());
                if (gi.getDamage() > 0) player.sendMessage("§cDamage: " + gi.getDamage());
                if (gi.getDefense() > 0) player.sendMessage("§9Defense: " + gi.getDefense());
                if (gi.getHealth() > 0) player.sendMessage("§aHealth: " + gi.getHealth());
                if (gi.getMana() > 0) player.sendMessage("§bMana: " + gi.getMana());
                if (gi.getCritChance() > 0) player.sendMessage("§eCrit Chance: " + gi.getCritChance() + "%");
                if (gi.getCritDamage() > 0) player.sendMessage("§eCrit Damage: " + gi.getCritDamage() + "%");
            }
            case "give" -> {
                if (!player.hasPermission("dungeonrealms.admin")) {
                    player.sendMessage("§cYou don't have permission.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /items give <player> <itemid>");
                    return true;
                }
                Player target = player.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }
                ItemStack item = plugin.getItemManager().createItemStack(args[2].toLowerCase());
                if (item == null) {
                    player.sendMessage("§cItem not found: " + args[2]);
                    return true;
                }
                target.getInventory().addItem(item);
                player.sendMessage("§aGave " + args[2] + " to " + target.getName());
            }
            default -> player.sendMessage("§cUsage: /items [info | give <player> <itemid>]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : Arrays.asList("info", "give")) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[2].toLowerCase();
            for (String itemId : plugin.getItemManager().getAllItemIds()) {
                if (itemId.startsWith(prefix)) completions.add(itemId);
            }
        }
        return completions;
    }
}
