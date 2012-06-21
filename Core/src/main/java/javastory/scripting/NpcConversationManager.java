package javastory.scripting;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.PartyMember;
import javastory.channel.client.ISkill;
import javastory.channel.client.MemberRank;
import javastory.channel.client.SkillEntry;
import javastory.channel.client.SkillFactory;
import javastory.channel.maps.AramiaFireWorks;
import javastory.channel.maps.Event_DojoAgent;
import javastory.channel.maps.GameMap;
import javastory.channel.packet.PlayerShopPacket;
import javastory.channel.server.CarnivalChallenge;
import javastory.channel.server.CarnivalParty;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.ShopFactory;
import javastory.channel.server.Squad;
import javastory.db.Database;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.Inventory;
import javastory.game.Item;
import javastory.game.Stat;
import javastory.game.StatValue;
import javastory.game.data.ItemInfoProvider;
import javastory.game.quest.QuestInfoProvider;
import javastory.server.channel.GuildRanking;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;

import com.google.common.collect.Lists;

public class NpcConversationManager extends AbstractPlayerInteraction {

	private final int npcId, questId;
	private String getText;
	private final byte type; // -1 = NPC, 0 = start quest, 1 = end quest
	private boolean isPendingDisposal;

	public NpcConversationManager(final ChannelClient client, final int npcId, final int questId, final byte type) {
		super(client);
		this.npcId = npcId;
		this.questId = questId;
		this.type = type;
		this.isPendingDisposal = false;
	}

	public int getNpcId() {
		return this.npcId;
	}

	public static int MAX_REBORNS = 3;

	public int getReborns() {
		return this.getPlayer().getReborns();
	}

	public int getVPoints() {
		return this.getPlayer().getVPoints();
	}

	public void gainVPoints(final int gainedpoints) {
		super.client.getPlayer().gainVPoints(gainedpoints);
	}

	public int getNX() {
		return this.getPlayer().getNX();
	}

	public int getWorld() {
		return this.getPlayer().getWorldId();
	}

	public int getQuest() {
		return this.questId;
	}

	public void giveBuff(final int skill, final int level) {
		SkillFactory.getSkill(skill).getEffect(level).applyTo(super.client.getPlayer());
	}

	public byte getType() {
		return this.type;
	}

	public void safeDispose() {
		this.isPendingDisposal = true;
	}

	public void dispose() {
		NpcScriptManager.getInstance().dispose(super.client);
	}

	public void askMapSelection(final String sel) {
		super.client.write(ChannelPackets.getMapSelection(this.npcId, sel));
	}

	public void sendNext(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "00 01", (byte) 0));
	}

	public void sendNextS(final String text, final byte type) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "00 01", type));
	}

	public void sendPrev(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "01 00", (byte) 0));
	}

	public void sendPrevS(final String text, final byte type) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "01 00", type));
	}

	public void sendNextPrev(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "01 01", (byte) 0));
	}

	public void sendNextPrevS(final String text, final byte type) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "01 01", type));
	}

	public void sendOk(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "00 00", (byte) 0));
	}

	public void sendOkS(final String text, final byte type) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0, text, "00 00", type));
	}

	public void sendYesNo(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 2, text, "", (byte) 0));
	}

	public void sendYesNoS(final String text, final byte type) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 2, text, "", type));
	}

	public void askAcceptDecline(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0x0C, text, "", (byte) 0));
	}

	public void askAcceptDeclineNoESC(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 0x0E, text, "", (byte) 0));
	}

	public void askAvatar(final String text, final int... args) {
		super.client.write(ChannelPackets.getNPCTalkStyle(this.npcId, text, args));
	}

	public void sendSimple(final String text) {
		super.client.write(ChannelPackets.getNPCTalk(this.npcId, (byte) 5, text, "", (byte) 0));
	}

	public void sendGetNumber(final String text, final int def, final int min, final int max) {
		super.client.write(ChannelPackets.getNPCTalkNum(this.npcId, text, def, min, max));
	}

	public void sendGetText(final String text) {
		super.client.write(ChannelPackets.getNPCTalkText(this.npcId, text));
	}

	public void setGetText(final String text) {
		this.getText = text;
	}

	public String getText() {
		return this.getText;
	}

	public int setRandomAvatar(final int ticket, final int... args_all) {
		if (!this.haveItem(ticket)) {
			return -1;
		}
		this.gainItem(ticket, (short) -1);
		final int args = args_all[Randomizer.nextInt(args_all.length)];

		final ChannelCharacter player = super.client.getPlayer();
		if (args < 100) {
			player.setSkinColorId(args);
			player.updateSingleStat(Stat.SKIN, args);
		} else if (args < 30000) {
			player.setFaceId(args);
			player.updateSingleStat(Stat.FACE, args);
		} else {
			player.setHairId(args);
			player.updateSingleStat(Stat.HAIR, args);
		}
		player.equipChanged();
		return 1;
	}

	public int setAvatar(final int ticket, final int args) {
		if (!this.haveItem(ticket)) {
			return -1;
		}
		this.gainItem(ticket, (short) -1);

		final ChannelCharacter player = super.client.getPlayer();
		if (args < 100) {
			player.setSkinColorId(args);
			player.updateSingleStat(Stat.SKIN, args);
		} else if (args < 30000) {
			player.setFaceId(args);
			player.updateSingleStat(Stat.FACE, args);
		} else {
			player.setHairId(args);
			player.updateSingleStat(Stat.HAIR, args);
		}
		player.equipChanged();
		return 1;
	}

	public void sendStorage() {
		final ChannelCharacter player = super.client.getPlayer();

		player.setConversationState(4);
		player.getStorage().sendStorage(super.client, this.npcId);
	}

	public void openShop(final int id) {
		ShopFactory.getInstance().getShop(id).sendShop(super.client);
	}

	public int gainGachaponItem(final int id, final int quantity) {
		final Item item = InventoryManipulator.addbyId_Gachapon(super.client, id, (short) quantity);
		if (item == null) {
			return -1;
		}
		final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
		if (rareness > 0) {
			try {
				ChannelServer.getWorldInterface().broadcastMessage(
					ChannelPackets.getGachaponMega(super.client.getPlayer().getName(), " : Lucky winner of Gachapon! Congratulations~", item, rareness));
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}
		return item.getItemId();
	}

	public void changeJob(final int job) {
		super.client.getPlayer().changeJob(job);
	}

	public void startQuest(final int id) {
		QuestInfoProvider.getInfo(id).start(this.getPlayer(), this.npcId);
	}

	public void completeQuest(final int id) {
		QuestInfoProvider.getInfo(id).complete(this.getPlayer(), this.npcId);
	}

	public void forfeitQuest(final int id) {
		QuestInfoProvider.getInfo(id).forfeit(this.getPlayer());
	}

	public String getQuestCustomData() {
		return super.client.getPlayer().getAddQuestStatus(this.questId).getCustomData();
	}

	public void setQuestCustomData(final String customData) {
		this.getPlayer().getAddQuestStatus(this.questId).setCustomData(customData);
	}

	public int getMeso() {
		return this.getPlayer().getMeso();
	}

	public void gainAp(final int amount) {
		super.client.getPlayer().gainAp(amount);
	}

	public void gainMeso(final int gain) {
		super.client.getPlayer().gainMeso(gain, true, false, true);
	}

	public void gainExp(final int gain) {
		super.client.getPlayer().gainExp(gain, true, true, true);
	}

	public void expandInventory(final byte type, final int amt) {
		super.client.getPlayer().getInventoryByTypeByte(type).addSlot((byte) 4);
	}

	public void unequipEverything() {
		final Inventory equipped = this.getPlayer().getEquippedItemsInventory();
		final Inventory equip = this.getPlayer().getEquipInventory();
		final List<Short> ids = Lists.newLinkedList();
		for (final Item item : equipped) {
			ids.add(item.getPosition());
		}
		for (final short id : ids) {
			InventoryManipulator.unequip(super.client, id, equip.getNextFreeSlot());
		}
	}

	public final void clearSkills() {
		final Map<ISkill, SkillEntry> skills = this.getPlayer().getSkills();
		for (final Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
			this.getPlayer().changeSkillLevel(skill.getKey(), (byte) 0, (byte) 0);
		}
	}

	public final boolean isCash(final int itemid) {
		return ItemInfoProvider.getInstance().isCash(itemid);
	}

	public boolean hasSkill(final int skillid) {
		final ISkill theSkill = SkillFactory.getSkill(skillid);
		if (theSkill != null) {
			return super.client.getPlayer().getCurrentSkillLevel(theSkill) > 0;
		}
		return false;
	}

	public void showEffect(final boolean broadcast, final String effect) {
		if (broadcast) {
			super.client.getPlayer().getMap().broadcastMessage(ChannelPackets.showEffect(effect));
		} else {
			super.client.write(ChannelPackets.showEffect(effect));
		}
	}

	public void playSound(final boolean broadcast, final String sound) {
		if (broadcast) {
			super.client.getPlayer().getMap().broadcastMessage(ChannelPackets.playSound(sound));
		} else {
			super.client.write(ChannelPackets.playSound(sound));
		}
	}

	public void environmentChange(final boolean broadcast, final String env) {
		if (broadcast) {
			super.client.getPlayer().getMap().broadcastMessage(ChannelPackets.environmentChange(env, 2));
		} else {
			super.client.write(ChannelPackets.environmentChange(env, 2));
		}
	}

	public void updateBuddyCapacity(final int capacity) {
		super.client.getPlayer().setBuddyCapacity(capacity);
	}

	public int getBuddyCapacity() {
		return super.client.getPlayer().getBuddyCapacity();
	}

	public int partyMembersInMap() {
		int inMap = 0;
		for (final ChannelCharacter char2 : this.getPlayer().getMap().getCharacters()) {
			final PartyMember char2member = char2.getPartyMembership();
			final PartyMember member = this.getPlayer().getPartyMembership();
			if (char2member != null && member != null && char2member.getPartyId() == member.getPartyId()) {
				inMap++;
			}
		}
		return inMap;
	}

	public List<ChannelCharacter> getPartyMembers() {
		if (!this.getPlayer().hasParty()) {
			return null;
		}
		final List<ChannelCharacter> chars = Lists.newLinkedList();
		// TODO: Not done. Finish when ChannelServer remoting is done.
		return chars;
	}

	public void warpPartyWithExp(final int mapId, final int exp) {
		final GameMap target = this.getMap(mapId);
		for (final PartyMember chr : this.getPlayer().getParty().getMembers()) {
			final ChannelCharacter curChar = ChannelServer.getPlayerStorage().getCharacterByName(chr.getName());
			if (curChar.getEventInstance() == null && this.getPlayer().getEventInstance() == null || curChar.getEventInstance() == this.getPlayer().getEventInstance()) {
				curChar.changeMap(target, target.getPortal(0));
				curChar.gainExp(exp, true, false, true);
			}
		}
	}

	public void warpPartyWithExpMeso(final int mapId, final int exp, final int meso) {
		final GameMap target = this.getMap(mapId);
		for (final PartyMember chr : this.getPlayer().getParty().getMembers()) {
			final ChannelCharacter curChar = ChannelServer.getPlayerStorage().getCharacterByName(chr.getName());
			if (curChar.getEventInstance() == null && this.getPlayer().getEventInstance() == null || curChar.getEventInstance() == this.getPlayer().getEventInstance()) {
				curChar.changeMap(target, target.getPortal(0));
				curChar.gainExp(exp, true, false, true);
				curChar.gainMeso(meso, true);
			}
		}
	}

	public int itemQuantity(final int itemid) {
		return this.getPlayer().getInventoryForItem(itemid).countById(itemid);
	}

	public int getSkillLevel(final int skillid) {
		return this.getPlayer().getSkillLevel(skillid);
	}

	public Squad getSquad(final String type) {
		return ChannelServer.getInstance().getMapleSquad(type);
	}

	public int getSquadAvailability(final String type) {
		final Squad squad = ChannelServer.getInstance().getMapleSquad(type);
		if (squad == null) {
			return -1;
		}
		return squad.getStatus();
	}

	public void registerSquad(final String type, final int minutes, final String startText) {
		final ChannelCharacter player = super.client.getPlayer();

		final Squad squad = new Squad(type, player, minutes * 60 * 1000);
		final GameMap map = player.getMap();

		map.broadcastMessage(ChannelPackets.getClock(minutes * 60));
		map.broadcastMessage(ChannelPackets.serverNotice(6, player.getName() + startText));
		ChannelServer.getInstance().addMapleSquad(squad, type);
	}

	public boolean getSquadList(final String type, final byte type_) {
		final Squad squad = ChannelServer.getInstance().getMapleSquad(type);
		if (squad == null) {
			return false;
		}
		if (type_ == 0) { // Normal viewing
			this.sendNext(squad.getSquadMemberString(type_));
		} else if (type_ == 1) { // Squad Leader banning, Check out banned
									// participant
			this.sendSimple(squad.getSquadMemberString(type_));
		} else if (type_ == 2) {
			if (squad.getBannedMemberSize() > 0) {
				this.sendSimple(squad.getSquadMemberString(type_));
			} else {
				this.sendNext(squad.getSquadMemberString(type_));
			}
		}
		return true;
	}

	public byte isSquadLeader(final String type) {
		final Squad squad = ChannelServer.getInstance().getMapleSquad(type);
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

	public void banMember(final String type, final int pos) {
		final Squad squad = ChannelServer.getInstance().getMapleSquad(type);
		if (squad != null) {
			squad.banMember(pos);
		}
	}

	public void acceptMember(final String type, final int pos) {
		final Squad squad = ChannelServer.getInstance().getMapleSquad(type);
		if (squad != null) {
			squad.acceptMember(pos);
		}
	}

	public int addMember(final String type, final boolean join) {
		final Squad squad = ChannelServer.getInstance().getMapleSquad(type);
		if (squad != null) {
			return squad.addMember(super.client.getPlayer(), join);
		}
		return -1;
	}

	public byte isSquadMember(final String type) {
		final Squad squad = ChannelServer.getInstance().getMapleSquad(type);
		if (squad == null) {
			return -1;
		} else {
			final ChannelCharacter player = super.client.getPlayer();

			if (squad.getMembers().contains(player)) {
				return 1;
			} else if (squad.isBanned(player)) {
				return 2;
			} else {
				return 0;
			}
		}
	}

	public void resetReactors() {
		this.getPlayer().getMap().resetReactors();
	}

	public void genericGuildMessage(final int code) {
		super.client.write(ChannelPackets.genericGuildMessage((byte) code));
	}

	public void disbandGuild() {
		final ChannelCharacter player = super.client.getPlayer();
		final int gid = player.getGuildId();
		if (gid <= 0 || player.getGuildRank().equals(MemberRank.MASTER)) {
			return;
		}
		try {
			ChannelServer.getWorldInterface().disbandGuild(gid);
		} catch (final RemoteException e) {
			System.err.println("Error while disbanding guild." + e);
		}
	}

	public void doReborn() {
		if (this.getWorld() == 2) {
			MAX_REBORNS += 3;
		}
		if (this.getPlayer().getReborns() < MAX_REBORNS) {
			this.getPlayer().setReborns(this.getPlayer().getReborns() + 1);
			// unequipEverything();
			final List<StatValue> reborns = Lists.newArrayListWithCapacity(4);
			this.getPlayer().setLevel(1);
			this.getPlayer().setExp(0);
			reborns.add(new StatValue(Stat.LEVEL, Integer.valueOf(1)));
			reborns.add(new StatValue(Stat.EXP, Integer.valueOf(0)));
			// getPlayer().super.client.write(MaplePacketCreator.updatePlayerStats(reborns));
			// getPlayer().getMap().broadcastMessage(getPlayer(),
			// MaplePacketCreator.showJobChange(getPlayer().getId()), false);
		} else {
			this.getPlayer().getClient().write(ChannelPackets.serverNotice(6, "You have reached the maximum amount of rebirths!"));
		}
	}

	public void increaseGuildCapacity() {
		final ChannelCharacter player = super.client.getPlayer();

		if (player.getMeso() < 5000000) {
			super.client.write(ChannelPackets.serverNotice(1, "You do not have enough mesos."));
			return;
		}
		final int gid = player.getGuildId();
		if (gid <= 0) {
			return;
		}
		try {
			ChannelServer.getWorldInterface().increaseGuildCapacity(gid);
		} catch (final RemoteException e) {
			System.err.println("Error while increasing capacity." + e);
			return;
		}
		player.gainMeso(-5000000, true, false, true);
	}

	public void displayGuildRanks() {
		super.client.write(ChannelPackets.showGuildRanks(this.npcId, GuildRanking.getInstance().getRank()));
	}

	public boolean removePlayerFromInstance() {
		final ChannelCharacter player = super.client.getPlayer();
		if (player.getEventInstance() != null) {
			player.getEventInstance().removePlayer(player);
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

	public void changeStat(final byte slot, final int type, final short amount) {
		final Equip sel = (Equip) super.client.getPlayer().getEquippedItemsInventory().getItem(slot);
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
			final Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM hiredmerchants WHERE merchantid = ?");
			ps.setInt(1, this.getPlayer().getId());
			final ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
			} else {
				mesos = rs.getLong("mesos");
			}
			rs.close();
			ps.close();

			ps = con.prepareStatement("UPDATE hiredmerchants SET mesos = 0 WHERE merchantid = ?");
			ps.setInt(1, this.getPlayer().getId());
			ps.executeUpdate();
			ps.close();

		} catch (final SQLException ex) {
			System.err.println("Error gaining mesos in hired merchant" + ex);
		}
		super.client.getPlayer().gainMeso((int) mesos, true);
	}

	public long getMerchantMesos() {
		long mesos = 0;
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("SELECT * FROM hiredmerchants WHERE merchantid = ?")) {
			ps.setInt(1, this.getPlayer().getId());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					mesos = rs.getLong("mesos");
				}
			}
		} catch (final SQLException ex) {
			System.err.println("Error gaining mesos in hired merchant" + ex);
		}
		return mesos;
	}

	public void openDuey() {
		super.client.getPlayer().setConversationState(2);
		super.client.write(ChannelPackets.sendDuey((byte) 9, null));
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
		final ChannelCharacter player = super.client.getPlayer();
		if (dojo) {
			return Event_DojoAgent.warpStartDojo(player, party);
		}
		return Event_DojoAgent.warpStartAgent(player, party);
	}

	public final short getKegs() {
		return AramiaFireWorks.getInstance().getKegsPercentage();
	}

	public void giveKegs(final int kegs) {
		AramiaFireWorks.getInstance().giveKegs(super.client.getPlayer(), kegs);
	}

	public final Inventory getInventory(final byte type) {
		return super.client.getPlayer().getInventoryByTypeByte(type);
	}

	public final CarnivalParty getCarnivalParty() {
		return super.client.getPlayer().getCarnivalParty();
	}

	public final CarnivalChallenge getNextCarnivalRequest() {
		return super.client.getPlayer().getNextCarnivalRequest();
	}

	public void resetStats(final int str, final int dex, final int int_, final int luk) {
		final List<StatValue> stats = Lists.newArrayListWithCapacity(2);
		final ChannelCharacter chr = super.client.getPlayer();
		int total = chr.getStats().getStr() + chr.getStats().getDex() + chr.getStats().getLuk() + chr.getStats().getInt() + chr.getRemainingAp();
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
		super.client.write(ChannelPackets.updatePlayerStats(stats, false, chr.getJobId()));
	}

	public final boolean dropItem(final int slot, final int invType, final int quantity) {
		final Inventory inventory = super.client.getPlayer().getInventoryByTypeByte((byte) invType);
		if (inventory == null) {
			return false;
		}
		InventoryManipulator.drop(super.client, inventory, (short) slot, (short) quantity);
		return true;
	}

	public void maxStats() {
		final List<StatValue> statup = Lists.newArrayListWithCapacity(2);
		final ChannelCharacter player = super.client.getPlayer();

		player.setRemainingAp(0);
		statup.add(new StatValue(Stat.AVAILABLE_AP, Integer.valueOf(0)));
		player.setRemainingSp(0);
		statup.add(new StatValue(Stat.AVAILABLE_SP, Integer.valueOf(0)));

		player.getStats().setStr(32767);
		statup.add(new StatValue(Stat.STR, Integer.valueOf(32767)));
		player.getStats().setDex(32767);
		statup.add(new StatValue(Stat.DEX, Integer.valueOf(32767)));
		player.getStats().setInt(32767);
		statup.add(new StatValue(Stat.INT, Integer.valueOf(32767)));
		player.getStats().setLuk(32767);
		statup.add(new StatValue(Stat.LUK, Integer.valueOf(32767)));

		player.getStats().setHp(30000);
		statup.add(new StatValue(Stat.HP, Integer.valueOf(30000)));
		player.getStats().setMaxHp(30000);
		statup.add(new StatValue(Stat.MAX_HP, Integer.valueOf(30000)));
		player.getStats().setMp(30000);
		statup.add(new StatValue(Stat.MP, Integer.valueOf(30000)));
		player.getStats().setMaxMp(30000);
		statup.add(new StatValue(Stat.MAX_MP, Integer.valueOf(30000)));

		super.client.write(ChannelPackets.updatePlayerStats(statup, player.getJobId()));
	}

	public void gainFame(final int fame) {
		final ChannelCharacter player = super.client.getPlayer();

		player.setFame(fame);
		player.updateSingleStat(Stat.FAME, Integer.valueOf(this.getPlayer().getFame()));
		super.client.write(ChannelPackets.serverNotice(6, "You have gained (+" + fame + ") fame."));
	}

	public boolean isPendingDisposal() {
		return this.isPendingDisposal;
	}
}