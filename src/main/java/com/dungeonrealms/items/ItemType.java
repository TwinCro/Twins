package com.dungeonrealms.items;

public enum ItemType {
    SWORD, BOW, STAFF, HELMET, CHESTPLATE, LEGGINGS, BOOTS, ACCESSORY, DUNGEON_KEY;

    public static ItemType fromString(String str) {
        if (str == null) return SWORD;
        for (ItemType t : values()) {
            if (t.name().equalsIgnoreCase(str)) return t;
        }
        return SWORD;
    }
}
