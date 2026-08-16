package com.dungeonrealms.listeners;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.dungeon.DungeonInstance;
import com.dungeonrealms.dungeon.DungeonRoom;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final DungeonRealms plugin;

    public EntityDeathListener(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        String dungeonTag = null;
        int roomIndex = -1;
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith("dr_dungeon:")) {
                dungeonTag = tag.substring("dr_dungeon:".length());
            } else if (tag.startsWith("dr_room:")) {
                roomIndex = Integer.parseInt(tag.substring("dr_room:".length()));
            }
        }

        if (dungeonTag != null && roomIndex >= 0) {
            for (DungeonInstance inst : plugin.getDungeonManager().getActiveInstances()) {
                if (inst.getConfig().getId().equals(dungeonTag)) {
                    plugin.getDungeonManager().onRoomMobDeath(inst, roomIndex);
                    break;
                }
            }

            Player killer = entity.getKiller();
            if (killer != null) {
                PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(killer);
                if (data != null && data.hasClass()) {
                    long xp = (long) (entity.getMaxHealth() / 5.0);
                    plugin.getLevelManager().addXp(killer, xp);
                    long gold = (long) (entity.getMaxHealth() / 20.0);
                    if (gold > 0) {
                        data.addGold(gold);
                        plugin.getSupabaseClient().updatePlayerGold(killer.getUniqueId(), data.getGold());
                    }
                }
            }
            event.setDroppedExp(0);
            event.getDrops().clear();
            return;
        }

        DungeonInstance instance = plugin.getDungeonManager().getInstanceByBoss(entity);
        if (instance != null) {
            plugin.getDungeonManager().onBossDeath(instance);
            event.setDroppedExp(0);
            event.getDrops().clear();
            return;
        }

        Player killer = entity.getKiller();
        if (killer != null) {
            PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(killer);
            if (data != null && data.hasClass()) {
                long xp = getMobXp(entity);
                plugin.getLevelManager().addXp(killer, xp);

                long gold = getMobGold(entity);
                if (gold > 0) {
                    data.addGold(gold);
                    plugin.getSupabaseClient().updatePlayerGold(killer.getUniqueId(), data.getGold());
                }
            }
        }
    }

    private long getMobXp(LivingEntity entity) {
        String type = entity.getType().name();
        var config = plugin.getConfigManager().getMainConfig()
                .getConfigurationSection("leveling.xp-sources.mob-kill");
        if (config == null) return 15;

        if (entity.getScoreboardTags().contains("dr_mob") || entity.getScoreboardTags().contains("dr_boss")) {
            double maxHp = entity.getMaxHealth();
            long baseXp = config.getLong("boss", 1000);
            if (entity.getScoreboardTags().contains("dr_boss")) {
                return baseXp + (long)(maxHp / 10.0);
            }
            return (long)(maxHp / 5.0);
        }

        if (entity.getCustomName() != null && entity.getCustomName().contains("boss")) {
            return config.getLong("boss", 1000);
        }

        switch (type) {
            case "ZOMBIE", "SKELETON", "SPIDER", "CAVE_SPIDER", "CREEPER", "ENDERMAN",
                 "BLAZE", "WITCH", "WITHER_SKELETON", "PHANTOM", "SHULKER":
                return config.getLong("hostile", 15);
            default:
                return config.getLong("passive", 8);
        }
    }

    private long getMobGold(LivingEntity entity) {
        var config = plugin.getConfigManager().getMainConfig()
                .getConfigurationSection("guild.gold-sources.mob-kill");
        if (config == null) return 0;

        if (entity.getScoreboardTags().contains("dr_boss")) {
            long base = config.getLong("boss", 200);
            return base + (long)(entity.getMaxHealth() / 50.0);
        }
        if (entity.getScoreboardTags().contains("dr_mob")) {
            return (long)(entity.getMaxHealth() / 20.0);
        }

        String type = entity.getType().name();
        switch (type) {
            case "ZOMBIE", "SKELETON", "SPIDER", "CAVE_SPIDER", "CREEPER", "ENDERMAN",
                 "BLAZE", "WITCH", "WITHER_SKELETON", "PHANTOM", "SHULKER":
                return config.getLong("hostile", 5);
            default:
                return config.getLong("passive", 2);
        }
    }
}
