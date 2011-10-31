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
package javastory.channel.maps;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelServer;
import javastory.io.GamePacket;
import javastory.rmi.WorldChannelInterface;
import javastory.server.TimerManager;
import javastory.tools.packets.ChannelPackets;

public class TVEffect {

	private static List<String> message = new LinkedList<>();
	private static ChannelCharacter user;
	private static boolean active;
	private static int type;
	private static ChannelCharacter partner = null;

	public TVEffect(final ChannelCharacter User, final ChannelCharacter Partner, final List<String> Msg, final int Type) {
		message = Msg;
		user = User;
		type = Type;
		partner = Partner;
		broadCastTV(true);
	}

	public static boolean isActive() {
		return active;
	}

	private static void setActive(final boolean set) {
		active = set;
	}

	private static GamePacket removeTV() {
		return ChannelPackets.removeTV();
	}

	public static GamePacket startTV() {
		return ChannelPackets.sendTV(user, message, type <= 2 ? type : type - 3, partner);
	}

	public static void broadCastTV(final boolean isActive) {
		setActive(isActive);
		final WorldChannelInterface wci = ChannelServer.getWorldInterface();
		try {
			if (isActive) {
				wci.broadcastMessage(ChannelPackets.enableTV());
				wci.broadcastMessage(startTV());

				TimerManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						broadCastTV(false);
					}
				}, getDelayTime(type));

			} else {
				wci.broadcastMessage(removeTV());
			}
		} catch (final RemoteException e) {
		}
	}

	public static int getDelayTime(final int type) {
		switch (type) {
		case 0:
		case 3:
			return 15000;
		case 1:
		case 4:
			return 30000;
		case 2:
		case 5:
			return 60000;
		}
		return 0;
	}
}
