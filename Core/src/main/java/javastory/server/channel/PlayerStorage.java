package javastory.server.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.ChannelCharacter;
import javastory.channel.CharacterTransfer;
import javastory.io.GamePacket;
import javastory.server.TimerManager;
import javastory.world.core.CheaterData;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class PlayerStorage {

	private final Lock activePlayerLock = new ReentrantLock();
	private final Lock pendingPlayerLock = new ReentrantLock();
	private final Lock sessionsLock = new ReentrantLock();
	private final Map<String, ChannelCharacter> nameToChar = Maps.newHashMap();
	private final Map<Integer, ChannelCharacter> idToChar = Maps.newHashMap();
	private final Map<Integer, String> sessions = Maps.newHashMap();
	private final Map<Integer, CharacterTransfer> pendingTransfers = Maps
			.newHashMap();

	public PlayerStorage() {
		// Prune once every 15 minutes
		TimerManager.getInstance().schedule(new PersistingTask(), 900000);
	}

	public final boolean registerSession(final int characterId,
			final String sessionIP) {
		Preconditions.checkNotNull(sessionIP);

		sessionsLock.lock();
		try {
			if (!sessions.get(characterId).equals(sessionIP)) {
				return false;
			}
			sessions.put(characterId, sessionIP);
			return true;
		} finally {
			sessionsLock.unlock();
		}
	}

	public final void deregisterSession(final int characterId) {
		sessionsLock.lock();
		try {
			sessions.remove(characterId);
		} finally {
			sessionsLock.unlock();
		}
	}

	public final boolean checkSession(final int characterId,
			final String sessionIP) {
		Preconditions.checkNotNull(sessionIP);

		sessionsLock.lock();
		try {
			return sessions.get(characterId).equals(sessionIP);
		} finally {
			sessionsLock.unlock();
		}
	}

	public final void registerTransfer(final CharacterTransfer chr,
			final int characterId) {
		pendingPlayerLock.lock();
		try {
			pendingTransfers.put(characterId, chr);
		} finally {
			pendingPlayerLock.unlock();
		}
	}

	public final void deregisterPlayer(final ChannelCharacter chr) {
		activePlayerLock.lock();
		try {
			nameToChar.remove(chr.getName().toLowerCase());
			idToChar.remove(chr.getId());
		} finally {
			activePlayerLock.unlock();
		}
	}

	public final void deregisterTransfer(final int characterId) {
		pendingPlayerLock.lock();
		try {
			pendingTransfers.remove(characterId);
		} finally {
			pendingPlayerLock.unlock();
		}
	}

	public final CharacterTransfer getPendingTransfer(final int characterId) {
		final CharacterTransfer transfer = pendingTransfers.get(characterId);
		if (transfer != null) {
			deregisterTransfer(characterId);
		}
		return transfer;
	}

	public final void registerPlayer(final ChannelCharacter chr) {
		activePlayerLock.lock();
		try {
			nameToChar.put(chr.getName().toLowerCase(), chr);
			idToChar.put(chr.getId(), chr);
		} finally {
			activePlayerLock.unlock();
		}
	}

	public final ChannelCharacter getCharacterByName(final String name) {
		return nameToChar.get(name.toLowerCase());
	}

	public final ChannelCharacter getCharacterById(final int id) {
		return idToChar.get(id);
	}

	public final int getConnectedClients() {
		return idToChar.size();
	}

	public final List<CheaterData> getCheaters() {
		final List<CheaterData> cheaters = new ArrayList<>();

		activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = nameToChar.values()
					.iterator();
			ChannelCharacter chr;
			while (itr.hasNext()) {
				chr = itr.next();
				final int points = chr.getCheatTracker().getPoints();

				if (points <= 0) {
					continue;
				}

				final String summary = chr.getCheatTracker().getSummary();
				final String readableName = chr.getName().toUpperCase();

				final String description = String.format("%s (%d) %s",
						readableName, points, summary);

				cheaters.add(new CheaterData(points, description));
			}
		} finally {
			activePlayerLock.unlock();
		}
		return cheaters;
	}

	public final void disconnectAll() {
		activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = nameToChar.values()
					.iterator();
			ChannelCharacter chr;
			while (itr.hasNext()) {
				chr = itr.next();

				if (!chr.isGM()) {
					chr.getClient().disconnect();
					chr.getClient().disconnect();
					itr.remove();
				}
			}
		} finally {
			activePlayerLock.unlock();
		}
	}

	public final String getOnlinePlayers(final boolean byGM) {
		final StringBuilder sb = new StringBuilder();
		if (byGM) {
			activePlayerLock.lock();
			try {
				final Iterator<ChannelCharacter> itr = nameToChar.values()
						.iterator();
				ChannelCharacter character = itr.next();
				while (itr.hasNext()) {
					sb.append(character.getWorldName().toUpperCase());
					sb.append(", ");
				}
			} finally {
				activePlayerLock.unlock();
			}
		} else {
			activePlayerLock.lock();
			try {
				final Iterator<ChannelCharacter> itr = nameToChar.values()
						.iterator();
				ChannelCharacter chr;
				while (itr.hasNext()) {
					chr = itr.next();
					if (!chr.isGM()) {
						sb.append(chr.getWorldName().toUpperCase());
						sb.append(", ");
					}
				}
			} finally {
				activePlayerLock.unlock();
			}
		}
		return sb.toString();
	}

	public final void broadcastPacket(final GamePacket data) {
		activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = nameToChar.values()
					.iterator();
			while (itr.hasNext()) {
				itr.next().getClient().write(data);
			}
		} finally {
			activePlayerLock.unlock();
		}
	}

	public final void broadcastSmegaPacket(final GamePacket data) {
		activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = nameToChar.values()
					.iterator();
			ChannelCharacter chr;
			while (itr.hasNext()) {
				chr = itr.next();

				if (chr.getClient().isLoggedIn() && chr.getSmega()) {
					chr.getClient().write(data);
				}
			}
		} finally {
			activePlayerLock.unlock();
		}
	}

	public final void broadcastGMPacket(final GamePacket data) {
		activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = nameToChar.values()
					.iterator();
			ChannelCharacter chr;
			while (itr.hasNext()) {
				chr = itr.next();

				if (chr.getClient().isLoggedIn() && chr.isGM()
						&& chr.isCallGM()) {
					chr.getClient().write(data);
				}
			}
		} finally {
			activePlayerLock.unlock();
		}
	}

	public class PersistingTask implements Runnable {

		@Override
		public void run() {
			pendingPlayerLock.lock();
			try {
				final long currenttime = System.currentTimeMillis();
				final Iterator<Map.Entry<Integer, CharacterTransfer>> itr = pendingTransfers
						.entrySet().iterator();

				while (itr.hasNext()) {
					final Entry<Integer, CharacterTransfer> current = itr
							.next();
					if (currenttime - current.getValue().TranferTime > 40000) { // 40
																				// sec
						itr.remove();
						deregisterSession(current.getKey());
					}
				}
				TimerManager.getInstance().schedule(new PersistingTask(),
						900000);
			} finally {
				pendingPlayerLock.unlock();
			}
		}
	}

	public Collection<ChannelCharacter> getAllCharacters() {
		return nameToChar.values();
	}
}