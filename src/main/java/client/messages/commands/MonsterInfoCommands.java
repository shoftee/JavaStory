package client.messages.commands;

import java.util.Arrays;

import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;

public class MonsterInfoCommands implements Command {
	@Override
	public void execute(MapleClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
		MapleMap map = c.getPlayer().getMap();
		double range = Double.POSITIVE_INFINITY;
		if (splitted.length > 1) {
			int irange = Integer.parseInt(splitted[1]);
			if (splitted.length <= 2) {
				range = irange * irange;
			} else {
				map = c.getChannelServer().getMapFactory(c.getPlayer().getWorld()).getMap(Integer.parseInt(splitted[2]));
			}
		}
		if (splitted[0].equals("-killall")) {
			MapleMonster mob;
			for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
				mob = (MapleMonster) monstermo;
				map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
			}
		} else if (splitted[0].equals("-killalldrops")) {
			MapleMonster mob;
			for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
				mob = (MapleMonster) monstermo;
				map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
			}
		} else if (splitted[0].equals("-killallnospawn")) {
			map.killAllMonsters(false);

		} else if (splitted[0].equals("-monsterdebug")) {
			MapleMonster mob;
			for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
				mob = (MapleMonster) monstermo;
				c.getPlayer().dropMessage(6, "Monster " + mob.toString());
			}
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[]{
			new CommandDefinition("killall", "[range] [mapid]", "Kills all monsters. When mapid specified, range ignored.", 2),
			new CommandDefinition("killallnospawn", "[range] [mapid]", "Kills all monsters without respawning", 2),
			new CommandDefinition("killalldrops", "[range] [mapid]", "Kills all monsters with drops. When mapid specified, range ignored.", 5),
			new CommandDefinition("monsterdebug", "[range]", "", 2)
		};
	}
}