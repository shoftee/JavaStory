package javastory.channel.anticheat;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Sets;

public class CheatingOffensePersister {

	private final static CheatingOffensePersister instance = new CheatingOffensePersister();
	private final Set<CheatingOffenseEntry> persist = Sets.newLinkedHashSet();
	private final Lock mutex = new ReentrantLock();

	public static CheatingOffensePersister getInstance() {
		return instance;
	}

	public void persistEntry(final CheatingOffenseEntry entry) {
		this.mutex.lock();
		try {
			this.persist.remove(entry); // equal/hashCode h4x
			this.persist.add(entry);
		} finally {
			this.mutex.unlock();
		}
	}
}