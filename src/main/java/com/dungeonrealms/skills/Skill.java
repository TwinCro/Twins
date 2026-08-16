package com.dungeonrealms.skills;

public class Skill {

    public enum Type { ACTIVE, PASSIVE }
    public enum DamageType { PHYSICAL, MAGIC, TRUE }

    public enum EffectType {
        AOE_DAMAGE, SINGLE_DAMAGE, PROJECTILE, BUFF, AOE_EFFECT,
        SHIELD, HEAL, AOE_HEAL, CLEANSE, REVIVE, LIFESTEAL,
        TELEPORT, ZONE_HEAL, DEBUFF, FULL_HEAL,
        DASH, LIFESTEAL_SINGLE, LIFESTEAL_AOE, CONE_DAMAGE
    }

    private final String id;
    private String displayName;
    private String description;
    private Type type;
    private int cooldown;
    private int manaCost;
    private EffectType effectType;
    private double damage;
    private double radius;
    private double knockback;
    private double healAmount;
    private double duration;
    private double buffAmount;
    private String buffType;
    private String debuffType;
    private String projectileType;
    private int projectileCount;
    private boolean pierce;
    private double absorption;
    private double lifestealPercent;
    private double teleportDistance;
    private double healPerSecond;
    private double slowDuration;
    private double stunDuration;
    private double immunityDuration;
    private double hpPercent;
    private boolean pull;
    private boolean revive;
    private double healAllies;
    private DamageType damageType = DamageType.PHYSICAL;
    private double damageScaling = 1.0;

    public Skill(String id) {
        this.id = id;
        this.type = Type.ACTIVE;
        this.cooldown = 0;
        this.manaCost = 0;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public int getManaCost() { return manaCost; }
    public void setManaCost(int manaCost) { this.manaCost = manaCost; }
    public EffectType getEffectType() { return effectType; }
    public void setEffectType(EffectType effectType) { this.effectType = effectType; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
    public double getKnockback() { return knockback; }
    public void setKnockback(double knockback) { this.knockback = knockback; }
    public double getHealAmount() { return healAmount; }
    public void setHealAmount(double healAmount) { this.healAmount = healAmount; }
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
    public double getBuffAmount() { return buffAmount; }
    public void setBuffAmount(double buffAmount) { this.buffAmount = buffAmount; }
    public String getBuffType() { return buffType; }
    public void setBuffType(String buffType) { this.buffType = buffType; }
    public String getDebuffType() { return debuffType; }
    public void setDebuffType(String debuffType) { this.debuffType = debuffType; }
    public String getProjectileType() { return projectileType; }
    public void setProjectileType(String projectileType) { this.projectileType = projectileType; }
    public int getProjectileCount() { return projectileCount; }
    public void setProjectileCount(int projectileCount) { this.projectileCount = projectileCount; }
    public boolean isPierce() { return pierce; }
    public void setPierce(boolean pierce) { this.pierce = pierce; }
    public double getAbsorption() { return absorption; }
    public void setAbsorption(double absorption) { this.absorption = absorption; }
    public double getLifestealPercent() { return lifestealPercent; }
    public void setLifestealPercent(double lifestealPercent) { this.lifestealPercent = lifestealPercent; }
    public double getTeleportDistance() { return teleportDistance; }
    public void setTeleportDistance(double teleportDistance) { this.teleportDistance = teleportDistance; }
    public double getHealPerSecond() { return healPerSecond; }
    public void setHealPerSecond(double healPerSecond) { this.healPerSecond = healPerSecond; }
    public double getSlowDuration() { return slowDuration; }
    public void setSlowDuration(double slowDuration) { this.slowDuration = slowDuration; }
    public double getStunDuration() { return stunDuration; }
    public void setStunDuration(double stunDuration) { this.stunDuration = stunDuration; }
    public double getImmunityDuration() { return immunityDuration; }
    public void setImmunityDuration(double immunityDuration) { this.immunityDuration = immunityDuration; }
    public double getHpPercent() { return hpPercent; }
    public void setHpPercent(double hpPercent) { this.hpPercent = hpPercent; }
    public boolean isPull() { return pull; }
    public void setPull(boolean pull) { this.pull = pull; }
    public boolean isRevive() { return revive; }
    public void setRevive(boolean revive) { this.revive = revive; }
    public double getHealAllies() { return healAllies; }
    public void setHealAllies(double healAllies) { this.healAllies = healAllies; }
    public DamageType getDamageType() { return damageType; }
    public void setDamageType(DamageType damageType) { this.damageType = damageType; }
    public double getDamageScaling() { return damageScaling; }
    public void setDamageScaling(double damageScaling) { this.damageScaling = damageScaling; }
}
