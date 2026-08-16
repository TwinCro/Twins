package com.dungeonrealms.api;

import com.dungeonrealms.DungeonRealms;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SupabaseClient {

    private final String supabaseUrl;
    private final String anonKey;
    private final HttpClient httpClient;
    private final boolean enabled;

    public SupabaseClient(DungeonRealms plugin) {
        String configUrl = plugin.getConfig().getString("supabase.url", "");
        String configKey = plugin.getConfig().getString("supabase.anon-key", "");

        if (configUrl.isEmpty()) configUrl = System.getenv().getOrDefault("SUPABASE_URL", "");
        if (configKey.isEmpty()) configKey = System.getenv().getOrDefault("SUPABASE_ANON_KEY", "");

        this.supabaseUrl = configUrl;
        this.anonKey = configKey;

        if (supabaseUrl.isEmpty() || anonKey.isEmpty()) {
            plugin.getLogger().warning("Supabase is not configured! Set 'supabase.url' and 'supabase.anon-key' in config.yml.");
            plugin.getLogger().warning("The plugin will run but data will NOT be saved.");
            this.enabled = false;
            this.httpClient = null;
        } else {
            this.enabled = true;
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            plugin.getLogger().info("Supabase connected: " + supabaseUrl);
        }
    }

    public boolean isEnabled() { return enabled; }

    public CompletableFuture<JsonObject> getPlayer(UUID minecraftUuid) {
        String uuidStr = minecraftUuid.toString();
        String url = supabaseUrl + "/rest/v1/dr_players?minecraft_uuid=eq." + uuidStr + "&limit=1";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return null;
            }
            var array = JsonParser.parseString(response.body()).getAsJsonArray();
            if (array.isEmpty()) {
                return null;
            }
            return array.get(0).getAsJsonObject();
        });
    }

    public CompletableFuture<Void> upsertPlayer(JsonObject playerData) {
        String url = supabaseUrl + "/rest/v1/dr_players?on_conflict=minecraft_uuid";
        return sendRequest(url, "POST", playerData.toString()).thenRun(() -> {});
    }

    public CompletableFuture<JsonArray> getPlayerSkills(UUID minecraftUuid) {
        String url = supabaseUrl + "/rest/v1/dr_player_skills?player_uuid=eq." + minecraftUuid.toString();
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return new JsonArray();
            }
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<Void> upsertPlayerSkill(UUID playerUuid, String skillId, boolean equipped, Integer skillSlot) {
        JsonObject data = new JsonObject();
        data.addProperty("player_uuid", playerUuid.toString());
        data.addProperty("skill_id", skillId);
        data.addProperty("unlocked", true);
        data.addProperty("equipped", equipped);
        if (skillSlot != null) {
            data.addProperty("skill_slot", skillSlot);
        } else {
            data.add("skill_slot", null);
        }
        String url = supabaseUrl + "/rest/v1/dr_player_skills?on_conflict=player_uuid,skill_id";
        return sendRequest(url, "POST", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> deletePlayerSkills(UUID playerUuid) {
        String url = supabaseUrl + "/rest/v1/dr_player_skills?player_uuid=eq." + playerUuid.toString();
        return sendRequest(url, "DELETE", null).thenRun(() -> {});
    }

    public CompletableFuture<Void> recordDungeonRun(JsonObject runData) {
        String url = supabaseUrl + "/rest/v1/dr_dungeon_runs";
        return sendRequest(url, "POST", runData.toString()).thenRun(() -> {});
    }

    public CompletableFuture<JsonArray> getSkillRegions() {
        String url = supabaseUrl + "/rest/v1/dr_skill_regions";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) {
                return new JsonArray();
            }
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<Void> insertSkillRegion(String regionName, String worldName,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        JsonObject data = new JsonObject();
        data.addProperty("region_name", regionName);
        data.addProperty("world_name", worldName);
        data.addProperty("min_x", minX);
        data.addProperty("min_y", minY);
        data.addProperty("min_z", minZ);
        data.addProperty("max_x", maxX);
        data.addProperty("max_y", maxY);
        data.addProperty("max_z", maxZ);
        String url = supabaseUrl + "/rest/v1/dr_skill_regions";
        return sendRequest(url, "POST", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> deleteSkillRegion(String regionName) {
        String url = supabaseUrl + "/rest/v1/dr_skill_regions?region_name=eq." + regionName;
        return sendRequest(url, "DELETE", null).thenRun(() -> {});
    }

    // ==================== GUILD METHODS ====================

    public CompletableFuture<JsonObject> getGuildByName(String guildName) {
        String url = supabaseUrl + "/rest/v1/dr_guilds?guild_name=eq." + java.net.URLEncoder.encode(guildName, java.nio.charset.StandardCharsets.UTF_8) + "&limit=1";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return null;
            var array = JsonParser.parseString(response.body()).getAsJsonArray();
            return array.isEmpty() ? null : array.get(0).getAsJsonObject();
        });
    }

    public CompletableFuture<JsonObject> getGuildByLeader(UUID leaderUuid) {
        String url = supabaseUrl + "/rest/v1/dr_guilds?leader_uuid=eq." + leaderUuid.toString() + "&limit=1";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return null;
            var array = JsonParser.parseString(response.body()).getAsJsonArray();
            return array.isEmpty() ? null : array.get(0).getAsJsonObject();
        });
    }

    public CompletableFuture<JsonObject> getGuildByMember(UUID playerUuid) {
        String url = supabaseUrl + "/rest/v1/dr_guild_members?player_uuid=eq." + playerUuid.toString() + "&limit=1";
        return sendRequest(url, "GET", null).thenCompose(response -> {
            if (response == null || response.body() == null || response.body().isBlank())
                return CompletableFuture.completedFuture(null);
            var array = JsonParser.parseString(response.body()).getAsJsonArray();
            if (array.isEmpty())
                return CompletableFuture.completedFuture(null);
            String guildId = array.get(0).getAsJsonObject().get("guild_id").getAsString();
            String guildUrl = supabaseUrl + "/rest/v1/dr_guilds?id=eq." + guildId + "&limit=1";
            return sendRequest(guildUrl, "GET", null).thenApply(r2 -> {
                if (r2 == null || r2.body() == null || r2.body().isBlank()) return null;
                var arr2 = JsonParser.parseString(r2.body()).getAsJsonArray();
                return arr2.isEmpty() ? null : arr2.get(0).getAsJsonObject();
            });
        });
    }

    public CompletableFuture<Void> createGuild(String guildName, UUID leaderUuid, String leaderName) {
        JsonObject data = new JsonObject();
        data.addProperty("guild_name", guildName);
        data.addProperty("leader_uuid", leaderUuid.toString());
        data.addProperty("leader_name", leaderName);
        String url = supabaseUrl + "/rest/v1/dr_guilds";
        return sendRequest(url, "POST", data.toString()).thenRun(() -> {
            getGuildByName(guildName).thenAccept(guild -> {
                if (guild != null) {
                    String guildId = guild.get("id").getAsString();
                    addGuildMember(guildId, leaderUuid.toString(), leaderName, "LEADER");
                }
            });
        });
    }

    public CompletableFuture<Void> addGuildMember(String guildId, String playerUuid, String playerName, String rank) {
        JsonObject data = new JsonObject();
        data.addProperty("guild_id", guildId);
        data.addProperty("player_uuid", playerUuid);
        data.addProperty("player_name", playerName);
        data.addProperty("rank", rank);
        String url = supabaseUrl + "/rest/v1/dr_guild_members";
        return sendRequest(url, "POST", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> removeGuildMember(String guildId, String playerUuid) {
        String url = supabaseUrl + "/rest/v1/dr_guild_members?guild_id=eq." + guildId + "&player_uuid=eq." + playerUuid;
        return sendRequest(url, "DELETE", null).thenRun(() -> {});
    }

    public CompletableFuture<JsonArray> getGuildMembers(String guildId) {
        String url = supabaseUrl + "/rest/v1/dr_guild_members?guild_id=eq." + guildId;
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return new JsonArray();
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<Void> updateMemberRank(String guildId, String playerUuid, String newRank) {
        JsonObject data = new JsonObject();
        data.addProperty("rank", newRank);
        String url = supabaseUrl + "/rest/v1/dr_guild_members?guild_id=eq." + guildId + "&player_uuid=eq." + playerUuid;
        return sendRequest(url, "PATCH", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> updateGuildMaxChunks(String guildId, int maxChunks) {
        JsonObject data = new JsonObject();
        data.addProperty("max_claim_chunks", maxChunks);
        String url = supabaseUrl + "/rest/v1/dr_guilds?id=eq." + guildId;
        return sendRequest(url, "PATCH", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> deleteGuild(String guildId) {
        String url = supabaseUrl + "/rest/v1/dr_guilds?id=eq." + guildId;
        return sendRequest(url, "DELETE", null).thenRun(() -> {});
    }

    public CompletableFuture<JsonArray> getGuildClaims(String guildId) {
        String url = supabaseUrl + "/rest/v1/dr_guild_claims?guild_id=eq." + guildId;
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return new JsonArray();
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<JsonArray> getAllClaims() {
        String url = supabaseUrl + "/rest/v1/dr_guild_claims?select=world_name,chunk_x,chunk_z,guild_id";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return new JsonArray();
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<Void> addGuildClaim(String guildId, String worldName, int chunkX, int chunkZ) {
        JsonObject data = new JsonObject();
        data.addProperty("guild_id", guildId);
        data.addProperty("world_name", worldName);
        data.addProperty("chunk_x", chunkX);
        data.addProperty("chunk_z", chunkZ);
        String url = supabaseUrl + "/rest/v1/dr_guild_claims";
        return sendRequest(url, "POST", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> removeGuildClaim(String guildId, String worldName, int chunkX, int chunkZ) {
        String url = supabaseUrl + "/rest/v1/dr_guild_claims?guild_id=eq." + guildId
                + "&world_name=eq." + worldName
                + "&chunk_x=eq." + chunkX
                + "&chunk_z=eq." + chunkZ;
        return sendRequest(url, "DELETE", null).thenRun(() -> {});
    }

    public CompletableFuture<Void> updatePlayerGold(UUID playerUuid, long gold) {
        JsonObject data = new JsonObject();
        data.addProperty("gold", gold);
        String url = supabaseUrl + "/rest/v1/dr_players?minecraft_uuid=eq." + playerUuid.toString();
        return sendRequest(url, "PATCH", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> updateGuildHome(String guildId, String world, double x, double y, double z, float yaw, float pitch) {
        JsonObject data = new JsonObject();
        data.addProperty("home_world", world);
        data.addProperty("home_x", x);
        data.addProperty("home_y", y);
        data.addProperty("home_z", z);
        data.addProperty("home_yaw", yaw);
        data.addProperty("home_pitch", pitch);
        String url = supabaseUrl + "/rest/v1/dr_guilds?id=eq." + guildId;
        return sendRequest(url, "PATCH", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> clearGuildHome(String guildId) {
        JsonObject data = new JsonObject();
        data.add("home_world", null);
        data.add("home_x", null);
        data.add("home_y", null);
        data.add("home_z", null);
        data.add("home_yaw", null);
        data.add("home_pitch", null);
        String url = supabaseUrl + "/rest/v1/dr_guilds?id=eq." + guildId;
        return sendRequest(url, "PATCH", data.toString()).thenRun(() -> {});
    }

    // ==================== CUSTOM DUNGEONS ====================

    public CompletableFuture<Void> upsertCustomDungeon(JsonObject dungeonData) {
        String url = supabaseUrl + "/rest/v1/dr_custom_dungeons?on_conflict=dungeon_name";
        return sendRequest(url, "POST", dungeonData.toString()).thenRun(() -> {});
    }

    public CompletableFuture<JsonArray> getAllCustomDungeons() {
        String url = supabaseUrl + "/rest/v1/dr_custom_dungeons";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return new JsonArray();
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<JsonObject> getCustomDungeonByName(String dungeonName) {
        String url = supabaseUrl + "/rest/v1/dr_custom_dungeons?dungeon_name=eq."
                + java.net.URLEncoder.encode(dungeonName, java.nio.charset.StandardCharsets.UTF_8) + "&limit=1";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return null;
            var array = JsonParser.parseString(response.body()).getAsJsonArray();
            return array.isEmpty() ? null : array.get(0).getAsJsonObject();
        });
    }

    public CompletableFuture<JsonArray> getDungeonRooms(String dungeonId) {
        String url = supabaseUrl + "/rest/v1/dr_custom_dungeon_rooms?dungeon_id=eq." + dungeonId + "&order=room_index.asc";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return new JsonArray();
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<JsonObject> getDungeonRoomByIndex(String dungeonId, int roomIndex) {
        String url = supabaseUrl + "/rest/v1/dr_custom_dungeon_rooms?dungeon_id=eq." + dungeonId
                + "&room_index=eq." + roomIndex + "&limit=1";
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return null;
            var array = JsonParser.parseString(response.body()).getAsJsonArray();
            return array.isEmpty() ? null : array.get(0).getAsJsonObject();
        });
    }

    public CompletableFuture<Void> insertDungeonRoom(JsonObject roomData) {
        String url = supabaseUrl + "/rest/v1/dr_custom_dungeon_rooms";
        return sendRequest(url, "POST", roomData.toString()).thenRun(() -> {});
    }

    public CompletableFuture<JsonArray> getRoomMobs(String roomId) {
        String url = supabaseUrl + "/rest/v1/dr_custom_room_mobs?room_id=eq." + roomId;
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return new JsonArray();
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<Void> insertRoomMob(JsonObject mobData) {
        String url = supabaseUrl + "/rest/v1/dr_custom_room_mobs";
        return sendRequest(url, "POST", mobData.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> deleteCustomDungeon(String dungeonName) {
        String url = supabaseUrl + "/rest/v1/dr_custom_dungeons?dungeon_name=eq."
                + java.net.URLEncoder.encode(dungeonName, java.nio.charset.StandardCharsets.UTF_8);
        return sendRequest(url, "DELETE", null).thenRun(() -> {});
    }

    // ==================== PLAYER HOMES (MULTI-HOME) ====================

    public CompletableFuture<JsonArray> getPlayerHomes(UUID playerUuid) {
        String url = supabaseUrl + "/rest/v1/dr_player_homes?player_uuid=eq." + playerUuid.toString();
        return sendRequest(url, "GET", null).thenApply(response -> {
            if (response == null || response.body() == null || response.body().isBlank()) return new JsonArray();
            return JsonParser.parseString(response.body()).getAsJsonArray();
        });
    }

    public CompletableFuture<Void> upsertPlayerHome(UUID playerUuid, String homeName,
            String worldName, double x, double y, double z, float yaw, float pitch) {
        JsonObject data = new JsonObject();
        data.addProperty("player_uuid", playerUuid.toString());
        data.addProperty("home_name", homeName);
        data.addProperty("world_name", worldName);
        data.addProperty("x", x);
        data.addProperty("y", y);
        data.addProperty("z", z);
        data.addProperty("yaw", yaw);
        data.addProperty("pitch", pitch);
        String url = supabaseUrl + "/rest/v1/dr_player_homes?on_conflict=player_uuid,home_name";
        return sendRequest(url, "POST", data.toString()).thenRun(() -> {});
    }

    public CompletableFuture<Void> deletePlayerHome(UUID playerUuid, String homeName) {
        String url = supabaseUrl + "/rest/v1/dr_player_homes?player_uuid=eq." + playerUuid.toString()
                + "&home_name=eq." + java.net.URLEncoder.encode(homeName, java.nio.charset.StandardCharsets.UTF_8);
        return sendRequest(url, "DELETE", null).thenRun(() -> {});
    }

    private CompletableFuture<HttpResponse<String>> sendRequest(String url, String method, String body) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("apikey", anonKey)
                .header("Authorization", "Bearer " + anonKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation,resolution=merge-duplicates");

        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            case "DELETE" -> builder.DELETE();
            case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            default -> builder.GET();
        }

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int code = response.statusCode();
                    if (code >= 400) {
                        pluginLog("Supabase request failed: HTTP " + code + " - " + response.body());
                        pluginLog("  URL: " + url);
                        if (body != null) pluginLog("  Body: " + body);
                        return null;
                    }
                    return response;
                })
                .exceptionally(ex -> {
                    pluginLog("Supabase request exception: " + ex.getMessage());
                    return null;
                });
    }

    private void pluginLog(String msg) {
        if (supabaseUrl != null && !supabaseUrl.isEmpty()) {
            System.out.println("[DungeonRealms] " + msg);
        }
    }
}
