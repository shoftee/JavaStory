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

package javastory.channel.client;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javastory.channel.life.MobSkill;

import com.google.common.collect.Maps;

public final class MonsterStatusEffect {

	private final Map<MonsterStatus, Integer> effects;
	private final ISkill skill;
	private final MobSkill mobskill;
	private final boolean monsterSkill;
	private ScheduledFuture<?> cancelTask;
	private ScheduledFuture<?> poisonSchedule;

	public MonsterStatusEffect(final Map<MonsterStatus, Integer> effects, final ISkill skillId, final MobSkill mobskill, final boolean monsterSkill) {
		this.effects = Maps.newEnumMap(effects);
		this.skill = skillId;
		this.monsterSkill = monsterSkill;
		this.mobskill = mobskill;
	}

	public Map<MonsterStatus, Integer> getEffects() {
		return this.effects;
	}

	public Integer setEffect(final MonsterStatus status, final Integer newVal) {
		return this.effects.put(status, newVal);
	}

	public ISkill getSkill() {
		return this.skill;
	}

	public MobSkill getMobSkill() {
		return this.mobskill;
	}

	public boolean isMonsterSkill() {
		return this.monsterSkill;
	}

	public void setCancelTask(final ScheduledFuture<?> cancelTask) {
		this.cancelTask = cancelTask;
	}

	public void removeActiveStatus(final MonsterStatus stat) {
		this.effects.remove(stat);
	}

	public void setPoisonSchedule(final ScheduledFuture<?> poisonSchedule) {
		this.poisonSchedule = poisonSchedule;
	}

	public void cancelTask() {
		if (this.cancelTask != null) {
			this.cancelTask.cancel(false);
		}
		this.cancelTask = null;
	}

	public void cancelPoisonSchedule() {
		if (this.poisonSchedule != null) {
			this.poisonSchedule.cancel(false);
		}
		this.poisonSchedule = null;
	}
}