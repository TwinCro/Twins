package com.dungeonrealms.items;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemManager {

    private final DungeonRealms plugin;
    private final Map<String, GameItem> items = new LinkedHashMap<>();

    public java.util.Set<String> getAllItemIds() { return items.keySet(); }

    public ItemManager(DungeonRealms plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        items.clear();
        ConfigurationSection section = plugin.getConfigManager().getItemsConfig().getConfigurationSection("items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) continue;

            GameItem item = new GameItem(key);
            item.setDisplayName(itemSection.getString("display-name", key));
            item.setRarity(Rarity.fromString(itemSection.getString("rarity", "COMMON")));
            item.setType(ItemType.fromString(itemSection.getString("type", "SWORD")));
            item.setDamage(itemSection.getDouble("damage", 0));
            item.setDefense(itemSection.getDouble("defense", 0));
            item.setHealth(itemSection.getDouble("health", 0));
            item.setMana(itemSection.getDouble("mana", 0));
            item.setCritChance(itemSection.getDouble("crit-chance", 0));
            item.setCritDamage(itemSection.getDouble("crit-damage", 0));
            item.setAttackSpeed(itemSection.getDouble("attack-speed", 1.0));
            item.setDefensePercent(itemSection.getDouble("defense-percent", 0));
            item.setMagicDefense(itemSection.getDouble("magic-defense", 0));
            item.setDungeon(itemSection.getString("dungeon", null));
            item.setDescription(itemSection.getString("description", ""));

            items.put(key, item);
        }

        plugin.getLogger().info("Loaded " + items.size() + " items.");
    }

    public GameItem getItem(String id) {
        return items.get(id);
    }

    public Collection<GameItem> getAllItems() {
        return items.values();
    }

    public ItemStack createItemStack(String id) {
        GameItem item = items.get(id);
        if (item == null) return null;
        return createItemStack(item);
    }

    public ItemStack createItemStack(GameItem item) {
        Material material = getMaterialForType(item.getType());
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(item.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(item.getRarity().getColor() + item.getRarity().getDisplayName() + " " + item.getType().name().toLowerCase());
        lore.add("");

        if (item.getDamage() > 0)
            lore.add("§cDamage: " + (int) item.getDamage());
        if (item.getDefense() > 0)
            lore.add("§9Defense: " + (int) item.getDefense());
        if (item.getHealth() > 0)
            lore.add("§aHealth: +" + (int) item.getHealth());
        if (item.getMana() > 0)
            lore.add("§bMana: +" + (int) item.getMana());
        if (item.getCritChance() > 0)
            lore.add("§eCrit Chance: " + (int) item.getCritChance() + "%");
        if (item.getCritDamage() > 0)
            lore.add("§eCrit Damage: " + (int) item.getCritDamage() + "%");
        if (item.getDefensePercent() > 0)
            lore.add("§9Damage Reduction: " + (int) item.getDefensePercent() + "%");
        if (item.getMagicDefense() > 0)
            lore.add("§dMagic Defense: " + (int) item.getMagicDefense());
        if (item.getAttackSpeed() != 1.0)
            lore.add("§7Attack Speed: " + item.getAttackSpeed());

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            lore.add("");
            lore.add("§7" + item.getDescription());
        }

        lore.add("");
        lore.add("§8" + item.getId());

        meta.setLore(lore);
        meta.setUnbreakable(true);
        stack.setItemMeta(meta);

        return stack;
    }

    public GameItem getGameItemFromStack(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return null;

        String lastLine = lore.get(lore.size() - 1);
        if (lastLine.startsWith("§8")) {
            String id = lastLine.replace("§8", "").trim();
            return items.get(id);
        }
        return null;
    }

    public GameItem getEquippedWeapon(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack weapon = inv.getItemInMainHand();
        return getGameItemFromStack(weapon);
    }

    public List<GameItem> getEquippedArmor(Player player) {
        List<GameItem> armor = new ArrayList<>();
        PlayerInventory inv = player.getInventory();
        ItemStack[] armorContents = inv.getArmorContents();
        for (ItemStack piece : armorContents) {
            GameItem gi = getGameItemFromStack(piece);
            if (gi != null) armor.add(gi);
        }
        return armor;
    }

    private Material getMaterialForType(ItemType type) {
        return switch (type) {
            case SWORD -> Material.IRON_SWORD;
            case BOW -> Material.BOW;
            case STAFF -> Material.BLAZE_ROD;
            case HELMET -> Material.IRON_HELMET;
            case CHESTPLATE -> Material.IRON_CHESTPLATE;
            case LEGGINGS -> Material.IRON_LEGGINGS;
            case BOOTS -> Material.IRON_BOOTS;
            case ACCESSORY -> Material.GOLD_NUGGET;
            case DUNGEON_KEY -> Material.TRIPWIRE_HOOK;
        };
    }
}
