package javastory.channel;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.client.MemberRank;
import javastory.db.Database;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.registry.Universe;
import javastory.registry.WorldRegistry;
import javastory.rmi.ChannelWorldInterface;
import javastory.server.Notes;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.GuildOperationResponse;
import javastory.world.core.GuildOperationType;

import org.apache.mina.util.CopyOnWriteMap;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public final class Guild {

	private final static int GUILD_CAPACITY_MAX = 100;
	private final static int GUILD_CAPACITY_STEP = 5;
	//
	private boolean rebuildIndex = true;
	// Guild information fields:
	private String name, notice;
	private GuildEmblem emblem;
	private int id, guildPoints, leader, capacity, signature;
	private final Map<MemberRank, String> rankTitles = Maps.newEnumMap(MemberRank.class);
	private final Map<Integer, GuildMember> members = new CopyOnWriteMap<>();
	// Misc filds:
	private final Lock lock = new ReentrantLock();
	private Multimap<Integer, Integer> channelIndex;

	public Guild(final GuildMember initiator) {
		super();
		final Connection connection = Database.getConnection();
		try {
			final int guildId = initiator.getGuildId();
			try (	PreparedStatement ps = getGuildSelectStatement(connection, guildId);
					ResultSet rs = ps.executeQuery()) {

				if (!rs.first()) {
					this.id = -1;
					return;
				}

				this.id = guildId;
				this.name = rs.getString("name");
				this.guildPoints = rs.getInt("GP");

				this.emblem = new GuildEmblem(rs);
				this.capacity = rs.getInt("capacity");

				this.rankTitles.put(MemberRank.MASTER, rs.getString("rank1title"));
				this.rankTitles.put(MemberRank.JR_MASTER, rs.getString("rank2title"));
				this.rankTitles.put(MemberRank.MEMBER_HIGH, rs.getString("rank3title"));
				this.rankTitles.put(MemberRank.MEMBER_MIDDLE, rs.getString("rank4title"));
				this.rankTitles.put(MemberRank.MEMBER_LOW, rs.getString("rank5title"));

				this.leader = rs.getInt("leader");
				this.notice = rs.getString("notice");
				this.signature = rs.getInt("signature");
			}

			try (	PreparedStatement ps = getSelectMemberInfoStatement(guildId, connection);
					ResultSet rs = ps.executeQuery()) {
				if (!rs.first()) {
					System.err.println("No members in guild.  Impossible...");
					return;
				}
				do {
					final GuildMember member = new GuildMember(rs);
					final int characterId = member.getCharacterId();
					this.members.put(characterId, member);
				} while (rs.next());
				this.setOnline(initiator.getCharacterId(), true, initiator.getChannel());
			}
		} catch (final SQLException se) {
			System.err.println("unable to read guild information from sql" + se);
			return;
		}
	}

	private PreparedStatement getSelectMemberInfoStatement(final int guildid, final Connection connection) throws SQLException {
		final String sql = "SELECT `id`, `name`, `level`, `job`, `guildrank` FROM `characters` WHERE `guildid` = ?";
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, guildid);
		return ps;
	}

	private PreparedStatement getGuildSelectStatement(final Connection connection, final int guildId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT * FROM `guilds` WHERE `guildid` = ?");
		ps.setInt(1, guildId);
		return ps;
	}

	public Guild(final int guildId) {
		// retrieves the guild from database, with guildId
		final Connection connection = Database.getConnection();
		try (	PreparedStatement ps = getGuildSelectStatement(connection, guildId);
				ResultSet rs = ps.executeQuery()) {
			if (!rs.first()) {
				// no result... most likely to be someone from a disbanded guild that got rolled back
				this.id = -1;
				return;
			}
			this.id = guildId;
			this.name = rs.getString("name");
			this.guildPoints = rs.getInt("GP");
			this.emblem = new GuildEmblem(rs);
			this.capacity = rs.getInt("capacity");
			this.rankTitles.put(MemberRank.MASTER, rs.getString("rank1title"));
			this.rankTitles.put(MemberRank.JR_MASTER, rs.getString("rank2title"));
			this.rankTitles.put(MemberRank.MEMBER_HIGH, rs.getString("rank3title"));
			this.rankTitles.put(MemberRank.MEMBER_MIDDLE, rs.getString("rank4title"));
			this.rankTitles.put(MemberRank.MEMBER_LOW, rs.getString("rank5title"));
			this.leader = rs.getInt("leader");
			this.notice = rs.getString("notice");
			this.signature = rs.getInt("signature");
		} catch (final SQLException se) {
			System.err.println("unable to read guild information from sql" + se);
			return;
		}
	}

	private void deleteFromDatabase() {
		final Connection connection = Database.getConnection();
		try (	PreparedStatement unmarkCharacters = getUnmarkCharactersStatement(connection);
				PreparedStatement deleteGuild = getDeleteGuildStatement(connection)) {

			unmarkCharacters.execute();
			deleteGuild.execute();
			this.broadcast(ChannelPackets.guildDisband(this.id));
		} catch (final SQLException se) {
			System.err.println("Error deleting guild from database: " + se);
		}
	}

	private PreparedStatement getDeleteGuildStatement(final Connection connection) throws SQLException {
		final PreparedStatement ps = connection.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
		ps.setInt(1, this.id);
		return ps;
	}

	private PreparedStatement getUnmarkCharactersStatement(final Connection connection) throws SQLException {
		final PreparedStatement ps = connection.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
		ps.setInt(1, this.id);
		return ps;
	}

	public int getId() {
		return this.id;
	}

	public int getLeaderId() {
		return this.leader;
	}

	public int getGuildPoints() {
		return this.guildPoints;
	}

	public GuildEmblem getEmblem() {
		return this.emblem;
	}

	public String getNotice() {
		if (this.notice == null) {
			return "";
		}
		return this.notice;
	}

	public String getName() {
		return this.name;
	}

	public int getCapacity() {
		return this.capacity;
	}

	public int getSignature() {
		return this.signature;
	}

	public void broadcast(final GamePacket packet) {
		this.broadcast(packet, -1, GuildOperationType.NONE);
	}

	public void broadcast(final GamePacket packet, final int exceptionId) {
		this.broadcast(packet, exceptionId, GuildOperationType.NONE);
	}

	// multi-purpose function that reaches every member of guild (except the character with exceptionId) in all channels with as little access to rmi as possible
	private void broadcast(final GamePacket packet, final int exceptionId, final GuildOperationType operation) {
		final WorldRegistry registry = Universe.getWorldRegistry();
		this.lock.lock();
		try {
			try {
				this.rebuildChannelIndex();
				final Set<Integer> activeChannels = registry.getActiveChannels();
				for (final Integer channelId : activeChannels) {
					final ChannelWorldInterface channel = registry.getChannel(channelId);
					final Collection<Integer> channelMembers = this.channelIndex.get(channelId);

					if (channelMembers.size() > 0) {
						if (operation == GuildOperationType.DISBAND) {
							channel.setGuildAndRank(channelMembers, 0, MemberRank.MEMBER_LOW, exceptionId);
						} else if (operation == GuildOperationType.EMBELMCHANGE) {
							channel.changeEmblem(this.id, channelMembers, new GuildSummary(this));
						} else {
							channel.sendPacket(channelMembers, packet, exceptionId);
						}
					}
				}
			} catch (final RemoteException re) {
				System.err.println("Failed to contact channel(s) for broadcast." + re);
			}
		} finally {
			this.lock.unlock();
		}
	}

	private void rebuildChannelIndex() throws RemoteException {
		// any function that calls this should be wrapped in synchronized(notifications) to make sure that it doesn't change before that function finishes with the updated notifications
		if (!this.rebuildIndex) {
			return;
		}
		final Set<Integer> activeChannels = Universe.getWorldRegistry().getActiveChannels();
		this.channelIndex = HashMultimap.create();
		for (final Map.Entry<Integer, GuildMember> entry : this.members.entrySet()) {
			final GuildMember member = entry.getValue();
			if (!member.isOnline()) {
				continue;
			}

			this.channelIndex.put(member.getChannel(), member.getCharacterId());
		}
		this.channelIndex.keySet().retainAll(activeChannels);
		this.rebuildIndex = false;
	}

	public void guildMessage(final GamePacket packet) {
		// TODO: Not done. Complete when proper ChannelServer remoting is done.
	}

	public void setOnline(final int cid, final boolean online, final int channel) {
		final GuildMember member = this.getMember(cid);

		// Only broadcast whatever if something /changed/.
		// To begin with this shouldn't get called otherwise, but... *shrug*
		if (!(member.isOnline() && online)) {
			return;
		}

		member.setOnline(online);
		member.setChannel((byte) channel);
		this.broadcast(ChannelPackets.guildMemberOnline(this.id, cid, online), cid);
		this.rebuildIndex = true;
	}

	private GuildMember getMember(final int characterId) {
		return this.members.get(characterId);
	}

	public void guildChat(final String name, final int cid, final String msg) {
		this.broadcast(ChannelPackets.multiChat(name, msg, 2), cid);
	}

	public void allianceChat(final String name, final int cid, final String msg) {
		this.broadcast(ChannelPackets.multiChat(name, msg, 3), cid);
	}

	public String getRankTitle(final MemberRank rank) {
		Preconditions.checkNotNull(rank);
		return this.rankTitles.get(rank);
	}

	// function to create guild, returns the guild id if successful, 0 if not
	public static int createGuild(final int leaderId, final String name) {
		final Connection connection = Database.getConnection();
		try {
			try (	PreparedStatement selectNameStatement = getSelectGuildStatement(connection, name);
					ResultSet rs = selectNameStatement.executeQuery()) {

				if (rs.first()) {
					// the guild name is taken.
					// TODO: shouldn't we do this a bit earlier?
					return 0;
				}
			}

			try (PreparedStatement ps = getInsertGuildStatement(connection, leaderId, name)) {
				ps.execute();
			}

			try (	PreparedStatement ps = getSelectGuildByLeaderStatement(connection, leaderId);
					ResultSet rs = ps.executeQuery()) {

				rs.first();
				final int result = rs.getInt("guildid");
				return result;
			}
		} catch (final SQLException se) {
			System.err.println("Error during guild creation: " + se);
			return 0;
		}
	}

	private static PreparedStatement getSelectGuildByLeaderStatement(final Connection connection, final int leaderId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
		ps.setInt(1, leaderId);
		return ps;
	}

	private static PreparedStatement getInsertGuildStatement(final Connection connection, final int leaderId, final String name) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`, `alliance`) VALUES (?, ?, ?, 0)");
		ps.setInt(1, leaderId);
		ps.setString(2, name);
		ps.setInt(3, (int) System.currentTimeMillis());
		return ps;
	}

	private static PreparedStatement getSelectGuildStatement(final Connection connection, final String name) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
		ps.setString(1, name);
		return ps;
	}

	public boolean addGuildMember(final GuildMember member) {
		this.lock.lock();
		try {
			if (this.members.size() >= this.capacity) {
				return false;
			}
			this.members.put(member.getCharacterId(), member);
			this.rebuildIndex = true;
		} finally {
			this.lock.unlock();
		}
		this.broadcast(ChannelPackets.newGuildMember(member));
		return true;
	}

	public void leaveGuild(final GuildMember member) {
		this.broadcast(ChannelPackets.memberLeft(member, false));
		this.lock.lock();
		try {
			this.members.remove(member.getCharacterId());
			this.rebuildIndex = true;
		} finally {
			this.lock.unlock();
		}
	}

	public void expelMember(final GuildMember initiator, final int targetId) {
		final GuildMember target = this.getMember(targetId);
		this.broadcast(ChannelPackets.memberLeft(target, true));
		this.rebuildIndex = true;
		this.members.remove(targetId);
		try {
			if (target.isOnline()) {
				Universe.getWorldRegistry().getChannel(target.getChannel()).setGuildAndRank(targetId, 0, MemberRank.MEMBER_LOW);
			} else {
				Notes.send(initiator.getName(), target.getName(), "You have been expelled from the guild.");
			}
		} catch (final RemoteException ex) {
			System.err.println("Could not expel member: " + ex);
			return;
		}
	}

	public void changeRank(final int targetId, final MemberRank newRank) {
		final GuildMember target = this.getMember(targetId);
		try {
			if (target.isOnline()) {
				Universe.getWorldRegistry().getChannel(target.getChannel()).setGuildAndRank(targetId, this.id, newRank);
			}
		} catch (final RemoteException ex) {
			System.err.println("Could not change rank: " + ex);
			return;
		}
		target.setGuildRank(newRank);
		this.broadcast(ChannelPackets.changeRank(target));
	}

	public void setGuildNotice(final String notice) {
		this.notice = notice;
		this.broadcast(ChannelPackets.guildNotice(this.id, notice));
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET notice = ? WHERE guildid = ?")) {
			ps.setString(1, notice);
			ps.setInt(2, this.id);
			ps.execute();
		} catch (final SQLException ex) {
			System.err.println("Could not save notice: " + ex);
		}
	}

	public void updateMemberLevel(final int characterId, final int level) {
		final GuildMember member = this.getMember(characterId);
		member.setLevel(level);
		this.broadcast(ChannelPackets.guildMemberInfoUpdate(member));
	}

	public void updateMemberJob(final int characterId, final int jobId) {
		final GuildMember member = this.getMember(characterId);
		member.setJobId(this.id);
		this.broadcast(ChannelPackets.guildMemberInfoUpdate(member));
	}

	public void changeRankTitle(final String[] titles) {
		Map<MemberRank, String> newTitles = Maps.newHashMap();
		for (int i = 1; i <= 5; i++) {
			final MemberRank rank = MemberRank.fromNumber(i);
			newTitles.put(rank, titles[i - 1]);
		}

		final Connection con = Database.getConnection();
		try (PreparedStatement ps = getUpdateMemberRanksStatement(con, newTitles)) {
			ps.execute();

			this.rankTitles.putAll(newTitles);
			this.broadcast(ChannelPackets.rankTitleChange(this.id, titles));
		} catch (final SQLException ex) {
			System.err.println("Could not save rank titles: " + ex);
		}
	}

	private PreparedStatement getUpdateMemberRanksStatement(final Connection connection, final Map<MemberRank, String> newTitles) throws SQLException {
		final String sql = "UPDATE guilds SET rank1title = ?, rank2title = ?, rank3title = ?, rank4title = ?, rank5title = ? WHERE guildid = ?";
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setString(1, newTitles.get(MemberRank.MASTER));
		ps.setString(2, newTitles.get(MemberRank.JR_MASTER));
		ps.setString(3, newTitles.get(MemberRank.MEMBER_HIGH));
		ps.setString(4, newTitles.get(MemberRank.MEMBER_MIDDLE));
		ps.setString(5, newTitles.get(MemberRank.MEMBER_LOW));
		ps.setInt(6, this.id);
		return ps;
	}

	public void disbandGuild() {
		this.deleteFromDatabase();
		this.broadcast(null, -1, GuildOperationType.DISBAND);
	}

	public void setGuildEmblem(final short fgStyle, final byte fgColor, final short bgStyle, final byte bgColor) {
		final GuildEmblem newEmblem = new GuildEmblem(fgStyle, fgColor, bgStyle, bgColor);

		final Connection connection = Database.getConnection();
		try (PreparedStatement ps = getUpdateEmblemStatement(connection, newEmblem)) {
			ps.execute();

			this.emblem = newEmblem;
			this.broadcast(null, -1, GuildOperationType.EMBELMCHANGE);
		} catch (final SQLException ex) {
			System.err.println("Could not save guild emblem: " + ex);
		}
	}

	private PreparedStatement getUpdateEmblemStatement(final Connection connection, GuildEmblem newEmblem) throws SQLException {
		final String sql = "UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?";
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, newEmblem.getFgStyle());
		ps.setInt(2, newEmblem.getFgColor());
		ps.setInt(3, newEmblem.getBgStyle());
		ps.setInt(4, newEmblem.getBgColor());
		ps.setInt(5, this.id);
		return ps;
	}

	public boolean increaseCapacity() {
		int newCapacity = this.capacity + GUILD_CAPACITY_STEP;
		if (newCapacity > GUILD_CAPACITY_MAX) {
			return false;
		}

		final Connection connection = Database.getConnection();
		try (PreparedStatement ps = getUpdateCapacityStatement(connection, newCapacity)) {
			ps.execute();

			this.capacity = newCapacity;
			this.broadcast(ChannelPackets.guildCapacityChange(this.id, this.capacity));
		} catch (final SQLException ex) {
			System.err.println("Could not save guild member capacity: " + ex);
		}
		return true;
	}

	private PreparedStatement getUpdateCapacityStatement(final Connection connection, int newCapacity) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE guilds SET capacity = ? WHERE guildid = ?");
		ps.setInt(1, newCapacity);
		ps.setInt(2, this.id);
		return ps;
	}

	public void gainGuildPoints(final int amount) {
		int newAmount = this.guildPoints + amount;

		final Connection connection = Database.getConnection();
		try (PreparedStatement ps = getUpdateGuildPointsStatement(connection, newAmount)) {
			ps.execute();

			this.guildPoints = newAmount;
			this.guildMessage(ChannelPackets.updateGuildPoints(this.id, this.guildPoints));
		} catch (final SQLException e) {
			System.err.println("Saving guild point ERROR" + e);
		}
	}

	private PreparedStatement getUpdateGuildPointsStatement(final Connection connection, int newAmount) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("UPDATE guilds SET gp = ? WHERE guildid = ?");
		ps.setInt(1, newAmount);
		ps.setInt(2, this.id);
		return ps;
	}

	public void addMemberData(final PacketBuilder builder) {
		builder.writeAsByte(this.members.size());
		for (final GuildMember mgc : this.members.values()) {
			builder.writeInt(mgc.getCharacterId());
		}
		for (final GuildMember member : this.members.values()) {
			builder.writePaddedString(member.getName(), 13);
			builder.writeInt(member.getJobId());
			builder.writeInt(member.getLevel());
			builder.writeInt(member.getRank().asNumber());
			builder.writeInt(member.isOnline() ? 1 : 0);
			builder.writeInt(this.signature);
			builder.writeInt(member.getRank().asNumber());
		}
	}

	// null indicates successful invitation being sent
	// keep in mind that this will be called by a handler most of the time
	// so this will be running mostly on a channel server, unlike the rest
	// of the class
	public static GuildOperationResponse sendInvite(final ChannelClient c, final String targetName) {
		final ChannelCharacter mc = ChannelServer.getPlayerStorage().getCharacterByName(targetName);
		if (mc == null) {
			return GuildOperationResponse.NOT_IN_CHANNEL;
		}
		if (mc.getGuildId() > 0) {
			return GuildOperationResponse.ALREADY_IN_GUILD;
		}
		final ChannelCharacter player = c.getPlayer();
		mc.getClient().write(ChannelPackets.guildInvite(player.getGuildId(), player.getName(), player.getLevel(), player.getJobId()));
		return null;
	}
}