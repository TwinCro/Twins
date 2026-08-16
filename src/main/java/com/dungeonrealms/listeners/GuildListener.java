package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GuildListener implements Listener {

    private final DungeonRealms plugin;

    public GuildListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    private boolean canInteract(Player player, Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        int cx = chunk.getX();
        int cz = chunk.getZ();

        if (!plugin.getClaimManager().isClaimed(worldName, cx, cz)) {
            return true;
        }

        return plugin.getClaimManager().isPlayerInClaimingGuild(player.getUniqueId(), worldName, cx, cz);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (canInteract(event.getPlayer(), event.getBlock().getChunk())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cThis land is claimed by another guild. You cannot break blocks here.");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (canInteract(event.getPlayer(), event.getBlock().getChunk())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cThis land is claimed by another guild. You cannot place blocks here.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (canInteract(player, event.getClickedBlock().getChunk())) return;
        event.setCancelled(true);
        player.sendMessage("§cThis land is claimed by another guild. You cannot interact here.");
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;

        Chunk chunk = event.getEntity().getLocation().getChunk();
        if (canInteract(player, chunk)) return;

        event.setCancelled(true);
        player.sendMessage("§cThis land is claimed by another guild. You cannot attack mobs here.");
    }
}
