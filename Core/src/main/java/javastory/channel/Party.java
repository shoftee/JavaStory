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
	private final Set<PartyMember> members = Sets.newLinkedHashSet();
	private final Map<PartyMember, DoorInfo> doors = Maps.newHashMap();
	private int id;

	public Party(final int id) {
		this.leader = null;
		this.id = id;
	}

	public boolean containsMember(final PartyMember member) {
		Preconditions.checkNotNull(member);

		return this.members.contains(member);
	}

	public void addMember(final PartyMember member) {
		Preconditions.checkNotNull(member);
		Preconditions.checkArgument(!this.members.contains(member), "This player is already in the party.");

		if (this.leader == null) {
			this.leader = member;
			this.leader.setLeader(true);
		}

		this.members.add(member);
		this.setNullDoor(member);
	}

	public void removeMember(final PartyMember member) {
		Preconditions.checkNotNull(member);
		Preconditions.checkArgument(this.members.contains(member), "This player is not in the party.");

		this.members.remove(member);
		this.setNullDoor(member);
	}

	public void updateMember(final PartyMember member) {
		Preconditions.checkNotNull(member);
		Preconditions.checkArgument(this.members.contains(member), "This player is not in the party.");

		this.members.remove(member);
		this.members.add(member);
	}

	private void setNullDoor(final PartyMember owner) {
		Preconditions.checkNotNull(owner);

		this.doors.put(owner, DoorInfo.NONE);
	}

	public void setDoor(final PartyMember owner, final Door door) {
		Preconditions.checkNotNull(owner);
		Preconditions.checkNotNull(door);

		this.doors.put(owner, new DoorInfo(door.getTown().getId(), door.getTarget().getId(), door.getTargetPosition()));
	}

	public void removeDoor(final PartyMember owner) {
		Preconditions.checkNotNull(owner);

		this.setNullDoor(owner);
	}

	public ImmutableMap<PartyMember, DoorInfo> getDoors() {
		return ImmutableMap.copyOf(this.doors);
	}

	public PartyMember getMemberById(final int id) {
		for (final PartyMember chr : this.members) {
			if (chr.getCharacterId() == id) {
				return chr;
			}
		}
		return null;
	}

	public ImmutableList<PartyMember> getMembers() {
		return ImmutableList.copyOf(this.members);
	}

	public int getId() {
		return this.id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public PartyMember getLeader() {
		return this.leader;
	}

	public void setLeader(final PartyMember newLeader) {
		Preconditions.checkNotNull(newLeader);
		this.leader.setLeader(false);
		this.leader = newLeader;
		this.leader.setLeader(true);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.id;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Party other = (Party) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}
}
