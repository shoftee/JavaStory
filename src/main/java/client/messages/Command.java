package client.messages;

import org.javastory.client.ChannelClient;

public interface Command {
	CommandDefinition[] getDefinition();
	void execute (final ChannelClient c, final String []splittedLine) throws Exception, IllegalCommandSyntaxException;
}