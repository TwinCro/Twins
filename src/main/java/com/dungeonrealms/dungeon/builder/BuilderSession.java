package com.dungeonrealms.dungeon.builder;

import com.dungeonrealms.dungeon.DungeonConfig;
import com.dungeonrealms.dungeon.DungeonRank;
import com.dungeonrealms.dungeon.DungeonRoom;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BuilderSession {

    public enum BuilderStep {
        SET_LOBBY_CORNER1,
        SET_LOBBY_CORNER2,
        SET_LOBBY_DOOR,
        ADD_ROOM_CORNER1,
        ADD_ROOM_CORNER2,
        SET_ROOM_DOOR,
        SET_ROOM_MARKER,
        SET_ROOM_MOBS,
        SET_BOSS_CORNER1,
        SET_BOSS_CORNER2,
        SET_BOSS_MARKER,
        SET_BOSS_MOBS,
        CONFIRM_SAVE
    }

    private final UUID adminUuid;
    private final String dungeonName;
    private final DungeonRank rank;
    private final int requiredMobRooms;

    private BuilderStep currentStep;
    private final List<DungeonRoom> rooms = new ArrayList<>();
    private DungeonRoom currentRoom;
    private int currentRoomNumber = 0;
    private Location tempCorner1;

    private Location lobbySpawn;
    private DungeonConfig.BossConfig bossConfig;
    private int chestCount = 3;

    public BuilderSession(UUID adminUuid, String dungeonName, DungeonRank rank) {
        this.adminUuid = adminUuid;
        this.dungeonName = dungeonName;
        this.rank = rank;
        this.requiredMobRooms = rank.getRoomCount();
        this.currentStep = BuilderStep.SET_LOBBY_CORNER1;
    }

    public int getTotalRoomCount() {
        return 1 + requiredMobRooms + 1;
    }

    public int getCompletedRoomCount() {
        return rooms.size();
    }

    public UUID getAdminUuid() { return adminUuid; }
    public String getDungeonName() { return dungeonName; }
    public DungeonRank getRank() { return rank; }
    public int getRequiredMobRooms() { return requiredMobRooms; }
    public BuilderStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(BuilderStep step) { this.currentStep = step; }
    public List<DungeonRoom> getRooms() { return rooms; }
    public DungeonRoom getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(DungeonRoom room) { this.currentRoom = room; }
    public int getCurrentRoomNumber() { return currentRoomNumber; }
    public void setCurrentRoomNumber(int n) { this.currentRoomNumber = n; }
    public Location getTempCorner1() { return tempCorner1; }
    public void setTempCorner1(Location loc) { this.tempCorner1 = loc; }
    public Location getLobbySpawn() { return lobbySpawn; }
    public void setLobbySpawn(Location loc) { this.lobbySpawn = loc; }
    public DungeonConfig.BossConfig getBossConfig() { return bossConfig; }
    public void setBossConfig(DungeonConfig.BossConfig config) { this.bossConfig = config; }
    public int getChestCount() { return chestCount; }
    public void setChestCount(int count) { this.chestCount = count; }
}
