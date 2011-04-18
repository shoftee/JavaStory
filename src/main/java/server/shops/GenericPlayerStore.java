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
package server.shops;

import java.util.ArrayList;
import java.util.List;
import client.IItem;
import client.GameCharacter;
import client.GameClient;
import server.InventoryManipulator;
import server.maps.GameMapObjectType;
import tools.packet.PlayerShopPacket;

public class GenericPlayerStore extends AbstractPlayerShop {

    private boolean open;
    private GameCharacter owner;
    private int boughtnumber = 0;
    private List<String> bannedList = new ArrayList<String>();

    public GenericPlayerStore(GameCharacter owner, int itemId, String desc) {
	super(owner, itemId, desc);
	this.owner = owner;
	open = false;
    }

    @Override
    public void buy(GameClient c, int item, short quantity) {
	PlayerShopItem pItem = items.get(item);
	if (pItem.bundles > 0) {
/*	    synchronized (items) {
		IItem newItem = pItem.item.copy();
		newItem.setQuantity(quantity);
		if (c.getPlayer().getMeso() >= pItem.price * quantity) {
		    if (MapleInventoryManipulator.addFromDrop(c, newItem, false)) {
			pItem.totalquantity -= pItem.bundles - quantity;
			c.getPlayer().gainMeso(-pItem.price * quantity, false);

			if (pItem == 0) {
			    boughtnumber++;
			    if (boughtnumber == items.size()) {
				removeAllVisitors(10, 1);
				owner.getClient().write(PlayerShopPacket.shopErrorMessage(10, 1));
				closeShop(false, true);
			    }
			}
		    } else {
			c.getPlayer().dropMessage(1, "Your inventory is full.");
		    }
		} else {
		    c.getPlayer().dropMessage(1, "You do not have enough mesos.");
		}
	    }*/
	    owner.getClient().write(PlayerShopPacket.shopItemUpdate(this));
	}
    }

    @Override
    public byte getShopType() {
	return PlayerShop.PLAYER_SHOP;
    }

    @Override
    public void closeShop(boolean saveItems, boolean remove) {
	owner.getMap().broadcastMessage(PlayerShopPacket.removeCharBox(owner));
	owner.getMap().removeMapObject(this);

	if (saveItems) {
	    saveItems();
	}
	owner.setPlayerShop(null);
    }

    public void banPlayer(String name) {
	if (!bannedList.contains(name)) {
	    bannedList.add(name);
	}
	for (int i = 0; i < 3; i++) {
	    GameCharacter chr = getVisitor(i);
	    if (chr.getName().equals(name)) {
		chr.getClient().write(PlayerShopPacket.shopErrorMessage(5, 1));
		chr.setPlayerShop(null);
		removeVisitor(chr);
	    }
	}
    }

    @Override
    public void setOpen(boolean open) {
	this.open = open;
    }

    @Override
    public boolean isOpen() {
	return open;
    }

    public boolean isBanned(String name) {
	if (bannedList.contains(name)) {
	    return true;
	}
	return false;
    }

    public GameCharacter getMCOwner() {
	return owner;
    }

    @Override
    public void sendDestroyData(GameClient client) {
    }

    @Override
    public void sendSpawnData(GameClient client) {
    }

    @Override
    public GameMapObjectType getType() {
	return GameMapObjectType.SHOP;
    }
}
