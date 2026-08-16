package com.dungeonrealms.skills;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.api.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkillRegionManager {

    private final DungeonRealms plugin;
    private final Map<String, SkillRegion> regions = new ConcurrentHashMap<>();

    public SkillRegionManager(DungeonRealms plugin) {
        this.plugin = plugin;
        loadRegions();
    }

    public void loadRegions() {
        SupabaseClient supabase = plugin.getSupabaseClient();
        supabase.getSkillRegions().thenAccept(arr -> {
            if (arr == null) return;
            regions.clear();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                String name = obj.get("region_name").getAsString();
                String world = obj.get("world_name").getAsString();
                double minX = obj.get("min_x").getAsDouble();
                double minY = obj.get("min_y").getAsDouble();
                double minZ = obj.get("min_z").getAsDouble();
                double maxX = obj.get("max_x").getAsDouble();
                double maxY = obj.get("max_y").getAsDouble();
                double maxZ = obj.get("max_z").getAsDouble();
                regions.put(name.toLowerCase(), new SkillRegion(name, world, minX, minY, minZ, maxX, maxY, maxZ));
            }
            plugin.getLogger().info("Loaded " + regions.size() + " skill regions.");
        });
    }

    public boolean isInSkillRegion(Location loc) {
        if (loc.getWorld() == null) return false;
        String worldName = loc.getWorld().getName();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        for (SkillRegion region : regions.values()) {
            if (!region.worldName.equals(worldName)) continue;
            if (x >= region.minX && x <= region.maxX &&
                y >= region.minY && y <= region.maxY &&
                z >= region.minZ && z <= region.maxZ) {
                return true;
            }
        }
        return false;
    }

    public boolean isInDungeon(Player player) {
        for (var instance : plugin.getDungeonManager().getActiveInstances()) {
            if (instance.getPlayers().contains(player)) return true;
        }
        return false;
    }

    public boolean canUseSkills(Player player) {
        return isInDungeon(player) || isInSkillRegion(player.getLocation());
    }

    public SkillRegion getRegion(String name) {
        return regions.get(name.toLowerCase());
    }

    public Collection<SkillRegion> getAllRegions() {
        return regions.values();
    }

    public void addRegion(String name, String world, double minX, double minY, double minZ,
                          double maxX, double maxY, double maxZ) {
        regions.put(name.toLowerCase(), new SkillRegion(name, world, minX, minY, minZ, maxX, maxY, maxZ));
        plugin.getSupabaseClient().insertSkillRegion(name, world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean removeRegion(String name) {
        SkillRegion removed = regions.remove(name.toLowerCase());
        if (removed != null) {
            plugin.getSupabaseClient().deleteSkillRegion(name);
            return true;
        }
        return false;
    }

    public static class SkillRegion {
        public final String name;
        public final String worldName;
        public final double minX, minY, minZ, maxX, maxY, maxZ;

        public SkillRegion(String name, String worldName, double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ) {
            this.name = name;
            this.worldName = worldName;
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }
    }
}
