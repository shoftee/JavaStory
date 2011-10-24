package javastory.channel;

import javastory.config.ChannelConfig;
import javastory.config.ChannelInfo;

/**
 * Hello world!
 * 
 */
public class StartChannel {

	public static void main(String[] args) {
		System.setProperty("org.javastory.world.ip", "127.0.0.1");
		System.setProperty("org.javastory.wzpath", "xml");

		if (args.length != 2) {
			showUsageInfo();
			return;
		}

		try {
			int worldId = Integer.parseInt(args[0]);
			int channelId = Integer.parseInt(args[1]);
			final ChannelInfo info = ChannelConfig.load(worldId, channelId);
			ChannelServer.initialize(info);
		} catch (NumberFormatException ex) {
			showUsageInfo();
		}
	}

	private static void showUsageInfo() {
		System.out.println("Usage: <StartChannel> [world id] [channel id]");
		System.out.println("       [world id]   - non-negative integer, the ID of the world to active a channel in.");
		System.out.println("       [channel id] - non-negative integer, the ID of the channel to activate.");
	}
}
