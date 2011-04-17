package org.javastory.server.channel;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import client.GameCharacterUtil;
import client.GameCharacter;
import handling.GamePacket;
import handling.world.CharacterTransfer;
import handling.world.remote.CheaterData;
import java.util.Collection;
import server.TimerManager;

public class PlayerStorage {

    private final Lock mutex = new ReentrantLock();
    private final Lock mutex2 = new ReentrantLock();
    private final Map<String, GameCharacter> nameToChar = new HashMap<String, GameCharacter>();
    private final Map<Integer, GameCharacter> idToChar = new HashMap<Integer, GameCharacter>();
    private final Map<Integer, CharacterTransfer> PendingCharacter = new HashMap<Integer, CharacterTransfer>();

    public PlayerStorage() {
        // Prune once every 15 minutes
        TimerManager.getInstance().schedule(new PersistingTask(), 900000);
    }

    public final void registerPlayer(final GameCharacter chr) {
        mutex.lock();
        try {
            nameToChar.put(chr.getName().toLowerCase(), chr);
            idToChar.put(chr.getId(), chr);
        } finally {
            mutex.unlock();
        }
    }

    public final void registerPendingPlayer(final CharacterTransfer chr, final int playerid) {
        mutex2.lock();
        try {
            PendingCharacter.put(playerid, chr);
        } finally {
            mutex2.unlock();
        }
    }

    public final void deregisterPlayer(final GameCharacter chr) {
        mutex.lock();
        try {
            nameToChar.remove(chr.getName().toLowerCase());
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

    public final GameCharacter getCharacterByName(final String name) {
        return nameToChar.get(name.toLowerCase());
    }

    public final GameCharacter getCharacterById(final int id) {
        return idToChar.get(id);
    }

    public final int getConnectedClients() {
        return idToChar.size();
    }

    public final List<CheaterData> getCheaters() {
        final List<CheaterData> cheaters = new ArrayList<CheaterData>();

        mutex.lock();
        try {
            final Iterator<GameCharacter> itr = nameToChar.values().iterator();
            GameCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getCheatTracker().getPoints() > 0) {
                    cheaters.add(new CheaterData(chr.getCheatTracker().getPoints(), GameCharacterUtil.makeMapleReadable(chr.getName()) + " (" + chr.getCheatTracker().getPoints() + ") " + chr.getCheatTracker().getSummary()));
                }
            }
        } finally {
            mutex.unlock();
        }
        return cheaters;
    }

    public final void disconnectAll() {
        mutex.lock();
        try {
            final Iterator<GameCharacter> itr = nameToChar.values().iterator();
            GameCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (!chr.isGM()) {
                    chr.getClient().disconnect(false);
                    chr.getClient().getSession().close(false);
                    itr.remove();
                }
            }
        } finally {
            mutex.unlock();
        }
    }

    public final String getOnlinePlayers(final boolean byGM) {
        final StringBuilder sb = new StringBuilder();
        if (byGM) {
            mutex.lock();
            try {
                final Iterator<GameCharacter> itr = nameToChar.values().iterator();
                while (itr.hasNext()) {
                    sb.append(GameCharacterUtil.makeMapleReadable(itr.next().getWorldName()));
                    sb.append(", ");
                }
            } finally {
                mutex.unlock();
            }
        } else {
            mutex.lock();
            try {
                final Iterator<GameCharacter> itr = nameToChar.values().iterator();
                GameCharacter chr;
                while (itr.hasNext()) {
                    chr = itr.next();
                    if (!chr.isGM()) {
                        sb.append(GameCharacterUtil.makeMapleReadable(chr.getWorldName()));
                        sb.append(", ");
                    }
                }
            } finally {
                mutex.unlock();
            }
        }
        return sb.toString();
    }

    public final void broadcastPacket(final GamePacket data) {
        mutex.lock();
        try {
            final Iterator<GameCharacter> itr = nameToChar.values().iterator();
            while (itr.hasNext()) {
                itr.next().getClient().getSession().write(data);
            }
        } finally {
            mutex.unlock();
        }
    }

    public final void broadcastSmegaPacket(final GamePacket data) {
        mutex.lock();
        try {
            final Iterator<GameCharacter> itr = nameToChar.values().iterator();
            GameCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn() && chr.getSmega()) {
                    chr.getClient().getSession().write(data);
                }
            }
        } finally {
            mutex.unlock();
        }
    }

    public final void broadcastGMPacket(final GamePacket data) {
        mutex.lock();
        try {
            final Iterator<GameCharacter> itr = nameToChar.values().iterator();
            GameCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();

                if (chr.getClient().isLoggedIn() && chr.isGM() && chr.isCallGM()) {
                    chr.getClient().getSession().write(data);
                }
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

    public Collection<GameCharacter> getAllCharacters() {
        return nameToChar.values();
    }
}