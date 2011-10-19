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

package javastory.client;

import java.io.Serializable;


public class Equip extends Item implements IEquip, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6945367220595306871L;

	private byte upgradeSlots, level, vicioushammer, itemLevel;
	private short STR, DEX, INT, LUK, hp, mp, physicalAttack, magicAttack,
			physicalDefense, magicDefense, acc, avoid, hands, speed, jump,
			itemEXP;
	private int ringid, job;

	public Equip(int id, short position, byte flag) {
		super(id, position, (short) 1, flag);
		this.ringid = -1;
	}

	public Equip(int id, short position, int ringid, byte flag) {
		super(id, position, (short) 1, flag);
		this.ringid = ringid;
	}

	@Override
	public IItem copy() {
		Equip ret = new Equip(getItemId(), getPosition(), ringid, getFlag());
		ret.STR = STR;
		ret.DEX = DEX;
		ret.INT = INT;
		ret.LUK = LUK;
		ret.hp = hp;
		ret.mp = mp;
		ret.magicAttack = magicAttack;
		ret.magicDefense = magicDefense;
		ret.physicalAttack = physicalAttack;
		ret.physicalDefense = physicalDefense;
		ret.acc = acc;
		ret.avoid = avoid;
		ret.hands = hands;
		ret.speed = speed;
		ret.jump = jump;
		ret.upgradeSlots = upgradeSlots;
		ret.level = level;
		ret.itemEXP = itemEXP;
		ret.itemLevel = itemLevel;
		ret.vicioushammer = vicioushammer;
		ret.setOwner(getOwner());
		ret.setQuantity(getQuantity());
		ret.setExpiration(getExpiration());
		return ret;
	}

	@Override
	public ItemType getType() {
		return ItemType.EQUIP;
	}

	@Override
	public byte getUpgradeSlots() {
		return upgradeSlots;
	}

	@Override
	public int getRingId() {
		return ringid;
	}

	@Override
	public short getStr() {
		return STR;
	}

	@Override
	public short getDex() {
		return DEX;
	}

	@Override
	public short getInt() {
		return INT;
	}

	@Override
	public short getLuk() {
		return LUK;
	}

	@Override
	public short getHp() {
		return hp;
	}

	@Override
	public short getMp() {
		return mp;
	}

	@Override
	public short getWatk() {
		return physicalAttack;
	}

	@Override
	public short getMatk() {
		return magicAttack;
	}

	@Override
	public short getWdef() {
		return physicalDefense;
	}

	@Override
	public short getMdef() {
		return magicDefense;
	}

	@Override
	public short getAcc() {
		return acc;
	}

	@Override
	public short getAvoid() {
		return avoid;
	}

	@Override
	public short getHands() {
		return hands;
	}

	@Override
	public short getSpeed() {
		return speed;
	}

	@Override
	public short getJump() {
		return jump;
	}

	public int getJob() {
		return job;
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

	public void setUpgradeSlots(byte upgradeSlots) {
		this.upgradeSlots = upgradeSlots;
	}

	@Override
	public byte getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	@Override
	public byte getViciousHammer() {
		return vicioushammer;
	}

	public void setViciousHammer(byte ham) {
		vicioushammer = ham;
	}

	@Override
	public byte getItemLevel() {
		return itemLevel;
	}

	public void setItemLevel(byte itemLevel) {
		if (itemLevel < 0) {
			itemLevel = 0;
		}
		this.itemLevel = itemLevel;
	}

	@Override
	public short getItemEXP() {
		return itemEXP;
	}

	public void setItemEXP(short itemEXP) {
		if (itemEXP < 0) {
			itemEXP = 0;
		}
		this.itemEXP = itemEXP;
	}

	@Override
	public void setQuantity(short quantity) {
		if (quantity < 0 || quantity > 1) {
			throw new RuntimeException("Setting the quantity to " + quantity
					+ " on an equip (itemid: " + getItemId() + ")");
		}
		super.setQuantity(quantity);
	}

	public void setJob(int job) {
		this.job = job;
	}

	public void setRingId(int ringId) {
		this.ringid = ringId;
	}
}