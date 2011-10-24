/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package javastory.game.quest;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javastory.channel.ChannelCharacter;
import javastory.scripting.NpcScriptManager;
import javastory.tools.packets.ChannelPackets;
import javastory.wz.WzData;
import javastory.wz.WzDataProvider;
import javastory.wz.WzDataProviderFactory;
import javastory.wz.WzDataTool;

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

	private static QuestInfo loadQuest(int questId) {
		QuestInfo quest = new QuestInfo();
		quest.questId = questId;
		quest.relevantMobs = new LinkedHashMap<>();
		// read reqs
		final WzData questRequirements = requirements.getChildByPath(String.valueOf(questId));
		final WzData questActions = actions.getChildByPath(String.valueOf(questId));

		if (questRequirements == null && questActions == null) {
			return null;
		}
		//-------------------------------------------------
		final WzData startReqData = questRequirements.getChildByPath("0");
		quest.startRequirements = new LinkedList<>();
		if (startReqData != null) {
			for (WzData startReq : startReqData.getChildren()) {
				final QuestRequirementType type = QuestRequirementType.getByWZName(startReq.getName());
				if (type.equals(QuestRequirementType.INTERVAL)) {
					quest.repeatable = true;
				}
				final QuestRequirement req = new QuestRequirement(quest.questId, type, startReq);
				if (req.getType().equals(QuestRequirementType.MONSTER)) {
					for (WzData mob : startReq.getChildren()) {
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
			quest.completionRequirements = new LinkedList<>();
			for (WzData completeReq : completeReqData.getChildren()) {
				QuestRequirement req = new QuestRequirement(quest.questId, QuestRequirementType.getByWZName(completeReq.getName()), completeReq);
				if (req.getType().equals(QuestRequirementType.MONSTER)) {
					for (WzData mob : completeReq.getChildren()) {
						quest.relevantMobs.put(WzDataTool.getInt(mob.getChildByPath("id")), WzDataTool.getInt(mob.getChildByPath("count"), 0));
					}
				}
				quest.completionRequirements.add(req);
			}
		}
		// read acts
		final WzData startActionData = questActions.getChildByPath("0");
		quest.startActions = new LinkedList<>();
		if (startActionData != null) {
			for (WzData startAct : startActionData.getChildren()) {
				final QuestAction action = new QuestAction(quest.questId, QuestActionType.getByWZName(startAct.getName()), startAct);
				quest.startActions.add(action);
			}
		}

		final WzData completeActionData = questActions.getChildByPath("1");
		quest.completionActions = new LinkedList<>();
		if (completeActionData != null) {
			for (WzData completeAct : completeActionData.getChildren()) {
				final QuestAction action = new QuestAction(quest.questId, QuestActionType.getByWZName(completeAct.getName()), completeAct);
				quest.completionActions.add(action);
			}
		}

		final WzData questInfo = info.getChildByPath(String.valueOf(questId));
		quest.autoStart = WzDataTool.getInt("autoStart", questInfo, 0) == 1;
		quest.autoPreComplete = WzDataTool.getInt("autoPreComplete", questInfo, 0) == 1;

		return quest;
	}

	public static QuestInfo getInfo(int questId) {
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

		public boolean canStart(ChannelCharacter c, Integer npcId) {
			QuestStatus status = c.getQuestStatus(questId);
			final boolean isActive = status.getState() == 0;
			final boolean canRepeat = status.getState() == 2 && repeatable;
			if (isActive || !canRepeat) {
				return false;
			}
			for (QuestRequirement requirement : startRequirements) {
				if (!requirement.check(c, npcId)) {
					return false;
				}
			}
			return true;
		}

		public boolean canComplete(ChannelCharacter c, Integer npcId) {
			QuestStatus status = c.getQuestStatus(questId);

			if (status == null || status.getState() != 1) {
				return false;
			}
			for (QuestRequirement requirement : completionRequirements) {
				if (!requirement.check(c, npcId)) {
					return false;
				}
			}
			return true;
		}

		public void restoreLostItems(final ChannelCharacter c, final int itemId) {
			for (final QuestAction action : startActions) {
				if (action.restoreLostItem(c, itemId)) {
					break;
				}
			}
		}

		public Map<Integer, Integer> getRelevantMobs() {
			return relevantMobs;
		}

		public void start(ChannelCharacter c, int npcId) {
			if ((autoStart || checkNpcOnMap(c, npcId)) && canStart(c, npcId)) {
				for (QuestAction a : startActions) {
					a.runStart(c, null);
				}
				if (!customend) {
					c.startQuest(questId, npcId);
				} else {
					NpcScriptManager.getInstance().endQuest(c.getClient(), npcId, questId, true);
				}
			}
		}

		public void forfeit(ChannelCharacter c) {
			QuestStatus status = c.getQuestStatus(questId);
			if (status.getState() != 1) {
				return;
			}
			c.forfeitQuest(questId);
		}

		public void complete(ChannelCharacter c, int npcId) {
			complete(c, npcId, null);
		}

		public void complete(ChannelCharacter c, int npcId, Integer selection) {
			if ((autoPreComplete || checkNpcOnMap(c, npcId)) && canComplete(c, npcId)) {
				for (QuestAction a : completionActions) {
					if (!a.checkEnd(c, selection)) {
						return;
					}
				}
				for (QuestAction a : completionActions) {
					a.runEnd(c, selection);
				}
				c.completeQuest(questId, npcId);

				// Quest completion
				c.getClient().write(ChannelPackets.showSpecialEffect(9));

				c.getMap().broadcastMessage(c, ChannelPackets.showSpecialEffect(c.getId(), 9), false);
			}
		}

		private boolean checkNpcOnMap(ChannelCharacter player, int npcid) {
			return player.getMap().containsNPC(npcid) != -1;
		}
	}
}
