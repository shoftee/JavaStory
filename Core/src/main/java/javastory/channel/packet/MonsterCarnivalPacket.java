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
package javastory.channel.packet;

import javastory.channel.ChannelCharacter;
import javastory.channel.server.CarnivalParty;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.handling.ServerPacketOpcode;

public class MonsterCarnivalPacket {

	/*
	 * MONSTER_CARNIVAL_START = 0xE2
	 * MONSTER_CARNIVAL_OBTAINED_CP = 0xE3
	 * MONSTER_CARNIVAL_PARTY_CP = 0xE4
	 * MONSTER_CARNIVAL_SUMMON = 0xE5
	 * MONSTER_CARNIVAL_DIED = 0xE7
	 */

	public static GamePacket startMonsterCarnival(final ChannelCharacter chr, final int enemyavailable, final int enemytotal) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MONSTER_CARNIVAL_START.getValue());
		final CarnivalParty friendly = chr.getCarnivalParty();
		builder.writeAsByte(friendly.getTeam());
		builder.writeAsShort(chr.getAvailableCP());
		builder.writeAsShort(chr.getTotalCP());
		builder.writeAsShort(friendly.getAvailableCP());
		builder.writeAsShort(friendly.getTotalCP());
		builder.writeAsShort(enemyavailable);
		builder.writeAsShort(enemytotal);
		builder.writeLong(0);
		builder.writeAsShort(0);

		return builder.getPacket();
	}

	public static GamePacket playerDiedMessage(final String name, final int lostCP, final int team) { // CPQ
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MONSTER_CARNIVAL_DIED.getValue());
		builder.writeAsByte(team); // team
		builder.writeLengthPrefixedString(name);
		builder.writeAsByte(lostCP);

		return builder.getPacket();
	}

	public static GamePacket CPUpdate(final boolean party, final int curCP, final int totalCP, final int team) {
		final PacketBuilder builder = new PacketBuilder();
		if (!party) {
			builder.writeAsShort(ServerPacketOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
		} else {
			builder.writeAsShort(ServerPacketOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
			builder.writeAsByte(team);
		}
		builder.writeAsShort(curCP);
		builder.writeAsShort(totalCP);

		return builder.getPacket();
	}

	public static GamePacket playerSummoned(final String name, final int tab, final int number) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
		builder.writeAsByte(tab);
		builder.writeAsByte(number);
		builder.writeLengthPrefixedString(name);

		return builder.getPacket();
	}
}
