package javastory.channel.client.messages.commands;

import java.util.Arrays;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.channel.life.Monster;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;

public class MonsterInfoCommands implements Command {

	@Override
	public void execute(ChannelClient c, String[] splitted) throws Exception,
			IllegalCommandSyntaxException {
		final ChannelCharacter player = c.getPlayer();
		GameMap map = player.getMap();
		double range = Double.POSITIVE_INFINITY;
		if (splitted.length > 1) {
			int irange = Integer.parseInt(splitted[1]);
			if (splitted.length <= 2) {
				range = irange * irange;
			} else {
				map = c.getChannelServer().getMapFactory(c.getWorldId())
						.getMap(Integer.parseInt(splitted[2]));
			}
		}
		switch (splitted[0]) {
		case "-killall": {
			Monster mob;
			for (GameMapObject monstermo : map.getMapObjectsInRange(player
					.getPosition(), range, Arrays
					.asList(GameMapObjectType.MONSTER))) {
				mob = (Monster) monstermo;
				map.killMonster(mob, player, false, false, (byte) 1);
			}
			break;
		}
		case "-killalldrops": {
			Monster mob;
			for (GameMapObject monstermo : map.getMapObjectsInRange(player
					.getPosition(), range, Arrays
					.asList(GameMapObjectType.MONSTER))) {
				mob = (Monster) monstermo;
				map.killMonster(mob, player, true, false, (byte) 1);
			}
			break;
		}
		case "-killallnospawn":
			map.killAllMonsters(false);
			break;
		case "-monsterdebug": {
			Monster mob;
			for (GameMapObject monstermo : map.getMapObjectsInRange(player
					.getPosition(), range, Arrays
					.asList(GameMapObjectType.MONSTER))) {
				mob = (Monster) monstermo;
				player.sendNotice(6, "Monster " + mob.toString());
			}
			break;
		}
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition(
									"killall",
									"[range] [mapid]",
									"Kills all monsters. When mapid specified, range ignored.",
									2),
			new CommandDefinition("killallnospawn", "[range] [mapid]",
									"Kills all monsters without respawning", 2),
			new CommandDefinition(
									"killalldrops",
									"[range] [mapid]",
									"Kills all monsters with drops. When mapid specified, range ignored.",
									5),
			new CommandDefinition("monsterdebug", "[range]", "", 2)
		};
	}
}