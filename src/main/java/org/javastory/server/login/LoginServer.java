package org.javastory.server.login;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import database.DatabaseConnection;
import handling.ServerType;
import handling.login.LoginWorldInterfaceImpl;
import org.javastory.server.mina.PacketHandler;
import handling.login.remote.LoginWorldInterface;
import handling.world.remote.WorldLoginInterface;
import handling.world.remote.WorldRegistry;
import org.javastory.server.GameService;
import org.javastory.server.ChannelInfo;
import org.javastory.server.LoginChannelInfo;
import org.javastory.tools.PropertyUtil;
import server.TimerManager;

public class LoginServer extends GameService {

    private static WorldRegistry worldRegistry = null;
    private LoginWorldInterface lwi;
    private WorldLoginInterface wli;
    private Properties prop = new Properties();
    private Boolean worldReady = Boolean.TRUE;
    private final Properties subnetInfo = new Properties();
    private final Map<Integer, LoginChannelInfo> channels = 
            Maps.newHashMap();
    private String serverName, eventMessage;
    private byte flag;
    private int maxCharacters, userLimit;
    private static final LoginServer instance = new LoginServer();

    public static LoginServer getInstance() {
        return instance;
    }

    private LoginServer() {
        super(8484);
    }

    public final void addChannel(ChannelInfo info) {
        final int channelId = info.getId();
        channels.put(channelId, new LoginChannelInfo(info, 0));
    }

    public final void removeChannel(final int channelId) {
        channels.remove(channelId);
    }

    public final String getIP(final int channelId) {
        return channels.get(channelId).getHost();
    }
    
    public final Map<Integer, LoginChannelInfo> getChannels() {
        return ImmutableMap.copyOf(this.channels);
    }

    public final void pingWorld() {
        try {
            wli.isAvailable();
        } catch (RemoteException ex) {
            synchronized (worldReady) {
                worldReady = Boolean.FALSE;
            }
            synchronized (lwi) {
                synchronized (worldReady) {
                    if (worldReady) {
                        return;
                    }
                }
                System.out.println("Reconnecting to world server");
                synchronized (wli) {
                    reconnectWorld();
                }
            }
            synchronized (worldReady) {
                worldReady.notifyAll();
            }
        }
    }

    private void reconnectWorld() {
        try {
            worldRegistry = super.getRegistry();
            
            lwi = new LoginWorldInterfaceImpl();
            wli = worldRegistry.registerLoginServer(lwi);
            prop = wli.getWorldProperties();
            userLimit = Integer.parseInt(prop.getProperty("org.javastory.login.userlimit"));
            serverName = prop.getProperty("org.javastory.login.serverName");
            eventMessage = prop.getProperty("org.javastory.login.eventMessage");
            flag = Byte.parseByte(prop.getProperty("org.javastory.login.flag"));
            maxCharacters = Integer.parseInt(prop.getProperty("org.javastory.login.maxCharacters"));
        } catch (Exception e) {
            System.err.println("Reconnecting failed" + e);
        }
        worldReady = Boolean.TRUE;
    }

    private void initialize() {
        try {
            DatabaseConnection.initialize();
            
            final String config = 
                    System.getProperty("org.javastory.login.config", "login.properties");
            
            PropertyUtil.loadInto(config, settings);

            worldRegistry = super.getRegistry();
            
            lwi = new LoginWorldInterfaceImpl();
            wli = worldRegistry.registerLoginServer(lwi);
            
            prop = wli.getWorldProperties();
            userLimit = Integer.parseInt(prop.getProperty("org.javastory.login.userlimit"));
            serverName = prop.getProperty("org.javastory.login.serverName");
            eventMessage = prop.getProperty("org.javastory.login.eventMessage");
            flag = Byte.parseByte(prop.getProperty("org.javastory.login.flag"));
            maxCharacters = Integer.parseInt(prop.getProperty("org.javastory.login.maxCharacters"));
            try {
                final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0");
                ps.executeUpdate();
                ps.close();
            } catch (SQLException ex) {
                throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
            }
        } catch (RemoteException re) {
            re.printStackTrace();
            throw new RuntimeException("[EXCEPTION] Could not connect to world server.");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException("[EXCEPTION] Failed or interrupted I/O operations.");
        } catch (NotBoundException nbe) {
            throw new RuntimeException("[EXCEPTION] Attempting lookup or unbind in the registry a name that has no associated binding.");
        }
        final PacketHandler packetHandler = new PacketHandler(ServerType.LOGIN);
        super.bind(packetHandler);
    }

    public final void shutdown() {
        System.out.println("Shutting down...");
        try {
            worldRegistry.deregisterLoginServer(lwi);
        } catch (RemoteException e) {
            //
        }
        TimerManager.getInstance().stop();
        System.exit(0);
    }

    public final WorldLoginInterface getWorldInterface() {
        synchronized (worldReady) {
            while (!worldReady) {
                try {
                    worldReady.wait();
                } catch (InterruptedException e) {
                    //
                }
            }
        }
        return wli;
    }

    public static void startLogin_Main() {
        try {
            LoginServer ls = LoginServer.getInstance();
            ls.initialize();
        } catch (Exception ex) {
            System.err.println("Error initializing loginserver : " + ex);
        }
    }

    public final Properties getSubnetInfo() {
        return subnetInfo;
    }

    public final String getServerName() {
        return serverName;
    }

    public final String getEventMessage() {
        return eventMessage;
    }

    public final byte getFlag() {
        return flag;
    }

    public final int getMaxCharacters() {
        return maxCharacters;
    }
    
    public void setLoad(final int channelId, final int load) {
        LoginChannelInfo info = channels.get(channelId);
        if (info == null) {
            throw new IllegalArgumentException("'channelId' is not a valid channel ID");
        }
        info.setLoad(load);
    }

    public final void setEventMessage(final String newMessage) {
        this.eventMessage = newMessage;
    }

    public final void setFlag(final byte newflag) {
        flag = newflag;
    }

    public final int getUserLimit() {
        return userLimit;
    }

    public final void setUserLimit(final int newLimit) {
        userLimit = newLimit;
    }
}