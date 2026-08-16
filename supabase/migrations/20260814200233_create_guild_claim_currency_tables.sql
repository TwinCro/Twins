/*
# Guild, Land Claim, and Currency System

1. Purpose
   Adds a guild system with land claiming and an in-game currency (gold).
   Players can create guilds, invite members, claim chunks of land, and
   expand their guild's claim by purchasing additional chunks with gold.
   Only the guild leader and appointed officers can expand claims.
   Claimed land is protected: non-members cannot break, place, or interact
   with blocks in claimed chunks.

2. New Tables
   - `dr_guilds`: one row per guild. Stores guild name, leader UUID,
     gold treasury, max-claim-chunks (starts at 10), and creation timestamp.
   - `dr_guild_members`: one row per (guild, player) pair. Stores the
     player's rank in the guild (MEMBER or OFFICER). The leader is
     stored as dr_guilds.leader_uuid and also has a row here.
   - `dr_guild_claims`: one row per claimed chunk. Stores guild_id,
     world name, chunk X, chunk Z. Unique on (guild_id, world_name, chunk_x, chunk_z).

3. Modified Tables
   - `dr_players`: added `gold` column (bigint, default 0) for the
     in-game currency. Players earn gold from mob kills and dungeon
     completion, and spend it on claim expansions.

4. Security
   - RLS enabled on all new tables.
   - Policies use `TO anon, authenticated` with `USING (true)` / `WITH CHECK (true)`
     because the Minecraft server plugin accesses these tables via the anon key
     (the server is a trusted client). No end-user browser sessions.

5. Important Notes
   - Guilds start with a max-claim-chunks of 10 (the initial free claim size).
   - Each additional chunk costs 500 gold (configurable in config.yml).
   - Only the leader and officers can expand claims or unclaim chunks.
   - Claimed chunks are identified by Minecraft chunk coordinates (chunk X, chunk Z)
     per world, matching Bukkit's Chunk.getX() / Chunk.getZ().
*/

-- Add gold currency column to players
ALTER TABLE dr_players ADD COLUMN IF NOT EXISTS gold bigint NOT NULL DEFAULT 0;

-- Guilds table
CREATE TABLE IF NOT EXISTS dr_guilds (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  guild_name text UNIQUE NOT NULL,
  leader_uuid text NOT NULL,
  leader_name text NOT NULL DEFAULT '',
  max_claim_chunks int NOT NULL DEFAULT 10,
  created_at timestamptz DEFAULT now()
);

ALTER TABLE dr_guilds ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_guilds_select" ON dr_guilds;
CREATE POLICY "dr_guilds_select" ON dr_guilds FOR SELECT
  TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "dr_guilds_insert" ON dr_guilds;
CREATE POLICY "dr_guilds_insert" ON dr_guilds FOR INSERT
  TO anon, authenticated WITH CHECK (true);

DROP POLICY IF EXISTS "dr_guilds_update" ON dr_guilds;
CREATE POLICY "dr_guilds_update" ON dr_guilds FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "dr_guilds_delete" ON dr_guilds;
CREATE POLICY "dr_guilds_delete" ON dr_guilds FOR DELETE
  TO anon, authenticated USING (true);

-- Guild members table
CREATE TABLE IF NOT EXISTS dr_guild_members (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  guild_id uuid NOT NULL REFERENCES dr_guilds(id) ON DELETE CASCADE,
  player_uuid text NOT NULL,
  player_name text NOT NULL DEFAULT '',
  rank text NOT NULL DEFAULT 'MEMBER',
  UNIQUE (guild_id, player_uuid)
);

ALTER TABLE dr_guild_members ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_guild_members_select" ON dr_guild_members;
CREATE POLICY "dr_guild_members_select" ON dr_guild_members FOR SELECT
  TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "dr_guild_members_insert" ON dr_guild_members;
CREATE POLICY "dr_guild_members_insert" ON dr_guild_members FOR INSERT
  TO anon, authenticated WITH CHECK (true);

DROP POLICY IF EXISTS "dr_guild_members_update" ON dr_guild_members;
CREATE POLICY "dr_guild_members_update" ON dr_guild_members FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "dr_guild_members_delete" ON dr_guild_members;
CREATE POLICY "dr_guild_members_delete" ON dr_guild_members FOR DELETE
  TO anon, authenticated USING (true);

-- Guild claims table
CREATE TABLE IF NOT EXISTS dr_guild_claims (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  guild_id uuid NOT NULL REFERENCES dr_guilds(id) ON DELETE CASCADE,
  world_name text NOT NULL,
  chunk_x int NOT NULL,
  chunk_z int NOT NULL,
  claimed_at timestamptz DEFAULT now(),
  UNIQUE (guild_id, world_name, chunk_x, chunk_z)
);

ALTER TABLE dr_guild_claims ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_guild_claims_select" ON dr_guild_claims;
CREATE POLICY "dr_guild_claims_select" ON dr_guild_claims FOR SELECT
  TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "dr_guild_claims_insert" ON dr_guild_claims;
CREATE POLICY "dr_guild_claims_insert" ON dr_guild_claims FOR INSERT
  TO anon, authenticated WITH CHECK (true);

DROP POLICY IF EXISTS "dr_guild_claims_delete" ON dr_guild_claims;
CREATE POLICY "dr_guild_claims_delete" ON dr_guild_claims FOR DELETE
  TO anon, authenticated USING (true);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_dr_guilds_leader ON dr_guilds (leader_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_guild_members_player ON dr_guild_members (player_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_guild_members_guild ON dr_guild_members (guild_id);
CREATE INDEX IF NOT EXISTS idx_dr_guild_claims_guild ON dr_guild_claims (guild_id);
CREATE INDEX IF NOT EXISTS idx_dr_guild_claims_chunk ON dr_guild_claims (world_name, chunk_x, chunk_z);
