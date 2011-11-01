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
package javastory.game.quest;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;

import javastory.channel.ChannelCharacter;
import javastory.channel.client.SkillFactory;
import javastory.game.Item;
import javastory.wz.WzData;
import javastory.wz.WzDataTool;

public class QuestRequirement implements Serializable {

	private static final long serialVersionUID = 9179541993413738569L;
	private final int questId;
	private final QuestRequirementType type;
	private int intStore;
	private String stringStore;
	private List<Entry> dataStore;

	private static class Entry {

		public int key;
		public int value;

		public Entry(final int key, final int value) {
			this.key = key;
			this.value = value;
		}
	}

	/** Creates a new instance of MapleQuestRequirement */
	public QuestRequirement(final int questId, final QuestRequirementType type, final WzData data) {
		this.type = type;
		this.questId = questId;

		switch (type) {
		case JOB: {
			final List<WzData> child = data.getChildren();
			this.dataStore = Lists.newLinkedList();

			for (int i = 0; i < child.size(); i++) {
				this.dataStore.add(new Entry(i, WzDataTool.getInt(child.get(i), -1)));
			}
			break;
		}
		case SKILL: {
			final List<WzData> child = data.getChildren();
			this.dataStore = Lists.newLinkedList();

			for (int i = 0; i < child.size(); i++) {
				final WzData childdata = child.get(i);
				this.dataStore.add(new Entry(WzDataTool.getInt(childdata.getChildByPath("id"), 0), WzDataTool.getInt(childdata.getChildByPath("acquire"), 0)));
			}
			break;
		}
		case QUEST: {
			final List<WzData> child = data.getChildren();
			this.dataStore = Lists.newLinkedList();

			for (int i = 0; i < child.size(); i++) {
				final WzData childdata = child.get(i);
				this.dataStore.add(new Entry(WzDataTool.getInt(childdata.getChildByPath("id")), WzDataTool.getInt(childdata.getChildByPath("state"), 0)));
			}
			break;
		}
		case ITEM: {
			final List<WzData> child = data.getChildren();
			this.dataStore = Lists.newLinkedList();

			for (int i = 0; i < child.size(); i++) {
				final WzData childdata = child.get(i);
				this.dataStore.add(new Entry(WzDataTool.getInt(childdata.getChildByPath("id")), WzDataTool.getInt(childdata.getChildByPath("count"), 0)));
			}
			break;
		}
		case NPC:
		case QUEST_COMPLETED:
		case FAME:
		case INTERVAL:
		case MIN_MONSTER_BOOK:
		case MAX_LEVEL:
		case MIN_LEVEL: {
			this.intStore = WzDataTool.getInt(data, -1);
			break;
		}
		case AVAILABLE_UNTIL: {
			this.stringStore = WzDataTool.getString(data, null);
			break;
		}
		case MONSTER: {
			final List<WzData> child = data.getChildren();
			this.dataStore = Lists.newLinkedList();

			for (int i = 0; i < child.size(); i++) {
				final WzData childdata = child.get(i);
				this.dataStore.add(new Entry(WzDataTool.getInt(childdata.getChildByPath("id"), 0), WzDataTool.getInt(childdata.getChildByPath("count"), 0)));
			}
			break;
		}
		case FIELD_ENTER: {
			final WzData zeroField = data.getChildByPath("0");
			if (zeroField != null) {
				this.intStore = WzDataTool.getInt(zeroField);
			} else {
				this.intStore = -1;
			}
			break;
		}
		}
	}

	public boolean check(final ChannelCharacter c, final Integer npcId) {
		switch (this.type) {
		case JOB:
			for (final Entry entry : this.dataStore) {
				if (entry.value == c.getJobId() || c.isGM()) {
					return true;
				}
			}
			return false;
		case SKILL: {
			for (final Entry entry : this.dataStore) {
				final boolean acquire = entry.value > 0;
				final int skill = entry.key;

				final byte masterSkillLevel = c.getMasterSkillLevel(SkillFactory.getSkill(skill));
				if (acquire) {
					if (masterSkillLevel == 0) {
						return false;
					}
				} else {
					if (masterSkillLevel > 0) {
						return false;
					}
				}
			}
			return true;
		}
		case QUEST:
			for (final Entry entry : this.dataStore) {
				final QuestStatus q = c.getQuestStatus(entry.key);
				final int state = entry.value;
				if (state != 0) {
					if (q == null && state == 0) {
						continue;
					}
					if (q == null || q.getState() != state) {
						return false;
					}
				}
			}
			return true;
		case ITEM:
			int itemId;
			short quantity;

			for (final Entry entry : this.dataStore) {
				itemId = entry.key;
				quantity = 0;
				for (final Item item : c.getInventoryForItem(itemId).listById(itemId)) {
					quantity += item.getQuantity();
				}
				final int count = entry.value;
				if (quantity < count || count <= 0 && quantity > 0) {
					return false;
				}
			}
			return true;
		case MIN_LEVEL:
			return c.getLevel() >= this.intStore;
		case MAX_LEVEL:
			return c.getLevel() <= this.intStore;
		case AVAILABLE_UNTIL:
			final String timeStr = this.stringStore;
			final Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), Integer.parseInt(timeStr.substring(6, 8)), Integer
				.parseInt(timeStr.substring(8, 10)), 0);
			return cal.getTimeInMillis() >= System.currentTimeMillis();
		case MONSTER:
			for (final Entry entry : this.dataStore) {
				final int mobId = entry.key;
				final int killReq = entry.value;
				if (c.getQuestStatus(this.questId).getMobKills(mobId) < killReq) {
					return false;
				}
			}
			return true;
		case NPC:
			return npcId == null || npcId == this.intStore;
		case FIELD_ENTER:
			if (this.intStore != -1) {
				return this.intStore == c.getMapId();
			}
			return false;
		case MIN_MONSTER_BOOK:
			if (c.getMonsterBook().getTotalCards() >= this.intStore) {
				return true;
			}
			return false;
		case FAME:
			return c.getFame() <= this.intStore;
		case QUEST_COMPLETED:
			if (c.getNumQuest() >= this.intStore) {
				return true;
			}
			return false;
		case INTERVAL:
			return c.getQuestStatus(this.questId).getState() != 2
				|| c.getQuestStatus(this.questId).getCompletionTime() <= System.currentTimeMillis() - this.intStore * 60 * 1000L;
//			case PET:
//			case MIN_PET_TAMENESS:
		default:
			return true;
		}
	}

	public QuestRequirementType getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return this.type.toString();
	}
}
