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

import java.awt.Point;
import java.util.concurrent.atomic.AtomicInteger;

import server.maps.GameMap;
import tools.MaplePacketCreator;

public class SpawnPoint extends Spawns {

    private Monster monster;
    private Point pos;
    private long nextPossibleSpawn;
    private int mobTime;
    private AtomicInteger spawnedMonsters = new AtomicInteger(0);
    private boolean immobile;
    private String msg;
    private byte carnivalTeam;

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
	return monster;
    }

    @Override
    public final byte getCarnivalTeam() {
	return carnivalTeam;
    }

    @Override
    public final boolean shouldSpawn() {
	if (mobTime < 0) {
	    return false;
	}
	// regular spawnpoints should spawn a maximum of 3 monsters; immobile spawnpoints or spawnpoints with mobtime a
	// maximum of 1
	if (((mobTime != 0 || immobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 1) {
	    return false;
	}
	return nextPossibleSpawn <= System.currentTimeMillis();
    }

    @Override
    public final Monster spawnMonster(final GameMap map) {
	final Monster mob = new Monster(monster);
	mob.setPosition(pos);
        mob.setCarnivalTeam(carnivalTeam);
	spawnedMonsters.incrementAndGet();
	mob.addListener(new MonsterListener() {

	    @Override
	    public void monsterKilled() {
		nextPossibleSpawn = System.currentTimeMillis();

		if (mobTime > 0) {
		    nextPossibleSpawn += mobTime;
		}
		spawnedMonsters.decrementAndGet();
	    }
	});
	map.spawnMonster(mob, -2);

	if (msg != null) {
	    map.broadcastMessage(MaplePacketCreator.serverNotice(6, msg));
	}
	return mob;
    }
}
