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
package javastory.channel.client;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;

import javastory.channel.ChannelClient;
import javastory.client.SimpleCharacterInfo;
import javastory.db.Database;
import javastory.tools.packets.ChannelPackets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class BuddyList implements Serializable {
	private static final long serialVersionUID = 1413738569L;
	private final Map<Integer, BuddyListEntry> buddies = Maps.newLinkedHashMap();
	private int capacity;
	private final Deque<SimpleCharacterInfo> pendingRequests = Lists.newLinkedList();

	public BuddyList(final int capacity) {
		super();
		this.capacity = capacity;
	}

	public boolean contains(final int characterId) {
		return this.buddies.containsKey(Integer.valueOf(characterId));
	}

	public boolean containsVisible(final int characterId) {
		final BuddyListEntry ble = this.buddies.get(characterId);
		if (ble == null) {
			return false;
		}
		return ble.isVisible();
	}

	public int getCapacity() {
		return this.capacity;
	}

	public void setCapacity(final int capacity) {
		this.capacity = capacity;
	}

	public BuddyListEntry get(final int characterId) {
		return this.buddies.get(Integer.valueOf(characterId));
	}

	public BuddyListEntry get(final String characterName) {
		final String lowerCaseName = characterName.toLowerCase();
		for (final BuddyListEntry ble : this.buddies.values()) {
			if (ble.getName().toLowerCase().equals(lowerCaseName)) {
				return ble;
			}
		}
		return null;
	}

	public void put(final BuddyListEntry entry) {
		this.buddies.put(Integer.valueOf(entry.getCharacterId()), entry);
	}

	public void remove(final int characterId) {
		this.buddies.remove(Integer.valueOf(characterId));
	}

	public Collection<BuddyListEntry> getBuddies() {
		return this.buddies.values();
	}

	public boolean isFull() {
		return this.buddies.size() >= this.capacity;
	}

	public int[] getBuddyIds() {
		final int buddyIds[] = new int[this.buddies.size()];
		int i = 0;
		for (final BuddyListEntry ble : this.buddies.values()) {
			buddyIds[i++] = ble.getCharacterId();
		}
		return buddyIds;
	}

	public void loadFromTransfer(final Map<SimpleCharacterInfo, Boolean> data) {
		SimpleCharacterInfo buddyid;
		boolean pair;
		for (final Map.Entry<SimpleCharacterInfo, Boolean> qs : data.entrySet()) {
			buddyid = qs.getKey();
			pair = qs.getValue();
			if (!pair) {
				this.pendingRequests.push(buddyid);
			} else {
				this.put(new BuddyListEntry(buddyid.getName(), buddyid.getId(), "ETC", -1, true, buddyid.getLevel(), buddyid.getJob()));
			}
		}
	}

	public void loadFromDb(final int characterId) throws SQLException {
		final Connection con = Database.getConnection();
		PreparedStatement ps = con
			.prepareStatement("SELECT b.buddyid, b.pending, c.name as buddyname, c.job as buddyjob, c.level as buddylevel, b.groupname FROM buddies as b, characters as c WHERE c.id = b.buddyid AND b.characterid = ?");
		ps.setInt(1, characterId);
		final ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			final int buddyid = rs.getInt("buddyid");
			final String buddyname = rs.getString("buddyname");
			if (rs.getInt("pending") == 1) {
				this.pendingRequests.push(new SimpleCharacterInfo(buddyid, buddyname, rs.getInt("buddylevel"), rs.getInt("buddyjob")));
			} else {
				this.put(new BuddyListEntry(buddyname, buddyid, rs.getString("groupname"), -1, true, rs.getInt("buddylevel"), rs.getInt("buddyjob")));
			}
		}
		rs.close();
		ps.close();

		ps = con.prepareStatement("DELETE FROM buddies WHERE pending = 1 AND characterid = ?");
		ps.setInt(1, characterId);
		ps.executeUpdate();
		ps.close();
	}

	public SimpleCharacterInfo pollPendingRequest() {
		return this.pendingRequests.pollLast();
	}

	public void addBuddyRequest(final ChannelClient c, final int cidFrom, final String nameFrom, final int channelFrom, final int levelFrom, final int jobFrom) {
		this.put(new BuddyListEntry(nameFrom, cidFrom, "ETC", channelFrom, false, levelFrom, jobFrom));
		if (this.pendingRequests.isEmpty()) {
			c.write(ChannelPackets.requestBuddylistAdd(cidFrom, nameFrom, levelFrom, jobFrom));
		} else {
			this.pendingRequests.push(new SimpleCharacterInfo(cidFrom, nameFrom, levelFrom, jobFrom));
		}
	}
}
