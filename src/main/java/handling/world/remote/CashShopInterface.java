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
package handling.world.remote;

import java.util.List;
import java.rmi.Remote;
import java.rmi.RemoteException;

import handling.world.Party;
import handling.world.PartyMember;
import handling.world.PartyOperation;
import handling.world.CharacterTransfer;
import handling.world.guild.GuildMember;

public interface CashShopInterface extends Remote {

    public boolean isAvailable() throws RemoteException;

    public String getChannelIP(int channel) throws RemoteException;

    public boolean isCharacterListConnected(List<String> charName) throws RemoteException;

    public void ChannelChange_Data(CharacterTransfer Data, int characterid, int toChannel) throws RemoteException;

    public Party getParty(int partyid) throws RemoteException;

    public void updateParty(int partyid, PartyOperation operation, PartyMember target) throws RemoteException;

    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    public void setGuildMemberOnline(GuildMember mgc, boolean bOnline, int channel) throws RemoteException;
}
