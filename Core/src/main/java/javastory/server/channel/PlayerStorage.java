package javastory.server.channel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javastory.channel.ChannelCharacter;
import javastory.channel.CharacterTransfer;
import javastory.io.GamePacket;
import javastory.world.core.CheaterData;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PlayerStorage {

	private final Lock activePlayerLock = new ReentrantLock();
	private final Lock pendingPlayerLock = new ReentrantLock();
	private final Lock sessionsLock = new ReentrantLock();
	private final Map<String, ChannelCharacter> nameToChar = Maps.newHashMap();
	private final Map<Integer, ChannelCharacter> idToChar = Maps.newHashMap();
	private final Map<Integer, String> sessions = Maps.newHashMap();
	
	private final Map<Integer, CharacterTransfer> pendingTransfers;
	
	private final ScheduledThreadPoolExecutor cleanupScheduler;
	
	private static class TransferTimeout implements Runnable {

		private final int characterId;
		private final PlayerStorage storage;
		
		public TransferTimeout(PlayerStorage storage, int characterId) {
			this.storage = storage;
			this.characterId = characterId;
		}
		
		@Override
		public void run() {
			this.storage.deregisterTransfer(this.characterId);
		}
		
	}
	
	public PlayerStorage() {
		this.pendingTransfers = Maps.newLinkedHashMap();
		cleanupScheduler = new ScheduledThreadPoolExecutor(10);
	}

	public final boolean registerSession(final int characterId, final String sessionIP) {
		Preconditions.checkNotNull(sessionIP);

		this.sessionsLock.lock();
		try {
			if (!this.sessions.get(characterId).equals(sessionIP)) {
				return false;
			}
			this.sessions.put(characterId, sessionIP);
			return true;
		} finally {
			this.sessionsLock.unlock();
		}
	}

	public final void deregisterSession(final int characterId) {
		this.sessionsLock.lock();
		try {
			this.sessions.remove(characterId);
		} finally {
			this.sessionsLock.unlock();
		}
	}

	public final boolean checkSession(final int characterId, final String sessionIP) {
		Preconditions.checkNotNull(sessionIP);

		this.sessionsLock.lock();
		try {
			return this.sessions.get(characterId).equals(sessionIP);
		} finally {
			this.sessionsLock.unlock();
		}
	}

	public final void registerTransfer(final CharacterTransfer chr, final int characterId) {
		this.pendingPlayerLock.lock();
		try {
			this.pendingTransfers.put(characterId, chr);
			cleanupScheduler.schedule(new TransferTimeout(this, characterId), 1, TimeUnit.MINUTES);
		} finally {
			this.pendingPlayerLock.unlock();
		}
	}

	public final void deregisterPlayer(final ChannelCharacter chr) {
		this.activePlayerLock.lock();
		try {
			this.nameToChar.remove(chr.getName().toLowerCase());
			this.idToChar.remove(chr.getId());
		} finally {
			this.activePlayerLock.unlock();
		}
	}

	public final void deregisterTransfer(final int characterId) {
		this.pendingPlayerLock.lock();
		try {
			this.pendingTransfers.remove(characterId);
		} finally {
			this.pendingPlayerLock.unlock();
		}
	}

	public final CharacterTransfer getPendingTransfer(final int characterId) {
		final CharacterTransfer transfer = this.pendingTransfers.get(characterId);
		if (transfer != null) {
			this.deregisterTransfer(characterId);
		}
		return transfer;
	}

	public final void registerPlayer(final ChannelCharacter chr) {
		this.activePlayerLock.lock();
		try {
			this.nameToChar.put(chr.getName().toLowerCase(), chr);
			this.idToChar.put(chr.getId(), chr);
		} finally {
			this.activePlayerLock.unlock();
		}
	}

	public final ChannelCharacter getCharacterByName(final String name) {
		return this.nameToChar.get(name.toLowerCase());
	}

	public final ChannelCharacter getCharacterById(final int id) {
		return this.idToChar.get(id);
	}

	public final int getConnectedClients() {
		return this.idToChar.size();
	}

	public final List<CheaterData> getCheaters() {
		final List<CheaterData> cheaters = Lists.newArrayList();

		this.activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = this.nameToChar.values().iterator();
			ChannelCharacter chr;
			while (itr.hasNext()) {
				chr = itr.next();
				final int points = chr.getCheatTracker().getPoints();

				if (points <= 0) {
					continue;
				}

				final String summary = chr.getCheatTracker().getSummary();
				final String readableName = chr.getName().toUpperCase();

				final String description = String.format("%s (%d) %s", readableName, points, summary);

				cheaters.add(new CheaterData(points, description));
			}
		} finally {
			this.activePlayerLock.unlock();
		}
		return cheaters;
	}

	public final void disconnectAll() {
		this.activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = this.nameToChar.values().iterator();
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
			this.activePlayerLock.unlock();
		}
	}

	public final String getOnlinePlayers(final boolean byGM) {
		final StringBuilder sb = new StringBuilder();
		if (byGM) {
			this.activePlayerLock.lock();
			try {
				final Iterator<ChannelCharacter> itr = this.nameToChar.values().iterator();
				final ChannelCharacter character = itr.next();
				while (itr.hasNext()) {
					sb.append(character.getWorldName().toUpperCase());
					sb.append(", ");
				}
			} finally {
				this.activePlayerLock.unlock();
			}
		} else {
			this.activePlayerLock.lock();
			try {
				final Iterator<ChannelCharacter> itr = this.nameToChar.values().iterator();
				ChannelCharacter chr;
				while (itr.hasNext()) {
					chr = itr.next();
					if (!chr.isGM()) {
						sb.append(chr.getWorldName().toUpperCase());
						sb.append(", ");
					}
				}
			} finally {
				this.activePlayerLock.unlock();
			}
		}
		return sb.toString();
	}

	public final void broadcastPacket(final GamePacket data) {
		this.activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = this.nameToChar.values().iterator();
			while (itr.hasNext()) {
				itr.next().getClient().write(data);
			}
		} finally {
			this.activePlayerLock.unlock();
		}
	}

	public final void broadcastSmegaPacket(final GamePacket data) {
		this.activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = this.nameToChar.values().iterator();
			ChannelCharacter chr;
			while (itr.hasNext()) {
				chr = itr.next();

				if (chr.getClient().isLoggedIn() && chr.getSmega()) {
					chr.getClient().write(data);
				}
			}
		} finally {
			this.activePlayerLock.unlock();
		}
	}

	public final void broadcastGMPacket(final GamePacket data) {
		this.activePlayerLock.lock();
		try {
			final Iterator<ChannelCharacter> itr = this.nameToChar.values().iterator();
			ChannelCharacter chr;
			while (itr.hasNext()) {
				chr = itr.next();

				if (chr.getClient().isLoggedIn() && chr.isGM() && chr.isCallGM()) {
					chr.getClient().write(data);
				}
			}
		} finally {
			this.activePlayerLock.unlock();
		}
	}

	public Collection<ChannelCharacter> getAllCharacters() {
		return this.nameToChar.values();
	}
}