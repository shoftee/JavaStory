package javastory.channel.anticheat;

import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javastory.channel.ChannelCharacter;
import javastory.game.GameConstants;
import javastory.server.TimerManager;
import javastory.tools.StringUtil;

import com.google.common.collect.Maps;

public class CheatTracker {

	private final Map<CheatingOffense, CheatingOffenseEntry> offenses = Maps.newConcurrentMap();
	private final WeakReference<ChannelCharacter> chr;
	// For keeping track of speed attack hack.
	private int lastAttackTickCount = 0;
	private byte Attack_tickResetCount = 0;
	private long Server_ClientAtkTickDiff = 0;
	private long lastDamage = 0;
	private long takingDamageSince;
	private int numSequentialDamage = 0;
	private long lastDamageTakenTime = 0;
	private byte numZeroDamageTaken = 0;
	private int numSequentialSummonAttack = 0;
	private long summonSummonTime = 0;
	private int numSameDamage = 0;
	private Point lastMonsterMove;
	private int monsterMoveCount;
	private int attacksWithoutHit = 0;
	private ScheduledFuture<?> invalidationTask;

	public CheatTracker(final ChannelCharacter chr) {
		this.chr = new WeakReference<>(chr);
		this.invalidationTask = TimerManager.getInstance().register(new InvalidationTask(), 60000);
		this.takingDamageSince = System.currentTimeMillis();
	}

	public final void checkAttack(final int skillId, final int tickcount) {
		final short AtkDelay = GameConstants.getAttackDelay(skillId);
		if (tickcount - this.lastAttackTickCount < AtkDelay) {
			this.registerOffense(CheatingOffense.FASTATTACK);
		}
		final long STime_TC = System.currentTimeMillis() - tickcount; // hack =
																		// -
																		// more
		if (this.Server_ClientAtkTickDiff - STime_TC > 250) { // 250 is the ping,
															// TODO
			this.registerOffense(CheatingOffense.FASTATTACK2);
		}
		// if speed hack, client tickcount values will be running at a faster
		// pace
		// For lagging, it isn't an issue since TIME is running simotaniously,
		// client
		// will be sending values of older time

		// System.out.println("Delay [" + skillId + "] = " + (tickcount -
		// lastAttackTickCount) + ", " + (Server_ClientAtkTickDiff - STime_TC));
		this.Attack_tickResetCount++; // Without this, the difference will always be
									// at 100
		if (this.Attack_tickResetCount >= (AtkDelay <= 200 ? 2 : 4)) {
			this.Attack_tickResetCount = 0;
			this.Server_ClientAtkTickDiff = STime_TC;
		}
		this.lastAttackTickCount = tickcount;
	}

	public final void checkTakeDamage(final int damage) {
		this.numSequentialDamage++;
		this.lastDamageTakenTime = System.currentTimeMillis();

		// System.out.println("tb" + timeBetweenDamage);
		// System.out.println("ns" + numSequentialDamage);
		// System.out.println(timeBetweenDamage / 1500 + "(" + timeBetweenDamage
		// / numSequentialDamage + ")");

		if (this.lastDamageTakenTime - this.takingDamageSince / 500 < this.numSequentialDamage) {
			this.registerOffense(CheatingOffense.FAST_TAKE_DAMAGE);
		}
		if (this.lastDamageTakenTime - this.takingDamageSince > 4500) {
			this.takingDamageSince = this.lastDamageTakenTime;
			this.numSequentialDamage = 0;
		}
		/*
		 * (non-thieves)
		 * Min Miss Rate: 2%
		 * Max Miss Rate: 80%
		 * (thieves)
		 * Min Miss Rate: 5%
		 * Max Miss Rate: 95%
		 */
		if (damage == 0) {
			this.numZeroDamageTaken++;
			if (this.numZeroDamageTaken >= 35) { // Num count MSEA a/b players
				this.numZeroDamageTaken = 0;
				this.registerOffense(CheatingOffense.HIGH_AVOID);
			}
		} else if (damage != -1) {
			this.numZeroDamageTaken = 0;
		}
	}

	public final void checkSameDamage(final int dmg) {
		if (dmg > 1 && this.lastDamage == dmg) {
			this.numSameDamage++;

			if (this.numSameDamage > 5) {
				this.numSameDamage = 0;
				this.registerOffense(CheatingOffense.SAME_DAMAGE, this.numSameDamage + " times: " + dmg);
			}
		} else {
			this.lastDamage = dmg;
			this.numSameDamage = 0;
		}
	}

	public final void checkMoveMonster(final Point pos) {
		if (pos == this.lastMonsterMove) {
			this.monsterMoveCount++;
			if (this.monsterMoveCount > 15) {
				this.registerOffense(CheatingOffense.MOVE_MONSTERS);
			}
		} else {
			this.lastMonsterMove = pos;
			this.monsterMoveCount = 1;
		}
	}

	public final void resetSummonAttack() {
		this.summonSummonTime = System.currentTimeMillis();
		this.numSequentialSummonAttack = 0;
	}

	public final boolean checkSummonAttack() {
		this.numSequentialSummonAttack++;
		// estimated
		// System.out.println(numMPRegens + "/" + allowedRegens);
		if ((System.currentTimeMillis() - this.summonSummonTime) / (2000 + 1) < this.numSequentialSummonAttack) {
			this.registerOffense(CheatingOffense.FAST_SUMMON_ATTACK);
			return false;
		}
		return true;
	}

	public final int getAttacksWithoutHit() {
		return this.attacksWithoutHit;
	}

	public final void setAttacksWithoutHit(final boolean increase) {
		if (increase) {
			this.attacksWithoutHit++;
		} else {
			this.attacksWithoutHit = 0;
		}
	}

	public final void registerOffense(final CheatingOffense offense) {
		this.registerOffense(offense, null);
	}

	public final void registerOffense(final CheatingOffense offense, final String param) {
		final ChannelCharacter character = this.chr.get();
		if (character == null || !offense.isEnabled()) {
			return;
		}
		CheatingOffenseEntry entry = this.offenses.get(offense);
		if (entry != null && entry.isExpired()) {
			this.expireEntry(entry);
			entry = null;
		}
		if (entry == null) {
			entry = new CheatingOffenseEntry(offense, character.getId());
		}
		if (param != null) {
			entry.setParam(param);
		}
		entry.incrementCount();
		if (offense.shouldAutoban(entry.getCount())) {
			final byte type = offense.getBanType();
			if (type == 1) {
				// AutobanManager.getInstance().autoban(chrhardref.getClient(),
				// StringUtil.makeEnumHumanReadable(offense.name()));
			} else if (type == 2) {
				if (!character.isGM()) {
					// chrhardref.getClient().disconnect();
				} else {
					character.sendNotice(5, "[WARNING] D/c triggred : " + offense.toString());
				}
			}
			return;
		}
		this.offenses.put(offense, entry);
		CheatingOffensePersister.getInstance().persistEntry(entry);
	}

	public final void expireEntry(final CheatingOffenseEntry coe) {
		this.offenses.remove(coe.getOffense());
	}

	public final int getPoints() {
		int ret = 0;
		CheatingOffenseEntry[] offenses_copy;
		offenses_copy = this.offenses.values().toArray(new CheatingOffenseEntry[this.offenses.size()]);

		for (final CheatingOffenseEntry entry : offenses_copy) {
			if (entry.isExpired()) {
				this.expireEntry(entry);
			} else {
				ret += entry.getPoints();
			}
		}
		return ret;
	}

	public final Map<CheatingOffense, CheatingOffenseEntry> getOffenses() {
		return Collections.unmodifiableMap(this.offenses);
	}

	public final String getSummary() {
		final StringBuilder ret = new StringBuilder();
		final List<CheatingOffenseEntry> offenseList = new ArrayList<>();

		for (final CheatingOffenseEntry entry : this.offenses.values()) {
			if (!entry.isExpired()) {
				offenseList.add(entry);
			}
		}

		Collections.sort(offenseList, new Comparator<CheatingOffenseEntry>() {

			@Override
			public final int compare(final CheatingOffenseEntry o1, final CheatingOffenseEntry o2) {
				final int thisVal = o1.getPoints();
				final int anotherVal = o2.getPoints();
				return thisVal < anotherVal ? 1 : thisVal == anotherVal ? 0 : -1;
			}
		});
		final int to = Math.min(offenseList.size(), 4);
		for (int x = 0; x < to; x++) {
			ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).getOffense().name()));
			ret.append(": ");
			ret.append(offenseList.get(x).getCount());
			if (x != to - 1) {
				ret.append(" ");
			}
		}
		return ret.toString();
	}

	public final void dispose() {
		if (this.invalidationTask != null) {
			this.invalidationTask.cancel(false);
		}
		this.invalidationTask = null;
	}

	private final class InvalidationTask implements Runnable {

		@Override
		public final void run() {
			CheatingOffenseEntry[] offenses_copy;
			synchronized (CheatTracker.this.offenses) {
				offenses_copy = CheatTracker.this.offenses.values().toArray(new CheatingOffenseEntry[CheatTracker.this.offenses.size()]);
			}
			for (final CheatingOffenseEntry offense : offenses_copy) {
				if (offense.isExpired()) {
					CheatTracker.this.expireEntry(offense);
				}
			}
			if (CheatTracker.this.chr.get() == null) {
				CheatTracker.this.dispose();
			}
		}
	}
}