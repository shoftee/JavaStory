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

import java.util.concurrent.ScheduledFuture;
import client.IItem;
import client.ItemFlag;
import client.GameConstants;
import client.GameCharacter;
import client.GameClient;
import org.javastory.server.channel.ChannelManager;
import server.InventoryManipulator;
import server.TimerManager;
import server.maps.GameMap;
import server.maps.GameMapObjectType;
import tools.packet.PlayerShopPacket;

public class HiredMerchantStore extends AbstractPlayerShop {

    public ScheduledFuture<?> schedule;
    private GameMap map;
    private int channel, storeid;
    private long start;

    public HiredMerchantStore(GameCharacter owner, int itemId, String desc) {
	super(owner, itemId, desc);
	start = System.currentTimeMillis();
	this.map = owner.getMap();
	this.channel = owner.getClient().getChannelId();
	this.schedule = TimerManager.getInstance().schedule(new Runnable() {

	    @Override
	    public void run() {
		closeShop(true, true);
	    }
	}, 1000 * 60 * 60 * 24);
    }

    public byte getShopType() {
	return PlayerShop.HIRED_MERCHANT;
    }

    public final void setStoreid(final int storeid) {
	this.storeid = storeid;
    }

    @Override
    public void buy(GameClient c, int item, short quantity) {
	final PlayerShopItem pItem = items.get(item);
	final IItem shopItem = pItem.item;
	final IItem newItem = shopItem.copy();
	final short perbundle = newItem.getQuantity();

	newItem.setQuantity((short) (quantity * perbundle));

	byte flag = newItem.getFlag();

	if (ItemFlag.KARMA_EQ.check(flag)) {
	    newItem.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
	} else if (ItemFlag.KARMA_USE.check(flag)) {
	    newItem.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
	}

	if (InventoryManipulator.addFromDrop(c, newItem, false)) {
	    pItem.bundles -= quantity; // Number remaining in the store

	    final int gainmeso = getMeso() + (pItem.price * quantity);
	    setMeso(gainmeso - GameConstants.EntrustedStoreTax(gainmeso));
	    c.getPlayer().gainMeso(-pItem.price * quantity, false);
	} else {
	    c.getPlayer().dropMessage(1, "Your inventory is full.");
	}
    }

    @Override
    public void closeShop(boolean saveItems, boolean remove) {
	if (schedule != null) {
	    schedule.cancel(false);
	}
	if (saveItems) {
	    saveItems();
	}
	if (remove) {
	    ChannelManager.getInstance(channel).removeMerchant(this);
	    map.broadcastMessage(PlayerShopPacket.destroyHiredMerchant(getOwnerId()));
	}
	map.removeMapObject(this);

	map = null;
	schedule = null;
    }

    public int getTimeLeft() {
	return (int) ((System.currentTimeMillis() - start) / 1000);
    }

    public GameMap getMap() {
	return map;
    }

    public final int getStoreId() {
	return storeid;
    }

    @Override
    public GameMapObjectType getType() {
	return GameMapObjectType.HIRED_MERCHANT;
    }

    @Override
    public void sendDestroyData(GameClient client) {
	client.write(PlayerShopPacket.destroyHiredMerchant(getOwnerId()));
    }

    @Override
    public void sendSpawnData(GameClient client) {
	client.write(PlayerShopPacket.spawnHiredMerchant(this));
    }
}