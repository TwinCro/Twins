package com.dungeonrealms.skills;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class SkillManager {

    private final DungeonRealms plugin;
    private final Map<String, Skill> skills = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SkillManager(DungeonRealms plugin) {
        this.plugin = plugin;
        loadSkills();
    }

    public void loadSkills() {
        skills.clear();
        ConfigurationSection section = plugin.getConfigManager().getSkillsConfig().getConfigurationSection("skills");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection skillSection = section.getConfigurationSection(key);
            if (skillSection == null) continue;

            Skill skill = new Skill(key);
            skill.setDisplayName(skillSection.getString("display-name", key));
            skill.setDescription(skillSection.getString("description", ""));
            skill.setType(Skill.Type.valueOf(skillSection.getString("type", "ACTIVE").toUpperCase()));
            skill.setCooldown(skillSection.getInt("cooldown", 0));
            skill.setManaCost(skillSection.getInt("mana-cost", 0));

            ConfigurationSection effectSection = skillSection.getConfigurationSection("effect");
            if (effectSection != null) {
                String effectTypeStr = effectSection.getString("type", "SINGLE_DAMAGE");
                skill.setEffectType(Skill.EffectType.valueOf(effectTypeStr.toUpperCase()));
                skill.setDamage(effectSection.getDouble("damage", 0));
                skill.setRadius(effectSection.getDouble("radius", 0));
                skill.setKnockback(effectSection.getDouble("knockback", 0));
                skill.setHealAmount(effectSection.getDouble("amount", 0));
                skill.setDuration(effectSection.getDouble("duration", 0));
                skill.setBuffAmount(effectSection.getDouble("amount", 0));
                skill.setBuffType(effectSection.getString("buff-type", ""));
                skill.setDebuffType(effectSection.getString("debuff-type", ""));
                skill.setProjectileType(effectSection.getString("projectile", ""));
                skill.setProjectileCount(effectSection.getInt("count", 1));
                skill.setPierce(effectSection.getBoolean("pierce", false));
                skill.setAbsorption(effectSection.getDouble("absorption", 0));
                skill.setLifestealPercent(effectSection.getDouble("percentage", 0));
                skill.setTeleportDistance(effectSection.getDouble("distance", 0));
                skill.setHealPerSecond(effectSection.getDouble("amount-per-second", 0));
                skill.setSlowDuration(effectSection.getDouble("slow-duration", 0));
                skill.setStunDuration(effectSection.getDouble("stun-duration", 0));
                skill.setImmunityDuration(effectSection.getDouble("immunity-duration", 0));
                skill.setHpPercent(effectSection.getDouble("hp-percent", 0));
                skill.setPull(effectSection.getBoolean("pull", false));
                skill.setRevive(effectSection.getBoolean("revive", false));
                skill.setHealAllies(effectSection.getDouble("heal-allies", 0));
                String dmgTypeStr = effectSection.getString("damage-type", "PHYSICAL");
                skill.setDamageType(Skill.DamageType.valueOf(dmgTypeStr.toUpperCase()));
                skill.setDamageScaling(effectSection.getDouble("damage-scaling", 1.0));
            }

            skills.put(key, skill);
        }

        plugin.getLogger().info("Loaded " + skills.size() + " skills.");
    }

    public Skill getSkill(String id) {
        return skills.get(id);
    }

    public Collection<Skill> getAllSkills() {
        return skills.values();
    }

    public boolean isOnCooldown(UUID playerUuid, String skillId) {
        Map<String, Long> playerCd = cooldowns.get(playerUuid);
        if (playerCd == null) return false;
        Long expiry = playerCd.get(skillId);
        if (expiry == null) return false;
        return System.currentTimeMillis() < expiry;
    }

    public long getCooldownRemaining(UUID playerUuid, String skillId) {
        Map<String, Long> playerCd = cooldowns.get(playerUuid);
        if (playerCd == null) return 0;
        Long expiry = playerCd.get(skillId);
        if (expiry == null) return 0;
        return Math.max(0, expiry - System.currentTimeMillis());
    }

    public void setCooldown(UUID playerUuid, String skillId, int cooldownSeconds) {
        cooldowns.computeIfAbsent(playerUuid, k -> new HashMap<>())
                .put(skillId, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    public boolean canUseSkill(PlayerDataManager.PlayerData data, String skillId) {
        if (data == null) return false;
        if (!data.getUnlockedSkills().contains(skillId)) return false;
        if (isOnCooldown(data.getUuid(), skillId)) return false;
        Skill skill = getSkill(skillId);
        if (skill == null) return false;
        return data.getMana() >= skill.getManaCost();
    }
}
