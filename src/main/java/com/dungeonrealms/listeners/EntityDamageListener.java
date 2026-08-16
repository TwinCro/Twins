package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.skills.Skill;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageListener implements Listener {

    private final DungeonRealms plugin;

    public EntityDamageListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;

        // Player attacking a mob
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity target) {
            PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(attacker);
            if (data == null || !data.hasClass()) {
                event.setDamage(1.0);
                return;
            }
            double damage = plugin.getDamageManager().calculatePlayerDamage(attacker);
            damage = plugin.getDamageManager().applyMobDefense(damage, Skill.DamageType.PHYSICAL, target);
            event.setDamage(damage);
        }

        // Mob attacking a player
        if (event.getEntity() instanceof Player victim && !(event.getDamager() instanceof Player)) {
            PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(victim);
            if (data == null || !data.hasClass()) {
                return;
            }
            double originalDamage = event.getDamage();
            boolean isMagicDamage = false;

            // Check if mob has custom damage tag
            if (event.getDamager() instanceof LivingEntity mob) {
                for (String tag : mob.getScoreboardTags()) {
                    if (tag.startsWith("dr_damage:")) {
                        try {
                            originalDamage = Double.parseDouble(tag.substring(10));
                        } catch (NumberFormatException ignored) {}
                    }
                    if (tag.startsWith("dr_magic_damage:")) {
                        try {
                            originalDamage = Double.parseDouble(tag.substring(16));
                            isMagicDamage = true;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            double defense = isMagicDamage
                    ? plugin.getDamageManager().calculatePlayerMagicDefense(victim)
                    : plugin.getDamageManager().calculatePlayerDefense(victim);

            double reducedDamage = originalDamage * (1.0 - defense / 100.0);
            event.setDamage(Math.max(0.5, reducedDamage));
        }
    }
}
