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
package javastory.game.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javastory.game.BanishInfo;
import javastory.game.Element;
import javastory.game.Effectiveness;
import javastory.game.SkillLevelEntry;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;
import javastory.wz.WzDataType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public final class MobInfo {

	private static final WzDataProvider stringRoot = WzDataProviderFactory.getDataProvider("String.wz");
	private static final WzData mobStrings = stringRoot.getData("Mob.img");

	private final int mobId;
	private final byte cp;
	private byte selfDestruction_action;
	private byte tagColor;
	private byte tagBgColor;
	private final byte rareItemDropLevel;
	private final byte hpDisplayType;
	private final short level, physicalDefense, magicDefense, evasion;
	private final int exp, hp, mp, removeAfter, buffToGive, fixedDamage;
	private int selfDestruction_hp;
	private final boolean boss, undead, isFreeForAllLoot;
	private boolean firstAttack;
	private final boolean isExplosiveReward;
	private final boolean mobile;
	private final boolean fly;
	private final boolean onlyNormalAttack;
	private final boolean friendly;
	private final String name;
	private final Map<Element, Effectiveness> effectiveness;
	private List<Integer> revives = Lists.newArrayList();
	private final List<SkillLevelEntry> skills = Lists.newArrayList();
	private BanishInfo banish;

	public MobInfo(final int mobId, final WzData data, final WzData linkedData) {
		WzData info = data.getChildByPath("info");
		this.mobId = mobId;
		this.hp = WzDataTool.getIntConvert("maxHP", info);
		this.mp = WzDataTool.getIntConvert("maxMP", info, 0);
		this.exp = WzDataTool.getIntConvert("exp", info, 0);
		this.level = (short) WzDataTool.getIntConvert("level", info);
		this.removeAfter = WzDataTool.getIntConvert("removeAfter", info, 0);
		this.rareItemDropLevel = (byte) WzDataTool.getIntConvert("rareItemDropLevel", info, 0);
		this.fixedDamage = WzDataTool.getIntConvert("fixedDamage", info, -1);
		this.onlyNormalAttack = WzDataTool.getIntConvert("onlyNormalAttack", info, 0) > 0;
		this.boss = WzDataTool.getIntConvert("boss", info, 0) > 0 || this.mobId == 8810018 || this.mobId == 9410066;
		this.isExplosiveReward = WzDataTool.getIntConvert("explosiveReward", info, 0) > 0;
		this.isFreeForAllLoot = WzDataTool.getIntConvert("publicReward", info, 0) > 0;
		this.undead = WzDataTool.getIntConvert("undead", info, 0) > 0;
		this.name = WzDataTool.getString(this.mobId + "/name", mobStrings, "MISSINGNO");
		this.buffToGive = WzDataTool.getIntConvert("buff", info, -1);
		this.friendly = WzDataTool.getIntConvert("damagedByMob", info, 0) > 0;
		this.cp = (byte) WzDataTool.getIntConvert("getCP", info, 0);
		this.physicalDefense = (short) WzDataTool.getIntConvert("PDDamage", info, 0);
		this.magicDefense = (short) WzDataTool.getIntConvert("MDDamage", info, 0);
		this.evasion = (short) WzDataTool.getIntConvert("eva", info, 0);

		final WzData selfd = info.getChildByPath("selfDestruction");
		if (selfd != null) {
			this.selfDestruction_hp = WzDataTool.getIntConvert("hp", selfd, 0);
			this.selfDestruction_action = (byte) WzDataTool.getIntConvert("action", selfd, -1);
		} else {
			this.selfDestruction_action = (byte) -1;
		}

		final WzData firstAttackData = info.getChildByPath("firstAttack");
		if (firstAttackData != null) {
			if (firstAttackData.getType() == WzDataType.FLOAT) {
				this.firstAttack = Math.round(WzDataTool.getFloat(firstAttackData)) > 0;
			} else {
				this.firstAttack = WzDataTool.getInt(firstAttackData) > 0;
			}
		}
		if (this.isBoss() || isDamageSponge(this.mobId)) {
			if (info.getChildByPath("hpTagColor") == null || info.getChildByPath("hpTagBgcolor") == null) {
				this.tagColor = (byte) 0;
				this.tagBgColor = (byte) 0;
			} else {
				this.tagColor = (byte) WzDataTool.getIntConvert("hpTagColor", info);
				this.tagBgColor = (byte) WzDataTool.getIntConvert("hpTagBgcolor", info);
			}
		}

		final WzData banishData = info.getChildByPath("ban");
		if (banishData != null) {
			this.banish = new BanishInfo(WzDataTool.getString("banMsg", banishData), WzDataTool.getInt("banMap/0/field", banishData, -1), WzDataTool.getString(
				"banMap/0/portal", banishData, "sp"));
		}

		final WzData reviveInfo = info.getChildByPath("revive");
		if (reviveInfo != null) {
			final List<Integer> revives = Lists.newLinkedList();
			for (final WzData bdata : reviveInfo) {
				revives.add(WzDataTool.getInt(bdata));
			}
			this.revives = revives;
		}

		final WzData monsterSkillData = info.getChildByPath("skill");
		if (monsterSkillData != null) {
			int i = 0;
			final List<SkillLevelEntry> skills = Lists.newArrayList();
			while (monsterSkillData.getChildByPath(Integer.toString(i)) != null) {
				final int skill = WzDataTool.getInt(i + "/skill", monsterSkillData, 0);
				final int level = WzDataTool.getInt(i + "/level", monsterSkillData, 0);
				skills.add(new SkillLevelEntry(skill, level));
				i++;
			}
			for (final SkillLevelEntry skill : skills) {
				this.skills.add(skill);
			}
		}

		final String elementString = WzDataTool.getString("elemAttr", info, "");
		this.effectiveness = Effectiveness.fromString(elementString);

		// Other data which isn't in the mob, but might in the linked data

		if (linkedData != null) {
			info = linkedData.getChildByPath("info");
		}

		this.mobile = info.getChildByPath("move") != null;
		this.fly = info.getChildByPath("fly") != null;
		// TS TODO: Make fun of the person who wrote this:
//		OUTER:
//		for (WzData idata : data) {
//			switch (idata.getName()) {
//			case "fly":
//				this.fly = true;
//				this.mobile = true;
//				break OUTER;
//			case "move":
//				this.mobile = true;
//				break;
//			}
//		}

		byte hpDisplayType = -1;
		if (this.getTagColor() > 0) {
			hpDisplayType = 0;
		} else if (this.isFriendly()) {
			hpDisplayType = 1;
		} else if (this.mobId >= 9300184 && this.mobId <= 9300215) {
			// Mulung TC mobs
			hpDisplayType = 2;
		} else if (!this.isBoss() || this.mobId == 9410066) {
			// Not boss and dong dong chiang
			hpDisplayType = 3;
		}

		this.hpDisplayType = hpDisplayType;
	}

	private static boolean isDamageSponge(final int lifeId) {
		switch (lifeId) {
		case 8810018:
		case 8820010:
		case 8820011:
		case 8820012:
		case 8820013:
		case 8820014:
			return true;
		}
		return false;
	}

	public int getExp() {
		return this.exp;
	}

	public int getHp() {
		return this.hp;
	}

	public int getMp() {
		return this.mp;
	}

	public short getLevel() {
		return this.level;
	}

	public byte getSelfD() {
		return this.selfDestruction_action;
	}

	public int getSelfDHp() {
		return this.selfDestruction_hp;
	}

	public int getFixedDamage() {
		return this.fixedDamage;
	}

	public short getPhysicalDefense() {
		return this.physicalDefense;
	}

	public final short getMagicDefense() {
		return this.magicDefense;
	}

	public final short getEvasion() {
		return this.evasion;
	}

	public boolean getOnlyNoramlAttack() {
		return this.onlyNormalAttack;
	}

	public BanishInfo getBanishInfo() {
		return this.banish;
	}

	public int getRemoveAfter() {
		return this.removeAfter;
	}

	public byte getRareItemDropLevel() {
		return this.rareItemDropLevel;
	}

	public boolean isBoss() {
		return this.boss;
	}

	public boolean isFreeForAllLoot() {
		return this.isFreeForAllLoot;
	}

	public boolean isExplosiveReward() {
		return this.isExplosiveReward;
	}

	public boolean getMobile() {
		return this.mobile;
	}

	public boolean getFly() {
		return this.fly;
	}

	public List<Integer> getRevives() {
		return this.revives;
	}

	public boolean getUndead() {
		return this.undead;
	}

	public ImmutableMap<Element, Effectiveness> getEffectivenessMap() {
		return ImmutableMap.copyOf(this.effectiveness);
	}

	public String getName() {
		return this.name;
	}

	public byte getTagColor() {
		return this.tagColor;
	}

	public byte getTagBgColor() {
		return this.tagBgColor;
	}

	public List<SkillLevelEntry> getSkills() {
		return Collections.unmodifiableList(this.skills);
	}

	public byte getNoSkills() {
		return (byte) this.skills.size();
	}

	public boolean hasSkill(final int skillId, final int level) {
		for (final SkillLevelEntry skill : this.skills) {
			if (skill.skill == skillId && skill.level == level) {
				return true;
			}
		}
		return false;
	}

	public boolean isFirstAttack() {
		return this.firstAttack;
	}

	public byte getCP() {
		return this.cp;
	}

	public boolean isFriendly() {
		return this.friendly;
	}

	public int getBuffToGive() {
		return this.buffToGive;
	}

	public byte getHpDisplayType() {
		return this.hpDisplayType;
	}

	public int getMobId() {
		return this.mobId;
	}
}
