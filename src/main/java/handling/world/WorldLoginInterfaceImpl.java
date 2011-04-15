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
package handling.world;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import handling.channel.remote.ChannelWorldInterface;
import handling.world.guild.MapleGuildCharacter;
import handling.world.remote.WorldLoginInterface;

public class WorldLoginInterfaceImpl extends UnicastRemoteObject implements WorldLoginInterface {

    private static final long serialVersionUID = -4965323089596332908L;

    public WorldLoginInterfaceImpl() throws RemoteException {
	super(0, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory());
    }

    public Properties getWorldProperties() throws RemoteException {
	return WorldServer.getInstance().getWorldProperties();
    }

    public boolean isAvailable() throws RemoteException {
	return true;
    }

    public Map<Integer, Integer> getChannelLoad() throws RemoteException {
	Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
	for (ChannelWorldInterface cwi : WorldRegistryImpl.getInstance().getAllChannelServers()) {
	    ret.put(cwi.getChannelId(), cwi.getConnected());
	}
	return ret;
    }

    @Override
    public void deleteGuildCharacter(MapleGuildCharacter mgc) throws RemoteException {
	WorldRegistryImpl wr = WorldRegistryImpl.getInstance();

	//ensure it's loaded on world server
	wr.setGuildMemberOnline(mgc, false, -1);

	if (mgc.getGuildRank() > 1) { //not leader
	    wr.leaveGuild(mgc);
	} else {
	    wr.disbandGuild(mgc.getGuildId());
	}
    }
}