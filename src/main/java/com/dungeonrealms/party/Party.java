package com.dungeonrealms.party;

import org.bukkit.entity.Player;

import java.util.*;

public class Party {

    private final UUID leader;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() { return leader; }

    public Set<UUID> getMembers() { return members; }

    public boolean isMember(UUID uuid) { return members.contains(uuid); }

    public void addMember(UUID uuid) {
        members.add(uuid);
        pendingInvites.remove(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public void invite(UUID uuid) {
        pendingInvites.add(uuid);
    }

    public boolean hasPendingInvite(UUID uuid) {
        return pendingInvites.contains(uuid);
    }

    public void removeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
    }

    public int size() { return members.size(); }
}
