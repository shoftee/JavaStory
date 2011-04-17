package client.messages.commands;

import client.GameCharacter;
import client.GameClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import server.Portal;
import server.maps.GameMap;
import server.life.Monster;
import server.life.LifeFactory;
import server.life.Monster;
import server.maps.GameMapFactory;
import tools.MaplePacketCreator;

public class WarpCommands implements Command {
	private GameCharacter victim;
	@Override
	public void execute(GameClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
		ChannelServer cserv = c.getChannelServer();
		if (splitted[0].equals("-warp")) {
			victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
			if ((victim != null)) {
				if (splitted.length == 2) {
					c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getPosition()));
				} else {
					GameMap target = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(Integer.parseInt(splitted[2]));
					victim.changeMap(target, target.getPortal(0));
				}
			} else {
				try {
					victim = c.getPlayer();
					int victimw = cserv.getPlayerStorage().getCharacterByName(splitted[1]).getWorld();
					GameMap target = cserv.getMapFactory(c.getPlayer().getWorld()).getMap(Integer.parseInt(splitted[1]));
					if (c.getChannelServer().getWorldInterface().getLocation(splitted[1]) == null) {
						c.getPlayer().changeMap(target, target.getPortal(0));
					} else {
						c.getPlayer().dropMessage(6, "Please make sure that you are on the same channel as the user.");
					}
				} catch (Exception e) {
					c.getPlayer().dropMessage(6, "Something went wrong " + e.getMessage());
				}
			}
		} else if (splitted[0].equals("-worldtrip")) {
			victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
			for (int i = 1; i <= 10; i++) {
				GameMap target = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(200000000);
				Portal targetPortal = target.getPortal(0);
				victim.changeMap(target, targetPortal);
				GameMap target1 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(102000000);
				Portal targetPortal1 = target.getPortal(0);
				victim.changeMap(target1, targetPortal1);
				GameMap target2 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(103000000);
				Portal targetPortal2 = target.getPortal(0);
				victim.changeMap(target2, targetPortal2);
				GameMap target3 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(100000000);
				Portal targetPortal3 = target.getPortal(0);
				victim.changeMap(target3, targetPortal3);
				GameMap target4 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(200000000);
				Portal targetPortal4 = target.getPortal(0);
				victim.changeMap(target4, targetPortal4);
				GameMap target5 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(211000000);
				Portal targetPortal5 = target.getPortal(0);
				victim.changeMap(target5, targetPortal5);
				GameMap target6 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(230000000);
				Portal targetPortal6 = target.getPortal(0);
				victim.changeMap(target6, targetPortal6);
				GameMap target7 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(222000000);
				Portal targetPortal7 = target.getPortal(0);
				victim.changeMap(target7, targetPortal7);
				GameMap target8 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(251000000);
				Portal targetPortal8 = target.getPortal(0);
				victim.changeMap(target8, targetPortal8);
				GameMap target9 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(220000000);
				Portal targetPortal9 = target.getPortal(0);
				victim.changeMap(target9, targetPortal9);
				GameMap target10 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(221000000);
				Portal targetPortal10 = target.getPortal(0);
				victim.changeMap(target10, targetPortal10);
				GameMap target11 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(240000000);
				Portal targetPortal11 = target.getPortal(0);
				victim.changeMap(target11, targetPortal11);
				GameMap target12 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(600000000);
				Portal targetPortal12 = target.getPortal(0);
				victim.changeMap(target12, targetPortal12);
				GameMap target13 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(800000000);
				Portal targetPortal13 = target.getPortal(0);
				victim.changeMap(target13, targetPortal13);
				GameMap target14 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(680000000);
				Portal targetPortal14 = target.getPortal(0);
				victim.changeMap(target14, targetPortal14);
				GameMap target15 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(105040300);
				Portal targetPortal15 = target.getPortal(0);
				victim.changeMap(target15, targetPortal15);
				GameMap target16 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(990000000);
				Portal targetPortal16 = target.getPortal(0);
				victim.changeMap(target16, targetPortal16);
				GameMap target17 = ChannelManager.getInstance(c.getChannelId()).getMapFactory(c.getPlayer().getWorld()).getMap(100000001);
				Portal targetPortal17 = target.getPortal(0);
				victim.changeMap(target17, targetPortal17);
			}
			victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(
			c.getPlayer().getPosition()));
		} else if (splitted[0].equals("-warphere")) {
			int victimw = cserv.getPlayerStorage().getCharacterByName(splitted[1]).getWorld();
			victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
			victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
		} else if (splitted[0].equals("-slime")) {
			Monster mob0 = LifeFactory.getMonster(9400202);
			c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob0, c.getPlayer().getPosition());
			c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(0, "[Event] EXP slimes!"));
		} else if (splitted[0].equals("-lolcastle")) {
			if (splitted.length != 2) {
				c.getPlayer().dropMessage(6, "Syntax: !lolcastle level (level = 1-5)");
			}
			GameMap target = c.getChannelServer().getEventSM(c.getPlayer().getWorld()).getEventManager("lolcastle").getInstance("lolcastle" + splitted[1]).getMapFactory().getMap(990000300, false, false);
			c.getPlayer().changeMap(target, target.getPortal(0));
		} else if (splitted[0].equals("-map")) {
			GameMap target = cserv.getMapFactory(c.getPlayer().getWorld()).getMap(Integer.parseInt(splitted[1]));
			Portal targetPortal = null;
			if (splitted.length > 2) {
				try {
					targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
				} catch (IndexOutOfBoundsException e) {
					c.getPlayer().dropMessage(5, "Invalid portal selected.");
				} catch (NumberFormatException a) {
					c.getPlayer().dropMessage(5, "Invalid map id.");
				}
			}
			if (targetPortal == null) {
				targetPortal = target.getPortal(0);
			}
			c.getPlayer().changeMap(target, targetPortal);
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("warp", "playername [targetid]", "Warps yourself to the player with the given name. When targetid is specified warps the player to the given mapid", 3),
			new CommandDefinition("warphere", "playername", "Warps the player with the given name to yourself", 3),
			new CommandDefinition("lolcastle", "[1-5]", "Warps you into Field of Judgement with the given level", 5),
			new CommandDefinition("map", "mapid", "Warps you to the given mapid (use /m instead)", 3),
			new CommandDefinition("worldtrip", "name", "Warps you to the given mapid (use /m instead)", 5),
			new CommandDefinition("slime", "monsterid", "summons nxx slime", 3)
		};
	}
}