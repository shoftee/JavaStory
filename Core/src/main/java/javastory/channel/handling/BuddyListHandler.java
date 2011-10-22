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

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.client.BuddyAddResult;
import javastory.channel.client.BuddyList;
import javastory.channel.client.BuddyListEntry;
import javastory.channel.client.BuddyOperation;
import javastory.client.SimpleCharacterInfo;
import javastory.db.Database;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.WorldChannelInterface;
import javastory.tools.packets.ChannelPackets;

public class BuddyListHandler {

	private static final class CharacterIdNameBuddyCapacity extends SimpleCharacterInfo {

		private int buddyCapacity;

		public CharacterIdNameBuddyCapacity(int id, String name, int level, int job, int buddyCapacity) {
			super(id, name, level, job);
			this.buddyCapacity = buddyCapacity;
		}

		public int getBuddyCapacity() {
			return buddyCapacity;
		}
	}

	private static void nextPendingRequest(final ChannelClient c) {
		SimpleCharacterInfo pendingBuddyRequest = c.getPlayer().getBuddyList().pollPendingRequest();
		if (pendingBuddyRequest != null) {
			c.write(ChannelPackets.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getName(), pendingBuddyRequest.getLevel(),
				pendingBuddyRequest.getJob()));
		}
	}

	private static CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(final String name) throws SQLException {
		Connection con = Database.getConnection();
		CharacterIdNameBuddyCapacity ret;
		try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name LIKE ?")) {
			ps.setString(1, name);
			try (ResultSet rs = ps.executeQuery()) {
				ret = null;
				if (rs.next()) {
					if (rs.getInt("gm") == 0) {
						ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), rs.getInt("level"), rs.getInt("job"), rs
							.getInt("buddyCapacity"));
					}
				}
			}
		}

		return ret;
	}

	public static void handleBuddyOperation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		final int mode = reader.readByte();
		final WorldChannelInterface worldInterface = ChannelServer.getWorldInterface();
		final ChannelCharacter player = c.getPlayer();
		final BuddyList buddylist = player.getBuddyList();

		if (mode == 1) { // add
			final String addName = reader.readLengthPrefixedString();
			final String groupName = reader.readLengthPrefixedString();
			final BuddyListEntry ble = buddylist.get(addName);

			if (addName.length() > 13 || groupName.length() > 16) {
				return;
			}
			if (ble != null && !ble.isVisible()) {
				c.write(ChannelPackets.buddylistMessage((byte) 13));
			} else if (buddylist.isFull()) {
				c.write(ChannelPackets.buddylistMessage((byte) 11));
			} else {
				try {
					CharacterIdNameBuddyCapacity charWithId = null;
					int channel;
					final ChannelCharacter otherChar = ChannelServer.getPlayerStorage().getCharacterByName(addName);
					if (otherChar != null) {
						channel = c.getChannelId();

						if (!otherChar.isGM() || player.isGM()) {
							charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(), otherChar.getLevel(), otherChar.getJobId(),
								otherChar.getBuddyList().getCapacity());
						}
					} else {
						channel = worldInterface.find(addName);
						charWithId = getCharacterIdAndNameFromDatabase(addName);
					}

					if (charWithId != null) {
						BuddyAddResult buddyAddResult = null;
						if (channel != -1) {
							final ChannelWorldInterface channelInterface = worldInterface.getChannelInterface(channel);
							buddyAddResult = channelInterface.requestBuddyAdd(addName, c.getChannelId(), player.getId(), player.getName(), player.getLevel(),
								player.getJobId());
						} else {
							Connection con = Database.getConnection();
							PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
							ps.setInt(1, charWithId.getId());
							ResultSet rs = ps.executeQuery();

							if (!rs.next()) {
								ps.close();
								rs.close();
								throw new RuntimeException("Result set expected");
							} else {
								int count = rs.getInt("buddyCount");
								if (count >= charWithId.getBuddyCapacity()) {
									buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
								}
							}
							rs.close();
							ps.close();

							ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
							ps.setInt(1, charWithId.getId());
							ps.setInt(2, player.getId());
							rs = ps.executeQuery();
							if (rs.next()) {
								buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
							}
							rs.close();
							ps.close();
						}
						if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
							c.write(ChannelPackets.buddylistMessage((byte) 12));
						} else {
							int displayChannel = -1;
							int otherCid = charWithId.getId();
							if (buddyAddResult == BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
								displayChannel = channel;
								notifyRemoteChannel(c, channel, otherCid, BuddyOperation.ADDED);
							} else if (buddyAddResult != BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
								Connection con = Database.getConnection();
								try (PreparedStatement ps = con
									.prepareStatement("INSERT INTO buddies (`characterid`, `buddyid`, `groupname`, `pending`) VALUES (?, ?, ?, 1)")) {
									ps.setInt(1, charWithId.getId());
									ps.setInt(2, player.getId());
									ps.setString(3, groupName);
									ps.executeUpdate();
								}
							}
							buddylist.put(new BuddyListEntry(charWithId.getName(), otherCid, groupName, displayChannel, true, charWithId.getLevel(), charWithId
								.getJob()));
							c.write(ChannelPackets.updateBuddyList(buddylist.getBuddies()));
						}
					} else {
						c.write(ChannelPackets.buddylistMessage((byte) 15));
					}
				} catch (RemoteException e) {
					System.err.println("REMOTE THROW" + e);
				} catch (SQLException e) {
					System.err.println("SQL THROW" + e);
				}
			}
		} else if (mode == 2) { // accept buddy
			int otherCid = reader.readInt();
			if (!buddylist.isFull()) {
				try {
					final int channel = worldInterface.find(otherCid);
					String otherName = null;
					int otherLevel = 0, otherJob = 0;
					final ChannelCharacter otherChar = ChannelServer.getPlayerStorage().getCharacterById(otherCid);
					if (otherChar == null) {
						Connection con = Database.getConnection();
						try (PreparedStatement ps = con.prepareStatement("SELECT name, level, job FROM characters WHERE id = ?")) {
							ps.setInt(1, otherCid);
							try (ResultSet rs = ps.executeQuery()) {
								if (rs.next()) {
									otherName = rs.getString("name");
									otherLevel = rs.getInt("level");
									otherJob = rs.getInt("job");
								}
							}
						}
					} else {
						otherName = otherChar.getName();
					}
					if (otherName != null) {
						buddylist.put(new BuddyListEntry(otherName, otherCid, "ETC", channel, true, otherLevel, otherJob));
						c.write(ChannelPackets.updateBuddyList(buddylist.getBuddies()));
						notifyRemoteChannel(c, channel, otherCid, BuddyOperation.ADDED);
					}
				} catch (RemoteException e) {
					System.err.println("REMOTE THROW" + e);
				} catch (SQLException e) {
					System.err.println("SQL THROW" + e);
				}
			}
			nextPendingRequest(c);
		} else if (mode == 3) { // delete
			final int otherCid = reader.readInt();
			if (buddylist.containsVisible(otherCid)) {
				try {
					notifyRemoteChannel(c, worldInterface.find(otherCid), otherCid, BuddyOperation.DELETED);
				} catch (RemoteException e) {
					System.err.println("REMOTE THROW" + e);
				}
			}
			buddylist.remove(otherCid);
			c.write(ChannelPackets.updateBuddyList(player.getBuddyList().getBuddies()));
			nextPendingRequest(c);
		}
	}

	private static void notifyRemoteChannel(final ChannelClient c, final int remoteChannel, final int otherCid, final BuddyOperation operation)
		throws RemoteException {
		final WorldChannelInterface worldInterface = ChannelServer.getWorldInterface();
		final ChannelCharacter player = c.getPlayer();

		if (remoteChannel != -1) {
			final ChannelWorldInterface channelInterface = worldInterface.getChannelInterface(remoteChannel);
			channelInterface.buddyChanged(otherCid, player.getId(), player.getName(), c.getChannelId(), operation, player.getLevel(), player.getJobId());
		}
	}
}
