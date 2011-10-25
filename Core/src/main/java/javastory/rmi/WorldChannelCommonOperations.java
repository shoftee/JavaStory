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
package javastory.rmi;

import java.rmi.RemoteException;
import java.util.List;

import javastory.io.GamePacket;
import javastory.world.core.CheaterData;

public interface WorldChannelCommonOperations extends RemotePingable {

	// TODO: Broadcasts should be more generic.
	public void broadcastMessage(GamePacket packet) throws RemoteException;

	public void broadcastSmega(GamePacket packet) throws RemoteException;

	public void broadcastGMMessage(GamePacket packet) throws RemoteException;

	public void whisper(String sender, String target, int channel, String message) throws RemoteException;

	public void shutdown(int time) throws RemoteException;

	public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException;

	public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException;

	public List<CheaterData> getCheaters() throws RemoteException;

	// TODO: BuddyRegistry stuff.
	public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException;

	// TODO: MessengerRegistry stuff.
	public void messengerInvite(String sender, int messengerid, String target, int fromchannel) throws RemoteException;

	// TODO: These two are not too useful. Remove them maybe?
	public boolean isConnected(String charName) throws RemoteException;

	public boolean isCharacterListConnected(List<String> charName) throws RemoteException;

	// TODO: AccountRegistry later.
	public boolean hasMerchant(int accountId) throws RemoteException;

}
