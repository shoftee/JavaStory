/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
                       Matthias Butz <matze@odinms.de>
                       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation. You may not use, modify
    or distribute this program under any other version of the
    GNU Affero General Public License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package tools.packet;

import handling.MaplePacket;
import handling.ServerPacketOpcode;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MonsterBookPacket {

    public static MaplePacket addCard(boolean full, int cardid, int level) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

	mplew.writeShort(ServerPacketOpcode.MONSTERBOOK_ADD.getValue());

        if (!full) {
            mplew.write(1);
            mplew.writeInt(cardid);
            mplew.writeInt(level);
        } else {
            mplew.write(0);
        }

	return mplew.getPacket();
    }

    public static MaplePacket showGainCard(final int itemid) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

	mplew.writeShort(ServerPacketOpcode.SHOW_STATUS_INFO.getValue());
	mplew.write(0);
	mplew.write(2);
	mplew.writeInt(itemid);

	return mplew.getPacket();
    }

    public static MaplePacket showForeginCardEffect(int id) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

	mplew.writeShort(ServerPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
	mplew.writeInt(id);
	mplew.write(0x0D);

	return mplew.getPacket();
    }

    public static MaplePacket changeCover(int cardid) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

	mplew.writeShort(ServerPacketOpcode.MONSTERBOOK_CHANGE_COVER.getValue());
	mplew.writeInt(cardid);

	return mplew.getPacket();
    }
}