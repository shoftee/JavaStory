package javastory.channel.client.messages;

import javastory.channel.ChannelClient;

public interface Command {
	CommandDefinition[] getDefinition();
	void execute (final ChannelClient c, final String []splittedLine) throws Exception, IllegalCommandSyntaxException;
}