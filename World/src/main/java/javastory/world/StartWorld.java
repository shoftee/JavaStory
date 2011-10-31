package javastory.world;

/**
 * Hello world!
 *
 */
public class StartWorld {

    public static void main(final String[] args) {
        System.setProperty("org.javastory.world.ip", "127.0.0.1");
        System.setProperty("org.javastory.wzpath", "xml");
        
        WorldServer.startWorld_Main();

    }
}
