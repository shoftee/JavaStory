package org.javastory.server.channel;

import java.rmi.RemoteException;
import java.sql.SQLException;

import database.DatabaseConnection;
import handling.world.remote.ServerStatus;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.javastory.server.ChannelInfo;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import server.TimerManager;

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
            ChannelServer.getWorldRegistry().deregisterChannelServer(channelId);
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