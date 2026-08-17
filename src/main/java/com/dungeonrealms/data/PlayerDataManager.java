package com.dungeonrealms.data;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.api.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final DungeonRealms plugin;
    private final SupabaseClient supabase;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(DungeonRealms plugin) {
        this.plugin = plugin;
        this.supabase = plugin.getSupabaseClient();
    }

    public void loadPlayer(Player player) {
        PlayerData data = new PlayerData(player.getUniqueId(), player.getName());
        cache.put(player.getUniqueId(), data);

        supabase.getPlayer(player.getUniqueId()).thenAccept(json -> {
            if (json != null) {
                PlayerData dbData = PlayerData.fromJson(json);
                data.setClassId(dbData.getClassId());
                data.setLevel(dbData.getLevel());
                data.setXp(dbData.getXp());
                data.setMana(dbData.getMana());
                data.setMaxMana(dbData.getMaxMana());
                data.setAwakened(dbData.isAwakened());
                data.setAwakeningCount(dbData.getAwakeningCount());
                data.setGold(dbData.getGold());
            } else {
                supabase.upsertPlayer(data.toJson());
            }

            supabase.getPlayerSkills(player.getUniqueId()).thenAccept(skillsArray -> {
                if (skillsArray == null) return;
                for (int i = 0; i < skillsArray.size(); i++) {
                    JsonObject skillObj = skillsArray.get(i).getAsJsonObject();
                    String skillId = skillObj.get("skill_id").getAsString();
                    boolean equipped = skillObj.has("equipped") && skillObj.get("equipped").getAsBoolean();
                    data.getUnlockedSkills().add(skillId);
                    if (equipped) {
                        data.getEquippedSkills().add(skillId);
                    }
                    if (skillObj.has("skill_slot") && !skillObj.get("skill_slot").isJsonNull()) {
                        int slot = skillObj.get("skill_slot").getAsInt();
                        if (slot >= 1 && slot <= 6) {
                            data.getSkillSlots().put(slot, skillId);
                        }
                    }
                }
            });

            supabase.getPlayerHomes(player.getUniqueId()).thenAccept(homesArray -> {
                if (homesArray == null) return;
                for (int i = 0; i < homesArray.size(); i++) {
                    JsonObject homeObj = homesArray.get(i).getAsJsonObject();
                    String homeName = homeObj.get("home_name").getAsString();
                    String worldName = homeObj.get("world_name").getAsString();
                    double x = homeObj.get("x").getAsDouble();
                    double y = homeObj.get("y").getAsDouble();
                    double z = homeObj.get("z").getAsDouble();
                    float yaw = homeObj.has("yaw") ? homeObj.get("yaw").getAsFloat() : 0;
                    float pitch = homeObj.has("pitch") ? homeObj.get("pitch").getAsFloat() : 0;
                    data.setHome(homeName, worldName, x, y, z, yaw, pitch);
                }
            });

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (data.hasClass()) {
                    plugin.getClassManager().applyClassStats(player, data);
                    data.setSkillBarMode(true);
                }
            });
        });
    }

    public void savePlayer(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        supabase.upsertPlayer(data.toJson());

        supabase.deletePlayerSkills(uuid).thenCompose(v -> {
            for (String skillId : data.getUnlockedSkills()) {
                boolean equipped = data.getEquippedSkills().contains(skillId);
                Integer slot = null;
                for (Map.Entry<Integer, String> entry : data.getSkillSlots().entrySet()) {
                    if (entry.getValue().equals(skillId)) {
                        slot = entry.getKey();
                        break;
                    }
                }
                supabase.upsertPlayerSkill(uuid, skillId, equipped, slot);
            }
            return CompletableFuture.completedFuture(null);
        });

        for (Map.Entry<String, HomeLocation> entry : data.getHomes().entrySet()) {
            HomeLocation loc = entry.getValue();
            supabase.upsertPlayerHome(uuid, entry.getKey(), loc.world, loc.x, loc.y, loc.z, loc.yaw, loc.pitch);
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            savePlayer(uuid);
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid);
        cache.remove(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    public PlayerData getPlayerData(Player player) {
        return cache.get(player.getUniqueId());
    }

    public PlayerData getOrCreatePlayerData(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(uuid, player.getName()));
    }

    public boolean hasPlayerData(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public static class PlayerData {
        private final UUID uuid;
        private volatile String username;
        private volatile String classId = "";
        private volatile int level = 1;
        private volatile long xp = 0;
        private volatile int mana = 0;
        private volatile int maxMana = 0;
        private volatile boolean awakened = false;
        private volatile int awakeningCount = 0;
        private volatile long gold = 0;
        private final Map<String, HomeLocation> homes = new ConcurrentHashMap<>();
        private final Set<String> unlockedSkills = ConcurrentHashMap.newKeySet();
        private final Set<String> equippedSkills = ConcurrentHashMap.newKeySet();
        private final Map<Integer, String> skillSlots = new ConcurrentHashMap<>();
        private volatile boolean skillBarMode = false;

        public PlayerData(UUID uuid, String username) {
            this.uuid = uuid;
            this.username = username;
        }

        public static PlayerData fromJson(JsonObject json) {
            PlayerData data = new PlayerData(
                    UUID.fromString(json.get("minecraft_uuid").getAsString()),
                    json.has("username") ? json.get("username").getAsString() : ""
            );
            data.classId = json.has("class_id") ? json.get("class_id").getAsString() : "";
            data.level = json.has("level") ? json.get("level").getAsInt() : 1;
            data.xp = json.has("xp") ? json.get("xp").getAsLong() : 0;
            data.mana = json.has("mana") ? json.get("mana").getAsInt() : 0;
            data.maxMana = json.has("max_mana") ? json.get("max_mana").getAsInt() : 0;
            data.awakened = json.has("awakened") && json.get("awakened").getAsBoolean();
            data.awakeningCount = json.has("awakening_count") ? json.get("awakening_count").getAsInt() : 0;
            data.gold = json.has("gold") ? json.get("gold").getAsLong() : 0;
            return data;
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("minecraft_uuid", uuid.toString());
            json.addProperty("username", username);
            json.addProperty("class_id", classId);
            json.addProperty("level", level);
            json.addProperty("xp", xp);
            json.addProperty("mana", mana);
            json.addProperty("max_mana", maxMana);
            json.addProperty("awakened", awakened);
            json.addProperty("awakening_count", awakeningCount);
            json.addProperty("gold", gold);
            return json;
        }

        public UUID getUuid() { return uuid; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        public boolean hasClass() { return !classId.isEmpty(); }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public long getXp() { return xp; }
        public void setXp(long xp) { this.xp = xp; }
        public void addXp(long amount) { this.xp += amount; }
        public int getMana() { return mana; }
        public void setMana(int mana) { this.mana = mana; }
        public void addMana(int amount) { this.mana = Math.min(mana + amount, maxMana); }
        public boolean spendMana(int amount) {
            if (mana >= amount) { mana -= amount; return true; }
            return false;
        }
        public int getMaxMana() { return maxMana; }
        public void setMaxMana(int maxMana) { this.maxMana = maxMana; }
        public boolean isAwakened() { return awakened; }
        public void setAwakened(boolean awakened) { this.awakened = awakened; }
        public int getAwakeningCount() { return awakeningCount; }
        public void setAwakeningCount(int count) { this.awakeningCount = count; }
        public long getGold() { return gold; }
        public void setGold(long gold) { this.gold = gold; }
        public void addGold(long amount) { this.gold += amount; }
        public Map<String, HomeLocation> getHomes() { return homes; }
        public HomeLocation getHome(String name) { return homes.get(name.toLowerCase()); }
        public boolean hasHome(String name) { return homes.containsKey(name.toLowerCase()); }
        public int getHomeCount() { return homes.size(); }
        public void setHome(String name, String world, double x, double y, double z, float yaw, float pitch) {
            homes.put(name.toLowerCase(), new HomeLocation(world, x, y, z, yaw, pitch));
        }
        public void removeHome(String name) { homes.remove(name.toLowerCase()); }
        public boolean spendGold(long amount) {
            if (gold >= amount) { gold -= amount; return true; }
            return false;
        }
        public Set<String> getUnlockedSkills() { return unlockedSkills; }
        public Set<String> getEquippedSkills() { return equippedSkills; }
        public Map<Integer, String> getSkillSlots() { return skillSlots; }
        public boolean isSkillBarMode() { return skillBarMode; }
        public void setSkillBarMode(boolean skillBarMode) { this.skillBarMode = skillBarMode; }
    }

    public static class HomeLocation {
        public final String world;
        public final double x, y, z;
        public final float yaw, pitch;

        public HomeLocation(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
        }
    }
}
