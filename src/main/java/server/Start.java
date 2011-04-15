package server;

import org.javastory.server.login.LoginServer;
import handling.world.WorldServer;
import java.util.Scanner;
import org.javastory.server.cashshop.CashShopServer;
import org.javastory.server.channel.ChannelManager;

public class Start {

    public static void main(final String args[]) {
        String serverType;
        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            serverType = scanner.next();
        } else {
            serverType = args[0];
        }
        
        System.setProperty("org.javastory.world.ip", "127.0.0.1");
        System.setProperty("org.javastory.wzpath", "xml");
        
        if (serverType.equals("WORLD")) {
            WorldServer.startWorld_Main();
        } else if (serverType.equals("CASHSHOP")) {
            System.setProperty("org.javastory.cashshop.config",
                               "cashshop.properties");
            CashShopServer.startCashShop_main();
        } else if (serverType.equals("CHANNEL")) {
            System.setProperty("org.javastory.channels.config",
                               "channels.properties");
            ChannelManager.startChannels();
        } else if (serverType.equals("LOGIN")) {
            System.setProperty("org.javastory.login.config",
                               "login.properties");
            LoginServer.startLogin_Main();
        } else {
            System.out.println("Invalid input for selected servers: 'CASHSHOP', 'CHANNEL', 'LOGIN' and 'WORLD'.");
        }
    }
}
