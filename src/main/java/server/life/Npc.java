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

import client.GameClient;
import server.ShopFactory;
import server.maps.GameMapObjectType;
import tools.MaplePacketCreator;

public class Npc extends AbstractLoadedGameLife {

    private final NpcStats stats;
    private boolean custom = false;

    public Npc(final int id, final NpcStats stats) {
	super(id);
	this.stats = stats;
    }

    public final boolean hasShop() {
	return ShopFactory.getInstance().getShopForNPC(getId()) != null;
    }

    public final void sendShop(final GameClient c) {
	ShopFactory.getInstance().getShopForNPC(getId()).sendShop(c);
    }

    @Override
    public final void sendSpawnData(final GameClient client) {

	if (getId() >= 9901000 && getId() <= 9901551) {
	    if (!stats.getName().equals("")) {
		client.write(MaplePacketCreator.spawnPlayerNPC(stats, getId()));
		client.write(MaplePacketCreator.spawnNPCRequestController(this, false));
	    }
	} else {
	    client.write(MaplePacketCreator.spawnNPC(this, true));
	    client.write(MaplePacketCreator.spawnNPCRequestController(this, true));
	}
    }

    @Override
    public final void sendDestroyData(final GameClient client) {
	client.write(MaplePacketCreator.removeNPC(getObjectId()));
    }

    @Override
    public final GameMapObjectType getType() {
	return GameMapObjectType.NPC;
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