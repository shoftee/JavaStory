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
package javastory.world.core;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class CharacterIdChannelPair implements Externalizable {

	private int charid = 0;
	private int channel = 1;

	/**
	 * only for externalisation
	 */
	public CharacterIdChannelPair() {
	}

	public CharacterIdChannelPair(final int charid, final int channel) {
		super();
		this.charid = charid;
		this.channel = channel;
	}

	public int getCharacterId() {
		return this.charid;
	}

	public int getChannel() {
		return this.channel;
	}

	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		this.charid = in.readInt();
		this.channel = in.readByte();
	}

	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.charid);
		out.writeByte(this.channel);
	}
}
