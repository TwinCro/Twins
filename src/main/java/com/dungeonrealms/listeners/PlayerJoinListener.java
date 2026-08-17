package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final DungeonRealms plugin;

    public PlayerJoinListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().loadPlayer(event.getPlayer());
        plugin.getGuildManager().loadPlayerGuild(event.getPlayer());

        event.getPlayer().sendMessage("§6§lWelcome to DungeonRealms!");
        var data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer());
        if (data == null || !data.hasClass()) {
            event.getPlayer().sendMessage("§eYou haven't chosen a class yet! Use §6/class list §eto see available classes.");
            event.getPlayer().sendMessage("§eThen choose with §6/class choose <classname>");
        }
    }
}
