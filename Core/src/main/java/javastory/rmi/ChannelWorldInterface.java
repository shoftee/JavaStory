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
package javastory.rmi;


import java.rmi.RemoteException;
import java.util.Collection;

import javastory.channel.CharacterTransfer;
import javastory.channel.GuildSummary;
import javastory.channel.Messenger;
import javastory.channel.Party;
import javastory.channel.PartyMember;
import javastory.channel.client.BuddyAddResult;
import javastory.channel.client.BuddyOperation;
import javastory.channel.client.MemberRank;
import javastory.io.GamePacket;
import javastory.server.ChannelInfo;
import javastory.world.core.PartyOperation;
import javastory.world.core.WorldChannelCommonOperations;

public interface ChannelWorldInterface extends RemotePingable, WorldChannelCommonOperations {

    public int getChannelId() throws RemoteException;

    public ChannelInfo getChannelInfo() throws RemoteException;

    public String getIP() throws RemoteException;

    public boolean isConnected(int characterId) throws RemoteException;

    public int getConnected() throws RemoteException;

    public int getLocation(String name) throws RemoteException;

    public void toggleMegaphoneMuteState() throws RemoteException;

    public boolean hasMerchant(int accountId) throws RemoteException;

    public void updateParty(Party party, PartyOperation operation, PartyMember target) throws RemoteException;

    public void partyChat(Party party, String chattext, String namefrom) throws RemoteException;

    public boolean isAvailable() throws RemoteException;

    public void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation op, int level, int job) throws RemoteException;

    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom, int levelFrom, int jobFrom) throws RemoteException;

    public int[] multiBuddyFind(int charIdFrom, int[] characterIds) throws RemoteException;

    public void ChannelChange_Data(CharacterTransfer transfer, int characterid) throws RemoteException;

    public void sendPacket(Collection<Integer> targetIds, GamePacket packet, int exception) throws RemoteException;

    public void setGuildAndRank(int characterId, int guildid, MemberRank rank) throws RemoteException;

    public void setOfflineGuildStatus(int guildid, MemberRank rank, int characterId) throws RemoteException;

    public void setGuildAndRank(Collection<Integer> characters, int guildId, MemberRank rank, int exception) throws RemoteException;

    public void changeEmblem(int gid, Collection<Integer> affectedPlayers, GuildSummary mgs) throws RemoteException;

    public void addMessengerPlayer(Messenger messenger, String namefrom, int fromchannel, int position) throws RemoteException;

    public void removeMessengerPlayer(Messenger messenger, int position) throws RemoteException;

    public void messengerChat(Messenger messenger, String chattext, String namefrom) throws RemoteException;

    public void declineChat(String target, String namefrom) throws RemoteException;

    public void updateMessenger(Messenger messenger, String namefrom, int position, int fromchannel) throws RemoteException;
}
