package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryClickListener implements Listener {

    private final DungeonRealms plugin;

    public InventoryClickListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§6§lSwitch Class")) {
            event.setCancelled(true);
        }
    }
}
