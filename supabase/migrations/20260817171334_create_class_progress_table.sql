/*
# Create Class Progress Table

1. Purpose
   Stores per-class progress for each player so they can switch between classes
   without losing progress on any class.

2. New Tables
   - dr_class_progress: one row per (player, class) pair with level, XP, and skills.

3. Security
   - RLS enabled. CRUD allowed for anon + authenticated (server plugin access).
*/

CREATE TABLE IF NOT EXISTS dr_class_progress (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  player_uuid text NOT NULL,
  class_id text NOT NULL,
  level int NOT NULL DEFAULT 1,
  xp bigint NOT NULL DEFAULT 0,
  skills jsonb NOT NULL DEFAULT '[]'::jsonb,
  UNIQUE (player_uuid, class_id)
);

ALTER TABLE dr_class_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "dr_class_progress_select" ON dr_class_progress;
CREATE POLICY "dr_class_progress_select" ON dr_class_progress
  FOR SELECT TO anon, authenticated USING (true);

DROP POLICY IF EXISTS "dr_class_progress_insert" ON dr_class_progress;
CREATE POLICY "dr_class_progress_insert" ON dr_class_progress
  FOR INSERT TO anon, authenticated WITH CHECK (true);

DROP POLICY IF EXISTS "dr_class_progress_update" ON dr_class_progress;
CREATE POLICY "dr_class_progress_update" ON dr_class_progress
  FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "dr_class_progress_delete" ON dr_class_progress;
CREATE POLICY "dr_class_progress_delete" ON dr_class_progress
  FOR DELETE TO anon, authenticated USING (true);

CREATE INDEX IF NOT EXISTS idx_dr_class_progress_player ON dr_class_progress (player_uuid);
