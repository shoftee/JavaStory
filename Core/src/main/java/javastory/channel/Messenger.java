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
import java.util.Collection;

import com.google.common.collect.ImmutableList;

public final class Messenger implements Serializable {

	private static final int MAX_MEMBERS = 3;
	private static final long serialVersionUID = 9179541993413738569L;
	private int id;
	private final MessengerMember[] m = new MessengerMember[MAX_MEMBERS];

	// TODO: Observer!
	public Messenger(final int id, final MessengerMember member) {
		this.addMember(member);
		this.id = id;
	}

	public boolean containsMembers(final MessengerMember member) {
		for (int i = 0; i < MAX_MEMBERS; i++) {
			if (this.m[i].equals(member)) {
				return true;
			}
		}
		return false;
	}

	public void addMember(final MessengerMember member) {
		final int position = this.getLowestPosition();
		if (position == -1) {
			throw new IllegalStateException("Cannot add more members.");
		}
		this.m[position] = member;
		member.setPosition(position);
	}

	public void removeMember(final MessengerMember member) {
		final int position = member.getPosition();
		if (!this.m[position].equals(member)) {
			throw new IllegalArgumentException("'member' doesn't match the position.");
		}
		this.m[position] = null;
	}

	public void silentRemoveMember(final MessengerMember member) {
		this.m[member.getPosition()] = null;
	}

	public void silentAddMember(final MessengerMember member, final int position) {
		this.m[position] = member;
		member.setPosition(position);
	}

	public void updateMember(final MessengerMember member) {
		this.m[member.getPosition()] = member;
	}

	public Collection<MessengerMember> getMembers() {
		return ImmutableList.copyOf(this.m);
	}

	public int getLowestPosition() {
		for (int position = 0; position < MAX_MEMBERS; position++) {
			if (this.m[position] == null) {
				return position;
			}
		}
		return -1;
	}

	public int getPositionByName(final String name) {
		for (int position = 0; position < 3; position++) {
			if (this.m[position].getName().equals(name)) {
				return position;
			}
		}
		return -1;
	}

	public int getId() {
		return this.id;
	}

	public void setId(final int id) {
		this.id = id;
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
		final Messenger other = (Messenger) obj;
		if (this.id != other.id) {
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
