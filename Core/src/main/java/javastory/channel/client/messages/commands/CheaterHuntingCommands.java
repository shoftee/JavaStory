package javastory.channel.client.messages.commands;

import java.rmi.RemoteException;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.world.core.CheaterData;

public class CheaterHuntingCommands implements Command {

	@Override
	public void execute(ChannelClient c, String[] splitted) throws Exception,
			IllegalCommandSyntaxException {
		final ChannelCharacter player = c.getPlayer();
		if (splitted[0].equals("-whosthere")) {
			StringBuilder builder = new StringBuilder("Players on Map: ");
			for (ChannelCharacter chr : player.getMap().getCharacters()) {
				if (builder.length() > 150) { // wild guess :o
					builder.setLength(builder.length() - 2);
					player.sendNotice(6, builder.toString());
					builder = new StringBuilder();
				}
				builder.append(chr.getName().toUpperCase());
				builder.append(", ");
			}
			builder.setLength(builder.length() - 2);
			player.sendNotice(6, builder.toString());
		} else if (splitted[0].equals("-cheaters")) {
			try {
				List<CheaterData> cheaters = c.getChannelServer()
						.getWorldInterface().getCheaters();
				for (int x = cheaters.size() - 1; x >= 0; x--) {
					CheaterData cheater = cheaters.get(x);
					player.sendNotice(6, cheater.getInfo());
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