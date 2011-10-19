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
package javastory.channel;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.collect.ImmutableList;

public final class Messenger implements Serializable {

    private static final int MAX_MEMBERS = 3;
    private static final long serialVersionUID = 9179541993413738569L;
    private int id;
    private MessengerMember[] m = new MessengerMember[MAX_MEMBERS];

    // TODO: Observer!
    public Messenger(int id, MessengerMember member) {
        addMember(member);
        this.id = id;
    }

    public boolean containsMembers(MessengerMember member) {
        for (int i = 0; i < MAX_MEMBERS; i++) {
            if (m[i].equals(member)) {
                return true;
            }
        }
        return false;
    }

    public void addMember(MessengerMember member) {
        int position = getLowestPosition();
        if (position == -1) {
            throw new IllegalStateException("Cannot add more members.");
        }
        m[position] = member;
        member.setPosition(position);
    }

    public void removeMember(MessengerMember member) {
        int position = member.getPosition();
        if (!m[position].equals(member)) {
            throw new IllegalArgumentException("'member' doesn't match the position.");
        }
        m[position] = null;
    }

    public void silentRemoveMember(MessengerMember member) {
        m[member.getPosition()] = null;
    }

    public void silentAddMember(MessengerMember member, int position) {
        m[position] = member;
        member.setPosition(position);
    }

    public void updateMember(MessengerMember member) {
        m[member.getPosition()] = member;
    }

    public Collection<MessengerMember> getMembers() {
        return ImmutableList.copyOf(m);
    }

    public int getLowestPosition() {
        for (int position = 0; position < MAX_MEMBERS; position++) {
            if (m[position] == null) {
                return position;
            }
        }
        return -1;
    }

    public int getPositionByName(String name) {
        for (int position = 0; position < 3; position++) {
            if (m[position].getName().equals(name)) {
                return position;
            }
        }
        return -1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Messenger other = (Messenger) obj;
        if (id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + this.id;
        return hash;
    }
}
