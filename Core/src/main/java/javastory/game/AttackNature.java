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

public enum AttackNature {
	NEUTRAL, PHYSICAL, FIRE, ICE, LIGHTING, POISON, HOLY, DARKNESS;

	public static AttackNature fromCharacter(final char c) {
		switch (Character.toUpperCase(c)) {
		case 'F':
			return FIRE;
		case 'I':
			return ICE;
		case 'L':
			return LIGHTING;
		case 'S':
			return POISON;
		case 'H':
			return HOLY;
		case 'P':
			return PHYSICAL;
		case 'D':
			return DARKNESS;
			
		default:
			return null;
		}
	}
}