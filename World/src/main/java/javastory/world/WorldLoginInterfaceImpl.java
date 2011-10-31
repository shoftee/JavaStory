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

import java.rmi.RemoteException;

import javastory.rmi.ChannelWorldInterface;
import javastory.rmi.GenericRemoteObject;
import javastory.rmi.WorldLoginInterface;
import javastory.world.core.WorldRegistry;

import com.google.common.collect.ImmutableMap;

public class WorldLoginInterfaceImpl extends GenericRemoteObject implements WorldLoginInterface {

    private static final long serialVersionUID = -4965323089596332908L;
    private final WorldRegistry registry;

    public WorldLoginInterfaceImpl() throws RemoteException {
        super();
        this.registry = WorldRegistryImpl.getInstance();
    }

    @Override
	public ImmutableMap<Integer, Integer> getChannelLoad() throws RemoteException {
        final ImmutableMap.Builder<Integer, Integer> builder = ImmutableMap.builder();

        for (final ChannelWorldInterface cwi : this.registry.getAllChannelServers()) {
        	builder.put(cwi.getChannelId(), cwi.getConnected());
        }
        
        return builder.build();
    }
}