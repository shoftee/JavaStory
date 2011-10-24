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

	private final int GUILD_CAPACITY_MAX = 100;
	private final int GUILD_CAPACITY_STEP = 5;
	//
	private boolean rebuildIndex = true;
	// Guild information fields:
	private String name, notice;
	private int id, guildPoints, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature;
	private final Map<MemberRank, String> rankTitles = Maps.newEnumMap(MemberRank.class);
	private final Map<Integer, GuildMember> members = new CopyOnWriteMap<>();
	// Misc filds:
	private Lock lock = new ReentrantLock();
	private Multimap<Integer, Integer> channelIndex;

	public Guild(final GuildMember initiator) {
		super();
		int guildid = initiator.getGuildId();
		try {
			Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid=" + guildid);
			ResultSet rs = ps.executeQuery();
			if (!rs.first()) {
				rs.close();
				ps.close();
				id = -1;
				return;
			}
			id = guildid;
			name = rs.getString("name");
			guildPoints = rs.getInt("GP");
			logo = rs.getInt("logo");
			logoColor = rs.getInt("logoColor");
			logoBG = rs.getInt("logoBG");
			logoBGColor = rs.getInt("logoBGColor");
			capacity = rs.getInt("capacity");
			rankTitles.put(MemberRank.MASTER, rs.getString("rank1title"));
			rankTitles.put(MemberRank.JR_MASTER, rs.getString("rank2title"));
			rankTitles.put(MemberRank.MEMBER_HIGH, rs.getString("rank3title"));
			rankTitles.put(MemberRank.MEMBER_MIDDLE, rs.getString("rank4title"));
			rankTitles.put(MemberRank.MEMBER_LOW, rs.getString("rank5title"));
			leader = rs.getInt("leader");
			notice = rs.getString("notice");
			signature = rs.getInt("signature");
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
				members.put(characterId, member);
			} while (rs.next());
			setOnline(initiator.getCharacterId(), true, initiator.getChannel());
			rs.close();
			ps.close();
		} catch (SQLException se) {
			System.err.println("unable to read guild information from sql" + se);
			return;
		}
	}

	public Guild(final int guildId) {
		// retrieves the guild from database, with guildid
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("SELECT * FROM `guilds` WHERE `guildid` = ?")) {
			ps.setInt(1, guildId);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.first()) { // no result... most likely to be someone from a disbanded guild that got rolled back
					rs.close();
					ps.close();
					id = -1;
					return;
				}
				id = guildId;
				name = rs.getString("name");
				guildPoints = rs.getInt("GP");
				logo = rs.getInt("logo");
				logoColor = rs.getInt("logoColor");
				logoBG = rs.getInt("logoBG");
				logoBGColor = rs.getInt("logoBGColor");
				capacity = rs.getInt("capacity");
				rankTitles.put(MemberRank.MASTER, rs.getString("rank1title"));
				rankTitles.put(MemberRank.JR_MASTER, rs.getString("rank2title"));
				rankTitles.put(MemberRank.MEMBER_HIGH, rs.getString("rank3title"));
				rankTitles.put(MemberRank.MEMBER_MIDDLE, rs.getString("rank4title"));
				rankTitles.put(MemberRank.MEMBER_LOW, rs.getString("rank5title"));
				leader = rs.getInt("leader");
				notice = rs.getString("notice");
				signature = rs.getInt("signature");
			}
		} catch (SQLException se) {
			System.err.println("unable to read guild information from sql" + se);
			return;
		}
	}

	private void writeToDB(final boolean bDisband) {
		try {
			Connection con = Database.getConnection();
			if (!bDisband) {
				StringBuilder buf = new StringBuilder();
				buf.append("UPDATE guilds SET GP = ?, logo = ?, ");
				buf.append("logoColor = ?, logoBG = ?, logoBGColor = ?, ");
				buf.append("rank1title = ?, rank2title = ?, rank3title = ?, ");
				buf.append("rank4title = ?, rank5title = ?, capacity = ?, ");
				buf.append("notice = ? WHERE guildid = ?");
				try (PreparedStatement ps = con.prepareStatement(buf.toString())) {
					ps.setInt(1, guildPoints);
					ps.setInt(2, logo);
					ps.setInt(3, logoColor);
					ps.setInt(4, logoBG);
					ps.setInt(5, logoBGColor);
					ps.setString(6, rankTitles.get(MemberRank.MASTER));
					ps.setString(7, rankTitles.get(MemberRank.JR_MASTER));
					ps.setString(8, rankTitles.get(MemberRank.MEMBER_HIGH));
					ps.setString(9, rankTitles.get(MemberRank.MEMBER_MIDDLE));
					ps.setString(10, rankTitles.get(MemberRank.MEMBER_LOW));
					ps.setInt(11, capacity);
					ps.setString(12, notice);
					ps.setInt(13, id);
					ps.execute();
				}
			} else {
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
				ps.setInt(1, id);
				ps.execute();
				ps.close();

				ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
				ps.setInt(1, id);
				ps.execute();
				ps.close();
				broadcast(ChannelPackets.guildDisband(id));
			}
		} catch (SQLException se) {
			System.err.println("Error saving guild to SQL" + se);
		}
	}

	public int getId() {
		return id;
	}

	public int getLeaderId() {
		return leader;
	}

	public int getGuildPoints() {
		return guildPoints;
	}

	public int getLogo() {
		return logo;
	}

	public void setLogo(final int l) {
		logo = l;
	}

	public int getLogoColor() {
		return logoColor;
	}

	public void setLogoColor(final int c) {
		logoColor = c;
	}

	public int getLogoBG() {
		return logoBG;
	}

	public void setLogoBG(final int bg) {
		logoBG = bg;
	}

	public int getLogoBGColor() {
		return logoBGColor;
	}

	public void setLogoBGColor(final int c) {
		logoBGColor = c;
	}

	public String getNotice() {
		if (notice == null) {
			return "";
		}
		return notice;
	}

	public String getName() {
		return name;
	}

	public int getCapacity() {
		return capacity;
	}

	public int getSignature() {
		return signature;
	}

	public void broadcast(final GamePacket packet) {
		broadcast(packet, -1, GuildOperationType.NONE);
	}

	public void broadcast(final GamePacket packet, final int exceptionId) {
		broadcast(packet, exceptionId, GuildOperationType.NONE);
	}

	// multi-purpose function that reaches every member of guild (except the character with exceptionId) in all channels with as little access to rmi as possible
	private void broadcast(final GamePacket packet, final int exceptionId, final GuildOperationType operation) {
		final WorldRegistry registry = GameService.getWorldRegistry();
		lock.lock();
		try {
			try {
				rebuildChannelIndex();
				final Set<Integer> activeChannels = registry.getActiveChannels();
				for (final Integer channelId : activeChannels) {
					final ChannelWorldInterface channel = registry.getChannel(channelId);
					final Collection<Integer> channelMembers = channelIndex.get(channelId);

					if (channelMembers.size() > 0) {
						if (operation == GuildOperationType.DISBAND) {
							channel.setGuildAndRank(channelMembers, 0, MemberRank.MEMBER_LOW, exceptionId);
						} else if (operation == GuildOperationType.EMBELMCHANGE) {
							channel.changeEmblem(id, channelMembers, new GuildSummary(this));
						} else {
							channel.sendPacket(channelMembers, packet, exceptionId);
						}
					}
				}
			} catch (RemoteException re) {
				System.err.println("Failed to contact channel(s) for broadcast." + re);
			}
		} finally {
			lock.unlock();
		}
	}

	private void rebuildChannelIndex() throws RemoteException {
		// any function that calls this should be wrapped in synchronized(notifications) to make sure that it doesn't change before that function finishes with the updated notifications
		if (!rebuildIndex) {
			return;
		}
		Set<Integer> activeChannels = GameService.getWorldRegistry().getActiveChannels();
		channelIndex = HashMultimap.create();
		for (Map.Entry<Integer, GuildMember> entry : members.entrySet()) {
			final GuildMember member = entry.getValue();
			if (!member.isOnline()) {
				continue;
			}

			channelIndex.put(member.getChannel(), member.getCharacterId());
		}
		channelIndex.keySet().retainAll(activeChannels);
		rebuildIndex = false;
	}

	public void guildMessage(final GamePacket packet) {
		// TODO: Not done. Complete when proper ChannelServer remoting is done.
	}

	public void setOnline(final int cid, final boolean online, final int channel) {
		final GuildMember member = getMember(cid);

		// Only broadcast whatever if something /changed/.
		// To begin with this shouldn't get called otherwise, but... *shrug*
		if (!(member.isOnline() && online)) {
			return;
		}

		member.setOnline(online);
		member.setChannel((byte) channel);
		broadcast(ChannelPackets.guildMemberOnline(id, cid, online), cid);
		rebuildIndex = true;
	}

	private GuildMember getMember(int characterId) {
		return members.get(characterId);
	}

	public void guildChat(final String name, final int cid, final String msg) {
		broadcast(ChannelPackets.multiChat(name, msg, 2), cid);
	}

	public void allianceChat(final String name, final int cid, final String msg) {
		broadcast(ChannelPackets.multiChat(name, msg, 3), cid);
	}

	public String getRankTitle(final MemberRank rank) {
		Preconditions.checkNotNull(rank);
		return rankTitles.get(rank);
	}

	// function to create guild, returns the guild id if successful, 0 if not
	public static int createGuild(final int leaderId, final String name) {
		try {
			Connection con = Database.getConnection();
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
		} catch (SQLException se) {
			System.err.println("SQL THROW" + se);
			return 0;
		}
	}

	public boolean addGuildMember(final GuildMember member) {
		lock.lock();
		try {
			if (members.size() >= capacity) {
				return false;
			}
			members.put(member.getCharacterId(), member);
			rebuildIndex = true;
		} finally {
			lock.unlock();
		}
		broadcast(ChannelPackets.newGuildMember(member));
		return true;
	}

	public void leaveGuild(final GuildMember member) {
		broadcast(ChannelPackets.memberLeft(member, false));
		lock.lock();
		try {
			members.remove(member.getCharacterId());
			rebuildIndex = true;
		} finally {
			lock.unlock();
		}
	}

	public void expelMember(final GuildMember initiator, final int targetId) {
		final GuildMember target = getMember(targetId);
		broadcast(ChannelPackets.memberLeft(target, true));
		rebuildIndex = true;
		members.remove(targetId);
		try {
			if (target.isOnline()) {
				GameService.getWorldRegistry().getChannel(target.getChannel()).setGuildAndRank(targetId, 0, MemberRank.MEMBER_LOW);
			} else {
				Notes.send(initiator.getName(), target.getName(), "You have been expelled from the guild.");
			}
		} catch (RemoteException ex) {
			System.err.println("Could not expel member: " + ex);
			return;
		}
	}

	public void changeRank(final int targetId, final MemberRank newRank) {
		final GuildMember target = getMember(targetId);
		try {
			if (target.isOnline()) {
				GameService.getWorldRegistry().getChannel(target.getChannel()).setGuildAndRank(targetId, this.id, newRank);
			}
		} catch (RemoteException ex) {
			System.err.println("Could not change rank: " + ex);
			return;
		}
		target.setGuildRank(newRank);
		broadcast(ChannelPackets.changeRank(target));
	}

	public void setGuildNotice(final String notice) {
		this.notice = notice;
		broadcast(ChannelPackets.guildNotice(id, notice));
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET notice = ? WHERE guildid = ?")) {
			ps.setString(1, notice);
			ps.setInt(2, id);
			ps.execute();
		} catch (SQLException ex) {
			System.err.println("Could not save notice: " + ex);
		}
	}

	public void updateMemberLevel(final int characterId, final int level) {
		final GuildMember member = getMember(characterId);
		member.setLevel(level);
		broadcast(ChannelPackets.guildMemberInfoUpdate(member));
	}

	public void updateMemberJob(final int characterId, final int jobId) {
		final GuildMember member = getMember(characterId);
		member.setJobId(id);
		broadcast(ChannelPackets.guildMemberInfoUpdate(member));
	}

	public void changeRankTitle(final String[] titles) {
		for (int i = 1; i <= 5; i++) {
			final MemberRank rank = MemberRank.fromNumber(i);
			rankTitles.put(rank, titles[i - 1]);
		}
		broadcast(ChannelPackets.rankTitleChange(id, titles));

		Connection con = Database.getConnection();
		try (PreparedStatement ps = con
			.prepareStatement("UPDATE guilds SET rank1title = ?, rank2title = ?, rank3title = ?, rank4title = ?, rank5title = ? WHERE guildid = ?")) {
			ps.setString(1, rankTitles.get(MemberRank.MASTER));
			ps.setString(2, rankTitles.get(MemberRank.JR_MASTER));
			ps.setString(3, rankTitles.get(MemberRank.MEMBER_HIGH));
			ps.setString(4, rankTitles.get(MemberRank.MEMBER_MIDDLE));
			ps.setString(5, rankTitles.get(MemberRank.MEMBER_LOW));
			ps.setInt(6, id);
			ps.execute();
		} catch (SQLException ex) {
			System.err.println("Could not save rank titles: " + ex);
		}
	}

	public void disbandGuild() {
		writeToDB(true);
		broadcast(null, -1, GuildOperationType.DISBAND);
	}

	public void setGuildEmblem(final short bg, final byte bgcolor, final short logo, final byte logocolor) {
		this.logoBG = bg;
		this.logoBGColor = bgcolor;
		this.logo = logo;
		this.logoColor = logocolor;
		broadcast(null, -1, GuildOperationType.EMBELMCHANGE);
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?")) {
			ps.setInt(1, logo);
			ps.setInt(2, logoColor);
			ps.setInt(3, logoBG);
			ps.setInt(4, logoBGColor);
			ps.setInt(5, id);
			ps.execute();
		} catch (SQLException ex) {
			System.err.println("Could not save guild emblem: " + ex);
		}
	}

	public boolean increaseCapacity() {
		if (capacity + GUILD_CAPACITY_STEP > GUILD_CAPACITY_MAX) {
			return false;
		}
		capacity += 5;
		broadcast(ChannelPackets.guildCapacityChange(this.id, this.capacity));
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET capacity = ? WHERE guildid = ?")) {
			ps.setInt(1, this.capacity);
			ps.setInt(2, this.id);
			ps.execute();
		} catch (SQLException ex) {
			System.err.println("Could not save guild member capacity: " + ex);
		}
		return true;
	}

	public void gainGuildPoints(final int amount) {
		guildPoints += amount;
		guildMessage(ChannelPackets.updateGuildPoints(id, guildPoints));
		Connection con = Database.getConnection();

		try (PreparedStatement ps = con.prepareStatement("UPDATE guilds SET gp = ? WHERE guildid = ?")) {
			ps.setInt(1, this.guildPoints);
			ps.setInt(2, this.id);
			ps.execute();
		} catch (SQLException e) {
			System.err.println("Saving guild point ERROR" + e);
		}
	}

	public void addMemberData(final PacketBuilder builder) {
		builder.writeAsByte(members.size());
		for (final GuildMember mgc : members.values()) {
			builder.writeInt(mgc.getCharacterId());
		}
		for (final GuildMember member : members.values()) {
			builder.writePaddedString(member.getName(), 13);
			builder.writeInt(member.getJobId());
			builder.writeInt(member.getLevel());
			builder.writeInt(member.getRank().asNumber());
			builder.writeInt(member.isOnline() ? 1 : 0);
			builder.writeInt(signature);
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