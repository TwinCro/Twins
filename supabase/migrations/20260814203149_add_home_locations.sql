/*
# Add Home Locations for Players and Guilds

1. Purpose
   Adds personal home teleport locations for players and a shared guild home
   location for guilds. Players can set one personal home and teleport to it.
   Guild leaders/officers can set one guild home that all guild members can
   teleport to.

2. Modified Tables
   - `dr_players`: added home location columns:
     - `home_world` (text, nullable) - world name of the player's home
     - `home_x` (double precision, nullable) - X coordinate
     - `home_y` (double precision, nullable) - Y coordinate
     - `home_z` (double precision, nullable) - Z coordinate
     - `home_yaw` (real, nullable) - facing direction yaw
     - `home_pitch` (real, nullable) - facing direction pitch
   - `dr_guilds`: added guild home location columns:
     - `home_world` (text, nullable) - world name of the guild home
     - `home_x` (double precision, nullable) - X coordinate
     - `home_y` (double precision, nullable) - Y coordinate
     - `home_z` (double precision, nullable) - Z coordinate
     - `home_yaw` (real, nullable) - facing direction yaw
     - `home_pitch` (real, nullable) - facing direction pitch

3. Security
   - No policy changes. Existing CRUD policies on dr_players and dr_guilds
     already allow the server plugin (anon key) to read/write all rows.

4. Important Notes
   - All home columns are nullable: a null home_world means no home is set.
   - Players use /sethome, /home, /delhome for personal homes.
   - Guild leaders/officers use /guild sethome, members use /guild home.
*/

ALTER TABLE dr_players ADD COLUMN IF NOT EXISTS home_world text;
ALTER TABLE dr_players ADD COLUMN IF NOT EXISTS home_x double precision;
ALTER TABLE dr_players ADD COLUMN IF NOT EXISTS home_y double precision;
ALTER TABLE dr_players ADD COLUMN IF NOT EXISTS home_z double precision;
ALTER TABLE dr_players ADD COLUMN IF NOT EXISTS home_yaw real;
ALTER TABLE dr_players ADD COLUMN IF NOT EXISTS home_pitch real;

ALTER TABLE dr_guilds ADD COLUMN IF NOT EXISTS home_world text;
ALTER TABLE dr_guilds ADD COLUMN IF NOT EXISTS home_x double precision;
ALTER TABLE dr_guilds ADD COLUMN IF NOT EXISTS home_y double precision;
ALTER TABLE dr_guilds ADD COLUMN IF NOT EXISTS home_z double precision;
ALTER TABLE dr_guilds ADD COLUMN IF NOT EXISTS home_yaw real;
ALTER TABLE dr_guilds ADD COLUMN IF NOT EXISTS home_pitch real;
