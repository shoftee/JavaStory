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

public class Party implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private PartyMember leader;
    private List<PartyMember> members = new LinkedList<>();
    private int id;

    public Party(int id, PartyMember chrfor) {
	this.leader = chrfor;
	this.members.add(this.leader);
	this.id = id;
    }

    public boolean containsMembers(PartyMember member) {
	return members.contains(member);
    }

    public void addMember(PartyMember member) {
	members.add(member);
    }

    public void removeMember(PartyMember member) {
	members.remove(member);
    }

    public void updateMember(PartyMember member) {
	for (int i = 0; i < members.size(); i++) {
	    PartyMember chr = members.get(i);
	    if (chr.equals(member)) {
		members.set(i, member);
	    }
	}
    }

    public PartyMember getMemberById(int id) {
	for (PartyMember chr : members) {
	    if (chr.getId() == id) {
		return chr;
	    }
	}
	return null;
    }

    public Collection<PartyMember> getMembers() {
	return Collections.unmodifiableList(members);
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public PartyMember getLeader() {
	return leader;
    }

    public void setLeader(PartyMember nLeader) {
	leader = nLeader;
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
	final Party other = (Party) obj;
	if (id != other.id) {
	    return false;
	}
	return true;
    }
}
