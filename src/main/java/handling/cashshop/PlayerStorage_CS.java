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
package handling.cashshop;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import client.MapleCharacter;
import handling.world.CharacterTransfer;
import server.TimerManager;

public class PlayerStorage_CS {

    private final Lock mutex = new ReentrantLock();
    private final Lock mutex2 = new ReentrantLock();
    private final Map<Integer, MapleCharacter> idToChar = new HashMap<Integer, MapleCharacter>();
    private final Map<Integer, CharacterTransfer> PendingCharacter = new HashMap<Integer, CharacterTransfer>();

    public PlayerStorage_CS() {
	// Prune once every 15 minutes
	TimerManager.getInstance().schedule(new PersistingTask(), 900000);
    }

    public final void registerPlayer(final MapleCharacter chr) {
	mutex.lock();
	try {
	    idToChar.put(chr.getId(), chr);
	} finally {
	    mutex.unlock();
	}
    }

    public final void registerPendingPlayer(final CharacterTransfer chr, final int playerid) {
	mutex2.lock();
	try {
	    PendingCharacter.put(playerid, chr);//new Pair(System.currentTimeMillis(), chr));
	} finally {
	    mutex2.unlock();
	}
    }

    public final void deregisterPlayer(final MapleCharacter chr) {
	mutex.lock();
	try {
	    idToChar.remove(chr.getId());
	} finally {
	    mutex.unlock();
	}
    }

    public final void deregisterPendingPlayer(final int charid) {
	mutex2.lock();
	try {
	    PendingCharacter.remove(charid);
	} finally {
	    mutex2.unlock();
	}
    }

    public final CharacterTransfer getPendingCharacter(final int charid) {
	final CharacterTransfer toreturn = PendingCharacter.get(charid);//.right;
	if (toreturn != null) {
	    deregisterPendingPlayer(charid);
	}
	return toreturn;
    }

    public final boolean isCharacterConnected(final String name) {
	boolean connected = false;

	mutex.lock();
	try {
	    final Iterator<MapleCharacter> itr = idToChar.values().iterator();
	    while (itr.hasNext()) {
		if (itr.next().getName().equals(name)) {
		    connected = true;
		    break;
		}
	    }
	} finally {
	    mutex.unlock();
	}
	return connected;
    }

    public final void disconnectAll() {
	mutex.lock();
	try {
	    final List<MapleCharacter> dcList = new ArrayList();
	    final Iterator<MapleCharacter> itr = idToChar.values().iterator();
	    MapleCharacter chr;
	    while (itr.hasNext()) {
		chr = itr.next();

		chr.getClient().disconnect(false, true);
//		this.deregisterPlayer(chr);
		dcList.add(chr);
		chr.getClient().getSession().close();
	    }

	    for (final MapleCharacter character : dcList) {
		this.deregisterPlayer(character);
	    }
	} finally {
	    mutex.unlock();
	}
    }

    public class PersistingTask implements Runnable {

	@Override
	public void run() {
	    mutex2.lock();
	    try {
		final long currenttime = System.currentTimeMillis();
		final Iterator<Map.Entry<Integer, CharacterTransfer>> itr = PendingCharacter.entrySet().iterator();

		while (itr.hasNext()) {
		    if (currenttime - itr.next().getValue().TranferTime > 40000) { // 40 sec
			itr.remove();
		    }
		}
		TimerManager.getInstance().schedule(new PersistingTask(), 900000);
	    } finally {
		mutex2.unlock();
	    }
	}
    }
}
