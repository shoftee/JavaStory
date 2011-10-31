package javastory.login;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javastory.config.ChannelInfo;
import javastory.db.Database;
import javastory.rmi.LoginWorldInterface;
import javastory.rmi.WorldLoginInterface;
import javastory.server.GameService;
import javastory.server.TimerManager;
import javastory.server.handling.PacketHandler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

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

    public final void addChannel(final ChannelInfo info) {
        final int channelId = info.getId();
        this.channels.put(channelId, new LoginChannelInfo(info, 0));
    }

    public final void removeChannel(final int channelId) {
        this.channels.remove(channelId);
    }

    public final String getChannelServerIP(final int channelId) {
        return this.channels.get(channelId).getHost();
    }

    // TODO: Use proper IP + port instead.
    // This is only useful if the channel is on a well-known host (such as 127.0.0.1)
    public final int getChannelServerPort(final int channelId) {
        return this.channels.get(channelId).getPort();
    }
    //

    public final Map<Integer, LoginChannelInfo> getChannels() {
        return ImmutableMap.copyOf(this.channels);
    }

    public final void pingWorld() {
        try {
            this.wli.ping();
        } catch (final RemoteException ex) {
            if (this.isWorldReady.compareAndSet(true, false)) {
                this.connectToWorld();
            }
        }
    }

    @Override
	protected final void connectToWorld() {
        try {
            worldRegistry = super.getRegistry();

            this.lwi = new LoginWorldInterfaceImpl();
            this.wli = worldRegistry.registerLoginServer(this.lwi);
        } catch (final NotBoundException ex) {
            throw new RuntimeException("[EXCEPTION] Attempting lookup or unbind in the registry a name that has no associated binding.");
        } catch (final RemoteException ex) {
            throw new RuntimeException("[EXCEPTION] Could not connect to world server.");
        }
        this.isWorldReady.compareAndSet(false, true);
    }

    @Override
	protected final void loadSettings() {
    }

    private void initialize() {
        this.connectToWorld();
        this.loadSettings();

        try (PreparedStatement ps = Database.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0")) {
            ps.executeUpdate();
        } catch (final SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
        }

        final PacketHandler packetHandler = new LoginPacketHandler();
        super.bind(packetHandler);
    }

    @Override
	public final void shutdown() {
        System.out.println("Shutting down...");
        try {
            worldRegistry.deregisterLoginServer(this.lwi);
        } catch (final RemoteException e) {
            //
        }
        TimerManager.getInstance().stop();
        System.exit(0);
    }

    public final WorldLoginInterface getWorldInterface() {
        if (!this.isWorldReady.get()) {
            throw new IllegalStateException("The world server is not ready.");
        }
        return this.wli;
    }

    public static void start() {
        try {
            instance.initialize();
        } catch (final Exception ex) {
            System.err.println("Error initializing loginserver : " + ex);
        }
    }

    public final Properties getSubnetInfo() {
        return this.subnetInfo;
    }

    public void setLoad(final int channelId, final int load) {
        final LoginChannelInfo info = this.channels.get(channelId);
        if (info == null) {
            throw new IllegalArgumentException("'channelId' is not a valid channel ID");
        }
        info.setLoad(load);
    }
}