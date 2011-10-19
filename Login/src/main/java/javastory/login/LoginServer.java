package javastory.login;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javastory.db.DatabaseConnection;
import javastory.server.handling.PacketHandler;
import javastory.rmi.LoginWorldInterface;
import javastory.world.core.WorldLoginInterface;
import javastory.server.GameService;
import javastory.server.ChannelInfo;
import javastory.server.LoginChannelInfo;
import javastory.server.TimerManager;

public class LoginServer extends GameService {

    // TODO: This should be a per-channel constant.
    public static final int USER_LIMIT = 1200;
    private LoginWorldInterface lwi;
    private WorldLoginInterface wli;
    private final Properties subnetInfo = new Properties();
    private final Map<Integer, LoginChannelInfo> channels =
            Maps.newHashMap();
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

    public final String getChannelServerIP(final int channelId) {
        return channels.get(channelId).getHost();
    }

    // TODO: Use proper IP + port instead.
    // This is only useful if the channel is on a well-known host (such as 127.0.0.1)
    public final int getChannelServerPort(final int channelId) {
        return channels.get(channelId).getPort();
    }
    //

    public final Map<Integer, LoginChannelInfo> getChannels() {
        return ImmutableMap.copyOf(this.channels);
    }

    public final void pingWorld() {
        try {
            wli.ping();
        } catch (RemoteException ex) {
            if (isWorldReady.compareAndSet(true, false)) {
                connectToWorld();
            }
        }
    }

    protected final void connectToWorld() {
        try {
            worldRegistry = super.getRegistry();

            lwi = new LoginWorldInterfaceImpl();
            wli = worldRegistry.registerLoginServer(lwi);
        } catch (NotBoundException ex) {
            throw new RuntimeException("[EXCEPTION] Attempting lookup or unbind in the registry a name that has no associated binding.");
        } catch (RemoteException ex) {
            throw new RuntimeException("[EXCEPTION] Could not connect to world server.");
        }
        isWorldReady.compareAndSet(false, true);
    }

    protected final void loadSettings() {
    }

    private void initialize() {
        connectToWorld();
        loadSettings();

        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0")) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
        }

        final PacketHandler packetHandler = new LoginPacketHandler();
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
        if (!isWorldReady.get()) {
            throw new IllegalStateException("The world server is not ready.");
        }
        return wli;
    }

    public static void start() {
        try {
            instance.initialize();
        } catch (Exception ex) {
            System.err.println("Error initializing loginserver : " + ex);
        }
    }

    public final Properties getSubnetInfo() {
        return subnetInfo;
    }

    public void setLoad(final int channelId, final int load) {
        LoginChannelInfo info = channels.get(channelId);
        if (info == null) {
            throw new IllegalArgumentException("'channelId' is not a valid channel ID");
        }
        info.setLoad(load);
    }
}