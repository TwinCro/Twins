package com.dungeonrealms.guild;

public class Guild {

    public enum Rank {
        MEMBER, OFFICER, LEADER;

        public static Rank fromString(String str) {
            if (str == null) return MEMBER;
            for (Rank r : values()) {
                if (r.name().equalsIgnoreCase(str)) return r;
            }
            return MEMBER;
        }
    }

    private String id;
    private String guildName;
    private String leaderUuid;
    private String leaderName;
    private int maxClaimChunks;
    private long createdAt;
    private String homeWorld;
    private double homeX, homeY, homeZ;
    private float homeYaw, homePitch;

    public Guild(String id, String guildName, String leaderUuid, String leaderName, int maxClaimChunks) {
        this.id = id;
        this.guildName = guildName;
        this.leaderUuid = leaderUuid;
        this.leaderName = leaderName;
        this.maxClaimChunks = maxClaimChunks;
    }

    public String getId() { return id; }
    public String getGuildName() { return guildName; }
    public String getLeaderUuid() { return leaderUuid; }
    public String getLeaderName() { return leaderName; }
    public int getMaxClaimChunks() { return maxClaimChunks; }
    public void setMaxClaimChunks(int max) { this.maxClaimChunks = max; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public boolean hasHome() { return homeWorld != null; }
    public String getHomeWorld() { return homeWorld; }
    public double getHomeX() { return homeX; }
    public double getHomeY() { return homeY; }
    public double getHomeZ() { return homeZ; }
    public float getHomeYaw() { return homeYaw; }
    public float getHomePitch() { return homePitch; }
    public void setHome(String world, double x, double y, double z, float yaw, float pitch) {
        this.homeWorld = world; this.homeX = x; this.homeY = y; this.homeZ = z;
        this.homeYaw = yaw; this.homePitch = pitch;
    }
    public void clearHome() { this.homeWorld = null; }
}
