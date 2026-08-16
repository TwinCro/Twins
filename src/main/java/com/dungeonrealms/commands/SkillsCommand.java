package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.skills.Skill;
import com.dungeonrealms.skills.SkillExecutor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillsCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public SkillsCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage("§cYou must choose a class first! Use §e/class choose <class>");
            return true;
        }

        if (args.length == 0) {
            listSkills(player, data);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listSkills(player, data);
            case "info" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /skills info <skillname>");
                    return true;
                }
                showSkillInfo(player, args[1]);
            }
            case "bind" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /skills bind <skillname> <1-6>");
                    return true;
                }
                bindSkill(player, data, args[1], args[2]);
            }
            case "unbind" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /skills unbind <1-6>");
                    return true;
                }
                unbindSkill(player, data, args[1]);
            }
            case "use" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /skills use <skillname>");
                    return true;
                }
                useSkill(player, args[1]);
            }
            case "bar" -> {
                toggleSkillBar(player, data);
            }
            default -> player.sendMessage("§cUsage: /skills [list | info <skill> | bind <skill> <1-6> | unbind <1-6> | use <skill> | bar]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : Arrays.asList("list", "info", "bind", "unbind", "use", "bar")) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("info") || sub.equals("bind") || sub.equals("use")) {
                if (sender instanceof Player player) {
                    PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
                    if (data != null) {
                        String prefix = args[1].toLowerCase();
                        for (String skillId : data.getUnlockedSkills()) {
                            if (skillId.startsWith(prefix)) completions.add(skillId);
                        }
                    }
                }
            } else if (sub.equals("unbind")) {
                for (int i = 1; i <= 6; i++) {
                    if (String.valueOf(i).startsWith(args[1])) completions.add(String.valueOf(i));
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("bind")) {
            for (int i = 1; i <= 6; i++) {
                if (String.valueOf(i).startsWith(args[2])) completions.add(String.valueOf(i));
            }
        }
        return completions;
    }

    private void listSkills(Player player, PlayerDataManager.PlayerData data) {
        player.sendMessage("§6§l=== Your Skills ===");
        for (String skillId : data.getUnlockedSkills()) {
            Skill skill = plugin.getSkillManager().getSkill(skillId);
            if (skill != null) {
                String cd = plugin.getSkillManager().isOnCooldown(player.getUniqueId(), skillId)
                        ? " §c(CD: " + (plugin.getSkillManager().getCooldownRemaining(player.getUniqueId(), skillId) / 1000) + "s)"
                        : "";
                String slotStr = "";
                for (var entry : data.getSkillSlots().entrySet()) {
                    if (entry.getValue().equals(skillId)) {
                        slotStr = " §a[Slot " + entry.getKey() + "]";
                        break;
                    }
                }
                player.sendMessage("§e" + skillId + " §7- " + ChatColor.translateAlternateColorCodes('&', skill.getDisplayName())
                        + " §7[" + skill.getType() + "] §b(" + skill.getManaCost() + " MP)" + cd + slotStr);
                player.sendMessage("§7  " + skill.getDescription());
            }
        }
        if (data.getUnlockedSkills().isEmpty()) {
            player.sendMessage("§7No skills unlocked yet. Level up to gain skills!");
        }
        player.sendMessage("§7Bind skills with §e/skills bind <skill> <1-6>§7, then press §eF§7 to toggle skill bar mode.");
    }

    private void showSkillInfo(Player player, String skillId) {
        Skill skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) {
            player.sendMessage("§cSkill not found: " + skillId);
            return;
        }
        player.sendMessage("§6§l=== Skill Info ===");
        player.sendMessage("§eName: " + ChatColor.translateAlternateColorCodes('&', skill.getDisplayName()));
        player.sendMessage("§eType: " + skill.getType());
        player.sendMessage("§eCooldown: " + skill.getCooldown() + "s");
        player.sendMessage("§eMana Cost: " + skill.getManaCost());
        player.sendMessage("§eEffect: " + skill.getEffectType());
        player.sendMessage("§7" + skill.getDescription());
    }

    private void bindSkill(Player player, PlayerDataManager.PlayerData data, String skillId, String slotStr) {
        Skill skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) {
            player.sendMessage("§cSkill not found: " + skillId);
            return;
        }
        if (!data.getUnlockedSkills().contains(skillId)) {
            player.sendMessage("§cYou have not unlocked this skill yet!");
            return;
        }
        int slot;
        try {
            slot = Integer.parseInt(slotStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§cSlot must be a number from 1 to 6.");
            return;
        }
        if (slot < 1 || slot > 6) {
            player.sendMessage("§cSlot must be between 1 and 6.");
            return;
        }
        if (skill.getType() == Skill.Type.PASSIVE) {
            player.sendMessage("§cPassive skills cannot be bound to slots.");
            return;
        }
        data.getSkillSlots().put(slot, skillId);
        player.sendMessage("§aBound §e" + ChatColor.translateAlternateColorCodes('&', skill.getDisplayName()) + " §ato slot §e" + slot + "§a.");
        player.sendMessage("§7Press §eF §7to toggle skill bar mode, then press §e" + slot + " §7to use it.");
    }

    private void unbindSkill(Player player, PlayerDataManager.PlayerData data, String slotStr) {
        int slot;
        try {
            slot = Integer.parseInt(slotStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§cSlot must be a number from 1 to 6.");
            return;
        }
        if (slot < 1 || slot > 6) {
            player.sendMessage("§cSlot must be between 1 and 6.");
            return;
        }
        String removed = data.getSkillSlots().remove(slot);
        if (removed == null) {
            player.sendMessage("§cNo skill bound to slot " + slot + ".");
            return;
        }
        player.sendMessage("§aUnbound slot §e" + slot + "§a (was: §e" + removed + "§a).");
    }

    private void useSkill(Player player, String skillId) {
        SkillExecutor executor = new SkillExecutor(plugin);
        executor.executeSkill(player, skillId);
    }

    private void toggleSkillBar(Player player, PlayerDataManager.PlayerData data) {
        data.setSkillBarMode(!data.isSkillBarMode());
        if (data.isSkillBarMode()) {
            player.sendMessage("§a§lSkill Bar Mode: §aON §7- Press §e1-6 §7to use bound skills. Press §eF §7to turn off.");
        } else {
            player.sendMessage("§cSkill Bar Mode: OFF §7- Normal hotbar active.");
        }
    }
}
