package client.messages.commands;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.io.File;

import client.GameClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import server.ItemInfoProvider;
import tools.Pair;
import tools.StringUtil;
import provider.WzData;
import provider.WzDataProvider;
import provider.WzDataProviderFactory;
import provider.WzDataTool;

public class SearchCommands implements Command {
	@Override
	public void execute(GameClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
		if (splitted.length == 1) {
			c.getPlayer().dropMessage(6, splitted[0] + ": <NPC> <MOB> <ITEM> <MAP> <SKILL>");
		} else {
			String type = splitted[1];
			String search = StringUtil.joinStringFrom(splitted, 2);
			WzData data = null;
			WzDataProvider dataProvider = WzDataProviderFactory.getDataProvider(new File(System.getProperty("org.javastory.wzpath") + "/" + "String.wz"));
			c.getPlayer().dropMessage(6, "<<Type: " + type + " | Search: " + search + ">>");
			if (type.equalsIgnoreCase("NPC")) {
				List<String> retNpcs = new ArrayList<String>();
				data = dataProvider.getData("Npc.img");
				List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>();
				for (WzData npcIdData : data.getChildren()) {
					npcPairList.add(new Pair<Integer, String>(Integer.parseInt(npcIdData.getName()), WzDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
				}
				for (Pair<Integer, String> npcPair : npcPairList) {
					if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
						retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
					}
				}
				if (retNpcs != null && retNpcs.size() > 0) {
					for (String singleRetNpc : retNpcs) {
						c.getPlayer().dropMessage(6, singleRetNpc);
					}
				} else {
					c.getPlayer().dropMessage(6, "No NPC's Found");
				}
			} else if (type.equalsIgnoreCase("MAP")) {
				List<String> retMaps = new ArrayList<String>();
				data = dataProvider.getData("Map.img");
				List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>();
				for (WzData mapAreaData : data.getChildren()) {
					for (WzData mapIdData : mapAreaData.getChildren()) {
						mapPairList.add(new Pair<Integer, String>(Integer.parseInt(mapIdData.getName()), WzDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + WzDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
					}
				}
				for (Pair<Integer, String> mapPair : mapPairList) {
					if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
						retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
					}
				}
				if (retMaps != null && retMaps.size() > 0) {
					for (String singleRetMap : retMaps) {
						c.getPlayer().dropMessage(6, singleRetMap);
					}
				} else {
					c.getPlayer().dropMessage(6, "No Maps Found");
				}
			} else if (type.equalsIgnoreCase("MOB")) {
				List<String> retMobs = new ArrayList<String>();
				data = dataProvider.getData("Mob.img");
				List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
				for (WzData mobIdData : data.getChildren()) {
					mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), WzDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
				}
				for (Pair<Integer, String> mobPair : mobPairList) {
					if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
						retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
					}
				}
				if (retMobs != null && retMobs.size() > 0) {
					for (String singleRetMob : retMobs) {
						c.getPlayer().dropMessage(6, singleRetMob);
					}
				} else {
					c.getPlayer().dropMessage(6, "No Mob's Found");
				}
			} else if (type.equalsIgnoreCase("REACTOR")) {
				c.getPlayer().dropMessage(6, "Not available at this moment");
			} else if (type.equalsIgnoreCase("ITEM")) {
				List<String> retItems = new ArrayList<String>();
				for (Pair<Integer, String> itemPair : ItemInfoProvider.getInstance().getAllItems()) {
					if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) {
						retItems.add(itemPair.getLeft() + " - " + itemPair.getRight());
					}
				}
				if (retItems != null && retItems.size() > 0) {
					for (String singleRetItem : retItems) {
						c.getPlayer().dropMessage(6, singleRetItem);
					}
				} else {
					c.getPlayer().dropMessage(6, "No Item's Found");
				}
			} else if (type.equalsIgnoreCase("SKILL")) {
				List<String> retSkills = new ArrayList<String>();
				data = dataProvider.getData("Skill.img");
				List<Pair<Integer, String>> skillPairList = new LinkedList<Pair<Integer, String>>();
				for (WzData skillIdData : data.getChildren()) {
					skillPairList.add(new Pair<Integer, String>(Integer.parseInt(skillIdData.getName()), WzDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME")));
				}
				for (Pair<Integer, String> skillPair : skillPairList) {
					if (skillPair.getRight().toLowerCase().contains(search.toLowerCase())) {
						retSkills.add(skillPair.getLeft() + " - " + skillPair.getRight());
					}
				}
				if (retSkills != null && retSkills.size() > 0) {
					for (String singleRetSkill : retSkills) {
						c.getPlayer().dropMessage(6, singleRetSkill);
					}
				} else {
					c.getPlayer().dropMessage(6, "No Skills Found");
				}
			} else {
				c.getPlayer().dropMessage(6, "Sorry, that search call is unavailable");
			}
		}
	}

	@Override
	public CommandDefinition[] getDefinition() {
		return new CommandDefinition[] {
			new CommandDefinition("find", "", "", 3),
			new CommandDefinition("lookup", "", "", 3),
			new CommandDefinition("search", "", "", 3)
		};
	}
}