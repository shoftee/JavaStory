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
package handling.world;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Messenger implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private List<MessengerCharacter> members = new LinkedList<MessengerCharacter>();
    private int id;
    private boolean pos0 = false;
    private boolean pos1 = false;
    @SuppressWarnings("unused")
    private boolean pos2 = false;

    public Messenger(int id, MessengerCharacter chrfor) {
	this.members.add(chrfor);
	int position = getLowestPosition();
	chrfor.setPosition(position);
	this.id = id;
    }

    public boolean containsMembers(MessengerCharacter member) {
	return members.contains(member);
    }

    public void addMember(MessengerCharacter member) {
	members.add(member);
	int position = getLowestPosition();
	member.setPosition(position);
    }

    public void removeMember(MessengerCharacter member) {
	int position = member.getPosition();
	if (position == 0) {
	    pos0 = false;
	} else if (position == 1) {
	    pos1 = false;
	} else if (position == 2) {
	    pos2 = false;
	}
	members.remove(member);
    }

    public void silentRemoveMember(MessengerCharacter member) {
	members.remove(member);
    }

    public void silentAddMember(MessengerCharacter member, int position) {
	members.add(member);
	member.setPosition(position);
    }

    public void updateMember(MessengerCharacter member) {
	for (int i = 0; i < members.size(); i++) {
	    MessengerCharacter chr = members.get(i);
	    if (chr.equals(member)) {
		members.set(i, member);
	    }
	}
    }

    public Collection<MessengerCharacter> getMembers() {
	return Collections.unmodifiableList(members);
    }

    public int getLowestPosition() {
	int position;
	if (pos0) {
	    if (pos1) {
		this.pos2 = true;
		position = 2;
	    } else {
		this.pos1 = true;
		position = 1;
	    }
	} else {
	    this.pos0 = true;
	    position = 0;
	}
	return position;
    }

    public int getPositionByName(String name) {
	for (MessengerCharacter messengerchar : members) {
	    if (messengerchar.getName().equals(name)) {
		return messengerchar.getPosition();
	    }
	}
	return 4;
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + id;
	return result;
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
}

