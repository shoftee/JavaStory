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
package javastory.channel;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javastory.channel.maps.Door;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Party implements Serializable {
	private static final long serialVersionUID = 9179541993413738569L;
	private PartyMember leader;
	private Set<PartyMember> members = Sets.newLinkedHashSet();
	private Map<PartyMember, DoorInfo> doors = Maps.newHashMap();
	private int id;

	public Party(int id) {
		this.leader = null;
		this.id = id;
	}

	public boolean containsMember(PartyMember member) {
		Preconditions.checkNotNull(member);

		return members.contains(member);
	}

	public void addMember(PartyMember member) {
		Preconditions.checkNotNull(member);
		Preconditions.checkArgument(!members.contains(member), "This player is already in the party.");

		if (leader == null) {
			leader = member;
			leader.setLeader(true);
		}

		members.add(member);
		setNullDoor(member);
	}

	public void removeMember(PartyMember member) {
		Preconditions.checkNotNull(member);
		Preconditions.checkArgument(members.contains(member), "This player is not in the party.");

		members.remove(member);
		setNullDoor(member);
	}

	public void updateMember(PartyMember member) {
		Preconditions.checkNotNull(member);
		Preconditions.checkArgument(members.contains(member), "This player is not in the party.");

		members.remove(member);
		members.add(member);
	}

	private void setNullDoor(PartyMember owner) {
		Preconditions.checkNotNull(owner);

		doors.put(owner, DoorInfo.NONE);
	}

	public void setDoor(PartyMember owner, Door door) {
		Preconditions.checkNotNull(owner);
		Preconditions.checkNotNull(door);

		doors.put(owner, new DoorInfo(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
	}

	public void removeDoor(PartyMember owner) {
		Preconditions.checkNotNull(owner);

		setNullDoor(owner);
	}

	public ImmutableMap<PartyMember, DoorInfo> getDoors() {
		return ImmutableMap.copyOf(doors);
	}

	public PartyMember getMemberById(int id) {
		for (PartyMember chr : members) {
			if (chr.getCharacterId() == id) {
				return chr;
			}
		}
		return null;
	}

	public ImmutableList<PartyMember> getMembers() {
		return ImmutableList.copyOf(members);
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

	public void setLeader(PartyMember newLeader) {
		Preconditions.checkNotNull(newLeader);
		leader.setLeader(false);
		leader = newLeader;
		leader.setLeader(true);
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
