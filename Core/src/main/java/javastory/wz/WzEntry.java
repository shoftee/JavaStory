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
package javastory.wz;

public class WzEntry implements WzDataEntry {

	private final String name;
	private final int size;
	private final int checksum;
	private int offset;
	private final WzDataEntity parent;

	public WzEntry(final String name, final int size, final int checksum, final WzDataEntity parent) {
		super();
		this.name = name;
		this.size = size;
		this.checksum = checksum;
		this.parent = parent;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int getSize() {
		return this.size;
	}

	@Override
	public int getChecksum() {
		return this.checksum;
	}

	@Override
	public int getOffset() {
		return this.offset;
	}

	@Override
	public WzDataEntity getParent() {
		return this.parent;
	}
}
