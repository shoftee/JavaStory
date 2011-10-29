package javastory.channel;

import java.awt.Point;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javastory.channel.anticheat.CheatTracker;
import javastory.channel.client.ActivePlayerStats;
import javastory.channel.client.BuddyList;
import javastory.channel.client.BuddyListEntry;
import javastory.channel.client.BuffStat;
import javastory.channel.client.BuffStatValueHolder;
import javastory.channel.client.CancelCooldownAction;
import javastory.channel.client.CooldownValueHolder;
import javastory.channel.client.Disease;
import javastory.channel.client.DiseaseValueHolder;
import javastory.channel.client.ISkill;
import javastory.channel.client.KeyBinding;
import javastory.channel.client.KeyLayout;
import javastory.channel.client.MemberRank;
import javastory.channel.client.MonsterBook;
import javastory.channel.client.Mount;
import javastory.channel.client.MultiInventory;
import javastory.channel.client.Pet;
import javastory.channel.client.Skill;
import javastory.channel.client.SkillEntry;
import javastory.channel.client.SkillFactory;
import javastory.channel.client.SkillMacro;
import javastory.channel.life.MobSkill;
import javastory.channel.life.Monster;
import javastory.channel.maps.AbstractAnimatedGameMapObject;
import javastory.channel.maps.Door;
import javastory.channel.maps.Dragon;
import javastory.channel.maps.FieldLimitType;
import javastory.channel.maps.GameMap;
import javastory.channel.maps.GameMapFactory;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.maps.SavedLocationType;
import javastory.channel.maps.Summon;
import javastory.channel.movement.LifeMovementFragment;
import javastory.channel.packet.MTSCSPacket;
import javastory.channel.packet.MobPacket;
import javastory.channel.packet.MonsterCarnivalPacket;
import javastory.channel.packet.PetPacket;
import javastory.channel.server.CarnivalChallenge;
import javastory.channel.server.CarnivalParty;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.Portal;
import javastory.channel.server.Shop;
import javastory.channel.server.StatEffect;
import javastory.channel.server.Storage;
import javastory.channel.server.Trade;
import javastory.channel.shops.PlayerShop;
import javastory.client.GameCharacter;
import javastory.client.PlayerRandomStream;
import javastory.db.Database;
import javastory.db.DatabaseException;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.Gender;
import javastory.game.IEquip;
import javastory.game.IItem;
import javastory.game.Inventory;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.ItemFlag;
import javastory.game.Jobs;
import javastory.game.Skills;
import javastory.game.Stat;
import javastory.game.StatValue;
import javastory.game.data.ItemInfoProvider;
import javastory.game.data.RandomRewards;
import javastory.game.quest.QuestInfoProvider;
import javastory.game.quest.QuestInfoProvider.QuestInfo;
import javastory.game.quest.QuestStatus;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.rmi.WorldChannelInterface;
import javastory.scripting.EventInstanceManager;
import javastory.scripting.NpcScriptManager;
import javastory.server.BuffStatValue;
import javastory.server.FameLog;
import javastory.server.Notes;
import javastory.server.Notes.Note;
import javastory.server.TimerManager;
import javastory.tools.Randomizer;
import javastory.tools.packets.ChannelPackets;
import javastory.tools.packets.UIPacket;
import javastory.world.core.PartyOperation;
import javastory.world.core.PlayerCooldownValueHolder;
import javastory.world.core.PlayerDiseaseValueHolder;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ChannelCharacter extends AbstractAnimatedGameMapObject implements GameCharacter, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2519039181363990945L;

	// TODO: Global Damage caps should not be here.
	public static int damageCap = 100000000;
	public static int magicCap = 999999999;
	
	private transient int linkedMonsterId = 0;

	private transient Dragon dragon;
	
	private transient List<LifeMovementFragment> lastMovement;

	private transient Set<Monster> controlledMonsters;
	private transient Set<GameMapObject> visibleMapObjects;

	private transient Map<Integer, Summon> summons;
	
	// TODO: Create a class for these triplet.
	private transient Map<Integer, CooldownValueHolder> cooldowns;
	private transient Map<Disease, DiseaseValueHolder> diseases;
	private transient Map<BuffStat, BuffStatValueHolder> effects;

	// TODO: Find a better place for these two.
	private transient Deque<CarnivalChallenge> pendingCarnivalRequests;
	private transient CarnivalParty carnivalParty;

	private transient CheatTracker cheatTracker;

	private transient AtomicInteger conversationState;
	private transient EventInstanceManager eventInstance;
	
	// TODO: Either add an interface for the map or for the character, circular dependency bad!
	private transient GameMap map;
	
	// TODO: This can likely be generalized into a playerInteraction thingie?
	private transient Shop shop;
	
	// TODO: This can likely be generalized into a PlayerIntraction thingie?
	private transient Trade trade;

	// TODO: Move fullness schedules to Pet class.
	private transient ScheduledFuture<?> fullnessSchedule, fullnessSchedule_1, fullnessSchedule_2, hpDecreaseTask;

	// TODO: Surely these should be buff tasks?
	private transient ScheduledFuture<?> beholderHealingSchedule, beholderBuffSchedule, BerserkSchedule;
	private transient ScheduledFuture<?> dragonBloodSchedule;
	private transient ScheduledFuture<?> mapTimeLimitTask, fishing;

	// TODO: Use skill checking instead?
	private boolean canDoor, berserk, smega, hidden;

	// TODO: Are these really necessary?
	private boolean ondmg = true, callgm = true;

	// Make this a quest record, 
	// TODO : Transfer it somehow with the current data
	private byte dojoRecord;

	//
	private int id;
	private Gender gender;
	private byte gmLevel;
	private int hairId, faceId, skinColorId;
	private int level, jobId, fame;

	// TODO: WorldId is obsolete here.
	public int worldId;

	private short mulung_energy, combo, availableCP, totalCP;

	// TODO: Move into Account class.
	public int vpoints;
	public int accountId;
	public int maplePoints;
	public int aCash;

	// TODO: Create classes for these arrays.
	private int[] wishlist, teleportRocks;
	
	private EnumMap<SavedLocationType, Integer> savedLocations;
	
	public int reborns;

	// TODO: Subcategory should be in the Job class.
	private int subcategory;

	// TODO: Extract HP AP Used, MP AP Used, remaining AP into another class.
	private int mpApUsed, hpApUsed, remainingAp;
	
	private int meso, exp, mapId, initialSpawnPoint, bookCover, dojo, fallcounter, chair, itemEffect;
	
	// TODO: Extract SP logic into class.
	private int[] remainingSp = new int[10];

	private long lastCombo, lastFameTime, keydown_skill;
	private String name, chalktext, BlessOfFairy_Origin;

	//
	private Map<Integer, QuestStatus> quests;
	private Map<Integer, String> questInfo;

	private final Map<ISkill, SkillEntry> skills;

	// TODO: Surely this shouldn't be here?
	private final List<Door> doors;

	private final List<Pet> pets;

	// TODO: Extract SkillMacro list into a class. 
	private SkillMacro[] skillMacros = new SkillMacro[5];

	// TODO: This stuff should be in GuildMember
	private int guildId;
	private MemberRank guildRank;

	private GuildMember guildMember;

	//
	private BuddyList buddies;

	private MonsterBook monsterBook;

	private ActivePlayerStats stats;

	private Storage storage;

	private Mount mount;

	// TODO: MessengerMember instead.
	private Messenger messenger;
	private int messengerPosition;

	// TODO: Probably not the best place.
	private PlayerShop playerShop;

	// TODO: Remove Party uses from this class completely.
	private PartyMember partyMember;
	private Party party;

	private KeyLayout keylayout;
	
	private MultiInventory inventory;

	private ChannelClient client;

	private PlayerRandomStream randomStream;

	private ChannelCharacter() {
		setStance(0);
		setPosition(new Point(0, 0));
		for (int i = 0; i < remainingSp.length; i++) {
			remainingSp[i] = 0;
		}

		lastCombo = 0;
		mulung_energy = 0;
		combo = 0;
		keydown_skill = 0;
		messengerPosition = 4;
		canDoor = true;
		berserk = false;
		smega = true;
		wishlist = new int[10];
		teleportRocks = new int[10];

		// 1 = NPC/Quest,
		// 2 = Duey,
		// 3 = Hired Merch store,
		// 4 = Storage
		conversationState = new AtomicInteger();

		keylayout = new KeyLayout();
		inventory = new MultiInventory();
		stats = new ActivePlayerStats(this);
		cheatTracker = new CheatTracker(this);

		cooldowns = Maps.newLinkedHashMap();
		effects = Maps.newEnumMap(BuffStat.class);
		diseases = Maps.newEnumMap(Disease.class);

		doors = Lists.newArrayList();
		
		pets = Lists.newArrayList();
		
		pendingCarnivalRequests = Lists.newLinkedList();
		
		controlledMonsters = Sets.newLinkedHashSet();
		visibleMapObjects = Sets.newLinkedHashSet();
		
		quests = Maps.newLinkedHashMap();
		questInfo = Maps.newLinkedHashMap();
		
		summons = Maps.newLinkedHashMap();
		
		skills = Maps.newLinkedHashMap();
		
		// TODO: Get rid of arrays.
		savedLocations = Maps.newEnumMap(SavedLocationType.class);
	}

	public static ChannelCharacter reconstructCharacter(final CharacterTransfer ct, final ChannelClient client) {
		final ChannelCharacter ret = new ChannelCharacter();
		ret.client = client;
		ret.id = ct.CharacterId;
		ret.name = ct.CharacterName;
		ret.level = ct.Level;
		ret.fame = ct.Fame;

		ret.randomStream = new PlayerRandomStream();

		ret.stats.setStr(ct.STR);
		ret.stats.setDex(ct.DEX);
		ret.stats.setInt(ct.INT);
		ret.stats.setLuk(ct.LUK);
		ret.stats.setMaxHp(ct.MaxHP);
		ret.stats.setMaxMp(ct.MaxMP);
		ret.stats.setHp(ct.HP);
		ret.stats.setMp(ct.MP);

		ret.exp = ct.Exp;
		ret.hpApUsed = ct.hpApUsed;
		ret.mpApUsed = ct.mpApUsed;
		ret.remainingSp = ct.RemainingSP;
		ret.remainingAp = ct.RemainingAP;
		ret.meso = ct.Meso;
		ret.gmLevel = ct.GmLevel;
		ret.skinColorId = ct.SkinColorId;
		ret.gender = ct.Gender;
		ret.jobId = ct.JobId;
		ret.hairId = ct.HairId;
		ret.faceId = ct.FaceId;
		ret.accountId = ct.AccountId;
		ret.mapId = ct.MapId;
		ret.initialSpawnPoint = ct.InitialSpawnPoint;
		ret.worldId = ct.WorldId;

		ret.bookCover = ct.MonsterBookCover;
		ret.dojo = ct.Dojo;
		ret.dojoRecord = ct.DojoRecord;
		ret.reborns = ct.RebornCount;

		ret.guildId = ct.GuildId;
		ret.guildRank = ct.GuildRank;

		ret.subcategory = ct.Subcategory;
		ret.ondmg = ct.ondmg;
		ret.callgm = ct.callgm;
		if (ret.guildId > 0) {
			ret.guildMember = new GuildMember(ret);
		}
		ret.buddies = new BuddyList(ct.BuddyListCapacity);

		final GameMapFactory mapFactory = ChannelServer.getMapFactory();
		ret.map = mapFactory.getMap(ret.mapId);
		if (ret.map == null) {
			// char is on a map that doesn't exist warp it to henesys
			ret.map = mapFactory.getMap(100000000);
		} else {
			if (ret.map.getForcedReturnId() != 999999999) {
				ret.map = ret.map.getForcedReturnMap();
			}
		}
		Portal portal = ret.map.getPortal(ret.initialSpawnPoint);
		if (portal == null) {
			portal = ret.map.getPortal(0); // char is on a spawnpoint that
											// doesn't exist - select the first
											// spawnpoint instead
			ret.initialSpawnPoint = 0;
		}
		ret.setPosition(portal.getPosition());

		final int partyId = ct.PartyId;
		if (partyId >= 0) {
			try {
				final Party party = ChannelServer.getWorldInterface().getParty(partyId);
				if (party != null && party.getMemberById(ret.id) != null) {
					ret.party = party;
				}
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}

		final int messengerid = ct.MessengerId;
		final int position = ct.MessengerPosition;
		if (messengerid > 0 && position < 4 && position > -1) {
			try {
				final WorldChannelInterface wci = ChannelServer.getWorldInterface();
				final Messenger messenger = wci.getMessenger(messengerid);
				if (messenger != null) {
					ret.messenger = messenger;
					ret.messengerPosition = position;
				}
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}

		ret.quests = ct.Quests;
		// TODO: transfer of active quest information

		for (final Map.Entry<Integer, SkillEntry> qs : ct.Skills.entrySet()) {
			ret.skills.put(SkillFactory.getSkill(qs.getKey()), qs.getValue());
		}
		ret.monsterBook = ct.MonsterBook;
		ret.inventory = ct.Inventories;
		ret.BlessOfFairy_Origin = ct.BlessOfFairy;
		ret.skillMacros = ct.SkillMacros;
		ret.keylayout = ct.KeyLayout;
		ret.questInfo = ct.QuestInfoEntries;
		ret.savedLocations = ct.SavedLocations;
		ret.wishlist = ct.Wishlist;
		ret.teleportRocks = ct.TeleportRocks;
		ret.buddies.loadFromTransfer(ct.BuddyListEntries);
		ret.keydown_skill = 0; // Keydown skill can't be brought over
		ret.lastFameTime = ct.LastFameTime;
		ret.storage = ct.Storage;
		client.setAccountName(ct.AccountName);
		ret.aCash = ct.ACash;
		ret.vpoints = ct.vpoints;
		ret.maplePoints = ct.MaplePoints;

		ret.mount = new Mount(ret, ct.MountItemId, 1004, ct.MountFatigue, ct.MountLevel, ct.MountExp);

		ret.stats.recalcLocalStats();
		ret.silentEnforceMaxHpMp();

		return ret;
	}

	public static ChannelCharacter loadFromDb(final int characterId, final ChannelClient client) {
		final ChannelCharacter ret = new ChannelCharacter();
		ret.client = client;
		ret.accountId = client.getAccountId();
		ret.id = characterId;

		final Connection con = Database.getConnection();

		try {
			ret.loadCharacterData(characterId, con);

			ret.quests = loadQuestData(con, characterId);

			ret.randomStream = new PlayerRandomStream();

			ret.monsterBook = MonsterBook.loadFromDb(characterId);

			ret.loadInventoryCapacity(characterId, con);

			ret.loadInventoryItems(characterId, con);

			ret.loadAccountData(con);

			ret.loadQuestData(characterId, con);

			ret.loadSkillInfo(characterId, con);

			ret.loadBlessOfFairy(characterId, con);

			ret.loadSkillMacros(characterId, con);

			ret.keylayout = KeyLayout.loadFromDb(characterId);

			ret.loadSavedLocations(characterId, con);

			ret.lastFameTime = FameLog.getLastTimestamp(characterId);

			ret.buddies.loadFromDb(characterId);
			ret.storage = Storage.loadStorage(ret.accountId);

			ret.loadWishlist(characterId, con);

			ret.loadTeleportRockLocations(characterId, con);

			ret.loadMountData(characterId, con);
		} catch (final SQLException ex) {
			System.out.println("Failed to load character: " + ex);
			return null;
		}

		ret.stats.recalcLocalStats();
		ret.silentEnforceMaxHpMp();

		return ret;
	}

	private void loadMountData(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectMountData(characterId, con);
				final ResultSet rs = ps.executeQuery()) {
			if (!rs.next()) {
				throw new RuntimeException("No mount data found on SQL column");
			}
			final IItem mount = this.getEquippedItemsInventory().getItem((byte) -22);
			this.mount = new Mount(this, mount != null ? mount.getItemId() : 0, 1004, rs.getInt("Fatigue"), rs.getInt("Level"), rs.getInt("Exp"));
		}
	}

	private void loadTeleportRockLocations(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectTeleportRockLocations(characterId, con);
				final ResultSet rs = ps.executeQuery()) {
			int r = 0;
			while (rs.next()) {
				this.teleportRocks[r] = rs.getInt("mapid");
				r++;
			}
			while (r < 10) {
				this.teleportRocks[r] = 999999999;
				r++;
			}
		}
	}

	private void loadWishlist(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectWishlistItems(characterId, con);
				final ResultSet rs = ps.executeQuery()) {
			int i = 0;
			while (rs.next()) {
				this.wishlist[i] = rs.getInt("sn");
				i++;
			}
			while (i < 10) {
				this.wishlist[i] = 0;
				i++;
			}
		}
	}

	private void loadSavedLocations(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectSavedLocations(characterId, con);
				final ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				final int locationType = rs.getInt("locationtype");
				final int savedMapId = rs.getInt("map");
				this.savedLocations.put(SavedLocationType.fromNumber(locationType), savedMapId);
			}
		}
	}

	private void loadSkillMacros(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectSkillMacros(characterId, con);
				final ResultSet rs = ps.executeQuery()) {
			int position;
			while (rs.next()) {
				position = rs.getInt("position");
				final int skillId1 = rs.getInt("skill1");
				final int skillId2 = rs.getInt("skill2");
				final int skillId3 = rs.getInt("skill3");
				final int shout = rs.getInt("shout");
				final SkillMacro macro = new SkillMacro(skillId1, skillId2, skillId3, rs.getString("name"), shout, position);
				this.skillMacros[position] = macro;
			}
		}
	}

	private void loadBlessOfFairy(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectCharactersByLevelDesc(this.accountId, con);
				final ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				if (rs.getInt("id") != characterId) { // Not this character
					byte maxlevel = (byte) (rs.getInt("level") / 10);

					// if (!GameConstants.isKOC(ret.job)) {
					if (maxlevel > 20) {
						maxlevel = 20;
					}
					// }
					this.BlessOfFairy_Origin = rs.getString("name");
					final SkillEntry skillEntry = new SkillEntry(maxlevel, (byte) 0);
					final int skillId = Skills.getBlessOfFairyForJob(this.jobId);
					this.skills.put(SkillFactory.getSkill(skillId), skillEntry);
					break;
				}
			}
		}
	}

	private void loadSkillInfo(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectSkillInfo(characterId, con);
				final ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				final int skillId = rs.getInt("skillid");
				if (GameConstants.isApplicableSkill(skillId)) {
					final byte masterLevel = rs.getByte("masterlevel");
					final byte skillLevel = rs.getByte("skilllevel");
					final SkillEntry skillEntry = new SkillEntry(skillLevel, masterLevel);
					this.skills.put(SkillFactory.getSkill(skillId), skillEntry);
				}
			}
		}
	}

	private void loadQuestData(final int characterId, final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectQuestInfo(characterId, con);
				final ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				final int questId = rs.getInt("quest");
				final String customData = rs.getString("customData");
				this.questInfo.put(questId, customData);
			}
		}
	}

	private void loadAccountData(final Connection con) throws SQLException {
		try (	final PreparedStatement ps = getSelectAccount(this.accountId, con);
				final ResultSet rs = ps.executeQuery();) {
			if (rs.next()) {
				this.getClient().setAccountName(rs.getString("name"));
				this.aCash = rs.getInt("ACash");
				this.vpoints = rs.getInt("vpoints");
				this.maplePoints = rs.getInt("mPoints");
			}
		}
	}

	private void loadInventoryItems(final int characterId, final Connection con) throws SQLException {
		try (	PreparedStatement ps = getSelectEquips(characterId, con);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				final byte inventoryType = rs.getByte("inventorytype");
				final InventoryType type = InventoryType.fromNumber(inventoryType);

				final int itemId = rs.getInt("itemid");
				final byte position = rs.getByte("position");
				final short quantity = rs.getShort("quantity");
				final byte flag = rs.getByte("flag");
				final long expiration = rs.getLong("expiredate");
				final String owner = rs.getString("owner");
				final String gmLog = rs.getString("GM_Log");

				if (type.equals(InventoryType.EQUIP) || type.equals(InventoryType.EQUIPPED)) {
					final Equip equip = new Equip(itemId, position, rs.getInt("ringid"), flag);
					equip.setOwner(owner);
					equip.setQuantity(quantity);
					equip.setExpiration(expiration);
					equip.setGMLog(gmLog);

					equip.setAcc(rs.getShort("acc"));
					equip.setAvoid(rs.getShort("avoid"));
					equip.setDex(rs.getShort("dex"));
					equip.setHands(rs.getShort("hands"));
					equip.setHp(rs.getShort("hp"));
					equip.setInt(rs.getShort("int"));
					equip.setJump(rs.getShort("jump"));
					equip.setLuk(rs.getShort("luk"));
					equip.setMatk(rs.getShort("matk"));
					equip.setMdef(rs.getShort("mdef"));
					equip.setMp(rs.getShort("mp"));
					equip.setSpeed(rs.getShort("speed"));
					equip.setStr(rs.getShort("str"));
					equip.setWatk(rs.getShort("watk"));
					equip.setWdef(rs.getShort("wdef"));
					equip.setItemLevel(rs.getByte("itemLevel"));
					equip.setItemEXP(rs.getShort("itemEXP"));
					equip.setViciousHammer(rs.getByte("ViciousHammer"));
					equip.setUpgradeSlots(rs.getByte("upgradeslots"));
					equip.setLevel(rs.getByte("level"));

					this.inventory.get(type).addFromDb(equip);
				} else {
					final Item item = new Item(itemId, position, quantity, flag);
					item.setOwner(owner);
					item.setExpiration(expiration);
					item.setGMLog(gmLog);

					this.inventory.get(type).addFromDb(item);

					if (rs.getInt("petid") > -1) {
						final int petId = rs.getInt("petid");
						final Pet pet = Pet.loadFromDb(item.getItemId(), petId, item.getPosition());
						this.pets.add(pet);
						item.setPet(pet);
					}
				}
			}
		}
	}

	private void loadInventoryCapacity(final int characterId, final Connection con) throws SQLException {
		try (	PreparedStatement ps = getSelectInventorySlot(characterId, con);
				ResultSet rs = ps.executeQuery()) {
			if (!rs.next()) {
				throw new RuntimeException("No Inventory slot column found in SQL. [inventoryslot]");
			}

			final byte equipSlots = rs.getByte("equip");
			this.getEquipInventory().setSlotLimit(equipSlots);

			final byte useSlots = rs.getByte("use");
			this.getUseInventory().setSlotLimit(useSlots);

			final byte setupSlots = rs.getByte("setup");
			this.getSetupInventory().setSlotLimit(setupSlots);

			final byte etcSlots = rs.getByte("etc");
			this.getEtcInventory().setSlotLimit(etcSlots);

			final byte cashSlots = rs.getByte("cash");
			this.getCashInventory().setSlotLimit(cashSlots);
		}
	}

	private static PreparedStatement getSelectMountData(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectTeleportRockLocations(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectWishlistItems(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectSavedLocations(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectSkillMacros(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectCharactersByLevelDesc(final int accountId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
		ps.setInt(1, accountId);
		return ps;
	}

	private static PreparedStatement getSelectSkillInfo(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel FROM skills WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectQuestInfo(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectAccount(final int accountId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
		ps.setInt(1, accountId);
		return ps;
	}

	private static PreparedStatement getSelectEquips(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con
			.prepareStatement("SELECT * FROM inventoryitems LEFT JOIN inventoryequipment USING (inventoryitemid) WHERE characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectInventorySlot(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM inventoryslot where characterid = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private static PreparedStatement getSelectCharacter(final int characterId, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
		ps.setInt(1, characterId);
		return ps;
	}

	private void loadCharacterData(final int characterId, final Connection con) throws SQLException {
		try (	PreparedStatement ps = getSelectCharacter(characterId, con);
				ResultSet rs = ps.executeQuery()) {
			if (!rs.next()) {
				throw new RuntimeException("Loading the Char Failed (char not found)");
			}
			this.name = rs.getString("name");
			this.level = rs.getShort("level");
			this.fame = rs.getInt("fame");

			this.stats = new ActivePlayerStats(this);

			this.stats.setStr(rs.getInt("str"));
			this.stats.setDex(rs.getInt("dex"));
			this.stats.setInt(rs.getInt("int"));
			this.stats.setLuk(rs.getInt("luk"));
			this.stats.setMaxHp(rs.getInt("maxhp"));
			this.stats.setMaxMp(rs.getInt("maxmp"));
			this.stats.setHp(rs.getInt("hp"));
			this.stats.setMp(rs.getInt("mp"));

			this.exp = rs.getInt("exp");
			this.hpApUsed = rs.getInt("hpApUsed");
			this.mpApUsed = rs.getInt("mpApUsed");
			final String[] sp = rs.getString("sp").split(",");

			for (int i = 0; i < this.remainingSp.length; i++) {
				this.remainingSp[i] = Integer.parseInt(sp[i]);
			}
			this.remainingAp = rs.getInt("ap");
			this.subcategory = rs.getInt("subcategory");
			this.meso = rs.getInt("meso");
			this.gmLevel = rs.getByte("gm");
			this.skinColorId = rs.getInt("skincolor");
			this.gender = Gender.fromNumber(rs.getByte("gender"));
			this.jobId = rs.getInt("job");
			this.hairId = rs.getInt("hair");
			this.faceId = rs.getInt("face");
			this.accountId = rs.getInt("accountid");
			this.mapId = rs.getInt("map");
			this.initialSpawnPoint = rs.getInt("spawnpoint");
			this.worldId = rs.getInt("world");
			this.guildId = rs.getInt("guildid");
			this.guildRank = MemberRank.fromNumber(rs.getInt("guildrank"));
			this.reborns = rs.getInt("reborns");
			if (this.guildId > 0) {
				this.guildMember = new GuildMember(this);
			}
			this.buddies = new BuddyList(rs.getInt("buddyCapacity"));

			final GameMapFactory mapFactory = ChannelServer.getMapFactory();
			this.map = mapFactory.getMap(this.mapId);
			if (this.map == null) {
				// char is on a map that doesn't exist warp it to henesys
				this.map = mapFactory.getMap(100000000);
			}
			Portal portal = this.map.getPortal(this.initialSpawnPoint);
			if (portal == null) {
				// char is on a spawnpoint that doesn't exist - select the first
				// spawnpoint instead
				portal = this.map.getPortal(0);
				this.initialSpawnPoint = 0;
			}
			this.setPosition(portal.getPosition());

			final int partyid = rs.getInt("party");
			if (partyid >= 0) {
				try {
					final Party party = ChannelServer.getWorldInterface().getParty(partyid);
					if (party != null && party.getMemberById(this.id) != null) {
						this.party = party;
					}
				} catch (final RemoteException e) {
					ChannelServer.pingWorld();
				}
			}

			final int messengerId = rs.getInt("messengerid");
			final int messengerPosition = rs.getInt("messengerposition");
			if (messengerId > 0 && messengerPosition < 4 && messengerPosition > -1) {
				try {
					final WorldChannelInterface wci = ChannelServer.getWorldInterface();
					final Messenger messenger = wci.getMessenger(messengerId);
					if (messenger != null) {
						this.messenger = messenger;
						this.messengerPosition = messengerPosition;
					}
				} catch (final RemoteException e) {
					ChannelServer.pingWorld();
				}
			}
			this.bookCover = rs.getInt("monsterbookcover");
			this.dojo = rs.getInt("dojo_pts");
			this.dojoRecord = rs.getByte("dojoRecord");
		}
	}

	private static Map<Integer, QuestStatus> loadQuestData(final Connection c, final int characterId) throws SQLException {
		try (	final PreparedStatement statusQuery = getSelectQuestStatus(c, characterId);
				final ResultSet statusResultSet = statusQuery.executeQuery()) {
			return loadQuestMobStatus(c, statusResultSet);
		}
	}

	private static Map<Integer, QuestStatus> loadQuestMobStatus(final Connection c, final ResultSet statusResultSet) throws SQLException {
		final Map<Integer, QuestStatus> quests = Maps.newHashMap();
		try (final PreparedStatement mobsQuery = getSelectQuestMobStatus(c)) {
			while (statusResultSet.next()) {
				final QuestStatus status = new QuestStatus(statusResultSet);

				final int questId = status.getQuestId();
				quests.put(questId, status);

				final int questStatusId = statusResultSet.getInt("queststatusid");
				mobsQuery.setInt(1, questStatusId);
				try (ResultSet mobResultSet = mobsQuery.executeQuery()) {
					while (mobResultSet.next()) {
						final int mobId = mobResultSet.getInt("mob");
						final int killCount = mobResultSet.getInt("count");
						status.setMobKills(mobId, killCount);
					}
				}
			}
			return quests;
		}
	}

	private static PreparedStatement getSelectQuestMobStatus(final Connection c) throws SQLException {
		return c.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
	}

	private static PreparedStatement getSelectQuestStatus(final Connection c, final int characterId) throws SQLException {
		final PreparedStatement statusQuery = c.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
		statusQuery.setInt(1, characterId);
		return statusQuery;
	}

	public void saveToDb(final boolean forcedDc) {
		final Connection con = Database.getConnection();

		try {
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);

			{
				saveCharacterData(con);
			}

			for (final Pet pet : pets) {
				if (pet.isSummoned()) {
					// Only save those summoned :P
					pet.saveToDb();
				}
			}

			deleteByCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
			saveSkillMacros(con);

			deleteByCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
			saveInventorySlots(con);

			deleteByCharacterId(con, "DELETE FROM inventoryitems WHERE characterid = ?");
			saveInventoryItems(con);

			deleteByCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
			saveQuestInfo(con);

			deleteByCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
			saveQuestStatus(con);

			deleteByCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
			saveSkillInfo(con);

			if (forcedDc && getAllCooldowns().size() > 0) {
				saveSkillCooldowns(con);
			}

			deleteByCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
			saveSavedLocations(con);

			deleteByCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
			saveBuddyEntries(con);

			saveAccountInfo(con);

			if (storage != null) {
				storage.saveToDB();
			}

			keylayout.saveKeys(id);
			mount.saveMount(id);
			monsterBook.saveCards(id);

			deleteByCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
			saveWishlist(con);

			deleteByCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
			saveTeleportRockLocations(con);

			con.commit();
		} catch (SQLException | DatabaseException e) {
			final String failMessage = ChannelClient.getLogMessage(this, "[charsave] Error saving character data");
			System.err.println(failMessage + e);
			try {
				con.rollback();
			} catch (final SQLException ex) {
				final String completelyFailMessage = ChannelClient.getLogMessage(this, "[charsave] Error Rolling Back");
				System.err.println(completelyFailMessage + e);
			}
		} finally {
			try {
				con.setAutoCommit(true);
				con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			} catch (final SQLException e) {
				System.err.println(ChannelClient.getLogMessage(this, "[charsave] Error going back to autocommit mode") + e);
			}
		}
	}

	private void saveCharacterData(final Connection con) throws SQLException {
		try (final PreparedStatement ps = getUpdateCharacterData(con)) {
			ps.setInt(1, level);
			ps.setInt(2, fame);
			ps.setInt(3, stats.getStr());
			ps.setInt(4, stats.getDex());
			ps.setInt(5, stats.getLuk());
			ps.setInt(6, stats.getInt());
			ps.setInt(7, exp);
			ps.setInt(8, stats.getHp() < 1 ? 50 : stats.getHp());
			ps.setInt(9, stats.getMp());
			ps.setInt(10, stats.getMaxHp());
			ps.setInt(11, stats.getMaxMp());

			final String joinedSp = Joiner.on(',').join(Lists.newArrayList(remainingSp));
			ps.setString(12, joinedSp);

			ps.setInt(13, remainingAp);
			ps.setInt(14, gmLevel);
			ps.setInt(15, skinColorId);
			ps.setInt(16, gender.asNumber());
			ps.setInt(17, jobId);
			ps.setInt(18, hairId);
			ps.setInt(19, faceId);

			if (map.getForcedReturnId() != 999999999) {
				ps.setInt(20, map.getForcedReturnId());
			} else {
				ps.setInt(20, stats.getHp() < 1 ? map.getReturnMapId() : map.getId());
			}

			ps.setInt(21, meso);
			ps.setInt(22, hpApUsed);
			ps.setInt(23, mpApUsed);

			if (map == null) {
				ps.setInt(24, 0);
			} else {
				final Portal closest = map.findClosestSpawnpoint(getPosition());
				ps.setInt(24, closest != null ? closest.getId() : 0);
			}

			ps.setInt(25, party != null ? party.getId() : -1);
			ps.setInt(26, buddies.getCapacity());

			if (messenger != null) {
				ps.setInt(27, messenger.getId());
				ps.setInt(28, messengerPosition);
			} else {
				ps.setInt(27, 0);
				ps.setInt(28, 4);
			}

			ps.setInt(29, bookCover);
			ps.setInt(30, dojo);
			ps.setInt(31, dojoRecord);
			ps.setInt(32, getReborns());
			ps.setInt(33, subcategory);
			ps.setInt(34, id);

			if (ps.executeUpdate() < 1) {
				throw new DatabaseException("Character not in database (" + id + ")");
			}
		}
	}

	private PreparedStatement getUpdateCharacterData(final Connection con) throws SQLException {
		final PreparedStatement ps = con
			.prepareStatement(
				"UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, monsterbookcover = ?, dojo_pts = ?, dojoRecord = ?, reborns = ?, subcategory = ? WHERE id = ?",
				Statement.RETURN_GENERATED_KEYS);
		return ps;
	}

	private void saveTeleportRockLocations(final Connection con) throws SQLException {
		try (final PreparedStatement ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ")) {
			for (int i = 0; i < getRockSize(); i++) {
				if (teleportRocks[i] != 999999999) {
					ps.setInt(1, getId());
					ps.setInt(2, teleportRocks[i]);
					ps.execute();
				}
			}
		}
	}

	private void saveWishlist(final Connection con) throws SQLException {
		try (final PreparedStatement ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ")) {
			for (int i = 0; i < getWishlistSize(); i++) {
				ps.setInt(1, getId());
				ps.setInt(2, wishlist[i]);
				ps.execute();
			}
		}
	}

	private void saveAccountInfo(final Connection con) throws SQLException {
		try (final PreparedStatement ps = getUpdateAccount(con)) {
			ps.setInt(1, aCash);
			ps.setInt(2, maplePoints);
			ps.setInt(3, vpoints);
			ps.setInt(4, client.getAccountId());
			ps.execute();
		}
	}

	private PreparedStatement getUpdateAccount(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("UPDATE accounts SET `ACash` = ?, `mPoints` = ?, `vpoints` = ? WHERE id = ?");
		return ps;
	}

	private void saveBuddyEntries(final Connection con) throws SQLException {
		try (final PreparedStatement ps = getInsertBuddyEntry(con)) {
			for (final BuddyListEntry entry : buddies.getBuddies()) {
				if (entry.isVisible()) {
					ps.setInt(2, entry.getCharacterId());
					ps.execute();
				}
			}
		}
	}

	private PreparedStatement getInsertBuddyEntry(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 0)");
		ps.setInt(1, id);
		return ps;
	}

	private void saveSavedLocations(final Connection con) throws SQLException {
		try (final PreparedStatement ps = getInsertSavedLocation(con)) {
			for (final Map.Entry<SavedLocationType, Integer> entry : this.savedLocations.entrySet()) {
				ps.setInt(2, entry.getKey().asNumber());
				ps.setInt(3, entry.getValue());
				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertSavedLocation(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
		ps.setInt(1, id);
		return ps;
	}

	private void saveSkillCooldowns(final Connection con) throws SQLException {
		try (final PreparedStatement ps = getInsertSkillCooldown(con)) {
			for (final PlayerCooldownValueHolder cooling : getAllCooldowns()) {
				ps.setInt(2, cooling.skillId);
				ps.setLong(3, cooling.startTime);
				ps.setLong(4, cooling.length);
				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertSkillCooldown(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveSkillInfo(final Connection con) throws SQLException {
		try (final PreparedStatement ps = getInsertSkillInfo(con)) {
			for (final Entry<ISkill, SkillEntry> skill : skills.entrySet()) {

				final int skillId = skill.getKey().getId();
				ps.setInt(2, skillId);

				final SkillEntry entry = skill.getValue();
				ps.setInt(3, entry.getCurrentLevel());
				ps.setInt(4, entry.getMasterLevel());

				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertSkillInfo(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES (?, ?, ?, ?)");
		ps.setInt(1, id);
		return ps;
	}

	private void saveQuestStatus(final Connection con) throws SQLException {
		try (	final PreparedStatement questStatement = getInsertQuestStatus(con);
				final PreparedStatement mobStatement = getInsertQuestMobStatus(con)) {
			questStatement.setInt(1, id);
			for (final QuestStatus quest : quests.values()) {
				setQuestStatusData(questStatement, quest);
				questStatement.executeUpdate();

				if (quest.hasMobKills()) {
					try (final ResultSet rs = questStatement.getGeneratedKeys()) {
						if (!rs.next()) {
							continue;
						}

						for (final int mob : quest.getMobKills().keySet()) {
							mobStatement.setInt(1, rs.getInt(1));
							mobStatement.setInt(2, mob);
							mobStatement.setInt(3, quest.getMobKills(mob));
							mobStatement.executeUpdate();
						}
					}
				}
			}
		}
	}

	private void setQuestStatusData(final PreparedStatement statement, final QuestStatus quest) throws SQLException {
		statement.setInt(2, quest.getQuestId());
		statement.setInt(3, quest.getState());
		statement.setInt(4, (int) (quest.getCompletionTime() / 1000));
		statement.setInt(5, quest.getForfeited());
		statement.setString(6, quest.getCustomData());
	}

	private PreparedStatement getInsertQuestMobStatus(final Connection con) throws SQLException {
		return con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
	}

	private PreparedStatement getInsertQuestStatus(final Connection con) throws SQLException {
		return con
			.prepareStatement(
				"INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
	}

	private void saveQuestInfo(final Connection con) throws SQLException {
		try (final PreparedStatement ps = getInsertQuestInfo(con)) {
			for (final Entry<Integer, String> q : questInfo.entrySet()) {
				ps.setInt(2, q.getKey());
				ps.setString(3, q.getValue());
				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertQuestInfo(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `data`) VALUES (?, ?, ?)");
		ps.setInt(1, id);
		return ps;
	}

	private void saveInventoryItems(final Connection con) throws SQLException {
		try (final PreparedStatement items = getInsertInventoryItem(con)) {

			saveItemInventory(items, getUseInventory());
			saveItemInventory(items, getSetupInventory());
			saveItemInventory(items, getEtcInventory());
			saveItemInventory(items, getCashInventory());

			try (final PreparedStatement equips = getInsertInventoryEquip(con)) {
				saveEquipInventory(items, equips, getEquipInventory());
				saveEquipInventory(items, equips, getEquippedItemsInventory());
			}
		}
	}

	private void saveItemInventory(PreparedStatement statement, Inventory inventory) throws SQLException {
		statement.setInt(1, id);
		statement.setInt(2, inventory.getType().asNumber());
		for (final IItem item : inventory) {
			setItemData(statement, item);
			statement.executeUpdate();
		}
	}

	private void setItemData(PreparedStatement statement, final IItem item) throws SQLException {
		statement.setInt(3, item.getItemId());
		statement.setInt(4, item.getPosition());
		statement.setInt(5, item.getQuantity());
		statement.setString(6, item.getOwner());
		statement.setString(7, item.getGMLog());
		statement.setInt(8, item.getPet() != null ? item.getPet().getUniqueId() : -1);
		statement.setLong(9, item.getExpiration());
		statement.setByte(10, item.getFlag());
	}

	private void saveEquipInventory(final PreparedStatement itemStatement, final PreparedStatement equipStatement, final Inventory inventory)
		throws SQLException {
		itemStatement.setInt(3, inventory.getType().asNumber());
		for (final IItem item : inventory) {
			setItemData(itemStatement, item);
			itemStatement.executeUpdate();

			final ResultSet rs = itemStatement.getGeneratedKeys();
			int itemid;
			if (rs.next()) {
				itemid = rs.getInt(1);
			} else {
				throw new DatabaseException("Inserting char failed.");
			}

			if (inventory.getType().equals(InventoryType.EQUIP) || inventory.getType().equals(InventoryType.EQUIPPED)) {
				equipStatement.setInt(1, itemid);
				setEquipData(equipStatement, item);
				equipStatement.executeUpdate();
			}
		}
	}

	private void setEquipData(final PreparedStatement statement, final IItem item) throws SQLException {
		final IEquip equip = (IEquip) item;
		statement.setInt(2, equip.getUpgradeSlots());
		statement.setInt(3, equip.getLevel());
		statement.setInt(4, equip.getStr());
		statement.setInt(5, equip.getDex());
		statement.setInt(6, equip.getInt());
		statement.setInt(7, equip.getLuk());
		statement.setInt(8, equip.getHp());
		statement.setInt(9, equip.getMp());
		statement.setInt(10, equip.getWatk());
		statement.setInt(11, equip.getMatk());
		statement.setInt(12, equip.getWdef());
		statement.setInt(13, equip.getMdef());
		statement.setInt(14, equip.getAcc());
		statement.setInt(15, equip.getAvoid());
		statement.setInt(16, equip.getHands());
		statement.setInt(17, equip.getSpeed());
		statement.setInt(18, equip.getJump());
		statement.setInt(19, equip.getRingId());
		statement.setInt(20, equip.getViciousHammer());
		statement.setInt(21, equip.getItemLevel());
		statement.setInt(22, equip.getItemEXP());
	}

	private PreparedStatement getInsertInventoryEquip(final Connection con) throws SQLException {
		return con.prepareStatement("INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
	}

	private PreparedStatement getInsertInventoryItem(final Connection con) throws SQLException {
		return con
			.prepareStatement(
				"INSERT INTO inventoryitems (characterid, inventorytype, itemid, position, quantity, owner, GM_Log, petid, expiredate, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);
	}

	private void saveInventorySlots(final Connection con) throws SQLException {
		try (PreparedStatement ps = getInsertInventorySlots(con)) {
			ps.execute();
		}
	}

	private PreparedStatement getInsertInventorySlots(final Connection con) throws SQLException {
		final PreparedStatement ps = con
			.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
		ps.setInt(1, id);
		ps.setInt(2, getEquipInventory().getSlotLimit());
		ps.setInt(3, getUseInventory().getSlotLimit());
		ps.setInt(4, getSetupInventory().getSlotLimit());
		ps.setInt(5, getEtcInventory().getSlotLimit());
		ps.setInt(6, getCashInventory().getSlotLimit());
		return ps;
	}

	private void saveSkillMacros(final Connection con) throws SQLException {
		try (PreparedStatement ps = con
			.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
			for (int i = 0; i < 5; i++) {
				final SkillMacro macro = skillMacros[i];
				if (macro == null) {
					continue;
				}

				ps.setInt(1, id);
				ps.setInt(2, macro.getSkill1());
				ps.setInt(3, macro.getSkill2());
				ps.setInt(4, macro.getSkill3());
				ps.setString(5, macro.getName());
				ps.setInt(6, macro.getShout());
				ps.setInt(7, i);
				ps.execute();
			}
		}
	}

	private void deleteByCharacterId(final Connection con, final String sql) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setInt(1, id);
			ps.executeUpdate();
		}
	}

	@Override
	public final ActivePlayerStats getStats() {
		return stats;
	}

	public final PlayerRandomStream getRandomStream() {
		return randomStream;
	}

	public final void writeQuestInfoPacket(final PacketBuilder builder) {
		builder.writeAsShort(questInfo.size());

		for (final Entry<Integer, String> q : questInfo.entrySet()) {
			builder.writeAsShort(q.getKey());
			builder.writeLengthPrefixedString(q.getValue() == null ? "" : q.getValue());
		}
		builder.writeInt(0); // PQ rank and stuff
	}

	public final void updateInfoQuest(final int questid, final String data) {
		questInfo.put(questid, data);
		client.write(ChannelPackets.updateInfoQuest(questid, data));
	}

	public final String getInfoQuest(final int questid) {
		if (questInfo.containsKey(questid)) {
			return questInfo.get(questid);
		}
		return "";
	}

	public final int getNumQuest() {
		int i = 0;
		for (final QuestStatus q : quests.values()) {
			if (q.getState() == 2) {
				i++;
			}
		}
		return i;
	}

	public final byte getQuestCompletionStatus(final int questId) {
		for (final QuestStatus q : quests.values()) {
			if (q.getQuestId() == questId) {
				return q.getState();
			}
		}
		return 0;
	}

	public final QuestStatus getQuestStatus(final int questId) {
		QuestStatus status = quests.get(questId);
		if (status == null) {
			status = new QuestStatus(questId, (byte) 0);
			quests.put(questId, status);
		}
		return status;
	}

	public final QuestStatus getAddQuestStatus(final int questId) {
		if (!quests.containsKey(questId)) {
			final QuestStatus status = new QuestStatus(questId, (byte) 0);
			quests.put(questId, status);
			return status;
		}
		return quests.get(questId);
	}

	public final void updateQuest(final int questId) {
		final QuestStatus status = getQuestStatus(questId);
		switch (status.getState()) {
		case 0:
			client.write(ChannelPackets.forfeitQuest(this, questId));
			break;
		case 1:
			client.write(ChannelPackets.startQuest(this, questId, status.getCustomData()));
			client.write(ChannelPackets.updateQuestInfo(this, questId, status.getNpc(), (byte) 8));
			break;
		case 2:
			client.write(ChannelPackets.completeQuest(questId));
			break;
		}
	}

	public final Map<Integer, String> getQuestInfoMap() {
		return questInfo;
	}

	public final Map<Integer, QuestStatus> getQuestStatusMap() {
		return quests;
	}

	public boolean isActiveBuffedValue(final int skillid) {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());
		for (final BuffStatValueHolder value : allBuffs) {
			if (value.effect.isSkill() && value.effect.getSourceId() == skillid) {
				return true;
			}
		}
		return false;
	}

	public Integer getBuffedValue(final BuffStat effect) {
		final BuffStatValueHolder value = effects.get(effect);
		return value == null ? null : Integer.valueOf(value.value);
	}

	public final Integer getBuffedSkill_X(final BuffStat effect) {
		final BuffStatValueHolder value = effects.get(effect);
		if (value == null) {
			return null;
		}
		return value.effect.getX();
	}

	public final Integer getBuffedSkill_Y(final BuffStat effect) {
		final BuffStatValueHolder value = effects.get(effect);
		if (value == null) {
			return null;
		}
		return value.effect.getY();
	}

	public final StatEffect getBuffedSkillEffect(final BuffStat effect) {
		final BuffStatValueHolder value = effects.get(effect);
		if (value == null) {
			return null;
		}
		return value.effect;
	}

	public boolean isBuffFrom(final BuffStat stat, final ISkill skill) {
		final BuffStatValueHolder value = effects.get(stat);
		if (value == null) {
			return false;
		}
		return value.effect.isSkill() && value.effect.getSourceId() == skill.getId();
	}

	public int getBuffSource(final BuffStat stat) {
		final BuffStatValueHolder value = effects.get(stat);
		return value == null ? -1 : value.effect.getSourceId();
	}

	public int getItemQuantity(final int itemId, final boolean checkEquipped) {
		int count = inventory.get(GameConstants.getInventoryType(itemId)).countById(itemId);
		if (checkEquipped) {
			count += inventory.get(InventoryType.EQUIPPED).countById(itemId);
		}
		return count;
	}

	public int getReborns() {
		return reborns;
	}

	public int getVPoints() {
		return vpoints;
	}

	public int getSnipeDamage() {
		return Math.min(damageCap, (500000 * getReborns()) + 500000);
	}

	public int getMaxStats() {
		return (getJobId() > 999 && getJobId() < 2000 ? 15000 : 32000);

	}

	public int getNX() {
		return aCash;
	}

	public void gainVPoints(final int gainedpoints) {
		this.vpoints += gainedpoints;
	}

	@Override
	public int getWorldId() {
		return worldId;
	}

	public void setBuffedValue(final BuffStat effect, final int statValue) {
		final BuffStatValueHolder value = effects.get(effect);
		if (value == null) {
			return;
		}
		value.value = statValue;
	}

	public Long getBuffedStartTime(final BuffStat effect) {
		final BuffStatValueHolder value = effects.get(effect);
		return value == null ? null : Long.valueOf(value.startTime);
	}

	public StatEffect getStatForBuff(final BuffStat effect) {
		final BuffStatValueHolder value = effects.get(effect);
		return value == null ? null : value.effect;
	}

	private void prepareDragonBlood(final StatEffect bloodEffect) {
		if (dragonBloodSchedule != null) {
			dragonBloodSchedule.cancel(false);
		}
		dragonBloodSchedule = TimerManager.getInstance().register(new DragonBloodRunnable(bloodEffect), 4000, 4000);

	}

	private final class DragonBloodRunnable implements Runnable {

		private final StatEffect effect;

		public DragonBloodRunnable(final StatEffect effect) {
			this.effect = effect;
		}

		@Override
		public void run() {
			if (stats.getHp() - effect.getX() > 1) {
				cancelBuffStats(BuffStat.DRAGONBLOOD);
			} else {
				addHP(-effect.getX());
				final int bloodEffectSourceId = effect.getSourceId();
				final GamePacket ownEffectPacket = ChannelPackets.showOwnBuffEffect(bloodEffectSourceId, 5);
				client.write(ownEffectPacket);
				final GamePacket otherEffectPacket = ChannelPackets.showBuffeffect(getId(), bloodEffectSourceId, 5);
				map.broadcastMessage(ChannelCharacter.this, otherEffectPacket, false);
			}
		}
	}

	public void startFullnessSchedule(final int decrease, final Pet pet, final int petSlot) {
		final ScheduledFuture<?> schedule = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				final int newFullness = pet.getFullness() - decrease;
				if (newFullness <= 5) {
					pet.setFullness(15);
					unequipPet(pet, true, true);
				} else {
					pet.setFullness(newFullness);
					client.write(PetPacket.updatePet(pet, true));
				}
			}
		}, 60000, 60000);
		switch (petSlot) {
		case 0:
			fullnessSchedule = schedule;
			break;
		case 1:
			fullnessSchedule_1 = schedule;
			break;
		case 2:
			fullnessSchedule_2 = schedule;
			break;
		}
	}

	public void cancelFullnessSchedule(final int petSlot) {
		switch (petSlot) {
		case 0:
			if (fullnessSchedule != null) {
				fullnessSchedule.cancel(false);
			}
			break;
		case 1:
			if (fullnessSchedule_1 != null) {
				fullnessSchedule_1.cancel(false);
			}
			break;
		case 2:
			if (fullnessSchedule_2 != null) {
				fullnessSchedule_2.cancel(false);
			}
			break;
		}
	}

	public void startMapTimeLimitTask(int time, final GameMap to) {
		client.write(ChannelPackets.getClock(time));

		time *= 1000;
		mapTimeLimitTask = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				changeMap(to, to.getPortal(0));
			}
		}, time, time);
	}

	public void startFishingTask(final boolean VIP) {
		final int time = VIP ? 30000 : 60000;
		cancelFishingTask();

		fishing = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				if (!haveItem(2300000, 1, false, true)) {
					cancelFishingTask();
					return;
				}
				InventoryManipulator.removeById(client, getUseInventory(), 2300000, 1, false, false);

				final int randval = RandomRewards.getInstance().getFishingReward();

				switch (randval) {
				case 0: // Meso
					final int money = Randomizer.rand(10, 50000);
					gainMeso(money, true);
					client.write(UIPacket.fishingUpdate((byte) 1, money));
					break;
				case 1: // EXP
					final int experi = Randomizer.nextInt(GameConstants.getExpNeededForLevel(level) / 200);
					gainExp(experi, true, false, true);
					client.write(UIPacket.fishingUpdate((byte) 2, experi));
					break;
				default:
					InventoryManipulator.addById(client, randval, (short) 1);
					client.write(UIPacket.fishingUpdate((byte) 0, randval));
					break;
				}
				map.broadcastMessage(UIPacket.fishingCaught(id));
			}
		}, time, time);
	}

	public void cancelMapTimeLimitTask() {
		if (mapTimeLimitTask != null) {
			mapTimeLimitTask.cancel(false);
		}
	}

	public void cancelFishingTask() {
		if (fishing != null) {
			fishing.cancel(false);
		}
	}

	public void registerEffect(final StatEffect effect, final long starttime, final ScheduledFuture<?> schedule) {
		if (effect.isHide()) {
			this.hidden = true;
			map.broadcastMessage(this, ChannelPackets.removePlayerFromMap(getId()), false);
		} else if (effect.isDragonBlood()) {
			prepareDragonBlood(effect);
		} else if (effect.isBerserk()) {
			checkBerserk();
		} else if (effect.isBeholder()) {
			prepareBeholderEffect();
		}
		for (final BuffStatValue statup : effect.getStatups()) {
			effects.put(statup.stat, new BuffStatValueHolder(effect, starttime, schedule, statup.value));
		}
		stats.recalcLocalStats();
	}

	public List<BuffStat> getBuffStats(final StatEffect effect, final long startTime) {
		final List<BuffStat> bstats = new ArrayList<>();

		for (final Entry<BuffStat, BuffStatValueHolder> stateffect : effects.entrySet()) {
			final BuffStatValueHolder value = stateffect.getValue();
			if (value.effect.sameSource(effect) && (startTime == -1 || startTime == value.startTime)) {
				bstats.add(stateffect.getKey());
			}
		}
		return bstats;
	}

	private void deregisterBuffStats(final List<BuffStat> stats) {
		final List<BuffStatValueHolder> effectsToCancel = new ArrayList<>(stats.size());
		for (final BuffStat stat : stats) {
			final BuffStatValueHolder value = effects.get(stat);
			if (value != null) {
				effects.remove(stat);
				boolean addMbsvh = true;
				for (final BuffStatValueHolder contained : effectsToCancel) {
					if (value.startTime == contained.startTime && contained.effect == value.effect) {
						addMbsvh = false;
					}
				}
				if (addMbsvh) {
					effectsToCancel.add(value);
				}
				if (stat == BuffStat.SUMMON || stat == BuffStat.PUPPET || stat == BuffStat.MIRROR_TARGET) {
					final int summonId = value.effect.getSourceId();
					final Summon summon = summons.get(summonId);
					if (summon != null) {
						map.broadcastMessage(ChannelPackets.removeSummon(summon, true));
						map.removeMapObject(summon);
						removeVisibleMapObject(summon);
						summons.remove(summonId);
						if (summon.getSkill() == 1321007) {
							if (beholderHealingSchedule != null) {
								beholderHealingSchedule.cancel(false);
								beholderHealingSchedule = null;
							}
							if (beholderBuffSchedule != null) {
								beholderBuffSchedule.cancel(false);
								beholderBuffSchedule = null;
							}
						}
					}
				} else if (stat == BuffStat.DRAGONBLOOD) {
					if (dragonBloodSchedule != null) {
						dragonBloodSchedule.cancel(false);
						dragonBloodSchedule = null;
					}
				}
			}
		}
		for (final BuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
			if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).isEmpty()) {
				if (cancelEffectCancelTasks.schedule != null) {
					cancelEffectCancelTasks.schedule.cancel(false);
				}
			}
		}
	}

	/**
	 * @param effect
	 * @param overwrite
	 *            when overwrite is set no data is sent and all the Buffstats in
	 *            the StatEffect are deregistered
	 * @param startTime
	 */
	public void cancelEffect(final StatEffect effect, final boolean overwrite, final long startTime) {
		List<BuffStat> buffstats;
		if (!overwrite) {
			buffstats = getBuffStats(effect, startTime);
		} else {
			final List<BuffStatValue> statups = effect.getStatups();
			buffstats = new ArrayList<>(statups.size());
			for (final BuffStatValue statup : statups) {
				buffstats.add(statup.stat);
			}
		}
		deregisterBuffStats(buffstats);
		if (effect.isMagicDoor()) {
			// remove for all on maps
			if (!getDoors().isEmpty()) {
				final Door door = getDoors().iterator().next();
				for (final ChannelCharacter chr : door.getTarget().getCharacters()) {
					door.sendDestroyData(chr.getClient());
				}
				for (final ChannelCharacter chr : door.getTown().getCharacters()) {
					door.sendDestroyData(chr.getClient());
				}
				for (final Door destroyDoor : getDoors()) {
					door.getTarget().removeMapObject(destroyDoor);
					door.getTown().removeMapObject(destroyDoor);
				}
				clearDoors();
				silentPartyUpdate();
			}
		} else if (effect.isMonsterRiding()) {
			// if (effect.getSourceId() != 5221006) {
			// getMount().cancelSchedule();
			// }
		} else if (effect.isAranCombo()) {
			combo = 0;
		}
		// check if we are still logged in o.o
		if (!overwrite) {
			cancelPlayerBuffs(buffstats);
			if (effect.isHide() && (ChannelCharacter) map.getMapObject(getObjectId()) != null) {
				this.hidden = false;
				map.broadcastMessage(this, ChannelPackets.spawnPlayerMapObject(this), false);

				for (final Pet pet : pets) {
					if (pet.isSummoned()) {
						final GamePacket packet = PetPacket.showPet(this, pet);
						map.broadcastMessage(this, packet, false);
					}
				}
			}
		}
	}

	public void cancelBuffStats(final BuffStat stat) {
		final List<BuffStat> buffStatList = Arrays.asList(stat);
		deregisterBuffStats(buffStatList);
		cancelPlayerBuffs(buffStatList);
	}

	public void cancelEffectFromBuffStat(final BuffStat stat) {
		cancelEffect(effects.get(stat).effect, false, -1);
	}

	private void cancelPlayerBuffs(final List<BuffStat> buffstats) {
		if (ChannelServer.getPlayerStorage().getCharacterById(getId()) != null) {
			// are we still connected ?
			if (buffstats.contains(BuffStat.HOMING_BEACON)) {
				client.write(ChannelPackets.cancelHoming());
			} else {
				stats.recalcLocalStats();
				enforceMaxHpMp();
				client.write(ChannelPackets.cancelBuff(buffstats, buffstats.contains(BuffStat.MONSTER_RIDING)));
				map.broadcastMessage(this, ChannelPackets.cancelForeignBuff(getId(), buffstats), false);
			}
		}
		if (buffstats.contains(BuffStat.MONSTER_RIDING) && Jobs.isEvan(jobId) && jobId >= 2200) {
			makeDragon();
			map.spawnDragon(dragon);
		}
	}

	public void dispel() {
		if (!isHidden()) {
			final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());
			for (final BuffStatValueHolder value : allBuffs) {
				if (value.effect.isSkill() && value.schedule != null && !value.effect.isMorph()) {
					cancelEffect(value.effect, false, value.startTime);
				}
			}
		}
	}

	public void dispelSkill(final int skillid) {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			if (skillid == 0) {
				if (value.effect.isSkill()
					&& (value.effect.getSourceId() == 1004 || value.effect.getSourceId() == 10001004 || value.effect.getSourceId() == 20001004
						|| value.effect.getSourceId() == 20011004 || value.effect.getSourceId() == 1321007 || value.effect.getSourceId() == 2121005
						|| value.effect.getSourceId() == 2221005 || value.effect.getSourceId() == 2311006 || value.effect.getSourceId() == 2321003
						|| value.effect.getSourceId() == 3111002 || value.effect.getSourceId() == 3111005 || value.effect.getSourceId() == 3211002
						|| value.effect.getSourceId() == 3211005 || value.effect.getSourceId() == 4111002)) {
					cancelEffect(value.effect, false, value.startTime);
					break;
				}
			} else {
				if (value.effect.isSkill() && value.effect.getSourceId() == skillid) {
					cancelEffect(value.effect, false, value.startTime);
					break;
				}
			}
		}
	}

	public void clearAllBuffEffects() {
		effects.clear();
	}

	public void cancelAllBuffs() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			cancelEffect(value.effect, false, value.startTime);
		}
	}

	public void cancelMorphs() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			switch (value.effect.getSourceId()) {
			case 5111005:
			case 5121003:
			case 15111002:
			case 13111005:
				return; // Since we can't have more than 1, save up on loops
			default:
				if (value.effect.isMorph()) {
					cancelEffect(value.effect, false, value.startTime);
					continue;
				}
			}
		}
	}

	public int getMorphState() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());
		for (final BuffStatValueHolder value : allBuffs) {
			if (value.effect.isMorph()) {
				return value.effect.getSourceId();
			}
		}
		return -1;
	}

	public void silentGiveBuffs(final Collection<PlayerBuffValueHolder> buffs) {
		for (final PlayerBuffValueHolder value : buffs) {
			value.effect.silentApplyBuff(this, value.startTime);
		}
	}

	public List<PlayerBuffValueHolder> getAllBuffs() {
		final List<PlayerBuffValueHolder> ret = Lists.newArrayList();
		for (final BuffStatValueHolder value : effects.values()) {
			ret.add(new PlayerBuffValueHolder(value.startTime, value.effect));
		}
		return ret;
	}

	public void cancelMagicDoor() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			if (value.effect.isMagicDoor()) {
				cancelEffect(value.effect, false, value.startTime);
				break;
			}
		}
	}

	public final void handleEnergyCharge(final int skillid, final byte targets) {
		final ISkill echskill = SkillFactory.getSkill(skillid);
		final byte skilllevel = getCurrentSkillLevel(echskill);
		if (skilllevel > 0) {
			if (targets > 0) {
				if (getBuffedValue(BuffStat.ENERGY_CHARGE) == null) {
					echskill.getEffect(skilllevel).applyEnergyBuff(this, true); // Infinity
																				// time
				} else {
					Integer energyLevel = getBuffedValue(BuffStat.ENERGY_CHARGE);

					if (energyLevel < 10000) {
						energyLevel += (100 * targets);
						setBuffedValue(BuffStat.ENERGY_CHARGE, energyLevel);
						client.write(ChannelPackets.giveEnergyChargeTest(energyLevel));

						if (energyLevel >= 10000) {
							energyLevel = 10001;
						}
					} else if (energyLevel == 10001) {
						echskill.getEffect(skilllevel).applyEnergyBuff(this, false); // One
																						// with
																						// time
						energyLevel = 10002;
					}
				}
			}
		}
	}

	public final void handleOrbgain() {
		final int orbcount = getBuffedValue(BuffStat.COMBO);
		ISkill comboSkill;
		ISkill advancedComboSkill;

		switch (getJobId()) {
		case 1110:
		case 1111:
			comboSkill = SkillFactory.getSkill(11111001);
			advancedComboSkill = SkillFactory.getSkill(11110005);
			break;
		default:
			comboSkill = SkillFactory.getSkill(1111002);
			advancedComboSkill = SkillFactory.getSkill(1120003);
			break;
		}

		StatEffect ceffect = null;
		final int advComboSkillLevel = getCurrentSkillLevel(advancedComboSkill);
		if (advComboSkillLevel > 0) {
			ceffect = advancedComboSkill.getEffect(advComboSkillLevel);
		} else {
			ceffect = comboSkill.getEffect(getCurrentSkillLevel(comboSkill));
		}

		if (orbcount < ceffect.getX() + 1) {
			int neworbcount = orbcount + 1;
			if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
				if (neworbcount < ceffect.getX() + 1) {
					neworbcount++;
				}
			}
			final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.COMBO, neworbcount));
			setBuffedValue(BuffStat.COMBO, neworbcount);
			int duration = ceffect.getDuration();
			duration += (int) ((getBuffedStartTime(BuffStat.COMBO) - System.currentTimeMillis()));

			client.write(ChannelPackets.giveBuff(1111002, duration, stat, ceffect));
			map.broadcastMessage(this, ChannelPackets.giveForeignBuff(getId(), stat, ceffect), false);
		}
	}

	public void handleOrbconsume() {
		ISkill comboSkill;

		switch (getJobId()) {
		case 1110:
		case 1111:
			comboSkill = SkillFactory.getSkill(11111001);
			break;
		default:
			comboSkill = SkillFactory.getSkill(1111002);
			break;
		}

		final StatEffect ceffect = comboSkill.getEffect(getCurrentSkillLevel(comboSkill));
		final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.COMBO, 1));
		setBuffedValue(BuffStat.COMBO, 1);
		int duration = ceffect.getDuration();
		duration += (int) ((getBuffedStartTime(BuffStat.COMBO) - System.currentTimeMillis()));

		client.write(ChannelPackets.giveBuff(1111002, duration, stat, ceffect));
		map.broadcastMessage(this, ChannelPackets.giveForeignBuff(getId(), stat, ceffect), false);
	}

	private void silentEnforceMaxHpMp() {
		stats.setMp(stats.getMp());
		stats.setHp(stats.getHp(), true);
	}

	private void enforceMaxHpMp() {
		final List<StatValue> statups = new ArrayList<>(2);
		if (stats.getMp() > stats.getCurrentMaxMp()) {
			stats.setMp(stats.getMp());
			statups.add(new StatValue(Stat.MP, Integer.valueOf(stats.getMp())));
		}
		if (stats.getHp() > stats.getCurrentMaxHp()) {
			stats.setHp(stats.getHp());
			statups.add(new StatValue(Stat.HP, Integer.valueOf(stats.getHp())));
		}
		if (statups.size() > 0) {
			client.write(ChannelPackets.updatePlayerStats(statups, getJobId()));
		}
	}

	public GameMap getMap() {
		return map;
	}

	public MonsterBook getMonsterBook() {
		return monsterBook;
	}

	public void setMap(final GameMap newMap) {
		this.map = newMap;
	}

	public void setMap(final int newMapId) {
		this.mapId = newMapId;
	}

	@Override
	public int getMapId() {
		if (map != null) {
			return map.getId();
		}
		return mapId;
	}

	@Override
	public int getInitialSpawnPoint() {
		return initialSpawnPoint;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getWorldName() {
		return name;
	}

	public static int getDamageCap() {
		return damageCap;
	}

	public final String getBlessOfFairyOrigin() {
		return this.BlessOfFairy_Origin;
	}

	@Override
	public final int getLevel() {
		return level;
	}

	@Override
	public int getFame() {
		return fame;
	}

	public final int getDojo() {
		return dojo;
	}

	public final int getDojoRecord() {
		return dojoRecord;
	}

	public final int getFallCounter() {
		return fallcounter;
	}

	public final ChannelClient getClient() {
		return client;
	}

	public final void setClient(final ChannelClient client) {
		this.client = client;
	}

	@Override
	public int getExp() {
		return exp;
	}

	@Override
	public int getRemainingAp() {
		return remainingAp;
	}

	@Override
	public int[] getRemainingSps() {
		return remainingSp;
	}

	@Override
	public int getRemainingSp() {
		return remainingSp[Skills.getSkillbook(jobId)]; // default
	}

	@Override
	public int getRemainingSp(final int skillbook) {
		return remainingSp[skillbook];
	}

	@Override
	public int getRemainingSpSize() {
		int ret = 0;
		for (int i = 0; i < remainingSp.length; i++) {
			if (remainingSp[i] > 0) {
				ret++;
			}

		}
		return ret;
	}

	public int getMpApUsed() {
		return mpApUsed;
	}

	public void setMpApUsed(final int mpApUsed) {
		this.mpApUsed = mpApUsed;
	}

	public int getHpApUsed() {
		return hpApUsed;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHpApUsed(final int hpApUsed) {
		this.hpApUsed = hpApUsed;
	}

	@Override
	public int getSkinColorId() {
		return skinColorId;
	}

	public void setSkinColorId(final int skinColorId) {
		this.skinColorId = skinColorId;
	}

	@Override
	public int getJobId() {
		return jobId;
	}

	@Override
	public Gender getGender() {
		return gender;
	}

	@Override
	public int getHairId() {
		return hairId;
	}

	@Override
	public int getFaceId() {
		return faceId;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setExp(final int exp) {
		this.exp = exp;
	}

	public void setHairId(final int hairId) {
		this.hairId = hairId;
	}

	public void setFaceId(final int faceId) {
		this.faceId = faceId;
	}

	public void setFame(final int fame) {
		this.fame = fame;
	}

	public void setDojo(final int dojo) {
		this.dojo = dojo;
	}

	public void setDojoRecord(final boolean reset) {
		if (reset) {
			dojo = 0;
			dojoRecord = 0;
		} else {
			dojoRecord++;
		}
	}

	public void setFallCounter(final int fallcounter) {
		this.fallcounter = fallcounter;
	}

	public void setRemainingAp(final int remainingAp) {
		this.remainingAp = remainingAp;
	}

	public void setRemainingSp(final int remainingSp) {
		this.remainingSp[Skills.getSkillbook(jobId)] = remainingSp; // default
	}

	public void setRemainingSp(final int remainingSp, final int skillbook) {
		this.remainingSp[skillbook] = remainingSp;
	}

	public void setGender(final Gender gender) {
		this.gender = gender;
	}

	public CheatTracker getCheatTracker() {
		return cheatTracker;
	}

	public BuddyList getBuddyList() {
		return buddies;
	}

	public void addFame(final int famechange) {
		this.fame += famechange;
	}

	public void changeMapBanish(final int mapid, final String portal, final String msg) {
		sendNotice(5, msg);
		final GameMap newMap = ChannelServer.getMapFactory().getMap(mapid);
		changeMap(newMap, newMap.getPortal(portal));
	}

	public void changeMap(final GameMap to, final Point pos) {
		changeMapInternal(to, pos, ChannelPackets.getWarpToMap(to, 0x81, this));
	}

	public void changeMap(final GameMap to, final Portal pto) {
		changeMapInternal(to, pto.getPosition(), ChannelPackets.getWarpToMap(to, pto.getId(), this));
	}

	private void changeMapInternal(final GameMap to, final Point pos, final GamePacket warpPacket) {
		if (eventInstance != null) {
			eventInstance.changedMap(this, to.getId());
		}
		client.write(warpPacket);
		map.removePlayer(this);
		if (ChannelServer.getPlayerStorage().getCharacterById(getId()) != null) {
			map = to;
			setPosition(pos);
			to.addPlayer(this);
			stats.relocHeal();
		}
	}

	public void leaveMap() {
		controlledMonsters.clear();
		visibleMapObjects.clear();
		if (chair != 0) {
			cancelFishingTask();
			chair = 0;
		}
		if (hpDecreaseTask != null) {
			hpDecreaseTask.cancel(false);
		}
		cancelMapTimeLimitTask();
	}

	public void resetStats(final int str, final int dex, final int int_, final int luk) {
		final List<StatValue> newStats = Lists.newArrayList();
		final ChannelCharacter chr = this;
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
		newStats.add(new StatValue(Stat.STR, str));
		newStats.add(new StatValue(Stat.DEX, dex));
		newStats.add(new StatValue(Stat.INT, int_));
		newStats.add(new StatValue(Stat.LUK, luk));
		newStats.add(new StatValue(Stat.AVAILABLE_AP, total));
		client.write(ChannelPackets.updatePlayerStats(newStats, false, chr.getJobId()));
	}

	public void startHurtHp() {
		hpDecreaseTask = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				if (map.getHPDec() < 1 || !isAlive()) {
					return;
				} else if (getEquippedItemsInventory().findById(map.getHPDecProtect()) == null) {
					addHP(-map.getHPDec());
				}
			}
		}, 10000);
	}

	public void changeJob(final int newJob) {
		final boolean wasEvan = Jobs.isEvan(jobId);
		this.jobId = (short) newJob;
		if (Jobs.isBeginner(newJob)) {
			if (Jobs.isEvan(newJob)) {
				remainingSp[Skills.getSkillbook(newJob)] += 3;
			} else {
				remainingSp[Skills.getSkillbook(newJob)]++;
				if (newJob % 10 >= 2) {
					remainingSp[Skills.getSkillbook(newJob)] += 2;
				}
			}
		}
		if (!isGM()) {
			if (newJob % 1000 == 100) { // first job = warrior
				resetStats(25, 4, 4, 4);
			} else if (newJob % 1000 == 200) {
				resetStats(4, 4, 20, 4);
			} else if (newJob % 1000 == 300 || newJob % 1000 == 400) {
				resetStats(4, 25, 4, 4);
			} else if (newJob % 1000 == 500) {
				resetStats(4, 20, 4, 4);
			}
		}
		client.write(ChannelPackets.updateSp(this, false, wasEvan));
		updateSingleStat(Stat.JOB, newJob);

		int maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

		switch (jobId) {
		case 100: // Warrior
		case 1100: // Soul Master
		case 2100: // Aran
			maxhp += Randomizer.rand(200, 250);
			break;
		case 200: // Magician
		case 2200:
		case 2210:
			maxmp += Randomizer.rand(100, 150);
			break;
		case 300: // Bowman
		case 400: // Thief
		case 500: // Pirate
			maxhp += Randomizer.rand(100, 150);
			maxmp += Randomizer.rand(25, 50);
			break;
		case 110: // Fighter
			maxhp += Randomizer.rand(300, 350);
			break;
		case 120: // Page
		case 130: // Spearman
		case 1110: // Soul Master
		case 2110: // Aran
			maxhp += Randomizer.rand(300, 350);
			break;
		case 210: // FP
		case 220: // IL
		case 230: // Cleric
			maxmp += Randomizer.rand(400, 450);
			break;
		case 310: // Bowman
		case 320: // Crossbowman
		case 410: // Assasin
		case 420: // Bandit
		case 430:
		case 1310: // Wind Breaker
		case 1410: // Night Walker
			maxhp += Randomizer.rand(300, 350);
			maxhp += Randomizer.rand(150, 200);
			break;
		case 900: // GM
		case 800: // Manager
			maxhp += 30000;
			maxhp += 30000;
			break;
		}
		if (maxhp >= 30000) {
			maxhp = 30000;
		}
		if (maxmp >= 30000) {
			maxmp = 30000;
		}
		stats.setMaxHp(maxhp);
		stats.setMaxMp(maxmp);
		stats.setHp(maxhp);
		stats.setMp(maxmp);
		final List<StatValue> statup = Lists.newArrayList();
		statup.add(new StatValue(Stat.MAX_HP, Integer.valueOf(maxhp)));
		statup.add(new StatValue(Stat.MAX_MP, Integer.valueOf(maxmp)));
		stats.recalcLocalStats();
		client.write(ChannelPackets.updatePlayerStats(statup, getJobId()));
		map.broadcastMessage(this, ChannelPackets.showForeignEffect(getId(), 8), false);
		silentPartyUpdate();
		updateJob();
		if (dragon != null) {
			map.broadcastMessage(ChannelPackets.removeDragon(this.id));
			map.removeMapObject(dragon);
			dragon = null;
		}
		if (newJob >= 2200 && newJob <= 2218) { // make new
			if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
				cancelBuffStats(BuffStat.MONSTER_RIDING);
			}
			makeDragon();
			map.spawnDragon(dragon);
			if (newJob == 2217) {
				for (final int id : Skill.EVAN_SKILLS_1) {
					final ISkill skill = SkillFactory.getSkill(id);
					if (skill != null && getCurrentSkillLevel(skill) <= 0 && getMasterSkillLevel(skill) <= 0) {
						changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel());
					}
				}
			} else if (newJob == 2218) {
				for (final int id : Skill.EVAN_SKILLS_2) {
					final ISkill skill = SkillFactory.getSkill(id);
					if (skill != null && getCurrentSkillLevel(skill) <= 0 && getMasterSkillLevel(skill) <= 0) {
						changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel());
					}
				}
			}

		} else if (newJob >= 431 && newJob <= 434) { // master skills
			for (final int id : Skill.DUALBLADE_SKILLS) {
				final ISkill skill = SkillFactory.getSkill(id);
				if (skill != null && getCurrentSkillLevel(skill) <= 0 && getMasterSkillLevel(skill) <= 0) {
					changeSkillLevel(skill, (byte) 0, (byte) skill.getMasterLevel());
				}
			}
		}
	}

	public void makeDragon() {
		dragon = new Dragon(this);
	}

	public Dragon getDragon() {
		return dragon;
	}

	public void gainAp(final int ap) {
		this.remainingAp += ap;
		updateSingleStat(Stat.AVAILABLE_AP, this.remainingAp);
	}

	public void gainSP(final int sp) {
		this.remainingSp[Skills.getSkillbook(jobId)] += sp; // default
		client.write(ChannelPackets.updateSp(this, false));
		client.write(UIPacket.getSPMsg((byte) sp));
	}

	public void changeSkillLevel(final ISkill skill, byte newLevel, byte newMasterlevel) {
		if (skill == null || (!GameConstants.isApplicableSkill(skill.getId()) && !GameConstants.isApplicableSkill_(skill.getId()))) {

			return;

		}
		if (newLevel == 0 && newMasterlevel == 0) {
			if (skills.containsKey(skill)) {
				skills.remove(skill);
			}
		} else {
			if (newLevel < 0) {
				newLevel = 0;
			}
			if (newMasterlevel < 0) {
				newMasterlevel = 0;
			}
			skills.put(skill, new SkillEntry(newLevel, newMasterlevel));
		}
		if (Skills.isRecoveryIncSkill(skill.getId())) {
			stats.relocHeal();
		} else if (Skills.isElementAmplification(skill.getId())) {
			stats.recalcLocalStats();
		}
		client.write(ChannelPackets.updateSkill(skill.getId(), newLevel, newMasterlevel));
	}

	public void playerDead() {
		if (getEventInstance() != null) {
			getEventInstance().playerKilled(this);
		}
		dispelSkill(0);
		if (getBuffedValue(BuffStat.MORPH) != null) {
			cancelEffectFromBuffStat(BuffStat.MORPH);
		}
		if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
			cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
		}
		if (getBuffedValue(BuffStat.SUMMON) != null) {
			cancelEffectFromBuffStat(BuffStat.SUMMON);
		}
		if (getBuffedValue(BuffStat.PUPPET) != null) {
			cancelEffectFromBuffStat(BuffStat.PUPPET);
		}
		if (getBuffedValue(BuffStat.MIRROR_TARGET) != null) {
			cancelEffectFromBuffStat(BuffStat.MIRROR_TARGET);
		}

		if (jobId != 0 && jobId != 1000 && jobId != 2000 && jobId != 2001) {
			int charms = getItemQuantity(5130000, false);
			if (charms > 0) {
				InventoryManipulator.removeById(client, getCashInventory(), 5130000, 1, true, false);

				charms--;
				if (charms > 0xFF) {
					charms = 0xFF;
				}
				client.write(MTSCSPacket.useCharm((byte) charms, (byte) 0));
			} else {
				float diepercentage = 0.0f;
				final int expforlevel = GameConstants.getExpNeededForLevel(level);
				if (map.isTown() || FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
					diepercentage = 0.01f;
				} else {
					float v8 = 0.0f;
					if (this.jobId / 100 == 3) {
						v8 = 0.08f;
					} else {
						v8 = 0.2f;
					}
					diepercentage = (float) (v8 / this.stats.getLuk() + 0.05);
				}
				int v10 = (int) (exp - (long) ((double) expforlevel * diepercentage));
				if (v10 < 0) {
					v10 = 0;
				}
				this.exp = v10;
			}
		}
		this.updateSingleStat(Stat.EXP, this.exp);
	}

	public void updatePartyMemberHP() {
		if (party != null) {
			final int channel = client.getChannelId();
			for (final PartyMember partychar : party.getMembers()) {
				if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
					final ChannelCharacter other = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
					if (other != null) {
						final GamePacket packet = ChannelPackets.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp());
						other.getClient().write(packet);
					}
				}
			}
		}
	}

	public void receivePartyMemberHP() {
		final int channel = client.getChannelId();
		for (final PartyMember partychar : party.getMembers()) {
			if (partychar.getMapId() == getMapId() && partychar.getChannel() == channel) {
				final ChannelCharacter other = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
				if (other != null) {
					final GamePacket packet = ChannelPackets.updatePartyMemberHP(other.getId(), other.getStats().getHp(), other.getStats().getCurrentMaxHp());
					client.write(packet);
				}
			}
		}
	}

	/**
	 * Convenience function which adds the supplied parameter to the current hp
	 * then directly does a updateSingleStat.
	 * 
	 * @see ChannelCharacter#setHp(int)
	 * @param delta
	 */
	public void addHP(final int delta) {
		if (stats.setHp(stats.getHp() + delta)) {
			updateSingleStat(Stat.HP, stats.getHp());
		}
	}

	/**
	 * Convenience function which adds the supplied parameter to the current mp
	 * then directly does a updateSingleStat.
	 * 
	 * @see ChannelCharacter#setMp(int)
	 * @param delta
	 */
	public void addMP(final int delta) {
		if (stats.setMp(stats.getMp() + delta)) {
			updateSingleStat(Stat.MP, stats.getMp());
		}
	}

	public void addMPHP(final int hpDiff, final int mpDiff) {
		final List<StatValue> statups = new ArrayList<>();

		if (stats.setHp(stats.getHp() + hpDiff)) {
			statups.add(new StatValue(Stat.HP, Integer.valueOf(stats.getHp())));
		}
		if (stats.setMp(stats.getMp() + mpDiff)) {
			statups.add(new StatValue(Stat.MP, Integer.valueOf(stats.getMp())));
		}
		if (statups.size() > 0) {
			client.write(ChannelPackets.updatePlayerStats(statups, getJobId()));
		}
	}

	public void updateSingleStat(final Stat stat, final int newval) {
		updateSingleStat(stat, newval, false);
	}

	/**
	 * Updates a single stat of this GameCharacter for the client. This method
	 * only creates and sends an update packet, it does not update the stat
	 * stored in this GameCharacter instance.
	 * 
	 * @param stat
	 * @param newval
	 * @param itemReaction
	 */
	public void updateSingleStat(final Stat stat, final int newval, final boolean itemReaction) {
		if (stat == Stat.AVAILABLE_SP) {
			client.write(ChannelPackets.updateSp(this, itemReaction, false));
			return;
		}
		final StatValue value = new StatValue(stat, Integer.valueOf(newval));
		client.write(ChannelPackets.updatePlayerStats(Collections.singletonList(value), itemReaction, getJobId()));
	}

	public void gainExp(final int total, final boolean show, final boolean inChat, final boolean white) {
		if (level == 200 || (Jobs.isCygnus(jobId) && level == 120)) {
			final int needed = GameConstants.getExpNeededForLevel(level);
			if (exp + total > needed) {
				setExp(needed);
			} else {
				exp += total;
			}
		} else {
			if (exp + total >= GameConstants.getExpNeededForLevel(level)) {
				exp += total;
				levelUp();

				final int needed = GameConstants.getExpNeededForLevel(level);
				if (exp > needed) {
					setExp(needed);
				}
			} else {
				exp += total;
			}
		}
		if (total != 0) {
			if (exp < 0) { // After adding, and negative
				if (total > 0) {
					setExp(GameConstants.getExpNeededForLevel(level));
				} else if (total < 0) {
					setExp(0);
				}
			}
			updateSingleStat(Stat.EXP, getExp());
			if (show) { // still show the expgain even if it's not there
				client.write(ChannelPackets.GainEXP_Others(total, inChat, white));
			}
		}
	}

	public void gainExpMonster(final int gain, final boolean show, final boolean white, final byte partyMembers, final int CLASS_EXP) {
		final Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getDefault());
		final int day = cal.get(Calendar.DAY_OF_WEEK);
		final int hour = cal.get(Calendar.HOUR_OF_DAY);
		int eventExp = 0;
		final int weddingExp = 0;
		int partyRingExp = 0;
		int partyExp = 0;
		int premiumExp = 0;
		int itemExp = 0;
		int rainbowExp = 0;

		int baseExp = gain;
		if ((haveItem(5210006, 1, false, true) && hour > 22 && hour < 2) || (haveItem(5210007, 1, false, true) && hour > 2 && hour < 6)
			|| (haveItem(5210008, 1, false, true) && hour > 6 && hour < 10) || (haveItem(5210009, 1, false, true) && hour > 10 && hour < 14)
			|| (haveItem(5210010, 1, false, true) && hour > 14 && hour < 18) || (haveItem(5210011, 1, false, true) && hour > 18 && hour < 22)) {
			baseExp *= 2;
		}

		if (level >= 1 && level <= 10) {
			eventExp = (int) (baseExp * 0.1);
			premiumExp = (int) (baseExp * 0.1);
			itemExp = (int) (baseExp * 0.1);
		}

		if ((haveItem(1112127, 1, true, true))) {
			// Welcome Back Ring | must be equipped in order to work
			partyRingExp = (int) (baseExp * 0.8);
		}

		if (partyMembers > 1) {
			partyExp = (int) (((float) (baseExp / 10)) * (partyMembers + 1)); // 10%
		}
		if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
			// Saturday and Sunday
			rainbowExp = (int) (baseExp * 0.1);
		}
		if (level == 200 || (Jobs.isCygnus(jobId) && level == 120)) {
			final int needed = GameConstants.getExpNeededForLevel(level);
			if (exp + baseExp > needed) {
				setExp(needed);
			} else {
				exp += baseExp + eventExp + weddingExp + partyRingExp + partyExp + premiumExp + itemExp + rainbowExp + CLASS_EXP;
			}
		} else {
			if (exp + baseExp >= GameConstants.getExpNeededForLevel(level)) {
				exp += baseExp;
				levelUp();
				final int needed = GameConstants.getExpNeededForLevel(level);
				if (exp > needed) {
					setExp(needed);
				}
			} else {
				exp += baseExp + eventExp + weddingExp + partyRingExp + partyExp + premiumExp + itemExp + rainbowExp + CLASS_EXP;
			}
		}
		if (gain != 0) {
			if (exp < 0) {
				// After adding, and negative
				if (gain > 0) {
					setExp(GameConstants.getExpNeededForLevel(level));
				} else if (gain < 0) {
					setExp(0);
				}
			}
			updateSingleStat(Stat.EXP, getExp());
			if (show) {
				// still show the expgain even if it's not there
				client.write(ChannelPackets.GainEXP_Monster(baseExp, white, eventExp, weddingExp, partyRingExp, partyExp, premiumExp, itemExp, rainbowExp,
					CLASS_EXP));
			}
		}
	}

	public void silentPartyUpdate() {
		if (party != null) {
			try {
				final PartyMember newMember = new PartyMember(party.getId(), this);
				ChannelServer.getWorldInterface().updateParty(party.getId(), PartyOperation.SILENT_UPDATE, newMember);
			} catch (final RemoteException e) {
				System.err.println("REMOTE THROW, silentPartyUpdate" + e);
				ChannelServer.pingWorld();
			}
		}
	}

	public boolean isGM() {
		return gmLevel > 0;
	}

	@Override
	public int getGmLevel() {
		return gmLevel;
	}

	public boolean hasGmLevel(final int level) {
		return gmLevel >= level;
	}

	public final Inventory getEquipInventory() {
		return inventory.get(InventoryType.EQUIP);
	}

	public final Inventory getUseInventory() {
		return inventory.get(InventoryType.USE);
	}

	public final Inventory getSetupInventory() {
		return inventory.get(InventoryType.SETUP);
	}

	public final Inventory getEtcInventory() {
		return inventory.get(InventoryType.ETC);
	}

	public final Inventory getCashInventory() {
		return inventory.get(InventoryType.CASH);
	}

	@Override
	public final Inventory getEquippedItemsInventory() {
		return inventory.get(InventoryType.EQUIPPED);
	}

	public final Inventory getInventoryForItem(final int itemId) {
		return inventory.get(GameConstants.getInventoryType(itemId));
	}

	public final Inventory getInventoryByTypeByte(final byte typeByte) {
		final InventoryType type = InventoryType.fromNumber(typeByte);
		if (type == null) {
			return null;
		}
		return inventory.get(type);
	}

	public final MultiInventory getInventories() {
		return inventory;
	}

	public final void expirationTask() {
		long expiration;
		final long currenttime = System.currentTimeMillis();
		final List<IItem> toberemove = new ArrayList<>(); // This is here to
															// prevent deadlock.

		for (final Inventory inv : inventory) {
			for (final IItem item : inv) {
				expiration = item.getExpiration();

				if (expiration != -1 && !GameConstants.isPet(item.getItemId())) {
					final byte flag = item.getFlag();

					if (ItemFlag.LOCK.check(flag)) {
						if (currenttime > expiration) {
							item.setExpiration(-1);
							item.setFlag((byte) (flag - ItemFlag.LOCK.getValue()));
							client.write(ChannelPackets.updateSpecialItemUse(item, item.getType().asByte()));
						}
					} else if (currenttime > expiration) {
						client.write(MTSCSPacket.itemExpired(item.getItemId()));
						toberemove.add(item);
					}
				}
			}
			for (final IItem item : toberemove) {
				InventoryManipulator.removeFromSlot(client, inv, item.getPosition(), item.getQuantity(), false);
			}
		}
	}

	public Shop getShop() {
		return shop;
	}

	public void setShop(final Shop shop) {
		this.shop = shop;
	}

	@Override
	public int getMeso() {
		return meso;
	}

	public final EnumMap<SavedLocationType, Integer> getSavedLocations() {
		return Maps.newEnumMap(this.savedLocations);
	}

	public int getSavedLocation(final SavedLocationType type) {
		return savedLocations.get(type).intValue();
	}

	public void saveLocation(final SavedLocationType type) {
		savedLocations.put(type, this.mapId);
	}

	public void clearSavedLocation(final SavedLocationType type) {
		savedLocations.remove(type);
	}

	public void gainMeso(final int gain, final boolean show) {
		gainMeso(gain, show, false, false);
	}

	public void gainMeso(final int gain, final boolean show, final boolean enableActions) {
		gainMeso(gain, show, enableActions, false);
	}

	public void gainMeso(final int gain, final boolean show, final boolean enableActions, final boolean inChat) {
		if (meso + gain < 0) {
			client.write(ChannelPackets.enableActions());
			return;
		}
		meso += gain;
		updateSingleStat(Stat.MESO, meso, enableActions);
		if (show) {
			client.write(ChannelPackets.showMesoGain(gain, inChat));
		}
	}

	public void controlMonster(final Monster monster, final boolean aggro) {
		monster.setController(this);
		controlledMonsters.add(monster);
		client.write(MobPacket.controlMonster(monster, false, aggro));
	}

	public void stopControllingMonster(final Monster monster) {
		controlledMonsters.remove(monster);
	}

	public void checkMonsterAggro(final Monster monster) {
		if (monster.getController() == this) {
			monster.setControllerHasAggro(true);
		} else {
			monster.switchController(this, true);
		}
	}

	public Collection<Monster> getControlledMonsters() {
		return Collections.unmodifiableCollection(controlledMonsters);
	}

	public int getAccountId() {
		return accountId;
	}

	public void mobKilled(final int id) {
		for (final Map.Entry<Integer, QuestStatus> entry : quests.entrySet()) {
			final QuestStatus status = entry.getValue();
			if (status.getState() != 1 || !status.hasMobKills()) {
				continue;
			}
			if (status.mobKilled(id)) {
				client.write(ChannelPackets.updateQuestMobKills(status));
				final int questId = status.getQuestId();
				final QuestInfo info = QuestInfoProvider.getInfo(questId);
				if (info.canComplete(this, null)) {
					client.write(ChannelPackets.getShowQuestCompletion(questId));
				}
			}
		}
	}

	public final List<QuestStatus> getStartedQuests() {
		final List<QuestStatus> ret = new LinkedList<>();
		for (final QuestStatus q : quests.values()) {
			if (q.getState() == 1) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public final List<QuestStatus> getCompletedQuests() {
		final List<QuestStatus> ret = new LinkedList<>();
		for (final QuestStatus q : quests.values()) {
			if (q.getState() == 2) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public Map<ISkill, SkillEntry> getSkills() {
		return Collections.unmodifiableMap(skills);
	}

	public byte getCurrentSkillLevel(final ISkill skill) {
		final SkillEntry ret = skills.get(skill);
		if (ret == null) {
			return 0;
		}
		return ret.getCurrentLevel();
	}

	public byte getMasterSkillLevel(final ISkill skill) {
		final SkillEntry ret = skills.get(skill);
		if (ret == null) {
			return 0;
		}
		return ret.getMasterLevel();
	}

	public void levelUp() {
		if (Jobs.isCygnus(jobId)) {
			if (level <= 70) {
				remainingAp += 6;
			} else {
				remainingAp += 5;
			}
		} else {
			remainingAp += 5;
		}
		int maxhp = stats.getMaxHp();
		int maxmp = stats.getMaxMp();
		if (jobId == 0 || jobId == 1000 || jobId == 2000) {
			// Beginner
			maxhp += Randomizer.rand(12, 16);
			maxmp += Randomizer.rand(10, 12);
		} else if (jobId >= 100 && jobId <= 132) {
			// Warrior
			final ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
			final int slevel = getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(24, 28);
			maxmp += Randomizer.rand(4, 6);
		} else if (jobId >= 200 && jobId <= 232) {
			// Magician
			final ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
			final int skillLevel = getCurrentSkillLevel(improvingMaxMP);
			if (skillLevel > 0) {
				maxmp += improvingMaxMP.getEffect(skillLevel).getX() * 2;
			}
			maxhp += Randomizer.rand(10, 14);
			maxmp += Randomizer.rand(22, 24);
		} else if ((jobId >= 300 && jobId <= 322) || (jobId >= 400 && jobId <= 434) || (jobId >= 1300 && jobId <= 1311) || (jobId >= 1400 && jobId <= 1411)) {
			// Bowman, Thief, Wind Breaker and Night Walker
			maxhp += Randomizer.rand(20, 24);
			maxmp += Randomizer.rand(14, 16);
		} else if (jobId >= 500 && jobId <= 522) {
			// Pirate
			final ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
			final int slevel = getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(22, 26);
			maxmp += Randomizer.rand(18, 22);
		} else if (jobId >= 1100 && jobId <= 1111) {
			// Soul Master
			final ISkill improvingMaxHP = SkillFactory.getSkill(11000000);
			final int slevel = getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(24, 28);
			maxmp += Randomizer.rand(4, 6);
		} else if (jobId >= 1200 && jobId <= 1211) {
			// Flame Wizard
			final ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
			final int slevel = getCurrentSkillLevel(improvingMaxMP);
			if (slevel > 0) {
				maxmp += improvingMaxMP.getEffect(slevel).getX() * 2;
			}
			maxhp += Randomizer.rand(10, 14);
			maxmp += Randomizer.rand(22, 24);
		} else if (jobId >= 2200 && jobId <= 2218) {
			// Evan
			maxhp += Randomizer.rand(12, 16);
			maxmp += Randomizer.rand(50, 52);
		} else if (jobId >= 1500 && jobId <= 1512) {
			// Pirate
			final ISkill improvingMaxHP = SkillFactory.getSkill(15100000);
			final int slevel = getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(22, 26);
			maxmp += Randomizer.rand(18, 22);
		} else if (jobId >= 2100 && jobId <= 2112) {
			// Aran
			maxhp += Randomizer.rand(50, 52);
			maxmp += Randomizer.rand(4, 6);
		} else {
			// GameMaster
			maxhp += Randomizer.rand(50, 100);
			maxmp += Randomizer.rand(50, 100);
		}
		maxmp += stats.getTotalInt() / 10;
		exp -= GameConstants.getExpNeededForLevel(level);
		level += 1;
		if (level == 200 && !isGM()) {
			try {
				final StringBuilder sb = new StringBuilder("[Congratulation] ");
				final IItem medal = getEquippedItemsInventory().getItem((byte) -46);
				if (medal != null) { // Medal
					sb.append("<");
					sb.append(ItemInfoProvider.getInstance().getName(medal.getItemId()));
					sb.append("> ");
				}
				sb.append(getName());
				sb.append(" has achieved Level 200. Let us Celebrate! Maplers!");
				ChannelServer.getWorldInterface().broadcastMessage(ChannelPackets.serverNotice(6, sb.toString()));
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}
		maxhp = Math.min(30000, maxhp);
		maxmp = Math.min(30000, maxmp);
		final List<StatValue> statup = new ArrayList<>(8);
		statup.add(new StatValue(Stat.MAX_HP, maxhp));
		statup.add(new StatValue(Stat.MAX_MP, maxmp));
		statup.add(new StatValue(Stat.HP, maxhp));
		statup.add(new StatValue(Stat.MP, maxmp));
		statup.add(new StatValue(Stat.EXP, exp));
		statup.add(new StatValue(Stat.LEVEL, level));
		if (jobId != 0 && jobId != 1000 && jobId != 2000 && jobId != 2001) { // Not
																				// Beginner,
																				// Nobless
																				// and
																				// Legend
			remainingSp[Skills.getSkillbook(jobId)] += 3;
			client.write(ChannelPackets.updateSp(this, false));
		} else {
			if (level <= 10) {
				stats.setStr(stats.getStr() + remainingAp);
				remainingAp = 0;
				statup.add(new StatValue(Stat.STR, stats.getStr()));
			}
		}
		statup.add(new StatValue(Stat.AVAILABLE_AP, remainingAp));
		stats.setMaxHp(maxhp);
		stats.setMaxMp(maxmp);
		stats.setHp(maxhp);
		stats.setMp(maxmp);
		client.write(ChannelPackets.updatePlayerStats(statup, getJobId()));
		map.broadcastMessage(this, ChannelPackets.showForeignEffect(getId(), 0), false);
		stats.recalcLocalStats();
		silentPartyUpdate();
		updateLevel();
		NpcScriptManager.getInstance().start(getClient(), 9105010); // Vavaan
	}

	public void changeKeybinding(final int key, final KeyBinding keybinding) {
		if (keybinding.getType() != 0) {
			keylayout.Layout().put(Integer.valueOf(key), keybinding);
		} else {
			keylayout.Layout().remove(Integer.valueOf(key));
		}
	}

	public void sendMacros() {
		for (int i = 0; i < 5; i++) {
			if (skillMacros[i] != null) {
				client.write(ChannelPackets.getMacros(skillMacros));
				break;
			}
		}
	}

	public void updateMacros(final int position, final SkillMacro updateMacro) {
		skillMacros[position] = updateMacro;
	}

	public final SkillMacro[] getMacros() {
		return skillMacros;
	}

	public void temporaryBan(final String reason, final Calendar duration, final int tempBanReason) {
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
			ps.setString(1, client.getSessionIP());
			ps.execute();
			ps.close();

			client.disconnect(true);

			ps = con.prepareStatement("UPDATE accounts SET tempban = ?, tempban_reason = ? WHERE id = ?");
			final Timestamp timestamp = new Timestamp(duration.getTimeInMillis());
			ps.setTimestamp(1, timestamp);
			ps.setString(2, reason);
			ps.setInt(3, tempBanReason);
			ps.setInt(4, accountId);
			ps.execute();
			ps.close();
		} catch (final SQLException ex) {
			System.err.println("Error while tempbanning" + ex);
		}

	}

	public final boolean ban(final String banReason, final boolean isAutoban) {
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?")) {
			ps.setInt(1, isAutoban ? 2 : 1);
			ps.setString(2, banReason);
			ps.setInt(3, accountId);
			ps.execute();
		} catch (final SQLException ex) {
			System.err.println("Error while banning" + ex);
			return false;
		}
		return true;
	}

	@Override
	public int getObjectId() {
		return getId();
	}

	@Override
	public void setObjectId(final int id) {
		throw new UnsupportedOperationException();
	}

	public Storage getStorage() {
		return storage;
	}

	public void addVisibleMapObject(final GameMapObject mo) {
		visibleMapObjects.add(mo);
	}

	public void removeVisibleMapObject(final GameMapObject mo) {
		visibleMapObjects.remove(mo);
	}

	public boolean isMapObjectVisible(final GameMapObject mo) {
		return visibleMapObjects.contains(mo);
	}

	public Collection<GameMapObject> getVisibleMapObjects() {
		return Collections.unmodifiableCollection(visibleMapObjects);
	}

	public boolean isAlive() {
		return stats.getHp() > 0;
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
		client.write(ChannelPackets.removePlayerFromMap(this.getObjectId()));
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
		if (!isHidden()) {
			client.write(ChannelPackets.spawnPlayerMapObject(this));

			for (final Pet pet : pets) {
				if (pet.isSummoned()) {
					client.write(PetPacket.showPet(this, pet));
				}
			}

			if (dragon != null) {
				client.write(ChannelPackets.spawnDragon(dragon));
			}
		}
	}

	public void setDragon(final Dragon d) {
		this.dragon = d;
	}

	public final void equipChanged() {
		map.broadcastMessage(this, ChannelPackets.updateCharLook(this), false);
		stats.recalcLocalStats();
		enforceMaxHpMp();

		final ChannelCharacter player = client.getPlayer();
		if (player.getMessenger() != null) {
			final WorldChannelInterface wci = ChannelServer.getWorldInterface();
			try {
				wci.updateMessenger(player.getMessenger().getId(), player.getName(), client.getChannelId());
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}
	}

	public final Pet getPet(final int index) {
		byte count = 0;
		for (final Pet pet : pets) {
			if (pet.isSummoned()) {
				if (count == index) {
					return pet;
				}
				count++;
			}
		}
		return null;
	}

	public void addPet(final Pet pet) {
		pets.remove(pet);
		pets.add(pet);
		// So that the pet will be at the last
		// Pet index logic :(
	}

	public void removePet(final Pet pet, final boolean shiftLeft) {
		pet.setSummoned(false);
		/*
		 * int slot = -1; for (int i = 0; i < 3; i++) { if (pets[i] != null) {
		 * if (pets[i].getUniqueId() == pet.getUniqueId()) { pets[i] = null;
		 * slot = i; break; } } } if (shiftLeft) { if (slot > -1) { for (int i =
		 * slot; i < 3; i++) { if (i != 2) { pets[i] = pets[i + 1]; } else {
		 * pets[i] = null; } } } }
		 */
	}

	public final byte getPetIndex(final Pet petz) {
		byte count = 0;
		for (final Pet pet : pets) {
			if (pet.isSummoned()) {
				if (pet == petz) {
					return count;
				}
				count++;
			}
		}
		return -1;
	}

	public final byte getPetIndex(final int petId) {
		byte count = 0;
		for (final Pet pet : pets) {
			if (pet.isSummoned()) {
				if (pet.getUniqueId() == petId) {
					return count;
				}
				count++;
			}
		}
		return -1;
	}

	public final List<Pet> getPets() {
		return pets;
	}

	public final void unequipAllPets() {
		for (final Pet pet : pets) {
			if (pet != null) {
				unequipPet(pet, true, false);
			}
		}
	}

	public void unequipPet(final Pet pet, final boolean shiftLeft, final boolean hunger) {
		cancelFullnessSchedule(getPetIndex(pet));
		pet.saveToDb();

		map.broadcastMessage(this, PetPacket.removePet(this, pet, hunger), true);
		final List<StatValue> petStat = Lists.newArrayList();
		petStat.add(new StatValue(Stat.PET, Integer.valueOf(0)));
		client.write(PetPacket.petStatUpdate(this));
		client.write(ChannelPackets.enableActions());
		removePet(pet, shiftLeft);
	}

	/*
	 * public void shiftPetsRight() { if (pets[2] == null) { pets[2] = pets[1];
	 * pets[1] = pets[0]; pets[0] = null; } }
	 */
	public final long getLastFameTime() {
		return lastFameTime;
	}

	public final void setLastFameTime(final long timestamp) {
		this.lastFameTime = timestamp;
	}

	public final boolean hasFamedToday() {
		final long day = (long) Math.floor(lastFameTime / 86400000.0);
		final long today = (long) Math.floor(System.currentTimeMillis() / 86400000.0);
		return day < today;
	}

	public final KeyLayout getKeyLayout() {
		return this.keylayout;
	}

	public boolean hasParty() {
		return partyMember != null;
	}

	public PartyMember getPartyMembership() {
		return partyMember;
	}

	public PartyMember setPartyMembership(final int partyId) {
		this.partyMember = new PartyMember(partyId, this);
		return partyMember;
	}

	public void removePartyMembership() {
		this.partyMember = null;
	}

	public void setWorld(final int world) {
		this.worldId = world;
	}

	public void setParty(final Party party) {
		this.party = party;
	}

	public Trade getTrade() {
		return trade;
	}

	public void setTrade(final Trade trade) {
		this.trade = trade;
	}

	public EventInstanceManager getEventInstance() {
		return eventInstance;
	}

	public void setEventInstance(final EventInstanceManager eventInstance) {
		this.eventInstance = eventInstance;
	}

	public void addDoor(final Door door) {
		doors.add(door);
	}

	public void clearDoors() {
		doors.clear();
	}

	public ImmutableList<Door> getDoors() {
		return ImmutableList.copyOf(doors);
	}

	public void setSmega() {
		if (smega) {
			smega = false;
			sendNotice(5, "You have set megaphone to disabled mode");
		} else {
			smega = true;
			sendNotice(5, "You have set megaphone to enabled mode");
		}
	}

	public boolean getSmega() {
		return smega;
	}

	public boolean canDoor() {
		return canDoor;
	}

	public void disableDoor() {
		canDoor = false;
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				canDoor = true;
			}
		}, 5000);
	}

	public Map<Integer, Summon> getSummons() {
		return summons;
	}

	public int getChair() {
		return chair;
	}

	public int getItemEffect() {
		return itemEffect;
	}

	public void setChair(final int chair) {
		this.chair = chair;
		stats.relocHeal();
	}

	public void setItemEffect(final int itemEffect) {
		this.itemEffect = itemEffect;
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.PLAYER;
	}

	public int getGuildId() {
		return guildId;
	}

	public MemberRank getGuildRank() {
		return guildRank;
	}

	public void setGuildId(final int _id) {
		guildId = _id;
		if (guildId > 0) {
			if (guildMember == null) {
				guildMember = new GuildMember(this);
			} else {
				guildMember.setGuildId(guildId);
			}
		} else {
			guildMember = null;
		}
	}

	public void setGuildRank(final MemberRank newRank) {
		guildRank = newRank;
		if (guildMember != null) {
			guildMember.setGuildRank(newRank);
		}
	}

	public GuildMember getGuildMembership() {
		return guildMember;
	}

	public Guild getGuild() {
		try {
			return ChannelServer.getWorldInterface().getGuild(getGuildId());
		} catch (final RemoteException e) {
			ChannelServer.pingWorld();
		}
		return null;
	}

	private void updateJob() {
		final WorldChannelInterface world = ChannelServer.getWorldInterface();
		if (guildMember == null) {
			return;
		} else {
			try {
				world.updateGuildMemberJob(guildId, hairId, jobId);
			} catch (final RemoteException ex) {
				System.err.println("Could not update level: " + ex);
			}
		}
	}

	private void updateLevel() {
		final WorldChannelInterface world = ChannelServer.getWorldInterface();
		if (guildMember == null) {
			return;
		} else {
			try {
				world.updateGuildMemberLevel(guildId, hairId, level);
			} catch (final RemoteException ex) {
				System.err.println("Could not update level: " + ex);
			}
		}
		// TODO: more stuff here.
	}

	public void setReborns(final int reborns) {
		this.reborns = reborns;
	}

	public void saveGuildStatus() {
		final Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?")) {
			ps.setInt(1, guildId);
			ps.setInt(2, guildRank.asNumber());
			ps.setInt(3, id);
			ps.execute();
		} catch (final SQLException se) {
			System.err.println("SQL error: " + se.getLocalizedMessage() + se);
		}
	}

	public void modifyCSPoints(final int type, final int quantity, final boolean show) {
		if (getNX() < 0) {
			aCash = 0;
		}
		if (getNX() > 1000000) {
			aCash = 900000;
		}
		if (getNX() + quantity < 900000) {
			switch (type) {
			case 1:
				aCash += quantity;
				break;
			case 2:
				maplePoints += quantity;
				break;
			default:
				break;
			}
			if (show) {
				sendNotice(5, "You have gained " + quantity + " cash.");
				client.write(ChannelPackets.showSpecialEffect(19));
			}
		} else {
			sendNotice(5, "You have reached the maximum ammount of @cash");
		}
	}

	public int getCSPoints(final int type) {
		switch (type) {
		case 1:
			return aCash;
		case 2:
			return maplePoints;
		default:
			return 0;
		}
	}

	public final boolean haveItem(final int itemId, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
		int count = getInventoryForItem(itemId).countById(itemId);
		if (checkEquipped) {
			count += inventory.get(InventoryType.EQUIPPED).countById(itemId);
		}
		if (greaterOrEquals) {
			return count >= quantity;
		} else {
			return count == quantity;
		}
	}

	public void setLevel(final int level) {
		this.level = (short) level;
	}

	public int getSkillLevel(final int skill) {
		final SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
		if (ret == null) {
			return 0;
		}
		return ret.getCurrentLevel();
	}

	public void forfeitQuest(final int questId) {
		this.getQuestStatus(questId).forfeit();
		this.updateQuest(questId);
	}

	public void completeQuest(final int questId, final int npcId) {
		this.getQuestStatus(questId).complete(npcId);
		this.updateQuest(questId);
	}

	public void startQuest(final int questId, final int npcId) {
		this.getQuestStatus(questId).start(npcId, "");
		this.updateQuest(questId);
	}

	public static enum FameStatus {

		OK, NOT_TODAY, NOT_THIS_MONTH
	}

	public int getBuddyCapacity() {
		return buddies.getCapacity();
	}

	public void setBuddyCapacity(final int capacity) {
		buddies.setCapacity(capacity);
		client.write(ChannelPackets.updateBuddyCapacity(capacity));
	}

	public Messenger getMessenger() {
		return messenger;
	}

	public void setMessenger(final Messenger messenger) {
		this.messenger = messenger;
	}

	public int getMessengerPosition() {
		return messengerPosition;
	}

	public void setMessengerPosition(final int position) {
		this.messengerPosition = position;
	}

	public void addCooldown(final int skillId, final long startTime, final long length, final ScheduledFuture<?> timer) {
		cooldowns.put(Integer.valueOf(skillId), new CooldownValueHolder(skillId, startTime, length, timer));
	}

	public void removeCooldown(final int skillId) {
		if (cooldowns.containsKey(Integer.valueOf(skillId))) {
			cooldowns.remove(Integer.valueOf(skillId));
		}
	}

	public boolean isInCooldown(final int skillId) {
		return cooldowns.containsKey(Integer.valueOf(skillId));
	}

	public void giveCooldowns(final int skillid, final long starttime, final long length) {
		final int time = (int) ((length + starttime) - System.currentTimeMillis());
		final ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time);
		addCooldown(skillid, System.currentTimeMillis(), time, timer);
	}

	public void giveCooldowns(final Collection<PlayerCooldownValueHolder> cooldowns) {
		int time;
		if (cooldowns != null) {
			for (final PlayerCooldownValueHolder cooldown : cooldowns) {
				time = (int) ((cooldown.length + cooldown.startTime) - System.currentTimeMillis());
				final ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, cooldown.skillId), time);
				addCooldown(cooldown.skillId, System.currentTimeMillis(), time, timer);
			}
		} else {
			final Connection con = Database.getConnection();
			try (	PreparedStatement ps = getSelectCooldowns(con);
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final long length = rs.getLong("length");
					final long startTime = rs.getLong("StartTime");
					if (length + startTime - System.currentTimeMillis() <= 0) {
						continue;
					}

					final int skillId = rs.getInt("SkillID");
					giveCooldowns(skillId, startTime, length);
				}
				deleteByCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");

			} catch (final SQLException e) {
				System.err.println("Error while retriving cooldown from SQL storage");
			}
		}
	}

	private PreparedStatement getSelectCooldowns(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
		ps.setInt(1, getId());
		return ps;
	}

	public List<PlayerCooldownValueHolder> getAllCooldowns() {
		final List<PlayerCooldownValueHolder> ret = new ArrayList<>();
		for (final CooldownValueHolder mcdvh : cooldowns.values()) {
			ret.add(new PlayerCooldownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
		}
		return ret;
	}

	public final List<PlayerDiseaseValueHolder> getAllDiseases() {
		final List<PlayerDiseaseValueHolder> ret = new ArrayList<>(5);

		DiseaseValueHolder vh;
		for (final Entry<Disease, DiseaseValueHolder> disease : diseases.entrySet()) {
			vh = disease.getValue();
			ret.add(new PlayerDiseaseValueHolder(disease.getKey(), vh.startTime, vh.length));
		}
		return ret;
	}

	public final boolean hasDisease(final Disease disease) {
		for (final Disease current : diseases.keySet()) {
			if (current == disease) {
				return true;
			}
		}
		return false;
	}

	public void giveDebuff(final Disease disease, final MobSkill skill) {
		final List<DiseaseValue> debuff = Lists.newArrayList(new DiseaseValue(disease, skill.getX()));

		if (!hasDisease(disease) && diseases.size() < 2) {
			if (!(disease == Disease.SEDUCE || disease == Disease.STUN)) {
				if (isActiveBuffedValue(2321005)) {
					return;
				}
			}
			TimerManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					dispelDebuff(disease);
				}
			}, skill.getDuration());

			diseases.put(disease, new DiseaseValueHolder(System.currentTimeMillis(), skill.getDuration()));
			client.write(ChannelPackets.giveDebuff(debuff, skill));
			map.broadcastMessage(this, ChannelPackets.giveForeignDebuff(id, debuff, skill), false);
		}
	}

	public final void giveSilentDebuff(final Collection<PlayerDiseaseValueHolder> ld) {
		if (ld != null) {
			for (final PlayerDiseaseValueHolder disease : ld) {

				TimerManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						dispelDebuff(disease.disease);
					}
				}, (disease.length + disease.startTime) - System.currentTimeMillis());

				diseases.put(disease.disease, new DiseaseValueHolder(disease.startTime, disease.length));
			}
		}
	}

	public void dispelDebuff(final Disease debuff) {
		if (hasDisease(debuff)) {
			final long mask = debuff.getValue();
			client.write(ChannelPackets.cancelDebuff(mask));
			map.broadcastMessage(this, ChannelPackets.cancelForeignDebuff(id, mask), false);

			diseases.remove(debuff);
		}
	}

	public void dispelDebuffs() {
		dispelDebuff(Disease.CURSE);
		dispelDebuff(Disease.DARKNESS);
		dispelDebuff(Disease.POISON);
		dispelDebuff(Disease.SEAL);
		dispelDebuff(Disease.WEAKEN);
	}

	public void cancelAllDebuffs() {
		diseases.clear();
	}

	public void setLevel(final short level) {
		this.level = (short) (level - 1);
	}

	public void sendNote(final String recepientName, final String message) {
		Notes.send(name, recepientName, message);
	}

	public void showNote() {
		final List<Note> notes = Notes.loadReceived(name);
		if (notes.isEmpty()) {
			return;
		}

		MTSCSPacket.showNotes(notes);
	}

	public void deleteNote(final int noteId) {
		Notes.delete(noteId);
	}

	public void mulung_EnergyModify(final boolean inc) {
		if (inc) {
			if (mulung_energy + 100 > 10000) {
				mulung_energy = 10000;
			} else {
				mulung_energy += 100;
			}
		} else {
			mulung_energy = 0;
		}
		client.write(ChannelPackets.MulungEnergy(mulung_energy));
	}

	public void writeMulungEnergy() {
		client.write(ChannelPackets.MulungEnergy(mulung_energy));
	}

	public final short getCombo() {
		return combo;
	}

	public void setCombo(final short combo) {
		this.combo = combo;
	}

	public final long getLastCombo() {
		return lastCombo;
	}

	public void setLastCombo(final long combo) {
		this.lastCombo = combo;
	}

	public final long getKeyDownSkill_Time() {
		return keydown_skill;
	}

	public void setKeyDownSkill_Time(final long keydown_skill) {
		this.keydown_skill = keydown_skill;
	}

	public void checkBerserk() {
		if (BerserkSchedule != null) {
			BerserkSchedule.cancel(false);
			BerserkSchedule = null;
		}

		final ISkill berserkX = SkillFactory.getSkill(1320006);
		final int skilllevel = getCurrentSkillLevel(berserkX);
		if (skilllevel >= 1) {
			final StatEffect ampStat = berserkX.getEffect(skilllevel);

			if (stats.getHp() * 100 / stats.getMaxHp() > ampStat.getX()) {
				berserk = false;
			} else {
				berserk = true;
			}

			client.write(ChannelPackets.showOwnBerserk(skilllevel, berserk));
			map.broadcastMessage(this, ChannelPackets.showBerserk(getId(), skilllevel, berserk), false);

			BerserkSchedule = TimerManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					checkBerserk();
				}
			}, 10000);
		}
	}

	private void prepareBeholderEffect() {
		if (beholderHealingSchedule != null) {
			beholderHealingSchedule.cancel(false);
		}
		if (beholderBuffSchedule != null) {
			beholderBuffSchedule.cancel(false);
		}
		final ISkill bHealing = SkillFactory.getSkill(1320008);
		final int bHealingLvl = getCurrentSkillLevel(bHealing);
		if (bHealingLvl > 0) {
			final StatEffect healEffect = bHealing.getEffect(bHealingLvl);
			final int healInterval = healEffect.getX() * 1000;
			beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {

				@Override
				public void run() {
					addHP(healEffect.getHp());
					client.write(ChannelPackets.showOwnBuffEffect(1321007, 2));
					map.broadcastMessage(ChannelCharacter.this, ChannelPackets.summonSkill(getId(), 1321007, 5), true);
					map.broadcastMessage(ChannelCharacter.this, ChannelPackets.showBuffeffect(getId(), 1321007, 2), false);
				}
			}, healInterval, healInterval);
		}
		final ISkill bBuff = SkillFactory.getSkill(1320009);
		final int bBuffLvl = getCurrentSkillLevel(bBuff);
		if (bBuffLvl > 0) {
			final StatEffect buffEffect = bBuff.getEffect(bBuffLvl);
			final int buffInterval = buffEffect.getX() * 1000;
			beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {

				@Override
				public void run() {
					buffEffect.applyTo(ChannelCharacter.this);
					client.write(ChannelPackets.showOwnBuffEffect(1321007, 2));
					map.broadcastMessage(ChannelCharacter.this, ChannelPackets.summonSkill(getId(), 1321007, (int) (Math.random() * 3) + 6), true);
					map.broadcastMessage(ChannelCharacter.this, ChannelPackets.showBuffeffect(getId(), 1321007, 2), false);
				}
			}, buffInterval, buffInterval);
		}
	}

	public void setChalkboard(final String text) {
		this.chalktext = text;
		map.broadcastMessage(MTSCSPacket.useChalkboard(getId(), text));
	}

	public String getChalkboard() {
		return chalktext;
	}

	public Mount getMount() {
		return mount;
	}

	public int[] getWishlist() {
		return wishlist;
	}

	public void clearWishlist() {
		for (int i = 0; i < 10; i++) {
			wishlist[i] = 0;
		}
	}

	public int getWishlistSize() {
		int ret = 0;
		for (int i = 0; i < 10; i++) {
			if (wishlist[i] > 0) {
				ret++;
			}
		}
		return ret;
	}

	public int[] getRocks() {
		return teleportRocks;
	}

	public int getRockSize() {
		int ret = 0;
		for (int i = 0; i < 10; i++) {
			if (teleportRocks[i] > 0) {
				ret++;
			}
		}
		return ret;
	}

	public void deleteFromRocks(final int map) {
		for (int i = 0; i < 10; i++) {
			if (teleportRocks[i] == map) {
				teleportRocks[i] = -1;
				break;
			}
		}
	}

	public void addRockMap() {
		if (getRockSize() >= 10) {
			return;
		}
		teleportRocks[getRockSize()] = getMapId();
	}

	public boolean isRockMap(final int id) {
		for (int i = 0; i < 10; i++) {
			if (teleportRocks[i] == id) {
				return true;
			}
		}
		return false;
	}

	public List<LifeMovementFragment> getLastRes() {
		return lastMovement;
	}

	public void setLastRes(final List<LifeMovementFragment> lastres) {
		this.lastMovement = lastres;
	}

	public void setMonsterBookCover(final int bookCover) {
		this.bookCover = bookCover;
	}

	public int getMonsterBookCover() {
		return bookCover;
	}

	public void sendNotice(final int type, final String message) {
		client.write(ChannelPackets.serverNotice(type, message));
	}

	public PlayerShop getPlayerShop() {
		return playerShop;
	}

	public void setPlayerShop(final PlayerShop playerShop) {
		this.playerShop = playerShop;
	}

	public int getConversationState() {
		return conversationState.get();
	}

	public void setConversationState(final int state) {
		this.conversationState.set(state);
	}

	public CarnivalParty getCarnivalParty() {
		return carnivalParty;
	}

	public void setCarnivalParty(final CarnivalParty party) {
		carnivalParty = party;
	}

	public void addCP(final int ammount) {
		totalCP += ammount;
		availableCP += ammount;
	}

	public void useCP(final int ammount) {
		availableCP -= ammount;
	}

	public int getAvailableCP() {
		return availableCP;
	}

	public int getTotalCP() {
		return totalCP;
	}

	public void resetCP() {
		totalCP = 0;
		availableCP = 0;
	}

	public void addCarnivalRequest(final CarnivalChallenge request) {
		pendingCarnivalRequests.add(request);
	}

	public final CarnivalChallenge getNextCarnivalRequest() {
		return pendingCarnivalRequests.pollLast();
	}

	public void clearCarnivalRequests() {
		pendingCarnivalRequests = new LinkedList<>();
	}

	public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
		client.write(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
	}

	public void CPUpdate(final boolean party, final int available, final int total, final int team) {
		client.write(MonsterCarnivalPacket.CPUpdate(party, available, total, team));
	}

	public void playerDiedCPQ(final String name, final int lostCP, final int team) {
		client.write(MonsterCarnivalPacket.playerDiedMessage(name, lostCP, team));
	}

	@Override
	public int getSubcategory() {
		if (jobId >= 430 && jobId <= 434) {
			return 1; // dont set it
		}
		return subcategory;
	}

	public int getLinkedMonsterId() {
		return linkedMonsterId;
	}

	public void setLinkedMonsterId(final int lm) {
		this.linkedMonsterId = lm;
	}

	public boolean isOnDMG() {
		return ondmg;
	}

	public boolean isCallGM() {
		return callgm;
	}

	public void setCallGM(final boolean b) {
		this.callgm = b;
	}

	public void setOnDMG(final boolean b) {
		this.ondmg = b;
	}

	public Party getParty() {
		final PartyMember member = this.getPartyMembership();

		if (member != null) {
			try {
				return ChannelServer.getWorldInterface().getParty(member.getPartyId());
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}

		return null;
	}
}