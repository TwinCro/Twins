package com.dungeonrealms.leveling;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.classes.GameClass;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.entity.Player;

public class AwakeningManager {

    private final DungeonRealms plugin;

    public AwakeningManager(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    public boolean canAwaken(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return false;
        if (!data.hasClass()) return false;
        if (data.isAwakened()) return false;

        int requiredLevel = plugin.getConfigManager().getMainConfig().getInt("awakening.required-level", 60);
        return data.getLevel() >= requiredLevel;
    }

    public boolean awaken(Player player) {
        if (!canAwaken(player)) {
            player.sendMessage("§cYou must be level 60 to awaken!");
            return false;
        }

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        GameClass currentClass = plugin.getClassManager().getClass(data.getClassId());
        if (currentClass == null) return false;

        String awakenedClassId = currentClass.getAwakenedClass();
        if (awakenedClassId == null || awakenedClassId.equalsIgnoreCase("none")) {
            player.sendMessage("§cYour class has no awakened form!");
            return false;
        }

        GameClass awakenedClass = plugin.getClassManager().getClass(awakenedClassId);
        if (awakenedClass == null) {
            player.sendMessage("§cAwakened class not found: " + awakenedClassId);
            return false;
        }

        // Perform awakening
        data.setAwakened(true);
        data.setAwakeningCount(data.getAwakeningCount() + 1);
        data.setClassId(awakenedClassId);
        data.setLevel(1);
        data.setXp(0);

        // Clear old skills and unlock new class skills at level 1
        data.getUnlockedSkills().clear();
        data.getEquippedSkills().clear();
        plugin.getSupabaseClient().deletePlayerSkills(player.getUniqueId());
        for (GameClass.ClassSkill cs : awakenedClass.getSkillsUpToLevel(1)) {
            data.getUnlockedSkills().add(cs.getSkillId());
        }

        // Apply new stats
        plugin.getClassManager().applyClassStats(player, data);

        player.sendMessage("§6§l========================================");
        player.sendMessage("§6§l         AWAKENING COMPLETE!         ");
        player.sendMessage("§eYour class has evolved into: " + awakenedClass.getDisplayName());
        player.sendMessage("§eYou are now level 1 with new powers!");
        player.sendMessage("§6§l========================================");

        return true;
    }
}
