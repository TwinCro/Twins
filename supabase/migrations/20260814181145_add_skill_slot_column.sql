/*
# Add skill_slot column to dr_player_skills

1. Purpose
   Allows players to bind unlocked skills to hotbar slots 1-6. When the player
   presses F to activate "skill bar mode" and then presses number keys 1-6,
   the skill bound to that slot is triggered instead of switching hotbar items.

2. Changes
   - Added `skill_slot` column (nullable int) to `dr_player_skills`.
     A value of 1-6 means the skill is bound to that slot.
     NULL means the skill is unlocked but not bound to any slot.

3. Security
   - No policy changes. Existing CRUD policies on dr_player_skills already
     allow the server plugin (anon key) to read/write all rows.
*/

ALTER TABLE dr_player_skills ADD COLUMN IF NOT EXISTS skill_slot int;
