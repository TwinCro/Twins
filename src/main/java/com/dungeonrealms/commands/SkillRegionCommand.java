package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.skills.SkillRegionManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillRegionCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public SkillRegionCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dungeonrealms.admin")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /skillregion [create <name> <radius> | createbox <name> | list | remove <name> | reload]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can create regions.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /skillregion create <name> <radius>");
                    return true;
                }
                String name = args[1];
                double radius;
                try {
                    radius = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cRadius must be a number.");
                    return true;
                }
                if (radius < 1 || radius > 500) {
                    sender.sendMessage("§cRadius must be between 1 and 500.");
                    return true;
                }
                Location loc = player.getLocation();
                String world = loc.getWorld().getName();
                double minX = loc.getX() - radius;
                double minY = loc.getY() - radius;
                double minZ = loc.getZ() - radius;
                double maxX = loc.getX() + radius;
                double maxY = loc.getY() + radius;
                double maxZ = loc.getZ() + radius;
                plugin.getSkillRegionManager().addRegion(name, world, minX, minY, minZ, maxX, maxY, maxZ);
                sender.sendMessage("§aCreated skill region §e" + name + " §a(" + world + ", radius " + radius + ")");
            }
            case "createbox" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can create regions.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /skillregion createbox <name>  (stand at corner 1, then use /skillregion createbox2 <name>)");
                    return true;
                }
                String name = args[1];
                Location loc = player.getLocation();
                player.setMetadata("dr_sr_corner1_" + name.toLowerCase(), new org.bukkit.metadata.FixedMetadataValue(plugin, loc));
                sender.sendMessage("§aCorner 1 set for region §e" + name + "§a. Now go to the opposite corner and use §e/skillregion createbox2 " + name);
            }
            case "createbox2" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can create regions.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /skillregion createbox2 <name>");
                    return true;
                }
                String name = args[1];
                String key = "dr_sr_corner1_" + name.toLowerCase();
                if (!player.hasMetadata(key)) {
                    sender.sendMessage("§cCorner 1 not set. Use §e/skillregion createbox " + name + " §cfirst.");
                    return true;
                }
                Location corner1 = (Location) player.getMetadata(key).get(0).value();
                player.removeMetadata(key, plugin);
                Location corner2 = player.getLocation();
                String world = corner2.getWorld().getName();
                plugin.getSkillRegionManager().addRegion(name, world,
                        corner1.getX(), corner1.getY(), corner1.getZ(),
                        corner2.getX(), corner2.getY(), corner2.getZ());
                sender.sendMessage("§aCreated skill region §e" + name + " §a(box from corner 1 to your current position)");
            }
            case "list" -> {
                sender.sendMessage("§6§l=== Skill Regions ===");
                for (SkillRegionManager.SkillRegion region : plugin.getSkillRegionManager().getAllRegions()) {
                    sender.sendMessage("§e" + region.name + " §7- " + region.worldName
                            + " (" + (int) region.minX + "," + (int) region.minY + "," + (int) region.minZ
                            + " to " + (int) region.maxX + "," + (int) region.maxY + "," + (int) region.maxZ + ")");
                }
                if (plugin.getSkillRegionManager().getAllRegions().isEmpty()) {
                    sender.sendMessage("§7No skill regions defined.");
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /skillregion remove <name>");
                    return true;
                }
                if (plugin.getSkillRegionManager().removeRegion(args[1])) {
                    sender.sendMessage("§aRemoved skill region: §e" + args[1]);
                } else {
                    sender.sendMessage("§cRegion not found: " + args[1]);
                }
            }
            case "reload" -> {
                plugin.getSkillRegionManager().loadRegions();
                sender.sendMessage("§aSkill regions reloaded.");
            }
            default -> sender.sendMessage("§cUsage: /skillregion [create <name> <radius> | createbox <name> | list | remove <name> | reload]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : Arrays.asList("create", "createbox", "createbox2", "list", "remove", "reload")) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            String prefix = args[1].toLowerCase();
            for (SkillRegionManager.SkillRegion region : plugin.getSkillRegionManager().getAllRegions()) {
                if (region.name.toLowerCase().startsWith(prefix)) completions.add(region.name);
            }
        }
        return completions;
    }
}
