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
package handling.channel.handler;

import org.javastory.client.ChannelClient;
import handling.world.GuildUnion;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;

public class AllianceHandler {

    public static final void handleAllianceOperation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
	final byte mode = reader.readByte();

	final GuildUnion alliance = new GuildUnion(c, c.getChannelServer().getGuildSummary(c.getPlayer().getGuildId()).getAllianceId());

	switch (mode) {
	    case 0x01: // show info?
		//c.write(MaplePacketCreator.showAllianceInfo(c.getPlayer()));
		//c.write(MaplePacketCreator.showAllianceMembers(c.getPlayer()));
		break;
	    case 0x08: // change titles
		String[] ranks = new String[5];
		for (int i = 0; i < 5; i++) {
		    ranks[i] = reader.readLengthPrefixedString();
		}
		alliance.setTitles(ranks);
		break;
	    case 0x0A: // change notice
		String notice = reader.readLengthPrefixedString(); // new notice (100 is de max)
		alliance.setNotice(notice);
		break;
	    default:
		System.out.println("Unknown Alliance operation:\r\n" + reader.toString());
		break;
	}
    }
}
