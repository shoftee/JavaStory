/*
 * This file is part of the OdinMS Maple Story Server
 * Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 * Matthias Butz <matze@odinms.de>
 * Jan Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation. You may not use, modify
 * or distribute this program under any other version of the
 * GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.channel.handling;

import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.scripting.EventManager;
import javastory.scripting.NpcScriptManager;
import javastory.tools.packets.ChannelPackets;

public class UserInterfaceHandler {

	public static final void handleNpcRequestCygnusSummon(final ChannelClient c) {
		NpcScriptManager.getInstance().start(c, 1101008);
	}

	/*
	 * public static final void InGame_Poll(final SeekableLittleEndianAccessor
	 * reader, final GameClient c) {
	 * if (ServerConstants.PollEnabled) {
	 * reader.skip(4);
	 * final int selection = reader.readInt();
	 * 
	 * if (selection >= 0 && selection <= ServerConstants.Poll_Answers.length) {
	 * if (MapleCharacterUtil.SetPoll(c.getAccID(), selection)) {
	 * // c.write(MaplePacketCreator.InGame_Poll_Reply());
	 * }
	 * }
	 * }
	 * }
	 */

	public static final void handleShipObjectRequest(final int mapid, final ChannelClient c) {
		// BB 00 6C 24 05 06 00 - Ellinia
		// BB 00 6E 1C 4E 0E 00 - Leafre

		EventManager em;
		int effect = 3; // 1 = Coming, 3 = going, 1034 = balrog

		switch (mapid) {
		case 101000300: // Ellinia Station >> Orbis
		case 200000111: // Orbis Station >> Ellinia
			em = ChannelServer.getInstance().getEventSM().getEventManager("Boats");
			if (em.getProperty("docked").equals("true")) {
				effect = 1;
			}
			break;
		case 200000121: // Orbis Station >> Ludi
		case 220000110: // Ludi Station >> Orbis
			em = ChannelServer.getInstance().getEventSM().getEventManager("Trains");
			if (em.getProperty("docked").equals("true")) {
				effect = 1;
			}
			break;
		case 200000151: // Orbis Station >> Ariant
		case 260000100: // Ariant Station >> Orbis
			em = ChannelServer.getInstance().getEventSM().getEventManager("Geenie");
			if (em.getProperty("docked").equals("true")) {
				effect = 1;
			}
			break;
		case 240000110: // Leafre Station >> Orbis
		case 200000131: // Orbis Station >> Leafre
			em = ChannelServer.getInstance().getEventSM().getEventManager("Flight");
			if (em.getProperty("docked").equals("true")) {
				effect = 1;
			}
			break;
		case 200090010: // During the ride to Orbis
		case 200090000: // During the ride to Ellinia
			em = ChannelServer.getInstance().getEventSM().getEventManager("Boats");
			if (em.getProperty("haveBalrog").equals("true")) {
				effect = 1;
			} else {
				return; // shyt, fixme!
			}
			break;
		default:
			System.out.println("Unhandled ship object, MapID : " + mapid);
			break;
		}
		c.write(ChannelPackets.boatPacket(effect));
	}
}
