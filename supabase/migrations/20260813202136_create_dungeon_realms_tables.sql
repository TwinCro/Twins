/*
# DungeonRealms Plugin Tables

1. Purpose
   Stores persistent MMORPG data for the DungeonRealms Minecraft plugin:
   player class/level/XP/mana, awakening state, unlocked skills, and dungeon run history.

2. New Tables
   - `dr_players`: one row per Minecraft player. Tracks class, level, XP, mana,
     awakened status, awakening count, and last-save timestamp.
   - `dr_player_skills`: one row per (player, skill) pair. Tracks which skills
     a player has unlocked and whether the skill is currently equipped.
   - `dr_dungeon_runs`: one row per dungeon completion. Tracks who entered,
     which dungeon, its rank, party members, completion time, and loot received.

3. Security
   - RLS enabled on all tables.
   - Policies use `TO anon, authenticated` with `USING (true)` / `WITH CHECK (true)`
     because this data is accessed by the Minecraft server plugin via the anon key
     (the server is a trusted client). This is intentionally public CRUD from the
     server's perspective — there are no end-user browser sessions.
*/

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

-- Indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_dr_players_uuid ON dr_players (minecraft_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_skills_player ON dr_player_skills (player_uuid);
CREATE INDEX IF NOT EXISTS idx_dr_runs_player ON dr_dungeon_runs (player_uuid);
