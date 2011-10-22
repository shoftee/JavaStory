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
package javastory.channel.handling;

import java.awt.Point;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import javastory.channel.packet.MonsterCarnivalPacket;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;

public class MonsterCarnivalHandler {

	public static void handleMonsterCarnival(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		final int tab = reader.readByte();
		final int num = reader.readByte();
		final ChannelCharacter player = c.getPlayer();
		player.getMap().broadcastMessage(MonsterCarnivalPacket.playerSummoned(player.getName(), tab, num));

		if (tab == 0) { // only spawning for now..
			final Monster mob = LifeFactory.getMonster(getMonsterIdByNum(num));
			player.getMap().spawnMonsterOnGroundBelow(mob, randomizePosition(player.getMapId(), 1));
		}

	}

	private static Point randomizePosition(final int mapid, final int team) {
		int posx = 0;
		int posy = 0;
		if (mapid == 980000301) { // room 3 iirc
			posy = 162;
			if (team == 0) { // maple red goes left
				// posx = rand(-1554, -151);
			} else { // maple blue goes right
//                posx = rand(148, 1571);
			}
		}
		return new Point(posx, posy);
	}

	private static int getMonsterIdByNum(int num) {
		/*
		 * 1 - Brown Teddy - 3000005 2 - Bloctopus - 3230302 3 - Ratz - 3110102
		 * 4 - Chronos - 3230306 5 - Toy Trojan - 3230305 6 - Tick-Tock -
		 * 4230113 7 - Robo - 4230111 8 - King Bloctopus - 3230103 9 - Master
		 * Chronos - 4230115 10 - Rombot - 4130103
		 */
		int mid = 0;
		num++; // whatever, don't wanna change all the cases XD

		switch (num) {
		case 1:
			mid = 3000005;
			break;
		case 2:
			mid = 3230302;
			break;
		case 3:
			mid = 3110102;
			break;
		case 4:
			mid = 3230306;
			break;
		case 5:
			mid = 3230305;
			break;
		case 6:
			mid = 4230113;
			break;
		case 7:
			mid = 4230111;
			break;
		case 8:
			mid = 3230103;
			break;
		case 9:
			mid = 4230115;
			break;
		case 10:
			mid = 4130103;
			break;
		default:
			mid = 210100; // LOL slime.. w/e, shouldn't happen
			break;
		}
		return mid;
	}
}
