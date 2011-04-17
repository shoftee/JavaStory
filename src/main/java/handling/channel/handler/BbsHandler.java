/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package handling.channel.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.GameClient;
import database.DatabaseConnection;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;
import tools.MaplePacketCreator;

public class BbsHandler {

    private static final String correctLength(final String in, final int maxSize) {
	if (in.length() > maxSize) {
	    return in.substring(0, maxSize);
	}
	return in;
    }

    public static final void handleBbsOperatopn(final PacketReader reader, final GameClient c) throws PacketFormatException {
	if (c.getPlayer().getGuildId() <= 0) {
	    return; // expelled while viewing bbs or hax
	}
	int localthreadid = 0;
	switch (reader.readByte()) {
	    case 0: // start a new post
		final boolean bEdit = reader.readByte() == 1 ? true : false;
		if (bEdit) {
		    localthreadid = reader.readInt();
		}
		final boolean bNotice = reader.readByte() == 1 ? true : false;
		final String title = correctLength(reader.readLengthPrefixedString(), 25);
		String text = correctLength(reader.readLengthPrefixedString(), 600);
		final int icon = reader.readInt();
		if (icon >= 0x64 && icon <= 0x6a) {
		    if (!c.getPlayer().haveItem(5290000 + icon - 0x64, 1, false, true)) {
			return; // hax, using an nx icon that s/he doesn't have
		    }
		} else if (!(icon >= 0 && icon <= 2)) {
		    return; // hax, using an invalid icon
		}
		if (!bEdit) {
		    newBBSThread(c, title, text, icon, bNotice);
		} else {
		    editBBSThread(c, title, text, icon, localthreadid);
		}
		break;
	    case 1: // delete a thread
		localthreadid = reader.readInt();
		deleteBBSThread(c, localthreadid);
		break;
	    case 2: // list threads
		int start = reader.readInt();
		listBBSThreads(c, start * 10);
		break;
	    case 3: // list thread + reply, followed by id (int)
		localthreadid = reader.readInt();
		displayThread(c, localthreadid, true);
		break;
	    case 4: // reply
		localthreadid = reader.readInt();
		text = correctLength(reader.readLengthPrefixedString(), 25);
		newBBSReply(c, localthreadid, text);
		break;
	    case 5: // delete reply
		localthreadid = reader.readInt(); // we don't use this
		int replyid = reader.readInt();
		deleteBBSReply(c, replyid);
		break;
	}
    }

    private static void listBBSThreads(GameClient c, int start) {
	try {
	    Connection con = DatabaseConnection.getConnection();
	    PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC");
	    ps.setInt(1, c.getPlayer().getGuildId());
	    ResultSet rs = ps.executeQuery();

	    c.getSession().write(MaplePacketCreator.BBSThreadList(rs, start));

	    rs.close();
	    ps.close();
	} catch (SQLException se) {
	    System.err.println("SQLException: " + se.getLocalizedMessage() + se);
	}
    }

    private static final void newBBSReply(final GameClient c, final int localthreadid, final String text) {
	if (c.getPlayer().getGuildId() <= 0) {
	    return;
	}
	Connection con = DatabaseConnection.getConnection();
	try {
	    PreparedStatement ps = con.prepareStatement("SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
	    ps.setInt(1, c.getPlayer().getGuildId());
	    ps.setInt(2, localthreadid);
	    ResultSet threadRS = ps.executeQuery();

	    if (!threadRS.next()) {
		threadRS.close();
		ps.close();
		return; // thread no longer exists, deleted?
	    }
	    int threadid = threadRS.getInt("threadid");
	    threadRS.close();
	    ps.close();

	    ps = con.prepareStatement("INSERT INTO bbs_replies (`threadid`, `postercid`, `timestamp`, `content`) VALUES " + "(?, ?, ?, ?)");
	    ps.setInt(1, threadid);
	    ps.setInt(2, c.getPlayer().getId());
	    ps.setLong(3, System.currentTimeMillis());
	    ps.setString(4, text);
	    ps.execute();
	    ps.close();

	    ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount + 1 WHERE threadid = ?");
	    ps.setInt(1, threadid);
	    ps.execute();
	    ps.close();

	    displayThread(c, localthreadid, true);
	} catch (SQLException se) {
	    System.err.println("SQLException: " + se.getLocalizedMessage() + se);
	}
    }

    private static final void editBBSThread(final GameClient c, final String title, final String text, final int icon, final int localthreadid) {
	if (c.getPlayer().getGuildId() <= 0) {
	    return; // expelled while viewing?
	}
	try {
	    Connection con = DatabaseConnection.getConnection();
	    PreparedStatement ps = con.prepareStatement("UPDATE bbs_threads SET " + "`name` = ?, `timestamp` = ?, " + "`icon` = ?, " + "`startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)");
	    ps.setString(1, title);
	    ps.setLong(2, System.currentTimeMillis());
	    ps.setInt(3, icon);
	    ps.setString(4, text);
	    ps.setInt(5, c.getPlayer().getGuildId());
	    ps.setInt(6, localthreadid);
	    ps.setInt(7, c.getPlayer().getId());
	    ps.setBoolean(8, c.getPlayer().getGuildRank() <= 2);
	    ps.execute();
	    ps.close();

	    displayThread(c, localthreadid, true);
	} catch (SQLException se) {
	    System.err.println("SQLException: " + se.getLocalizedMessage() + se);
	}
    }

    private static final void newBBSThread(final GameClient c, final String title, final String text, final int icon, final boolean bNotice) {
	if (c.getPlayer().getGuildId() <= 0) {
	    return; // expelled while viewing?
	}
	int nextId = 0;
	try {
	    Connection con = DatabaseConnection.getConnection();
	    PreparedStatement ps;

	    if (!bNotice) { // notice's local id is always 0, so we don't need to fetch it
		ps = con.prepareStatement("SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?");
		ps.setInt(1, c.getPlayer().getGuildId());
		ResultSet rs = ps.executeQuery();

		rs.next();
		nextId = rs.getInt("lastLocalId") + 1;
		rs.close();
		ps.close();
	    }

	    ps = con.prepareStatement("INSERT INTO bbs_threads (`postercid`, `name`, `timestamp`, `icon`, `startpost`, " + "`guildid`, `localthreadid`) VALUES(?, ?, ?, ?, ?, ?, ?)");
	    ps.setInt(1, c.getPlayer().getId());
	    ps.setString(2, title);
	    ps.setLong(3, System.currentTimeMillis());
	    ps.setInt(4, icon);
	    ps.setString(5, text);
	    ps.setInt(6, c.getPlayer().getGuildId());
	    ps.setInt(7, nextId);
	    ps.execute();

	    ps.close();
	    displayThread(c, nextId, true);
	} catch (SQLException se) {
	    System.err.println("SQLException: " + se.getLocalizedMessage() + se);
	}
    }

    private static final void deleteBBSThread(final GameClient c, final int localthreadid) {
	if (c.getPlayer().getGuildId() <= 0) {
	    return;
	}
	Connection con = DatabaseConnection.getConnection();
	try {
	    PreparedStatement ps = con.prepareStatement("SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
	    ps.setInt(1, c.getPlayer().getGuildId());
	    ps.setInt(2, localthreadid);
	    ResultSet threadRS = ps.executeQuery();

	    if (!threadRS.next()) {
		threadRS.close();
		ps.close();
		return; // thread no longer exists, deleted?
	    }
	    if (c.getPlayer().getId() != threadRS.getInt("postercid") && c.getPlayer().getGuildRank() > 2) {
		threadRS.close();
		ps.close();
		return; // [hax] deleting a thread that he didn't make
	    }
	    int threadid = threadRS.getInt("threadid");
	    threadRS.close();
	    ps.close();

	    ps = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?");
	    ps.setInt(1, threadid);
	    ps.execute();
	    ps.close();

	    ps = con.prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?");
	    ps.setInt(1, threadid);
	    ps.execute();
	    ps.close();
	} catch (SQLException se) {
	    System.err.println("SQLException: " + se.getLocalizedMessage() + se);
	}
    }

    private static final void deleteBBSReply(final GameClient c, final int replyid) {
	if (c.getPlayer().getGuildId() <= 0) {
	    return;
	}

	int threadid;
	Connection con = DatabaseConnection.getConnection();
	try {
	    PreparedStatement ps = con.prepareStatement("SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?");
	    ps.setInt(1, replyid);
	    ResultSet rs = ps.executeQuery();

	    if (!rs.next()) {
		rs.close();
		ps.close();
		return; // thread no longer exists, deleted?
	    }
	    if (c.getPlayer().getId() != rs.getInt("postercid") && c.getPlayer().getGuildRank() > 2) {
		rs.close();
		ps.close();
		return; // [hax] deleting a reply that he didn't make
	    }
	    threadid = rs.getInt("threadid");
	    rs.close();
	    ps.close();

	    ps = con.prepareStatement("DELETE FROM bbs_replies WHERE replyid = ?");
	    ps.setInt(1, replyid);
	    ps.execute();
	    ps.close();

	    ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount - 1 WHERE threadid = ?");
	    ps.setInt(1, threadid);
	    ps.execute();
	    ps.close();

	    displayThread(c, threadid, false);
	} catch (SQLException se) {
	    System.err.println("SQLException: " + se.getLocalizedMessage() + se);
	}
    }

    private static final void displayThread(final GameClient c, final int threadid, final boolean bIsThreadIdLocal) {
	if (c.getPlayer().getGuildId() <= 0) {
	    return;
	}
	Connection con = DatabaseConnection.getConnection();
	PreparedStatement ps = null;
	PreparedStatement ps2 = null;
	ResultSet repliesRS = null;
	ResultSet threadRS = null;

	try {
	    ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? AND " + (bIsThreadIdLocal ? "local" : "") + "threadid = ?");
	    ps.setInt(1, c.getPlayer().getGuildId());
	    ps.setInt(2, threadid);
	    threadRS = ps.executeQuery();

	    if (!threadRS.next()) {
		threadRS.close();
		ps.close();
		return; // thread no longer exists, deleted?
	    }

	    if (threadRS.getInt("replycount") > 0) {
		ps2 = con.prepareStatement("SELECT * FROM bbs_replies WHERE threadid = ?");
		ps2.setInt(1, !bIsThreadIdLocal ? threadid : threadRS.getInt("threadid"));
		repliesRS = ps2.executeQuery();
		// the lack of repliesRS.next() is intentional
	    }
	    c.getSession().write(MaplePacketCreator.showThread(bIsThreadIdLocal ? threadid : threadRS.getInt("localthreadid"), threadRS, repliesRS));

	} catch (SQLException se) {
	    System.err.println("SQLException: " + se.getLocalizedMessage() + se);
	} catch (RuntimeException re) {
	    System.err.println("The number of reply rows does not match the replycount in thread.  ThreadId = " + re.getMessage() + re);
	    try {
		ps = con.prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?");
		ps.setInt(1, Integer.parseInt(re.getMessage()));
		ps.execute();
		ps.close();

		ps = con.prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?");
		ps.setInt(1, Integer.parseInt(re.getMessage()));
		ps.execute();
		ps.close();

		if (ps != null) {
		    ps.close();
		}
		if (repliesRS != null) {
		    repliesRS.close();
		}
		if (threadRS != null) {
		    threadRS.close();
		}
		if (ps2 != null) {
		    ps2.close();
		}
	    } catch (SQLException e) {
	    }
	} finally {
	    try {
		if (ps != null) {
		    ps.close();
		}
		if (repliesRS != null) {
		    repliesRS.close();
		}
		if (threadRS != null) {
		    threadRS.close();
		}
		if (ps2 != null) {
		    ps2.close();
		}
	    } catch (SQLException ignore) {
	    }
	}
    }
}
