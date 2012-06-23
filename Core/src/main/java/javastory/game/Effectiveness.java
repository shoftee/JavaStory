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
package javastory.game;

import java.util.EnumMap;

import com.google.common.collect.Maps;

public enum Effectiveness {

	NORMAL, IMMUNE, STRONG, WEAK;

	public static EnumMap<AttackNature, Effectiveness> fromString(final String attributes) {
		final EnumMap<AttackNature, Effectiveness> map = Maps.newEnumMap(AttackNature.class);
		for (int i = 0; i < attributes.length(); i += 2) {
			final char elementChar = attributes.charAt(i);
			final AttackNature element = AttackNature.fromCharacter(elementChar);

			final int effectivenessId = attributes.charAt(i + 1) - '0';
			final Effectiveness effectiveness = Effectiveness.fromNumber(effectivenessId);
			map.put(element, effectiveness);
		}
		return map;
	}

	private static Effectiveness fromNumber(final int value) {
		switch (value) {
		case 1:
			return IMMUNE;
		case 2:
			return STRONG;
		case 3:
			return WEAK;
		default:
			return NORMAL;
		}
	}
}
