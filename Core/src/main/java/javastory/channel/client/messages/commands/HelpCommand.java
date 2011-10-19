package javastory.channel.client.messages.commands;

import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.CommandProcessor;
import javastory.channel.client.messages.IllegalCommandSyntaxException;

public class HelpCommand implements Command {

	@Override
	public void execute(ChannelClient c, String[] splittedLine)
			throws Exception, IllegalCommandSyntaxException {
		CommandProcessor.getInstance()
				.dropHelp(	c.getPlayer(),
							CommandProcessor.getOptionalIntArg(	splittedLine,
																1,
																1));
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("help", "[page - defaults to 1]",
									"Shows the help", 1)
		};
	}
}