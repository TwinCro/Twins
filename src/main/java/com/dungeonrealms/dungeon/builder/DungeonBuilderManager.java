package com.dungeonrealms.dungeon.builder;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.dungeon.DungeonConfig;
import com.dungeonrealms.dungeon.DungeonRank;
import com.dungeonrealms.dungeon.DungeonRoom;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DungeonBuilderManager {

    private final DungeonRealms plugin;
    private final Map<UUID, BuilderSession> activeSessions = new HashMap<>();

    public DungeonBuilderManager(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    public BuilderSession createSession(UUID adminUuid, String name, DungeonRank rank) {
        BuilderSession session = new BuilderSession(adminUuid, name, rank);
        activeSessions.put(adminUuid, session);
        return session;
    }

    public BuilderSession getSession(UUID adminUuid) {
        return activeSessions.get(adminUuid);
    }

    public void removeSession(UUID adminUuid) {
        activeSessions.remove(adminUuid);
    }

    public void saveDungeon(BuilderSession session) {
        JsonObject data = new JsonObject();
        data.addProperty("dungeon_name", session.getDungeonName());
        data.addProperty("display_name", session.getDungeonName());
        data.addProperty("rank", session.getRank().name());
        data.addProperty("min_level", 1);
        data.addProperty("max_players", 5);

        Location lobby = session.getLobbySpawn();
        if (lobby != null) {
            data.addProperty("lobby_world", lobby.getWorld().getName());
            data.addProperty("lobby_x", lobby.getX());
            data.addProperty("lobby_y", lobby.getY());
            data.addProperty("lobby_z", lobby.getZ());
        }

        DungeonConfig.BossConfig boss = session.getBossConfig();
        if (boss != null) {
            data.addProperty("boss_type", boss.getType().name());
            data.addProperty("boss_name", boss.getName());
            data.addProperty("boss_hp", boss.getHealth());
            data.addProperty("boss_damage", boss.getDamage());
            data.addProperty("boss_defense", boss.getDefense());
            data.addProperty("boss_magic_defense", boss.getMagicDefense());
        }

        data.addProperty("chest_count", session.getChestCount());
        data.addProperty("created_by", session.getAdminUuid().toString());

        plugin.getSupabaseClient().upsertCustomDungeon(data).thenRun(() -> {
            plugin.getSupabaseClient().getCustomDungeonByName(session.getDungeonName()).thenAccept(dungeonJson -> {
                if (dungeonJson == null) {
                    registerLocalDungeon(session, UUID.randomUUID().toString());
                    return;
                }
                String dungeonId = dungeonJson.get("id").getAsString();
                saveRooms(session, dungeonId);
                registerLocalDungeon(session, dungeonId);
            });
        });
    }

    private void saveRooms(BuilderSession session, String dungeonId) {
        for (DungeonRoom room : session.getRooms()) {
            JsonObject roomData = new JsonObject();
            roomData.addProperty("dungeon_id", dungeonId);
            roomData.addProperty("room_index", room.getIndex());
            roomData.addProperty("room_type", room.getType().name());
            roomData.addProperty("world_name", room.getWorldName());
            roomData.addProperty("min_x", room.getMinX());
            roomData.addProperty("min_y", room.getMinY());
            roomData.addProperty("min_z", room.getMinZ());
            roomData.addProperty("max_x", room.getMaxX());
            roomData.addProperty("max_y", room.getMaxY());
            roomData.addProperty("max_z", room.getMaxZ());

            if (room.getDoorX() != null) {
                roomData.addProperty("door_x", room.getDoorX());
                roomData.addProperty("door_y", room.getDoorY());
                roomData.addProperty("door_z", room.getDoorZ());
                roomData.addProperty("door_material", room.getDoorMaterial() != null ? room.getDoorMaterial() : "IRON_BLOCK");
            }

            if (room.getSpawnMarkerX() != null) {
                roomData.addProperty("spawn_marker_x", room.getSpawnMarkerX());
                roomData.addProperty("spawn_marker_y", room.getSpawnMarkerY());
                roomData.addProperty("spawn_marker_z", room.getSpawnMarkerZ());
            }

            plugin.getSupabaseClient().insertDungeonRoom(roomData).thenRun(() -> {
                plugin.getSupabaseClient().getDungeonRoomByIndex(dungeonId, room.getIndex()).thenAccept(roomJson -> {
                    if (roomJson == null) return;
                    String roomId = roomJson.get("id").getAsString();
                    for (DungeonRoom.RoomMobEntry mob : room.getMobs()) {
                        JsonObject mobData = new JsonObject();
                        mobData.addProperty("room_id", roomId);
                        mobData.addProperty("entity_type", mob.getType().name());
                        mobData.addProperty("mob_count", mob.getCount());
                        mobData.addProperty("health", mob.getHealth());
                        mobData.addProperty("damage", mob.getDamage());
                        mobData.addProperty("defense", mob.getDefense());
                        mobData.addProperty("magic_defense", mob.getMagicDefense());
                        plugin.getSupabaseClient().insertRoomMob(mobData);
                    }
                });
            });
        }
    }

    private void registerLocalDungeon(BuilderSession session, String dungeonId) {
        DungeonConfig config = sessionToConfig(session, dungeonId);
        plugin.getDungeonManager().registerCustomDungeon(config);
    }

    public DungeonConfig sessionToConfig(BuilderSession session, String configId) {
        DungeonConfig config = new DungeonConfig(configId);
        config.setDisplayName(session.getDungeonName());
        config.setRank(session.getRank());
        config.setMinLevel(1);
        config.setMaxPlayers(5);
        config.setCustom(true);
        config.setChestCount(session.getChestCount());

        Location lobby = session.getLobbySpawn();
        if (lobby != null) {
            config.setSpawnWorld(lobby.getWorld().getName());
            config.setSpawnX(lobby.getX());
            config.setSpawnY(lobby.getY());
            config.setSpawnZ(lobby.getZ());
            config.setLobbySpawn(lobby);
        }

        config.setBoss(session.getBossConfig());
        config.setRooms(new ArrayList<>(session.getRooms()));
        return config;
    }

    public void loadCustomDungeons() {
        plugin.getSupabaseClient().getAllCustomDungeons().thenAccept(dungeonsArray -> {
            if (dungeonsArray == null) return;
            for (int i = 0; i < dungeonsArray.size(); i++) {
                JsonObject dj = dungeonsArray.get(i).getAsJsonObject();
                String id = dj.get("id").getAsString();
                String name = dj.get("dungeon_name").getAsString();

                DungeonConfig config = new DungeonConfig(id);
                config.setDisplayName(dj.has("display_name") ? dj.get("display_name").getAsString() : name);
                config.setRank(DungeonRank.fromString(dj.get("rank").getAsString()));
                config.setMinLevel(dj.get("min_level").getAsInt());
                config.setMaxPlayers(dj.get("max_players").getAsInt());
                config.setSpawnWorld(dj.get("lobby_world").getAsString());
                config.setSpawnX(dj.get("lobby_x").getAsDouble());
                config.setSpawnY(dj.get("lobby_y").getAsDouble());
                config.setSpawnZ(dj.get("lobby_z").getAsDouble());
                config.setCustom(true);
                config.setChestCount(dj.has("chest_count") ? dj.get("chest_count").getAsInt() : 3);

                config.setLobbySpawn(new Location(
                        Bukkit.getWorld(config.getSpawnWorld()),
                        config.getSpawnX(), config.getSpawnY(), config.getSpawnZ()));

                DungeonConfig.BossConfig boss = new DungeonConfig.BossConfig();
                boss.setType(EntityType.valueOf(dj.get("boss_type").getAsString()));
                boss.setName(dj.get("boss_name").getAsString());
                boss.setHealth(dj.get("boss_hp").getAsDouble());
                boss.setDamage(dj.get("boss_damage").getAsDouble());
                boss.setDefense(dj.get("boss_defense").getAsDouble());
                boss.setMagicDefense(dj.get("boss_magic_defense").getAsDouble());
                config.setBoss(boss);

                loadRoomsForDungeon(config, id);
                plugin.getDungeonManager().registerCustomDungeon(config);
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getLogger().info("Loaded " + dungeonsArray.size() + " custom dungeons from database."));
        });
    }

    private void loadRoomsForDungeon(DungeonConfig config, String dungeonId) {
        plugin.getSupabaseClient().getDungeonRooms(dungeonId).thenAccept(roomsArray -> {
            if (roomsArray == null) return;
            List<DungeonRoom> rooms = new ArrayList<>();
            List<CompletableFuture<Void>> mobFutures = new ArrayList<>();

            for (int i = 0; i < roomsArray.size(); i++) {
                JsonObject rj = roomsArray.get(i).getAsJsonObject();
                DungeonRoom room = new DungeonRoom();
                room.setIndex(rj.get("room_index").getAsInt());
                room.setType(DungeonRoom.RoomType.valueOf(rj.get("room_type").getAsString()));
                room.setWorldName(rj.get("world_name").getAsString());
                room.setMinX(rj.get("min_x").getAsDouble());
                room.setMinY(rj.get("min_y").getAsDouble());
                room.setMinZ(rj.get("min_z").getAsDouble());
                room.setMaxX(rj.get("max_x").getAsDouble());
                room.setMaxY(rj.get("max_y").getAsDouble());
                room.setMaxZ(rj.get("max_z").getAsDouble());

                if (rj.has("door_x") && !rj.get("door_x").isJsonNull()) {
                    room.setDoorX(rj.get("door_x").getAsDouble());
                    room.setDoorY(rj.get("door_y").getAsDouble());
                    room.setDoorZ(rj.get("door_z").getAsDouble());
                    room.setDoorMaterial(rj.has("door_material") ? rj.get("door_material").getAsString() : "IRON_BLOCK");
                }

                if (rj.has("spawn_marker_x") && !rj.get("spawn_marker_x").isJsonNull()) {
                    room.setSpawnMarkerX(rj.get("spawn_marker_x").getAsDouble());
                    room.setSpawnMarkerY(rj.get("spawn_marker_y").getAsDouble());
                    room.setSpawnMarkerZ(rj.get("spawn_marker_z").getAsDouble());
                }

                String roomId = rj.get("id").getAsString();
                CompletableFuture<Void> mobFuture = plugin.getSupabaseClient().getRoomMobs(roomId).thenAccept(mobsArray -> {
                    if (mobsArray == null) return;
                    for (int j = 0; j < mobsArray.size(); j++) {
                        JsonObject mj = mobsArray.get(j).getAsJsonObject();
                        DungeonRoom.RoomMobEntry entry = new DungeonRoom.RoomMobEntry(
                                EntityType.valueOf(mj.get("entity_type").getAsString()),
                                mj.get("mob_count").getAsInt(),
                                mj.get("health").getAsDouble(),
                                mj.get("damage").getAsDouble(),
                                mj.get("defense").getAsDouble(),
                                mj.get("magic_defense").getAsDouble()
                        );
                        room.getMobs().add(entry);
                    }
                });
                mobFutures.add(mobFuture);
                rooms.add(room);
            }

            rooms.sort(Comparator.comparingInt(DungeonRoom::getIndex));
            config.setRooms(rooms);
        });
    }

    public void deleteCustomDungeon(String dungeonName) {
        plugin.getSupabaseClient().deleteCustomDungeon(dungeonName);
        plugin.getDungeonManager().unregisterCustomDungeon(dungeonName);
    }
}
