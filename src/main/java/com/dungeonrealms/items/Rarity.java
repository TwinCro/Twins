package com.dungeonrealms.items;

public enum Rarity {
    COMMON("&f", "Common", 0),
    UNCOMMON("&a", "Uncommon", 1),
    RARE("&9", "Rare", 2),
    EPIC("&5", "Epic", 3),
    LEGENDARY("&6", "Legendary", 4),
    MYTHIC("&d", "Mythic", 5);

    private final String color;
    private final String displayName;
    private final int tier;

    Rarity(String color, String displayName, int tier) {
        this.color = color;
        this.displayName = displayName;
        this.tier = tier;
    }

    public String getColor() { return color; }
    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }

    public static Rarity fromString(String str) {
        if (str == null) return COMMON;
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(str)) return r;
        }
        return COMMON;
    }
}
