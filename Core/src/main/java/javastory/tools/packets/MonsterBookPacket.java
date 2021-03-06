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
package javastory.tools.packets;

import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.handling.ServerPacketOpcode;

public class MonsterBookPacket {

	public static GamePacket addCard(final boolean full, final int cardid, final int level) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MONSTERBOOK_ADD.getValue());

		if (!full) {
			builder.writeAsByte(1);
			builder.writeInt(cardid);
			builder.writeInt(level);
		} else {
			builder.writeAsByte(0);
		}

		return builder.getPacket();
	}

	public static GamePacket showGainCard(final int itemid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
		builder.writeAsByte(0);
		builder.writeAsByte(2);
		builder.writeInt(itemid);

		return builder.getPacket();
	}

	public static GamePacket showForeginCardEffect(final int id) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
		builder.writeInt(id);
		builder.writeAsByte(0x0D);

		return builder.getPacket();
	}

	public static GamePacket changeCover(final int cardid) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MONSTERBOOK_CHANGE_COVER.getValue());
		builder.writeInt(cardid);

		return builder.getPacket();
	}
}