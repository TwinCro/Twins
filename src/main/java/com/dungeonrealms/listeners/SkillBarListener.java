package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.skills.Skill;
import com.dungeonrealms.skills.SkillExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class SkillBarListener implements Listener {

    private final DungeonRealms plugin;

    public SkillBarListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !data.hasClass()) return;

        event.setCancelled(true);
        data.setSkillBarMode(!data.isSkillBarMode());

        if (data.isSkillBarMode()) {
            player.sendMessage("§a§lSkill Bar Mode: §aON §7- Press §e1-6 §7to use bound skills. Press §eF §7to turn off.");
        } else {
            player.sendMessage("§cSkill Bar Mode: OFF §7- Normal hotbar active.");
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !data.isSkillBarMode()) return;

        int newSlot = event.getNewSlot();
        if (newSlot < 0 || newSlot > 5) return;

        int skillSlot = newSlot + 1;
        String skillId = data.getSkillSlots().get(skillSlot);
        if (skillId == null) {
            return;
        }

        Skill skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) return;

        event.setCancelled(true);

        SkillExecutor executor = new SkillExecutor(plugin);
        executor.executeSkill(player, skillId);
    }
}
