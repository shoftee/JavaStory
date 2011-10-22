/*
 * This file is part of the OdinMS Maple Story Server
 * Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 * Matthias Butz <matze@odinms.de>
 * Jan Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation. You may not use, modify
 * or distribute this program under any other version of the
 * GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.game;

public enum WeaponType {

	NOT_A_WEAPON(4f),
	BOW(3.4f),
	CLAW(3.6f),
	DAGGER(4.0f),
	CROSSBOW(3.6f),
	AXE1H(4.4f),
	SWORD1H(4.0f),
	BLUNT1H(4.4f),
	AXE2H(3.4f, 4.8f), // Note : Swing = 4.8, Stab = 3.4
	SWORD2H(4.6f),
	BLUNT2H(3.4f, 4.8f), // Note : Swing = 4.8, Stab = 3.4
	POLE_ARM(3.0f, 5.0f), // NOTE : Swing = 5.0, stab = 3.0
	SPEAR(3.0f, 5.0f), // NOTE : Stab = 5.0, swing = 3.0
	STAFF(3.6f),
	WAND(3.6f),
	KNUCKLE(4.8f),
	GUN(3.6f),
	KATARA(4.0f);
	private final float maxDamageMultiplier;
	private final float minDamageMultiplier;

	private WeaponType(final float damageMultiplier) {
		this.minDamageMultiplier = damageMultiplier;
		this.maxDamageMultiplier = damageMultiplier;
	}

	private WeaponType(final float minDamageMultiplier, final float maxDamageMultiplier) {
		this.minDamageMultiplier = minDamageMultiplier;
		this.maxDamageMultiplier = maxDamageMultiplier;

	}

	public final float getMinDamageMultiplier() {
		return minDamageMultiplier;
	}

	public final float getMaxDamageMultiplier() {
		return maxDamageMultiplier;
	}
};
