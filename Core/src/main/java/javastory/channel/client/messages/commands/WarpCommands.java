package javastory.channel.client.messages.commands;

import java.rmi.RemoteException;
import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.server.Portal;
import javastory.channel.maps.GameMap;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import javastory.channel.maps.GameMapFactory;
import javastory.server.ChannelServer;
import tools.MaplePacketCreator;

public class WarpCommands implements Command {

    private ChannelCharacter victim;

    @Override
    public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        ChannelServer cserv = c.getChannelServer();
        final GameMapFactory mapFactory = c.getChannelServer().getMapFactory(c.getWorldId());
        final ChannelCharacter player = c.getPlayer();
        switch (splitted[0]) {
            case "-warp":
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                if ((victim != null)) {
                    if (splitted.length == 2) {
                        player.changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getPosition()));
                    } else {
                        GameMap target = mapFactory.getMap(Integer.parseInt(splitted[2]));
                        victim.changeMap(target, target.getPortal(0));
                    }
                } else {
                    try {
                        victim = player;
                        GameMap target = cserv.getMapFactory(c.getWorldId()).getMap(Integer.parseInt(splitted[1]));
                        if (c.getChannelServer().getWorldInterface().getLocation(splitted[1]) ==
                                null) {
                            player.changeMap(target, target.getPortal(0));
                        } else {
                            player.sendNotice(6, "Please make sure that you are on the same channel as the user.");
                        }
                    } catch (NumberFormatException | RemoteException e) {
                        player.sendNotice(6, "Something went wrong " +
                                e.getMessage());
                    }
                }
                break;
            case "-worldtrip":
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                for (int i = 1; i <= 10; i++) {
                    GameMap target = mapFactory.getMap(200000000);
                    Portal targetPortal = target.getPortal(0);
                    victim.changeMap(target, targetPortal);
                    GameMap target1 = mapFactory.getMap(102000000);
                    Portal targetPortal1 = target.getPortal(0);
                    victim.changeMap(target1, targetPortal1);
                    GameMap target2 = mapFactory.getMap(103000000);
                    Portal targetPortal2 = target.getPortal(0);
                    victim.changeMap(target2, targetPortal2);
                    GameMap target3 = mapFactory.getMap(100000000);
                    Portal targetPortal3 = target.getPortal(0);
                    victim.changeMap(target3, targetPortal3);
                    GameMap target4 = mapFactory.getMap(200000000);
                    Portal targetPortal4 = target.getPortal(0);
                    victim.changeMap(target4, targetPortal4);
                    GameMap target5 = mapFactory.getMap(211000000);
                    Portal targetPortal5 = target.getPortal(0);
                    victim.changeMap(target5, targetPortal5);
                    GameMap target6 = mapFactory.getMap(230000000);
                    Portal targetPortal6 = target.getPortal(0);
                    victim.changeMap(target6, targetPortal6);
                    GameMap target7 = mapFactory.getMap(222000000);
                    Portal targetPortal7 = target.getPortal(0);
                    victim.changeMap(target7, targetPortal7);
                    GameMap target8 = mapFactory.getMap(251000000);
                    Portal targetPortal8 = target.getPortal(0);
                    victim.changeMap(target8, targetPortal8);
                    GameMap target9 = mapFactory.getMap(220000000);
                    Portal targetPortal9 = target.getPortal(0);
                    victim.changeMap(target9, targetPortal9);
                    GameMap target10 = mapFactory.getMap(221000000);
                    Portal targetPortal10 = target.getPortal(0);
                    victim.changeMap(target10, targetPortal10);
                    GameMap target11 = mapFactory.getMap(240000000);
                    Portal targetPortal11 = target.getPortal(0);
                    victim.changeMap(target11, targetPortal11);
                    GameMap target12 = mapFactory.getMap(600000000);
                    Portal targetPortal12 = target.getPortal(0);
                    victim.changeMap(target12, targetPortal12);
                    GameMap target13 = mapFactory.getMap(800000000);
                    Portal targetPortal13 = target.getPortal(0);
                    victim.changeMap(target13, targetPortal13);
                    GameMap target14 = mapFactory.getMap(680000000);
                    Portal targetPortal14 = target.getPortal(0);
                    victim.changeMap(target14, targetPortal14);
                    GameMap target15 = mapFactory.getMap(105040300);
                    Portal targetPortal15 = target.getPortal(0);
                    victim.changeMap(target15, targetPortal15);
                    GameMap target16 = mapFactory.getMap(990000000);
                    Portal targetPortal16 = target.getPortal(0);
                    victim.changeMap(target16, targetPortal16);
                    GameMap target17 = mapFactory.getMap(100000001);
                    Portal targetPortal17 = target.getPortal(0);
                    victim.changeMap(target17, targetPortal17);
                }
                victim.changeMap(player.getMap(), player.getMap().findClosestSpawnpoint(
                        player.getPosition()));
                break;
            case "-warphere":
			victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                victim.changeMap(player.getMap(), player.getMap().findClosestSpawnpoint(player.getPosition()));
                break;
            case "-slime":
                Monster mob0 = LifeFactory.getMonster(9400202);
                player.getMap().spawnMonsterOnGroundBelow(mob0, player.getPosition());
                player.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "[Event] EXP slimes!"));
                break;
            case "-lolcastle":
                {
                    if (splitted.length != 2) {
                        player.sendNotice(6, "Syntax: !lolcastle level (level = 1-5)");
                    }
                    GameMap target = c.getChannelServer().getEventSM(c.getWorldId()).getEventManager("lolcastle").getInstance("lolcastle" +
                            splitted[1]).getMapFactory().getMap(990000300, false, false);
                    player.changeMap(target, target.getPortal(0));
                    break;
                }
            case "-map":
                {
                    GameMap target = cserv.getMapFactory(c.getWorldId()).getMap(Integer.parseInt(splitted[1]));
                    Portal targetPortal = null;
                    if (splitted.length > 2) {
                        try {
                            targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                        } catch (IndexOutOfBoundsException e) {
                            player.sendNotice(5, "Invalid portal selected.");
                        } catch (NumberFormatException a) {
                            player.sendNotice(5, "Invalid map id.");
                        }
                    }
                    if (targetPortal == null) {
                        targetPortal = target.getPortal(0);
                    }
                    player.changeMap(target, targetPortal);
                    break;
                }
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("warp", "playername [targetid]", "Warps yourself to the player with the given name. When targetid is specified warps the player to the given mapid", 3),
            new CommandDefinition("warphere", "playername", "Warps the player with the given name to yourself", 3),
            new CommandDefinition("lolcastle", "[1-5]", "Warps you into Field of Judgement with the given level", 5),
            new CommandDefinition("map", "mapid", "Warps you to the given mapid (use /m instead)", 3),
            new CommandDefinition("worldtrip", "name", "Warps you to the given mapid (use /m instead)", 5),
            new CommandDefinition("slime", "monsterid", "summons nxx slime", 3)
        };
    }
}