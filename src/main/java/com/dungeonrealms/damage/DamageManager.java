package com.dungeonrealms.damage;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.items.ItemManager;
import com.dungeonrealms.items.GameItem;
import com.dungeonrealms.skills.Skill;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DamageManager {

    private final DungeonRealms plugin;

    public DamageManager(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    public double calculatePlayerDamage(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return 1.0;

        double baseDamage = plugin.getClassManager().getBaseDamage(data);
        double weaponDamage = getWeaponDamage(player);
        double totalDamage = baseDamage + weaponDamage;

        double critChance = getCritChance(player);
        double critDamage = getCritDamage(player);

        if (Math.random() * 100 < critChance) {
            totalDamage *= (1 + critDamage / 100.0);
        }

        return Math.max(1.0, totalDamage);
    }

    public double calculatePlayerDefense(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return 0;

        double baseDefense = plugin.getClassManager().getBaseDefense(data);
        double armorDefense = getArmorDefense(player);
        double defensePercent = getDefensePercent(player);

        double totalDefense = baseDefense + armorDefense;
        double damageReduction = totalDefense / (totalDefense + 100.0) * 100.0;
        damageReduction = Math.min(damageReduction, 80.0);
        damageReduction += defensePercent;

        return Math.min(damageReduction, 90.0);
    }

    public double calculatePlayerMagicDefense(Player player) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return 0;

        double baseDefense = plugin.getClassManager().getBaseDefense(data);
        double armorMagicDefense = getArmorMagicDefense(player);
        double defensePercent = getDefensePercent(player);

        double totalDefense = baseDefense + armorMagicDefense;
        double damageReduction = totalDefense / (totalDefense + 100.0) * 100.0;
        damageReduction = Math.min(damageReduction, 80.0);
        damageReduction += defensePercent;

        return Math.min(damageReduction, 90.0);
    }

    public double getWeaponDamage(Player player) {
        ItemManager im = plugin.getItemManager();
        GameItem weapon = im.getEquippedWeapon(player);
        return weapon != null ? weapon.getDamage() : 0;
    }

    public double getArmorDefense(Player player) {
        ItemManager im = plugin.getItemManager();
        double total = 0;
        for (GameItem armor : im.getEquippedArmor(player)) {
            total += armor.getDefense();
        }
        return total;
    }

    public double getArmorMagicDefense(Player player) {
        ItemManager im = plugin.getItemManager();
        double total = 0;
        for (GameItem armor : im.getEquippedArmor(player)) {
            total += armor.getMagicDefense();
        }
        return total;
    }

    public double getCritChance(Player player) {
        double crit = 0;
        ItemManager im = plugin.getItemManager();
        GameItem weapon = im.getEquippedWeapon(player);
        if (weapon != null) crit += weapon.getCritChance();
        for (GameItem armor : im.getEquippedArmor(player)) {
            crit += armor.getCritChance();
        }
        return crit;
    }

    public double getCritDamage(Player player) {
        double crit = 0;
        ItemManager im = plugin.getItemManager();
        GameItem weapon = im.getEquippedWeapon(player);
        if (weapon != null) crit += weapon.getCritDamage();
        for (GameItem armor : im.getEquippedArmor(player)) {
            crit += armor.getCritDamage();
        }
        return crit;
    }

    public double getDefensePercent(Player player) {
        double pct = 0;
        ItemManager im = plugin.getItemManager();
        for (GameItem armor : im.getEquippedArmor(player)) {
            pct += armor.getDefensePercent();
        }
        return pct;
    }

    public double getBonusHealth(Player player) {
        double bonus = 0;
        ItemManager im = plugin.getItemManager();
        GameItem weapon = im.getEquippedWeapon(player);
        if (weapon != null) bonus += weapon.getHealth();
        for (GameItem armor : im.getEquippedArmor(player)) {
            bonus += armor.getHealth();
        }
        return bonus;
    }

    public double getBonusMana(Player player) {
        double bonus = 0;
        ItemManager im = plugin.getItemManager();
        GameItem weapon = im.getEquippedWeapon(player);
        if (weapon != null) bonus += weapon.getMana();
        for (GameItem armor : im.getEquippedArmor(player)) {
            bonus += armor.getMana();
        }
        return bonus;
    }

    public double calculateSkillDamage(Player player, Skill skill) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return skill.getDamage();

        double classDamage = plugin.getClassManager().getBaseDamage(data);
        double baseSkillDamage = skill.getDamage();
        double scaling = skill.getDamageScaling();

        double scaledDamage = baseSkillDamage + (classDamage * scaling);

        double critChance = getCritChance(player);
        double critDamage = getCritDamage(player);
        if (Math.random() * 100 < critChance) {
            scaledDamage *= (1 + critDamage / 100.0);
        }

        return Math.max(1.0, scaledDamage);
    }

    public double applyMobDefense(double rawDamage, Skill.DamageType damageType, LivingEntity mob) {
        if (damageType == Skill.DamageType.TRUE) return rawDamage;

        double defense = 0;
        if (mob.getScoreboardTags().contains("dr_defense")) {
            for (String tag : mob.getScoreboardTags()) {
                if (tag.startsWith("dr_defense:")) {
                    try { defense = Double.parseDouble(tag.substring(11)); } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (damageType == Skill.DamageType.MAGIC) {
            double magicDefense = 0;
            for (String tag : mob.getScoreboardTags()) {
                if (tag.startsWith("dr_magic_defense:")) {
                    try { magicDefense = Double.parseDouble(tag.substring(17)); } catch (NumberFormatException ignored) {}
                }
            }
            defense = Math.max(defense, magicDefense);
        }

        double reduction = defense / (defense + 100.0);
        double finalDamage = rawDamage * (1.0 - reduction);
        return Math.max(1.0, finalDamage);
    }
}
