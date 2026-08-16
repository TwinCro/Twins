/*
# Custom Dungeon Builder Tables

1. Purpose
   Stores admin-created dungeon definitions built with the in-game dungeon builder.
   Each dungeon has ordered rooms with bounding boxes, door locations, and mob configs.

2. New Tables
   - dr_custom_dungeons: one row per custom dungeon (name, rank, boss config, lobby spawn)
   - dr_custom_dungeon_rooms: one row per room (index, type, bounds, door, spawn marker)
   - dr_custom_room_mobs: one row per mob entry in a room (type, count, stats)

3. Security
   - RLS enabled on all tables.
   - CRUD allowed for anon + authenticated (server plugin uses anon key).

4. Notes
   - Rooms ordered by room_index (0=lobby, 1-N=mob/miniboss, last=boss).
   - Deleting a dungeon cascades to rooms, which cascades to room mobs.
*/

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

CREATE INDEX IF NOT EXISTS idx_dr_cdr_dungeon ON dr_custom_dungeon_rooms (dungeon_id);
CREATE INDEX IF NOT EXISTS idx_dr_crm_room ON dr_custom_room_mobs (room_id);
