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

import client.MapleCharacter;
import handling.MaplePacket;
import handling.ServerPacketOpcode;
import server.MapleCarnivalParty;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MonsterCarnivalPacket {

    /*    MONSTER_CARNIVAL_START = 0xE2
    MONSTER_CARNIVAL_OBTAINED_CP = 0xE3
    MONSTER_CARNIVAL_PARTY_CP = 0xE4
    MONSTER_CARNIVAL_SUMMON = 0xE5
    MONSTER_CARNIVAL_DIED = 0xE7*/

    public static MaplePacket startMonsterCarnival(final MapleCharacter chr, final int enemyavailable, final int enemytotal) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

	mplew.writeShort(ServerPacketOpcode.MONSTER_CARNIVAL_START.getValue());
        final MapleCarnivalParty friendly = chr.getCarnivalParty();
	mplew.write(friendly.getTeam());
        mplew.writeShort(chr.getAvailableCP());
        mplew.writeShort(chr.getTotalCP());
	mplew.writeShort(friendly.getAvailableCP());
        mplew.writeShort(friendly.getTotalCP());
        mplew.writeShort(enemyavailable);
        mplew.writeShort(enemytotal);
	mplew.writeLong(0);
	mplew.writeShort(0);

	return mplew.getPacket();
    }

    public static MaplePacket playerDiedMessage(String name, int lostCP, int team) { //CPQ
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

	mplew.writeShort(ServerPacketOpcode.MONSTER_CARNIVAL_DIED.getValue());
	mplew.write(team); //team
	mplew.writeMapleAsciiString(name);
	mplew.write(lostCP);

	return mplew.getPacket();
    }

    public static MaplePacket CPUpdate(boolean party, int curCP, int totalCP, int team) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
	if (!party) {
	    mplew.writeShort(ServerPacketOpcode.MONSTER_CARNIVAL_OBTAINED_CP.getValue());
	} else {
	    mplew.writeShort(ServerPacketOpcode.MONSTER_CARNIVAL_PARTY_CP.getValue());
	    mplew.write(team);
	}
	mplew.writeShort(curCP);
	mplew.writeShort(totalCP);

	return mplew.getPacket();
    }

    public static MaplePacket playerSummoned(String name, int tab, int number) {
	MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

	mplew.writeShort(ServerPacketOpcode.MONSTER_CARNIVAL_SUMMON.getValue());
	mplew.write(tab);
	mplew.write(number);
	mplew.writeMapleAsciiString(name);

	return mplew.getPacket();
    }
}
