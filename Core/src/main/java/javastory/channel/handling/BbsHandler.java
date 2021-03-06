/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.channel.handling;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.db.Database;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.packets.ChannelPackets;

public class BbsHandler {

	private static String correctLength(final String in, final int maxSize) {
		if (in.length() > maxSize) {
			return in.substring(0, maxSize);
		}
		return in;
	}

	public static void handleBbsOperatopn(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		final ChannelCharacter player = c.getPlayer();
		if (player.getGuildId() <= 0) {
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
				if (!player.haveItem(5290000 + icon - 0x64, 1, false, true)) {
					return; // hax, using an nx icon that s/he doesn't have
				}
			} else if (!(icon >= 0 && icon <= 2)) {
				return; // hax, using an invalid icon
			}
			if (!bEdit) {
				newBbsThread(c, title, text, icon, bNotice);
			} else {
				editBbsThread(c, title, text, icon, localthreadid);
			}
			break;
		case 1: // delete a thread
			localthreadid = reader.readInt();
			deleteBbsThread(c, localthreadid);
			break;
		case 2: // list threads
			final int start = reader.readInt();
			listBBSThreads(c, start * 10);
			break;
		case 3: // list thread + reply, followed by id (int)
			localthreadid = reader.readInt();
			displayThread(c, localthreadid, true);
			break;
		case 4: // reply
			localthreadid = reader.readInt();
			text = correctLength(reader.readLengthPrefixedString(), 25);
			newBbsReply(c, localthreadid, text);
			break;
		case 5: // delete reply
			localthreadid = reader.readInt(); // we don't use this
			final int replyid = reader.readInt();
			deleteBbsReply(c, replyid);
			break;
		}
	}

	private static void listBBSThreads(final ChannelClient c, final int start) {
		try {
			final Connection con = Database.getConnection();
			final PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC");
			ps.setInt(1, c.getPlayer().getGuildId());
			final ResultSet rs = ps.executeQuery();

			c.write(ChannelPackets.showBbsThreadList(rs, start));

			rs.close();
			ps.close();
		} catch (final SQLException se) {
			System.err.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	private static void newBbsReply(final ChannelClient c, final int localthreadid, final String text) {
		final ChannelCharacter player = c.getPlayer();
		if (player.getGuildId() <= 0) {
			return;
		}
		final Connection con = Database.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT threadid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
			ps.setInt(1, player.getGuildId());
			ps.setInt(2, localthreadid);
			final ResultSet threadRS = ps.executeQuery();

			if (!threadRS.next()) {
				threadRS.close();
				ps.close();
				return; // thread no longer exists, deleted?
			}
			final int threadid = threadRS.getInt("threadid");
			threadRS.close();
			ps.close();

			ps = con.prepareStatement("INSERT INTO bbs_replies (`threadid`, `postercid`, `timestamp`, `content`) VALUES " + "(?, ?, ?, ?)");
			ps.setInt(1, threadid);
			ps.setInt(2, player.getId());
			ps.setLong(3, System.currentTimeMillis());
			ps.setString(4, text);
			ps.execute();
			ps.close();

			ps = con.prepareStatement("UPDATE bbs_threads SET replycount = replycount + 1 WHERE threadid = ?");
			ps.setInt(1, threadid);
			ps.execute();
			ps.close();

			displayThread(c, localthreadid, true);
		} catch (final SQLException se) {
			System.err.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	private static void editBbsThread(final ChannelClient c, final String title, final String text, final int icon, final int localThreadId) {
		final ChannelCharacter player = c.getPlayer();
		if (player.getGuildId() <= 0) {
			return; // expelled while viewing?
		}

		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE bbs_threads SET " + "`name` = ?, `timestamp` = ?, " + "`icon` = ?, "
			+ "`startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)")) {
			ps.setString(1, title);
			ps.setLong(2, System.currentTimeMillis());
			ps.setInt(3, icon);
			ps.setString(4, text);
			ps.setInt(5, player.getGuildId());
			ps.setInt(6, localThreadId);
			ps.setInt(7, player.getId());
			ps.setBoolean(8, player.getGuildRank().isMaster());
			ps.execute();

			displayThread(c, localThreadId, true);
		} catch (final SQLException se) {
			System.err.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	private static void newBbsThread(final ChannelClient c, final String title, final String text, final int icon, final boolean bNotice) {
		final ChannelCharacter player = c.getPlayer();
		if (player.getGuildId() <= 0) {
			return; // expelled while viewing?
		}
		int nextId = 0;
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps;

			if (!bNotice) { // notice's local id is always 0, so we don't need
							// to fetch it
				ps = con.prepareStatement("SELECT MAX(localthreadid) AS lastLocalId FROM bbs_threads WHERE guildid = ?");
				ps.setInt(1, player.getGuildId());
				final ResultSet rs = ps.executeQuery();

				rs.next();
				nextId = rs.getInt("lastLocalId") + 1;
				rs.close();
				ps.close();
			}

			ps = con.prepareStatement("INSERT INTO bbs_threads (`postercid`, `name`, `timestamp`, `icon`, `startpost`, "
				+ "`guildid`, `localthreadid`) VALUES(?, ?, ?, ?, ?, ?, ?)");
			ps.setInt(1, player.getId());
			ps.setString(2, title);
			ps.setLong(3, System.currentTimeMillis());
			ps.setInt(4, icon);
			ps.setString(5, text);
			ps.setInt(6, player.getGuildId());
			ps.setInt(7, nextId);
			ps.execute();

			ps.close();
			displayThread(c, nextId, true);
		} catch (final SQLException se) {
			System.err.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	private static void deleteBbsThread(final ChannelClient c, final int localthreadid) {
		final ChannelCharacter player = c.getPlayer();
		if (player.getGuildId() <= 0) {
			return;
		}
		final Connection con = Database.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT threadid, postercid FROM bbs_threads WHERE guildid = ? AND localthreadid = ?");
			ps.setInt(1, player.getGuildId());
			ps.setInt(2, localthreadid);
			final ResultSet threadRS = ps.executeQuery();

			if (!threadRS.next()) {
				threadRS.close();
				ps.close();
				return; // thread no longer exists, deleted?
			}
			if (player.getId() != threadRS.getInt("postercid") && !player.getGuildRank().isMaster()) {
				threadRS.close();
				ps.close();
				return; // [hax] deleting a thread that he didn't make
			}
			final int threadid = threadRS.getInt("threadid");
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
		} catch (final SQLException se) {
			System.err.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	private static void deleteBbsReply(final ChannelClient c, final int replyid) {
		final ChannelCharacter player = c.getPlayer();
		if (player.getGuildId() <= 0) {
			return;
		}

		int threadid;
		final Connection con = Database.getConnection();
		try {
			PreparedStatement ps = con.prepareStatement("SELECT postercid, threadid FROM bbs_replies WHERE replyid = ?");
			ps.setInt(1, replyid);
			final ResultSet rs = ps.executeQuery();

			if (!rs.next()) {
				rs.close();
				ps.close();
				return; // thread no longer exists, deleted?
			}
			if (player.getId() != rs.getInt("postercid") && !player.getGuildRank().isMaster()) {
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
		} catch (final SQLException se) {
			System.err.println("SQLException: " + se.getLocalizedMessage() + se);
		}
	}

	private static void displayThread(final ChannelClient c, final int threadid, final boolean bIsThreadIdLocal) {
		if (c.getPlayer().getGuildId() <= 0) {
			return;
		}
		final Connection con = Database.getConnection();
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
			c.write(ChannelPackets.showThread(bIsThreadIdLocal ? threadid : threadRS.getInt("localthreadid"), threadRS, repliesRS));

		} catch (final SQLException se) {
			System.err.println("SQLException: " + se.getLocalizedMessage() + se);
		} catch (final RuntimeException re) {
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
			} catch (final SQLException e) {
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
			} catch (final SQLException ignore) {
			}
		}
	}
}
