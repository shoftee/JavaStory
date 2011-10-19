/*
 * This file is part of the OdinMS Maple Story Server
 * Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 * Matthias Butz <matze@odinms.de>
 * Jan Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation. You may not use, modify
 * or distribute this program under any other version of the
 * GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.client;

public class BuddyListEntry {

	private String name, group;
	private int characterId, channelId, level, job;
	private boolean isVisible;

	/**
	 * 
	 * @param name
	 *            The name of the buddy character.
	 * @param characterId
	 *            The ID of the buddy character.
	 * @param channel
	 *            The current channel of the buddy character,
	 *            or -1 if they're offline.
	 * @param visible
	 *            Whether the buddy character is visible by the player or not.
	 */
	public BuddyListEntry(String name, int characterId, String group,
			int channel, boolean visible, int level, int job) {
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
		return channelId;
	}

	public void setChannel(int channel) {
		this.channelId = channel;
	}

	public boolean isOnline() {
		return channelId >= 0;
	}

	public void setOffline() {
		channelId = -1;
	}

	public String getName() {
		return name;
	}

	public int getCharacterId() {
		return characterId;
	}

	public void setVisible(boolean visible) {
		this.isVisible = visible;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public String getGroup() {
		return group;
	}

	public int getLevel() {
		return level;
	}

	public int getJob() {
		return job;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + characterId;
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
		final BuddyListEntry other = (BuddyListEntry) obj;
		if (characterId != other.characterId) {
			return false;
		}
		return true;
	}
}