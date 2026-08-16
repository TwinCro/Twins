/*
# Create Player Homes Table (Multiple Homes)

1. Purpose
   Replaces the single-home columns on dr_players with a dedicated table
   that supports up to 3 named homes per player. Each home has a name,
   world, coordinates, and facing direction. The max of 3 is enforced
   in application code.

2. New Tables
   - `dr_player_homes`: one row per saved home.
     - `id` (uuid, primary key)
     - `player_uuid` (text, not null) - the player's Minecraft UUID
     - `home_name` (text, not null) - user-chosen name for the home
     - `world_name` (text, not null) - Minecraft world name
     - `x` (double precision) - X coordinate
     - `y` (double precision) - Y coordinate
     - `z` (double precision) - Z coordinate
     - `yaw` (real) - facing yaw
     - `pitch` (real) - facing pitch
     - UNIQUE constraint on (player_uuid, home_name)

3. Security
   - RLS enabled. Policies use `TO anon, authenticated` with `USING (true)`
     because this data is accessed by the Minecraft server plugin via the
     anon key (the server is a trusted client).

4. Important Notes
   - The old single-home columns on dr_players (home_world, home_x, etc.)
     are left in place (not dropped) to preserve any existing data. They
     are no longer used by the code.
   - Application code enforces the 3-home limit before inserting.
*/

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

CREATE INDEX IF NOT EXISTS idx_dr_player_homes_player ON dr_player_homes (player_uuid);
