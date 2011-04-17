package handling.world;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import handling.world.remote.ServerStatus;
import handling.world.remote.WorldChannelInterface;
import handling.world.remote.WorldLoginInterface;
import handling.world.remote.WorldRegistry;
import org.javastory.server.ChannelInfo;

public class WorldRegistryImpl extends UnicastRemoteObject implements WorldRegistry {

    private static final long serialVersionUID = -5170574938159280746L;
    private static WorldRegistryImpl instance = null;
    private ServerStatus csStatus, loginStatus;
    private Map<Integer, ServerStatus> channelStatus;
    private final Map<Integer, ChannelWorldInterface> channels = new LinkedHashMap<Integer, ChannelWorldInterface>();
    private final List<LoginWorldInterface> logins = new LinkedList<LoginWorldInterface>();
    private final Map<Integer, MapleParty> parties = new HashMap<Integer, MapleParty>();
    private final AtomicInteger runningPartyId = new AtomicInteger();
    private final Map<Integer, MapleMessenger> messengers = new HashMap<Integer, MapleMessenger>();
    private final AtomicInteger runningMessengerId = new AtomicInteger();
    private final Map<Integer, MapleGuild> guilds = new LinkedHashMap<Integer, MapleGuild>();
    private final PlayerBuffStorage buffStorage = new PlayerBuffStorage();
    private final Lock Guild_Mutex = new ReentrantLock();

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
            // TODO Auto-generated catch block
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
        if (status == null) return ServerStatus.OFFLINE;
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
        return new LinkedList<LoginWorldInterface>(logins);
    }

    public ChannelWorldInterface getChannel(final int channel) {
        return channels.get(channel);
    }

    public Set<Integer> getChannelServer() {
        return new HashSet<Integer>(channels.keySet());
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

    public MapleParty createParty(final MaplePartyCharacter chrfor) {
        final int partyid = runningPartyId.getAndIncrement();
        final MapleParty party = new MapleParty(partyid, chrfor);
        parties.put(party.getId(), party);
        return party;
    }

    public MapleParty getParty(final int partyid) {
        return parties.get(partyid);
    }

    public MapleParty disbandParty(final int partyid) {
        return parties.remove(partyid);
    }

    public final String getStatus() throws RemoteException {
        StringBuilder ret = new StringBuilder();
        List<Entry<Integer, ChannelWorldInterface>> channelServers = new ArrayList<Entry<Integer, ChannelWorldInterface>>(channels.entrySet());
        Collections.sort(channelServers, new Comparator<Entry<Integer, ChannelWorldInterface>>() {

            @Override
            public int compare(Entry<Integer, ChannelWorldInterface> o1, Entry<Integer, ChannelWorldInterface> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
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
        return MapleGuild.createGuild(leaderId, name);
    }

    public final MapleGuild getGuild(final int id, final MapleGuildCharacter mgc) {
        Guild_Mutex.lock();
        try {
            if (guilds.get(id) != null) {
                return guilds.get(id);
            }
            if (mgc == null) {
                return null;
            }
            final MapleGuild g = new MapleGuild(mgc);
            if (g.getId() == -1) { //failed to load
                return null;
            }
            guilds.put(id, g);
            return g;
        } finally {
            Guild_Mutex.unlock();
        }
    }

    public void setGuildMemberOnline(final MapleGuildCharacter mgc, final boolean bOnline, final int channel) {
        getGuild(mgc.getGuildId(), mgc).setOnline(mgc.getId(), bOnline, channel);
    }

    public final int addGuildMember(final MapleGuildCharacter mgc) {
        final MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            return g.addGuildMember(mgc);
        }
        return 0;
    }

    public void leaveGuild(final MapleGuildCharacter mgc) {
        final MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.leaveGuild(mgc);
        }
    }

    public void allianceChat(final int gid, final String name, final int cid, final String msg) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.allianceChat(name, cid, msg);
        }
    }

    public void guildChat(final int gid, final String name, final int cid, final String msg) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.guildChat(name, cid, msg);
        }
    }

    public void changeRank(final int gid, final int cid, final int newRank) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRank(cid, newRank);
        }
    }

    public void expelMember(final MapleGuildCharacter initiator, final String name, final int cid) {
        final MapleGuild g = guilds.get(initiator.getGuildId());
        if (g != null) {
            g.expelMember(initiator, name, cid);
        }
    }

    public void setGuildNotice(final int gid, final String notice) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildNotice(notice);
        }
    }

    public void memberLevelJobUpdate(final MapleGuildCharacter mgc) {
        final MapleGuild g = guilds.get(mgc.getGuildId());
        if (g != null) {
            g.memberLevelJobUpdate(mgc);
        }
    }

    public void changeRankTitle(final int gid, final String[] ranks) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.changeRankTitle(ranks);
        }
    }

    public void setGuildEmblem(final int gid, final short bg, final byte bgcolor, final short logo, final byte logocolor) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.setGuildEmblem(bg, bgcolor, logo, logocolor);
        }
    }

    public void disbandGuild(final int gid) {
        Guild_Mutex.lock();
        try {
            guilds.get(gid).disbandGuild();
            guilds.remove(gid);
        } finally {
            Guild_Mutex.unlock();
        }
    }

    public final boolean increaseGuildCapacity(final int gid) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            return g.increaseCapacity();
        }
        return false;
    }

    public void gainGP(final int gid, final int amount) {
        final MapleGuild g = guilds.get(gid);
        if (g != null) {
            g.gainGP(amount);
        }
    }

    public final MapleMessenger createMessenger(final MapleMessengerCharacter chrfor) {
        final int messengerid = runningMessengerId.getAndIncrement();
        final MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    public final MapleMessenger getMessenger(final int messengerid) {
        return messengers.get(messengerid);
    }

    public final PlayerBuffStorage getPlayerBuffStorage() {
        return buffStorage;
    }
}