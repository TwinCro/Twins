package com.dungeonrealms.commands;

import com.dungeonrealms.DungeonRealms;
import com.dungeonrealms.dungeon.DungeonConfig;
import com.dungeonrealms.dungeon.DungeonRank;
import com.dungeonrealms.dungeon.DungeonRoom;
import com.dungeonrealms.dungeon.builder.BuilderSession;
import com.dungeonrealms.dungeon.builder.BuilderSession.BuilderStep;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonBuilderCommand implements CommandExecutor, TabCompleter {

    private final DungeonRealms plugin;

    public DungeonBuilderCommand(DungeonRealms plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }
        if (!player.hasPermission("dungeonrealms.admin")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "setcorner1" -> handleSetCorner1(player);
            case "setcorner2" -> handleSetCorner2(player);
            case "setdoor" -> handleSetDoor(player);
            case "setmarker" -> handleSetMarker(player);
            case "settype" -> handleSetType(player, args);
            case "addmob" -> handleAddMob(player, args);
            case "setboss" -> handleSetBoss(player, args);
            case "donemobs" -> handleDoneMobs(player);
            case "save" -> handleSave(player);
            case "cancel" -> handleCancel(player);
            case "status" -> handleStatus(player);
            case "list" -> handleList(player);
            case "delete" -> handleDelete(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6§l=== Dungeon Builder ===");
        player.sendMessage("§e/db create <name> <rank> §7- Start building a dungeon");
        player.sendMessage("§e/db setcorner1 §7- Set first corner of current room");
        player.sendMessage("§e/db setcorner2 §7- Set second corner of current room");
        player.sendMessage("§e/db setdoor §7- Set door location for current room");
        player.sendMessage("§e/db setmarker §7- Set spawn marker (miniboss/boss rooms)");
        player.sendMessage("§e/db settype <MOB|MINIBOSS> §7- Set current room type");
        player.sendMessage("§e/db addmob <type> <count> <hp> <dmg> <def> <mdef> §7- Add mob");
        player.sendMessage("§e/db setboss <type> <name> <hp> <dmg> <def> <mdef> §7- Set boss");
        player.sendMessage("§e/db donemobs §7- Finish mob setup, move to next room");
        player.sendMessage("§e/db save §7- Save the dungeon");
        player.sendMessage("§e/db cancel §7- Cancel building");
        player.sendMessage("§e/db status §7- Show progress");
        player.sendMessage("§e/db list §7- List custom dungeons");
        player.sendMessage("§e/db delete <name> §7- Delete a custom dungeon");
    }

    private void handleCreate(Player player, String[] args) {
        if (plugin.getDungeonBuilderManager().getSession(player.getUniqueId()) != null) {
            player.sendMessage("§cYou already have an active builder session! Use §e/db cancel §cto cancel it.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§cUsage: /db create <name> <rank>");
            player.sendMessage("§7Ranks: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY");
            return;
        }

        String name = args[1];
        if (!name.matches("[a-zA-Z0-9_]+") || name.length() < 3 || name.length() > 24) {
            player.sendMessage("§cName must be 3-24 characters (letters, numbers, underscores).");
            return;
        }

        DungeonRank rank = DungeonRank.fromString(args[2]);
        BuilderSession session = plugin.getDungeonBuilderManager().createSession(player.getUniqueId(), name, rank);

        player.sendMessage("§a§lDungeon Builder started!");
        player.sendMessage("§eDungeon: §f" + name + " §7| Rank: " + rank.getColor().replace("&", "§") + rank.getDisplayName());
        player.sendMessage("§7Rooms needed: §e1 lobby + " + rank.getRoomCount() + " mob rooms + 1 boss room = " + session.getTotalRoomCount() + " total");
        player.sendMessage("");
        player.sendMessage("§e§lStep 1: §fSet up the LOBBY room.");
        player.sendMessage("§7Stand at corner 1 of your lobby and use §e/db setcorner1");
    }

    private void handleSetCorner1(Player player) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        BuilderStep step = session.getCurrentStep();
        if (step != BuilderStep.SET_LOBBY_CORNER1
                && step != BuilderStep.ADD_ROOM_CORNER1
                && step != BuilderStep.SET_BOSS_CORNER1) {
            player.sendMessage("§cYou're not at a step that needs corner 1. Use §e/db status §cto check.");
            return;
        }

        session.setTempCorner1(player.getLocation().clone());
        player.sendMessage("§aCorner 1 set at your position.");
        player.sendMessage("§7Now go to the opposite corner and use §e/db setcorner2");

        if (step == BuilderStep.SET_LOBBY_CORNER1) {
            session.setCurrentStep(BuilderStep.SET_LOBBY_CORNER2);
        } else if (step == BuilderStep.ADD_ROOM_CORNER1) {
            session.setCurrentStep(BuilderStep.ADD_ROOM_CORNER2);
        } else {
            session.setCurrentStep(BuilderStep.SET_BOSS_CORNER2);
        }
    }

    private void handleSetCorner2(Player player) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        BuilderStep step = session.getCurrentStep();
        if (step != BuilderStep.SET_LOBBY_CORNER2
                && step != BuilderStep.ADD_ROOM_CORNER2
                && step != BuilderStep.SET_BOSS_CORNER2) {
            player.sendMessage("§cSet corner 1 first with §e/db setcorner1");
            return;
        }

        Location corner1 = session.getTempCorner1();
        Location corner2 = player.getLocation().clone();

        DungeonRoom room = new DungeonRoom();
        room.setWorldName(corner2.getWorld().getName());
        room.setMinX(Math.min(corner1.getX(), corner2.getX()));
        room.setMinY(Math.min(corner1.getY(), corner2.getY()));
        room.setMinZ(Math.min(corner1.getZ(), corner2.getZ()));
        room.setMaxX(Math.max(corner1.getX(), corner2.getX()));
        room.setMaxY(Math.max(corner1.getY(), corner2.getY()));
        room.setMaxZ(Math.max(corner1.getZ(), corner2.getZ()));

        int sizeX = (int) (room.getMaxX() - room.getMinX());
        int sizeY = (int) (room.getMaxY() - room.getMinY());
        int sizeZ = (int) (room.getMaxZ() - room.getMinZ());

        if (step == BuilderStep.SET_LOBBY_CORNER2) {
            room.setIndex(0);
            room.setType(DungeonRoom.RoomType.LOBBY);
            session.setCurrentRoom(room);
            session.setLobbySpawn(corner1.clone());
            player.sendMessage("§aLobby bounds set! §7(" + sizeX + "x" + sizeY + "x" + sizeZ + " blocks)");
            player.sendMessage("§7Now stand at the DOOR to Room 1 and use §e/db setdoor");
            session.setCurrentStep(BuilderStep.SET_LOBBY_DOOR);
        } else if (step == BuilderStep.ADD_ROOM_CORNER2) {
            int roomNum = session.getCurrentRoomNumber();
            room.setIndex(roomNum);
            room.setType(DungeonRoom.RoomType.MOB);
            session.setCurrentRoom(room);
            player.sendMessage("§aRoom " + roomNum + " bounds set! §7(" + sizeX + "x" + sizeY + "x" + sizeZ + " blocks)");
            player.sendMessage("§7Now stand at the DOOR to the next room and use §e/db setdoor");
            player.sendMessage("§7To make this a miniboss room, use §e/db settype MINIBOSS §7first.");
            session.setCurrentStep(BuilderStep.SET_ROOM_DOOR);
        } else {
            int bossIndex = 1 + session.getRequiredMobRooms();
            room.setIndex(bossIndex);
            room.setType(DungeonRoom.RoomType.BOSS);
            session.setCurrentRoom(room);
            player.sendMessage("§aBoss room bounds set! §7(" + sizeX + "x" + sizeY + "x" + sizeZ + " blocks)");
            player.sendMessage("§7Now stand at the BOSS spawn point and use §e/db setmarker");
            session.setCurrentStep(BuilderStep.SET_BOSS_MARKER);
        }
    }

    private void handleSetDoor(Player player) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        BuilderStep step = session.getCurrentStep();
        if (step != BuilderStep.SET_LOBBY_DOOR && step != BuilderStep.SET_ROOM_DOOR) {
            player.sendMessage("§cYou're not at a step that needs a door location.");
            return;
        }

        DungeonRoom room = session.getCurrentRoom();
        Location loc = player.getLocation().clone();
        room.setDoorX(loc.getBlockX() + 0.5);
        room.setDoorY((double) loc.getBlockY());
        room.setDoorZ(loc.getBlockZ() + 0.5);
        room.setDoorMaterial("IRON_BLOCK");

        if (step == BuilderStep.SET_LOBBY_DOOR) {
            session.getRooms().add(room);
            session.setCurrentRoomNumber(1);
            player.sendMessage("§aLobby complete with door set!");
            player.sendMessage("");
            player.sendMessage("§e§lRoom 1/" + session.getRequiredMobRooms() + " (MOB)");
            player.sendMessage("§7Go to room 1, stand at corner 1, use §e/db setcorner1");
            session.setCurrentStep(BuilderStep.ADD_ROOM_CORNER1);
        } else {
            if (room.getType() == DungeonRoom.RoomType.MINIBOSS) {
                player.sendMessage("§aDoor set! Now stand at the MINIBOSS spawn point and use §e/db setmarker");
                session.setCurrentStep(BuilderStep.SET_ROOM_MARKER);
            } else {
                player.sendMessage("§aDoor set! Now add mobs with §e/db addmob <type> <count> <hp> <dmg> <def> <mdef>");
                session.setCurrentStep(BuilderStep.SET_ROOM_MOBS);
            }
        }
    }

    private void handleSetMarker(Player player) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        BuilderStep step = session.getCurrentStep();
        if (step != BuilderStep.SET_ROOM_MARKER && step != BuilderStep.SET_BOSS_MARKER) {
            player.sendMessage("§cYou're not at a step that needs a spawn marker.");
            return;
        }

        DungeonRoom room = session.getCurrentRoom();
        Location loc = player.getLocation().clone();
        room.setSpawnMarkerX(loc.getX());
        room.setSpawnMarkerY(loc.getY());
        room.setSpawnMarkerZ(loc.getZ());

        if (step == BuilderStep.SET_BOSS_MARKER) {
            player.sendMessage("§aBoss spawn marker set!");
            player.sendMessage("§7Now set the boss with §e/db setboss <type> <name> <hp> <dmg> <def> <mdef>");
            player.sendMessage("§7And add any additional mobs with §e/db addmob");
            session.setCurrentStep(BuilderStep.SET_BOSS_MOBS);
        } else {
            player.sendMessage("§aMiniboss spawn marker set!");
            player.sendMessage("§7Now add mobs with §e/db addmob <type> <count> <hp> <dmg> <def> <mdef>");
            session.setCurrentStep(BuilderStep.SET_ROOM_MOBS);
        }
    }

    private void handleSetType(Player player, String[] args) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        if (session.getCurrentStep() != BuilderStep.SET_ROOM_DOOR) {
            player.sendMessage("§cYou can only set the room type after setting corners and before setting the door.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cUsage: /db settype <MOB|MINIBOSS>");
            return;
        }

        DungeonRoom room = session.getCurrentRoom();
        String typeStr = args[1].toUpperCase();
        if (typeStr.equals("MINIBOSS")) {
            room.setType(DungeonRoom.RoomType.MINIBOSS);
            player.sendMessage("§aRoom set to MINIBOSS type. You'll need to set a spawn marker after the door.");
        } else {
            room.setType(DungeonRoom.RoomType.MOB);
            player.sendMessage("§aRoom set to MOB type.");
        }
    }

    private void handleAddMob(Player player, String[] args) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        BuilderStep step = session.getCurrentStep();
        if (step != BuilderStep.SET_ROOM_MOBS && step != BuilderStep.SET_BOSS_MOBS) {
            player.sendMessage("§cYou're not at a mob configuration step.");
            return;
        }
        if (args.length < 7) {
            player.sendMessage("§cUsage: /db addmob <type> <count> <hp> <dmg> <def> <mdef>");
            player.sendMessage("§7Example: /db addmob ZOMBIE 8 400 25 20 10");
            return;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid entity type: " + args[1]);
            return;
        }

        try {
            int count = Integer.parseInt(args[2]);
            double hp = Double.parseDouble(args[3]);
            double dmg = Double.parseDouble(args[4]);
            double def = Double.parseDouble(args[5]);
            double mdef = Double.parseDouble(args[6]);

            DungeonRoom.RoomMobEntry entry = new DungeonRoom.RoomMobEntry(type, count, hp, dmg, def, mdef);
            session.getCurrentRoom().getMobs().add(entry);

            player.sendMessage("§aAdded " + count + "x " + type.name() + " §7(HP:" + (int) hp
                    + " DMG:" + (int) dmg + " DEF:" + (int) def + " MDEF:" + (int) mdef + ")");
            player.sendMessage("§7Add more mobs or use §e/db donemobs §7to finish this room.");
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid numbers. Usage: /db addmob <type> <count> <hp> <dmg> <def> <mdef>");
        }
    }

    private void handleSetBoss(Player player, String[] args) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        if (session.getCurrentStep() != BuilderStep.SET_BOSS_MOBS) {
            player.sendMessage("§cYou can only set the boss in the boss room.");
            return;
        }
        if (args.length < 7) {
            player.sendMessage("§cUsage: /db setboss <type> <name> <hp> <dmg> <def> <mdef>");
            player.sendMessage("§7Example: /db setboss WITHER WitherKing 10000 85 70 100");
            return;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid entity type: " + args[1]);
            return;
        }

        try {
            String bossName = args[2].replace("_", " ");
            double hp = Double.parseDouble(args[3]);
            double dmg = Double.parseDouble(args[4]);
            double def = Double.parseDouble(args[5]);
            double mdef = Double.parseDouble(args[6]);

            DungeonConfig.BossConfig bossConfig = new DungeonConfig.BossConfig();
            bossConfig.setType(type);
            bossConfig.setName("§4§l" + bossName);
            bossConfig.setHealth(hp);
            bossConfig.setDamage(dmg);
            bossConfig.setDefense(def);
            bossConfig.setMagicDefense(mdef);
            session.setBossConfig(bossConfig);

            player.sendMessage("§aBoss set: §4§l" + bossName + " §7(" + type.name() + ")");
            player.sendMessage("§7HP:" + (int) hp + " DMG:" + (int) dmg + " DEF:" + (int) def + " MDEF:" + (int) mdef);
            player.sendMessage("§7Add extra mobs with §e/db addmob §7or finish with §e/db donemobs");
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid numbers. Check your <hp> <dmg> <def> <mdef> values.");
        }
    }

    private void handleDoneMobs(Player player) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        BuilderStep step = session.getCurrentStep();
        if (step != BuilderStep.SET_ROOM_MOBS && step != BuilderStep.SET_BOSS_MOBS) {
            player.sendMessage("§cYou're not at a mob configuration step.");
            return;
        }

        DungeonRoom room = session.getCurrentRoom();
        if (room.getMobs().isEmpty() && step == BuilderStep.SET_ROOM_MOBS) {
            player.sendMessage("§cYou must add at least one mob to this room! Use §e/db addmob");
            return;
        }

        if (step == BuilderStep.SET_BOSS_MOBS) {
            if (session.getBossConfig() == null) {
                player.sendMessage("§cYou must set a boss first! Use §e/db setboss");
                return;
            }
            session.getRooms().add(room);
            player.sendMessage("§a§lBoss room complete!");
            player.sendMessage("");
            player.sendMessage("§a§lAll " + session.getTotalRoomCount() + " rooms configured!");
            player.sendMessage("§7Use §e/db save §7to save the dungeon or §e/db cancel §7to discard.");
            session.setCurrentStep(BuilderStep.CONFIRM_SAVE);
            return;
        }

        session.getRooms().add(room);
        int roomNum = session.getCurrentRoomNumber();
        int required = session.getRequiredMobRooms();

        if (roomNum >= required) {
            session.setCurrentRoomNumber(roomNum + 1);
            player.sendMessage("§aRoom " + roomNum + "/" + required + " complete!");
            player.sendMessage("");
            player.sendMessage("§e§lBoss Room §7- Go to your boss room, stand at corner 1, use §e/db setcorner1");
            session.setCurrentStep(BuilderStep.SET_BOSS_CORNER1);
        } else {
            int nextRoom = roomNum + 1;
            session.setCurrentRoomNumber(nextRoom);
            player.sendMessage("§aRoom " + roomNum + "/" + required + " complete!");
            player.sendMessage("");
            player.sendMessage("§e§lRoom " + nextRoom + "/" + required + " (MOB)");
            player.sendMessage("§7Go to room " + nextRoom + ", stand at corner 1, use §e/db setcorner1");
            session.setCurrentStep(BuilderStep.ADD_ROOM_CORNER1);
        }
    }

    private void handleSave(Player player) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        if (session.getCurrentStep() != BuilderStep.CONFIRM_SAVE) {
            player.sendMessage("§cYou haven't finished setting up all rooms yet. Use §e/db status §cto check progress.");
            return;
        }

        plugin.getDungeonBuilderManager().saveDungeon(session);
        plugin.getDungeonBuilderManager().removeSession(player.getUniqueId());

        player.sendMessage("§a§lDungeon '" + session.getDungeonName() + "' saved!");
        player.sendMessage("§7" + session.getTotalRoomCount() + " rooms | "
                + session.getRank().getColor().replace("&", "§") + session.getRank().getDisplayName() + " §7rank");
        player.sendMessage("§7Players can enter with §6/dungeon enter " + session.getDungeonName());
    }

    private void handleCancel(Player player) {
        BuilderSession session = plugin.getDungeonBuilderManager().getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§cYou don't have an active builder session.");
            return;
        }
        plugin.getDungeonBuilderManager().removeSession(player.getUniqueId());
        player.sendMessage("§cBuilder session cancelled.");
    }

    private void handleStatus(Player player) {
        BuilderSession session = requireSession(player);
        if (session == null) return;

        player.sendMessage("§6§l=== Dungeon Builder: " + session.getDungeonName() + " ===");
        player.sendMessage("§7Rank: " + session.getRank().getColor().replace("&", "§")
                + session.getRank().getDisplayName() + " §7| Total rooms: " + session.getTotalRoomCount());
        player.sendMessage("");

        int completed = session.getRooms().size();
        int total = session.getTotalRoomCount();

        for (int i = 0; i < total; i++) {
            String label;
            if (i == 0) label = "Lobby";
            else if (i == total - 1) label = "Boss Room";
            else label = "Room " + i;

            if (i < completed) {
                DungeonRoom room = session.getRooms().get(i);
                int mobCount = room.getMobs().stream().mapToInt(DungeonRoom.RoomMobEntry::getCount).sum();
                player.sendMessage("§a+ §f" + label + " §7" + room.getType().name()
                        + (room.getType() != DungeonRoom.RoomType.LOBBY ? " — " + mobCount + " mobs" : ""));
            } else if (i == completed) {
                player.sendMessage("§e> §f" + label + " §7(in progress — " + session.getCurrentStep().name() + ")");
            } else {
                player.sendMessage("§8- §f" + label + " §7(pending)");
            }
        }
    }

    private void handleList(Player player) {
        var dungeons = plugin.getDungeonManager().getAllDungeons();
        List<DungeonConfig> custom = dungeons.stream().filter(DungeonConfig::isCustom).collect(Collectors.toList());
        if (custom.isEmpty()) {
            player.sendMessage("§7No custom dungeons created yet.");
            return;
        }
        player.sendMessage("§6§l=== Custom Dungeons ===");
        for (DungeonConfig dg : custom) {
            player.sendMessage("§e" + dg.getDisplayName() + " §7| "
                    + dg.getRank().getColor().replace("&", "§") + dg.getRank().getDisplayName()
                    + " §7| " + dg.getRooms().size() + " rooms");
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /db delete <name>");
            return;
        }
        String name = args[1];
        plugin.getDungeonBuilderManager().deleteCustomDungeon(name);
        player.sendMessage("§aCustom dungeon '" + name + "' deleted.");
    }

    private BuilderSession requireSession(Player player) {
        BuilderSession session = plugin.getDungeonBuilderManager().getSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§cNo active builder session. Start one with §e/db create <name> <rank>");
        }
        return session;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("create", "setcorner1", "setcorner2", "setdoor",
                    "setmarker", "settype", "addmob", "setboss", "donemobs", "save", "cancel",
                    "status", "list", "delete");
            String prefix = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(prefix)) completions.add(s);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("settype")) {
                completions.addAll(Arrays.asList("MOB", "MINIBOSS"));
            } else if (sub.equals("addmob") || sub.equals("setboss")) {
                String prefix = args[1].toUpperCase();
                for (EntityType et : EntityType.values()) {
                    if (et.isAlive() && et.name().startsWith(prefix)) {
                        completions.add(et.name());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            for (DungeonRank rank : DungeonRank.values()) {
                completions.add(rank.name());
            }
        }
        return completions;
    }
}
