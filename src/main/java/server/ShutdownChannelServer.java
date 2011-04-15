package server;

import java.rmi.RemoteException;
import java.sql.SQLException;

import database.DatabaseConnection;
import handling.world.remote.ServerStatus;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.javastory.server.ChannelInfo;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;

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

        while (ChannelManager.getInstance(channelId).getPlayerStorage().getConnectedClients() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("ERROR " + e);
            }
        }

        System.out.println("Channel " + channelId + ", Deregistering channel");
        try {
            ChannelServer.getWorldRegistry().deregisterChannelServer(channelId);
        } catch (RemoteException ex) {
            System.err.println("ERROR on deregistering channel server: " + ex);
        }

        System.out.println("Channel " + channelId + ", Unbinding ports...");

        boolean error = true;
        while (error) {
            try {
                ChannelManager.getInstance(channelId).unbind();
                error = false;
            } catch (Exception e) {
                error = true;
            }
        }

        System.out.println("Channel " + channelId + ", closing...");

        for (ChannelServer cserv : ChannelManager.getAllInstances()) {
            while (cserv.getStatus() != ServerStatus.OFFLINE) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.err.println("ERROR" + e);
                }
            }
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