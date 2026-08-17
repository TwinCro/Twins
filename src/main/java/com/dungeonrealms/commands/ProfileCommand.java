package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.classes.GameClass;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.damage.DamageManager;
import com.dungeonrealms.items.GameItem;
import com.dungeonrealms.items.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ProfileCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public ProfileCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length > 0 && sender.hasPermission("dungeonrealms.admin")) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            target = player;
        }

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(target);
        if (data == null || !data.hasClass()) {
            sender.sendMessage("§cNo class data found. Use §e/class choose <class> §cfirst.");
            return true;
        }

        GameClass gc = plugin.getClassManager().getClass(data.getClassId());
        if (gc == null) {
            sender.sendMessage("§cClass not found: " + data.getClassId());
            return true;
        }

        DamageManager dm = plugin.getDamageManager();
        ItemManager im = plugin.getItemManager();

        String displayName = ChatColor.translateAlternateColorCodes('&', gc.getDisplayName());

        double baseDamage = plugin.getClassManager().getBaseDamage(data);
        double baseDefense = plugin.getClassManager().getBaseDefense(data);
        double weaponDamage = dm.getWeaponDamage(target);
        double armorDefense = dm.getArmorDefense(target);
        double armorMagicDefense = dm.getArmorMagicDefense(target);
        double bonusHealth = dm.getBonusHealth(target);
        double bonusMana = dm.getBonusMana(target);
        double critChance = dm.getCritChance(target);
        double critDamage = dm.getCritDamage(target);
        double defensePercent = dm.getDefensePercent(target);
        double totalDamage = dm.calculatePlayerDamage(target);
        double totalDefense = dm.calculatePlayerDefense(target);
        double totalMagicDefense = dm.calculatePlayerMagicDefense(target);

        double maxHealth;
        AttributeInstance healthAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        maxHealth = healthAttr != null ? healthAttr.getValue() : target.getMaxHealth();

        sender.sendMessage("§6§l╔══════════════════════════╗");
        sender.sendMessage("§6§l║  §e§l" + target.getName() + "'s Profile  §6§l║");
        sender.sendMessage("§6§l╚══════════════════════════╝");

        sender.sendMessage("§eClass: §f" + displayName);
        sender.sendMessage("§eLevel: §f" + data.getLevel() + "§7/" + plugin.getLevelManager().getMaxLevel());
        sender.sendMessage("§eXP: §f" + data.getXp() + "§7/" + plugin.getLevelManager().getXpForLevel(data.getLevel()));
        sender.sendMessage("§eMana: §f" + data.getMana() + "§7/" + data.getMaxMana());
        sender.sendMessage("§eGold: §6" + data.getGold());
        sender.sendMessage("§eAwakened: " + (data.isAwakened() ? "§aYes" : "§cNo"));
        if (data.isAwakened()) {
            sender.sendMessage("§eAwakening Count: §f" + data.getAwakeningCount());
        }

        sender.sendMessage("§6§l═══════════════════════════");
        sender.sendMessage("§c§lCombat Stats:");
        sender.sendMessage("§c  HP: §f" + (int) target.getHealth() + "§7/" + (int) maxHealth
                + (bonusHealth > 0 ? " §7(base " + (int)(maxHealth - bonusHealth) + " + §a" + (int)bonusHealth + "§7)" : ""));
        sender.sendMessage("§c  Damage: §f" + (int) totalDamage
                + " §7(base " + (int) baseDamage
                + (weaponDamage > 0 ? " + §c" + (int) weaponDamage + " §7weapon" : "")
                + ")");
        sender.sendMessage("§c  Defense: §f" + String.format("%.1f", totalDefense) + "%"
                + " §7(base " + (int) baseDefense
                + (armorDefense > 0 ? " + §9" + (int) armorDefense + " §7armor" : "")
                + (defensePercent > 0 ? " + §9" + (int) defensePercent + "% §7extra" : "")
                + ")");
        sender.sendMessage("§c  Magic Defense: §f" + String.format("%.1f", totalMagicDefense) + "%"
                + (armorMagicDefense > 0 ? " §7(+ §d" + (int) armorMagicDefense + " §7armor)" : ""));
        sender.sendMessage("§c  Crit Chance: §f" + (int) critChance + "%");
        sender.sendMessage("§c  Crit Damage: §f" + (int) critDamage + "%");

        sender.sendMessage("§6§l═══════════════════════════");
        sender.sendMessage("§b§lEquipment Bonuses:");
        GameItem weapon = im.getEquippedWeapon(target);
        if (weapon != null) {
            sender.sendMessage("§b  Weapon: §f" + ChatColor.translateAlternateColorCodes('&', weapon.getDisplayName()));
            sender.sendMessage("§b    §7DMG: §c" + (int) weapon.getDamage()
                    + (weapon.getCritChance() > 0 ? " §7Crit: §e" + (int) weapon.getCritChance() + "%" : "")
                    + (weapon.getCritDamage() > 0 ? " §7CDmg: §e" + (int) weapon.getCritDamage() + "%" : ""));
        } else {
            sender.sendMessage("§b  Weapon: §7None");
        }

        List<GameItem> armor = im.getEquippedArmor(target);
        if (armor.isEmpty()) {
            sender.sendMessage("§b  Armor: §7None equipped");
        } else {
            sender.sendMessage("§b  Armor (" + armor.size() + " pieces):");
            for (GameItem piece : armor) {
                sender.sendMessage("§b    §7" + ChatColor.translateAlternateColorCodes('&', piece.getDisplayName())
                        + " §7| DEF: §9" + (int) piece.getDefense()
                        + (piece.getHealth() > 0 ? " §7HP: §a+" + (int) piece.getHealth() : "")
                        + (piece.getMana() > 0 ? " §7MP: §a+" + (int) piece.getMana() : "")
                        + (piece.getMagicDefense() > 0 ? " §7MDef: §d" + (int) piece.getMagicDefense() : ""));
            }
        }

        sender.sendMessage("§6§l═══════════════════════════");
        sender.sendMessage("§eSkills: §f" + data.getUnlockedSkills().size() + " unlocked§7, §f" + data.getEquippedSkills().size() + " equipped");
        sender.sendMessage("§eHomes: §f" + data.getHomeCount() + "§7/3");
        sender.sendMessage("§6§l═══════════════════════════");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("dungeonrealms.admin")) {
            String prefix = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) completions.add(p.getName());
            }
        }
        return completions;
    }
}
