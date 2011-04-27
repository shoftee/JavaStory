package client.messages.commands;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

import org.javastory.client.ChannelClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import org.javastory.game.IdNameEntry;
import server.ItemInfoProvider;
import tools.StringUtil;
import provider.WzData;
import provider.WzDataProvider;
import provider.WzDataProviderFactory;
import provider.WzDataTool;

public class SearchCommands implements Command {

    @Override
    public void execute(ChannelClient c, String[] splitted) throws Exception, IllegalCommandSyntaxException {
        if (splitted.length == 1) {
            c.getPlayer().sendNotice(6, splitted[0] +
                    ": <NPC> <MOB> <ITEM> <MAP> <SKILL>");
            return;
        }
        String commandType = splitted[1].toUpperCase();
        String search = StringUtil.joinStringFrom(splitted, 2);
        WzData data = null;
        WzDataProvider dataProvider = WzDataProviderFactory.getDataProvider("String.wz");
        c.getPlayer().sendNotice(6, "<<Type: " + commandType + " | Search: " +
                search + ">>");
        switch (commandType) {
            case "NPC":
                npcSearch(dataProvider, data, search, c);
                break;
            case "MAP":
                mapSearch(dataProvider, data, search, c);
                break;
            case "MOB":
                monsterSearch(dataProvider, data, search, c);
                break;
            case "ITEM":
                itemSearch(search, c);
                break;
            case "SKILL":
                skillSearch(dataProvider, data, search, c);
                break;
            default:
                c.getPlayer().sendNotice(6, "Sorry, that search call is unavailable");
                break;
        }
    }

    private static void npcSearch(WzDataProvider dataProvider, WzData data, String search, ChannelClient c) throws NumberFormatException {
        List<String> retNpcs = new ArrayList<>();
        List<IdNameEntry> npcPairList = new LinkedList<>();
        for (WzData npcIdData : dataProvider.getData("Npc.img").getChildren()) {
            npcPairList.add(new IdNameEntry(Integer.parseInt(npcIdData.getName()), WzDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
        }
        for (IdNameEntry npcPair : npcPairList) {
            if (npcPair.name.toLowerCase().contains(search.toLowerCase())) {
                retNpcs.add(npcPair.id + " - " + npcPair.name);
            }
        }
        if (retNpcs != null && retNpcs.size() > 0) {
            for (String singleRetNpc : retNpcs) {
                c.getPlayer().sendNotice(6, singleRetNpc);
            }
        } else {
            c.getPlayer().sendNotice(6, "No NPC's Found");
        }
    }

    private static void mapSearch(WzDataProvider dataProvider, WzData data, String search, ChannelClient c) throws NumberFormatException {
        List<String> retMaps = new ArrayList<>();
        List<IdNameEntry> mapPairList = new LinkedList<>();
        for (WzData mapAreaData : dataProvider.getData("Map.img").getChildren()) {
            for (WzData mapIdData : mapAreaData.getChildren()) {
                final String street = WzDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME");
                final String map = WzDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME");
                final IdNameEntry entry = new IdNameEntry(
                        Integer.parseInt(mapIdData.getName()),
                        street + " - " + map);
                mapPairList.add(entry);
            }
        }
        for (IdNameEntry mapPair : mapPairList) {
            if (mapPair.name.toLowerCase().contains(search.toLowerCase())) {
                retMaps.add(mapPair.id + " - " + mapPair.name);
            }
        }
        if (retMaps != null && retMaps.size() > 0) {
            for (String singleRetMap : retMaps) {
                c.getPlayer().sendNotice(6, singleRetMap);
            }
        } else {
            c.getPlayer().sendNotice(6, "No Maps Found");
        }
    }

    private static void monsterSearch(WzDataProvider dataProvider, WzData data, String search, ChannelClient c) throws NumberFormatException {
        List<String> retMobs = new ArrayList<>();
        List<IdNameEntry> mobPairList = new LinkedList<>();
        for (WzData mobIdData : dataProvider.getData("Mob.img").getChildren()) {
            mobPairList.add(new IdNameEntry(Integer.parseInt(mobIdData.getName()), WzDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
        }
        for (IdNameEntry mobPair : mobPairList) {
            if (mobPair.name.toLowerCase().contains(search.toLowerCase())) {
                retMobs.add(mobPair.id + " - " + mobPair.name);
            }
        }
        if (retMobs != null && retMobs.size() > 0) {
            for (String singleRetMob : retMobs) {
                c.getPlayer().sendNotice(6, singleRetMob);
            }
        } else {
            c.getPlayer().sendNotice(6, "No Mob's Found");
        }
    }

    private static void itemSearch(String search, ChannelClient c) {
        List<String> retItems = new ArrayList<>();
        for (IdNameEntry entry : ItemInfoProvider.getInstance().getAllItems()) {
            if (entry.name.toLowerCase().contains(search.toLowerCase())) {
                retItems.add(entry.id + " - " + entry.name);
            }
        }
        if (retItems != null && retItems.size() > 0) {
            for (String singleRetItem : retItems) {
                c.getPlayer().sendNotice(6, singleRetItem);
            }
        } else {
            c.getPlayer().sendNotice(6, "No Item's Found");
        }
    }

    private static void skillSearch(WzDataProvider dataProvider, WzData data, String search, ChannelClient c) throws NumberFormatException {
        List<String> retSkills = new ArrayList<>();
        data = dataProvider.getData("Skill.img");
        List<IdNameEntry> skillPairList = new LinkedList<>();
        for (WzData skillIdData : data.getChildren()) {
            skillPairList.add(new IdNameEntry(Integer.parseInt(skillIdData.getName()), WzDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME")));
        }
        for (IdNameEntry skillPair : skillPairList) {
            if (skillPair.name.toLowerCase().contains(search.toLowerCase())) {
                retSkills.add(skillPair.id + " - " + skillPair.name);
            }
        }
        if (retSkills != null && retSkills.size() > 0) {
            for (String singleRetSkill : retSkills) {
                c.getPlayer().sendNotice(6, singleRetSkill);
            }
        } else {
            c.getPlayer().sendNotice(6, "No Skills Found");
        }
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
                    new CommandDefinition("find", "", "", 3),
                    new CommandDefinition("lookup", "", "", 3),
                    new CommandDefinition("search", "", "", 3)
                };
    }
}