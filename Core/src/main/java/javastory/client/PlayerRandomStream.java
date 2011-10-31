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
package javastory.client;

import javastory.io.PacketBuilder;
import javastory.tools.Randomizer;

public class PlayerRandomStream {

	final static class SeedPack {

		public long seed1;
		public long seed2;
		public long seed3;
		public SeedPack next;

		public SeedPack(final long s1, final long s2, final long s3) {
			this.seed1 = s1 | 0x100000;
			this.seed2 = s2 | 0x1000;
			this.seed3 = s3 | 0x10;
		}

		public int roll() {
			this.next.seed1 = (this.seed1 & 0xFFFFFFFE) << 12 ^ (this.seed1 & 0x7FFC0 ^ this.seed1 >> 13) >> 6;

			this.next.seed2 = 16 * (this.seed2 & 0xFFFFFFF8) ^ (this.seed2 >> 2 ^ this.seed2 & 0x3F800000) >> 23;

			this.next.seed3 = (this.seed3 & 0xFFFFFFF0) << 17 ^ (this.seed3 >> 3 ^ this.seed3 & 0x1FFFFF00) >> 8;

			return this.next.xor();
		}

		public int xor() {
			return (int) (this.seed1 ^ this.seed2 ^ this.seed3);
		}
	}

	private transient SeedPack general;
	private transient SeedPack character;
	private transient SeedPack damage;
	private transient SeedPack monster;

	public PlayerRandomStream() {
		final int v4 = 5;
		this.initializeSeeds(Randomizer.nextInt(), 1170746341 * v4 - 755606699, 1170746341 * v4 - 755606699);
	}

	public final void initializeSeeds(final long s1, final long s2, final long s3) {
		this.general = new SeedPack(s1, s2, s3);
		this.character = new SeedPack(s1, s2, s3);
		this.damage = new SeedPack(s1, s2, s3);
		this.monster = new SeedPack(s1, s2, s3);

		this.general.next = this.character;
		this.character.next = this.damage;
		this.damage.next = this.monster;
		this.monster.next = this.character;
	}

	public final void connectData(final PacketBuilder builder) {
		final int int1 = this.general.roll();
		final int int2 = this.general.roll();
		final int int3 = this.general.roll();

		this.initializeSeeds(int1, int2, int3);

		builder.writeInt(int1);
		builder.writeInt(int2);
		builder.writeInt(int3);
	}
}
