package com.dungeonrealms.guild;

import com.dungeonrealms.DungeonRealms;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {

    private final DungeonRealms plugin;
    private final Set<String> claimedChunks = ConcurrentHashMap.newKeySet();
    private final java.util.Map<String, String> chunkToGuild = new ConcurrentHashMap<>();

    public ClaimManager(DungeonRealms plugin) {
        this.plugin = plugin;
        loadClaims();
    }

    public void loadClaims() {
        plugin.getSupabaseClient().getAllClaims().thenAccept(claims -> {
            if (claims == null) return;
            claimedChunks.clear();
            chunkToGuild.clear();
            for (JsonElement el : claims) {
                JsonObject c = el.getAsJsonObject();
                String worldName = c.get("world_name").getAsString();
                int chunkX = c.get("chunk_x").getAsInt();
                int chunkZ = c.get("chunk_z").getAsInt();
                String guildId = c.get("guild_id").getAsString();
                String key = chunkKey(worldName, chunkX, chunkZ);
                claimedChunks.add(key);
                chunkToGuild.put(key, guildId);
            }
            Bukkit.getConsoleSender().sendMessage("[DungeonRealms] Loaded " + claimedChunks.size() + " guild claims.");
        });
    }

    public boolean isClaimed(String worldName, int chunkX, int chunkZ) {
        return claimedChunks.contains(chunkKey(worldName, chunkX, chunkZ));
    }

    public String getClaimingGuildId(String worldName, int chunkX, int chunkZ) {
        return chunkToGuild.get(chunkKey(worldName, chunkX, chunkZ));
    }

    public boolean isPlayerInClaimingGuild(java.util.UUID playerUuid, String worldName, int chunkX, int chunkZ) {
        String guildId = getClaimingGuildId(worldName, chunkX, chunkZ);
        if (guildId == null) return false;
        Guild guild = plugin.getGuildManager().getPlayerGuild(playerUuid);
        return guild != null && guild.getId().equals(guildId);
    }

    private String chunkKey(String worldName, int chunkX, int chunkZ) {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }
}
