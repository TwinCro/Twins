package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final DungeonRealms plugin;

    public PlayerQuitListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unloadPlayer(event.getPlayer().getUniqueId());
        plugin.getGuildManager().unloadPlayerGuild(event.getPlayer().getUniqueId());
    }
}
