package client.messages.commands;

import java.util.Arrays;

import client.GameClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import server.life.Monster;
import server.maps.GameMap;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;

public class MonsterInfoCommands implements Command {
	@Override
	public void execute(GameClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
		GameMap map = c.getPlayer().getMap();
		double range = Double.POSITIVE_INFINITY;
		if (splitted.length > 1) {
			int irange = Integer.parseInt(splitted[1]);
			if (splitted.length <= 2) {
				range = irange * irange;
			} else {
				map = c.getChannelServer().getMapFactory(c.getWorldId()).getMap(Integer.parseInt(splitted[2]));
			}
		}
		if (splitted[0].equals("-killall")) {
			Monster mob;
			for (GameMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(GameMapObjectType.MONSTER))) {
				mob = (Monster) monstermo;
				map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
			}
		} else if (splitted[0].equals("-killalldrops")) {
			Monster mob;
			for (GameMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(GameMapObjectType.MONSTER))) {
				mob = (Monster) monstermo;
				map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
			}
		} else if (splitted[0].equals("-killallnospawn")) {
			map.killAllMonsters(false);

		} else if (splitted[0].equals("-monsterdebug")) {
			Monster mob;
			for (GameMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(GameMapObjectType.MONSTER))) {
				mob = (Monster) monstermo;
				c.getPlayer().sendNotice(6, "Monster " + mob.toString());
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