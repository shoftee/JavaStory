package javastory.scripting;

import java.awt.Point;
import java.rmi.RemoteException;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.Guild;
import javastory.channel.PartyMember;
import javastory.channel.client.Pet;
import javastory.channel.client.SkillFactory;
import javastory.channel.life.LifeFactory;
import javastory.channel.life.Monster;
import javastory.channel.maps.Event_DojoAgent;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.Reactor;
import javastory.channel.maps.SavedLocationType;
import javastory.channel.packet.PetPacket;
import javastory.channel.server.InventoryManipulator;
import javastory.client.Equip;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.game.GameConstants;
import javastory.game.InventoryType;
import javastory.game.quest.QuestStatus;
import javastory.server.ItemInfoProvider;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;
import javastory.tools.packets.UIPacket;

public class AbstractPlayerInteraction {

    protected ChannelClient client;

    public AbstractPlayerInteraction(final ChannelClient client) {
        this.client = client;
    }

    public final ChannelClient getClient() {
        return client;
    }

    public final ChannelCharacter getPlayer() {
        return client.getPlayer();
    }

    public final EventManager getEventManager(final String event) {
        return ChannelServer.getInstance().getEventSM().getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return client.getPlayer().getEventInstance();
    }

    public final void warp(final int map) {
        final GameMap target = getWarpMap(map);
        client.getPlayer().changeMap(target, target.getPortal(Randomizer.nextInt(target.getPortals().size())));
    }

    public final void warp(final int mapId, final int portalId) {
        final GameMap target = getWarpMap(mapId);
        client.getPlayer().changeMap(target, target.getPortal(portalId));
    }

    public final void warp(final int mapId, final String portal) {
        final GameMap target = getWarpMap(mapId);
        client.getPlayer().changeMap(target, target.getPortal(portal));
    }

    public final void warpMap(final int mapId, final int portal) {
        final GameMap target = getMap(mapId);
        for (ChannelCharacter chr : client.getPlayer().getMap().getCharacters()) {
            chr.changeMap(target, target.getPortal(portal));
        }
    }

    public final void playPortalSE() {
        client.write(ChannelPackets.showOwnBuffEffect(0, 7));
    }

    private GameMap getWarpMap(final int map) {
		return ChannelServer.getMapFactory().getMap(map);
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
        return client.getPlayer().getInventoryForItem(itemid).getNextFreeSlot()
                > -1;
    }

    public final QuestStatus getQuestRecord(final int id) {
        return client.getPlayer().getQuestStatus(id);
    }

    public final byte getQuestStatus(final int id) {
        return client.getPlayer().getQuestCompletionStatus(id);
    }

    public final void forceStartQuest(final int id, final String data) {
        client.getPlayer().getQuestStatus(id).start(0, data);
    }

    public final void forceCompleteQuest(final int id) {
        client.getPlayer().getQuestStatus(id).complete(0);

    }

    public void spawnNpc(final int npcId) {
        final ChannelCharacter player = client.getPlayer();
        player.getMap().spawnNpc(npcId, player.getPosition());
    }

    public final void spawnNpc(final int npcId, final int x, final int y) {
        client.getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public final void spawnNpc(final int npcId, final Point pos) {
        client.getPlayer().getMap().spawnNpc(npcId, pos);
    }

    public final void removeNpc(final int mapid, final int npcId) {
    	ChannelServer.getInstance();
		ChannelServer.getMapFactory().getMap(mapid).removeNpc(npcId);
    }

    public final void forceStartReactor(final int mapid, final int id) {
        ChannelServer.getInstance();
		GameMap map = ChannelServer.getMapFactory().getMap(mapid);
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
        ChannelServer.getInstance();
		GameMap map = ChannelServer.getMapFactory().getMap(mapid);
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
        ChannelServer.getInstance();
		GameMap map = ChannelServer.getMapFactory().getMap(mapid);
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
        return client.getPlayer().getJobId();
    }

    public int getJobId() {
        return getPlayer().getJobId();
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
        final Inventory inventory = client.getPlayer().getInventoryForItem(id);
        if (quantity >= 0) {
            final ItemInfoProvider ii = ItemInfoProvider.getInstance();
            if (!InventoryManipulator.checkSpace(client, id, quantity, "")) {
                return;
            }
            if (inventory.getType().equals(InventoryType.EQUIP)
                    && !GameConstants.isThrowingStar(id)
                    && !GameConstants.isBullet(id)) {
                final IItem item = randomStats ? ii.randomizeStats((Equip) ii.getEquipById(id)) : ii.getEquipById(id);
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + period);
                }
                InventoryManipulator.addbyItem(client, item);
            } else {
                InventoryManipulator.addById(client, id, quantity, "", null, period);
            }
        } else {
            InventoryManipulator.removeById(client, inventory, id, -quantity, true, false);
        }
        client.write(ChannelPackets.getShowItemGain(id, quantity, true));
    }

    public final void changeMusic(final String songName) {
        getPlayer().getMap().broadcastMessage(ChannelPackets.musicChange(songName));
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
        client.write(ChannelPackets.serverNotice(type, message));
    }

    public final void mapMessage(final int type, final String message) {
        client.getPlayer().getMap().broadcastMessage(ChannelPackets.serverNotice(type, message));
    }

    public final void guildMessage(final int type, final String message) {
        if (getGuild() != null) {
            getGuild().guildMessage(ChannelPackets.serverNotice(type, message));
        }
    }

    public final Guild getGuild() {
        try {
            ChannelServer.getInstance();
			return ChannelServer.getWorldInterface().getGuild(getPlayer().getGuildId());
        } catch (final RemoteException ex) {
            System.err.println("Could not connect to World Server: " + ex);
        }
        return null;
    }

    public final boolean isLeader() {
        return client.getPlayer().getPartyMembership().isLeader();
    }

    public final boolean isAllPartyMembersAllowedJob(final int job) {
        boolean allow = true;
        PartyMember member = client.getPlayer().getPartyMembership();
        for (final ChannelCharacter chr : ChannelServer.getInstance().getPartyMembers(member.getPartyId())) {
            if (chr.getJobId() / 100 != job) {
                allow = false;
                break;
            }
        }
        return allow;
    }

    public final boolean allMembersHere() {
        boolean allHere = true;
        final ChannelCharacter player = client.getPlayer();
        PartyMember member = player.getPartyMembership();
        for (final ChannelCharacter partymem : ChannelServer.getInstance().getPartyMembers(member.getPartyId())) {
            // TODO, store info in MaplePartyCharacter instead
            if (partymem.getMapId() != player.getMapId()) {
                allHere = false;
                break;
            }
        }
        return allHere;
    }

    public final void warpParty(final int mapId) {
        final int cMap = client.getPlayer().getMapId();
        final GameMap target = getMap(mapId);
        PartyMember member = client.getPlayer().getPartyMembership();
        for (final ChannelCharacter chr : ChannelServer.getInstance().getPartyMembers(member.getPartyId())) {
            if (chr != null && chr.getMapId() == cMap) {
                chr.changeMap(target, target.getPortal(0));
            }
        }
    }

    public final void givePartyItems(final int id, final short quantity, final List<ChannelCharacter> party) {
        for (ChannelCharacter chr : party) {
            if (quantity >= 0) {
                InventoryManipulator.addById(chr.getClient(), id, quantity);
            } else {
                InventoryManipulator.removeById(chr.getClient(), chr.getInventoryForItem(id), id, -quantity, true, false);
            }
            chr.getClient().write(ChannelPackets.getShowItemGain(id, quantity, true));
        }
    }

    public final void givePartyExp(final int amount, final List<ChannelCharacter> party) {
        for (final ChannelCharacter chr : party) {
            chr.gainExp(amount * ChannelServer.getInstance().getExpRate(), true, true, true);
        }
    }

    public final void removeFromParty(final int id, final List<ChannelCharacter> party) {
        for (final ChannelCharacter chr : party) {
            final Inventory inventory = chr.getInventoryForItem(id);
            final int possesed = inventory.countById(id);
            if (possesed > 0) {
                InventoryManipulator.removeById(client, inventory, id, possesed, true, false);
                chr.getClient().write(ChannelPackets.getShowItemGain(id, (short) -possesed, true));
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
        final Inventory inventory = client.getPlayer().getInventoryForItem(id);
        final int count = inventory.countById(id);
        if (count > 0) {
            InventoryManipulator.removeById(client, inventory, id, count, true, false);
            client.write(ChannelPackets.getShowItemGain(id, (short) -count, true));
        }
    }

    public final void gainCloseness(final int closeness, final int index) {
        if (getPlayer().getPet(index) != null) {
            getPlayer().getPet(index).setCloseness(getPlayer().getPet(index).getCloseness()
                    + closeness);
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
        ChannelServer.getInstance();
		return ChannelServer.getMapFactory().getMap(mapid).getAllMonster().size();
    }

    public final void teachSkill(final int id, final byte level, final byte masterlevel) {
        getPlayer().changeSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public final int getPlayerCount(final int mapid) {
        ChannelServer.getInstance();
		return ChannelServer.getMapFactory().getMap(mapid).getCharactersSize();
    }

    public final void dojo_getUp() {
        client.write(ChannelPackets.Mulung_DojoUp());
        client.write(ChannelPackets.Mulung_DojoUp2());
        client.write(ChannelPackets.instantMapWarp((byte) 6));
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
        client.write(ChannelPackets.sendHint(msg, width, height));
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