package javastory.channel.client.messages.commands;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.CommandProcessor;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.server.channel.ShutdownChannelServer;

public class ShutdownCommands implements Command {

	@Override
	public void execute(ChannelClient c, String[] splitted) throws Exception,
			IllegalCommandSyntaxException {
		final ChannelCharacter player = c.getPlayer();
		switch (splitted[0]) {
		case "-shutdown": {
			int time = 60000;
			if (splitted.length > 1) {
				time = Integer.parseInt(splitted[1]) * 60000;
			}
			CommandProcessor.forcePersisting();
			c.getChannelServer().shutdown(time);
			player.sendNotice(6, "Shutting down... in " + time + "");
			break;
		}
		case "-shutdownworld": {
			int time = 60000;
			if (splitted.length > 1) {
				time = Integer.parseInt(splitted[1]) * 60000;
			}
			CommandProcessor.forcePersisting();
			c.getChannelServer().shutdownWorld(time);
			player.sendNotice(6, "Shutting down... in 1 minutes");
			break;
		}
		case "-shutdownlogin":
			c.getChannelServer().shutdownLogin();
			player.sendNotice(6, "Shutting down...");
			break;
		case "-shutdownnow":
			CommandProcessor.forcePersisting();
			new ShutdownChannelServer(c.getChannelServer().getChannelId())
					.run();
			player.sendNotice(6, "Shutting down...");
			break;
		case "-shutdownmerchant":
			c.getChannelServer().closeAllMerchant();
			player.sendNotice(6, "All Merchant has been closed.");
			break;
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("shutdownmerchant", "",
									"Shuts down all merchants in this channel",
									5),
			new CommandDefinition(
									"shutdown",
									"[when in Minutes]",
									"Shuts down the current channel - don't use atm",
									5),
			new CommandDefinition("shutdownnow", "",
									"Shuts down the current channel now", 5),
			new CommandDefinition(
									"shutdownlogin",
									"",
									"Shuts down the current login - don't use atm",
									5),
			new CommandDefinition(
									"shutdownworld",
									"[when in Minutes]",
									"Cleanly shuts down all channels and the loginserver of this world",
									5)
		};
	}
}