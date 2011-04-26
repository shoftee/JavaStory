package client.messages;

import client.ChannelClient;

public interface Command {
	CommandDefinition[] getDefinition();
	void execute (final ChannelClient c, final String []splittedLine) throws Exception, IllegalCommandSyntaxException;
}