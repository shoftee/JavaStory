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
package javastory.world.core;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import javastory.channel.Guild;
import javastory.channel.GuildMember;
import javastory.channel.Messenger;
import javastory.channel.MessengerMember;
import javastory.channel.Party;
import javastory.channel.PlayerBuffStorage;
import javastory.channel.client.MemberRank;
import javastory.config.ChannelInfo;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.LoginWorldInterface;
import javastory.rmi.WorldChannelInterface;

import com.google.common.collect.ImmutableSet;

public interface WorldRegistry extends Remote {

	public WorldLoginInterface registerLoginServer(LoginWorldInterface login)
			throws RemoteException;

	public WorldChannelInterface registerChannelServer(ChannelInfo info,
			ChannelWorldInterface channel) throws RemoteException;

	public void deregisterLoginServer(LoginWorldInterface cb)
			throws RemoteException;

	public void deregisterChannelServer(int channelId) throws RemoteException;

	public String getStatus() throws RemoteException;

	boolean addGuildMember(final GuildMember mgc) throws RemoteException;

	void changeRank(final int gid, final int cid, final MemberRank newRank)
			throws RemoteException;

	void changeRankTitle(final int gid, final String[] ranks)
			throws RemoteException;

	int createGuild(final int leaderId, final String name)
			throws RemoteException;

	Messenger createMessenger(final MessengerMember chrfor)
			throws RemoteException;

	Party createParty() throws RemoteException;

	void disbandGuild(final int guildId) throws RemoteException;

	Party disbandParty(final int partyid) throws RemoteException;

	void expelMember(final GuildMember initiator, final int cid)
			throws RemoteException;

	void gainGP(final int guildId, final int amount) throws RemoteException;

	ImmutableSet<Integer> getActiveChannels() throws RemoteException;

	Collection<ChannelWorldInterface> getAllChannelServers()
			throws RemoteException;

	ChannelWorldInterface getChannel(final int channel) throws RemoteException;

	Guild getGuild(final int guildId) throws RemoteException;

	List<LoginWorldInterface> getLoginServer() throws RemoteException;

	Messenger getMessenger(final int messengerid) throws RemoteException;

	Party getParty(final int partyid) throws RemoteException;

	PlayerBuffStorage getPlayerBuffStorage() throws RemoteException;

	void guildChat(final int gid, final String name, final int cid,
			final String msg) throws RemoteException;

	boolean increaseGuildCapacity(final int guildId) throws RemoteException;

	void leaveGuild(final GuildMember mgc) throws RemoteException;

	void setGuildEmblem(final int gid, final short bg, final byte bgcolor,
			final short logo, final byte logocolor) throws RemoteException;

	void setGuildMemberOnline(final GuildMember mgc, final boolean bOnline,
			final int channel) throws RemoteException;

	void setGuildNotice(final int gid, final String notice)
			throws RemoteException;

	void updateGuildMemberJob(int guildId, int characterId, int jobId)
			throws RemoteException;

	void updateGuildMemberLevel(int guildId, int characterId, int level)
			throws RemoteException;
}
