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
package javastory.channel.life;

import java.util.EnumMap;

import com.google.common.collect.Maps;

public enum ElementalEffectiveness {

	NORMAL, IMMUNE, STRONG, WEAK;

	public static ElementalEffectiveness fromNumber(int value) {
		switch (value) {
		case 1:
			return IMMUNE;
		case 2:
			return STRONG;
		case 3:
			return WEAK;
		default:
			throw new IllegalArgumentException("Unkown effectiveness: " + value);
		}
	}
	
	public static EnumMap<Element, ElementalEffectiveness> fromString(String elementalAttributes) {
		EnumMap<Element, ElementalEffectiveness> map = Maps.newEnumMap(Element.class);
		for (int i = 0; i < elementalAttributes.length(); i += 2) {
			final char elementChar = elementalAttributes.charAt(i);
			final Element element = Element.fromCharacter(elementChar);
			
			final int effectivenessId = elementalAttributes.charAt(i + 1) - '0';
			final ElementalEffectiveness effectiveness = ElementalEffectiveness.fromNumber(effectivenessId);
			map.put(element, effectiveness);
		}
		return map;
	}

}
