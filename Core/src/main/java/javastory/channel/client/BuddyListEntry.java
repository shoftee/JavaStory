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

public class BuddyListEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3264298238627998238L;
	
	private final String name, group;
	private final int characterId;
	private int channelId;
	private final int level;
	private final int job;
	private boolean isVisible;

	/**
	 * 
	 * @param name
	 *            The name of the buddy character.
	 * @param characterId
	 *            The ID of the buddy character.
	 * @param channel
	 *            The current channel of the buddy character, or -1 if they're
	 *            offline.
	 * @param visible
	 *            Whether the buddy character is visible by the player or not.
	 */
	public BuddyListEntry(final String name, final int characterId, final String group, final int channel, final boolean visible, final int level, final int job) {
		super();
		this.name = name;
		this.characterId = characterId;
		this.group = group;
		this.channelId = channel;
		this.isVisible = visible;
		this.level = level;
		this.job = job;
	}

	/**
	 * @return the channel the character is on. If the character is offline
	 *         returns -1.
	 */
	public int getChannel() {
		return this.channelId;
	}

	public void setChannel(final int channel) {
		this.channelId = channel;
	}

	public boolean isOnline() {
		return this.channelId >= 0;
	}

	public void setOffline() {
		this.channelId = -1;
	}

	public String getName() {
		return this.name;
	}

	public int getCharacterId() {
		return this.characterId;
	}

	public void setVisible(final boolean visible) {
		this.isVisible = visible;
	}

	public boolean isVisible() {
		return this.isVisible;
	}

	public String getGroup() {
		return this.group;
	}

	public int getLevel() {
		return this.level;
	}

	public int getJob() {
		return this.job;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.characterId;
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
		final BuddyListEntry other = (BuddyListEntry) obj;
		if (this.characterId != other.characterId) {
			return false;
		}
		return true;
	}
}