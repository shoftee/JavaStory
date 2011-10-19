package javastory.channel.client.messages.commands;

import static javastory.channel.client.messages.CommandProcessor.getOptionalIntArg;
import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;
import javastory.tools.StringUtil;
import javastory.tools.packets.ChannelPackets;
import javastory.tools.packets.TestPacket;

public class TestCommands implements Command {

    @Override
    public void execute(final ChannelClient c, final String[] splitted) throws Exception, IllegalCommandSyntaxException {
        final ChannelCharacter player = c.getPlayer();
        switch (splitted[0]) {
            case "-test1":
                c.write(TestPacket.EXPTest1());
                break;
            case "-test2":
                c.write(TestPacket.EXPTest2());
                break;
            case "-clock":
                player.getMap().broadcastMessage(ChannelPackets.getClock(getOptionalIntArg(splitted, 1, 60)));
                break;
            case "-packet":
                if (splitted.length > 1) {
                    c.write(ChannelPackets.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
                } else {
                    player.sendNotice(6, "Please enter packet data!");
                }
                break;
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