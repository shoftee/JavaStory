package server;

import org.javastory.server.login.LoginServer;
import handling.world.WorldServer;
import java.util.Scanner;
import org.javastory.server.channel.ChannelManager;

public class Start {

    public static void main(final String args[]) {
        System.out.println("Enter mode: ");
        String serverType;
        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            serverType = scanner.next();
        } else {
            serverType = args[0];
        }
        
        System.setProperty("org.javastory.world.ip", "127.0.0.1");
        System.setProperty("org.javastory.wzpath", "xml");
        switch (serverType) {
            case "WORLD":
                WorldServer.startWorld_Main();
                break;
            case "CHANNEL":
                System.setProperty("org.javastory.channels.config",
                                   "channels.properties");
                ChannelManager.startChannels();
                break;
            case "LOGIN":
                System.setProperty("org.javastory.login.config",
                                   "login.properties");
                LoginServer.startLogin_Main();
                break;
            default:
                System.out.println("Invalid input for selected servers: 'CHANNEL', 'LOGIN' and 'WORLD'.");
                break;
        }
    }
}
