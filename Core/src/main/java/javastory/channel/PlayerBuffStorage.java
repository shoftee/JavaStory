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
package javastory.channel;

import java.io.Serializable;
import java.util.Collection;

import javastory.world.core.PlayerCooldownValueHolder;
import javastory.world.core.PlayerDiseaseValueHolder;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class PlayerBuffStorage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2496799531785198823L;

	private final Multimap<Integer, PlayerBuffValueHolder> buffs = HashMultimap.create();
	private final Multimap<Integer, PlayerCooldownValueHolder> cooldowns = HashMultimap.create();
	private final Multimap<Integer, PlayerDiseaseValueHolder> diseases = HashMultimap.create();

	public final void addBuffsToStorage(final int characterId, final Collection<PlayerBuffValueHolder> buffValues) {
		buffs.removeAll(characterId);
		buffs.putAll(characterId, buffValues);
	}

	public final void addCooldownsToStorage(final int characterId, final Collection<PlayerCooldownValueHolder> cooldownValues) {
		cooldowns.removeAll(characterId);
		cooldowns.putAll(characterId, cooldownValues);
	}

	public final void addDiseaseToStorage(final int characterId, final Collection<PlayerDiseaseValueHolder> diseaseValues) {
		diseases.removeAll(characterId);
		diseases.putAll(characterId, diseaseValues);
	}

	public final Collection<PlayerBuffValueHolder> getBuffsFromStorage(final int characterId) {
		return buffs.removeAll(characterId);
	}

	public final Collection<PlayerCooldownValueHolder> getCooldownsFromStorage(final int characterId) {
		return cooldowns.removeAll(characterId);
	}

	public final Collection<PlayerDiseaseValueHolder> getDiseaseFromStorage(final int characterId) {
		return diseases.removeAll(characterId);
	}
}
