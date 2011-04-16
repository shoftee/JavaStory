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

import java.rmi.RemoteException;
import java.util.Iterator;

import client.MapleCharacter;
import client.MapleClient;
import handling.world.guild.*;
import org.javastory.io.PacketFormatException;
import tools.MaplePacketCreator;
import org.javastory.io.PacketReader;

public class GuildHandler {
    
    public static final void handleDenyGuildInvitation(final String from, final MapleClient c) {
	final MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
	if (cfrom != null) {
	    cfrom.getClient().getSession().write(MaplePacketCreator.denyGuildInvitation(c.getPlayer().getName()));
	}
    }

    private static final boolean isGuildNameAcceptable(final String name) {
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

    private static final void respawnPlayer(final MapleCharacter mc) {
		mc.getMap().broadcastMessage(mc, MaplePacketCreator.removePlayerFromMap(mc.getId()), false);
		mc.getMap().broadcastMessage(mc, MaplePacketCreator.spawnPlayerMapobject(mc), false);
    }

    private static final class Invited {

	public String name;
	public int gid;
	public long expiration;

	public Invited(final String n, final int id) {
	    name = n.toLowerCase();
	    gid = id;
	    expiration = System.currentTimeMillis() + 60 * 60 * 1000; // 1 hr expiration
	}

	@Override
	public final boolean equals(Object other) {
	    if (!(other instanceof Invited)) {
		return false;
	    }
	    Invited oth = (Invited) other;
	    return (gid == oth.gid && name.equals(oth));
	}
    }
    private static final java.util.List<Invited> invited = new java.util.LinkedList<Invited>();
    private static long nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;

    public static final void handleGuildOperation(final PacketReader reader, final MapleClient c) throws PacketFormatException {
	if (System.currentTimeMillis() >= nextPruneTime) {
	    Iterator<Invited> itr = invited.iterator();
	    Invited inv;
	    while (itr.hasNext()) {
		inv = itr.next();
		if (System.currentTimeMillis() >= inv.expiration) {
		    itr.remove();
		}
	    }
	    nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000;
	}

	switch (reader.readByte()) {
	    case 0x02: // Create guild
		if (c.getPlayer().getGuildId() > 0 || c.getPlayer().getMapId() != 200000301) {
		    c.getPlayer().dropMessage(1, "You cannot create a new Guild while in one.");
		    return;
		} else if (c.getPlayer().getMeso() < 5000000) {
		    c.getPlayer().dropMessage(1, "You do not have enough mesos to create a Guild.");
		    return;
		}
		final String guildName = reader.readLengthPrefixedString();

		if (!isGuildNameAcceptable(guildName)) {
		    c.getPlayer().dropMessage(1, "The Guild name you have chosen is not accepted.");
		    return;
		}
		int guildId;

		try {
		    guildId = c.getChannelServer().getWorldInterface().createGuild(c.getPlayer().getId(), guildName);
		} catch (RemoteException re) {
		    System.err.println("RemoteException occurred" + re);
		    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
		    return;
		}
		if (guildId == 0) {
		    c.getSession().write(MaplePacketCreator.genericGuildMessage((byte) 0x1c));
		    return;
		}
		c.getPlayer().gainMeso(-5000000, true, false, true);
		c.getPlayer().setGuildId(guildId);
		c.getPlayer().setGuildRank(1);
		c.getPlayer().saveGuildStatus();
		c.getSession().write(MaplePacketCreator.showGuildInfo(c.getPlayer()));
		c.getPlayer().dropMessage(1, "You have successfully created a Guild.");
		respawnPlayer(c.getPlayer());
		break;
	    case 0x05: // invitation
		if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) { // 1 == guild master, 2 == jr
		    return;
		}
		String name = reader.readLengthPrefixedString();
		final MapleGuildResponse mgr = MapleGuild.sendInvite(c, name);

		if (mgr != null) {
		    c.getSession().write(mgr.getPacket());
		} else {
		    Invited inv = new Invited(name, c.getPlayer().getGuildId());
		    if (!invited.contains(inv)) {
			invited.add(inv);
		    }
		}
		break;
	    case 0x06: // accepted guild invitation
		if (c.getPlayer().getGuildId() > 0) {
		    return;
		}
		guildId = reader.readInt();
		int cid = reader.readInt();

		if (cid != c.getPlayer().getId()) {
		    return;
		}
		name = c.getPlayer().getName().toLowerCase();
		Iterator<Invited> itr = invited.iterator();

		while (itr.hasNext()) {
		    Invited inv = itr.next();
		    if (guildId == inv.gid && name.equals(inv.name)) {
			c.getPlayer().setGuildId(guildId);
			c.getPlayer().setGuildRank(5);
			itr.remove();

			int s;

			try {
			    s = c.getChannelServer().getWorldInterface().addGuildMember(c.getPlayer().getMGC());
			} catch (RemoteException e) {
			    System.err.println("RemoteException occurred while attempting to add character to guild" + e);
			    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
			    c.getPlayer().setGuildId(0);
			    return;
			}
			if (s == 0) {
			    c.getPlayer().dropMessage(1, "The Guild you are trying to join is already full.");
			    c.getPlayer().setGuildId(0);
			    return;
			}
			c.getSession().write(MaplePacketCreator.showGuildInfo(c.getPlayer()));
			c.getPlayer().saveGuildStatus();
			respawnPlayer(c.getPlayer());
			break;
		    }
		}
		break;
	    case 0x07: // leaving
		cid = reader.readInt();
		name = reader.readLengthPrefixedString();

		if (cid != c.getPlayer().getId() || !name.equals(c.getPlayer().getName()) || c.getPlayer().getGuildId() <= 0) {
		    return;
		}
		try {
		    c.getChannelServer().getWorldInterface().leaveGuild(c.getPlayer().getMGC());
		} catch (RemoteException re) {
		    System.err.println("RemoteException occurred while attempting to leave guild" + re);
		    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
		    return;
		}
		c.getSession().write(MaplePacketCreator.showGuildInfo(null));
		c.getPlayer().setGuildId(0);
		c.getPlayer().saveGuildStatus();
		respawnPlayer(c.getPlayer());
		break;
	    case 0x08: // Expel
		cid = reader.readInt();
		name = reader.readLengthPrefixedString();

		if (c.getPlayer().getGuildRank() > 2 || c.getPlayer().getGuildId() <= 0) {
		    return;
		}
		try {
		    c.getChannelServer().getWorldInterface().expelMember(c.getPlayer().getMGC(), name, cid);
		} catch (RemoteException re) {
		    System.err.println("RemoteException occurred while attempting to change rank" + re);
		    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
		    return;
		}
		break;
	    case 0x0d: // handleGuildOperation rank titles change
		if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() != 1) {
		    return;
		}
		String ranks[] = new String[5];
		for (int i = 0; i < 5; i++) {
		    ranks[i] = reader.readLengthPrefixedString();
		}

		try {
		    c.getChannelServer().getWorldInterface().changeRankTitle(c.getPlayer().getGuildId(), ranks);
		} catch (RemoteException re) {
		    System.err.println("RemoteException occurred" + re);
		    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
		    return;
		}
		break;
	    case 0x0e: // Rank change
		cid = reader.readInt();
		byte newRank = reader.readByte();

		if ((newRank <= 1 || newRank > 5) || c.getPlayer().getGuildRank() > 2 || (newRank <= 2 && c.getPlayer().getGuildRank() != 1) || c.getPlayer().getGuildId() <= 0) {
		    return;
		}

		try {
		    c.getChannelServer().getWorldInterface().changeRank(c.getPlayer().getGuildId(), cid, newRank);
		} catch (RemoteException re) {
		    System.err.println("RemoteException occurred while attempting to change rank" + re);
		    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
		    return;
		}
		break;
	    case 0x0f: // guild emblem change
		if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() != 1 || c.getPlayer().getMapId() != 200000301) {
		    return;
		}

		if (c.getPlayer().getMeso() < 15000000) {
		    c.getPlayer().dropMessage(1, "You do not have enough mesos to create a Guild.");
		    return;
		}
		final short bg = reader.readShort();
		final byte bgcolor = reader.readByte();
		final short logo = reader.readShort();
		final byte logocolor = reader.readByte();

		try {
		    c.getChannelServer().getWorldInterface().setGuildEmblem(c.getPlayer().getGuildId(), bg, bgcolor, logo, logocolor);
		} catch (RemoteException re) {
		    System.err.println("RemoteException occurred" + re);
		    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
		    return;
		}

		c.getPlayer().gainMeso(-15000000, true, false, true);
		respawnPlayer(c.getPlayer());
		break;
	    case 0x10: // guild notice change
		final String notice = reader.readLengthPrefixedString();
		if (notice.length() > 100 || c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) {
		    return;
		}
		try {
		    c.getChannelServer().getWorldInterface().setGuildNotice(c.getPlayer().getGuildId(), notice);
		} catch (RemoteException re) {
		    System.err.println("RemoteException occurred" + re);
		    c.getPlayer().dropMessage(5, "Unable to connect to the World Server. Please try again later.");
		    return;
		}
		break;
	}
    }
}
