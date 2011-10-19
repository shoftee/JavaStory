package javastory.channel;

/**
 * Hello world!
 *
 */
public class StartChannel {

    public static void main(String[] args) {
        System.setProperty("org.javastory.world.ip", "127.0.0.1");
        System.setProperty("org.javastory.wzpath", "xml");
        
        System.setProperty("org.javastory.channels.config",
                "channels.properties");
        ChannelManager.startChannels();
    }
}
