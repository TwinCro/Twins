package com.dungeonrealms.guild;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.api.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildManager {

    private final DungeonRealms plugin;
    private final Map<String, Guild> guildCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerToGuild = new ConcurrentHashMap<>();
    private final Map<UUID, Guild.Rank> playerRanks = new ConcurrentHashMap<>();
    private final Set<String> loadedGuilds = new HashSet<>();

    private final int expansionCost;
    private final int startingChunks;

    public GuildManager(DungeonRealms plugin) {
        this.plugin = plugin;
        this.expansionCost = plugin.getConfigManager().getMainConfig().getInt("guild.expansion-cost", 500);
        this.startingChunks = plugin.getConfigManager().getMainConfig().getInt("guild.starting-chunks", 10);
        loadAllGuilds();
    }

    public int getExpansionCost() { return expansionCost; }
    public int getStartingChunks() { return startingChunks; }

    private void loadAllGuilds() {
        plugin.getSupabaseClient().getGuildByName("__LOAD_ALL__").thenRun(() -> {});
        // SupabaseClient doesn't have a "get all guilds" method, so we use getAllClaims
        // and load guilds as players interact with them. Claims are loaded separately
        // by ClaimManager.
    }

    public void loadPlayerGuild(Player player) {
        plugin.getSupabaseClient().getGuildByMember(player.getUniqueId()).thenAccept(guildJson -> {
            if (guildJson == null) return;
            String guildId = guildJson.get("id").getAsString();
            String guildName = guildJson.get("guild_name").getAsString();
            String leaderUuid = guildJson.get("leader_uuid").getAsString();
            String leaderName = guildJson.has("leader_name") ? guildJson.get("leader_name").getAsString() : "";
            int maxChunks = guildJson.has("max_claim_chunks") ? guildJson.get("max_claim_chunks").getAsInt() : startingChunks;

            Guild guild = new Guild(guildId, guildName, leaderUuid, leaderName, maxChunks);
            if (guildJson.has("home_world") && !guildJson.get("home_world").isJsonNull()) {
                guild.setHome(
                        guildJson.get("home_world").getAsString(),
                        guildJson.get("home_x").getAsDouble(),
                        guildJson.get("home_y").getAsDouble(),
                        guildJson.get("home_z").getAsDouble(),
                        guildJson.get("home_yaw").getAsFloat(),
                        guildJson.get("home_pitch").getAsFloat()
                );
            }
            guildCache.put(guildId, guild);

            // Determine rank
            if (leaderUuid.equals(player.getUniqueId().toString())) {
                playerRanks.put(player.getUniqueId(), Guild.Rank.LEADER);
            } else {
                plugin.getSupabaseClient().getGuildMembers(guildId).thenAccept(members -> {
                    if (members == null) return;
                    for (JsonElement el : members) {
                        JsonObject m = el.getAsJsonObject();
                        if (m.get("player_uuid").getAsString().equals(player.getUniqueId().toString())) {
                            String rank = m.has("rank") ? m.get("rank").getAsString() : "MEMBER";
                            playerRanks.put(player.getUniqueId(), Guild.Rank.fromString(rank));
                        }
                    }
                });
            }

            playerToGuild.put(player.getUniqueId(), guildId);
        });
    }

    public void unloadPlayerGuild(UUID playerUuid) {
        playerToGuild.remove(playerUuid);
        playerRanks.remove(playerUuid);
    }

    public Guild getPlayerGuild(UUID playerUuid) {
        String guildId = playerToGuild.get(playerUuid);
        if (guildId == null) return null;
        return guildCache.get(guildId);
    }

    public Guild.Rank getPlayerRank(UUID playerUuid) {
        return playerRanks.getOrDefault(playerUuid, Guild.Rank.MEMBER);
    }

    public boolean isInGuild(UUID playerUuid) {
        return playerToGuild.containsKey(playerUuid);
    }

    public boolean isLeader(UUID playerUuid) {
        return getPlayerRank(playerUuid) == Guild.Rank.LEADER;
    }

    public boolean isOfficerOrLeader(UUID playerUuid) {
        Guild.Rank rank = getPlayerRank(playerUuid);
        return rank == Guild.Rank.OFFICER || rank == Guild.Rank.LEADER;
    }

    public void createGuild(Player player, String guildName) {
        if (isInGuild(player.getUniqueId())) {
            player.sendMessage("§cYou are already in a guild! Leave it first with §e/guild leave");
            return;
        }
        if (guildName.length() < 3 || guildName.length() > 16) {
            player.sendMessage("§cGuild name must be 3-16 characters.");
            return;
        }
        if (!guildName.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage("§cGuild name can only contain letters, numbers, and underscores.");
            return;
        }

        plugin.getSupabaseClient().getGuildByName(guildName).thenAccept(existing -> {
            if (existing != null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§cA guild with that name already exists!"));
                return;
            }

            plugin.getSupabaseClient().createGuild(guildName, player.getUniqueId(), player.getName()).thenRun(() -> {
                if (!plugin.getSupabaseClient().isEnabled()) {
                    String guildId = java.util.UUID.randomUUID().toString();
                    Guild guild = new Guild(guildId, guildName, player.getUniqueId().toString(), player.getName(), startingChunks);
                    guildCache.put(guildId, guild);
                    playerToGuild.put(player.getUniqueId(), guildId);
                    playerRanks.put(player.getUniqueId(), Guild.Rank.LEADER);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§a§lGuild created! §e" + guildName);
                        player.sendMessage("§7Your guild starts with §e" + startingChunks + " §7claim chunks.");
                        player.sendMessage("§7Stand in a chunk and use §e/guild claim §7to claim it.");
                    });
                    return;
                }
                plugin.getSupabaseClient().getGuildByName(guildName).thenAccept(guildJson -> {
                    if (guildJson == null) return;
                    String guildId = guildJson.get("id").getAsString();
                    int maxChunks = guildJson.has("max_claim_chunks") ? guildJson.get("max_claim_chunks").getAsInt() : startingChunks;
                    Guild guild = new Guild(guildId, guildName, player.getUniqueId().toString(), player.getName(), maxChunks);
                    guildCache.put(guildId, guild);
                    playerToGuild.put(player.getUniqueId(), guildId);
                    playerRanks.put(player.getUniqueId(), Guild.Rank.LEADER);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§a§lGuild created! §e" + guildName);
                        player.sendMessage("§7Your guild starts with §e" + startingChunks + " §7claim chunks.");
                        player.sendMessage("§7Stand in a chunk and use §e/guild claim §7to claim it.");
                    });
                });
            });
        });
    }

    public void invitePlayer(Player leader, String targetName) {
        if (!isLeader(leader.getUniqueId())) {
            leader.sendMessage("§cOnly the guild leader can invite players.");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            leader.sendMessage("§cPlayer not found or not online.");
            return;
        }
        if (isInGuild(target.getUniqueId())) {
            leader.sendMessage("§cThat player is already in a guild.");
            return;
        }

        Guild guild = getPlayerGuild(leader.getUniqueId());
        if (guild == null) return;

        pendingInvites.put(target.getUniqueId(), guild.getId());
        leader.sendMessage("§aInvited §e" + targetName + " §ato your guild.");
        target.sendMessage("§e" + leader.getName() + " §ainvited you to join guild §e" + guild.getGuildName() + "§a!");
        target.sendMessage("§7Use §e/guild accept §ato join.");
    }

    private final Map<UUID, String> pendingInvites = new ConcurrentHashMap<>();

    public void acceptInvite(Player player) {
        String guildId = pendingInvites.get(player.getUniqueId());
        if (guildId == null) {
            player.sendMessage("§cYou have no pending guild invites.");
            return;
        }
        pendingInvites.remove(player.getUniqueId());

        Guild guild = guildCache.get(guildId);
        if (guild == null) {
            player.sendMessage("§cThat guild no longer exists.");
            return;
        }

        plugin.getSupabaseClient().addGuildMember(guildId, player.getUniqueId().toString(), player.getName(), "MEMBER")
                .thenRun(() -> {
                    playerToGuild.put(player.getUniqueId(), guildId);
                    playerRanks.put(player.getUniqueId(), Guild.Rank.MEMBER);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§a§lYou joined guild §e" + guild.getGuildName() + "§a!");
                    });
                });
    }

    public void leaveGuild(Player player) {
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild.");
            return;
        }
        if (isLeader(player.getUniqueId())) {
            player.sendMessage("§cYou are the guild leader! Transfer leadership or disband the guild with §e/guild disband");
            return;
        }

        plugin.getSupabaseClient().removeGuildMember(guild.getId(), player.getUniqueId().toString()).thenRun(() -> {
            unloadPlayerGuild(player.getUniqueId());
            player.sendMessage("§aYou left guild §e" + guild.getGuildName() + "§a.");
        });
    }

    public void disbandGuild(Player leader) {
        if (!isLeader(leader.getUniqueId())) {
            leader.sendMessage("§cOnly the guild leader can disband the guild.");
            return;
        }
        Guild guild = getPlayerGuild(leader.getUniqueId());
        if (guild == null) return;

        String guildId = guild.getId();
        plugin.getSupabaseClient().deleteGuild(guildId).thenRun(() -> {
            // Remove all players mapped to this guild
            Set<UUID> toRemove = new HashSet<>();
            for (Map.Entry<UUID, String> entry : playerToGuild.entrySet()) {
                if (entry.getValue().equals(guildId)) {
                    toRemove.add(entry.getKey());
                }
            }
            for (UUID uuid : toRemove) {
                playerToGuild.remove(uuid);
                playerRanks.remove(uuid);
            }
            guildCache.remove(guildId);
            plugin.getClaimManager().loadClaims();
            Bukkit.getScheduler().runTask(plugin, () -> {
                leader.sendMessage("§aGuild §e" + guild.getGuildName() + " §ahas been disbanded.");
            });
        });
    }

    public void promotePlayer(Player leader, String targetName) {
        if (!isLeader(leader.getUniqueId())) {
            leader.sendMessage("§cOnly the guild leader can promote players.");
            return;
        }
        Guild guild = getPlayerGuild(leader.getUniqueId());
        if (guild == null) return;

        plugin.getSupabaseClient().getGuildMembers(guild.getId()).thenAccept(members -> {
            if (members == null) return;
            String foundUuid = null;
            for (JsonElement el : members) {
                JsonObject m = el.getAsJsonObject();
                if (m.has("player_name") && m.get("player_name").getAsString().equalsIgnoreCase(targetName)) {
                    foundUuid = m.get("player_uuid").getAsString();
                    break;
                }
            }
            if (foundUuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        leader.sendMessage("§cPlayer not found in your guild."));
                return;
            }
            final String targetUuid = foundUuid;
            plugin.getSupabaseClient().updateMemberRank(guild.getId(), targetUuid, "OFFICER").thenRun(() -> {
                UUID uuid = UUID.fromString(targetUuid);
                if (playerToGuild.containsKey(uuid)) {
                    playerRanks.put(uuid, Guild.Rank.OFFICER);
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    leader.sendMessage("§aPromoted §e" + targetName + " §ato Officer.");
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null && target.isOnline()) {
                        target.sendMessage("§aYou have been promoted to Officer in §e" + guild.getGuildName() + "§a!");
                        target.sendMessage("§7You can now expand and unclaim guild land.");
                    }
                });
            });
        });
    }

    public void demotePlayer(Player leader, String targetName) {
        if (!isLeader(leader.getUniqueId())) {
            leader.sendMessage("§cOnly the guild leader can demote players.");
            return;
        }
        Guild guild = getPlayerGuild(leader.getUniqueId());
        if (guild == null) return;

        plugin.getSupabaseClient().getGuildMembers(guild.getId()).thenAccept(members -> {
            if (members == null) return;
            String foundUuid = null;
            for (JsonElement el : members) {
                JsonObject m = el.getAsJsonObject();
                if (m.has("player_name") && m.get("player_name").getAsString().equalsIgnoreCase(targetName)) {
                    foundUuid = m.get("player_uuid").getAsString();
                    break;
                }
            }
            if (foundUuid == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        leader.sendMessage("§cPlayer not found in your guild."));
                return;
            }
            final String targetUuid = foundUuid;
            plugin.getSupabaseClient().updateMemberRank(guild.getId(), targetUuid, "MEMBER").thenRun(() -> {
                UUID uuid = UUID.fromString(targetUuid);
                if (playerToGuild.containsKey(uuid)) {
                    playerRanks.put(uuid, Guild.Rank.MEMBER);
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    leader.sendMessage("§aDemoted §e" + targetName + " §ato Member.");
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null && target.isOnline()) {
                        target.sendMessage("§7You have been demoted to Member in §e" + guild.getGuildName() + "§7.");
                    }
                });
            });
        });
    }

    public void claimChunk(Player player) {
        if (!isOfficerOrLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the guild leader or officers can claim land.");
            return;
        }
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild.");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        plugin.getSupabaseClient().getAllClaims().thenAccept(allClaims -> {
            if (allClaims == null) return;
            for (JsonElement el : allClaims) {
                JsonObject c = el.getAsJsonObject();
                if (c.get("world_name").getAsString().equals(worldName)
                        && c.get("chunk_x").getAsInt() == chunkX
                        && c.get("chunk_z").getAsInt() == chunkZ) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            player.sendMessage("§cThis chunk is already claimed by a guild."));
                    return;
                }
            }

            plugin.getSupabaseClient().getGuildClaims(guild.getId()).thenAccept(guildClaims -> {
                if (guildClaims == null) return;
                int currentClaims = guildClaims.size();
                if (currentClaims >= guild.getMaxClaimChunks()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§cYour guild has reached its claim limit of §e" + guild.getMaxClaimChunks() + " §cchunks.");
                        player.sendMessage("§7Use §e/guild expand §7to buy more claim slots (costs §e" + expansionCost + " gold§7).");
                    });
                    return;
                }

                plugin.getSupabaseClient().addGuildClaim(guild.getId(), worldName, chunkX, chunkZ).thenRun(() -> {
                    plugin.getClaimManager().loadClaims();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage("§a§lChunk claimed! §e(" + worldName + ", " + chunkX + ", " + chunkZ + ")");
                        player.sendMessage("§7Claims: §e" + (currentClaims + 1) + "§7/§e" + guild.getMaxClaimChunks());
                    });
                });
            });
        });
    }

    public void unclaimChunk(Player player) {
        if (!isOfficerOrLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the guild leader or officers can unclaim land.");
            return;
        }
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild.");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        String worldName = chunk.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        plugin.getSupabaseClient().getGuildClaims(guild.getId()).thenAccept(guildClaims -> {
            if (guildClaims == null) return;
            boolean found = false;
            for (JsonElement el : guildClaims) {
                JsonObject c = el.getAsJsonObject();
                if (c.get("world_name").getAsString().equals(worldName)
                        && c.get("chunk_x").getAsInt() == chunkX
                        && c.get("chunk_z").getAsInt() == chunkZ) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§cYour guild has not claimed this chunk."));
                return;
            }

            plugin.getSupabaseClient().removeGuildClaim(guild.getId(), worldName, chunkX, chunkZ).thenRun(() -> {
                plugin.getClaimManager().loadClaims();
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage("§aChunk unclaimed: §e(" + worldName + ", " + chunkX + ", " + chunkZ + ")"));
            });
        });
    }

    public void expandClaim(Player player) {
        if (!isOfficerOrLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the guild leader or officers can expand the claim limit.");
            return;
        }
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild.");
            return;
        }

        var playerData = plugin.getPlayerDataManager().getPlayerData(player);
        if (playerData == null) {
            player.sendMessage("§cYour data is still loading. Please try again.");
            return;
        }
        long playerGold = playerData.getGold();
        if (playerGold < expansionCost) {
            player.sendMessage("§cYou need §e" + expansionCost + " gold §cto expand your claim limit.");
            player.sendMessage("§7You have §e" + playerGold + " gold§7.");
            return;
        }

        playerData.setGold(playerGold - expansionCost);
        plugin.getSupabaseClient().updatePlayerGold(player.getUniqueId(), playerGold - expansionCost);

        int newMax = guild.getMaxClaimChunks() + 1;
        guild.setMaxClaimChunks(newMax);
        plugin.getSupabaseClient().updateGuildMaxChunks(guild.getId(), newMax).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§a§lClaim expanded! §eYour guild can now claim §e" + newMax + " §achunks.");
                player.sendMessage("§7Cost: §e" + expansionCost + " gold§7. Remaining gold: §e" + (playerGold - expansionCost));
            });
        });
    }

    public void showGuildInfo(Player player) {
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild. Use §e/guild create <name> §cto create one.");
            return;
        }

        player.sendMessage("§6§l======== Guild Info ========");
        player.sendMessage("§eName: §f" + guild.getGuildName());
        player.sendMessage("§eLeader: §f" + guild.getLeaderName());
        player.sendMessage("§eYour Rank: §f" + getPlayerRank(player.getUniqueId()).name());

        plugin.getSupabaseClient().getGuildClaims(guild.getId()).thenAccept(claims -> {
            if (claims == null) return;
            int claimCount = claims.size();
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§eClaims: §f" + claimCount + " / " + guild.getMaxClaimChunks());
                player.sendMessage("§eExpansion Cost: §f" + expansionCost + " gold per extra chunk");
            });
        });

        plugin.getSupabaseClient().getGuildMembers(guild.getId()).thenAccept(members -> {
            if (members == null) return;
            List<String> memberNames = new ArrayList<>();
            for (JsonElement el : members) {
                JsonObject m = el.getAsJsonObject();
                String name = m.has("player_name") ? m.get("player_name").getAsString() : "Unknown";
                String rank = m.has("rank") ? m.get("rank").getAsString() : "MEMBER";
                if (rank.equals("LEADER") || rank.equals("OFFICER")) {
                    memberNames.add("§6" + name + " [" + rank + "]");
                } else {
                    memberNames.add("§7" + name);
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§eMembers (" + memberNames.size() + "): §f" + String.join(", ", memberNames));
                player.sendMessage("§6§l============================");
            });
        });
    }

    public void setGuildHome(Player player) {
        if (!isOfficerOrLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the guild leader or officers can set the guild home.");
            return;
        }
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild.");
            return;
        }

        Location loc = player.getLocation();
        guild.setHome(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        plugin.getSupabaseClient().updateGuildHome(guild.getId(),
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        player.sendMessage("§a§lGuild home set! §7Members can teleport here with §e/guild home");
        player.sendMessage("§7Location: §f" + loc.getWorld().getName() + " (" +
                (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + ")");
    }

    public void teleportGuildHome(Player player) {
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild.");
            return;
        }
        if (!guild.hasHome()) {
            player.sendMessage("§cYour guild doesn't have a home set. An officer or leader can use §e/guild sethome§c.");
            return;
        }

        World world = Bukkit.getWorld(guild.getHomeWorld());
        if (world == null) {
            player.sendMessage("§cThe world the guild home is in no longer exists.");
            return;
        }

        Location homeLoc = new Location(world, guild.getHomeX(), guild.getHomeY(), guild.getHomeZ(),
                guild.getHomeYaw(), guild.getHomePitch());
        player.teleport(homeLoc);
        player.sendMessage("§a§lTeleported to guild home!");
    }

    public void delGuildHome(Player player) {
        if (!isOfficerOrLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the guild leader or officers can remove the guild home.");
            return;
        }
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage("§cYou are not in a guild.");
            return;
        }
        if (!guild.hasHome()) {
            player.sendMessage("§cYour guild doesn't have a home set.");
            return;
        }

        guild.clearHome();
        plugin.getSupabaseClient().clearGuildHome(guild.getId());
        player.sendMessage("§aGuild home removed.");
    }
}
