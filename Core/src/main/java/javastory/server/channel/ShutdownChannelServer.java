package javastory.server.channel;

import java.rmi.RemoteException;
import java.sql.SQLException;

import javastory.channel.ChannelManager;
import javastory.db.DatabaseConnection;
import javastory.server.GameService;
import javastory.server.TimerManager;

public class ShutdownChannelServer implements Runnable {

    private int channelId;

    public ShutdownChannelServer(int id) {
        this.channelId = id;
    }

    @Override
    public void run() {
        try {
            ChannelManager.getInstance(channelId).shutdown();
        } catch (Throwable t) {
            System.err.println("SHUTDOWN ERROR " + t);
        }

        System.out.println("Channel " + channelId + ", Deregistering channel");
        try {
            GameService.getWorldRegistry().deregisterChannelServer(channelId);
        } catch (RemoteException ex) {
            System.err.println("ERROR on deregistering channel server: " + ex);
        }

        TimerManager.getInstance().stop();
        try {
            DatabaseConnection.closeAll();
        } catch (SQLException e) {
            System.err.println("THROW" + e);
        }
        System.exit(0);
    }
}