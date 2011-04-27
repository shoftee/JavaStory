package scripting;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.rmi.RemoteException;

import client.Equip;
import client.ISkill;
import client.IItem;
import org.javastory.client.ChannelCharacter;
import client.GameConstants;
import org.javastory.client.ChannelClient;
import client.Inventory;
import client.SkillFactory;
import client.SkillEntry;
import client.Stat;
import server.CarnivalParty;
import org.javastory.tools.Randomizer;
import server.InventoryManipulator;
import server.ShopFactory;
import server.Squad;
import server.maps.GameMap;
import server.maps.Event_DojoAgent;
import server.maps.AramiaFireWorks;
import tools.MaplePacketCreator;
import tools.packet.PlayerShopPacket;
import org.javastory.server.channel.MapleGuildRanking;
import database.DatabaseConnection;
import handling.world.PartyMember;
import org.javastory.client.MemberRank;
import org.javastory.quest.QuestInfoProvider;
import org.javastory.server.channel.ChannelManager;
import org.javastory.server.channel.ChannelServer;
import server.CarnivalChallenge;
import server.ItemInfoProvider;
import server.StatEffect.StatValue;

public class NpcConversationManager extends AbstractPlayerInteraction {

    private int npcId, questId;
    private String getText;
    private byte type; // -1 = NPC, 0 = start quest, 1 = end quest
    private boolean isPendingDisposal;

    public NpcConversationManager(ChannelClient client, int npcId, int questId, byte type) {
        super(client);
        this.npcId = npcId;
        this.questId = questId;
        this.type = type;
        this.isPendingDisposal = false;
    }

    public int getNpcId() {
        return npcId;
    }
    public static int MAX_REBORNS = 3;

    public int getReborns() {
        return getPlayer().getReborns();
    }

    public int getVPoints() {
        return getPlayer().getVPoints();
    }

    public void gainVPoints(int gainedpoints) {
        super.client.getPlayer().gainVPoints(gainedpoints);
    }

    public int getNX() {
        return getPlayer().getNX();
    }

    public int getWorld() {
        return getPlayer().getWorldId();
    }

    public int getQuest() {
        return questId;
    }

    public void giveBuff(int skill, int level) {
        SkillFactory.getSkill(skill).getEffect(level).applyTo(super.client.getPlayer());
    }

    public byte getType() {
        return type;
    }

    public void safeDispose() {
        isPendingDisposal = true;
    }

    public void dispose() {
        NpcScriptManager.getInstance().dispose(super.client);
    }

    public void askMapSelection(final String sel) {
        super.client.write(MaplePacketCreator.getMapSelection(npcId, sel));
    }

    public void sendNext(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "00 01", (byte) 0));
    }

    public void sendNextS(String text, byte type) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "00 01", type));
    }

    public void sendPrev(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "01 00", (byte) 0));
    }

    public void sendPrevS(String text, byte type) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "01 00", type));
    }

    public void sendNextPrev(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "01 01", (byte) 0));
    }

    public void sendNextPrevS(String text, byte type) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "01 01", type));
    }

    public void sendOk(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "00 00", (byte) 0));
    }

    public void sendOkS(String text, byte type) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0, text, "00 00", type));
    }

    public void sendYesNo(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 2, text, "", (byte) 0));
    }

    public void sendYesNoS(String text, byte type) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 2, text, "", type));
    }

    public void askAcceptDecline(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0x0C, text, "", (byte) 0));
    }

    public void askAcceptDeclineNoESC(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 0x0E, text, "", (byte) 0));
    }

    public void askAvatar(String text, int... args) {
        super.client.write(MaplePacketCreator.getNPCTalkStyle(npcId, text, args));
    }

    public void sendSimple(String text) {
        super.client.write(MaplePacketCreator.getNPCTalk(npcId, (byte) 5, text, "", (byte) 0));
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        super.client.write(MaplePacketCreator.getNPCTalkNum(npcId, text, def, min, max));
    }

    public void sendGetText(String text) {
        super.client.write(MaplePacketCreator.getNPCTalkText(npcId, text));
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return getText;
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            super.client.getPlayer().setSkinColorId(args);
            super.client.getPlayer().updateSingleStat(Stat.SKIN, args);
        } else if (args < 30000) {
            super.client.getPlayer().setFaceId(args);
            super.client.getPlayer().updateSingleStat(Stat.FACE, args);
        } else {
            super.client.getPlayer().setHairId(args);
            super.client.getPlayer().updateSingleStat(Stat.HAIR, args);
        }
        super.client.getPlayer().equipChanged();
        return 1;
    }

    public int setAvatar(int ticket, int args) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        if (args < 100) {
            super.client.getPlayer().setSkinColorId(args);
            super.client.getPlayer().updateSingleStat(Stat.SKIN, args);
        } else if (args < 30000) {
            super.client.getPlayer().setFaceId(args);
            super.client.getPlayer().updateSingleStat(Stat.FACE, args);
        } else {
            super.client.getPlayer().setHairId(args);
            super.client.getPlayer().updateSingleStat(Stat.HAIR, args);
        }
        super.client.getPlayer().equipChanged();
        return 1;
    }

    public void sendStorage() {
        super.client.getPlayer().setConversationState(4);
        super.client.getPlayer().getStorage().sendStorage(super.client, npcId);
    }

    public void openShop(int id) {
        ShopFactory.getInstance().getShop(id).sendShop(super.client);
    }

    public int gainGachaponItem(int id, int quantity) {
        final IItem item = InventoryManipulator.addbyId_Gachapon(super.client, id, (short) quantity);
        if (item == null) {
            return -1;
        }
        final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
        if (rareness > 0) {
            try {
                super.client.getChannelServer().getWorldInterface().broadcastMessage(MaplePacketCreator.getGachaponMega(super.client.getPlayer().getName(), " : Lucky winner of Gachapon! Congratulations~", item, rareness).getBytes());
            } catch (RemoteException e) {
                super.client.getChannelServer().pingWorld();
            }
        }
        return item.getItemId();
    }

    public void changeJob(int job) {
        super.client.getPlayer().changeJob(job);
    }

    public void startQuest(int id) {
        QuestInfoProvider.getInfo(id).start(getPlayer(), npcId);
    }

    public void completeQuest(int id) {
        QuestInfoProvider.getInfo(id).complete(getPlayer(), npcId);
    }

    public void forfeitQuest(int id) {
        QuestInfoProvider.getInfo(id).forfeit(getPlayer());
    }

    public String getQuestCustomData() {
        return super.client.getPlayer().getAddQuestStatus(questId).getCustomData();
    }

    public void setQuestCustomData(String customData) {
        getPlayer().getAddQuestStatus(questId).setCustomData(customData);
    }

    public int getMeso() {
        return getPlayer().getMeso();
    }

    public void gainAp(final int amount) {
        super.client.getPlayer().gainAp(amount);
    }

    public void gainMeso(int gain) {
        super.client.getPlayer().gainMeso(gain, true, false, true);
    }

    public void gainExp(int gain) {
        super.client.getPlayer().gainExp(gain, true, true, true);
    }

    public void expandInventory(byte type, int amt) {
        super.client.getPlayer().getInventoryByTypeByte(type).addSlot((byte) 4);
    }

    public void unequipEverything() {
        Inventory equipped = getPlayer().getEquippedItemsInventory();
        Inventory equip = getPlayer().getEquipInventory();
        List<Short> ids = new LinkedList<>();
        for (IItem item : equipped) {
            ids.add(item.getPosition());
        }
        for (short id : ids) {
            InventoryManipulator.unequip(super.client, id, equip.getNextFreeSlot());
        }
    }

    public final void clearSkills() {
        Map<ISkill, SkillEntry> skills = getPlayer().getSkills();
        for (Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
            getPlayer().changeSkillLevel(skill.getKey(), (byte) 0, (byte) 0);
        }
    }

    public final boolean isCash(final int itemid) {
        return ItemInfoProvider.getInstance().isCash(itemid);
    }

    public boolean hasSkill(int skillid) {
        ISkill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return super.client.getPlayer().getCurrentSkillLevel(theSkill) > 0;
        }
        return false;
    }

    public void showEffect(boolean broadcast, String effect) {
        if (broadcast) {
            super.client.getPlayer().getMap().broadcastMessage(MaplePacketCreator.showEffect(effect));
        } else {
            super.client.write(MaplePacketCreator.showEffect(effect));
        }
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            super.client.getPlayer().getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
        } else {
            super.client.write(MaplePacketCreator.playSound(sound));
        }
    }

    public void environmentChange(boolean broadcast, String env) {
        if (broadcast) {
            super.client.getPlayer().getMap().broadcastMessage(MaplePacketCreator.environmentChange(env, 2));
        } else {
            super.client.write(MaplePacketCreator.environmentChange(env, 2));
        }
    }

    public void updateBuddyCapacity(int capacity) {
        super.client.getPlayer().setBuddyCapacity(capacity);
    }

    public int getBuddyCapacity() {
        return super.client.getPlayer().getBuddyCapacity();
    }

    public int partyMembersInMap() {
        int inMap = 0;
        for (ChannelCharacter char2 : getPlayer().getMap().getCharacters()) {
            if (char2.getParty() == getPlayer().getParty()) {
                inMap++;
            }
        }
        return inMap;
    }

    public List<ChannelCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<ChannelCharacter> chars = new LinkedList<>(); // creates an empty array full of shit..
        for (ChannelServer channel : ChannelManager.getAllInstances()) {
            for (ChannelCharacter chr : channel.getPartyMembers(getPlayer().getParty())) {
                if (chr != null) { // double check <3
                    chars.add(chr);
                }
            }
        }
        return chars;
    }

    public void warpPartyWithExp(int mapId, int exp) {
        GameMap target = getMap(mapId);
        for (PartyMember chr : getPlayer().getParty().getMembers()) {
            ChannelCharacter curChar = super.client.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null &&
                    getPlayer().getEventInstance() == null) ||
                    curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        GameMap target = getMap(mapId);
        for (PartyMember chr : getPlayer().getParty().getMembers()) {
            ChannelCharacter curChar = super.client.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null &&
                    getPlayer().getEventInstance() == null) ||
                    curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
                curChar.gainMeso(meso, true);
            }
        }
    }

    public int itemQuantity(int itemid) {
        return getPlayer().getInventoryForItem(itemid).countById(itemid);
    }

    public int getSkillLevel(int skillid) {
        return getPlayer().getSkillLevel(skillid);
    }

    public Squad getSquad(String type) {
        return super.client.getChannelServer().getMapleSquad(type);
    }

    public int getSquadAvailability(String type) {
        final Squad squad = super.client.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        }
        return squad.getStatus();
    }

    public void registerSquad(String type, int minutes, String startText) {
        final Squad squad = new Squad(super.client.getChannelId(), type, super.client.getPlayer(), minutes *
                60 * 1000);
        final GameMap map = super.client.getPlayer().getMap();

        map.broadcastMessage(MaplePacketCreator.getClock(minutes * 60));
        map.broadcastMessage(MaplePacketCreator.serverNotice(6, super.client.getPlayer().getName() +
                startText));
        super.client.getChannelServer().addMapleSquad(squad, type);
    }

    public boolean getSquadList(String type, byte type_) {
        final Squad squad = super.client.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return false;
        }
        if (type_ == 0) { // Normal viewing
            sendNext(squad.getSquadMemberString(type_));
        } else if (type_ == 1) { // Squad Leader banning, Check out banned participant
            sendSimple(squad.getSquadMemberString(type_));
        } else if (type_ == 2) {
            if (squad.getBannedMemberSize() > 0) {
                sendSimple(squad.getSquadMemberString(type_));
            } else {
                sendNext(squad.getSquadMemberString(type_));
            }
        }
        return true;
    }

    public byte isSquadLeader(String type) {
        final Squad squad = super.client.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getLeader().getId() == super.client.getPlayer().getId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public void banMember(String type, int pos) {
        final Squad squad = super.client.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(pos);
        }
    }

    public void acceptMember(String type, int pos) {
        final Squad squad = super.client.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.acceptMember(pos);
        }
    }

    public int addMember(String type, boolean join) {
        final Squad squad = super.client.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            return squad.addMember(super.client.getPlayer(), join);
        }
        return -1;
    }

    public byte isSquadMember(String type) {
        final Squad squad = super.client.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getMembers().contains(super.client.getPlayer())) {
                return 1;
            } else if (squad.isBanned(super.client.getPlayer())) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void genericGuildMessage(int code) {
        super.client.write(MaplePacketCreator.genericGuildMessage((byte) code));
    }

    public void disbandGuild() {
        final ChannelCharacter player = super.client.getPlayer();
        final int gid = player.getGuildId();
        if (gid <= 0 || player.getGuildRank().equals(MemberRank.MASTER)) {
            return;
        }
        try {
            super.client.getChannelServer().getWorldInterface().disbandGuild(gid);
        } catch (RemoteException e) {
            System.err.println("Error while disbanding guild." + e);
        }
    }

    public void doReborn() {
        if (getWorld() == 2) {
            MAX_REBORNS += 3;
        }
        if (getPlayer().getReborns() < MAX_REBORNS) {
            getPlayer().setReborns(getPlayer().getReborns() + 1);
            //unequipEverything();
            List<StatValue> reborns = new ArrayList<>(4);
            getPlayer().setLevel(1);
            getPlayer().setExp(0);
            reborns.add(new StatValue(Stat.LEVEL, Integer.valueOf(1)));
            reborns.add(new StatValue(Stat.EXP, Integer.valueOf(0)));
            //getPlayer().super.client.write(MaplePacketCreator.updatePlayerStats(reborns));
            //getPlayer().getMap().broadcastMessage(getPlayer(), MaplePacketCreator.showJobChange(getPlayer().getId()), false);
        } else {
            getPlayer().getClient().write(MaplePacketCreator.serverNotice(6, "You have reached the maximum amount of rebirths!"));
        }
    }

    public void increaseGuildCapacity() {
        if (super.client.getPlayer().getMeso() < 5000000) {
            super.client.write(MaplePacketCreator.serverNotice(1, "You do not have enough mesos."));
            return;
        }
        final int gid = super.client.getPlayer().getGuildId();
        if (gid <= 0) {
            return;
        }
        try {
            super.client.getChannelServer().getWorldInterface().increaseGuildCapacity(gid);
        } catch (RemoteException e) {
            System.err.println("Error while increasing capacity." + e);
            return;
        }
        super.client.getPlayer().gainMeso(-5000000, true, false, true);
    }

    public void createUnion(String name) {
        super.client.getPlayer().getGuild().createAlliance(super.client, name);
    }

    public boolean hasUnion() {
        return super.client.getPlayer().getGuild().getUnion(super.client) !=
                null;
    }

    public void sendUnionInvite(String charname) {
        ChannelCharacter z = super.client.getChannelServer().getPlayerStorage().getCharacterByName(charname);
        if (z != null) {
            if (z.getGuild().getLeader(z.getClient()) == z) {
                //                z.dropMessage(getPlayer().getName() + " invites your guild to join his alliance");
                //               z.dropMessage("If you want to accept that offer type @accept, else type @decline");
                //               z.setAllianceInvited(getPlayer().getGuild().getAlliance(getPlayer().super.client));
                super.client.getPlayer().getGuildUnion().addGuild(super.client, super.client.getPlayer().getGuildId());
            } else {
                getPlayer().sendNotice(0, "That character is not the leader of the guild");
            }
        } else {
            getPlayer().sendNotice(0, "That character is offline");
        }
    }

    public void displayGuildRanks() {
        super.client.write(MaplePacketCreator.showGuildRanks(npcId, MapleGuildRanking.getInstance().getRank()));
    }

    public boolean removePlayerFromInstance() {
        if (super.client.getPlayer().getEventInstance() != null) {
            super.client.getPlayer().getEventInstance().removePlayer(super.client.getPlayer());
            return true;
        }
        return false;
    }

    public boolean isPlayerInstance() {
        if (super.client.getPlayer().getEventInstance() != null) {
            return true;
        }
        return false;
    }

    public void changeStat(byte slot, int type, short amount) {
        Equip sel = (Equip) super.client.getPlayer().getEquippedItemsInventory().getItem(slot);
        switch (type) {
            case 0:
                sel.setStr(amount);
                break;
            case 1:
                sel.setDex(amount);
                break;
            case 2:
                sel.setInt(amount);
                break;
            case 3:
                sel.setLuk(amount);
                break;
            case 4:
                sel.setHp(amount);
                break;
            case 5:
                sel.setMp(amount);
                break;
            case 6:
                sel.setWatk(amount);
                break;
            case 7:
                sel.setMatk(amount);
                break;
            case 8:
                sel.setWdef(amount);
                break;
            case 9:
                sel.setMdef(amount);
                break;
            case 10:
                sel.setAcc(amount);
                break;
            case 11:
                sel.setAvoid(amount);
                break;
            case 12:
                sel.setHands(amount);
                break;
            case 13:
                sel.setSpeed(amount);
                break;
            case 14:
                sel.setJump(amount);
                break;
            case 15:
                sel.setUpgradeSlots((byte) amount);
                break;
            case 16:
                sel.setViciousHammer((byte) amount);
                break;
            case 17:
                sel.setLevel((byte) amount);
                break;
            default:
                break;
        }
        super.client.getPlayer().equipChanged();
    }

    public void giveMerchantMesos() {
        long mesos = 0;
        try {
            Connection con = (Connection) DatabaseConnection.getConnection();
            PreparedStatement ps = (PreparedStatement) con.prepareStatement("SELECT * FROM hiredmerchants WHERE merchantid = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
            } else {
                mesos = rs.getLong("mesos");
            }
            rs.close();
            ps.close();

            ps = (PreparedStatement) con.prepareStatement("UPDATE hiredmerchants SET mesos = 0 WHERE merchantid = ?");
            ps.setInt(1, getPlayer().getId());
            ps.executeUpdate();
            ps.close();

        } catch (SQLException ex) {
            System.err.println("Error gaining mesos in hired merchant" + ex);
        }
        super.client.getPlayer().gainMeso((int) mesos, true);
    }

    public long getMerchantMesos() {
        long mesos = 0;
        try {
            Connection con = (Connection) DatabaseConnection.getConnection();
            PreparedStatement ps = (PreparedStatement) con.prepareStatement("SELECT * FROM hiredmerchants WHERE merchantid = ?");
            ps.setInt(1, getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
            } else {
                mesos = rs.getLong("mesos");
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error gaining mesos in hired merchant" + ex);
        }
        return mesos;
    }

    public void openDuey() {
        super.client.getPlayer().setConversationState(2);
        super.client.write(MaplePacketCreator.sendDuey((byte) 9, null));
    }

    public void openMerchantItemStore() {
        super.client.getPlayer().setConversationState(3);
        super.client.write(PlayerShopPacket.merchItemStore((byte) 0x22));
    }

    public final int getDojoPoints() {
        return super.client.getPlayer().getDojo();
    }

    public final int getDojoRecord() {
        return super.client.getPlayer().getDojoRecord();
    }

    public void setDojoRecord(final boolean reset) {
        super.client.getPlayer().setDojoRecord(reset);
    }

    public boolean start_DojoAgent(final boolean dojo, final boolean party) {
        if (dojo) {
            return Event_DojoAgent.warpStartDojo(super.client.getPlayer(), party);
        }
        return Event_DojoAgent.warpStartAgent(super.client.getPlayer(), party);
    }

    public final short getKegs() {
        return AramiaFireWorks.getInstance().getKegsPercentage();
    }

    public void giveKegs(final int kegs) {
        AramiaFireWorks.getInstance().giveKegs(super.client.getPlayer(), kegs);
    }

    public final Inventory getInventory(byte type) {
        return super.client.getPlayer().getInventoryByTypeByte(type);
    }

    public final CarnivalParty getCarnivalParty() {
        return super.client.getPlayer().getCarnivalParty();
    }

    public final CarnivalChallenge getNextCarnivalRequest() {
        return super.client.getPlayer().getNextCarnivalRequest();
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk) {
        List<StatValue> stats = new ArrayList<>(2);
        final ChannelCharacter chr = super.client.getPlayer();
        int total = chr.getStats().getStr() + chr.getStats().getDex() +
                chr.getStats().getLuk() + chr.getStats().getInt() +
                chr.getRemainingAp();
        total -= str;
        chr.getStats().setStr(str);
        total -= dex;
        chr.getStats().setDex(dex);
        total -= int_;
        chr.getStats().setInt(int_);
        total -= luk;
        chr.getStats().setLuk(luk);
        chr.setRemainingAp(total);
        stats.add(new StatValue(Stat.STR, str));
        stats.add(new StatValue(Stat.DEX, dex));
        stats.add(new StatValue(Stat.INT, int_));
        stats.add(new StatValue(Stat.LUK, luk));
        stats.add(new StatValue(Stat.AVAILABLE_AP, total));
        super.client.write(MaplePacketCreator.updatePlayerStats(stats, false, super.client.getPlayer().getJobId()));
    }

    public final boolean dropItem(int slot, int invType, int quantity) {
        final Inventory inventory = super.client.getPlayer().getInventoryByTypeByte((byte) invType);
        if (inventory == null) {
            return false;
        }
        InventoryManipulator.drop(super.client, inventory, (short) slot, (short) quantity);
        return true;
    }

    public void maxStats() {
        List<StatValue> statup = new ArrayList<>(2);
        
        super.client.getPlayer().setRemainingAp(0);
        statup.add(new StatValue(Stat.AVAILABLE_AP, Integer.valueOf(0)));
        super.client.getPlayer().setRemainingSp(0);
        statup.add(new StatValue(Stat.AVAILABLE_SP, Integer.valueOf(0)));
        
        super.client.getPlayer().getStats().setStr(32767);
        statup.add(new StatValue(Stat.STR, Integer.valueOf(32767)));
        super.client.getPlayer().getStats().setDex(32767);
        statup.add(new StatValue(Stat.DEX, Integer.valueOf(32767)));
        super.client.getPlayer().getStats().setInt(32767);
        statup.add(new StatValue(Stat.INT, Integer.valueOf(32767)));
        super.client.getPlayer().getStats().setLuk(32767);
        statup.add(new StatValue(Stat.LUK, Integer.valueOf(32767)));
        
        super.client.getPlayer().getStats().setHp(30000);
        statup.add(new StatValue(Stat.HP, Integer.valueOf(30000)));
        super.client.getPlayer().getStats().setMaxHp(30000);
        statup.add(new StatValue(Stat.MAX_HP, Integer.valueOf(30000)));
        super.client.getPlayer().getStats().setMp(30000);
        statup.add(new StatValue(Stat.MP, Integer.valueOf(30000)));
        super.client.getPlayer().getStats().setMaxMp(30000);
        statup.add(new StatValue(Stat.MAX_MP, Integer.valueOf(30000)));
        
        super.client.write(MaplePacketCreator.updatePlayerStats(statup, super.client.getPlayer().getJobId()));
    }

    public void gainFame(int fame) {
        super.client.getPlayer().setFame(fame);
        super.client.getPlayer().updateSingleStat(Stat.FAME, Integer.valueOf(getPlayer().getFame()));
        super.client.write(MaplePacketCreator.serverNotice(6, "You have gained (+" +
                fame +
                ") fame."));
    }

    public boolean isPendingDisposal() {
        return isPendingDisposal;
    }
}