package client.messages.commands;

import client.GameClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.CommandProcessor;
import client.messages.IllegalCommandSyntaxException;
import org.javastory.server.channel.ShutdownChannelServer;

public class ShutdownCommands implements Command {

    @Override
    public void execute(GameClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        if (splitted[0].equals("-shutdown")) {
            int time = 60000;
            if (splitted.length > 1) {
                time = Integer.parseInt(splitted[1]) * 60000;
            }
            CommandProcessor.forcePersisting();
            c.getChannelServer().shutdown(time);
            c.getPlayer().sendNotice(6, "Shutting down... in " + time + "");
        } else if (splitted[0].equals("-shutdownworld")) {
            int time = 60000;
            if (splitted.length > 1) {
                time = Integer.parseInt(splitted[1]) * 60000;
            }
            CommandProcessor.forcePersisting();
            c.getChannelServer().shutdownWorld(time);
            c.getPlayer().sendNotice(6, "Shutting down... in 1 minutes");
        } else if (splitted[0].equals("-shutdownlogin")) {
            c.getChannelServer().shutdownLogin();
            c.getPlayer().sendNotice(6, "Shutting down...");
        } else if (splitted[0].equals("-shutdownnow")) {
            CommandProcessor.forcePersisting();
            new ShutdownChannelServer(c.getChannelServer().getChannelId()).run();
            c.getPlayer().sendNotice(6, "Shutting down...");
        } else if (splitted[0].equals("-shutdownmerchant")) {
            c.getChannelServer().closeAllMerchant();
            c.getPlayer().sendNotice(6, "All Merchant has been closed.");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("shutdownmerchant", "", "Shuts down all merchants in this channel", 5),
                    new CommandDefinition("shutdown", "[when in Minutes]", "Shuts down the current channel - don't use atm", 5),
                    new CommandDefinition("shutdownnow", "", "Shuts down the current channel now", 5),
                    new CommandDefinition("shutdownlogin", "", "Shuts down the current login - don't use atm", 5),
                    new CommandDefinition("shutdownworld", "[when in Minutes]", "Cleanly shuts down all channels and the loginserver of this world", 5)
                };
    }
}