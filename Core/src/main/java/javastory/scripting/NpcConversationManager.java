package javastory.scripting;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
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
import javastory.client.Equip;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.client.Stat;
import javastory.db.Database;
import javastory.game.GameConstants;
import javastory.game.quest.QuestInfoProvider;
import javastory.server.ItemInfoProvider;
import javastory.server.StatValue;
import javastory.server.channel.MapleGuildRanking;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;

public class NpcConversationManager extends AbstractPlayerInteraction {

	private int npcId, questId;
	private String getText;
	private byte type; // -1 = NPC, 0 = start quest, 1 = end quest
	private boolean isPendingDisposal;

	public NpcConversationManager(ChannelClient client, int npcId, int questId,
			byte type) {
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
		SkillFactory.getSkill(skill).getEffect(level)
				.applyTo(super.client.getPlayer());
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
		super.client.write(ChannelPackets.getMapSelection(npcId, sel));
	}

	public void sendNext(String text) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"00 01", (byte) 0));
	}

	public void sendNextS(String text, byte type) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"00 01", type));
	}

	public void sendPrev(String text) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"01 00", (byte) 0));
	}

	public void sendPrevS(String text, byte type) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"01 00", type));
	}

	public void sendNextPrev(String text) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"01 01", (byte) 0));
	}

	public void sendNextPrevS(String text, byte type) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"01 01", type));
	}

	public void sendOk(String text) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"00 00", (byte) 0));
	}

	public void sendOkS(String text, byte type) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 0,
														text,
														"00 00", type));
	}

	public void sendYesNo(String text) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 2,
														text,
														"", (byte) 0));
	}

	public void sendYesNoS(String text, byte type) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 2,
														text,
														"", type));
	}

	public void askAcceptDecline(String text) {
		super.client
				.write(ChannelPackets.getNPCTalk(npcId, (byte) 0x0C,
													text,
													"", (byte) 0));
	}

	public void askAcceptDeclineNoESC(String text) {
		super.client
				.write(ChannelPackets.getNPCTalk(npcId, (byte) 0x0E,
													text,
													"", (byte) 0));
	}

	public void askAvatar(String text, int... args) {
		super.client.write(ChannelPackets
				.getNPCTalkStyle(npcId, text, args));
	}

	public void sendSimple(String text) {
		super.client.write(ChannelPackets.getNPCTalk(npcId, (byte) 5,
														text,
														"", (byte) 0));
	}

	public void sendGetNumber(String text, int def, int min, int max) {
		super.client.write(ChannelPackets.getNPCTalkNum(npcId, text, def,
														min, max));
	}

	public void sendGetText(String text) {
		super.client.write(ChannelPackets.getNPCTalkText(npcId, text));
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

	public int setAvatar(int ticket, int args) {
		if (!haveItem(ticket)) {
			return -1;
		}
		gainItem(ticket, (short) -1);

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
		player.getStorage().sendStorage(super.client, npcId);
	}

	public void openShop(int id) {
		ShopFactory.getInstance().getShop(id).sendShop(super.client);
	}

	public int gainGachaponItem(int id, int quantity) {
		final IItem item = InventoryManipulator
				.addbyId_Gachapon(super.client, id, (short) quantity);
		if (item == null) {
			return -1;
		}
		final byte rareness = GameConstants.gachaponRareItem(item.getItemId());
		if (rareness > 0) {
			try {
				super.client
						.getChannelServer()
						.getWorldInterface()
						.broadcastMessage(ChannelPackets
								.getGachaponMega(super.client
										.getPlayer()
										.getName(),
													" : Lucky winner of Gachapon! Congratulations~",
													item,
													rareness)
								.getBytes());
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
		return super.client.getPlayer().getAddQuestStatus(questId)
				.getCustomData();
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
			InventoryManipulator.unequip(super.client, id, equip
					.getNextFreeSlot());
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
			super.client.getPlayer().getMap()
					.broadcastMessage(ChannelPackets.showEffect(effect));
		} else {
			super.client.write(ChannelPackets.showEffect(effect));
		}
	}

	public void playSound(boolean broadcast, String sound) {
		if (broadcast) {
			super.client.getPlayer().getMap()
					.broadcastMessage(ChannelPackets.playSound(sound));
		} else {
			super.client.write(ChannelPackets.playSound(sound));
		}
	}

	public void environmentChange(boolean broadcast, String env) {
		if (broadcast) {
			super.client.getPlayer().getMap()
					.broadcastMessage(ChannelPackets
							.environmentChange(env, 2));
		} else {
			super.client.write(ChannelPackets.environmentChange(env, 2));
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
			final PartyMember char2member = char2.getPartyMembership();
			final PartyMember member = getPlayer().getPartyMembership();
			if (char2member != null && member != null
					&& char2member.getPartyId() == member.getPartyId()) {
				inMap++;
			}
		}
		return inMap;
	}

	public List<ChannelCharacter> getPartyMembers() {
		if (!getPlayer().hasParty()) {
			return null;
		}
		List<ChannelCharacter> chars = new LinkedList<>();
		// TODO: Not done. Finish when ChannelServer remoting is done.
		return chars;
	}

	public void warpPartyWithExp(int mapId, int exp) {
		GameMap target = getMap(mapId);
		for (PartyMember chr : getPlayer().getParty().getMembers()) {
			ChannelCharacter curChar = super.client.getChannelServer()
					.getPlayerStorage().getCharacterByName(chr.getName());
			if ((curChar.getEventInstance() == null
					&& getPlayer().getEventInstance() == null)
					|| curChar.getEventInstance() == getPlayer()
							.getEventInstance()) {
				curChar.changeMap(target, target.getPortal(0));
				curChar.gainExp(exp, true, false, true);
			}
		}
	}

	public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
		GameMap target = getMap(mapId);
		for (PartyMember chr : getPlayer().getParty().getMembers()) {
			ChannelCharacter curChar = super.client.getChannelServer()
					.getPlayerStorage().getCharacterByName(chr.getName());
			if ((curChar.getEventInstance() == null
					&& getPlayer().getEventInstance() == null)
					|| curChar.getEventInstance() == getPlayer()
							.getEventInstance()) {
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
		final ChannelCharacter player = super.client.getPlayer();

		final Squad squad = new Squad(type, player, minutes * 60 * 1000);
		final GameMap map = player.getMap();

		map.broadcastMessage(ChannelPackets.getClock(minutes * 60));
		map.broadcastMessage(ChannelPackets.serverNotice(6, player
				.getName()
				+ startText));
		super.client.getChannelServer().addMapleSquad(squad, type);
	}

	public boolean getSquadList(String type, byte type_) {
		final Squad squad = super.client.getChannelServer().getMapleSquad(type);
		if (squad == null) {
			return false;
		}
		if (type_ == 0) { // Normal viewing
			sendNext(squad.getSquadMemberString(type_));
		} else if (type_ == 1) { // Squad Leader banning, Check out banned
									// participant
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
		getPlayer().getMap().resetReactors();
	}

	public void genericGuildMessage(int code) {
		super.client.write(ChannelPackets.genericGuildMessage((byte) code));
	}

	public void disbandGuild() {
		final ChannelCharacter player = super.client.getPlayer();
		final int gid = player.getGuildId();
		if (gid <= 0 || player.getGuildRank().equals(MemberRank.MASTER)) {
			return;
		}
		try {
			super.client.getChannelServer().getWorldInterface()
					.disbandGuild(gid);
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
			// unequipEverything();
			List<StatValue> reborns = new ArrayList<>(4);
			getPlayer().setLevel(1);
			getPlayer().setExp(0);
			reborns.add(new StatValue(Stat.LEVEL, Integer.valueOf(1)));
			reborns.add(new StatValue(Stat.EXP, Integer.valueOf(0)));
			// getPlayer().super.client.write(MaplePacketCreator.updatePlayerStats(reborns));
			// getPlayer().getMap().broadcastMessage(getPlayer(),
			// MaplePacketCreator.showJobChange(getPlayer().getId()), false);
		} else {
			getPlayer()
					.getClient()
					.write(ChannelPackets
							.serverNotice(6,
											"You have reached the maximum amount of rebirths!"));
		}
	}

	public void increaseGuildCapacity() {
		final ChannelCharacter player = super.client.getPlayer();

		if (player.getMeso() < 5000000) {
			super.client.write(ChannelPackets
					.serverNotice(1, "You do not have enough mesos."));
			return;
		}
		final int gid = player.getGuildId();
		if (gid <= 0) {
			return;
		}
		try {
			super.client.getChannelServer().getWorldInterface()
					.increaseGuildCapacity(gid);
		} catch (RemoteException e) {
			System.err.println("Error while increasing capacity." + e);
			return;
		}
		player.gainMeso(-5000000, true, false, true);
	}

	public void displayGuildRanks() {
		super.client.write(ChannelPackets
				.showGuildRanks(npcId, MapleGuildRanking.getInstance()
						.getRank()));
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

	public void changeStat(byte slot, int type, short amount) {
		Equip sel = (Equip) super.client.getPlayer()
				.getEquippedItemsInventory().getItem(slot);
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
			Connection con = Database.getConnection();
			PreparedStatement ps = con
					.prepareStatement("SELECT * FROM hiredmerchants WHERE merchantid = ?");
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

			ps = con.prepareStatement("UPDATE hiredmerchants SET mesos = 0 WHERE merchantid = ?");
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
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con
				.prepareStatement("SELECT * FROM hiredmerchants WHERE merchantid = ?")) {
			ps.setInt(1, getPlayer().getId());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					mesos = rs.getLong("mesos");
				}
			}
		} catch (SQLException ex) {
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

	public final Inventory getInventory(byte type) {
		return super.client.getPlayer().getInventoryByTypeByte(type);
	}

	public final CarnivalParty getCarnivalParty() {
		return super.client.getPlayer().getCarnivalParty();
	}

	public final CarnivalChallenge getNextCarnivalRequest() {
		return super.client.getPlayer().getNextCarnivalRequest();
	}

	public void resetStats(final int str, final int dex, final int int_,
			final int luk) {
		List<StatValue> stats = new ArrayList<>(2);
		final ChannelCharacter chr = super.client.getPlayer();
		int total = chr.getStats().getStr() + chr.getStats().getDex()
				+ chr.getStats().getLuk() + chr.getStats().getInt()
				+ chr.getRemainingAp();
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
		super.client
				.write(ChannelPackets.updatePlayerStats(stats, false, chr
						.getJobId()));
	}

	public final boolean dropItem(int slot, int invType, int quantity) {
		final Inventory inventory = super.client.getPlayer()
				.getInventoryByTypeByte((byte) invType);
		if (inventory == null) {
			return false;
		}
		InventoryManipulator.drop(super.client, inventory, (short) slot,
									(short) quantity);
		return true;
	}

	public void maxStats() {
		List<StatValue> statup = new ArrayList<>(2);
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

		super.client.write(ChannelPackets.updatePlayerStats(statup, player
				.getJobId()));
	}

	public void gainFame(int fame) {
		final ChannelCharacter player = super.client.getPlayer();

		player.setFame(fame);
		player.updateSingleStat(Stat.FAME, Integer.valueOf(getPlayer()
				.getFame()));
		super.client.write(ChannelPackets
				.serverNotice(6, "You have gained (+"
						+ fame
						+ ") fame."));
	}

	public boolean isPendingDisposal() {
		return isPendingDisposal;
	}
}