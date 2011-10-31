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

package javastory.channel.anticheat;

public class CheatingOffenseEntry {

	private final CheatingOffense offense;
	private int count = 0;
	private final int characterId;
	private long lastOffense;
	private String param;
	private int recordId = -1;

	public CheatingOffenseEntry(final CheatingOffense offense, final int characterId) {
		super();
		this.offense = offense;
		this.characterId = characterId;
	}

	public CheatingOffense getOffense() {
		return this.offense;
	}

	public int getCount() {
		return this.count;
	}

	public int getChrfor() {
		return this.characterId;
	}

	public void incrementCount() {
		this.count++;
		this.lastOffense = System.currentTimeMillis();
	}

	public boolean isExpired() {
		if (this.lastOffense < System.currentTimeMillis() - this.offense.getValidityDuration()) {
			return true;
		}
		return false;
	}

	public int getPoints() {
		return this.count * this.offense.getPoints();
	}

	public String getParam() {
		return this.param;
	}

	public void setParam(final String param) {
		this.param = param;
	}

	public long getLastOffenseTime() {
		return this.lastOffense;
	}

	public int getRecordId() {
		return this.recordId;
	}

	public void setRecordId(final int id) {
		this.recordId = id;
	}

	/*
	 * @Override
	 * public int hashCode() {
	 * final int prime = 31;
	 * int result = 1;
	 * result = prime * result + ((chrfor == null) ? 0 : chrfor.getId());
	 * result = prime * result + ((offense == null) ? 0 : offense.hashCode());
	 * result = prime * result + Long.valueOf(firstOffense).hashCode();
	 * return result;
	 * }
	 * 
	 * @Override
	 * public boolean equals(Object obj) {
	 * if (this == obj) {
	 * return true;
	 * }
	 * if (obj == null) {
	 * return false;
	 * }
	 * if (getClass() != obj.getClass()) {
	 * return false;
	 * }
	 * final CheatingOffenseEntry other = (CheatingOffenseEntry) obj;
	 * if (chrfor == null) {
	 * if (other.chrfor != null) {
	 * return false;
	 * }
	 * } else if (chrfor.getId() != other.chrfor.getId()) {
	 * return false;
	 * }
	 * if (offense == null) {
	 * if (other.offense != null) {
	 * return false;
	 * }
	 * } else if (!offense.equals(other.offense)) {
	 * return false;
	 * }
	 * if (other.firstOffense != firstOffense) {
	 * return false;
	 * }
	 * return true;
	 * }
	 */
}
