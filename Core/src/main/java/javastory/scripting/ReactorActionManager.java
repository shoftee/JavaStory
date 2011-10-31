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
package javastory.scripting;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.life.LifeFactory;
import javastory.channel.maps.Reactor;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.IItem;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.data.ItemInfoProvider;
import javastory.game.data.ReactorDropEntry;

public class ReactorActionManager extends AbstractPlayerInteraction {

	private final Reactor reactor;

	public ReactorActionManager(final ChannelClient c, final Reactor reactor) {
		super(c);
		this.reactor = reactor;
	}

	// only used for meso = false, really. No minItems because meso is used to
	// fill the gap
	public void dropItems() {
		this.dropItems(false, 0, 0, 0, 0);
	}

	public void dropItems(final boolean meso, final int mesoChance, final int minMeso, final int maxMeso) {
		this.dropItems(meso, mesoChance, minMeso, maxMeso, 0);
	}

	public void dropItems(final boolean meso, final int mesoChance, final int minMeso, final int maxMeso, final int minItems) {
		final List<ReactorDropEntry> chances = ReactorScriptManager.getInstance().getDrops(this.reactor.getReactorId());
		final List<ReactorDropEntry> items = new LinkedList<>();

		if (meso) {
			if (Math.random() < 1 / (double) mesoChance) {
				items.add(new ReactorDropEntry(0, mesoChance, -1));
			}
		}

		int numItems = 0;
		// narrow list down by chances
		final Iterator<ReactorDropEntry> iter = chances.iterator();
		// for (DropEntry d : chances){
		while (iter.hasNext()) {
			final ReactorDropEntry d = iter.next();
			if (Math.random() < 1 / (double) d.chance) {
				numItems++;
				items.add(d);
			}
		}

		// if a minimum number of drops is required, add meso
		while (items.size() < minItems) {
			items.add(new ReactorDropEntry(0, mesoChance, -1));
			numItems++;
		}
		final Point dropPos = this.reactor.getPosition();

		dropPos.x -= 12 * numItems;

		int range, mesoDrop;
		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		for (final ReactorDropEntry d : items) {
			if (d.itemId == 0) {
				range = maxMeso - minMeso;
				final double randomMeso = Math.random() * range + minMeso;
				final float mesoRate = ChannelServer.getInstance().getMesoRate();
				mesoDrop = (int) (randomMeso * mesoRate);
				this.reactor.getMap().spawnMesoDrop(mesoDrop, dropPos, this.reactor, this.getPlayer(), false, (byte) 0);
			} else {
				IItem drop;
				if (GameConstants.getInventoryType(d.itemId) != InventoryType.EQUIP) {
					drop = new Item(d.itemId, (byte) 0, (short) 1, (byte) 0);
				} else {
					drop = ii.randomizeStats((Equip) ii.getEquipById(d.itemId));
				}
				this.reactor.getMap().spawnItemDrop(this.reactor, this.getPlayer(), drop, dropPos, false, false);
			}
			dropPos.x += 25;
		}
	}

	// summon one monster on reactor location
	public void spawnMonster(final int id) {
		this.spawnMonster(id, 1, this.getPosition());
	}

	// summon one monster, remote location
	public void spawnMonster(final int id, final int x, final int y) {
		this.spawnMonster(id, 1, new Point(x, y));
	}

	// multiple monsters, reactor location
	public void spawnMonster(final int id, final int qty) {
		this.spawnMonster(id, qty, this.getPosition());
	}

	// multiple monsters, remote location
	public void spawnMonster(final int id, final int qty, final int x, final int y) {
		this.spawnMonster(id, qty, new Point(x, y));
	}

	// handler for all spawnMonster
	private void spawnMonster(final int id, final int qty, final Point pos) {
		for (int i = 0; i < qty; i++) {
			this.reactor.getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(id), pos);
		}
	}

	@Override
	public void spawnNpc(final int npcId) {
		this.spawnNpc(npcId, this.getPosition());
	}

	// returns slightly above the reactor's position for monster spawns
	public Point getPosition() {
		final Point pos = this.reactor.getPosition();
		pos.y -= 10;
		return pos;
	}

	public Reactor getReactor() {
		return this.reactor;
	}

	public void spawnZakum() {
		this.reactor.getMap().spawnZakum(this.getPosition());
	}

	public void spawnFakeMonster(final int id) {
		this.spawnFakeMonster(id, 1, this.getPosition());
	}

	// summon one monster, remote location
	public void spawnFakeMonster(final int id, final int x, final int y) {
		this.spawnFakeMonster(id, 1, new Point(x, y));
	}

	// multiple monsters, reactor location
	public void spawnFakeMonster(final int id, final int qty) {
		this.spawnFakeMonster(id, qty, this.getPosition());
	}

	// multiple monsters, remote location
	public void spawnFakeMonster(final int id, final int qty, final int x, final int y) {
		this.spawnFakeMonster(id, qty, new Point(x, y));
	}

	// handler for all spawnFakeMonster
	private void spawnFakeMonster(final int id, final int qty, final Point pos) {
		for (int i = 0; i < qty; i++) {
			this.reactor.getMap().spawnFakeMonsterOnGroundBelow(LifeFactory.getMonster(id), pos);
		}
	}

	public void killAll() {
		this.reactor.getMap().killAllMonsters(true);
	}

	public void killMonster(final int monsId) {
		this.reactor.getMap().killMonster(monsId);
	}
}
