package com.dungeonrealms;

import com.dungeonrealms.api.SupabaseClient;
import com.dungeonrealms.classes.ClassManager;
import com.dungeonrealms.commands.*;
import com.dungeonrealms.config.ConfigManager;
import com.dungeonrealms.damage.DamageManager;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.dungeon.DungeonManager;
import com.dungeonrealms.dungeon.builder.DungeonBuilderManager;
import com.dungeonrealms.guild.GuildManager;
import com.dungeonrealms.guild.ClaimManager;
import com.dungeonrealms.items.ItemManager;
import com.dungeonrealms.leveling.LevelManager;
import com.dungeonrealms.listeners.*;
import com.dungeonrealms.party.PartyManager;
import com.dungeonrealms.skills.SkillManager;
import com.dungeonrealms.skills.SkillRegionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonRealms extends JavaPlugin {

    private static DungeonRealms instance;

    private ConfigManager configManager;
    private SupabaseClient supabaseClient;
    private PlayerDataManager playerDataManager;
    private ClassManager classManager;
    private LevelManager levelManager;
    private DamageManager damageManager;
    private ItemManager itemManager;
    private SkillManager skillManager;
    private SkillRegionManager skillRegionManager;
    private DungeonManager dungeonManager;
    private PartyManager partyManager;
    private GuildManager guildManager;
    private ClaimManager claimManager;
    private DungeonBuilderManager dungeonBuilderManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("classes.yml", false);
        saveResource("skills.yml", false);
        saveResource("items.yml", false);
        saveResource("dungeons.yml", false);

        configManager = new ConfigManager(this);
        supabaseClient = new SupabaseClient(this);

        playerDataManager = new PlayerDataManager(this);
        classManager = new ClassManager(this);
        levelManager = new LevelManager(this);
        damageManager = new DamageManager(this);
        itemManager = new ItemManager(this);
        skillManager = new SkillManager(this);
        skillRegionManager = new SkillRegionManager(this);
        dungeonManager = new DungeonManager(this);
        partyManager = new PartyManager(this);
        guildManager = new GuildManager(this);
        claimManager = new ClaimManager(this);
        dungeonBuilderManager = new DungeonBuilderManager(this);
        dungeonBuilderManager.loadCustomDungeons();

        registerListeners();
        registerCommands();

        levelManager.startManaRegenTask();

        getLogger().info("DungeonRealms enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        if (dungeonManager != null) {
            dungeonManager.cleanupAll();
        }
        getLogger().info("DungeonRealms disabled.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new SkillBarListener(this), this);
        getServer().getPluginManager().registerEvents(new GuildListener(this), this);
        getServer().getPluginManager().registerEvents(new ClassSwitchListener(this), this);
    }

    private void registerCommands() {
        ClassCommand classCmd = new ClassCommand(this);
        getCommand("class").setExecutor(classCmd);
        getCommand("class").setTabCompleter(classCmd);
        SkillsCommand skillsCmd = new SkillsCommand(this);
        getCommand("skills").setExecutor(skillsCmd);
        getCommand("skills").setTabCompleter(skillsCmd);
        LevelCommand levelCmd = new LevelCommand(this);
        getCommand("level").setExecutor(levelCmd);
        getCommand("level").setTabCompleter(levelCmd);
        PartyCommand partyCmd = new PartyCommand(this);
        getCommand("party").setExecutor(partyCmd);
        getCommand("party").setTabCompleter(partyCmd);
        DungeonCommand dungeonCmd = new DungeonCommand(this);
        getCommand("dungeon").setExecutor(dungeonCmd);
        getCommand("dungeon").setTabCompleter(dungeonCmd);
        ItemsCommand itemsCmd = new ItemsCommand(this);
        getCommand("items").setExecutor(itemsCmd);
        getCommand("items").setTabCompleter(itemsCmd);
        AwakenCommand awakenCmd = new AwakenCommand(this);
        getCommand("awaken").setExecutor(awakenCmd);
        getCommand("awaken").setTabCompleter(awakenCmd);
        DRAdminCommand adminCmd = new DRAdminCommand(this);
        getCommand("dradmin").setExecutor(adminCmd);
        getCommand("dradmin").setTabCompleter(adminCmd);
        SkillRegionCommand srCmd = new SkillRegionCommand(this);
        getCommand("skillregion").setExecutor(srCmd);
        getCommand("skillregion").setTabCompleter(srCmd);
        GuildCommand guildCmd = new GuildCommand(this);
        getCommand("guild").setExecutor(guildCmd);
        getCommand("guild").setTabCompleter(guildCmd);
        HomeCommand homeCmd = new HomeCommand(this);
        getCommand("home").setExecutor(homeCmd);
        getCommand("home").setTabCompleter(homeCmd);
        getCommand("sethome").setExecutor(homeCmd);
        getCommand("delhome").setExecutor(homeCmd);
        getCommand("delhome").setTabCompleter(homeCmd);
        getCommand("homes").setExecutor(homeCmd);
        ProfileCommand profileCmd = new ProfileCommand(this);
        getCommand("profile").setExecutor(profileCmd);
        getCommand("profile").setTabCompleter(profileCmd);
        BalanceCommand balanceCmd = new BalanceCommand(this);
        getCommand("bal").setExecutor(balanceCmd);
        getCommand("bal").setTabCompleter(balanceCmd);
        getCommand("balance").setExecutor(balanceCmd);
        getCommand("balance").setTabCompleter(balanceCmd);
        DungeonBuilderCommand dbCmd = new DungeonBuilderCommand(this);
        getCommand("dungeonbuilder").setExecutor(dbCmd);
        getCommand("dungeonbuilder").setTabCompleter(dbCmd);
        ClassSwitchCommand csCmd = new ClassSwitchCommand(this);
        getCommand("classswitch").setExecutor(csCmd);
    }

    public static DungeonRealms getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SupabaseClient getSupabaseClient() {
        return supabaseClient;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ClassManager getClassManager() {
        return classManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public DamageManager getDamageManager() {
        return damageManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public SkillRegionManager getSkillRegionManager() {
        return skillRegionManager;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public DungeonBuilderManager getDungeonBuilderManager() {
        return dungeonBuilderManager;
    }
}
