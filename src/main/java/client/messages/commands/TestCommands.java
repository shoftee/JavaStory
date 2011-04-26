package client.messages.commands;

import static client.messages.CommandProcessor.getOptionalIntArg;
import client.ChannelClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.packet.TestPacket;

public class TestCommands implements Command {
	@Override
	public void execute(final ChannelClient c, final String[] splitted) throws Exception, IllegalCommandSyntaxException {
		if (splitted[0].equals("-test1")) {
			c.write(TestPacket.EXPTest1());
		} else if (splitted[0].equals("-test2")) {
			c.write(TestPacket.EXPTest2());
		} else if (splitted[0].equals("-clock")) {
			c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(getOptionalIntArg(splitted, 1, 60)));
		} else if (splitted[0].equals("-packet")) {
			if (splitted.length > 1) {
				c.write(MaplePacketCreator.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
			} else {
				c.getPlayer().sendNotice(6, "Please enter packet data!");
			}
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[]{
			new CommandDefinition("test1", "?", "Probably does something", 5),
			new CommandDefinition("test2", "?", "Probably does something", 5),
			new CommandDefinition("clock", "[time]", "Shows a clock to everyone in the map", 5),
			new CommandDefinition("packet", "hex data", "Shows a clock to everyone in the map", 5)
		};
	}
}