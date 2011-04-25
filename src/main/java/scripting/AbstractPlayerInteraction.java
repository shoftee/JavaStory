package scripting;

import java.rmi.RemoteException;
import java.awt.Point;
import java.util.List;

import client.Equip;
import client.IItem;
import client.SkillFactory;
import client.GameConstants;
import client.GameCharacter;
import client.GameClient;
import client.InventoryType;
import client.Pet;
import client.QuestStatus;
import handling.world.Party;
import handling.world.PartyCharacter;
import handling.world.guild.Guild;
import org.javastory.server.channel.ChannelManager;
import org.javastory.tools.Randomizer;
import server.InventoryManipulator;
import server.ItemInfoProvider;
import server.maps.GameMap;
import server.maps.Reactor;
import server.maps.GameMapObject;
import server.maps.SavedLocationType;
import server.maps.Event_DojoAgent;
import server.life.Monster;
import server.life.LifeFactory;
import server.quest.Quest;
import tools.MaplePacketCreator;
import tools.packet.PetPacket;
import tools.packet.UIPacket;

public class AbstractPlayerInteraction {

    protected GameClient client;

    public AbstractPlayerInteraction(final GameClient client) {
        this.client = client;
    }

    public final GameClient getClient() {
        return client;
    }

    public final GameCharacter getPlayer() {
        return client.getPlayer();
    }

    public final EventManager getEventManager(final String event) {
        return client.getChannelServer().getEventSM(client.getPlayer().getWorld()).getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return client.getPlayer().getEventInstance();
    }

    public final void warp(final int map) {
        final GameMap mapz = getWarpMap(map);
        client.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
    }

    public final void warp(final int map, final int portal) {
        final GameMap mapz = getWarpMap(map);
        client.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warp(final int map, final String portal) {
        final GameMap mapz = getWarpMap(map);
        client.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warpMap(final int mapid, final int portal) {
        final GameMap map = getMap(mapid);
        for (GameCharacter chr : client.getPlayer().getMap().getCharacters()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public final void playPortalSE() {
        client.write(MaplePacketCreator.showOwnBuffEffect(0, 7));
    }

    private GameMap getWarpMap(final int map) {
        return ChannelManager.getInstance(client.getChannelId()).getMapFactory(client.getPlayer().getWorld()).getMap(map);
    }

    public final GameMap getMap() {
        return client.getPlayer().getMap();
    }

    public final GameMap getMap(final int map) {
        return getWarpMap(map);
    }

    public final void spawnMob(final int id, final int x, final int y) {
        spawnMob(id, 1, new Point(x, y));
    }

    private void spawnMob(final int id, final int qty, final Point pos) {
        for (int i = 0; i < qty; i++) {
            client.getPlayer().getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(id), pos);
        }
    }

    public final void killMob(int ids) {
        client.getPlayer().getMap().killMonster(ids);
    }

    public final void killAllMob() {
        client.getPlayer().getMap().killAllMonsters(true);
    }

    public final void addHP(final int delta) {
        client.getPlayer().addHP(delta);
    }

    public final int getPlayerStat(final String type) {
        if (type.equals("LVL")) {
            return client.getPlayer().getLevel();
        } else if (type.equals("STR")) {
            return client.getPlayer().getStat().getStr();
        } else if (type.equals("DEX")) {
            return client.getPlayer().getStat().getDex();
        } else if (type.equals("INT")) {
            return client.getPlayer().getStat().getInt();
        } else if (type.equals("LUK")) {
            return client.getPlayer().getStat().getLuk();
        } else if (type.equals("HP")) {
            return client.getPlayer().getStat().getHp();
        } else if (type.equals("MP")) {
            return client.getPlayer().getStat().getMp();
        } else if (type.equals("MAXHP")) {
            return client.getPlayer().getStat().getMaxHp();
        } else if (type.equals("MAXMP")) {
            return client.getPlayer().getStat().getMaxMp();
        } else if (type.equals("RAP")) {
            return client.getPlayer().getRemainingAp();
        } else if (type.equals("RSP")) {
            return client.getPlayer().getRemainingSp();
        } else if (type.equals("GID")) {
            return client.getPlayer().getGuildId();
        } else if (type.equals("AID")) {
            return client.getPlayer().getGuild().getAllianceId();
        } else if (type.equals("GRANK")) {
            return client.getPlayer().getGuildRank();
        } else if (type.equals("GM")) {
            return client.getPlayer().isGM() ? 1 : 0;
        } else if (type.equals("GENDER")) {
            return client.getPlayer().getGender();
        } else if (type.equals("FACE")) {
            return client.getPlayer().getFace();
        } else if (type.equals("HAIR")) {
            return client.getPlayer().getHair();
        }
        return -1;
    }

    public final String getName() {
        return client.getPlayer().getName();
    }

    public final boolean haveItem(final int itemid) {
        return haveItem(itemid, 1);
    }

    public final boolean haveItem(final int itemid, final int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public final boolean haveItem(final int itemid, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
        return client.getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public final boolean canHold(final int itemid) {
        return client.getPlayer().getInventoryType(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public final QuestStatus getQuestRecord(final int id) {
        return client.getPlayer().getQuestNAdd(Quest.getInstance(id));
    }

    public final byte getQuestStatus(final int id) {
        return client.getPlayer().getQuestStatus(id);
    }

    public final void forceStartQuest(final int id, final String data) {
        Quest.getInstance(id).forceStart(client.getPlayer(), 0, data);
    }

    public final void forceCompleteQuest(final int id) {
        Quest.getInstance(id).forceComplete(getPlayer(), 0);
    }

    public void spawnNpc(final int npcId) {
        client.getPlayer().getMap().spawnNpc(npcId, client.getPlayer().getPosition());
    }

    public final void spawnNpc(final int npcId, final int x, final int y) {
        client.getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public final void spawnNpc(final int npcId, final Point pos) {
        client.getPlayer().getMap().spawnNpc(npcId, pos);
    }

    public final void removeNpc(final int mapid, final int npcId) {
        client.getChannelServer().getMapFactory(client.getPlayer().getWorld()).getMap(mapid).removeNpc(npcId);
    }

    public final void forceStartReactor(final int mapid, final int id) {
        GameMap map = client.getChannelServer().getMapFactory(client.getPlayer().getWorld()).getMap(mapid);
        Reactor react;
        for (final GameMapObject remo : map.getAllReactor()) {
            react = (Reactor) remo;
            if (react.getReactorId() == id) {
                react.forceStartReactor(client);
                break;
            }
        }
    }

    public final void destroyReactor(final int mapid, final int id) {
        GameMap map = client.getChannelServer().getMapFactory(client.getPlayer().getWorld()).getMap(mapid);
        Reactor react;
        for (final GameMapObject remo : map.getAllReactor()) {
            react = (Reactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(client);
                break;
            }
        }
    }

    public final void hitReactor(final int mapid, final int id) {
        GameMap map = client.getChannelServer().getMapFactory(client.getPlayer().getWorld()).getMap(mapid);
        Reactor react;
        for (final GameMapObject remo : map.getAllReactor()) {
            react = (Reactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(client);
                break;
            }
        }
    }

    public final int getJob() {
        return client.getPlayer().getJob();
    }

    public int getJobId() {
        return getPlayer().getJob();
    }

    public final void gainNX(final int amount) {
        client.getPlayer().modifyCSPoints(1, amount, true);
    }

    public final void gainItem(final int id, final short quantity) {
        gainItem(id, quantity, false, 0);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats) {
        gainItem(id, quantity, randomStats, 0);
    }

    public final void gainItem(final int id, final short quantity, final long period) {
        gainItem(id, quantity, false, period);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period) {
        if (quantity >= 0) {
            final ItemInfoProvider ii = ItemInfoProvider.getInstance();
            final InventoryType type = GameConstants.getInventoryType(id);
            if (!InventoryManipulator.checkSpace(client, id, quantity, "")) {
                return;
            }
            if (type.equals(InventoryType.EQUIP) && !GameConstants.isThrowingStar(id) && !GameConstants.isBullet(id)) {
                final IItem item = randomStats ? ii.randomizeStats((Equip) ii.getEquipById(id)) : ii.getEquipById(id);
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + period);
                }
                InventoryManipulator.addbyItem(client, item);
            } else {
                InventoryManipulator.addById(client, id, quantity, "", null, period);
            }
        } else {
            InventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        client.write(MaplePacketCreator.getShowItemGain(id, quantity, true));
    }

    public final void changeMusic(final String songName) {
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    public final void playerMessage(final String message) {
        playerMessage(5, message); // default playerMessage and mapMessage to use type 5
    }

    public final void mapMessage(final String message) {
        mapMessage(5, message);
    }

    public final void guildMessage(final String message) {
        guildMessage(5, message);
    }

    public final void playerMessage(final int type, final String message) {
        client.write(MaplePacketCreator.serverNotice(type, message));
    }

    public final void mapMessage(final int type, final String message) {
        client.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public final void guildMessage(final int type, final String message) {
        if (getGuild() != null) {
            getGuild().guildMessage(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public final Guild getGuild() {
        try {
            return client.getChannelServer().getWorldInterface().getGuild(getPlayer().getGuildId(), null);
        } catch (final RemoteException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public final Party getParty() {
        return client.getPlayer().getParty();
    }

    public final int getCurrentPartyId(int mapid) {
        return getMap(mapid).getCurrentPartyId();
    }

    public final boolean isLeader() {
        return getParty().getLeader().equals(new PartyCharacter(client.getPlayer()));
    }

    public final boolean isAllPartyMembersAllowedJob(final int job) {
        boolean allow = true;
        for (final GameCharacter mem : client.getChannelServer().getPartyMembers(client.getPlayer().getParty())) {
            if (mem.getJob() / 100 != job) {
                allow = false;
                break;
            }
        }
        return allow;
    }

    public final boolean allMembersHere() {
        boolean allHere = true;
        for (final GameCharacter partymem : client.getChannelServer().getPartyMembers(client.getPlayer().getParty())) { // TODO, store info in MaplePartyCharacter instead
            if (partymem.getMapId() != client.getPlayer().getMapId()) {
                allHere = false;
                break;
            }
        }
        return allHere;
    }

    public final void warpParty(final int mapId) {
        final int cMap = client.getPlayer().getMapId();
        final GameMap target = getMap(mapId);
        for (final PartyCharacter chr : getPlayer().getParty().getMembers()) {
            final GameCharacter curChar = getClient().getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if (curChar != null && curChar.getMapId() == cMap) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public final void givePartyItems(final int id, final short quantity, final List<GameCharacter> party) {
        for (GameCharacter chr : party) {
            if (quantity >= 0) {
                InventoryManipulator.addById(chr.getClient(), id, quantity);
            } else {
                InventoryManipulator.removeById(chr.getClient(), GameConstants.getInventoryType(id), id, -quantity, true, false);
            }
            chr.getClient().write(MaplePacketCreator.getShowItemGain(id, quantity, true));
        }
    }

    public final void givePartyExp(final int amount, final List<GameCharacter> party) {
        for (final GameCharacter chr : party) {
            chr.gainExp(amount * client.getChannelServer().getExpRate(), true, true, true);
        }
    }

    public final void removeFromParty(final int id, final List<GameCharacter> party) {
        for (final GameCharacter chr : party) {
            final int possesed = chr.getInventoryType(GameConstants.getInventoryType(id)).countById(id);
            if (possesed > 0) {
                InventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, possesed, true, false);
                chr.getClient().write(MaplePacketCreator.getShowItemGain(id, (short) -possesed, true));
            }
        }
    }

    public final void useItem(final int id) {
        ItemInfoProvider.getInstance().getItemEffect(id).applyTo(client.getPlayer());
        client.write(UIPacket.getStatusMsg(id));
    }

    public final void cancelItem(final int id) {
        client.getPlayer().cancelEffect(ItemInfoProvider.getInstance().getItemEffect(id), false, -1);
    }

    public final int getMorphState() {
        return client.getPlayer().getMorphState();
    }

    public final void removeAll(final int id) {
        final int possessed = client.getPlayer().getInventoryType(GameConstants.getInventoryType(id)).countById(id);
        if (possessed > 0) {
            InventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, possessed, true, false);
            client.write(MaplePacketCreator.getShowItemGain(id, (short) -possessed, true));
        }
    }

    public final void gainCloseness(final int closeness, final int index) {
        if (getPlayer().getPet(index) != null) {
            getPlayer().getPet(index).setCloseness(getPlayer().getPet(index).getCloseness() + closeness);
            getClient().write(PetPacket.updatePet(getPlayer().getPet(index), true));
        }
    }

    public final void gainClosenessAll(final int closeness) {
        for (final Pet pet : getPlayer().getPets()) {
            if (pet != null) {
                pet.setCloseness(pet.getCloseness() + closeness);
                getClient().write(PetPacket.updatePet(pet, true));
            }
        }
    }

    public final void resetMap(final int mapid) {
        final GameMap map = getMap(mapid);
        map.resetReactors();
        map.killAllMonsters(false);
        for (final GameMapObject i : map.getAllItems()) {
            map.removeMapObject(i);
        }
    }

    public final void openNpc(final int id) {
        NpcScriptManager.getInstance().start(getClient(), id);
    }

    public final int getMapId() {
        return client.getPlayer().getMap().getId();
    }

    public final boolean haveMonster(final int mobid) {
        for (GameMapObject obj : client.getPlayer().getMap().getAllMonster()) {
            final Monster mob = (Monster) obj;
            if (mob.getId() == mobid) {
                return true;
            }
        }
        return false;
    }

    public final int getChannelNumber() {
        return client.getChannelId();
    }

    public final int getMonsterCount(final int mapid) {
        return client.getChannelServer().getMapFactory(client.getPlayer().getWorld()).getMap(mapid).getAllMonster().size();
    }

    public final void teachSkill(final int id, final byte level, final byte masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public final int getPlayerCount(final int mapid) {
        return client.getChannelServer().getMapFactory(client.getPlayer().getWorld()).getMap(mapid).getCharactersSize();
    }

    public final void dojo_getUp() {
        client.write(MaplePacketCreator.Mulung_DojoUp());
        client.write(MaplePacketCreator.Mulung_DojoUp2());
        client.write(MaplePacketCreator.instantMapWarp((byte) 6));
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting) {
        if (dojo) {
            return Event_DojoAgent.warpNextMap(client.getPlayer(), fromresting);
        }
        return Event_DojoAgent.warpNextMap_Agent(client.getPlayer(), fromresting);
    }

    public final int dojo_getPts() {
        return client.getPlayer().getDojo();
    }

    public final int getSavedLocation(final String loc) {
        final Integer ret = client.getPlayer().getSavedLocation(SavedLocationType.fromString(loc));
        if (ret == null || ret == -1) {
            return 102000000;
        }
        return ret;
    }

    public final void saveLocation(final String loc) {
        client.getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public final void clearSavedLocation(final String loc) {
        client.getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public final void summonMsg(final String msg) {
        client.write(UIPacket.summonMessage(msg));
    }

    public final void summonMsg(final int type) {
        client.write(UIPacket.summonMessage(type));
    }

    public final void showInstruction(final String msg, final int width, final int height) {
        client.write(MaplePacketCreator.sendHint(msg, width, height));
    }

    public final void playerSummonHint(final boolean summon) {
        client.write(UIPacket.summonHelper(summon));
    }

    public final void playerSummonMessage(final int type) {
        client.write(UIPacket.summonMessage(type));
    }

    public final void playerSummonMessage(final String message) {
        client.write(UIPacket.summonMessage(message));
    }

    public final String getInfoQuest(final int id) {
        return client.getPlayer().getInfoQuest(id);
    }

    public final void updateInfoQuest(final int id, final String data) {
        client.getPlayer().updateInfoQuest(id, data);
    }

    public final void Aran_Start() {
        client.write(UIPacket.Aran_Start());
    }

    public final void AranTutInstructionalBubble(final String data) {
        client.write(UIPacket.AranTutInstructionalBalloon(data));
    }

    public final void EvanTutInstructionalBubble(final String data) {
        client.write(UIPacket.EvanTutInstructionalBalloon(data));
    }

    public final void EvanDragonEyes() {
        client.write(UIPacket.EvanDragonEyes());
    }

    public final void ShowWZEffect(final String data) {
        client.write(UIPacket.ShowWZEffect(data));
    }

    public final void EarnTitleMsg(final String data) {
        client.write(UIPacket.EarnTitleMsg(data));
    }

    public final void MovieClipIntroUI(final boolean enabled) {
        client.write(UIPacket.IntroDisableUI(enabled));
        client.write(UIPacket.IntroLock(enabled));
    }
}