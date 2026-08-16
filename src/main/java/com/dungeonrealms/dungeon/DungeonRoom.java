package com.dungeonrealms.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class DungeonRoom {

    public enum RoomType { LOBBY, MOB, MINIBOSS, BOSS }

    private int index;
    private RoomType type;
    private String worldName;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private Double doorX, doorY, doorZ;
    private String doorMaterial;
    private Double spawnMarkerX, spawnMarkerY, spawnMarkerZ;
    private final List<RoomMobEntry> mobs = new ArrayList<>();

    public static class RoomMobEntry {
        private EntityType type;
        private int count;
        private double health;
        private double damage;
        private double defense;
        private double magicDefense;

        public RoomMobEntry() {}

        public RoomMobEntry(EntityType type, int count, double health, double damage, double defense, double magicDefense) {
            this.type = type;
            this.count = count;
            this.health = health;
            this.damage = damage;
            this.defense = defense;
            this.magicDefense = magicDefense;
        }

        public EntityType getType() { return type; }
        public void setType(EntityType type) { this.type = type; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getHealth() { return health; }
        public void setHealth(double health) { this.health = health; }
        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }
        public double getDefense() { return defense; }
        public void setDefense(double defense) { this.defense = defense; }
        public double getMagicDefense() { return magicDefense; }
        public void setMagicDefense(double magicDefense) { this.magicDefense = magicDefense; }
    }

    public Location getRandomSpawnLocation() {
        double x = minX + Math.random() * (maxX - minX);
        double z = minZ + Math.random() * (maxZ - minZ);
        return new Location(Bukkit.getWorld(worldName), x, minY + 1, z);
    }

    public Location getSpawnMarkerLocation() {
        if (spawnMarkerX == null) return null;
        return new Location(Bukkit.getWorld(worldName), spawnMarkerX, spawnMarkerY, spawnMarkerZ);
    }

    public Location getDoorLocation() {
        if (doorX == null) return null;
        return new Location(Bukkit.getWorld(worldName), doorX, doorY, doorZ);
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    public double getMinX() { return minX; }
    public void setMinX(double minX) { this.minX = minX; }
    public double getMinY() { return minY; }
    public void setMinY(double minY) { this.minY = minY; }
    public double getMinZ() { return minZ; }
    public void setMinZ(double minZ) { this.minZ = minZ; }
    public double getMaxX() { return maxX; }
    public void setMaxX(double maxX) { this.maxX = maxX; }
    public double getMaxY() { return maxY; }
    public void setMaxY(double maxY) { this.maxY = maxY; }
    public double getMaxZ() { return maxZ; }
    public void setMaxZ(double maxZ) { this.maxZ = maxZ; }
    public Double getDoorX() { return doorX; }
    public void setDoorX(Double doorX) { this.doorX = doorX; }
    public Double getDoorY() { return doorY; }
    public void setDoorY(Double doorY) { this.doorY = doorY; }
    public Double getDoorZ() { return doorZ; }
    public void setDoorZ(Double doorZ) { this.doorZ = doorZ; }
    public String getDoorMaterial() { return doorMaterial; }
    public void setDoorMaterial(String doorMaterial) { this.doorMaterial = doorMaterial; }
    public Double getSpawnMarkerX() { return spawnMarkerX; }
    public void setSpawnMarkerX(Double x) { this.spawnMarkerX = x; }
    public Double getSpawnMarkerY() { return spawnMarkerY; }
    public void setSpawnMarkerY(Double y) { this.spawnMarkerY = y; }
    public Double getSpawnMarkerZ() { return spawnMarkerZ; }
    public void setSpawnMarkerZ(Double z) { this.spawnMarkerZ = z; }
    public List<RoomMobEntry> getMobs() { return mobs; }
}
