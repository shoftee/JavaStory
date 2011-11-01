package javastory.channel.server;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.tools.packets.ChannelPackets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class AutobanManager implements Runnable {

	private static class ExpirationEntry implements Comparable<ExpirationEntry> {

		public long time;
		public int acc;
		public int points;

		public ExpirationEntry(final long time, final int acc, final int points) {
			this.time = time;
			this.acc = acc;
			this.points = points;
		}

		@Override
		public int compareTo(final AutobanManager.ExpirationEntry o) {
			return (int) (this.time - o.time);
		}
	}

	private final Map<Integer, Integer> points = Maps.newHashMap();
	private final Map<Integer, List<String>> reasons = Maps.newHashMap();
	private final Set<ExpirationEntry> expirations = Sets.newTreeSet();
	private static final int AUTOBAN_POINTS = 1000;
	private static AutobanManager instance = new AutobanManager();

	public static AutobanManager getInstance() {
		return instance;
	}

	public final void autoban(final ChannelClient c, final String reason) {
		if (c.getPlayer().isGM()) {
			c.getPlayer().sendNotice(5, "[WARNING] A/b triggled : " + reason);
			return;
		}
		this.addPoints(c, AUTOBAN_POINTS, 0, reason);
	}

	public final synchronized void addPoints(final ChannelClient c, final int points, final long expiration, final String reason) {
		List<String> reasonList;
		final ChannelCharacter player = c.getPlayer();
		final int acc = player.getAccountId();

		if (this.points.containsKey(acc)) {
			final int SavedPoints = this.points.get(acc);
			if (SavedPoints >= AUTOBAN_POINTS) { // Already auto ban'd.
				return;
			}
			this.points.put(acc, SavedPoints + points); // Add
			reasonList = this.reasons.get(acc);
			reasonList.add(reason);
		} else {
			this.points.put(acc, points);
			reasonList = Lists.newLinkedList();
			reasonList.add(reason);
			this.reasons.put(acc, reasonList);
		}

		if (this.points.get(acc) >= AUTOBAN_POINTS) { // See if it's sufficient
														// to auto ban
			if (player.isGM()) {
				player.sendNotice(5, "[WARNING] A/b triggled : " + reason);
				return;
			}
			final StringBuilder sb = new StringBuilder("a/b ");
			sb.append(player.getName());
			sb.append(" (IP ");
			sb.append(c.getSessionIP());
			sb.append("): ");
			for (final String s : this.reasons.get(acc)) {
				sb.append(s);
				sb.append(", ");
			}
			try {
				ChannelServer.getWorldInterface().broadcastMessage(
					ChannelPackets.serverNotice(0, "[Autoban] " + player.getName() + " banned by the system (Last reason: " + reason + ")"));
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
			// Calendar cal = Calendar.getInstance();
			// cal.add(Calendar.DATE, 60);
			// c.getPlayer().tempban(sb.toString(), cal, 1, false);
			player.ban(sb.toString(), true);
			c.disconnect();
		} else {
			if (expiration > 0) {
				this.expirations.add(new ExpirationEntry(System.currentTimeMillis() + expiration, acc, points));
			}
		}
	}

	@Override
	public final void run() {
		final long now = System.currentTimeMillis();
		for (final ExpirationEntry e : this.expirations) {
			if (e.time <= now) {
				this.points.put(e.acc, this.points.get(e.acc) - e.points);
			} else {
				return;
			}
		}
	}
}