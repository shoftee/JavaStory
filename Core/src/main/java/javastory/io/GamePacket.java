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
package javastory.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents a game packet.
 * 
 * @author shoftee
 * 
 */
public final class GamePacket implements Externalizable {

	/**
	 * Constructs a new instance of GamePacket and copies the specified byte
	 * array into it. Changes to the byte array will not affect the packet data.
	 * 
	 * @param bytes
	 *            the data for the constructed packet.
	 * @return the new GamePacket instance.
	 */
	public static GamePacket copyFrom(byte[] bytes) {
		int length = bytes.length;
		byte[] copy = new byte[length];
		System.arraycopy(bytes, 0, copy, 0, length);
		return new GamePacket(copy);
	}

	/**
	 * Constructs a new instance of GamePacket and wraps it around the specified
	 * byte array. Changes to the byte array will change the packet.
	 * 
	 * @param bytes
	 *            the data for the constructed packet.
	 * @return the new GamePacket instance.
	 */
	public static GamePacket wrapperOf(byte[] bytes) {
		return new GamePacket(bytes);
	}

	private byte[] bytes;

	private GamePacket(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * Gets the underlying byte array for this packet.
	 * 
	 * @return the bytes of this packet.
	 */
	public byte[] getBytes() {
		return bytes;
	}

	/**
	 * Gets a copy of the underlying byte array for this packet.
	 * 
	 * @return a copy of the internal byte array.
	 */
	public byte[] getCopy() {
		final int length = this.bytes.length;
		byte[] copy = new byte[length];
		System.arraycopy(this.bytes, 0, copy, 0, length);
		return copy;
	}

	@Override
	public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
		int length = input.read();

		this.bytes = new byte[length];

		int read = 0;
		do {
			read += input.read(this.bytes, read, length - read);
		} while (read < length);
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.write(this.bytes.length);
		// TODO Auto-generated method stub
		output.write(this.bytes);
	}
}
