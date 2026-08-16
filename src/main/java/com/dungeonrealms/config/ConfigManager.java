package com.dungeonrealms.config;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final DungeonRealms plugin;
    private FileConfiguration classesConfig;
    private FileConfiguration skillsConfig;
    private FileConfiguration itemsConfig;
    private FileConfiguration dungeonsConfig;

    public ConfigManager(DungeonRealms plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        classesConfig = loadConfig("classes.yml");
        skillsConfig = loadConfig("skills.yml");
        itemsConfig = loadConfig("items.yml");
        dungeonsConfig = loadConfig("dungeons.yml");
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    private FileConfiguration loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getClassesConfig() {
        return classesConfig;
    }

    public FileConfiguration getSkillsConfig() {
        return skillsConfig;
    }

    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }

    public FileConfiguration getDungeonsConfig() {
        return dungeonsConfig;
    }

    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }
}
