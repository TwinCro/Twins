package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DRAdminCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public DRAdminCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dungeonrealms.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /dradmin [reload | setlevel <player> <level> | giveitem <player> <itemid>]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reloadConfigs();
                plugin.getClassManager().loadClasses();
                plugin.getSkillManager().loadSkills();
                plugin.getItemManager().loadItems();
                plugin.getDungeonManager().loadDungeons();
                plugin.getSkillRegionManager().loadRegions();
                sender.sendMessage("§aDungeonRealms config reloaded.");
            }
            case "setlevel" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /dradmin setlevel <player> <level>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                int level = Integer.parseInt(args[2]);
                PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
                if (data == null || !data.hasClass()) {
                    sender.sendMessage("§cPlayer has no class data.");
                    return true;
                }
                data.setLevel(level);
                data.setXp(0);

                data.getUnlockedSkills().clear();
                data.getEquippedSkills().clear();
                data.getSkillSlots().clear();
                var gc = plugin.getClassManager().getClass(data.getClassId());
                if (gc != null) {
                    for (var cs : gc.getSkillsUpToLevel(level)) {
                        data.getUnlockedSkills().add(cs.getSkillId());
                    }
                }

                plugin.getClassManager().applyClassStats(target, data);
                plugin.getPlayerDataManager().savePlayer(target.getUniqueId());
                sender.sendMessage("§aSet " + target.getName() + " to level " + level + " and unlocked all skills up to that level.");
            }
            case "giveitem" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /dradmin giveitem <player> <itemid>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.");
                    return true;
                }
                ItemStack item = plugin.getItemManager().createItemStack(args[2].toLowerCase());
                if (item == null) {
                    sender.sendMessage("§cItem not found: " + args[2]);
                    return true;
                }
                target.getInventory().addItem(item);
                sender.sendMessage("§aGave " + args[2] + " to " + target.getName());
            }
            default -> sender.sendMessage("§cUsage: /dradmin [reload | setlevel <player> <level> | giveitem <player> <itemid>]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : Arrays.asList("reload", "setlevel", "giveitem")) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setlevel") || sub.equals("giveitem")) {
                String prefix = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(prefix)) completions.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("giveitem")) {
            String prefix = args[2].toLowerCase();
            for (String itemId : plugin.getItemManager().getAllItemIds()) {
                if (itemId.startsWith(prefix)) completions.add(itemId);
            }
        }
        return completions;
    }
}
