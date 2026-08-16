package com.dungeonrealms.party;

import com.dungeonrealms.DungeonRealms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {

    private final DungeonRealms plugin;
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    public PartyManager(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    public Party createParty(Player leader) {
        Party party = new Party(leader.getUniqueId());
        parties.put(leader.getUniqueId(), party);
        return party;
    }

    public Party getParty(Player player) {
        return parties.get(player.getUniqueId());
    }

    public Party getPartyByMember(UUID uuid) {
        for (Party party : parties.values()) {
            if (party.isMember(uuid)) return party;
        }
        return null;
    }

    public boolean hasParty(Player player) {
        return getPartyByMember(player.getUniqueId()) != null;
    }

    public void invitePlayer(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null) {
            party = createParty(leader);
        }
        if (party.size() >= 5) {
            leader.sendMessage("§cParty is full (max 5 players).");
            return;
        }
        party.invite(target.getUniqueId());
        pendingInvites.put(target.getUniqueId(), leader.getUniqueId());
        leader.sendMessage("§aInvited " + target.getName() + " to your party.");
        target.sendMessage("§a" + leader.getName() + " invited you to their party. Use §e/party accept§a to join.");
    }

    public boolean acceptInvite(Player player) {
        UUID leaderUuid = pendingInvites.get(player.getUniqueId());
        if (leaderUuid == null) {
            player.sendMessage("§cYou have no pending party invites.");
            return false;
        }
        Party party = parties.get(leaderUuid);
        if (party == null) {
            pendingInvites.remove(player.getUniqueId());
            player.sendMessage("§cThat party no longer exists.");
            return false;
        }
        party.addMember(player.getUniqueId());
        parties.put(player.getUniqueId(), party);
        pendingInvites.remove(player.getUniqueId());

        for (UUID memberUuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null) {
                member.sendMessage("§a" + player.getName() + " joined the party.");
            }
        }
        return true;
    }

    public void leaveParty(Player player) {
        Party party = getPartyByMember(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cYou are not in a party.");
            return;
        }
        party.removeMember(player.getUniqueId());
        parties.remove(player.getUniqueId());

        for (UUID memberUuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null) {
                member.sendMessage("§e" + player.getName() + " left the party.");
            }
        }

        if (party.getMembers().isEmpty()) {
            parties.remove(party.getLeader());
        }
        player.sendMessage("§aYou left the party.");
    }

    public void disbandParty(Player leader) {
        Party party = getParty(leader);
        if (party == null) return;
        for (UUID uuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                member.sendMessage("§eThe party has been disbanded.");
            }
            parties.remove(uuid);
        }
    }

    public void listParty(Player player) {
        Party party = getPartyByMember(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cYou are not in a party.");
            return;
        }
        player.sendMessage("§a§lParty Members:");
        for (UUID uuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            String name = member != null ? member.getName() : "Offline";
            String role = uuid.equals(party.getLeader()) ? " §7(Leader)" : "";
            player.sendMessage("§7- §f" + name + role);
        }
    }
}
