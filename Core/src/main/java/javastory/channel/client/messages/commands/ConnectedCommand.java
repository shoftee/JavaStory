package javastory.channel.client.messages.commands;

import java.rmi.RemoteException;
import java.util.Map;

import javastory.channel.ChannelClient;
import javastory.channel.client.messages.Command;
import javastory.channel.client.messages.CommandDefinition;
import javastory.channel.client.messages.IllegalCommandSyntaxException;

public class ConnectedCommand implements Command {
	@Override
	public void execute(ChannelClient c, String[] splittedLine) throws Exception, IllegalCommandSyntaxException {
		try {
			Map<Integer, Integer> connected = c.getChannelServer().getWorldInterface().getConnected();
			StringBuilder conStr = new StringBuilder("Connected Clients: ");
			boolean first = true;
			for (int i : connected.keySet()) {
				if (!first) {
					conStr.append(", ");
				} else {
					first = false;
				}
				if (i == 0) {
					conStr.append("Total: ");
					conStr.append(connected.get(i));
				} else {
					conStr.append("Channel");
					conStr.append(i);
					conStr.append(": ");
					conStr.append(connected.get(i));
				}
			}
			c.getPlayer().sendNotice(6, conStr.toString());
		} catch (RemoteException e) {
			c.getChannelServer().pingWorld();
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("connected", "", "Shows how many players are connected on each channel", 3)
		};
	}
}