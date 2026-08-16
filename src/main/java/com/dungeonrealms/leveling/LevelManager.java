package com.dungeonrealms.leveling;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LevelManager {

    private final DungeonRealms plugin;
    private final int maxLevel;
    private final double baseXp;
    private final double xpMultiplier;

    public LevelManager(DungeonRealms plugin) {
        this.plugin = plugin;
        this.maxLevel = plugin.getConfigManager().getMainConfig().getInt("leveling.max-level", 60);
        this.baseXp = plugin.getConfigManager().getMainConfig().getDouble("leveling.base-xp", 100);
        this.xpMultiplier = plugin.getConfigManager().getMainConfig().getDouble("leveling.xp-multiplier", 1.15);
    }

    public int getMaxLevel() { return maxLevel; }

    public long getXpForLevel(int level) {
        return (long) Math.floor(baseXp * Math.pow(xpMultiplier, level - 1));
    }

    public long getTotalXpForLevel(int level) {
        long total = 0;
        for (int i = 1; i < level; i++) {
            total += getXpForLevel(i);
        }
        return total;
    }

    public void addXp(Player player, long amount) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !data.hasClass()) return;
        if (data.getLevel() >= maxLevel) return;

        data.addXp(amount);
        checkLevelUp(player, data);
    }

    private void checkLevelUp(Player player, PlayerDataManager.PlayerData data) {
        while (data.getLevel() < maxLevel) {
            long xpNeeded = getXpForLevel(data.getLevel());
            if (data.getXp() < xpNeeded) break;

            data.setXp(data.getXp() - xpNeeded);
            data.setLevel(data.getLevel() + 1);
            onLevelUp(player, data);
        }

        if (data.getLevel() >= maxLevel) {
            data.setXp(0);
            player.sendMessage("§6§lYou have reached the maximum level! §eUse /awaken to awaken your class.");
        }
    }

    private void onLevelUp(Player player, PlayerDataManager.PlayerData data) {
        int newLevel = data.getLevel();
        plugin.getClassManager().applyClassStats(player, data);

        player.sendMessage("§a§lLevel Up! §eYou are now level " + newLevel);

        // Unlock skills at this level
        var gc = plugin.getClassManager().getClass(data.getClassId());
        if (gc != null) {
            for (var cs : gc.getSkills()) {
                if (cs.getUnlockLevel() == newLevel && !data.getUnlockedSkills().contains(cs.getSkillId())) {
                    data.getUnlockedSkills().add(cs.getSkillId());
                    player.sendMessage("§bNew skill unlocked: §3" + cs.getSkillId());
                }
            }
        }
    }

    public void startManaRegenTask() {
        int interval = plugin.getConfigManager().getMainConfig()
                .getInt("mana.regen-interval-seconds", 3) * 20;
        int regenAmount = plugin.getConfigManager().getMainConfig()
                .getInt("mana.base-regen", 2);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                if (data != null && data.hasClass()) {
                    data.addMana(regenAmount);
                }
            }
        }, interval, interval);
    }
}
