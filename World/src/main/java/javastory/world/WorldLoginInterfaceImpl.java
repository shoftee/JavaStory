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
package javastory.world;

import javastory.world.core.WorldRegistry;
import java.rmi.RemoteException;
import com.google.common.collect.ImmutableMap;

import javastory.rmi.GenericRemoteObject;
import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.WorldLoginInterface;

public class WorldLoginInterfaceImpl extends GenericRemoteObject implements WorldLoginInterface {

    private static final long serialVersionUID = -4965323089596332908L;
    private final WorldRegistry registry;

    public WorldLoginInterfaceImpl() throws RemoteException {
        super();
        registry = WorldRegistryImpl.getInstance();
    }

    public ImmutableMap<Integer, Integer> getChannelLoad() throws RemoteException {
        ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();

        for (ChannelWorldInterface cwi : registry.getAllChannelServers()) {
        	builder.put(cwi.getChannelId(), cwi.getConnected());
        }
        
        return builder.build();
    }
}