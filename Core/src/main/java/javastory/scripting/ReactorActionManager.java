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
package javastory.scripting;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.life.LifeFactory;
import javastory.channel.maps.Reactor;
import javastory.channel.maps.ReactorDropEntry;
import javastory.client.Equip;
import javastory.client.IItem;
import javastory.client.Item;
import javastory.game.GameConstants;
import javastory.game.InventoryType;
import javastory.server.ItemInfoProvider;

public class ReactorActionManager extends AbstractPlayerInteraction {

	private Reactor reactor;

	public ReactorActionManager(ChannelClient c, Reactor reactor) {
		super(c);
		this.reactor = reactor;
	}

	// only used for meso = false, really. No minItems because meso is used to
	// fill the gap
	public void dropItems() {
		dropItems(false, 0, 0, 0, 0);
	}

	public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
		dropItems(meso, mesoChance, minMeso, maxMeso, 0);
	}

	public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minItems) {
		final List<ReactorDropEntry> chances = ReactorScriptManager.getInstance().getDrops(reactor.getReactorId());
		final List<ReactorDropEntry> items = new LinkedList<>();

		if (meso) {
			if (Math.random() < (1 / (double) mesoChance)) {
				items.add(new ReactorDropEntry(0, mesoChance, -1));
			}
		}

		int numItems = 0;
		// narrow list down by chances
		final Iterator<ReactorDropEntry> iter = chances.iterator();
		// for (DropEntry d : chances){
		while (iter.hasNext()) {
			ReactorDropEntry d = iter.next();
			if (Math.random() < (1 / (double) d.chance)) {
				numItems++;
				items.add(d);
			}
		}

		// if a minimum number of drops is required, add meso
		while (items.size() < minItems) {
			items.add(new ReactorDropEntry(0, mesoChance, -1));
			numItems++;
		}
		final Point dropPos = reactor.getPosition();

		dropPos.x -= (12 * numItems);

		int range, mesoDrop;
		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		for (final ReactorDropEntry d : items) {
			if (d.itemId == 0) {
				range = maxMeso - minMeso;
				mesoDrop = (int) (Math.random() * range) + minMeso * ChannelServer.getInstance().getMesoRate();
				reactor.getMap().spawnMesoDrop(mesoDrop, dropPos, reactor, getPlayer(), false, (byte) 0);
			} else {
				IItem drop;
				if (GameConstants.getInventoryType(d.itemId) != InventoryType.EQUIP) {
					drop = new Item(d.itemId, (byte) 0, (short) 1, (byte) 0);
				} else {
					drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
				}
				reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, dropPos, false, false);
			}
			dropPos.x += 25;
		}
	}

	// summon one monster on reactor location
	public void spawnMonster(int id) {
		spawnMonster(id, 1, getPosition());
	}

	// summon one monster, remote location
	public void spawnMonster(int id, int x, int y) {
		spawnMonster(id, 1, new Point(x, y));
	}

	// multiple monsters, reactor location
	public void spawnMonster(int id, int qty) {
		spawnMonster(id, qty, getPosition());
	}

	// multiple monsters, remote location
	public void spawnMonster(int id, int qty, int x, int y) {
		spawnMonster(id, qty, new Point(x, y));
	}

	// handler for all spawnMonster
	private void spawnMonster(int id, int qty, Point pos) {
		for (int i = 0; i < qty; i++) {
			reactor.getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(id), pos);
		}
	}

	@Override
	public void spawnNpc(int npcId) {
		spawnNpc(npcId, getPosition());
	}

	// returns slightly above the reactor's position for monster spawns
	public Point getPosition() {
		Point pos = reactor.getPosition();
		pos.y -= 10;
		return pos;
	}

	public Reactor getReactor() {
		return reactor;
	}

	public void spawnZakum() {
		reactor.getMap().spawnZakum(getPosition());
	}

	public void spawnFakeMonster(int id) {
		spawnFakeMonster(id, 1, getPosition());
	}

	// summon one monster, remote location
	public void spawnFakeMonster(int id, int x, int y) {
		spawnFakeMonster(id, 1, new Point(x, y));
	}

	// multiple monsters, reactor location
	public void spawnFakeMonster(int id, int qty) {
		spawnFakeMonster(id, qty, getPosition());
	}

	// multiple monsters, remote location
	public void spawnFakeMonster(int id, int qty, int x, int y) {
		spawnFakeMonster(id, qty, new Point(x, y));
	}

	// handler for all spawnFakeMonster
	private void spawnFakeMonster(int id, int qty, Point pos) {
		for (int i = 0; i < qty; i++) {
			reactor.getMap().spawnFakeMonsterOnGroundBelow(LifeFactory.getMonster(id), pos);
		}
	}

	public void killAll() {
		reactor.getMap().killAllMonsters(true);
	}

	public void killMonster(int monsId) {
		reactor.getMap().killMonster(monsId);
	}
}
