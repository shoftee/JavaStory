package handling.world;

import com.google.common.collect.ImmutableSet;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import database.DatabaseConnection;
import handling.channel.remote.ChannelWorldInterface;
import handling.login.remote.LoginWorldInterface;
import handling.world.remote.ServerStatus;
import handling.world.remote.WorldChannelInterface;
import handling.world.remote.WorldLoginInterface;
import handling.world.remote.WorldRegistry;
import org.javastory.client.MemberRank;
import org.javastory.server.ChannelInfo;

class WorldRegistryImpl extends UnicastRemoteObject implements WorldRegistry {

    private static final long serialVersionUID = -5170574938159280746L;
    private static WorldRegistryImpl instance = null;
    private ServerStatus csStatus, loginStatus;
    //
    private final List<LoginWorldInterface> logins = new LinkedList<>();
    //
    private Map<Integer, ServerStatus> channelStatus;
    private final Map<Integer, ChannelWorldInterface> channels = new LinkedHashMap<>();
    //
    private final AtomicInteger runningMessengerId = new AtomicInteger();
    private final Map<Integer, Messenger> messengers = new HashMap<>();
    //
    private final AtomicInteger runningPartyId = new AtomicInteger();
    private final Map<Integer, Party> parties = new HashMap<>();
    //
    private final Map<Integer, Guild> guilds = new LinkedHashMap<>();
    private final PlayerBuffStorage buffStorage = new PlayerBuffStorage();
    private final Lock guildMutex = new ReentrantLock();

    private WorldRegistryImpl() throws RemoteException {
        super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
        DatabaseConnection.initialize();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT MAX(party)+1 FROM characters");
            ResultSet rs = ps.executeQuery();
            rs.next();
            runningPartyId.set(rs.getInt(1));
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        csStatus = ServerStatus.OFFLINE;
        loginStatus = ServerStatus.OFFLINE;

        runningMessengerId.set(1);
    }

    public static WorldRegistryImpl getInstance() {
        if (instance == null) {
            try {
                instance = new WorldRegistryImpl();
            } catch (RemoteException e) {
                // can't do much anyway we are fucked ^^
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public ServerStatus getCashShopStatus() {
        return csStatus;
    }

    public ServerStatus getLoginStatus() {
        return loginStatus;
    }

    public ServerStatus getChannelStatus(int channelId) {
        ServerStatus status = channelStatus.get(channelId);
        if (status == null) {
            return ServerStatus.OFFLINE;
        }
        return status;
    }

    public WorldChannelInterface registerChannelServer(ChannelInfo info,
            final ChannelWorldInterface channel) throws RemoteException {
        int id = info.getId();
        ServerStatus status = channelStatus.get(id);
        if (status != ServerStatus.OFFLINE) {
            throw new IllegalStateException(
                    "The given channel info slot is already active.");
        }
        channels.put(id, channel);
        WorldChannelInterface ret = new WorldChannelInterfaceImpl(channel, id);
        return ret;
    }

    public void deregisterChannelServer(int channelId) throws RemoteException {
        ServerStatus status = channelStatus.get(channelId);
        if (status != ServerStatus.OFFLINE) {
            throw new IllegalStateException(
                    "The given channel info slot is currently active.");
        }

        channels.remove(channelId);
        channelStatus.put(channelId, ServerStatus.OFFLINE);

        for (final LoginWorldInterface wli : logins) {
            wli.channelOffline(channelId);
        }
        System.out.println("Channel " + channelId + " is offline.");
    }

    public WorldLoginInterface registerLoginServer(
            final LoginWorldInterface login) throws RemoteException {
        if (loginStatus != ServerStatus.OFFLINE) {
            throw new IllegalStateException(
                    "The login server is already active.");
        }
        WorldLoginInterface ret = new WorldLoginInterfaceImpl();

        logins.add(login);
        for (ChannelWorldInterface cwi : channels.values()) {
            login.channelOnline(cwi.getChannelInfo());
        }

        return ret;
    }

    public void deregisterLoginServer(LoginWorldInterface cb) throws RemoteException {
        if (loginStatus != ServerStatus.OFFLINE) {
            throw new IllegalStateException(
                    "The login server is currently active.");
        }
        logins.remove(cb);
    }

    public List<LoginWorldInterface> getLoginServer() {
        return new LinkedList<>(logins);
    }

    public ChannelWorldInterface getChannel(final int channel) {
        return channels.get(channel);
    }

    public ImmutableSet<Integer> getActiveChannels() {
        return ImmutableSet.copyOf(this.channels.keySet());
    }

    public Collection<ChannelWorldInterface> getAllChannelServers() {
        return channels.values();
    }

    public int getHighestChannelId() {
        int highest = 0;
        for (final Integer channel : channels.keySet()) {
            if (channel != null && channel.intValue() > highest) {
                highest = channel.intValue();
            }
        }
        return highest;
    }

    public Party createParty(final PartyMember chrfor) {
        final int partyid = runningPartyId.getAndIncrement();
        final Party party = new Party(partyid, chrfor);
        parties.put(party.getId(), party);
        return party;
    }

    public Party getParty(final int partyid) {
        return parties.get(partyid);
    }

    public Party disbandParty(final int partyid) {
        return parties.remove(partyid);
    }

    public final String getStatus() throws RemoteException {
        StringBuilder ret = new StringBuilder();
        List<Entry<Integer, ChannelWorldInterface>> channelServers = new ArrayList<>(channels.entrySet());
        int totalUsers = 0;
        for (final Entry<Integer, ChannelWorldInterface> cs : channelServers) {
            ret.append("Channel ");
            ret.append(cs.getKey());
            try {
                cs.getValue().isAvailable();
                ret.append(": online, ");
                int channelUsers = cs.getValue().getConnected();
                totalUsers += channelUsers;
                ret.append(channelUsers);
                ret.append(" users\n");
            } catch (RemoteException e) {
                ret.append(": offline\n");
            }
        }
        ret.append("Total users online: ");
        ret.append(totalUsers);
        ret.append("\n");
        //	Properties props = new Properties(WorldServer.getInstance().getWorldProperties());
        for (LoginWorldInterface lwi : logins) {
            ret.append("Login: ");
            try {
                lwi.isAvailable();
                ret.append("online\n");
            } catch (RemoteException e) {
                ret.append("offline\n");
            }
        }
        return ret.toString();
    }

    public final int createGuild(final int leaderId, final String name) {
        return Guild.createGuild(leaderId, name);
    }

    public final Guild getGuild(final int id, final GuildMember mgc) {
        guildMutex.lock();
        try {
            if (guilds.get(id) != null) {
                return guilds.get(id);
            }
            if (mgc == null) {
                return null;
            }
            final Guild g = new Guild(mgc);
            if (g.getId() == -1) { //failed to load
                return null;
            }
            guilds.put(id, g);
            return g;
        } finally {
            guildMutex.unlock();
        }
    }

    public void setGuildMemberOnline(final GuildMember mgc, final boolean bOnline, final int channel) {
        getGuild(mgc.getGuildId(), mgc).setOnline(mgc.getId(), bOnline, channel);
    }

    public final boolean addGuildMember(final GuildMember mgc) {
        final Guild g = guilds.get(mgc.getGuildId());
        return g != null && g.addGuildMember(mgc);
    }

    public void leaveGuild(final GuildMember mgc) {
        final Guild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.leaveGuild(mgc);
        }
    }

    public void allianceChat(final int gid, final String name, final int cid, final String msg) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            g.allianceChat(name, cid, msg);
        }
    }

    public void guildChat(final int gid, final String name, final int cid, final String msg) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            g.guildChat(name, cid, msg);
        }
    }

    public void changeRank(final int gid, final int cid, final MemberRank newRank) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            g.changeRank(cid, newRank);
        }
    }

    public void expelMember(final GuildMember initiator, final String name, final int cid) {
        final Guild g = guilds.get(initiator.getGuildId());
        if (g != null) {
            g.expelMember(initiator, name, cid);
        }
    }

    public void setGuildNotice(final int gid, final String notice) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            g.setGuildNotice(notice);
        }
    }

    public void memberLevelJobUpdate(final GuildMember mgc) {
        final Guild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.memberLevelJobUpdate(mgc);
        }
    }

    public void changeRankTitle(final int gid, final String[] ranks) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            g.changeRankTitle(ranks);
        }
    }

    public void setGuildEmblem(final int gid, final short bg, final byte bgcolor, final short logo, final byte logocolor) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            g.setGuildEmblem(bg, bgcolor, logo, logocolor);
        }
    }

    public void disbandGuild(final int gid) {
        guildMutex.lock();
        try {
            guilds.get(gid).disbandGuild();
            guilds.remove(gid);
        } finally {
            guildMutex.unlock();
        }
    }

    public final boolean increaseGuildCapacity(final int gid) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            return g.increaseCapacity();
        }
        return false;
    }

    public void gainGP(final int gid, final int amount) {
        final Guild g = guilds.get(gid);
        if (g != null) {
            g.gainGP(amount);
        }
    }

    public final Messenger createMessenger(final MessengerMember chrfor) {
        final int messengerid = runningMessengerId.getAndIncrement();
        final Messenger messenger = new Messenger(messengerid, chrfor);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    public final Messenger getMessenger(final int messengerid) {
        return messengers.get(messengerid);
    }

    public final PlayerBuffStorage getPlayerBuffStorage() {
        return buffStorage;
    }
}