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
package server.life;

import client.MapleClient;
import server.MapleShopFactory;
import server.maps.MapleMapObjectType;
import tools.MaplePacketCreator;

public class MapleNPC extends AbstractLoadedMapleLife {

    private final MapleNPCStats stats;
    private boolean custom = false;

    public MapleNPC(final int id, final MapleNPCStats stats) {
	super(id);
	this.stats = stats;
    }

    public final boolean hasShop() {
	return MapleShopFactory.getInstance().getShopForNPC(getId()) != null;
    }

    public final void sendShop(final MapleClient c) {
	MapleShopFactory.getInstance().getShopForNPC(getId()).sendShop(c);
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {

	if (getId() >= 9901000 && getId() <= 9901551) {
	    if (!stats.getName().equals("")) {
		client.getSession().write(MaplePacketCreator.spawnPlayerNPC(stats, getId()));
		client.getSession().write(MaplePacketCreator.spawnNPCRequestController(this, false));
	    }
	} else {
	    client.getSession().write(MaplePacketCreator.spawnNPC(this, true));
	    client.getSession().write(MaplePacketCreator.spawnNPCRequestController(this, true));
	}
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
	client.getSession().write(MaplePacketCreator.removeNPC(getObjectId()));
    }

    @Override
    public final MapleMapObjectType getType() {
	return MapleMapObjectType.NPC;
    }

    public final String getName() {
	return stats.getName();
    }

    public final boolean isCustom() {
	return custom;
    }

    public final void setCustom(final boolean custom) {
	this.custom = custom;
    }
}
