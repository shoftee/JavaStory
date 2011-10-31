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
package javastory.game;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

import javastory.channel.life.Monster;
import javastory.channel.life.MonsterListener;
import javastory.channel.life.Spawns;
import javastory.channel.maps.GameMap;
import javastory.tools.packets.ChannelPackets;

public class SpawnPoint extends Spawns {

	private final Monster monster;
	private final Point pos;
	private long nextPossibleSpawn;
	private final int mobTime;
	private final AtomicInteger spawnedMonsters = new AtomicInteger(0);
	private final boolean immobile;
	private final String msg;
	private final byte carnivalTeam;

	public SpawnPoint(final Monster monster, final Point pos, final int mobTime, final byte carnivalTeam, final String msg) {
		this.monster = monster;
		this.pos = pos;
		this.mobTime = mobTime * 1000;
		this.carnivalTeam = carnivalTeam;
		this.msg = msg;
		this.immobile = !monster.getStats().getMobile();
		this.nextPossibleSpawn = System.currentTimeMillis();
	}

	@Override
	public final Monster getMonster() {
		return this.monster;
	}

	@Override
	public final byte getCarnivalTeam() {
		return this.carnivalTeam;
	}

	@Override
	public final boolean shouldSpawn() {
		if (this.mobTime < 0) {
			return false;
		}
		// regular spawnpoints should spawn a maximum of 3 monsters; immobile
		// spawnpoints or spawnpoints with mobtime a
		// maximum of 1
		if ((this.mobTime != 0 || this.immobile) && this.spawnedMonsters.get() > 0 || this.spawnedMonsters.get() > 1) {
			return false;
		}
		return this.nextPossibleSpawn <= System.currentTimeMillis();
	}

	@Override
	public final Monster spawnMonster(final GameMap map) {
		final Monster mob = new Monster(this.monster);
		mob.setPosition(this.pos);
		mob.setCarnivalTeam(this.carnivalTeam);
		this.spawnedMonsters.incrementAndGet();
		mob.addListener(new MonsterListener() {

			@Override
			public void monsterKilled() {
				SpawnPoint.this.nextPossibleSpawn = System.currentTimeMillis();

				if (SpawnPoint.this.mobTime > 0) {
					SpawnPoint.this.nextPossibleSpawn += SpawnPoint.this.mobTime;
				}
				SpawnPoint.this.spawnedMonsters.decrementAndGet();
			}
		});
		map.spawnMonster(mob, -2);

		if (this.msg != null) {
			map.broadcastMessage(ChannelPackets.serverNotice(6, this.msg));
		}
		return mob;
	}
}
