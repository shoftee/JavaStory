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
package javastory.game.quest;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javastory.game.quest.QuestInfoProvider.QuestInfo;

public final class QuestStatus implements Serializable {

	private int questId;
	private static final long serialVersionUID = 91795419934134L;
	private byte state;
	private Map<Integer, Integer> killedMobs = null;
	private int npcId;
	private long completionTime;
	private int forfeited = 0;
	private String customData;

	public QuestStatus(final int questId, final byte status) {
		this.questId = questId;
		this.state = status;
		this.completionTime = System.currentTimeMillis();
		if (status == 1) {
			if (getQuestInfo().getRelevantMobs().size() > 0) {
				killedMobs = new LinkedHashMap<>();
				registerMobs();
			}
		}
	}

	public QuestStatus(final int questId, final byte status, final int npc) {
		this(questId, status);
		this.setNpc(npc);
	}

	public QuestStatus(ResultSet rs) throws SQLException {
		this.state = rs.getByte("status");
		this.questId = rs.getInt("quest");
		final long timestamp = rs.getLong("time");
		this.completionTime = timestamp;
		if (timestamp > -1) {
			completionTime *= 1000;
		}
		this.forfeited = rs.getInt("forfeited");
		this.customData = rs.getString("customData");
	}

	private QuestInfo getQuestInfo() {
		return QuestInfoProvider.getInfo(questId);
	}

	public int getQuestId() {
		return questId;
	}

	public byte getState() {
		return state;
	}

	public void setState(final byte status) {
		this.state = status;
	}

	public int getNpc() {
		return npcId;
	}

	public void setNpc(final int npc) {
		this.npcId = npc;
	}

	private void registerMobs() {
		for (final int i : getQuestInfo().getRelevantMobs().keySet()) {
			killedMobs.put(i, 0);
		}
	}

	private int maxMob(final int monsterId) {
		for (final Map.Entry<Integer, Integer> qs : getQuestInfo()
				.getRelevantMobs().entrySet()) {
			if (qs.getKey() == monsterId) {
				return qs.getValue();
			}
		}
		return 0;
	}

	public boolean mobKilled(final int monsterId) {
		final Integer mob = killedMobs.get(monsterId);
		if (mob != null) {
			killedMobs.put(monsterId, Math.min(mob + 1, maxMob(monsterId)));
			return true;
		}
		return false;
	}

	public void setMobKills(final int monsterId, final int count) {
		killedMobs.put(monsterId, count);
	}

	public boolean hasMobKills() {
		if (killedMobs == null) {
			return false;
		}
		return killedMobs.size() > 0;
	}

	public int getMobKills(final int monsterId) {
		final Integer mob = killedMobs.get(monsterId);
		if (mob == null) {
			return 0;
		}
		return mob;
	}

	public Map<Integer, Integer> getMobKills() {
		return killedMobs;
	}

	public long getCompletionTime() {
		return completionTime;
	}

	public int getForfeited() {
		return forfeited;
	}

	public String getCustomData() {
		return customData;
	}

	public void setCustomData(String data) {
		this.customData = data;
	}

	public void forfeit() {
		if (state != (byte) 1) {
			return;
		}
		forfeited++;
		state = 0;
	}

	public void start(int npcId, String customData) {
		this.state = 1;
		this.customData = customData;
		this.npcId = npcId;
	}

	public void complete(int npcId) {
		this.state = 2;
		this.completionTime = System.currentTimeMillis();
		this.npcId = npcId;
	}
}
