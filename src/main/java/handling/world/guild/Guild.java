package handling.world.guild;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import client.GameCharacter;
import client.GameClient;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import database.DatabaseConnection;
import handling.GamePacket;
import handling.channel.remote.ChannelWorldInterface;
import handling.world.WorldRegistryImpl;
import org.javastory.client.MemberRank;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import tools.MaplePacketCreator;
import org.javastory.io.PacketBuilder;

public class Guild implements java.io.Serializable {

    public static long serialVersionUID = 6322150443228168192L;
    private final List<GuildMember> members;
    private final Map<MemberRank, String> rankTitles = Maps.newEnumMap(MemberRank.class);
    private String name, notice;
    private int id, gp, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature;
    private final Map<Integer, List<Integer>> notifications = new LinkedHashMap<Integer, List<Integer>>();
    private boolean hasChanged = true;
    private int allianceid = 0;
    private GuildUnion ally;
    private Lock lock = new ReentrantLock();

    public Guild(final GuildMember initiator) {
        super();
        int guildid = initiator.getGuildId();
        members = new CopyOnWriteArrayList<GuildMember>();
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid=" +
                    guildid);
            ResultSet rs = ps.executeQuery();
            if (!rs.first()) {
                rs.close();
                ps.close();
                id = -1;
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
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
            allianceid = rs.getInt("alliance");
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT id, name, level, job, guildrank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            if (!rs.first()) {
                System.err.println("No members in guild.  Impossible...");
                rs.close();
                ps.close();
                return;
            }
            do {
                final int memberId = rs.getInt("id");
                final short memberLevel = rs.getShort("level");
                final String memberName = rs.getString("name");
                final byte memberChannelId = (byte) -1;
                final int memberJobId = rs.getInt("job");
                final MemberRank memberRank = MemberRank.fromNumber(rs.getInt("guildrank"));
                members.add(new GuildMember(memberId, memberLevel, memberName, memberChannelId, memberJobId, memberRank, guildid, false));
            } while (rs.next());
            setOnline(initiator.getId(), true, initiator.getChannel());
            rs.close();
            ps.close();
        } catch (SQLException se) {
            System.err.println("unable to read guild information from sql" + se);
            return;
        }
    }

    public Guild(final int guildid) { // retrieves the guild from database, with guildid
        members = new CopyOnWriteArrayList<GuildMember>();
        try { // first read the guild information
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid=" +
                    guildid);
            ResultSet rs = ps.executeQuery();
            if (!rs.first()) { // no result... most likely to be someone from a disbanded guild that got rolled back
                rs.close();
                ps.close();
                id = -1;
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
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
            allianceid = rs.getInt("alliance");
            rs.close();
            ps.close();
        } catch (SQLException se) {
            System.err.println("unable to read guild information from sql" + se);
            return;
        }
    }

    private void writeToDB(final boolean bDisband) {
        try {
            Connection con = DatabaseConnection.getConnection();
            if (!bDisband) {
                StringBuilder buf = new StringBuilder();
                buf.append("UPDATE guilds SET GP = ?, logo = ?, ");
                buf.append("logoColor = ?, logoBG = ?, logoBGColor = ?, ");
                buf.append("rank1title = ?, rank2title = ?, rank3title = ?, ");
                buf.append("rank4title = ?, rank5title = ?, capacity = ?, ");
                buf.append("notice = ?, alliance = ? WHERE guildid = ?");
                PreparedStatement ps = con.prepareStatement(buf.toString());
                ps.setInt(1, gp);
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
                ps.setInt(13, allianceid);
                ps.setInt(14, id);
                ps.execute();
                ps.close();
            } else {
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = 0, guildrank = 5 WHERE guildid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();
                //delete the alliance
                if (allianceid > 0) {
                    if (getUnion(null).getGuilds().get(0).getLeaderId() ==
                            getLeaderId()) {
                        ps = con.prepareStatement("DELETE FROM alliances WHERE id = ?");
                        ps.setInt(1, allianceid);
                        ps.execute();
                        ps.close();
                    }
                }
                ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();
                broadcast(MaplePacketCreator.guildDisband(id));
            }
        } catch (SQLException se) {
            System.err.println("Error saving guild to SQL" + se);
        }
    }

    public final int getId() {
        return id;
    }

    public final int getLeaderId() {
        return leader;
    }

    public final GameCharacter getLeader(final GameClient c) {
        return c.getChannelServer().getPlayerStorage().getCharacterById(leader);
    }

    public final int getGP() {
        return gp;
    }

    public final int getLogo() {
        return logo;
    }

    public final void setLogo(final int l) {
        logo = l;
    }

    public final int getLogoColor() {
        return logoColor;
    }

    public final void setLogoColor(final int c) {
        logoColor = c;
    }

    public final int getLogoBG() {
        return logoBG;
    }

    public final void setLogoBG(final int bg) {
        logoBG = bg;
    }

    public final int getLogoBGColor() {
        return logoBGColor;
    }

    public final void setLogoBGColor(final int c) {
        logoBGColor = c;
    }

    public final String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public final int getAllianceId() {
        return allianceid;
    }

    public final GuildUnion getUnion(final GameClient c) {
        if (ally != null) {
            return ally;
        } else if (allianceid > 0) {
            final GuildUnion al = new GuildUnion(c, allianceid);
            ally = al;
            return al;
        } else {
            return null;
        }
    }

    public final String getName() {
        return name;
    }

    public final int getCapacity() {
        return capacity;
    }

    public final int getSignature() {
        return signature;
    }

    public final void broadcast(final GamePacket packet) {
        broadcast(packet, -1, GuildOperationType.NONE);
    }

    public final void broadcast(final GamePacket packet, final int exception) {
        broadcast(packet, exception, GuildOperationType.NONE);
    }

    // multi-purpose function that reaches every member of guild (except the character with exceptionId) in all channels with as little access to rmi as possible
    public final void broadcast(final GamePacket packet, final int exceptionId, final GuildOperationType bcop) {
        final WorldRegistryImpl wr = WorldRegistryImpl.getInstance();
        final Set<Integer> chs = wr.getChannelServer();
        lock.lock();
        try {
            buildNotifications();
            try { // now call the channelworldinterface
                for (final Integer ch : chs) {
                    final ChannelWorldInterface cwi = wr.getChannel(ch);
                    if (notifications.get(ch).size() > 0) {
                        if (bcop == GuildOperationType.DISBAND) {
                            cwi.setGuildAndRank(notifications.get(ch), 0, MemberRank.MEMBER_LOW, exceptionId);
                        } else if (bcop == GuildOperationType.EMBELMCHANGE) {
                            cwi.changeEmblem(id, notifications.get(ch), new GuildSummary(this));
                        } else {
                            cwi.sendPacket(notifications.get(ch), packet, exceptionId);
                        }
                    }
                }
            } catch (RemoteException re) {
                System.err.println("Failed to contact channel(s) for broadcast." +
                        re);
            }
        } finally {
            lock.unlock();
        }
    }

    private void buildNotifications() {
        // any function that calls this should be wrapped in synchronized(notifications) to make sure that it doesn't change before that function finishes with the updated notifications
        if (!hasChanged) {
            return;
        }
        final Set<Integer> chs = WorldRegistryImpl.getInstance().getChannelServer();
        if (notifications.keySet().size() != chs.size()) {
            notifications.clear();
            for (final Integer ch : chs) {
                notifications.put(ch, new java.util.LinkedList<Integer>());
            }
        } else {
            for (List<Integer> l : notifications.values()) {
                l.clear();
            }
        }
        for (final GuildMember mgc : members) {
            if (!mgc.isOnline()) {
                continue;
            }
            final List<Integer> ch = notifications.get(mgc.getChannel());
            if (ch == null) {
                System.err.println("Unable to connect to channel " +
                        mgc.getChannel());
            } else {
                ch.add(mgc.getId());
            }
        }
        hasChanged = false;
    }

    public final void guildMessage(final GamePacket serverNotice) {
        for (final GuildMember mgc : members) {
            for (final ChannelServer cs : ChannelManager.getAllInstances()) {
                if (cs.getPlayerStorage().getCharacterById(mgc.getId()) != null) {
                    final GameCharacter chr = cs.getPlayerStorage().getCharacterById(mgc.getId());
                    if (serverNotice != null) {
                        chr.getClient().write(serverNotice);
                    } else {
                        chr.getMap().removePlayer(chr);
                        chr.getMap().addPlayer(chr);
                    }
                }
            }
        }
    }

    public final void setOnline(final int cid, final boolean online, final int channel) {
        boolean bBroadcast = true;
        for (final GuildMember mgc : members) {
            if (mgc.getId() == cid) {
                if (mgc.isOnline() && online) {
                    bBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel((byte) channel);
                break;
            }
        }
        if (bBroadcast) {
            broadcast(MaplePacketCreator.guildMemberOnline(id, cid, online), cid);
        }
        hasChanged = true; // member formation has changed, update notifications
    }

    public final void guildChat(final String name, final int cid, final String msg) {
        broadcast(MaplePacketCreator.multiChat(name, msg, 2), cid);
    }

    public final void allianceChat(final String name, final int cid, final String msg) {
        broadcast(MaplePacketCreator.multiChat(name, msg, 3), cid);
    }

    public final String getRankTitle(final MemberRank rank) {
        Preconditions.checkNotNull(rank);
        return rankTitles.get(rank);
    }

    // function to create guild, returns the guild id if successful, 0 if not
    public static int createGuild(final int leaderId, final String name) {
        try {
            Connection con = DatabaseConnection.getConnection();
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

    public final boolean addGuildMember(final GuildMember member) {
        // first of all, insert it into the members keeping alphabetical order of lowest ranks ;)
        lock.lock();
        try {
            if (members.size() >= capacity) {
                return false;
            }
            members.add(member);
            hasChanged = true;
        } finally {
            lock.unlock();
        }
        broadcast(MaplePacketCreator.newGuildMember(member));
        return true;
    }

    public final void leaveGuild(final GuildMember member) {
        broadcast(MaplePacketCreator.memberLeft(member, false));
        lock.lock();
        try {
            members.remove(member);
            hasChanged = true;
        } finally {
            lock.unlock();
        }
    }

    public final void expelMember(final GuildMember initiator, final String name, final int targetId) {
        final Iterator<GuildMember> itr = members.iterator();
        while (itr.hasNext()) {
            final GuildMember member = itr.next();
            if (member.getId() == targetId &&
                    initiator.getRank().isSuperior(member.getRank())) {
                broadcast(MaplePacketCreator.memberLeft(member, true));
                hasChanged = true;
                members.remove(member);
                try {
                    if (member.isOnline()) {
                        WorldRegistryImpl.getInstance().getChannel(member.getChannel()).setGuildAndRank(targetId, 0, MemberRank.MEMBER_LOW);
                    } else {
                        try {
                            Connection con = DatabaseConnection.getConnection();
                            PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
                            ps.setString(1, member.getName());
                            ps.setString(2, initiator.getName());
                            ps.setString(3, "You have been expelled from the guild.");
                            ps.setLong(4, System.currentTimeMillis());
                            ps.executeUpdate();
                            ps.close();
                        } catch (SQLException e) {
                            System.err.println("Error sending guild msg 'expelled'." +
                                    e);
                        }
                        WorldRegistryImpl.getInstance().getChannel(1).setOfflineGuildStatus((short) 0, MemberRank.MEMBER_LOW, targetId);
                    }
                } catch (RemoteException re) {
                    re.printStackTrace();
                    return;
                }
            }
        }
    }

    public final void changeRank(final int targetId, final MemberRank newRank) {
        for (final GuildMember member : members) {
            if (targetId == member.getId()) {
                try {
                    if (member.isOnline()) {
                        WorldRegistryImpl.getInstance().getChannel(member.getChannel()).setGuildAndRank(targetId, this.id, newRank);
                    } else {
                        WorldRegistryImpl.getInstance().getChannel(1).setOfflineGuildStatus((short) this.id, newRank, targetId);
                    }
                } catch (RemoteException re) {
                    re.printStackTrace();
                    return;
                }
                member.setGuildRank(newRank);
                broadcast(MaplePacketCreator.changeRank(member));
                return;
            }
        }
        // it should never get to this point unless cid was incorrect o_O
        System.err.println("INFO: unable to find the correct id for changeRank({" +
                targetId + "}, {" + newRank + "})");
    }

    public final void setGuildNotice(final String notice) {
        this.notice = notice;
        broadcast(MaplePacketCreator.guildNotice(id, notice));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET notice = ? WHERE guildid = ?");
            ps.setString(1, notice);
            ps.setInt(2, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Saving notice ERROR" + e);
        }
    }

    public final void createAlliance(final GameClient c, final String name) {
        if (allianceid != 0) {
            c.getPlayer().sendNotice(1, "You are already in an Alliance!");
            return;
        }
        if (checkAllianceName(name)) {
            try {
                if (name.equals("") || id <= 0) {
                    return;
                }
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO `alliances` (notice, name, guild1, guild2, guild3, guild4, guild5, rank1, rank2, rank3, rank4, rank5) VALUES ('', ?, ?, 0, 0, 0, 0, 'Master', 'Jr. Master', 'Member', 'Member', 'Member')");
                ps.setString(1, name);
                ps.setInt(2, id);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("SELECT id FROM alliances WHERE guild1 = ?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    allianceid = rs.getInt("id");
                }
                rs.close();
                ps.close();
                writeToDB(false);
                c.getPlayer().sendNotice(1, "Alliance successfully created!");
            } catch (SQLException a) {
                //
            }
        } else {
            c.getPlayer().sendNotice(1, "This name already exists.");
        }
    }

    public final void memberLevelJobUpdate(final GuildMember mgc) {
        for (final GuildMember member : members) {
            if (member.getId() == mgc.getId()) {
                member.setJobId(mgc.getJobId());
                member.setLevel((short) mgc.getLevel());
                broadcast(MaplePacketCreator.guildMemberLevelJobUpdate(mgc));
                break;
            }
        }
    }

    public final void changeRankTitle(final String[] titles) {
        for (int i = 1; i <= 5; i++) {
            final MemberRank rank = MemberRank.fromNumber(i);
            rankTitles.put(rank, titles[i - 1]);
        }
        broadcast(MaplePacketCreator.rankTitleChange(id, titles));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET rank1title = ?, rank2title = ?, rank3title = ?, rank4title = ?, rank5title = ? WHERE guildid = ?");
            ps.setString(1, rankTitles.get(MemberRank.MASTER));
            ps.setString(2, rankTitles.get(MemberRank.JR_MASTER));
            ps.setString(3, rankTitles.get(MemberRank.MEMBER_HIGH));
            ps.setString(4, rankTitles.get(MemberRank.MEMBER_MIDDLE));
            ps.setString(5, rankTitles.get(MemberRank.MEMBER_LOW));
            ps.setInt(6, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Saving rankTitle ERROR" + e);
        }
    }

    public final void disbandGuild() {
        writeToDB(true);
        broadcast(null, -1, GuildOperationType.DISBAND);
    }

    public final void setGuildEmblem(final short bg, final byte bgcolor, final short logo, final byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        broadcast(null, -1, GuildOperationType.EMBELMCHANGE);
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?");
            ps.setInt(1, logo);
            ps.setInt(2, logoColor);
            ps.setInt(3, logoBG);
            ps.setInt(4, logoBGColor);
            ps.setInt(5, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Saving guild logo / BG colo ERROR" + e);
        }
    }

    public final GuildMember getMember(final int cid) {
        for (final GuildMember mgc : members) {
            if (mgc.getId() == cid) {
                return mgc;
            }
        }
        return null;
    }

    public final boolean increaseCapacity() {
        if (capacity >= 100 || ((capacity + 5) > 100)) {
            return false;
        }
        capacity += 5;
        broadcast(MaplePacketCreator.guildCapacityChange(this.id, this.capacity));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET capacity = ? WHERE guildid = ?");
            ps.setInt(1, this.capacity);
            ps.setInt(2, this.id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Saving guild capacity ERROR" + e);
        }
        return true;
    }

    public final void gainGP(final int amount) {
        gp += amount;
        guildMessage(MaplePacketCreator.updateGP(id, gp));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET gp = ? WHERE guildid = ?");
            ps.setInt(1, this.gp);
            ps.setInt(2, this.id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Saving guild point ERROR" + e);
        }
    }

    public final void addMemberData(final PacketBuilder builder) {
        builder.writeAsByte(members.size());
        for (final GuildMember mgc : members) {
            builder.writeInt(mgc.getId());
        }
        for (final GuildMember member : members) {
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
    public static GuildOperationResponse sendInvite(final GameClient c, final String targetName) {
        final GameCharacter mc = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
        if (mc == null) {
            return GuildOperationResponse.NOT_IN_CHANNEL;
        }
        if (mc.getGuildId() > 0) {
            return GuildOperationResponse.ALREADY_IN_GUILD;
        }
        mc.getClient().write(MaplePacketCreator.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName(), c.getPlayer().getLevel(), c.getPlayer().getJob()));
        return null;
    }

    public final boolean checkAllianceName(final String name) {
        boolean canCreate = true;
        if (name.length() < 4 && name.length() > 13) {
            canCreate = false;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM alliances WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.first()) {
                canCreate = false;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            //
        }
        return canCreate;
    }
}