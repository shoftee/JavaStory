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

import java.io.Serializable;

public class Equip extends Item implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6945367220595306871L;

	private byte upgradeSlots, level, vicioushammer, itemLevel;
	private short STR, DEX, INT, LUK, hp, mp, physicalAttack, magicAttack,
			physicalDefense, magicDefense, acc, avoid, hands, speed, jump,
			itemEXP;
	private int ringid, job;

	public Equip(final int id, final short position, final byte flag) {
		super(id, position, (short) 1, flag);
		this.ringid = -1;
	}

	public Equip(final int id, final short position, final int ringid, final byte flag) {
		super(id, position, (short) 1, flag);
		this.ringid = ringid;
	}

	@Override
	public Item copy() {
		final Equip ret = new Equip(this.getItemId(), this.getPosition(), this.ringid, this.getFlag());
		ret.STR = this.STR;
		ret.DEX = this.DEX;
		ret.INT = this.INT;
		ret.LUK = this.LUK;
		ret.hp = this.hp;
		ret.mp = this.mp;
		ret.magicAttack = this.magicAttack;
		ret.magicDefense = this.magicDefense;
		ret.physicalAttack = this.physicalAttack;
		ret.physicalDefense = this.physicalDefense;
		ret.acc = this.acc;
		ret.avoid = this.avoid;
		ret.hands = this.hands;
		ret.speed = this.speed;
		ret.jump = this.jump;
		ret.upgradeSlots = this.upgradeSlots;
		ret.level = this.level;
		ret.itemEXP = this.itemEXP;
		ret.itemLevel = this.itemLevel;
		ret.vicioushammer = this.vicioushammer;
		ret.setOwner(this.getOwner());
		ret.setQuantity(this.getQuantity());
		ret.setExpiration(this.getExpiration());
		return ret;
	}

	@Override
	public ItemType getType() {
		return ItemType.EQUIP;
	}

	public byte getUpgradeSlots() {
		return this.upgradeSlots;
	}

	public int getRingId() {
		return this.ringid;
	}

	public short getStr() {
		return this.STR;
	}

	public short getDex() {
		return this.DEX;
	}

	public short getInt() {
		return this.INT;
	}

	public short getLuk() {
		return this.LUK;
	}

	public short getHp() {
		return this.hp;
	}

	public short getMp() {
		return this.mp;
	}

	public short getWatk() {
		return this.physicalAttack;
	}

	public short getMatk() {
		return this.magicAttack;
	}

	public short getWdef() {
		return this.physicalDefense;
	}

	public short getMdef() {
		return this.magicDefense;
	}

	public short getAcc() {
		return this.acc;
	}

	public short getAvoid() {
		return this.avoid;
	}

	public short getHands() {
		return this.hands;
	}

	public short getSpeed() {
		return this.speed;
	}

	public short getJump() {
		return this.jump;
	}

	public int getJob() {
		return this.job;
	}

	public void setStr(short str) {
		if (str < 0) {
			str = 0;
		}
		this.STR = str;
	}

	public void setDex(short dex) {
		if (dex < 0) {
			dex = 0;
		}
		this.DEX = dex;
	}

	public void setInt(short _int) {
		if (_int < 0) {
			_int = 0;
		}
		this.INT = _int;
	}

	public void setLuk(short luk) {
		if (luk < 0) {
			luk = 0;
		}
		this.LUK = luk;
	}

	public void setHp(short hp) {
		if (hp < 0) {
			hp = 0;
		}
		this.hp = hp;
	}

	public void setMp(short mp) {
		if (mp < 0) {
			mp = 0;
		}
		this.mp = mp;
	}

	public void setWatk(short watk) {
		if (watk < 0) {
			watk = 0;
		}
		this.physicalAttack = watk;
	}

	public void setMatk(short matk) {
		if (matk < 0) {
			matk = 0;
		}
		this.magicAttack = matk;
	}

	public void setWdef(short wdef) {
		if (wdef < 0) {
			wdef = 0;
		} else if (wdef > 255) {
			wdef = 255;
		}
		this.physicalDefense = wdef;
	}

	public void setMdef(short mdef) {
		if (mdef < 0) {
			mdef = 0;
		} else if (mdef > 255) {
			mdef = 255;
		}
		this.magicDefense = mdef;
	}

	public void setAcc(short acc) {
		if (acc < 0) {
			acc = 0;
		}
		this.acc = acc;
	}

	public void setAvoid(short avoid) {
		if (avoid < 0) {
			avoid = 0;
		}
		this.avoid = avoid;
	}

	public void setHands(short hands) {
		if (hands < 0) {
			hands = 0;
		}
		this.hands = hands;
	}

	public void setSpeed(short speed) {
		if (speed < 0) {
			speed = 0;
		}
		this.speed = speed;
	}

	public void setJump(short jump) {
		if (jump < 0) {
			jump = 0;
		}
		this.jump = jump;
	}

	public void setUpgradeSlots(final byte upgradeSlots) {
		this.upgradeSlots = upgradeSlots;
	}

	public byte getLevel() {
		return this.level;
	}

	public void setLevel(final byte level) {
		this.level = level;
	}

	public byte getViciousHammer() {
		return this.vicioushammer;
	}

	public void setViciousHammer(final byte ham) {
		this.vicioushammer = ham;
	}

	public byte getItemLevel() {
		return this.itemLevel;
	}

	public void setItemLevel(byte itemLevel) {
		if (itemLevel < 0) {
			itemLevel = 0;
		}
		this.itemLevel = itemLevel;
	}

	public short getItemEXP() {
		return this.itemEXP;
	}

	public void setItemEXP(short itemEXP) {
		if (itemEXP < 0) {
			itemEXP = 0;
		}
		this.itemEXP = itemEXP;
	}

	@Override
	public void setQuantity(final short quantity) {
		if (quantity < 0 || quantity > 1) {
			throw new RuntimeException("Setting the quantity to " + quantity
					+ " on an equip (itemid: " + this.getItemId() + ")");
		}
		super.setQuantity(quantity);
	}

	public void setJob(final int job) {
		this.job = job;
	}

	public void setRingId(final int ringId) {
		this.ringid = ringId;
	}
}