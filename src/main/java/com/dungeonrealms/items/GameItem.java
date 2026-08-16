package com.dungeonrealms.items;

public class GameItem {

    private final String id;
    private String displayName;
    private Rarity rarity;
    private ItemType type;
    private double damage;
    private double defense;
    private double health;
    private double mana;
    private double critChance;
    private double critDamage;
    private double attackSpeed;
    private double defensePercent;
    private double magicDefense;
    private String dungeon;
    private String description;

    public GameItem(String id) {
        this.id = id;
        this.rarity = Rarity.COMMON;
        this.type = ItemType.SWORD;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public double getDefense() { return defense; }
    public void setDefense(double defense) { this.defense = defense; }
    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }
    public double getMana() { return mana; }
    public void setMana(double mana) { this.mana = mana; }
    public double getCritChance() { return critChance; }
    public void setCritChance(double critChance) { this.critChance = critChance; }
    public double getCritDamage() { return critDamage; }
    public void setCritDamage(double critDamage) { this.critDamage = critDamage; }
    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }
    public double getDefensePercent() { return defensePercent; }
    public void setDefensePercent(double defensePercent) { this.defensePercent = defensePercent; }
    public double getMagicDefense() { return magicDefense; }
    public void setMagicDefense(double magicDefense) { this.magicDefense = magicDefense; }
    public String getDungeon() { return dungeon; }
    public void setDungeon(String dungeon) { this.dungeon = dungeon; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
