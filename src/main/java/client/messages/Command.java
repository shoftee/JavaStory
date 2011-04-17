package client.messages;

import client.GameClient;

public interface Command {
	CommandDefinition[] getDefinition();
	void execute (final GameClient c, final String []splittedLine) throws Exception, IllegalCommandSyntaxException;
}