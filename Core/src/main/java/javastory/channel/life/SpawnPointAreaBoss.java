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
package javastory.channel.life;

import java.awt.Point;
import java.util.concurrent.atomic.AtomicBoolean;

import javastory.channel.maps.GameMap;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;

public class SpawnPointAreaBoss implements Spawnable {

	private final Monster monster;
	private final Point pos1;
	private final Point pos2;
	private final Point pos3;
	private long nextPossibleSpawn;
	private final int mobTime;
	private final AtomicBoolean spawned = new AtomicBoolean(false);
	private final String msg;

	public SpawnPointAreaBoss(final Monster monster, final Point pos1, final Point pos2, final Point pos3, final int mobTime, final String msg) {
		this.monster = monster;
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.pos3 = pos3;
		this.mobTime = mobTime * 1000;
		this.msg = msg;
		this.nextPossibleSpawn = System.currentTimeMillis();
	}

	@Override
	public final Monster getMonster() {
		return this.monster;
	}

	@Override
	public final byte getCarnivalTeam() {
		return 0;
	}

	@Override
	public final boolean shouldSpawn() {
		if (this.mobTime < 0) {
			return false;
		}
		if (this.spawned.get()) {
			return false;
		}
		return this.nextPossibleSpawn <= System.currentTimeMillis();
	}

	@Override
	public final Monster spawnMonster(final GameMap map) {
		final Monster mob = new Monster(this.monster);
		final int rand = Randomizer.nextInt(3);
		mob.setPosition(rand == 0 ? this.pos1 : rand == 1 ? this.pos2 : this.pos3);
		this.spawned.set(true);
		mob.addListener(new MonsterListener() {

			@Override
			public void monsterKilled() {
				SpawnPointAreaBoss.this.nextPossibleSpawn = System.currentTimeMillis();

				if (SpawnPointAreaBoss.this.mobTime > 0) {
					SpawnPointAreaBoss.this.nextPossibleSpawn += SpawnPointAreaBoss.this.mobTime;
				}
				SpawnPointAreaBoss.this.spawned.set(false);
			}
		});
		map.spawnMonster(mob, -2);

		if (this.msg != null) {
			map.broadcastMessage(ChannelPackets.serverNotice(6, this.msg));
			// map.broadcastMessage(MaplePacketCreator.musicChange("Bgm04/WhiteChristmas_"));
		}
		return mob;
	}
}