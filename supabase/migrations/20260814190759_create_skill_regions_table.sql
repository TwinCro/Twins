/*
# Create skill_regions table

1. Purpose
   Stores admin-defined regions where skills can be used outside of dungeons
   for testing and showcase purposes. In these regions, skills affect mobs but
   never damage other players.

2. New Tables
   - `dr_skill_regions`: one row per region. Stores name, world, and the two
     corner coordinates (min/max x/y/z) defining a bounding box.

3. Security
   - RLS enabled. CRUD allowed for anon + authenticated (server plugin access).
*/

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
