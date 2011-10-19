package javastory.world;

import javastory.world.core.ServerStatus;
import javastory.world.core.WorldRegistry;
import javastory.world.core.WorldLoginInterface;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.db.DatabaseConnection;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.LoginWorldInterface;
import javastory.channel.Guild;
import javastory.channel.GuildMember;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.Party;
import javastory.channel.PlayerBuffStorage;
import javastory.rmi.WorldChannelInterface;
import javastory.client.MemberRank;
import javastory.rmi.GenericRemoteObject;
import javastory.server.ChannelInfo;

public class WorldRegistryImpl extends GenericRemoteObject implements
		WorldRegistry {

	private static final long serialVersionUID = -5170574938159280746L;
	private static WorldRegistryImpl instance = null;
	//
	private ServerStatus loginStatus;
	private final List<LoginWorldInterface> logins = new LinkedList<>();
	//
	private Map<Integer, ServerStatus> channelStatus;
	private final Map<Integer, ChannelWorldInterface> channels;
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
		super();

		DatabaseConnection.initialize();
		Connection con = DatabaseConnection.getConnection();
		try (PreparedStatement ps = con
				.prepareStatement("SELECT MAX(party)+1 FROM characters");
				ResultSet rs = ps.executeQuery()) {
			rs.next();
			runningPartyId.set(rs.getInt(1));
		} catch (SQLException ex) {
			System.err.println("Could not load max party ID: " + ex);
		}

		channelStatus = Maps.newLinkedHashMap();
		channels = Maps.newLinkedHashMap();

		loginStatus = ServerStatus.OFFLINE;

		runningMessengerId.set(1);
	}

	public static WorldRegistry getInstance() {
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

	private boolean isChannelActive(int channelId) {
		ServerStatus status = getChannelStatus(channelId);
		return !status.equals(ServerStatus.OFFLINE);
	}

	private boolean isLoginActive() {
		return !loginStatus.equals(ServerStatus.OFFLINE);
	}

	private ServerStatus getChannelStatus(int channelId) {
		ServerStatus status = channelStatus.get(channelId);
		if (status == null) {
			status = ServerStatus.OFFLINE;
			channelStatus.put(channelId, status);
		}
		return status;
	}

	public WorldChannelInterface registerChannelServer(ChannelInfo info,
			final ChannelWorldInterface channel) throws RemoteException {
		int id = info.getId();
		if (isChannelActive(id)) {
			throw new IllegalStateException(
					"The specified channel slot is already active.");
		}
		channels.put(id, channel);
		WorldChannelInterface ret = new WorldChannelInterfaceImpl(channel, id);
		return ret;
	}

	public void deregisterChannelServer(int channelId) throws RemoteException {
		if (!isChannelActive(channelId)) {
			throw new IllegalStateException(
					"The specified channel slot is not currently active.");
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
		if (isLoginActive()) {
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

	public void deregisterLoginServer(LoginWorldInterface cb)
			throws RemoteException {
		if (!isLoginActive()) {
			throw new IllegalStateException(
					"The login server is not currently active.");
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

	public Party createParty() {
		final int partyid = runningPartyId.getAndIncrement();
		final Party party = new Party(partyid);
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
		List<Entry<Integer, ChannelWorldInterface>> channelServers = new ArrayList<>(
				channels.entrySet());
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
		// Properties props = new
		// Properties(WorldServer.getInstance().getWorldProperties());
		for (LoginWorldInterface lwi : logins) {
			ret.append("Login: ");
			try {
				lwi.ping();
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

	public final Guild getGuild(final int guildId) {
		guildMutex.lock();
		try {
			return loadGuildIfAbsent(guildId);
		} finally {
			guildMutex.unlock();
		}
	}

	private Guild loadGuildIfAbsent(final int guildId) {
		Guild guild = guilds.get(guildId);
		if (guild == null) {
			guild = new Guild(guildId);
			if (guild.getId() == -1) {
				return null;
			}
			guilds.put(guildId, guild);
		}
		return guild;
	}

	public void setGuildMemberOnline(final GuildMember mgc,
			final boolean bOnline, final int channel) {
		getGuild(mgc.getGuildId()).setOnline(mgc.getCharacterId(), bOnline,
				channel);
	}

	public final boolean addGuildMember(final GuildMember mgc) {
		final Guild guild = guilds.get(mgc.getGuildId());
		return guild != null && guild.addGuildMember(mgc);
	}

	public void leaveGuild(final GuildMember mgc) {
		final Guild guild = guilds.get(mgc.getGuildId());
		if (guild != null) {
			guild.leaveGuild(mgc);
		}
	}

	public void guildChat(final int gid, final String name, final int cid,
			final String msg) throws RemoteException {
		final Guild guild = guilds.get(gid);
		if (guild != null) {
			guild.guildChat(name, cid, msg);
		}
	}

	public void changeRank(final int gid, final int cid,
			final MemberRank newRank) throws RemoteException {
		final Guild guild = guilds.get(gid);
		if (guild != null) {
			guild.changeRank(cid, newRank);
		}
	}

	public void expelMember(final GuildMember initiator, final int cid)
			throws RemoteException {
		final Guild guild = guilds.get(initiator.getGuildId());
		if (guild != null) {
			guild.expelMember(initiator, cid);
		}
	}

	public void setGuildNotice(final int gid, final String notice)
			throws RemoteException {
		final Guild guild = guilds.get(gid);
		if (guild != null) {
			guild.setGuildNotice(notice);
		}
	}

	public void updateGuildMemberLevel(int guildId, int characterId, int level)
			throws RemoteException {
		final Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.updateMemberLevel(characterId, level);
		}
	}

	public void updateGuildMemberJob(int guildId, int characterId, int jobId)
			throws RemoteException {
		final Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.updateMemberJob(characterId, jobId);
		}
	}

	public void changeRankTitle(final int gid, final String[] ranks)
			throws RemoteException {
		final Guild guild = guilds.get(gid);
		if (guild != null) {
			guild.changeRankTitle(ranks);
		}
	}

	public void setGuildEmblem(final int gid, final short bg,
			final byte bgcolor, final short logo, final byte logocolor)
			throws RemoteException {
		final Guild guild = guilds.get(gid);
		if (guild != null) {
			guild.setGuildEmblem(bg, bgcolor, logo, logocolor);
		}
	}

	public void disbandGuild(final int guildId) throws RemoteException {
		guildMutex.lock();
		try {
			guilds.get(guildId).disbandGuild();
			guilds.remove(guildId);
		} finally {
			guildMutex.unlock();
		}
	}

	public final boolean increaseGuildCapacity(final int guildId)
			throws RemoteException {
		final Guild guild = guilds.get(guildId);
		if (guild != null) {
			return guild.increaseCapacity();
		}
		return false;
	}

	public void gainGP(final int guildId, final int amount)
			throws RemoteException {
		final Guild guild = guilds.get(guildId);
		if (guild != null) {
			guild.gainGuildPoints(amount);
		}
	}

	public final Messenger createMessenger(final MessengerMember chrfor)
			throws RemoteException {
		final int messengerid = runningMessengerId.getAndIncrement();
		final Messenger messenger = new Messenger(messengerid, chrfor);
		messengers.put(messenger.getId(), messenger);
		return messenger;
	}

	public final Messenger getMessenger(final int messengerid)
			throws RemoteException {
		return messengers.get(messengerid);
	}

	public final PlayerBuffStorage getPlayerBuffStorage()
			throws RemoteException {
		return buffStorage;
	}
}