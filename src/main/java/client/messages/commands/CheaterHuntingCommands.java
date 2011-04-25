package client.messages.commands;

import java.rmi.RemoteException;
import java.util.List;

import client.GameCharacter;
import client.GameCharacterUtil;
import client.GameClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import handling.world.remote.CheaterData;

public class CheaterHuntingCommands implements Command {
	@Override
	public void execute(GameClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
		if (splitted[0].equals("-whosthere")) {
			StringBuilder builder = new StringBuilder("Players on Map: ");
			for (GameCharacter chr : c.getPlayer().getMap().getCharacters()) {
				if (builder.length() > 150) { // wild guess :o
					builder.setLength(builder.length() - 2);
					c.getPlayer().sendNotice(6, builder.toString());
					builder = new StringBuilder();
				}
				builder.append(GameCharacterUtil.makeMapleReadable(chr.getName()));
				builder.append(", ");
			}
			builder.setLength(builder.length() - 2);
			c.getPlayer().sendNotice(6, builder.toString());
		} else if (splitted[0].equals("-cheaters")) {
			try {
				List<CheaterData> cheaters = c.getChannelServer().getWorldInterface().getCheaters();
				for (int x = cheaters.size() - 1; x >= 0; x--) {
					CheaterData cheater = cheaters.get(x);
					c.getPlayer().sendNotice(6, cheater.getInfo());
				}
			} catch (RemoteException e) {
				c.getChannelServer().pingWorld();
			}
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("whosthere", "", "", 4),
			new CommandDefinition("cheaters", "", "", 4)
		};
	}
}