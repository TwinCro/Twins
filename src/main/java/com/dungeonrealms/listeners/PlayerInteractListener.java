package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.items.GameItem;
import com.dungeonrealms.items.ItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final DungeonRealms plugin;

    public PlayerInteractListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null && data.isSkillBarMode()) {
            int heldSlot = player.getInventory().getHeldItemSlot();
            if (heldSlot >= 0 && heldSlot <= 5) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        if (ClassSwitchListener.isSwitcherItem(item)) {
            event.setCancelled(true);
            new ClassSwitchListener(plugin).openSwitchMenu(player);
            return;
        }

        ItemManager im = plugin.getItemManager();
        GameItem gameItem = im.getGameItemFromStack(item);
        if (gameItem == null) return;

        // Dungeon key
        if (gameItem.getType() == com.dungeonrealms.items.ItemType.DUNGEON_KEY) {
            String dungeonId = gameItem.getDungeon();
            if (dungeonId != null && !dungeonId.isEmpty()) {
                event.setCancelled(true);
                boolean success = plugin.getDungeonManager().enterDungeon(player, dungeonId);
                if (success) {
                    // Consume the key
                    item.setAmount(item.getAmount() - 1);
                    player.sendMessage("§aDungeon key consumed.");
                }
            }
        }
    }
}
