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
package javastory.channel.client;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import javastory.channel.ChannelClient;
import javastory.db.DatabaseConnection;
import tools.MaplePacketCreator;
import client.BuddyListEntry;
import client.SimpleCharacterInfo;

public class BuddyList implements Serializable {
    private static final long serialVersionUID = 1413738569L;
    private Map<Integer, BuddyListEntry> buddies = new LinkedHashMap<>();
    private int capacity;
    private Deque<SimpleCharacterInfo> pendingRequests = new LinkedList<>();

    public BuddyList(int capacity) {
        super();
        this.capacity = capacity;
    }

    public boolean contains(int characterId) {
        return buddies.containsKey(Integer.valueOf(characterId));
    }

    public boolean containsVisible(int characterId) {
        BuddyListEntry ble = buddies.get(characterId);
        if (ble == null) {
            return false;
        }
        return ble.isVisible();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BuddyListEntry get(int characterId) {
        return buddies.get(Integer.valueOf(characterId));
    }

    public BuddyListEntry get(String characterName) {
        String lowerCaseName = characterName.toLowerCase();
        for (BuddyListEntry ble : buddies.values()) {
            if (ble.getName().toLowerCase().equals(lowerCaseName)) {
                return ble;
            }
        }
        return null;
    }

    public void put(BuddyListEntry entry) {
        buddies.put(Integer.valueOf(entry.getCharacterId()), entry);
    }

    public void remove(int characterId) {
        buddies.remove(Integer.valueOf(characterId));
    }

    public Collection<BuddyListEntry> getBuddies() {
        return buddies.values();
    }

    public boolean isFull() {
        return buddies.size() >= capacity;
    }

    public int[] getBuddyIds() {
        int buddyIds[] = new int[buddies.size()];
        int i = 0;
        for (BuddyListEntry ble : buddies.values()) {
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
                pendingRequests.push(buddyid);
            } else {
                put(new BuddyListEntry(buddyid.getName(), buddyid.getId(), "ETC", -1, true, buddyid.getLevel(), buddyid.getJob()));
            }
        }
    }

    public void loadFromDb(int characterId) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT b.buddyid, b.pending, c.name as buddyname, c.job as buddyjob, c.level as buddylevel, b.groupname FROM buddies as b, characters as c WHERE c.id = b.buddyid AND b.characterid = ?");
        ps.setInt(1, characterId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int buddyid = rs.getInt("buddyid");
            String buddyname = rs.getString("buddyname");
            if (rs.getInt("pending") == 1) {
                pendingRequests.push(new SimpleCharacterInfo(buddyid, buddyname, rs.getInt("buddylevel"), rs.getInt("buddyjob")));
            } else {
                put(new BuddyListEntry(buddyname, buddyid, rs.getString("groupname"), -1, true, rs.getInt("buddylevel"), rs.getInt("buddyjob")));
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
        return pendingRequests.pollLast();
    }

    public void addBuddyRequest(ChannelClient c, int cidFrom, String nameFrom, int channelFrom, int levelFrom, int jobFrom) {
        put(new BuddyListEntry(nameFrom, cidFrom, "ETC", channelFrom, false, levelFrom, jobFrom));
        if (pendingRequests.isEmpty()) {
            c.write(MaplePacketCreator.requestBuddylistAdd(cidFrom, nameFrom, levelFrom, jobFrom));
        } else {
            pendingRequests.push(new SimpleCharacterInfo(cidFrom, nameFrom, levelFrom, jobFrom));
        }
    }
}
