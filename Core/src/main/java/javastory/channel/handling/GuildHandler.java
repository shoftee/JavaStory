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

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.Guild;
import javastory.channel.client.MemberRank;
import javastory.io.GamePacket;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.packets.ChannelPackets;
import javastory.world.core.GuildOperationResponse;

public class GuildHandler {

	public static void handleDenyGuildInvitation(final String from, final ChannelClient c) {
		final ChannelCharacter cfrom = ChannelServer.getPlayerStorage().getCharacterByName(from);
		if (cfrom != null) {
			cfrom.getClient().write(ChannelPackets.denyGuildInvitation(c.getPlayer().getName()));
		}
	}

	private static void handleCreateGuild(final ChannelCharacter player, final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		// Create guild
		if (player.getGuildId() > 0 || player.getMapId() != 200000301) {
			player.sendNotice(1, "You cannot create a new Guild while in one.");
			return;
		} else if (player.getMeso() < 5000000) {
			player.sendNotice(1, "You do not have enough mesos to create a Guild.");
			return;
		}
		final String guildName = reader.readLengthPrefixedString();
		if (!isGuildNameAcceptable(guildName)) {
			player.sendNotice(1, "The Guild name you have chosen is not accepted.");
			return;
		}
		int guildId;
		try {
			guildId = ChannelServer.getWorldInterface().createGuild(player.getId(), guildName);
		} catch (final RemoteException re) {
			System.err.println("RemoteException occurred" + re);
			player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
			return;
		}
		if (guildId == 0) {
			c.write(ChannelPackets.genericGuildMessage((byte) 0x1c));
			return;
		}
		player.gainMeso(-5000000, true, false, true);
		player.setGuildId(guildId);
		player.setGuildRank(MemberRank.MASTER);
		player.saveGuildStatus();
		c.write(ChannelPackets.showGuildInfo(c, guildId));
		player.sendNotice(1, "You have successfully created a Guild.");
		respawnPlayer(player);
	}

	private static void handleInviteToGuild(final ChannelCharacter player, final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		// invitation
		if (player.getGuildId() <= 0 || !player.getGuildRank().isMaster()) {
			return;
		}
		final String name = reader.readLengthPrefixedString();
		final GuildOperationResponse mgr = Guild.sendInvite(c, name);
		if (mgr != null) {
			final GamePacket packet = ChannelPackets.genericGuildMessage((byte) mgr.getValue());
			c.write(packet);
		} else {
			final Invited inv = new Invited(name, player.getGuildId());
			if (!invited.contains(inv)) {
				invited.add(inv);
			}
		}
	}

	private static boolean isGuildNameAcceptable(final String name) {
		if (name.length() < 3 || name.length() > 12) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			if (!Character.isLowerCase(name.charAt(i)) && !Character.isUpperCase(name.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static void respawnPlayer(final ChannelCharacter mc) {
		mc.getMap().broadcastMessage(mc, ChannelPackets.removePlayerFromMap(mc.getId()), false);
		mc.getMap().broadcastMessage(mc, ChannelPackets.spawnPlayerMapObject(mc), false);
	}

	private static final class Invited {

		public String name;
		public int gid;
		public long expiration;

		public Invited(final String n, final int id) {
			this.name = n.toLowerCase();
			this.gid = id;
			this.expiration = System.currentTimeMillis() + 3600000;
		}

		@Override
		public final boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}

			final Invited other = (Invited) obj;
			return this.gid == other.gid && this.name.equals(other.name);
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 13 * hash + (this.name != null ? this.name.hashCode() : 0);
			hash = 13 * hash + this.gid;
			return hash;
		}
	}

	private static final List<Invited> invited = Lists.newLinkedList();
	private static long nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;

	private static void handleAcceptGuildInvite(final ChannelCharacter player, final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		// accepted guild invitation
		if (player.getGuildId() > 0) {
			return;
		}
		final int guildId = reader.readInt();
		final int targetId = reader.readInt();

		if (targetId != player.getId()) {
			return;
		}

		final String name = player.getName().toLowerCase();
		final Iterator<Invited> itr = invited.iterator();
		while (itr.hasNext()) {
			final Invited inv = itr.next();
			if (guildId == inv.gid && name.equals(inv.name)) {
				player.setGuildId(guildId);
				player.setGuildRank(MemberRank.MEMBER_LOW);
				itr.remove();

				boolean success = false;

				try {
					success = ChannelServer.getWorldInterface().addGuildMember(player.getGuildMembership());
				} catch (final RemoteException e) {
					System.err.println("RemoteException occurred while attempting to add character to guild" + e);
					player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
					player.setGuildId(0);
					return;
				}
				if (!success) {
					player.sendNotice(1, "The Guild you are trying to join is already full.");
					player.setGuildId(0);
					return;
				}
				c.write(ChannelPackets.showGuildInfo(c, guildId));
				player.saveGuildStatus();
				respawnPlayer(player);
				return;
			}
		}
	}

	public static void handleGuildOperation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		if (System.currentTimeMillis() >= nextPruneTime) {
			final Iterator<Invited> itr = invited.iterator();
			Invited inv;
			while (itr.hasNext()) {
				inv = itr.next();
				if (System.currentTimeMillis() >= inv.expiration) {
					itr.remove();
				}
			}
			nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
		}
		final ChannelCharacter player = c.getPlayer();

		String name;
		switch (reader.readByte()) {
		case 0x02:
			handleCreateGuild(player, reader, c);
			break;
		case 0x05:
			handleInviteToGuild(player, reader, c);
			break;
		case 0x06:
			handleAcceptGuildInvite(player, reader, c);
			break;
		case 0x07: // leaving
			int targetId = reader.readInt();
			name = reader.readLengthPrefixedString();

			if (targetId != player.getId() || !name.equals(player.getName()) || player.getGuildId() <= 0) {
				break;
			}
			try {
				ChannelServer.getWorldInterface().leaveGuild(player.getGuildMembership());
			} catch (final RemoteException re) {
				System.err.println("RemoteException occurred while attempting to leave guild" + re);
				player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
				break;
			}
			c.write(ChannelPackets.showNullGuildInfo());
			player.setGuildId(0);
			player.saveGuildStatus();
			respawnPlayer(player);
			break;
		case 0x08: // Expel
			targetId = reader.readInt();
			name = reader.readLengthPrefixedString();

			if (!player.getGuildRank().isMaster() || player.getGuildId() <= 0) {
				return;
			}
			try {
				ChannelServer.getWorldInterface().expelMember(player.getGuildMembership(), targetId);
			} catch (final RemoteException re) {
				System.err.println("RemoteException occurred while attempting to change rank" + re);
				player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
				return;
			}
			break;
		case 0x0d: // handleGuildOperation rank titles change
			if (player.getGuildId() <= 0 || !player.getGuildRank().equals(MemberRank.MASTER)) {
				return;
			}
			final String ranks[] = new String[5];
			for (int i = 0; i < 5; i++) {
				ranks[i] = reader.readLengthPrefixedString();
			}

			try {
				ChannelServer.getWorldInterface().changeRankTitle(player.getGuildId(), ranks);
			} catch (final RemoteException re) {
				System.err.println("RemoteException occurred" + re);
				player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
				return;
			}
			break;
		case 0x0e: // Rank change
			if (player.getGuildId() <= 0) {
				return;
			}
			targetId = reader.readInt();
			final byte newRankNumber = reader.readByte();
			final MemberRank newRank = MemberRank.fromNumber(newRankNumber);
			if (newRank == null) {
				return;
			}
			if (!player.getGuildRank().isMaster()) {
				return;
			}
			if (!player.getGuildRank().isSuperior(newRank)) {
				return;
			}

			try {
				ChannelServer.getWorldInterface().changeRank(player.getGuildId(), targetId, newRank);
			} catch (final RemoteException re) {
				System.err.println("RemoteException occurred while attempting to change rank" + re);
				player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
				return;
			}
			break;
		case 0x0f: // guild emblem change
			if (player.getMapId() != 200000301) {
				return;
			}

			if (player.getGuildId() <= 0) {
				return;
			}

			if (!player.getGuildRank().equals(MemberRank.MASTER)) {
				return;
			}

			if (player.getMeso() < 15000000) {
				player.sendNotice(1, "You do not have enough mesos to create a Guild.");
				return;
			}
			final short bg = reader.readShort();
			final byte bgcolor = reader.readByte();
			final short logo = reader.readShort();
			final byte logocolor = reader.readByte();

			try {
				ChannelServer.getWorldInterface().setGuildEmblem(player.getGuildId(), bg, bgcolor, logo, logocolor);
			} catch (final RemoteException re) {
				System.err.println("RemoteException occurred" + re);
				player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
				return;
			}

			player.gainMeso(-15000000, true, false, true);
			respawnPlayer(player);
			break;
		case 0x10: // guild notice change
			final String notice = reader.readLengthPrefixedString();
			if (notice.length() > 100 || player.getGuildId() <= 0) {
				return;
			}
			if (!player.getGuildRank().isMaster()) {
				return;
			}
			try {
				ChannelServer.getWorldInterface().setGuildNotice(player.getGuildId(), notice);
			} catch (final RemoteException re) {
				System.err.println("RemoteException occurred" + re);
				player.sendNotice(5, "Unable to connect to the World Server. Please try again later.");
				return;
			}
			break;
		}
	}
}
