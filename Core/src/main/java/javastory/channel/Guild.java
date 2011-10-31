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
import javastory.rmi.ChannelWorldInterface;
import javastory.server.GameService;
import javastory.server.Notes;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.GuildOperationResponse;
import javastory.world.core.GuildOperationType;
import javastory.world.core.WorldRegistry;

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
	private int id, guildPoints, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature;
	private final Map<MemberRank, String> rankTitles = Maps.newEnumMap(MemberRank.class);
	private final Map<Integer, GuildMember> members = new CopyOnWriteMap<>();
	// Misc filds:
	private final Lock lock = new ReentrantLock();
	private Multimap<Integer, Integer> channelIndex;

	public Guild(final GuildMember initiator) {
		super();
		final int guildid = initiator.getGuildId();
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid=" + guildid);
			ResultSet rs = ps.executeQuery();
			if (!rs.first()) {
				rs.close();
				ps.close();
				this.id = -1;
				return;
			}
			this.id = guildid;
			this.name = rs.getString("name");
			this.guildPoints = rs.getInt("GP");
			this.logo = rs.getInt("logo");
			this.logoColor = rs.getInt("logoColor");
			this.logoBG = rs.getInt("logoBG");
			this.logoBGColor = rs.getInt("logoBGColor");
			this.capacity = rs.getInt("capacity");
			this.rankTitles.put(MemberRank.MASTER, rs.getString("rank1title"));
			this.rankTitles.put(MemberRank.JR_MASTER, rs.getString("rank2title"));
			this.rankTitles.put(MemberRank.MEMBER_HIGH, rs.getString("rank3title"));
			this.rankTitles.put(MemberRank.MEMBER_MIDDLE, rs.getString("rank4title"));
			this.rankTitles.put(MemberRank.MEMBER_LOW, rs.getString("rank5title"));
			this.leader = rs.getInt("leader");
			this.notice = rs.getString("notice");
			this.signature = rs.getInt("signature");
			rs.close();
			ps.close();
			ps = con.prepareStatement("SELECT id, name, level, job, guildrank FROM characters WHERE guildid = ?");
			ps.setInt(1, guildid);
			rs = ps.executeQuery();
			if (!rs.first()) {
				System.err.println("No members in guild.  Impossible...");
				rs.close();
				ps.close();
				return;
			}
			do {
				final GuildMember member = new GuildMember(rs);
				final int characterId = member.getCharacterId();
				this.members.put(characterId, member);
			} while (rs.next());
			this.setOnline(initiator.getCharacterId(), true, initiator.getChannel());
			rs.close();
			ps.close();
		} catch (final SQLException se) {
			System.err.println("unable to read guild information from sql" + se);
			return;
		}
	}

	public Guild(final int guildId) {
		// retrieves the guild from database, with guildid
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("SELECT * FROM `guilds` WHERE `guildid` = ?")) {
			ps.setInt(1, guildId);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.first()) { // no result... most likely to be someone from a disbanded guild that got rolled back
					rs.close();
					ps.close();
					this.id = -1;
					return;
				}
				this.id = guildId;
				this.name = rs.getString("name");
				this.guildPoints = rs.getInt("GP");
				this.logo = rs.getInt("logo");
				this.logoColor = rs.getInt("logoColor");
				this.logoBG = rs.getInt("logoBG");
				this.logoBGColor = rs.getInt("logoBGColor");
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
		} catch (final SQLException se) {
			System.err.println("unable to read guild information from sql" + se);
			return;
		}
	}

	private void writeToDB(final boolean bDisband) {
		try {
			final Connection con = Database.getConnection();
			if (!bDisband) {
				final StringBuilder buf = new StringBuilder();
				buf.append("UPDATE guilds SET GP = ?, logo = ?, ");
				buf.append("logoColor = ?, logoBG = ?, logoBGColor = ?, ");
				buf.append("rank1title = ?, rank2title = ?, rank3title = ?, ");
				buf.append("rank4title = ?, rank5title = ?, capacity = ?, ");
				buf.append("notice = ? WHERE guildid = ?");
				try (PreparedStatement ps = con.prepareStatement(buf.toString())) {
					ps.setInt(1, this.guildPoints);
					ps.setInt(2, this.logo);
					ps.setInt(3, this.logoColor);
					ps.setInt(4, this.logoBG);
					ps.setInt(5, this.logoBGColor);
					ps.setString(6, this.rankTitles.get(MemberRank.MASTER));
					ps.setString(7, this.rankTitles.get(MemberRank.JR_MASTER));
					ps.setString(8, this.rankTitles.get(MemberRank.MEMBER_HIGH));
					ps.setString(9, this.rankTitles.get(MemberRank.MEMBER_MIDDLE));
					ps.setString(10, this.rankTitles.get(MemberRank.MEMBER_LOW));
					ps.setInt(11, this.capacity);
					ps.setString(12, this.notice);
					ps.setInt(13, this.id);
					ps.execute();
				}
			} else {
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();

				ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
				ps.setInt(1, this.id);
				ps.execute();
				ps.close();
				this.broadcast(ChannelPackets.guildDisband(this.id));
			}
		} catch (final SQLException se) {
			System.err.println("Error saving guild to SQL" + se);
		}
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

	public int getLogo() {
		return this.logo;
	}

	public void setLogo(final int l) {
		this.logo = l;
	}

	public int getLogoColor() {
		return this.logoColor;
	}

	public void setLogoColor(final int c) {
		this.logoColor = c;
	}

	public int getLogoBG() {
		return this.logoBG;
	}

	public void setLogoBG(final int bg) {
		this.logoBG = bg;
	}

	public int getLogoBGColor() {
		return this.logoBGColor;
	}

	public void setLogoBGColor(final int c) {
		this.logoBGColor = c;
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
		final WorldRegistry registry = GameService.getWorldRegistry();
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
		final Set<Integer> activeChannels = GameService.getWorldRegistry().getActiveChannels();
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
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.first()) {// name taken
				rs.close();
				ps.close();
				return 0;
			}
			ps.close();
			rs.close();
			ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`, `alliance`) VALUES (?, ?, ?, 0)");
			ps.setInt(1, leaderId);
			ps.setString(2, name);
			ps.setInt(3, (int) System.currentTimeMillis());
			ps.execute();
			ps.close();
			ps = con.prepareStatement("SELECT guildid FROM guilds WHERE leader = ?");
			ps.setInt(1, leaderId);
			rs = ps.executeQuery();
			rs.first();
			final int result = rs.getInt("guildid");
			rs.close();
			ps.close();
			return result;
		} catch (final SQLException se) {
			System.err.println("SQL THROW" + se);
			return 0;
		}
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
				GameService.getWorldRegistry().getChannel(target.getChannel()).setGuildAndRank(targetId, 0, MemberRank.MEMBER_LOW);
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
				GameService.getWorldRegistry().getChannel(target.getChannel()).setGuildAndRank(targetId, this.id, newRank);
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
		for (int i = 1; i <= 5; i++) {
			final MemberRank rank = MemberRank.fromNumber(i);
			this.rankTitles.put(rank, titles[i - 1]);
		}
		this.broadcast(ChannelPackets.rankTitleChange(this.id, titles));

		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con
			.prepareStatement("UPDATE guilds SET rank1title = ?, rank2title = ?, rank3title = ?, rank4title = ?, rank5title = ? WHERE guildid = ?")) {
			ps.setString(1, this.rankTitles.get(MemberRank.MASTER));
			ps.setString(2, this.rankTitles.get(MemberRank.JR_MASTER));
			ps.setString(3, this.rankTitles.get(MemberRank.MEMBER_HIGH));
			ps.setString(4, this.rankTitles.get(MemberRank.MEMBER_MIDDLE));
			ps.setString(5, this.rankTitles.get(MemberRank.MEMBER_LOW));
			ps.setInt(6, this.id);
			ps.execute();
		} catch (final SQLException ex) {
			System.err.println("Could not save rank titles: " + ex);
		}
	}

	public void disbandGuild() {
		this.writeToDB(true);
		this.broadcast(null, -1, GuildOperationType.DISBAND);
	}

	public void setGuildEmblem(final short bg, final byte bgcolor, final short logo, final byte logocolor) {
		this.logoBG = bg;
		this.logoBGColor = bgcolor;
		this.logo = logo;
		this.logoColor = logocolor;
		this.broadcast(null, -1, GuildOperationType.EMBELMCHANGE);
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?")) {
			ps.setInt(1, logo);
			ps.setInt(2, this.logoColor);
			ps.setInt(3, this.logoBG);
			ps.setInt(4, this.logoBGColor);
			ps.setInt(5, this.id);
			ps.execute();
		} catch (final SQLException ex) {
			System.err.println("Could not save guild emblem: " + ex);
		}
	}

	public boolean increaseCapacity() {
		if (this.capacity + GUILD_CAPACITY_STEP > GUILD_CAPACITY_MAX) {
			return false;
		}
		this.capacity += 5;
		this.broadcast(ChannelPackets.guildCapacityChange(this.id, this.capacity));
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET capacity = ? WHERE guildid = ?")) {
			ps.setInt(1, this.capacity);
			ps.setInt(2, this.id);
			ps.execute();
		} catch (final SQLException ex) {
			System.err.println("Could not save guild member capacity: " + ex);
		}
		return true;
	}

	public void gainGuildPoints(final int amount) {
		this.guildPoints += amount;
		this.guildMessage(ChannelPackets.updateGuildPoints(this.id, this.guildPoints));
		final Connection con = Database.getConnection();

		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET gp = ? WHERE guildid = ?")) {
			ps.setInt(1, this.guildPoints);
			ps.setInt(2, this.id);
			ps.execute();
		} catch (final SQLException e) {
			System.err.println("Saving guild point ERROR" + e);
		}
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