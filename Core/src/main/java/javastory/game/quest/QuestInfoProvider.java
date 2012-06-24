/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.game.quest;

import java.util.List;
import java.util.Map;

import javastory.channel.ChannelCharacter;
import javastory.scripting.NpcScriptManager;
import javastory.tools.packets.ChannelPackets;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 
 * @author shoftee
 */
public final class QuestInfoProvider {

	private static Map<Integer, QuestInfo> QUESTS = Maps.newLinkedHashMap();
	//
	private static final WzDataProvider questData = WzDataProviderFactory.getDataProvider("Quest.wz");
	private static final WzData actions = questData.getData("Act.img");
	private static final WzData requirements = questData.getData("Check.img");
	private static final WzData info = questData.getData("QuestInfo.img");

	private QuestInfoProvider() {
	}

	private static QuestInfo loadQuest(final int questId) {
		final QuestInfo quest = new QuestInfo();
		quest.questId = questId;
		quest.relevantMobs = Maps.newLinkedHashMap();
		// read reqs
		final WzData questRequirements = requirements.getChildByPath(String.valueOf(questId));
		final WzData questActions = actions.getChildByPath(String.valueOf(questId));

		if (questRequirements == null || questActions == null) {
			return null;
		}
		//-------------------------------------------------
		final WzData startReqData = questRequirements.getChildByPath("0");
		quest.startRequirements = Lists.newLinkedList();
		if (startReqData != null) {
			for (final WzData startReq : startReqData.getChildren()) {
				final QuestRequirementType type = QuestRequirementType.getByWZName(startReq.getName());
				if (type.equals(QuestRequirementType.INTERVAL)) {
					quest.repeatable = true;
				}
				final QuestRequirement req = new QuestRequirement(quest.questId, type, startReq);
				if (req.getType().equals(QuestRequirementType.MONSTER)) {
					for (final WzData mob : startReq.getChildren()) {
						quest.relevantMobs.put(WzDataTool.getInt(mob.getChildByPath("id")), WzDataTool.getInt(mob.getChildByPath("count"), 0));
					}
				}
				quest.startRequirements.add(req);
			}

		}
		//-------------------------------------------------
		final WzData completeReqData = questRequirements.getChildByPath("1");
		if (completeReqData != null) {
			if (completeReqData.getChildByPath("endscript") != null) {
				quest.customend = true;
			}
			quest.completionRequirements = Lists.newLinkedList();
			for (final WzData completeReq : completeReqData.getChildren()) {
				final QuestRequirement req = new QuestRequirement(quest.questId, QuestRequirementType.getByWZName(completeReq.getName()), completeReq);
				if (req.getType().equals(QuestRequirementType.MONSTER)) {
					for (final WzData mob : completeReq.getChildren()) {
						quest.relevantMobs.put(WzDataTool.getInt(mob.getChildByPath("id")), WzDataTool.getInt(mob.getChildByPath("count"), 0));
					}
				}
				quest.completionRequirements.add(req);
			}
		}
		// read acts
		final WzData startActionData = questActions.getChildByPath("0");
		quest.startActions = Lists.newLinkedList();
		if (startActionData != null) {
			for (final WzData startAct : startActionData.getChildren()) {
				final QuestAction action = new QuestAction(quest.questId, QuestActionType.getByWZName(startAct.getName()), startAct);
				quest.startActions.add(action);
			}
		}

		final WzData completeActionData = questActions.getChildByPath("1");
		quest.completionActions = Lists.newLinkedList();
		if (completeActionData != null) {
			for (final WzData completeAct : completeActionData.getChildren()) {
				final QuestAction action = new QuestAction(quest.questId, QuestActionType.getByWZName(completeAct.getName()), completeAct);
				quest.completionActions.add(action);
			}
		}

		final WzData questInfo = info.getChildByPath(String.valueOf(questId));
		quest.autoStart = WzDataTool.getInt("autoStart", questInfo, 0) == 1;
		quest.autoPreComplete = WzDataTool.getInt("autoPreComplete", questInfo, 0) == 1;

		return quest;
	}

	public static QuestInfo getInfo(final int questId) {
		QuestInfo quest = QUESTS.get(questId);
		if (quest == null) {
			quest = loadQuest(questId);
			if (quest == null) {
				return null;
			}
			QUESTS.put(questId, quest);
		}
		return quest;
	}

	public static class QuestInfo {

		//
		private int questId;
		private List<QuestRequirement> startRequirements;
		private List<QuestRequirement> completionRequirements;
		private List<QuestAction> startActions;
		private List<QuestAction> completionActions;
		//
		private boolean autoStart;
		private boolean autoPreComplete;
		private boolean repeatable = false, customend = false;
		private Map<Integer, Integer> relevantMobs;

		private QuestInfo() {
		}

		public boolean canStart(final ChannelCharacter c, final Integer npcId) {
			final QuestStatus status = c.getQuestStatus(this.questId);
			final boolean isActive = status.getState() == 0;
			final boolean canRepeat = status.getState() == 2 && this.repeatable;
			if (isActive || !canRepeat) {
				return false;
			}
			for (final QuestRequirement requirement : this.startRequirements) {
				if (!requirement.check(c, npcId)) {
					return false;
				}
			}
			return true;
		}

		public boolean canComplete(final ChannelCharacter c, final Integer npcId) {
			final QuestStatus status = c.getQuestStatus(this.questId);

			if (status == null || status.getState() != 1) {
				return false;
			}
			for (final QuestRequirement requirement : this.completionRequirements) {
				if (!requirement.check(c, npcId)) {
					return false;
				}
			}
			return true;
		}

		public void restoreLostItems(final ChannelCharacter c, final int itemId) {
			for (final QuestAction action : this.startActions) {
				if (action.restoreLostItem(c, itemId)) {
					break;
				}
			}
		}

		public Map<Integer, Integer> getRelevantMobs() {
			return this.relevantMobs;
		}

		public void start(final ChannelCharacter c, final int npcId) {
			if ((this.autoStart || this.checkNpcOnMap(c, npcId)) && this.canStart(c, npcId)) {
				for (final QuestAction a : this.startActions) {
					a.runStart(c, null);
				}
				if (!this.customend) {
					c.startQuest(this.questId, npcId);
				} else {
					NpcScriptManager.getInstance().endQuest(c.getClient(), npcId, this.questId, true);
				}
			}
		}

		public void forfeit(final ChannelCharacter c) {
			final QuestStatus status = c.getQuestStatus(this.questId);
			if (status.getState() != 1) {
				return;
			}
			c.forfeitQuest(this.questId);
		}

		public void complete(final ChannelCharacter c, final int npcId) {
			this.complete(c, npcId, null);
		}

		public void complete(final ChannelCharacter c, final int npcId, final Integer selection) {
			if ((this.autoPreComplete || this.checkNpcOnMap(c, npcId)) && this.canComplete(c, npcId)) {
				for (final QuestAction a : this.completionActions) {
					if (!a.checkEnd(c, selection)) {
						return;
					}
				}
				for (final QuestAction a : this.completionActions) {
					a.runEnd(c, selection);
				}
				c.completeQuest(this.questId, npcId);

				// Quest completion
				c.getClient().write(ChannelPackets.showSpecialEffect(9));

				c.getMap().broadcastMessage(c, ChannelPackets.showSpecialEffect(c.getId(), 9), false);
			}
		}

		private boolean checkNpcOnMap(final ChannelCharacter player, final int npcid) {
			return player.getMap().containsNPC(npcid) != -1;
		}
	}
}
