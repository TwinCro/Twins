package com.dungeonrealms.dungeon;

public enum DungeonRank {
    COMMON("&f", "Common", 0, 6),
    UNCOMMON("&a", "Uncommon", 1, 6),
    RARE("&9", "Rare", 2, 7),
    EPIC("&5", "Epic", 3, 8),
    LEGENDARY("&6", "Legendary", 4, 9);

    private final String color;
    private final String displayName;
    private final int tier;
    private final int roomCount;

    DungeonRank(String color, String displayName, int tier, int roomCount) {
        this.color = color;
        this.displayName = displayName;
        this.tier = tier;
        this.roomCount = roomCount;
    }

    public String getColor() { return color; }
    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }
    public int getRoomCount() { return roomCount; }

    public static DungeonRank fromString(String str) {
        if (str == null) return COMMON;
        for (DungeonRank r : values()) {
            if (r.name().equalsIgnoreCase(str)) return r;
        }
        return COMMON;
    }
}
