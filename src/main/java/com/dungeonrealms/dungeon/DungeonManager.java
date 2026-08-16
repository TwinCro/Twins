package com.dungeonrealms.dungeon;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.data.PlayerDataManager;
import com.dungeonrealms.items.GameItem;
import com.dungeonrealms.items.ItemManager;
import com.dungeonrealms.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DungeonManager {

    private final DungeonRealms plugin;
    private final Map<String, DungeonConfig> dungeons = new LinkedHashMap<>();
    private final List<DungeonInstance> activeInstances = new ArrayList<>();

    public DungeonManager(DungeonRealms plugin) {
        this.plugin = plugin;
        loadDungeons();
    }

    public void loadDungeons() {
        dungeons.clear();
        ConfigurationSection section = plugin.getConfigManager().getDungeonsConfig().getConfigurationSection("dungeons");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection dgSection = section.getConfigurationSection(key);
            if (dgSection == null) continue;

            DungeonConfig dg = new DungeonConfig(key);
            dg.setDisplayName(dgSection.getString("display-name", key));
            dg.setRank(DungeonRank.fromString(dgSection.getString("rank", "COMMON")));
            dg.setMinLevel(dgSection.getInt("min-level", 1));
            dg.setMaxPlayers(dgSection.getInt("max-players", 5));
            dg.setSpawnWorld(dgSection.getString("spawn-world", "world"));
            dg.setSpawnX(dgSection.getDouble("spawn-x", 0));
            dg.setSpawnY(dgSection.getDouble("spawn-y", 64));
            dg.setSpawnZ(dgSection.getDouble("spawn-z", 0));

            // Boss
            ConfigurationSection bossSection = dgSection.getConfigurationSection("boss");
            if (bossSection != null) {
                DungeonConfig.BossConfig boss = new DungeonConfig.BossConfig();
                boss.setType(EntityType.valueOf(bossSection.getString("type", "ZOMBIE").toUpperCase()));
                boss.setName(bossSection.getString("name", "Boss"));
                boss.setHealth(bossSection.getDouble("health", 100));
                boss.setDamage(bossSection.getDouble("damage", 10));
                boss.setDefense(bossSection.getDouble("defense", 0));
                boss.setMagicDefense(bossSection.getDouble("magic-defense", 0));
                dg.setBoss(boss);
            }

            // Mobs
            List<?> mobList = dgSection.getList("mobs");
            if (mobList != null) {
                for (Object obj : mobList) {
                    if (obj instanceof Map<?, ?> map) {
                        DungeonConfig.MobWave wave = new DungeonConfig.MobWave();
                        wave.setType(EntityType.valueOf(((String) map.get("type")).toUpperCase()));
                        wave.setCount(((Number) map.get("count")).intValue());
                        wave.setHealth(((Number) map.get("health")).doubleValue());
                        wave.setDamage(((Number) map.get("damage")).doubleValue());
                        Object defObj = map.get("defense");
                        wave.setDefense(defObj instanceof Number ? ((Number) defObj).doubleValue() : 0);
                        Object magDefObj = map.get("magic-defense");
                        wave.setMagicDefense(magDefObj instanceof Number ? ((Number) magDefObj).doubleValue() : 0);
                        dg.getMobs().add(wave);
                    }
                }
            }

            // Loot
            ConfigurationSection lootSection = dgSection.getConfigurationSection("loot");
            if (lootSection != null) {
                dg.setChestCount(lootSection.getInt("chest-count", 3));
                DungeonConfig.LootTable table = new DungeonConfig.LootTable();
                ConfigurationSection tableSection = lootSection.getConfigurationSection("table");
                if (tableSection != null) {
                    for (String lootKey : tableSection.getKeys(false)) {
                        ConfigurationSection entry = tableSection.getConfigurationSection(lootKey);
                        if (entry != null) {
                            String itemId = entry.getString("item-id", "");
                            double chance = entry.getDouble("chance", 0);
                            table.getEntries().add(new DungeonConfig.LootTable.LootEntry(itemId, chance));
                        }
                    }
                }
                dg.setLootTable(table);
            }

            dungeons.put(key, dg);
        }

        plugin.getLogger().info("Loaded " + dungeons.size() + " dungeons.");
    }

    public DungeonConfig getDungeon(String id) {
        return dungeons.get(id);
    }

    public Collection<DungeonConfig> getAllDungeons() {
        return dungeons.values();
    }

    public void registerCustomDungeon(DungeonConfig config) {
        dungeons.put(config.getId(), config);
    }

    public void unregisterCustomDungeon(String dungeonName) {
        dungeons.entrySet().removeIf(e -> e.getValue().getDisplayName().equals(dungeonName));
    }

    public DungeonInstance getInstanceByPlayer(Player player) {
        for (DungeonInstance inst : activeInstances) {
            if (inst.getPlayers().contains(player)) return inst;
        }
        return null;
    }

    public boolean enterDungeon(Player player, String dungeonId) {
        DungeonConfig dg = dungeons.get(dungeonId);
        if (dg == null) {
            player.sendMessage("§cDungeon not found: " + dungeonId);
            return false;
        }

        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage("§cYou must choose a class first! Use §e/class choose <class>");
            return false;
        }

        if (data.getLevel() < dg.getMinLevel()) {
            player.sendMessage("§cYou must be at least level " + dg.getMinLevel() + " to enter this dungeon.");
            return false;
        }

        DungeonInstance instance = new DungeonInstance(dg);

        Party party = plugin.getPartyManager().getPartyByMember(player.getUniqueId());
        if (party != null) {
            for (UUID memberUuid : party.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    PlayerDataManager.PlayerData memberData = plugin.getPlayerDataManager().getPlayerData(member);
                    if (memberData != null && memberData.hasClass() && memberData.getLevel() >= dg.getMinLevel()) {
                        instance.addPlayer(member);
                    }
                }
            }
        } else {
            instance.addPlayer(player);
        }

        if (instance.getPlayers().size() > dg.getMaxPlayers()) {
            player.sendMessage("§cToo many players for this dungeon (max " + dg.getMaxPlayers() + ").");
            return false;
        }

        activeInstances.add(instance);
        applyDifficultyScaling(instance);

        if (dg.hasRooms()) {
            return enterRoomDungeon(player, instance, dg);
        }

        Location spawnLoc = new Location(
                Bukkit.getWorld(dg.getSpawnWorld()),
                dg.getSpawnX(), dg.getSpawnY(), dg.getSpawnZ()
        );
        for (Player p : instance.getPlayers()) {
            p.teleport(spawnLoc);
            p.sendMessage(dg.getRank().getColor() + "§lEntering " + dg.getDisplayName() + " §7(" + dg.getRank().getDisplayName() + ")");
        }

        spawnMobs(instance);
        return true;
    }

    private boolean enterRoomDungeon(Player player, DungeonInstance instance, DungeonConfig dg) {
        Location lobbySpawn = dg.getLobbySpawn();
        if (lobbySpawn == null) {
            lobbySpawn = new Location(Bukkit.getWorld(dg.getSpawnWorld()),
                    dg.getSpawnX(), dg.getSpawnY(), dg.getSpawnZ());
        }

        for (Player p : instance.getPlayers()) {
            p.teleport(lobbySpawn);
            p.sendMessage(dg.getRank().getColor().replace("&", "§") + "§lEntering " + dg.getDisplayName()
                    + " §7(" + dg.getRank().getDisplayName() + ")");
            p.sendMessage("§7You are in the lobby. The dungeon has §e"
                    + (dg.getRooms().size() - 2) + " §7rooms and a boss.");
        }

        for (DungeonRoom room : dg.getRooms()) {
            closeDoor(room);
        }

        instance.setCurrentRoomIndex(1);
        if (dg.getRooms().size() > 1) {
            openDoor(dg.getRooms().get(0));
            spawnRoomMobs(instance, 1);
            for (Player p : instance.getPlayers()) {
                p.sendMessage("§e§lThe first room is open! Fight your way through!");
            }
        }

        return true;
    }

    private void applyDifficultyScaling(DungeonInstance instance) {
        int playerCount = instance.getPlayers().size();
        if (playerCount <= 1) return;
        double healthMult = 1.0 + (playerCount - 1) * 0.5;
        double damageMult = 1.0 + (playerCount - 1) * 0.3;
        double defenseMult = 1.0 + (playerCount - 1) * 0.2;
        instance.setHealthMultiplier(healthMult);
        instance.setDamageMultiplier(damageMult);
        instance.setDefenseMultiplier(defenseMult);
        for (Player p : instance.getPlayers()) {
            p.sendMessage("§7Dungeon difficulty scaled for §e" + playerCount + " §7players: HP x" + String.format("%.1f", healthMult) + ", DMG x" + String.format("%.1f", damageMult) + ", DEF x" + String.format("%.1f", defenseMult));
        }
    }

    private void spawnMobs(DungeonInstance instance) {
        DungeonConfig dg = instance.getConfig();
        Location spawnLoc = new Location(
                Bukkit.getWorld(dg.getSpawnWorld()),
                dg.getSpawnX(), dg.getSpawnY(), dg.getSpawnZ()
        );

        for (DungeonConfig.MobWave wave : dg.getMobs()) {
            for (int i = 0; i < wave.getCount(); i++) {
                double offsetX = (Math.random() - 0.5) * 10;
                double offsetZ = (Math.random() - 0.5) * 10;
                Location mobLoc = spawnLoc.clone().add(offsetX, 0, offsetZ);
                LivingEntity mob = (LivingEntity) spawnLoc.getWorld().spawnEntity(mobLoc, wave.getType());
                double scaledHealth = wave.getHealth() * instance.getHealthMultiplier();
                mob.setMaxHealth(scaledHealth);
                mob.setHealth(scaledHealth);
                double scaledDamage = wave.getDamage() * instance.getDamageMultiplier();
                double scaledDefense = wave.getDefense() * instance.getDefenseMultiplier();
                double scaledMagicDefense = wave.getMagicDefense() * instance.getDefenseMultiplier();
                mob.setCustomName("§c" + wave.getType().name() + " §7(" + (int) scaledHealth + " HP)");
                mob.setCustomNameVisible(true);
                mob.addScoreboardTag("dr_mob");
                mob.addScoreboardTag("dr_defense:" + scaledDefense);
                mob.addScoreboardTag("dr_magic_defense:" + scaledMagicDefense);
                mob.addScoreboardTag("dr_damage:" + scaledDamage);
                instance.addMob(mob);
            }
        }

        // Schedule boss spawn after mobs are cleared
        new BukkitRunnable() {
            @Override
            public void run() {
                if (instance.isEmpty() || instance.isCompleted()) {
                    cancel();
                    return;
                }
                // Check if all mobs are dead
                instance.getMobs().removeIf(m -> m == null || m.isDead());
                if (instance.getMobs().isEmpty() && !instance.isBossSpawned()) {
                    spawnBoss(instance);
                    startBossWatcher(instance);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 100L, 40L);
    }

    private void startBossWatcher(DungeonInstance instance) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (instance.isEmpty() || instance.isCompleted()) {
                    cancel();
                    return;
                }
                LivingEntity boss = instance.getBoss();
                if (boss == null || boss.isDead()) {
                    onBossDeath(instance);
                    cancel();
                    return;
                }
                // Keep flying bosses near the dungeon spawn point
                DungeonConfig dg = instance.getConfig();
                Location spawnLoc = new Location(
                        Bukkit.getWorld(dg.getSpawnWorld()),
                        dg.getSpawnX(), dg.getSpawnY(), dg.getSpawnZ()
                );
                double distance = boss.getLocation().distance(spawnLoc);
                if (distance > 30) {
                    Location dir = spawnLoc.clone().subtract(boss.getLocation()).toLocation(spawnLoc.getWorld());
                    dir.normalize().multiply(20);
                    Location newLoc = boss.getLocation().add(dir);
                    newLoc.setY(spawnLoc.getY() + 5);
                    boss.teleport(newLoc);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void spawnBoss(DungeonInstance instance) {
        DungeonConfig dg = instance.getConfig();
        DungeonConfig.BossConfig bossConfig = dg.getBoss();
        if (bossConfig == null) return;

        Location spawnLoc = new Location(
                Bukkit.getWorld(dg.getSpawnWorld()),
                dg.getSpawnX(), dg.getSpawnY(), dg.getSpawnZ()
        );

        LivingEntity boss = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, bossConfig.getType());
        double scaledBossHealth = bossConfig.getHealth() * instance.getHealthMultiplier();
        boss.setMaxHealth(scaledBossHealth);
        boss.setHealth(scaledBossHealth);
        double scaledBossDamage = bossConfig.getDamage() * instance.getDamageMultiplier();
        double scaledBossDefense = bossConfig.getDefense() * instance.getDefenseMultiplier();
        double scaledBossMagicDefense = bossConfig.getMagicDefense() * instance.getDefenseMultiplier();
        boss.setCustomName(bossConfig.getName() + " §7(" + (int) scaledBossHealth + " HP)");
        boss.setCustomNameVisible(true);
        boss.addScoreboardTag("dr_boss");
        boss.addScoreboardTag("dr_defense:" + scaledBossDefense);
        boss.addScoreboardTag("dr_magic_defense:" + scaledBossMagicDefense);
        boss.addScoreboardTag("dr_damage:" + scaledBossDamage);

        instance.setBoss(boss);
        instance.setBossSpawned(true);

        for (Player p : instance.getPlayers()) {
            p.sendMessage("§4§lThe boss has appeared: " + bossConfig.getName() + "!");
        }
    }

    public void onBossDeath(DungeonInstance instance) {
        if (instance.isCompleted()) return;
        instance.setCompleted(true);
        spawnLootChests(instance);
        DungeonConfig dg = instance.getConfig();
        long baseXp = plugin.getConfigManager().getMainConfig()
                .getLong("leveling.xp-sources.dungeon-complete", 500);
        long xpReward = (long) (baseXp * getRankXpMultiplier(dg.getRank()));
        long baseGold = plugin.getConfigManager().getMainConfig()
                .getLong("guild.gold-sources.dungeon-complete", 100);
        long goldReward = (long) (baseGold * getRankXpMultiplier(dg.getRank()));
        for (Player p : instance.getPlayers()) {
            p.sendMessage("§a§lDungeon Complete! §eLoot chests have spawned.");
            p.sendMessage("§7XP earned: §e" + xpReward + " §7| Gold earned: §e" + goldReward);
            plugin.getLevelManager().addXp(p, xpReward);
            var data = plugin.getPlayerDataManager().getPlayerData(p);
            if (data != null) {
                data.addGold(goldReward);
                plugin.getSupabaseClient().updatePlayerGold(p.getUniqueId(), data.getGold());
            }
        }
    }

    private double getRankXpMultiplier(DungeonRank rank) {
        return switch (rank) {
            case COMMON -> 1.0;
            case UNCOMMON -> 2.0;
            case RARE -> 4.0;
            case EPIC -> 8.0;
            case LEGENDARY -> 15.0;
        };
    }

    private void spawnLootChests(DungeonInstance instance) {
        DungeonConfig dg = instance.getConfig();
        Location baseLoc = new Location(
                Bukkit.getWorld(dg.getSpawnWorld()),
                dg.getSpawnX(), dg.getSpawnY(), dg.getSpawnZ()
        );

        int chestCount = dg.getChestCount();
        for (int i = 0; i < chestCount; i++) {
            double offsetX = (Math.random() - 0.5) * 6;
            double offsetZ = (Math.random() - 0.5) * 6;
            Location chestLoc = baseLoc.clone().add(offsetX, 0, offsetZ);
            chestLoc.getBlock().setType(Material.CHEST);
            Chest chest = (Chest) chestLoc.getBlock().getState();
            chest.getInventory().clear();
            chest.getInventory().addItem(generateLoot(dg));
        }
    }

    private ItemStack generateLoot(DungeonConfig dg) {
        DungeonConfig.LootTable table = dg.getLootTable();
        if (table == null || table.getEntries().isEmpty()) return new ItemStack(Material.AIR);

        List<DungeonConfig.LootTable.LootEntry> rolled = new ArrayList<>();
        for (DungeonConfig.LootTable.LootEntry entry : table.getEntries()) {
            if (Math.random() * 100 < entry.getChance()) {
                rolled.add(entry);
            }
        }

        if (rolled.isEmpty()) {
            // Pick a random entry as fallback
            DungeonConfig.LootTable.LootEntry fallback = table.getEntries().get(
                    (int)(Math.random() * table.getEntries().size())
            );
            rolled.add(fallback);
        }

        // Pick one random from rolled
        DungeonConfig.LootTable.LootEntry chosen = rolled.get((int)(Math.random() * rolled.size()));
        ItemManager im = plugin.getItemManager();
        ItemStack item = im.createItemStack(chosen.getItemId());
        return item != null ? item : new ItemStack(Material.AIR);
    }

    public DungeonInstance getInstanceByBoss(LivingEntity entity) {
        for (DungeonInstance instance : activeInstances) {
            if (entity.equals(instance.getBoss())) return instance;
        }
        return null;
    }

    public void leaveDungeon(Player player) {
        for (DungeonInstance instance : activeInstances) {
            if (instance.getPlayers().contains(player)) {
                instance.removePlayer(player);
                player.teleport(player.getWorld().getSpawnLocation());
                player.sendMessage("§aYou left the dungeon.");
                if (instance.isEmpty()) {
                    cleanupInstance(instance);
                }
                return;
            }
        }
        player.sendMessage("§cYou are not in a dungeon.");
    }

    public void spawnRoomMobs(DungeonInstance instance, int roomIndex) {
        DungeonConfig dg = instance.getConfig();
        if (roomIndex >= dg.getRooms().size()) return;
        DungeonRoom room = dg.getRooms().get(roomIndex);

        for (DungeonRoom.RoomMobEntry mob : room.getMobs()) {
            for (int i = 0; i < mob.getCount(); i++) {
                Location loc;
                if ((room.getType() == DungeonRoom.RoomType.MINIBOSS || room.getType() == DungeonRoom.RoomType.BOSS)
                        && i == 0 && room.getSpawnMarkerLocation() != null) {
                    loc = room.getSpawnMarkerLocation();
                } else {
                    loc = room.getRandomSpawnLocation();
                }

                LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, mob.getType());
                double scaledHp = mob.getHealth() * instance.getHealthMultiplier();
                entity.setMaxHealth(scaledHp);
                entity.setHealth(scaledHp);
                double scaledDmg = mob.getDamage() * instance.getDamageMultiplier();
                double scaledDef = mob.getDefense() * instance.getDefenseMultiplier();
                double scaledMdef = mob.getMagicDefense() * instance.getDefenseMultiplier();

                String prefix = (room.getType() == DungeonRoom.RoomType.BOSS) ? "§4§l" : "§c";
                entity.setCustomName(prefix + mob.getType().name() + " §7(" + (int) scaledHp + " HP)");
                entity.setCustomNameVisible(true);

                entity.addScoreboardTag("dr_mob");
                entity.addScoreboardTag("dr_dungeon:" + dg.getId());
                entity.addScoreboardTag("dr_room:" + roomIndex);
                entity.addScoreboardTag("dr_defense:" + scaledDef);
                entity.addScoreboardTag("dr_magic_defense:" + scaledMdef);
                entity.addScoreboardTag("dr_damage:" + scaledDmg);

                if (room.getType() == DungeonRoom.RoomType.BOSS && i == 0) {
                    entity.addScoreboardTag("dr_boss");
                    instance.setBoss(entity);
                    instance.setBossSpawned(true);
                }

                instance.addMob(entity);
                instance.addRoomMob(roomIndex, entity);
            }
        }

        String roomLabel;
        if (room.getType() == DungeonRoom.RoomType.BOSS) {
            roomLabel = "§4§lBoss Room";
        } else if (room.getType() == DungeonRoom.RoomType.MINIBOSS) {
            roomLabel = "§5§lMiniboss Room " + roomIndex;
        } else {
            roomLabel = "§c§lRoom " + roomIndex;
        }
        for (Player p : instance.getPlayers()) {
            p.sendMessage(roomLabel + " §7— Enemies have appeared!");
        }
    }

    public void onRoomMobDeath(DungeonInstance instance, int roomIndex) {
        if (instance.areAllRoomMobsDead(roomIndex)) {
            onRoomCleared(instance, roomIndex);
        }
    }

    private void onRoomCleared(DungeonInstance instance, int roomIndex) {
        DungeonConfig dg = instance.getConfig();
        List<DungeonRoom> rooms = dg.getRooms();
        DungeonRoom clearedRoom = rooms.get(roomIndex);

        if (clearedRoom.getType() == DungeonRoom.RoomType.BOSS) {
            onBossDeath(instance);
            return;
        }

        openDoor(clearedRoom);

        int nextIndex = roomIndex + 1;
        if (nextIndex >= rooms.size()) return;

        instance.setCurrentRoomIndex(nextIndex);
        for (Player p : instance.getPlayers()) {
            p.sendMessage("§a§lRoom cleared! §eThe way forward is open.");
        }

        spawnRoomMobs(instance, nextIndex);
    }

    public void openDoor(DungeonRoom room) {
        Location doorLoc = room.getDoorLocation();
        if (doorLoc != null && doorLoc.getWorld() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> doorLoc.getBlock().setType(Material.AIR));
        }
    }

    public void closeDoor(DungeonRoom room) {
        Location doorLoc = room.getDoorLocation();
        if (doorLoc != null && doorLoc.getWorld() != null && room.getDoorMaterial() != null) {
            try {
                Material mat = Material.valueOf(room.getDoorMaterial());
                Bukkit.getScheduler().runTask(plugin, () -> doorLoc.getBlock().setType(mat));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void cleanupInstance(DungeonInstance instance) {
        for (LivingEntity mob : instance.getMobs()) {
            if (mob != null && !mob.isDead()) mob.remove();
        }
        if (instance.getBoss() != null && !instance.getBoss().isDead()) {
            instance.getBoss().remove();
        }
        if (instance.getConfig().hasRooms()) {
            for (DungeonRoom room : instance.getConfig().getRooms()) {
                closeDoor(room);
            }
        }
        activeInstances.remove(instance);
    }

    public void cleanupAll() {
        for (DungeonInstance instance : new ArrayList<>(activeInstances)) {
            cleanupInstance(instance);
        }
    }

    public List<DungeonInstance> getActiveInstances() {
        return activeInstances;
    }
}
