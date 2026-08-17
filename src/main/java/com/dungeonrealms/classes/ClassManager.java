package com.dungeonrealms.classes;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class ClassManager {

    private final DungeonRealms plugin;
    private final Map<String, GameClass> classes = new LinkedHashMap<>();

    public ClassManager(DungeonRealms plugin) {
        this.plugin = plugin;
        loadClasses();
    }

    public void loadClasses() {
        classes.clear();
        ConfigurationSection section = plugin.getConfigManager().getClassesConfig().getConfigurationSection("classes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection classSection = section.getConfigurationSection(key);
            if (classSection == null) continue;

            GameClass gc = new GameClass(key);
            gc.setDisplayName(classSection.getString("display-name", key));
            gc.setDescription(classSection.getString("description", ""));
            gc.setMaxHealth(classSection.getInt("max-health", 20));
            gc.setBaseDamage(classSection.getInt("base-damage", 0));
            gc.setBaseDefense(classSection.getInt("base-defense", 0));
            gc.setBaseMana(classSection.getInt("base-mana", 20));
            gc.setHealthPerLevel(classSection.getDouble("health-per-level", 0));
            gc.setDamagePerLevel(classSection.getDouble("damage-per-level", 0));
            gc.setDefensePerLevel(classSection.getDouble("defense-per-level", 0));
            gc.setManaPerLevel(classSection.getDouble("mana-per-level", 0));
            gc.setAwakenedClass(classSection.getString("awakened-class", "none"));
            gc.setAwakened(classSection.getBoolean("is-awakened", false));

            List<?> skillList = classSection.getList("skills");
            if (skillList != null) {
                for (Object obj : skillList) {
                    if (obj instanceof Map<?, ?> map) {
                        String skillId = (String) map.get("id");
                        int unlockLevel = ((Number) map.get("unlock-level")).intValue();
                        gc.addSkill(skillId, unlockLevel);
                    }
                }
            }

            classes.put(key, gc);
        }

        plugin.getLogger().info("Loaded " + classes.size() + " classes.");
    }

    public GameClass getClass(String id) {
        return classes.get(id);
    }

    public Collection<GameClass> getAllClasses() {
        return classes.values();
    }

    public List<GameClass> getBaseClasses() {
        List<GameClass> result = new ArrayList<>();
        for (GameClass gc : classes.values()) {
            if (!gc.isAwakened()) {
                result.add(gc);
            }
        }
        return result;
    }

    public List<GameClass> getAwakenedClasses() {
        List<GameClass> result = new ArrayList<>();
        for (GameClass gc : classes.values()) {
            if (gc.isAwakened()) {
                result.add(gc);
            }
        }
        return result;
    }

    public boolean classExists(String id) {
        return classes.containsKey(id);
    }

    public void applyClassStats(Player player, PlayerDataManager.PlayerData data) {
        GameClass gc = getClass(data.getClassId());
        if (gc == null) return;

        int level = data.getLevel();

        double awakeningBonusHealth = 0;
        double awakeningBonusDamage = 0;
        double awakeningBonusDefense = 0;
        double awakeningBonusMana = 0;

        if (data.isAwakened()) {
            awakeningBonusHealth = plugin.getConfigManager().getMainConfig().getDouble("awakening.bonus-health", 0);
            awakeningBonusDamage = plugin.getConfigManager().getMainConfig().getDouble("awakening.bonus-damage", 0);
            awakeningBonusDefense = plugin.getConfigManager().getMainConfig().getDouble("awakening.bonus-defense", 0);
            awakeningBonusMana = plugin.getConfigManager().getMainConfig().getDouble("awakening.bonus-mana", 0);
        }

        double scaledHealth = gc.getMaxHealth() + (gc.getHealthPerLevel() * (level - 1)) + awakeningBonusHealth;
        double scaledMana = gc.getBaseMana() + (gc.getManaPerLevel() * (level - 1)) + awakeningBonusMana;

        double bonusHealth = plugin.getDamageManager().getBonusHealth(player);
        double bonusMana = plugin.getDamageManager().getBonusMana(player);

        double totalHealth = scaledHealth + bonusHealth;
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(Math.max(2.0, totalHealth));
        }
        player.setHealth(Math.max(1.0, Math.min(totalHealth, healthAttr != null ? healthAttr.getValue() : 1024.0)));
        player.setFoodLevel(20);
        player.setSaturation(20f);

        data.setMaxMana((int) (scaledMana + bonusMana));
        data.setMana((int) (scaledMana + bonusMana));
    }

    public double getBaseDamage(PlayerDataManager.PlayerData data) {
        GameClass gc = getClass(data.getClassId());
        if (gc == null) return 0;
        int level = data.getLevel();
        double damage = gc.getBaseDamage() + (gc.getDamagePerLevel() * (level - 1));
        if (data.isAwakened()) {
            damage += plugin.getConfigManager().getMainConfig().getDouble("awakening.bonus-damage", 0);
        }
        return damage;
    }

    public double getBaseDefense(PlayerDataManager.PlayerData data) {
        GameClass gc = getClass(data.getClassId());
        if (gc == null) return 0;
        int level = data.getLevel();
        double defense = gc.getBaseDefense() + (gc.getDefensePerLevel() * (level - 1));
        if (data.isAwakened()) {
            defense += plugin.getConfigManager().getMainConfig().getDouble("awakening.bonus-defense", 0);
        }
        return defense;
    }
}
