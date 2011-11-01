package javastory.world;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.Guild;
import javastory.channel.GuildMember;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.Party;
import javastory.channel.PlayerBuffStorage;
import javastory.channel.client.MemberRank;
import javastory.config.ChannelInfo;
import javastory.db.Database;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.GenericRemoteObject;
import javastory.rmi.LoginWorldInterface;
import javastory.rmi.WorldChannelInterface;
import javastory.rmi.WorldLoginInterface;
import javastory.world.core.ServerStatus;
import javastory.world.core.WorldRegistry;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class WorldRegistryImpl extends GenericRemoteObject implements WorldRegistry {

	private static final long serialVersionUID = -5170574938159280746L;
	private static WorldRegistryImpl instance = null;
	//
	private final ServerStatus loginStatus;
	private final List<LoginWorldInterface> logins = Lists.newLinkedList();
	//
	private final Map<Integer, ServerStatus> channelStatus;
	private final Map<Integer, ChannelWorldInterface> channels;
	//
	private final AtomicInteger runningMessengerId = new AtomicInteger();
	private final Map<Integer, Messenger> messengers = Maps.newHashMap();
	//
	private final AtomicInteger runningPartyId = new AtomicInteger();
	private final Map<Integer, Party> parties = Maps.newHashMap();
	//
	private final Map<Integer, Guild> guilds = Maps.newLinkedHashMap();
	private final PlayerBuffStorage buffStorage = new PlayerBuffStorage();
	private final Lock guildMutex = new ReentrantLock();

	private WorldRegistryImpl() throws RemoteException {
		super();

		final Connection con = Database.getConnection();
		try (	PreparedStatement ps = con.prepareStatement("SELECT MAX(party)+1 FROM characters");
				ResultSet rs = ps.executeQuery()) {
			rs.next();
			this.runningPartyId.set(rs.getInt(1));
		} catch (final SQLException ex) {
			System.err.println("Could not load max party ID: " + ex);
		}

		this.channelStatus = Maps.newLinkedHashMap();
		this.channels = Maps.newLinkedHashMap();

		this.loginStatus = ServerStatus.OFFLINE;

		this.runningMessengerId.set(1);
	}

	public static WorldRegistry getInstance() {
		if (instance == null) {
			try {
				instance = new WorldRegistryImpl();
			} catch (final RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		return instance;
	}

	private boolean isChannelActive(final int channelId) {
		final ServerStatus status = this.getChannelStatus(channelId);
		return !status.equals(ServerStatus.OFFLINE);
	}

	private boolean isLoginActive() {
		return !this.loginStatus.equals(ServerStatus.OFFLINE);
	}

	private ServerStatus getChannelStatus(final int channelId) {
		ServerStatus status = this.channelStatus.get(channelId);
		if (status == null) {
			status = ServerStatus.OFFLINE;
			this.channelStatus.put(channelId, status);
		}
		return status;
	}

	@Override
	public WorldChannelInterface registerChannelServer(final ChannelInfo info, final ChannelWorldInterface channel) throws RemoteException {
		final int id = info.getId();
		if (this.isChannelActive(id)) {
			throw new IllegalStateException("The specified channel slot is already active.");
		}
		this.channels.put(id, channel);
		final WorldChannelInterface ret = new WorldChannelInterfaceImpl(channel, id);
		return ret;
	}

	@Override
	public void deregisterChannelServer(final int channelId) throws RemoteException {
		if (!this.isChannelActive(channelId)) {
			throw new IllegalStateException("The specified channel slot is not currently active.");
		}

		this.channels.remove(channelId);
		this.channelStatus.put(channelId, ServerStatus.OFFLINE);

		for (final LoginWorldInterface wli : this.logins) {
			wli.channelOffline(channelId);
		}
		System.out.println("Channel " + channelId + " is offline.");
	}

	@Override
	public WorldLoginInterface registerLoginServer(final LoginWorldInterface login) throws RemoteException {
		if (this.isLoginActive()) {
			throw new IllegalStateException("The login server is already active.");
		}
		final WorldLoginInterface ret = new WorldLoginInterfaceImpl();

		this.logins.add(login);
		for (final ChannelWorldInterface cwi : this.channels.values()) {
			login.channelOnline(cwi.getChannelInfo());
		}

		return ret;
	}

	@Override
	public void deregisterLoginServer(final LoginWorldInterface cb) throws RemoteException {
		if (!this.isLoginActive()) {
			throw new IllegalStateException("The login server is not currently active.");
		}
		this.logins.remove(cb);
	}

	@Override
	public List<LoginWorldInterface> getLoginServer() {
		return Lists.newLinkedList(this.logins);
	}

	@Override
	public ChannelWorldInterface getChannel(final int channel) {
		return this.channels.get(channel);
	}

	@Override
	public ImmutableSet<Integer> getActiveChannels() {
		return ImmutableSet.copyOf(this.channels.keySet());
	}

	@Override
	public Collection<ChannelWorldInterface> getAllChannelServers() {
		return this.channels.values();
	}

	@Override
	public Party createParty() {
		final int partyid = this.runningPartyId.getAndIncrement();
		final Party party = new Party(partyid);
		this.parties.put(party.getId(), party);
		return party;
	}

	@Override
	public Party getParty(final int partyid) {
		return this.parties.get(partyid);
	}

	@Override
	public Party disbandParty(final int partyid) {
		return this.parties.remove(partyid);
	}

	@Override
	public final String getStatus() throws RemoteException {
		final StringBuilder ret = new StringBuilder();
		final List<Entry<Integer, ChannelWorldInterface>> channelServers = Lists.newArrayList(this.channels.entrySet());
		int totalUsers = 0;
		for (final Entry<Integer, ChannelWorldInterface> cs : channelServers) {
			ret.append("Channel ");
			ret.append(cs.getKey());
			try {
				cs.getValue().ping();
				ret.append(": online, ");
				final int channelUsers = cs.getValue().getConnected();
				totalUsers += channelUsers;
				ret.append(channelUsers);
				ret.append(" users\n");
			} catch (final RemoteException e) {
				ret.append(": offline\n");
			}
		}
		ret.append("Total users online: ");
		ret.append(totalUsers);
		ret.append("\n");
		// Properties props = new
		// Properties(WorldServer.getInstance().getWorldProperties());
		for (final LoginWorldInterface lwi : this.logins) {
			ret.append("Login: ");
			try {
				lwi.ping();
				ret.append("online\n");
			} catch (final RemoteException e) {
				ret.append("offline\n");
			}
		}
		return ret.toString();
	}

	@Override
	public final int createGuild(final int leaderId, final String name) {
		return Guild.createGuild(leaderId, name);
	}

	@Override
	public final Guild getGuild(final int guildId) {
		this.guildMutex.lock();
		try {
			return this.loadGuildIfAbsent(guildId);
		} finally {
			this.guildMutex.unlock();
		}
	}

	private Guild loadGuildIfAbsent(final int guildId) {
		Guild guild = this.guilds.get(guildId);
		if (guild == null) {
			guild = new Guild(guildId);
			if (guild.getId() == -1) {
				return null;
			}
			this.guilds.put(guildId, guild);
		}
		return guild;
	}

	@Override
	public void setGuildMemberOnline(final GuildMember mgc, final boolean bOnline, final int channel) {
		this.getGuild(mgc.getGuildId()).setOnline(mgc.getCharacterId(), bOnline, channel);
	}

	@Override
	public final boolean addGuildMember(final GuildMember mgc) {
		final Guild guild = this.guilds.get(mgc.getGuildId());
		return guild != null && guild.addGuildMember(mgc);
	}

	@Override
	public void leaveGuild(final GuildMember mgc) {
		final Guild guild = this.guilds.get(mgc.getGuildId());
		if (guild != null) {
			guild.leaveGuild(mgc);
		}
	}

	@Override
	public void guildChat(final int gid, final String name, final int cid, final String msg) throws RemoteException {
		final Guild guild = this.guilds.get(gid);
		if (guild != null) {
			guild.guildChat(name, cid, msg);
		}
	}

	@Override
	public void changeRank(final int gid, final int cid, final MemberRank newRank) throws RemoteException {
		final Guild guild = this.guilds.get(gid);
		if (guild != null) {
			guild.changeRank(cid, newRank);
		}
	}

	@Override
	public void expelMember(final GuildMember initiator, final int cid) throws RemoteException {
		final Guild guild = this.guilds.get(initiator.getGuildId());
		if (guild != null) {
			guild.expelMember(initiator, cid);
		}
	}

	@Override
	public void setGuildNotice(final int gid, final String notice) throws RemoteException {
		final Guild guild = this.guilds.get(gid);
		if (guild != null) {
			guild.setGuildNotice(notice);
		}
	}

	@Override
	public void updateGuildMemberLevel(final int guildId, final int characterId, final int level) throws RemoteException {
		final Guild guild = this.guilds.get(guildId);
		if (guild != null) {
			guild.updateMemberLevel(characterId, level);
		}
	}

	@Override
	public void updateGuildMemberJob(final int guildId, final int characterId, final int jobId) throws RemoteException {
		final Guild guild = this.guilds.get(guildId);
		if (guild != null) {
			guild.updateMemberJob(characterId, jobId);
		}
	}

	@Override
	public void changeRankTitle(final int gid, final String[] ranks) throws RemoteException {
		final Guild guild = this.guilds.get(gid);
		if (guild != null) {
			guild.changeRankTitle(ranks);
		}
	}

	@Override
	public void setGuildEmblem(final int gid, final short bg, final byte bgcolor, final short logo, final byte logocolor) throws RemoteException {
		final Guild guild = this.guilds.get(gid);
		if (guild != null) {
			guild.setGuildEmblem(bg, bgcolor, logo, logocolor);
		}
	}

	@Override
	public void disbandGuild(final int guildId) throws RemoteException {
		this.guildMutex.lock();
		try {
			this.guilds.get(guildId).disbandGuild();
			this.guilds.remove(guildId);
		} finally {
			this.guildMutex.unlock();
		}
	}

	@Override
	public final boolean increaseGuildCapacity(final int guildId) throws RemoteException {
		final Guild guild = this.guilds.get(guildId);
		if (guild != null) {
			return guild.increaseCapacity();
		}
		return false;
	}

	@Override
	public void gainGP(final int guildId, final int amount) throws RemoteException {
		final Guild guild = this.guilds.get(guildId);
		if (guild != null) {
			guild.gainGuildPoints(amount);
		}
	}

	@Override
	public final Messenger createMessenger(final MessengerMember chrfor) throws RemoteException {
		final int messengerid = this.runningMessengerId.getAndIncrement();
		final Messenger messenger = new Messenger(messengerid, chrfor);
		this.messengers.put(messenger.getId(), messenger);
		return messenger;
	}

	@Override
	public final Messenger getMessenger(final int messengerid) throws RemoteException {
		return this.messengers.get(messengerid);
	}

	@Override
	public final PlayerBuffStorage getPlayerBuffStorage() throws RemoteException {
		return this.buffStorage;
	}
}