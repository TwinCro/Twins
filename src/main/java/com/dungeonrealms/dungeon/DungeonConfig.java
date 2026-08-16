package com.dungeonrealms.dungeon;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class DungeonConfig {

    private final String id;
    private String displayName;
    private DungeonRank rank;
    private int minLevel;
    private int maxPlayers;
    private String spawnWorld;
    private double spawnX;
    private double spawnY;
    private double spawnZ;
    private BossConfig boss;
    private List<MobWave> mobs = new ArrayList<>();
    private LootTable lootTable;
    private int chestCount;
    private List<DungeonRoom> rooms = new ArrayList<>();
    private boolean custom = false;
    private Location lobbySpawn;

    public DungeonConfig(String id) {
        this.id = id;
    }

    public static class BossConfig {
        private EntityType type;
        private String name;
        private double health;
        private double damage;
        private double defense;
        private double magicDefense;

        public EntityType getType() { return type; }
        public void setType(EntityType type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getHealth() { return health; }
        public void setHealth(double health) { this.health = health; }
        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }
        public double getDefense() { return defense; }
        public void setDefense(double defense) { this.defense = defense; }
        public double getMagicDefense() { return magicDefense; }
        public void setMagicDefense(double magicDefense) { this.magicDefense = magicDefense; }
    }

    public static class MobWave {
        private EntityType type;
        private int count;
        private double health;
        private double damage;
        private double defense;
        private double magicDefense;

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

    public static class LootTable {
        private final List<LootEntry> entries = new ArrayList<>();

        public List<LootEntry> getEntries() { return entries; }

        public static class LootEntry {
            private final String itemId;
            private final double chance;

            public LootEntry(String itemId, double chance) {
                this.itemId = itemId;
                this.chance = chance;
            }

            public String getItemId() { return itemId; }
            public double getChance() { return chance; }
        }
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public DungeonRank getRank() { return rank; }
    public void setRank(DungeonRank rank) { this.rank = rank; }
    public int getMinLevel() { return minLevel; }
    public void setMinLevel(int minLevel) { this.minLevel = minLevel; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public String getSpawnWorld() { return spawnWorld; }
    public void setSpawnWorld(String spawnWorld) { this.spawnWorld = spawnWorld; }
    public double getSpawnX() { return spawnX; }
    public void setSpawnX(double spawnX) { this.spawnX = spawnX; }
    public double getSpawnY() { return spawnY; }
    public void setSpawnY(double spawnY) { this.spawnY = spawnY; }
    public double getSpawnZ() { return spawnZ; }
    public void setSpawnZ(double spawnZ) { this.spawnZ = spawnZ; }
    public BossConfig getBoss() { return boss; }
    public void setBoss(BossConfig boss) { this.boss = boss; }
    public List<MobWave> getMobs() { return mobs; }
    public void setMobs(List<MobWave> mobs) { this.mobs = mobs; }
    public LootTable getLootTable() { return lootTable; }
    public void setLootTable(LootTable lootTable) { this.lootTable = lootTable; }
    public int getChestCount() { return chestCount; }
    public void setChestCount(int chestCount) { this.chestCount = chestCount; }
    public List<DungeonRoom> getRooms() { return rooms; }
    public void setRooms(List<DungeonRoom> rooms) { this.rooms = rooms; }
    public boolean hasRooms() { return !rooms.isEmpty(); }
    public boolean isCustom() { return custom; }
    public void setCustom(boolean custom) { this.custom = custom; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location lobbySpawn) { this.lobbySpawn = lobbySpawn; }
}
