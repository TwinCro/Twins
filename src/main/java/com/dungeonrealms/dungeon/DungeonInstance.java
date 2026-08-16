package com.dungeonrealms.dungeon;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class DungeonInstance {

    private final DungeonConfig config;
    private final Set<Player> players = new HashSet<>();
    private final List<LivingEntity> mobs = new ArrayList<>();
    private LivingEntity boss;
    private boolean bossSpawned = false;
    private boolean completed = false;
    private final long startTime;
    private double healthMultiplier = 1.0;
    private double damageMultiplier = 1.0;
    private double defenseMultiplier = 1.0;
    private int currentRoomIndex = 0;
    private final Map<Integer, List<LivingEntity>> roomMobs = new HashMap<>();

    public DungeonInstance(DungeonConfig config) {
        this.config = config;
        this.startTime = System.currentTimeMillis();
    }

    public DungeonConfig getConfig() { return config; }
    public Set<Player> getPlayers() { return players; }
    public void addPlayer(Player p) { players.add(p); }
    public void removePlayer(Player p) { players.remove(p); }
    public List<LivingEntity> getMobs() { return mobs; }
    public void addMob(LivingEntity mob) { mobs.add(mob); }
    public LivingEntity getBoss() { return boss; }
    public void setBoss(LivingEntity boss) { this.boss = boss; }
    public boolean isBossSpawned() { return bossSpawned; }
    public void setBossSpawned(boolean spawned) { this.bossSpawned = spawned; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public long getStartTime() { return startTime; }
    public boolean isEmpty() { return players.isEmpty(); }
    public double getHealthMultiplier() { return healthMultiplier; }
    public void setHealthMultiplier(double healthMultiplier) { this.healthMultiplier = healthMultiplier; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(double damageMultiplier) { this.damageMultiplier = damageMultiplier; }
    public double getDefenseMultiplier() { return defenseMultiplier; }
    public void setDefenseMultiplier(double defenseMultiplier) { this.defenseMultiplier = defenseMultiplier; }

    public int getCurrentRoomIndex() { return currentRoomIndex; }
    public void setCurrentRoomIndex(int i) { this.currentRoomIndex = i; }

    public Map<Integer, List<LivingEntity>> getRoomMobs() { return roomMobs; }

    public void addRoomMob(int roomIndex, LivingEntity mob) {
        roomMobs.computeIfAbsent(roomIndex, k -> new ArrayList<>()).add(mob);
    }

    public boolean areAllRoomMobsDead(int roomIndex) {
        List<LivingEntity> list = roomMobs.get(roomIndex);
        if (list == null || list.isEmpty()) return true;
        list.removeIf(m -> m == null || m.isDead());
        return list.isEmpty();
    }
}
