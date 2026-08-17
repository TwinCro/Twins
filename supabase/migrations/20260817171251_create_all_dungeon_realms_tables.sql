/*
# Create All DungeonRealms Tables

1. Purpose
   Creates all tables needed for the DungeonRealms Minecraft plugin:
   players, player skills, dungeon runs, skill regions, guilds, guild members,
   guild claims, player homes, and custom dungeon builder tables.
   This migration combines all previously-written but never-applied migrations.

2. New Tables
   - dr_players: player class/level/XP/mana/gold/awakening data
   - dr_player_skills: unlocked and equipped skills per player
   - dr_dungeon_runs: dungeon completion history
   - dr_skill_regions: admin-defined skill-use regions
   - dr_guilds: guild definitions with treasury and claim limits
   - dr_guild_members: guild membership with ranks
   - dr_guild_claims: claimed land chunks per guild
   - dr_player_homes: multiple named homes per player
   - dr_custom_dungeons: admin-built custom dungeon definitions
   - dr_custom_dungeon_rooms: rooms within custom dungeons
   - dr_custom_room_mobs: mob configs for custom dungeon rooms

3. Security
   - RLS enabled on all tables.
   - All policies use TO anon, authenticated with USING(true)/WITH CHECK(true)
     because the Minecraft server plugin accesses these tables via the anon key
     (trusted server client, no end-user browser sessions).

4. Notes
   - Guilds start with max_claim_chunks of 10.
   - Players can have up to 3 homes (enforced in app code).
   - Custom dungeon rooms ordered by room_index (0=lobby, 1-N=mob, last=boss).
   - Deleting a guild cascades to members and claims.
   - Deleting a custom dungeon cascades to rooms and room mobs.
*/

-- Drop the broken table if it exists
DROP TABLE IF EXISTS dr_payer;

-- Players table
CREATE TABLE IF NOT EXISTS dr_players (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  minecraft_uuid text UNIQUE NOT NULL,
  username text NOT NULL DEFAULT '',
  class_id text NOT NULL DEFAULT '',
  level int NOT NULL DEFAULT 1,
  xp bigint NOT NULL DEFAULT 0,
  mana int NOT NULL DEFAULT 0,
  max_mana int NOT NULL DEFAULT 0,
  awakened boolean NOT NULL DEFAULT false,
  awakening_count int NOT NULL DEFAULT 0,
  gold bigint NOT NULL DEFAULT 0,
  last_saved timestamptz DEFAULT now()
);

ALTER TABLE dr_players ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_players_select" ON dr_players;
CREATE POLICY "dr_players_select" ON dr_players FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_players_insert" ON dr_players;
CREATE POLICY "dr_players_insert" ON dr_players FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_players_update" ON dr_players;
CREATE POLICY "dr_players_update" ON dr_players FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_players_delete" ON dr_players;
CREATE POLICY "dr_players_delete" ON dr_players FOR DELETE
  TO anon, authenticated USING (true);

-- Player skills table
CREATE TABLE IF NOT EXISTS dr_player_skills (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  player_uuid text NOT NULL,
  skill_id text NOT NULL,
  unlocked boolean NOT NULL DEFAULT true,
  equipped boolean NOT NULL DEFAULT false,
  skill_slot int,
  UNIQUE (player_uuid, skill_id)
);

ALTER TABLE dr_player_skills ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_skills_select" ON dr_player_skills;
CREATE POLICY "dr_skills_select" ON dr_player_skills FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_skills_insert" ON dr_player_skills;
CREATE POLICY "dr_skills_insert" ON dr_player_skills FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_skills_update" ON dr_player_skills;
CREATE POLICY "dr_skills_update" ON dr_player_skills FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_skills_delete" ON dr_player_skills;
CREATE POLICY "dr_skills_delete" ON dr_player_skills FOR DELETE
  TO anon, authenticated USING (true);

-- Dungeon runs table
CREATE TABLE IF NOT EXISTS dr_dungeon_runs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  player_uuid text NOT NULL,
  dungeon_id text NOT NULL,
  rank text NOT NULL DEFAULT 'COMMON',
  party_members text[] NOT NULL DEFAULT '{}',
  completed boolean NOT NULL DEFAULT false,
  completion_time timestamptz DEFAULT now(),
  loot_received text[] NOT NULL DEFAULT '{}'
);

ALTER TABLE dr_dungeon_runs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_runs_select" ON dr_dungeon_runs;
CREATE POLICY "dr_runs_select" ON dr_dungeon_runs FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_runs_insert" ON dr_dungeon_runs;
CREATE POLICY "dr_runs_insert" ON dr_dungeon_runs FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_runs_update" ON dr_dungeon_runs;
CREATE POLICY "dr_runs_update" ON dr_dungeon_runs FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_runs_delete" ON dr_dungeon_runs;
CREATE POLICY "dr_runs_delete" ON dr_dungeon_runs FOR DELETE
  TO anon, authenticated USING (true);

-- Skill regions table
CREATE TABLE IF NOT EXISTS dr_skill_regions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  region_name text NOT NULL,
  world_name text NOT NULL,
  min_x double precision NOT NULL,
  min_y double precision NOT NULL,
  min_z double precision NOT NULL,
  max_x double precision NOT NULL,
  max_y double precision NOT NULL,
  max_z double precision NOT NULL,
  created_at timestamptz DEFAULT now()
);

ALTER TABLE dr_skill_regions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_skill_regions_select" ON dr_skill_regions;
CREATE POLICY "dr_skill_regions_select" ON dr_skill_regions FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_skill_regions_insert" ON dr_skill_regions;
CREATE POLICY "dr_skill_regions_insert" ON dr_skill_regions FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_skill_regions_update" ON dr_skill_regions;
CREATE POLICY "dr_skill_regions_update" ON dr_skill_regions FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_skill_regions_delete" ON dr_skill_regions;
CREATE POLICY "dr_skill_regions_delete" ON dr_skill_regions FOR DELETE
  TO anon, authenticated USING (true);

-- Guilds table
CREATE TABLE IF NOT EXISTS dr_guilds (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  guild_name text UNIQUE NOT NULL,
  leader_uuid text NOT NULL,
  leader_name text NOT NULL DEFAULT '',
  max_claim_chunks int NOT NULL DEFAULT 10,
  home_world text,
  home_x double precision,
  home_y double precision,
  home_z double precision,
  home_yaw real,
  home_pitch real,
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

-- Player homes table
CREATE TABLE IF NOT EXISTS dr_player_homes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  player_uuid text NOT NULL,
  home_name text NOT NULL,
  world_name text NOT NULL,
  x double precision NOT NULL,
  y double precision NOT NULL,
  z double precision NOT NULL,
  yaw real NOT NULL DEFAULT 0,
  pitch real NOT NULL DEFAULT 0,
  UNIQUE (player_uuid, home_name)
);

ALTER TABLE dr_player_homes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_player_homes_select" ON dr_player_homes;
CREATE POLICY "dr_player_homes_select" ON dr_player_homes FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_player_homes_insert" ON dr_player_homes;
CREATE POLICY "dr_player_homes_insert" ON dr_player_homes FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_player_homes_update" ON dr_player_homes;
CREATE POLICY "dr_player_homes_update" ON dr_player_homes FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_player_homes_delete" ON dr_player_homes;
CREATE POLICY "dr_player_homes_delete" ON dr_player_homes FOR DELETE
  TO anon, authenticated USING (true);

-- Custom dungeons table
CREATE TABLE IF NOT EXISTS dr_custom_dungeons (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  dungeon_name text UNIQUE NOT NULL,
  display_name text NOT NULL DEFAULT '',
  rank text NOT NULL DEFAULT 'COMMON',
  min_level int NOT NULL DEFAULT 1,
  max_players int NOT NULL DEFAULT 5,
  lobby_world text NOT NULL DEFAULT 'world',
  lobby_x double precision NOT NULL DEFAULT 0,
  lobby_y double precision NOT NULL DEFAULT 64,
  lobby_z double precision NOT NULL DEFAULT 0,
  boss_type text NOT NULL DEFAULT 'ZOMBIE',
  boss_name text NOT NULL DEFAULT 'Boss',
  boss_hp double precision NOT NULL DEFAULT 500,
  boss_damage double precision NOT NULL DEFAULT 20,
  boss_defense double precision NOT NULL DEFAULT 10,
  boss_magic_defense double precision NOT NULL DEFAULT 10,
  chest_count int NOT NULL DEFAULT 3,
  created_by text NOT NULL DEFAULT '',
  created_at timestamptz DEFAULT now()
);

ALTER TABLE dr_custom_dungeons ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_cd_select" ON dr_custom_dungeons;
CREATE POLICY "dr_cd_select" ON dr_custom_dungeons FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_cd_insert" ON dr_custom_dungeons;
CREATE POLICY "dr_cd_insert" ON dr_custom_dungeons FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_cd_update" ON dr_custom_dungeons;
CREATE POLICY "dr_cd_update" ON dr_custom_dungeons FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_cd_delete" ON dr_custom_dungeons;
CREATE POLICY "dr_cd_delete" ON dr_custom_dungeons FOR DELETE
  TO anon, authenticated USING (true);

-- Custom dungeon rooms table
CREATE TABLE IF NOT EXISTS dr_custom_dungeon_rooms (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  dungeon_id uuid NOT NULL REFERENCES dr_custom_dungeons(id) ON DELETE CASCADE,
  room_index int NOT NULL,
  room_type text NOT NULL DEFAULT 'MOB',
  world_name text NOT NULL DEFAULT 'world',
  min_x double precision NOT NULL,
  min_y double precision NOT NULL,
  min_z double precision NOT NULL,
  max_x double precision NOT NULL,
  max_y double precision NOT NULL,
  max_z double precision NOT NULL,
  door_x double precision,
  door_y double precision,
  door_z double precision,
  door_material text DEFAULT 'IRON_BLOCK',
  spawn_marker_x double precision,
  spawn_marker_y double precision,
  spawn_marker_z double precision,
  UNIQUE (dungeon_id, room_index)
);

ALTER TABLE dr_custom_dungeon_rooms ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_cdr_select" ON dr_custom_dungeon_rooms;
CREATE POLICY "dr_cdr_select" ON dr_custom_dungeon_rooms FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_cdr_insert" ON dr_custom_dungeon_rooms;
CREATE POLICY "dr_cdr_insert" ON dr_custom_dungeon_rooms FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_cdr_update" ON dr_custom_dungeon_rooms;
CREATE POLICY "dr_cdr_update" ON dr_custom_dungeon_rooms FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_cdr_delete" ON dr_custom_dungeon_rooms;
CREATE POLICY "dr_cdr_delete" ON dr_custom_dungeon_rooms FOR DELETE
  TO anon, authenticated USING (true);

-- Custom room mobs table
CREATE TABLE IF NOT EXISTS dr_custom_room_mobs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  room_id uuid NOT NULL REFERENCES dr_custom_dungeon_rooms(id) ON DELETE CASCADE,
  entity_type text NOT NULL DEFAULT 'ZOMBIE',
  mob_count int NOT NULL DEFAULT 1,
  health double precision NOT NULL DEFAULT 100,
  damage double precision NOT NULL DEFAULT 10,
  defense double precision NOT NULL DEFAULT 0,
  magic_defense double precision NOT NULL DEFAULT 0
);

ALTER TABLE dr_custom_room_mobs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_crm_select" ON dr_custom_room_mobs;
CREATE POLICY "dr_crm_select" ON dr_custom_room_mobs FOR SELECT
  TO anon, authenticated USING (true);
DROP POLICY IF EXISTS "dr_crm_insert" ON dr_custom_room_mobs;
CREATE POLICY "dr_crm_insert" ON dr_custom_room_mobs FOR INSERT
  TO anon, authenticated WITH CHECK (true);
DROP POLICY IF EXISTS "dr_crm_update" ON dr_custom_room_mobs;
CREATE POLICY "dr_crm_update" ON dr_custom_room_mobs FOR UPDATE
  TO anon, authenticated USING (true) WITH CHECK (true);
DROP POLICY IF EXISTS "dr_crm_delete" ON dr_custom_room_mobs;
CREATE POLICY "dr_crm_delete" ON dr_custom_room_mobs FOR DELETE
  TO anon, authenticated USING (true);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_dr_players_uuid ON dr_players (minecraft_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_skills_player ON dr_player_skills (player_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_runs_player ON dr_dungeon_runs (player_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_guilds_leader ON dr_guilds (leader_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_guild_members_player ON dr_guild_members (player_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_guild_members_guild ON dr_guild_members (guild_id);
CREATE INDEX IF NOT EXISTS idx_dr_guild_claims_guild ON dr_guild_claims (guild_id);
CREATE INDEX IF NOT EXISTS idx_dr_guild_claims_chunk ON dr_guild_claims (world_name, chunk_x, chunk_z);
CREATE INDEX IF NOT EXISTS idx_dr_player_homes_player ON dr_player_homes (player_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_cdr_dungeon ON dr_custom_dungeon_rooms (dungeon_id);
CREATE INDEX IF NOT EXISTS idx_dr_crm_room ON dr_custom_room_mobs (room_id);
