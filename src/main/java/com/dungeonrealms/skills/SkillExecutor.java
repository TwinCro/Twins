package com.dungeonrealms.skills;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class SkillExecutor {

    private final DungeonRealms plugin;

    public SkillExecutor(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    public boolean executeSkill(Player player, String skillId) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null) return false;

        Skill skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) return false;

        if (!plugin.getSkillRegionManager().canUseSkills(player)) {
            player.sendMessage("§cSkills can only be used in dungeons or designated skill areas.");
            return false;
        }

        if (!plugin.getSkillManager().canUseSkill(data, skillId)) {
            long remaining = plugin.getSkillManager().getCooldownRemaining(data.getUuid(), skillId);
            if (remaining > 0) {
                player.sendMessage("§cSkill on cooldown: " + (remaining / 1000) + "s remaining");
                return false;
            }
            if (data.getMana() < skill.getManaCost()) {
                player.sendMessage("§cNot enough mana! Need " + skill.getManaCost() + ", have " + data.getMana());
                return false;
            }
            return false;
        }

        data.spendMana(skill.getManaCost());
        plugin.getSkillManager().setCooldown(data.getUuid(), skillId, skill.getCooldown());

        applyEffect(player, skill);
        player.sendMessage("§aUsed skill: §e" + skill.getDisplayName());
        return true;
    }

    private boolean isTargetable(Player player, Entity ent) {
        if (!(ent instanceof LivingEntity)) return false;
        if (ent.equals(player)) return false;
        if (ent instanceof Player) return false;
        return true;
    }

    private void applyEffect(Player player, Skill skill) {
        switch (skill.getEffectType()) {
            case AOE_DAMAGE -> doAoeDamage(player, skill);
            case SINGLE_DAMAGE -> doSingleDamage(player, skill);
            case PROJECTILE -> doProjectile(player, skill);
            case BUFF -> doBuff(player, skill);
            case AOE_EFFECT -> doAoeEffect(player, skill);
            case SHIELD -> doShield(player, skill);
            case HEAL -> doHeal(player, skill);
            case AOE_HEAL -> doAoeHeal(player, skill);
            case CLEANSE -> doCleanse(player, skill);
            case REVIVE -> doRevive(player, skill);
            case LIFESTEAL -> doLifesteal(player, skill);
            case TELEPORT -> doTeleport(player, skill);
            case ZONE_HEAL -> doZoneHeal(player, skill);
            case DEBUFF -> doDebuff(player, skill);
            case FULL_HEAL -> doFullHeal(player, skill);
            case DASH -> doDash(player, skill);
            case LIFESTEAL_SINGLE -> doLifestealSingle(player, skill);
            case LIFESTEAL_AOE -> doLifestealAoe(player, skill);
            case CONE_DAMAGE -> doConeDamage(player, skill);
        }
    }

    private void doAoeDamage(Player player, Skill skill) {
        double radius = skill.getRadius();
        List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
        double rawDamage = plugin.getDamageManager().calculateSkillDamage(player, skill);
        for (Entity ent : nearby) {
            if (isTargetable(player, ent)) {
                LivingEntity le = (LivingEntity) ent;
                double damage = plugin.getDamageManager().applyMobDefense(rawDamage, skill.getDamageType(), le);
                le.damage(damage, player);
                if (skill.getKnockback() > 0) {
                    Vector dir = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                    le.setVelocity(dir.multiply(skill.getKnockback()));
                }
            }
        }
        if (skill.getSlowDuration() > 0) {
            for (Entity ent : nearby) {
                if (isTargetable(player, ent)) {
                    ((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(skill.getSlowDuration() * 20), 2));
                }
            }
        }
        if (skill.getStunDuration() > 0) {
            for (Entity ent : nearby) {
                if (isTargetable(player, ent)) {
                    LivingEntity le = (LivingEntity) ent;
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(skill.getStunDuration() * 20), 10));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, (int)(skill.getStunDuration() * 20), -10));
                }
            }
        }
        if (skill.isPull()) {
            Location center = player.getLocation();
            for (Entity ent : nearby) {
                if (isTargetable(player, ent)) {
                    Vector dir = center.toVector().subtract(ent.getLocation().toVector()).normalize().multiply(2);
                    ent.setVelocity(dir);
                }
            }
        }
        if (skill.getHealAllies() > 0) {
            for (Entity ent : nearby) {
                if (ent instanceof Player p && !p.equals(player)) {
                    p.setHealth(Math.min(p.getHealth() + skill.getHealAllies(), p.getMaxHealth()));
                }
            }
        }
    }

    private void doSingleDamage(Player player, Skill skill) {
        List<Entity> nearby = player.getNearbyEntities(5, 5, 5);
        double rawDamage = plugin.getDamageManager().calculateSkillDamage(player, skill);
        for (Entity ent : nearby) {
            if (isTargetable(player, ent)) {
                LivingEntity le = (LivingEntity) ent;
                double damage = plugin.getDamageManager().applyMobDefense(rawDamage, skill.getDamageType(), le);
                le.damage(damage, player);
                if (skill.getKnockback() > 0) {
                    Vector dir = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                    le.setVelocity(dir.multiply(skill.getKnockback()));
                }
                break;
            }
        }
    }

    private void doProjectile(Player player, Skill skill) {
        int count = skill.getProjectileCount();
        for (int i = 0; i < count; i++) {
            Vector velocity = player.getLocation().getDirection();
            if (count > 1) {
                double spread = 0.2 * (i - count / 2.0);
                velocity = velocity.add(new Vector(spread, 0, spread * 0.5));
            }
            Projectile proj;
            if ("FIREBALL".equalsIgnoreCase(skill.getProjectileType())) {
                proj = player.launchProjectile(Fireball.class, velocity.normalize().multiply(1.5));
            } else {
                proj = player.launchProjectile(Arrow.class, velocity.normalize().multiply(2.5));
            }
            proj.setShooter(player);
        }
    }

    private void doBuff(Player player, Skill skill) {
        int durationTicks = (int)(skill.getDuration() * 20);
        if ("DAMAGE".equalsIgnoreCase(skill.getBuffType())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, durationTicks, (int)(skill.getBuffAmount() / 5)));
        } else if ("DAMAGE_REDUCTION".equalsIgnoreCase(skill.getBuffType())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 1));
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, 2));
    }

    private void doAoeEffect(Player player, Skill skill) {
        double radius = skill.getRadius();
        int durationTicks = (int)(skill.getDuration() * 20);
        for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
            if (isTargetable(player, ent)) {
                ((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 3));
            }
        }
    }

    private void doShield(Player player, Skill skill) {
        int durationTicks = (int)(skill.getDuration() * 20);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 2));
    }

    private void doHeal(Player player, Skill skill) {
        player.setHealth(Math.min(player.getHealth() + skill.getHealAmount(), player.getMaxHealth()));
    }

    private void doAoeHeal(Player player, Skill skill) {
        double radius = skill.getRadius();
        player.setHealth(Math.min(player.getHealth() + skill.getHealAmount(), player.getMaxHealth()));
        for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
            if (ent instanceof Player p) {
                p.setHealth(Math.min(p.getHealth() + skill.getHealAmount(), p.getMaxHealth()));
            }
        }
    }

    private void doCleanse(Player player, Skill skill) {
        for (PotionEffect eff : player.getActivePotionEffects()) {
            player.removePotionEffect(eff.getType());
        }
        double radius = skill.getRadius();
        for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
            if (ent instanceof Player p) {
                for (PotionEffect eff : p.getActivePotionEffects()) {
                    p.removePotionEffect(eff.getType());
                }
            }
        }
    }

    private void doRevive(Player player, Skill skill) {
        double radius = 30;
        for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
            if (ent instanceof Player p) {
                if (p.getHealth() < p.getMaxHealth() * 0.3) {
                    p.setHealth(p.getMaxHealth() * (skill.getHpPercent() / 100.0));
                }
            }
        }
    }

    private void doLifesteal(Player player, Skill skill) {
        int durationTicks = (int)(skill.getDuration() * 20);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks += 20;
                if (ticks >= durationTicks) cancel();
            }
        }.runTaskTimer(plugin, 0, 20);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 1));
    }

    private void doTeleport(Player player, Skill skill) {
        Vector dir = player.getLocation().getDirection().normalize().multiply(skill.getTeleportDistance());
        Location target = player.getLocation().add(dir);
        player.teleport(target);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
    }

    private void doZoneHeal(Player player, Skill skill) {
        int durationTicks = (int)(skill.getDuration() * 20);
        new BukkitRunnable() {
            int ticks = 0;
            final Location center = player.getLocation();
            final double r = skill.getRadius();
            @Override
            public void run() {
                ticks += 20;
                if (ticks >= durationTicks) { cancel(); return; }
                for (Entity ent : center.getWorld().getNearbyEntities(center, r, r, r)) {
                    if (ent instanceof Player p) {
                        p.setHealth(Math.min(p.getHealth() + skill.getHealPerSecond(), p.getMaxHealth()));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void doDebuff(Player player, Skill skill) {
        List<Entity> nearby = player.getNearbyEntities(10, 10, 10);
        for (Entity ent : nearby) {
            if (isTargetable(player, ent)) {
                ((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (int)(skill.getDuration() * 20), 2));
                break;
            }
        }
    }

    private void doFullHeal(Player player, Skill skill) {
        double radius = skill.getRadius();
        player.setHealth(player.getMaxHealth());
        for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
            if (ent instanceof Player p) {
                p.setHealth(p.getMaxHealth());
            }
        }
    }

    private void doDash(Player player, Skill skill) {
        Vector dir = player.getLocation().getDirection().normalize();
        double distance = skill.getTeleportDistance() > 0 ? skill.getTeleportDistance() : 8;
        Location target = player.getLocation().add(dir.multiply(distance));
        player.teleport(target);
        player.setVelocity(dir.multiply(1.5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2));
        if (skill.getDamage() > 0) {
            double radius = skill.getRadius() > 0 ? skill.getRadius() : 3;
            double rawDamage = plugin.getDamageManager().calculateSkillDamage(player, skill);
            for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
                if (isTargetable(player, ent)) {
                    LivingEntity le = (LivingEntity) ent;
                    double damage = plugin.getDamageManager().applyMobDefense(rawDamage, skill.getDamageType(), le);
                    le.damage(damage, player);
                }
            }
        }
    }

    private void doLifestealSingle(Player player, Skill skill) {
        List<Entity> nearby = player.getNearbyEntities(5, 5, 5);
        double rawDamage = plugin.getDamageManager().calculateSkillDamage(player, skill);
        for (Entity ent : nearby) {
            if (isTargetable(player, ent)) {
                LivingEntity le = (LivingEntity) ent;
                double damage = plugin.getDamageManager().applyMobDefense(rawDamage, skill.getDamageType(), le);
                le.damage(damage, player);
                double heal = damage * (skill.getLifestealPercent() / 100.0);
                player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
                break;
            }
        }
    }

    private void doLifestealAoe(Player player, Skill skill) {
        double radius = skill.getRadius();
        double totalHeal = 0;
        double rawDamage = plugin.getDamageManager().calculateSkillDamage(player, skill);
        for (Entity ent : player.getNearbyEntities(radius, radius, radius)) {
            if (isTargetable(player, ent)) {
                LivingEntity le = (LivingEntity) ent;
                double damage = plugin.getDamageManager().applyMobDefense(rawDamage, skill.getDamageType(), le);
                le.damage(damage, player);
                totalHeal += damage * (skill.getLifestealPercent() / 100.0);
            }
        }
        if (totalHeal > 0) {
            player.setHealth(Math.min(player.getHealth() + totalHeal, player.getMaxHealth()));
        }
    }

    private void doConeDamage(Player player, Skill skill) {
        double range = skill.getRadius() > 0 ? skill.getRadius() : 8;
        double coneAngle = Math.cos(Math.toRadians(45));
        Vector playerDir = player.getLocation().getDirection().normalize();
        double rawDamage = plugin.getDamageManager().calculateSkillDamage(player, skill);
        for (Entity ent : player.getNearbyEntities(range, range, range)) {
            if (isTargetable(player, ent)) {
                LivingEntity le = (LivingEntity) ent;
                Vector toEnt = ent.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                if (toEnt.dot(playerDir) > coneAngle) {
                    double damage = plugin.getDamageManager().applyMobDefense(rawDamage, skill.getDamageType(), le);
                    le.damage(damage, player);
                    if (skill.getKnockback() > 0) {
                        le.setVelocity(toEnt.multiply(skill.getKnockback()));
                    }
                }
            }
        }
    }
}
