package com.dungeonrealms.classes;

import java.util.ArrayList;
import java.util.List;

public class GameClass {

    private final String id;
    private String displayName;
    private String description;
    private int maxHealth;
    private int baseDamage;
    private int baseDefense;
    private int baseMana;
    private double healthPerLevel;
    private double damagePerLevel;
    private double defensePerLevel;
    private double manaPerLevel;
    private String awakenedClass;
    private boolean isAwakened;
    private final List<ClassSkill> skills = new ArrayList<>();

    public GameClass(String id) {
        this.id = id;
    }

    public void addSkill(String skillId, int unlockLevel) {
        skills.add(new ClassSkill(skillId, unlockLevel));
    }

    public List<ClassSkill> getSkills() {
        return skills;
    }

    public List<ClassSkill> getSkillsUpToLevel(int level) {
        List<ClassSkill> result = new ArrayList<>();
        for (ClassSkill cs : skills) {
            if (cs.getUnlockLevel() <= level) {
                result.add(cs);
            }
        }
        return result;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public int getBaseDamage() { return baseDamage; }
    public void setBaseDamage(int baseDamage) { this.baseDamage = baseDamage; }
    public int getBaseDefense() { return baseDefense; }
    public void setBaseDefense(int baseDefense) { this.baseDefense = baseDefense; }
    public int getBaseMana() { return baseMana; }
    public void setBaseMana(int baseMana) { this.baseMana = baseMana; }
    public double getHealthPerLevel() { return healthPerLevel; }
    public void setHealthPerLevel(double healthPerLevel) { this.healthPerLevel = healthPerLevel; }
    public double getDamagePerLevel() { return damagePerLevel; }
    public void setDamagePerLevel(double damagePerLevel) { this.damagePerLevel = damagePerLevel; }
    public double getDefensePerLevel() { return defensePerLevel; }
    public void setDefensePerLevel(double defensePerLevel) { this.defensePerLevel = defensePerLevel; }
    public double getManaPerLevel() { return manaPerLevel; }
    public void setManaPerLevel(double manaPerLevel) { this.manaPerLevel = manaPerLevel; }
    public String getAwakenedClass() { return awakenedClass; }
    public void setAwakenedClass(String awakenedClass) { this.awakenedClass = awakenedClass; }
    public boolean isAwakened() { return isAwakened; }
    public void setAwakened(boolean awakened) { isAwakened = awakened; }

    public static class ClassSkill {
        private final String skillId;
        private final int unlockLevel;

        public ClassSkill(String skillId, int unlockLevel) {
            this.skillId = skillId;
            this.unlockLevel = unlockLevel;
        }

        public String getSkillId() { return skillId; }
        public int getUnlockLevel() { return unlockLevel; }
    }
}
