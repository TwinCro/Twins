package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.data.PlayerDataManager.HomeLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_HOMES = 3;
    private final DungeonRealms plugin;

    public HomeCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "sethome" -> {
                String name = args.length > 0 ? args[0].toLowerCase() : "home";
                setHome(player, name);
            }
            case "delhome" -> {
                String name = args.length > 0 ? args[0].toLowerCase() : "home";
                delHome(player, name);
            }
            case "home" -> {
                String name = args.length > 0 ? args[0].toLowerCase() : "home";
                goHome(player, name);
            }
            case "homes" -> listHomes(player);
            default -> goHome(player, "home");
        }

        return true;
    }

    private void setHome(Player player, String name) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            player.sendMessage("§cYour data hasn't loaded yet. Try again in a moment.");
            return;
        }

        if (!name.matches("[a-zA-Z0-9_]+") || name.length() > 16) {
            player.sendMessage("§cHome name must be 1-16 characters (letters, numbers, underscores).");
            return;
        }

        if (!data.hasHome(name) && data.getHomeCount() >= MAX_HOMES) {
            player.sendMessage("§cYou've reached the maximum of §e" + MAX_HOMES + " §chomes.");
            player.sendMessage("§7Delete one with §e/delhome <name> §7first.");
            return;
        }

        Location loc = player.getLocation();
        data.setHome(name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        plugin.getSupabaseClient().upsertPlayerHome(player.getUniqueId(), name,
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        player.sendMessage("§a§lHome '" + name + "' set! §7Use §e/home " + name + " §7to teleport here.");
        player.sendMessage("§7Location: §f" + loc.getWorld().getName() + " (" +
                (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + ")");
        player.sendMessage("§7Homes: §e" + data.getHomeCount() + "§7/§e" + MAX_HOMES);
    }

    private void delHome(Player player, String name) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            player.sendMessage("§cYour data hasn't loaded yet. Try again in a moment.");
            return;
        }

        if (!data.hasHome(name)) {
            player.sendMessage("§cYou don't have a home named '§e" + name + "§c'.");
            if (data.getHomeCount() > 0) {
                player.sendMessage("§7Your homes: §e" + String.join(", ", data.getHomes().keySet()));
            }
            return;
        }

        data.removeHome(name);
        plugin.getSupabaseClient().deletePlayerHome(player.getUniqueId(), name);
        player.sendMessage("§aHome '§e" + name + "§a' removed.");
    }

    private void goHome(Player player, String name) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            player.sendMessage("§cYour data hasn't loaded yet. Try again in a moment.");
            return;
        }

        HomeLocation home = data.getHome(name);
        if (home == null) {
            if (data.getHomeCount() == 0) {
                player.sendMessage("§cYou don't have any homes set. Use §e/sethome <name> §cto set one.");
            } else {
                player.sendMessage("§cNo home named '§e" + name + "§c'.");
                player.sendMessage("§7Your homes: §e" + String.join(", ", data.getHomes().keySet()));
            }
            return;
        }

        World world = Bukkit.getWorld(home.world);
        if (world == null) {
            player.sendMessage("§cThe world your home is in no longer exists.");
            return;
        }

        Location homeLoc = new Location(world, home.x, home.y, home.z, home.yaw, home.pitch);
        player.teleport(homeLoc);
        player.sendMessage("§a§lTeleported to home '" + name + "'!");
    }

    private void listHomes(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) {
            player.sendMessage("§cYour data hasn't loaded yet. Try again in a moment.");
            return;
        }

        if (data.getHomeCount() == 0) {
            player.sendMessage("§cYou don't have any homes set. Use §e/sethome <name> §cto set one.");
            return;
        }

        player.sendMessage("§6§l======== Your Homes (" + data.getHomeCount() + "/" + MAX_HOMES + ") ========");
        for (Map.Entry<String, HomeLocation> entry : data.getHomes().entrySet()) {
            HomeLocation h = entry.getValue();
            player.sendMessage("§e" + entry.getKey() + " §7- §f" + h.world + " (" +
                    (int) h.x + ", " + (int) h.y + ", " + (int) h.z + ")");
        }
        player.sendMessage("§7Use §e/home <name> §7to teleport.");
        player.sendMessage("§6§l===================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
            if (data != null) {
                return new ArrayList<>(data.getHomes().keySet());
            }
        }
        return new ArrayList<>();
    }
}
