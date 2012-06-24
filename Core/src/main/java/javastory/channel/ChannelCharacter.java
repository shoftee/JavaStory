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

	private transient ChannelClient client;

	private transient PlayerRandomStream randomStream;

	private ChannelCharacter() {
		this.setStance(0);
		this.setPosition(new Point(0, 0));
		for (int i = 0; i < this.remainingSp.length; i++) {
			this.remainingSp[i] = 0;
		}

		this.lastCombo = 0;
		this.mulung_energy = 0;
		this.combo = 0;
		this.keydown_skill = 0;
		this.messengerPosition = 4;
		this.canDoor = true;
		this.berserk = false;
		this.smega = true;
		this.wishlist = new int[10];
		this.teleportRocks = new int[10];

		// 1 = NPC/Quest,
		// 2 = Duey,
		// 3 = Hired Merch store,
		// 4 = Storage
		this.conversationState = new AtomicInteger();

		this.keylayout = new KeyLayout();
		this.inventory = new MultiInventory();
		this.stats = new ActivePlayerStats(this);
		this.cheatTracker = new CheatTracker(this);

		this.cooldowns = Maps.newLinkedHashMap();
		this.effects = Maps.newEnumMap(BuffStat.class);
		this.diseases = Maps.newEnumMap(Disease.class);

		this.doors = Lists.newArrayList();

		this.pets = Lists.newArrayList();

		this.pendingCarnivalRequests = Lists.newLinkedList();

		this.controlledMonsters = Sets.newLinkedHashSet();
		this.visibleMapObjects = Sets.newLinkedHashSet();

		this.quests = Maps.newLinkedHashMap();
		this.questInfo = Maps.newLinkedHashMap();

		this.summons = Maps.newLinkedHashMap();

		this.skills = Maps.newLinkedHashMap();

		// TODO: Get rid of arrays.
		this.savedLocations = Maps.newEnumMap(SavedLocationType.class);
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
			final Item mount = this.getEquippedItemsInventory().getItem((byte) -22);
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

			this.saveCharacterData(con);

			for (final Pet pet : this.pets) {
				if (pet.isSummoned()) {
					// Only save those summoned :P
					pet.saveToDb();
				}
			}

			this.deleteByCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
			this.saveSkillMacros(con);

			this.deleteByCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
			this.saveInventorySlots(con);

			this.deleteByCharacterId(con, "DELETE FROM inventoryitems WHERE characterid = ?");
			this.saveInventoryItems(con);

			this.deleteByCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
			this.saveQuestInfo(con);

			this.deleteByCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
			this.saveQuestStatus(con);

			this.deleteByCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
			this.saveSkillInfo(con);

			if (forcedDc && this.getAllCooldowns().size() > 0) {
				this.saveSkillCooldowns(con);
			}

			this.deleteByCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
			this.saveSavedLocations(con);

			this.deleteByCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
			this.saveBuddyEntries(con);

			this.saveAccountInfo(con);

			if (this.storage != null) {
				this.storage.saveToDB();
			}

			this.keylayout.saveKeys(this.id);
			this.mount.saveMount(this.id);
			this.monsterBook.saveCards(this.id);

			this.deleteByCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
			this.saveWishlist(con);

			this.deleteByCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
			this.saveTeleportRockLocations(con);

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
		try (final PreparedStatement ps = this.getUpdateCharacterData(con)) {
			final int affectedRows = ps.executeUpdate();
			if (affectedRows < 1) {
				throw new DatabaseException("Character not in database (" + this.id + ")");
			}
		}
	}

	private PreparedStatement getUpdateCharacterData(final Connection con) throws SQLException {
		final String sql = "UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, monsterbookcover = ?, dojo_pts = ?, dojoRecord = ?, reborns = ?, subcategory = ? WHERE id = ?";
		final PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, this.level);
		ps.setInt(2, this.fame);
		ps.setInt(3, this.stats.getStr());
		ps.setInt(4, this.stats.getDex());
		ps.setInt(5, this.stats.getLuk());
		ps.setInt(6, this.stats.getInt());
		ps.setInt(7, this.exp);

		final int hp = this.stats.getHp();
		final int hpToSave = hp < 1 ? 50 : hp;
		ps.setInt(8, hpToSave);
		ps.setInt(9, this.stats.getMp());
		ps.setInt(10, this.stats.getMaxHp());
		ps.setInt(11, this.stats.getMaxMp());

		final String joinedSp = Joiner.on(',').join(Lists.newArrayList(this.remainingSp));
		ps.setString(12, joinedSp);

		ps.setInt(13, this.remainingAp);
		ps.setInt(14, this.gmLevel);
		ps.setInt(15, this.skinColorId);
		ps.setInt(16, this.gender.asNumber());
		ps.setInt(17, this.jobId);
		ps.setInt(18, this.hairId);
		ps.setInt(19, this.faceId);

		if (this.map.getForcedReturnId() != 999999999) {
			ps.setInt(20, this.map.getForcedReturnId());
		} else {
			ps.setInt(20, hp < 1 ? this.map.getReturnMapId() : this.map.getId());
		}

		ps.setInt(21, this.meso);
		ps.setInt(22, this.hpApUsed);
		ps.setInt(23, this.mpApUsed);

		final Portal closest = this.map.findClosestSpawnpoint(this.getPosition());
		ps.setInt(24, closest != null ? closest.getId() : 0);

		ps.setInt(25, this.party != null ? this.party.getId() : -1);
		ps.setInt(26, this.buddies.getCapacity());

		if (this.messenger != null) {
			ps.setInt(27, this.messenger.getId());
			ps.setInt(28, this.messengerPosition);
		} else {
			ps.setInt(27, 0);
			ps.setInt(28, 4);
		}

		ps.setInt(29, this.bookCover);
		ps.setInt(30, this.dojo);
		ps.setInt(31, this.dojoRecord);
		ps.setInt(32, this.getReborns());
		ps.setInt(33, this.subcategory);
		ps.setInt(34, this.id);
		return ps;
	}

	private void saveTeleportRockLocations(final Connection con) throws SQLException {
		final String sql = "INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ";
		try (final PreparedStatement ps = con.prepareStatement(sql)) {
			for (int i = 0; i < this.getRockSize(); i++) {
				if (this.teleportRocks[i] != 999999999) {
					ps.setInt(1, this.getId());
					ps.setInt(2, this.teleportRocks[i]);
					ps.execute();
				}
			}
		}
	}

	private void saveWishlist(final Connection con) throws SQLException {
		final String sql = "INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ";
		try (final PreparedStatement ps = con.prepareStatement(sql)) {
			for (int i = 0; i < this.getWishlistSize(); i++) {
				ps.setInt(1, this.getId());
				ps.setInt(2, this.wishlist[i]);
				ps.execute();
			}
		}
	}

	private void saveAccountInfo(final Connection con) throws SQLException {
		try (final PreparedStatement ps = this.getUpdateAccount(con)) {
			ps.execute();
		}
	}

	private PreparedStatement getUpdateAccount(final Connection connection) throws SQLException {
		final String sql = "UPDATE accounts SET `ACash` = ?, `mPoints` = ?, `vpoints` = ? WHERE id = ?";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.aCash);
		ps.setInt(2, this.maplePoints);
		ps.setInt(3, this.vpoints);
		ps.setInt(4, this.client.getAccountId());
		return ps;
	}

	private void saveBuddyEntries(final Connection connection) throws SQLException {
		try (final PreparedStatement ps = this.getInsertBuddyEntry(connection)) {
			for (final BuddyListEntry entry : this.buddies.getBuddies()) {
				if (entry.isVisible()) {
					ps.setInt(2, entry.getCharacterId());
					ps.execute();
				}
			}
		}
	}

	private PreparedStatement getInsertBuddyEntry(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 0)";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveSavedLocations(final Connection connection) throws SQLException {
		try (final PreparedStatement ps = this.getInsertSavedLocation(connection)) {
			for (final Map.Entry<SavedLocationType, Integer> entry : this.savedLocations.entrySet()) {
				ps.setInt(2, entry.getKey().asNumber());
				ps.setInt(3, entry.getValue());
				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertSavedLocation(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveSkillCooldowns(final Connection connection) throws SQLException {
		try (final PreparedStatement ps = this.getInsertSkillCooldown(connection)) {
			for (final PlayerCooldownValueHolder cooling : this.getAllCooldowns()) {
				ps.setInt(2, cooling.SkillId);
				ps.setLong(3, cooling.StartTime);
				ps.setLong(4, cooling.Length);
				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertSkillCooldown(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveSkillInfo(final Connection connection) throws SQLException {
		try (final PreparedStatement ps = this.getInsertSkillInfo(connection)) {
			for (final Entry<ISkill, SkillEntry> skill : this.skills.entrySet()) {

				final int skillId = skill.getKey().getId();
				ps.setInt(2, skillId);

				final SkillEntry entry = skill.getValue();
				ps.setInt(3, entry.getCurrentLevel());
				ps.setInt(4, entry.getMasterLevel());

				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertSkillInfo(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES (?, ?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveQuestStatus(final Connection connection) throws SQLException {
		try (	final PreparedStatement questStatement = this.getInsertQuestStatus(connection);
				final PreparedStatement mobStatement = this.getInsertQuestMobStatus(connection)) {
			for (final QuestStatus quest : this.quests.values()) {
				this.setQuestStatusData(questStatement, quest);
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

	private PreparedStatement getInsertQuestMobStatus(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)";
		return connection.prepareStatement(sql);
	}

	private PreparedStatement getInsertQuestStatus(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveQuestInfo(final Connection connection) throws SQLException {
		try (final PreparedStatement ps = this.getInsertQuestInfo(connection)) {
			for (final Entry<Integer, String> q : this.questInfo.entrySet()) {
				ps.setInt(2, q.getKey());
				ps.setString(3, q.getValue());
				ps.execute();
			}
		}
	}

	private PreparedStatement getInsertQuestInfo(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO questinfo (`characterid`, `quest`, `data`) VALUES (?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveInventoryItems(final Connection connection) throws SQLException {
		try (final PreparedStatement items = this.getInsertInventoryItem(connection)) {

			this.saveItemInventory(items, this.getUseInventory());
			this.saveItemInventory(items, this.getSetupInventory());
			this.saveItemInventory(items, this.getEtcInventory());
			this.saveItemInventory(items, this.getCashInventory());

			try (final PreparedStatement equips = this.getInsertInventoryEquip(connection)) {
				this.saveEquipInventory(items, equips, this.getEquipInventory());
				this.saveEquipInventory(items, equips, this.getEquippedItemsInventory());
			}
		}
	}

	private void saveItemInventory(final PreparedStatement statement, final Inventory inventory) throws SQLException {
		statement.setInt(2, inventory.getType().asNumber());
		for (final Item item : inventory) {
			this.setItemData(statement, item);
			statement.executeUpdate();
		}
	}

	private void setItemData(final PreparedStatement statement, final Item item) throws SQLException {
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
		for (final Item item : inventory) {
			this.setItemData(itemStatement, item);
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
				this.setEquipData(equipStatement, (Equip) item);
				equipStatement.executeUpdate();
			}
		}
	}

	private void setEquipData(final PreparedStatement statement, final Equip equip) throws SQLException {
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

	private PreparedStatement getInsertInventoryEquip(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		return connection.prepareStatement(sql);
	}

	private PreparedStatement getInsertInventoryItem(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO inventoryitems (characterid, inventorytype, itemid, position, quantity, owner, GM_Log, petid, expiredate, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, this.id);
		return ps;
	}

	private void saveInventorySlots(final Connection connection) throws SQLException {
		try (PreparedStatement ps = this.getInsertInventorySlots(connection)) {
			ps.execute();
		}
	}

	private PreparedStatement getInsertInventorySlots(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)";
		final PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.id);
		ps.setInt(2, this.getEquipInventory().getSlotLimit());
		ps.setInt(3, this.getUseInventory().getSlotLimit());
		ps.setInt(4, this.getSetupInventory().getSlotLimit());
		ps.setInt(5, this.getEtcInventory().getSlotLimit());
		ps.setInt(6, this.getCashInventory().getSlotLimit());
		return ps;
	}

	private void saveSkillMacros(final Connection connection) throws SQLException {
		try (final PreparedStatement ps = getInsertSkillMacros(connection)) {
			for (int i = 0; i < 5; i++) {
				final SkillMacro macro = this.skillMacros[i];
				if (macro == null) {
					continue;
				}

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

	private PreparedStatement getInsertSkillMacros(final Connection connection) throws SQLException {
		final String sql = "INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setInt(1, this.id);
		return ps;
	}

	private void deleteByCharacterId(final Connection connection, final String sql) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, this.id);
			ps.executeUpdate();
		}
	}

	@Override
	public final ActivePlayerStats getStats() {
		return this.stats;
	}

	public final PlayerRandomStream getRandomStream() {
		return this.randomStream;
	}

	public final void writeQuestInfoPacket(final PacketBuilder builder) {
		builder.writeAsShort(this.questInfo.size());

		for (final Entry<Integer, String> q : this.questInfo.entrySet()) {
			builder.writeAsShort(q.getKey());
			builder.writeLengthPrefixedString(q.getValue() == null ? "" : q.getValue());
		}
		builder.writeInt(0); // PQ rank and stuff
	}

	public final void updateInfoQuest(final int questid, final String data) {
		this.questInfo.put(questid, data);
		this.client.write(ChannelPackets.updateInfoQuest(questid, data));
	}

	public final String getInfoQuest(final int questid) {
		if (this.questInfo.containsKey(questid)) {
			return this.questInfo.get(questid);
		}
		return "";
	}

	public final int getNumQuest() {
		int i = 0;
		for (final QuestStatus q : this.quests.values()) {
			if (q.getState() == 2) {
				i++;
			}
		}
		return i;
	}

	public final byte getQuestCompletionStatus(final int questId) {
		for (final QuestStatus q : this.quests.values()) {
			if (q.getQuestId() == questId) {
				return q.getState();
			}
		}
		return 0;
	}

	public final QuestStatus getQuestStatus(final int questId) {
		QuestStatus status = this.quests.get(questId);
		if (status == null) {
			status = new QuestStatus(questId, (byte) 0);
			this.quests.put(questId, status);
		}
		return status;
	}

	public final QuestStatus getAddQuestStatus(final int questId) {
		if (!this.quests.containsKey(questId)) {
			final QuestStatus status = new QuestStatus(questId, (byte) 0);
			this.quests.put(questId, status);
			return status;
		}
		return this.quests.get(questId);
	}

	public final void updateQuest(final int questId) {
		final QuestStatus status = this.getQuestStatus(questId);
		switch (status.getState()) {
		case 0:
			this.client.write(ChannelPackets.forfeitQuest(this, questId));
			break;
		case 1:
			this.client.write(ChannelPackets.startQuest(this, questId, status.getCustomData()));
			this.client.write(ChannelPackets.updateQuestInfo(this, questId, status.getNpc(), (byte) 8));
			break;
		case 2:
			this.client.write(ChannelPackets.completeQuest(questId));
			break;
		}
	}

	public final Map<Integer, String> getQuestInfoMap() {
		return this.questInfo;
	}

	public final Map<Integer, QuestStatus> getQuestStatusMap() {
		return this.quests;
	}

	public boolean isActiveBuffedValue(final int skillid) {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(this.effects.values());
		for (final BuffStatValueHolder value : allBuffs) {
			if (value.effect.isSkill() && value.effect.getSourceId() == skillid) {
				return true;
			}
		}
		return false;
	}

	public Integer getBuffedValue(final BuffStat effect) {
		final BuffStatValueHolder value = this.effects.get(effect);
		return value == null ? null : Integer.valueOf(value.value);
	}

	public final Integer getBuffedSkill_X(final BuffStat effect) {
		final BuffStatValueHolder value = this.effects.get(effect);
		if (value == null) {
			return null;
		}
		return value.effect.getX();
	}

	public final Integer getBuffedSkill_Y(final BuffStat effect) {
		final BuffStatValueHolder value = this.effects.get(effect);
		if (value == null) {
			return null;
		}
		return value.effect.getY();
	}

	public final StatEffect getBuffedSkillEffect(final BuffStat effect) {
		final BuffStatValueHolder value = this.effects.get(effect);
		if (value == null) {
			return null;
		}
		return value.effect;
	}

	public boolean isBuffFrom(final BuffStat stat, final ISkill skill) {
		final BuffStatValueHolder value = this.effects.get(stat);
		if (value == null) {
			return false;
		}
		return value.effect.isSkill() && value.effect.getSourceId() == skill.getId();
	}

	public int getBuffSource(final BuffStat stat) {
		final BuffStatValueHolder value = this.effects.get(stat);
		return value == null ? -1 : value.effect.getSourceId();
	}

	public int getItemQuantity(final int itemId, final boolean checkEquipped) {
		int count = this.inventory.get(GameConstants.getInventoryType(itemId)).countById(itemId);
		if (checkEquipped) {
			count += this.inventory.get(InventoryType.EQUIPPED).countById(itemId);
		}
		return count;
	}

	public int getReborns() {
		return this.reborns;
	}

	public int getVPoints() {
		return this.vpoints;
	}

	public int getSnipeDamage() {
		return Math.min(damageCap, 500000 * this.getReborns() + 500000);
	}

	public int getMaxStats() {
		return this.getJobId() > 999 && this.getJobId() < 2000 ? 15000 : 32000;

	}

	public int getNX() {
		return this.aCash;
	}

	public void gainVPoints(final int gainedpoints) {
		this.vpoints += gainedpoints;
	}

	@Override
	public int getWorldId() {
		return this.worldId;
	}

	public void setBuffedValue(final BuffStat effect, final int statValue) {
		final BuffStatValueHolder value = this.effects.get(effect);
		if (value == null) {
			return;
		}
		value.value = statValue;
	}

	public Long getBuffedStartTime(final BuffStat effect) {
		final BuffStatValueHolder value = this.effects.get(effect);
		return value == null ? null : Long.valueOf(value.startTime);
	}

	public StatEffect getStatForBuff(final BuffStat effect) {
		final BuffStatValueHolder value = this.effects.get(effect);
		return value == null ? null : value.effect;
	}

	private void prepareDragonBlood(final StatEffect bloodEffect) {
		if (this.dragonBloodSchedule != null) {
			this.dragonBloodSchedule.cancel(false);
		}
		this.dragonBloodSchedule = TimerManager.getInstance().register(new DragonBloodRunnable(bloodEffect), 4000, 4000);

	}

	private final class DragonBloodRunnable implements Runnable {

		private final StatEffect effect;

		public DragonBloodRunnable(final StatEffect effect) {
			this.effect = effect;
		}

		@Override
		public void run() {
			if (ChannelCharacter.this.stats.getHp() - this.effect.getX() > 1) {
				ChannelCharacter.this.cancelBuffStats(BuffStat.DRAGONBLOOD);
			} else {
				ChannelCharacter.this.addHP(-this.effect.getX());
				final int bloodEffectSourceId = this.effect.getSourceId();
				final GamePacket ownEffectPacket = ChannelPackets.showOwnBuffEffect(bloodEffectSourceId, 5);
				ChannelCharacter.this.client.write(ownEffectPacket);
				final GamePacket otherEffectPacket = ChannelPackets.showBuffeffect(ChannelCharacter.this.getId(), bloodEffectSourceId, 5);
				ChannelCharacter.this.map.broadcastMessage(ChannelCharacter.this, otherEffectPacket, false);
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
					ChannelCharacter.this.unequipPet(pet, true, true);
				} else {
					pet.setFullness(newFullness);
					ChannelCharacter.this.client.write(PetPacket.updatePet(pet, true));
				}
			}
		}, 60000, 60000);
		switch (petSlot) {
		case 0:
			this.fullnessSchedule = schedule;
			break;
		case 1:
			this.fullnessSchedule_1 = schedule;
			break;
		case 2:
			this.fullnessSchedule_2 = schedule;
			break;
		}
	}

	public void cancelFullnessSchedule(final int petSlot) {
		switch (petSlot) {
		case 0:
			if (this.fullnessSchedule != null) {
				this.fullnessSchedule.cancel(false);
			}
			break;
		case 1:
			if (this.fullnessSchedule_1 != null) {
				this.fullnessSchedule_1.cancel(false);
			}
			break;
		case 2:
			if (this.fullnessSchedule_2 != null) {
				this.fullnessSchedule_2.cancel(false);
			}
			break;
		}
	}

	public void startMapTimeLimitTask(int time, final GameMap to) {
		this.client.write(ChannelPackets.getClock(time));

		time *= 1000;
		this.mapTimeLimitTask = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				ChannelCharacter.this.changeMap(to, to.getPortal(0));
			}
		}, time, time);
	}

	public void startFishingTask(final boolean VIP) {
		final int time = VIP ? 30000 : 60000;
		this.cancelFishingTask();

		this.fishing = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				if (!ChannelCharacter.this.haveItem(2300000, 1, false, true)) {
					ChannelCharacter.this.cancelFishingTask();
					return;
				}
				InventoryManipulator.removeById(ChannelCharacter.this.client, ChannelCharacter.this.getUseInventory(), 2300000, 1, false, false);

				final int randval = RandomRewards.getInstance().getFishingReward();

				switch (randval) {
				case 0: // Meso
					final int money = Randomizer.rand(10, 50000);
					ChannelCharacter.this.gainMeso(money, true);
					ChannelCharacter.this.client.write(UIPacket.fishingUpdate((byte) 1, money));
					break;
				case 1: // EXP
					final int experi = Randomizer.nextInt(GameConstants.getExpNeededForLevel(ChannelCharacter.this.level) / 200);
					ChannelCharacter.this.gainExp(experi, true, false, true);
					ChannelCharacter.this.client.write(UIPacket.fishingUpdate((byte) 2, experi));
					break;
				default:
					InventoryManipulator.addById(ChannelCharacter.this.client, randval, (short) 1);
					ChannelCharacter.this.client.write(UIPacket.fishingUpdate((byte) 0, randval));
					break;
				}
				ChannelCharacter.this.map.broadcastMessage(UIPacket.fishingCaught(ChannelCharacter.this.id));
			}
		}, time, time);
	}

	public void cancelMapTimeLimitTask() {
		if (this.mapTimeLimitTask != null) {
			this.mapTimeLimitTask.cancel(false);
		}
	}

	public void cancelFishingTask() {
		if (this.fishing != null) {
			this.fishing.cancel(false);
		}
	}

	public void registerEffect(final StatEffect effect, final long starttime, final ScheduledFuture<?> schedule) {
		if (effect.isHide()) {
			this.hidden = true;
			this.map.broadcastMessage(this, ChannelPackets.removePlayerFromMap(this.getId()), false);
		} else if (effect.isDragonBlood()) {
			this.prepareDragonBlood(effect);
		} else if (effect.isBerserk()) {
			this.checkBerserk();
		} else if (effect.isBeholder()) {
			this.prepareBeholderEffect();
		}
		for (final BuffStatValue statup : effect.getStatups()) {
			this.effects.put(statup.stat, new BuffStatValueHolder(effect, starttime, schedule, statup.value));
		}
		this.stats.recalcLocalStats();
	}

	public List<BuffStat> getBuffStats(final StatEffect effect, final long startTime) {
		final List<BuffStat> bstats = Lists.newArrayList();

		for (final Entry<BuffStat, BuffStatValueHolder> stateffect : this.effects.entrySet()) {
			final BuffStatValueHolder value = stateffect.getValue();
			if (value.effect.sameSource(effect) && (startTime == -1 || startTime == value.startTime)) {
				bstats.add(stateffect.getKey());
			}
		}
		return bstats;
	}

	private void deregisterBuffStats(final List<BuffStat> stats) {
		final List<BuffStatValueHolder> effectsToCancel = Lists.newArrayListWithCapacity(stats.size());
		for (final BuffStat stat : stats) {
			final BuffStatValueHolder value = this.effects.get(stat);
			if (value != null) {
				this.effects.remove(stat);
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
					final Summon summon = this.summons.get(summonId);
					if (summon != null) {
						this.map.broadcastMessage(ChannelPackets.removeSummon(summon, true));
						this.map.removeMapObject(summon);
						this.removeVisibleMapObject(summon);
						this.summons.remove(summonId);
						if (summon.getSkill() == 1321007) {
							if (this.beholderHealingSchedule != null) {
								this.beholderHealingSchedule.cancel(false);
								this.beholderHealingSchedule = null;
							}
							if (this.beholderBuffSchedule != null) {
								this.beholderBuffSchedule.cancel(false);
								this.beholderBuffSchedule = null;
							}
						}
					}
				} else if (stat == BuffStat.DRAGONBLOOD) {
					if (this.dragonBloodSchedule != null) {
						this.dragonBloodSchedule.cancel(false);
						this.dragonBloodSchedule = null;
					}
				}
			}
		}
		for (final BuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
			if (this.getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).isEmpty()) {
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
			buffstats = this.getBuffStats(effect, startTime);
		} else {
			final List<BuffStatValue> statups = effect.getStatups();
			buffstats = Lists.newArrayListWithCapacity(statups.size());
			for (final BuffStatValue statup : statups) {
				buffstats.add(statup.stat);
			}
		}
		this.deregisterBuffStats(buffstats);
		if (effect.isMagicDoor()) {
			// remove for all on maps
			if (!this.getDoors().isEmpty()) {
				final Door door = this.getDoors().iterator().next();
				for (final ChannelCharacter chr : door.getTarget().getCharacters()) {
					door.sendDestroyData(chr.getClient());
				}
				for (final ChannelCharacter chr : door.getTown().getCharacters()) {
					door.sendDestroyData(chr.getClient());
				}
				for (final Door destroyDoor : this.getDoors()) {
					door.getTarget().removeMapObject(destroyDoor);
					door.getTown().removeMapObject(destroyDoor);
				}
				this.clearDoors();
				this.silentPartyUpdate();
			}
		} else if (effect.isMonsterRiding()) {
			// if (effect.getSourceId() != 5221006) {
			// getMount().cancelSchedule();
			// }
		} else if (effect.isAranCombo()) {
			this.combo = 0;
		}
		// check if we are still logged in o.o
		if (!overwrite) {
			this.cancelPlayerBuffs(buffstats);
			if (effect.isHide() && (ChannelCharacter) this.map.getMapObject(this.getObjectId()) != null) {
				this.hidden = false;
				this.map.broadcastMessage(this, ChannelPackets.spawnPlayerMapObject(this), false);

				for (final Pet pet : this.pets) {
					if (pet.isSummoned()) {
						final GamePacket packet = PetPacket.showPet(this, pet);
						this.map.broadcastMessage(this, packet, false);
					}
				}
			}
		}
	}

	public void cancelBuffStats(final BuffStat stat) {
		final List<BuffStat> buffStatList = Arrays.asList(stat);
		this.deregisterBuffStats(buffStatList);
		this.cancelPlayerBuffs(buffStatList);
	}

	public void cancelEffectFromBuffStat(final BuffStat stat) {
		this.cancelEffect(this.effects.get(stat).effect, false, -1);
	}

	private void cancelPlayerBuffs(final List<BuffStat> buffstats) {
		if (ChannelServer.getPlayerStorage().getCharacterById(this.getId()) != null) {
			// are we still connected ?
			if (buffstats.contains(BuffStat.HOMING_BEACON)) {
				this.client.write(ChannelPackets.cancelHoming());
			} else {
				this.stats.recalcLocalStats();
				this.enforceMaxHpMp();
				this.client.write(ChannelPackets.cancelBuff(buffstats, buffstats.contains(BuffStat.MONSTER_RIDING)));
				this.map.broadcastMessage(this, ChannelPackets.cancelForeignBuff(this.getId(), buffstats), false);
			}
		}
		if (buffstats.contains(BuffStat.MONSTER_RIDING) && Jobs.isEvan(this.jobId) && this.jobId >= 2200) {
			this.makeDragon();
			this.map.spawnDragon(this.dragon);
		}
	}

	public void dispel() {
		if (!this.isHidden()) {
			final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(this.effects.values());
			for (final BuffStatValueHolder value : allBuffs) {
				if (value.effect.isSkill() && value.schedule != null && !value.effect.isMorph()) {
					this.cancelEffect(value.effect, false, value.startTime);
				}
			}
		}
	}

	public void dispelSkill(final int skillid) {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(this.effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			if (skillid == 0) {
				if (value.effect.isSkill()
					&& (value.effect.getSourceId() == 1004 || value.effect.getSourceId() == 10001004 || value.effect.getSourceId() == 20001004
						|| value.effect.getSourceId() == 20011004 || value.effect.getSourceId() == 1321007 || value.effect.getSourceId() == 2121005
						|| value.effect.getSourceId() == 2221005 || value.effect.getSourceId() == 2311006 || value.effect.getSourceId() == 2321003
						|| value.effect.getSourceId() == 3111002 || value.effect.getSourceId() == 3111005 || value.effect.getSourceId() == 3211002
						|| value.effect.getSourceId() == 3211005 || value.effect.getSourceId() == 4111002)) {
					this.cancelEffect(value.effect, false, value.startTime);
					break;
				}
			} else {
				if (value.effect.isSkill() && value.effect.getSourceId() == skillid) {
					this.cancelEffect(value.effect, false, value.startTime);
					break;
				}
			}
		}
	}

	public void clearAllBuffEffects() {
		this.effects.clear();
	}

	public void cancelAllBuffs() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(this.effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			this.cancelEffect(value.effect, false, value.startTime);
		}
	}

	public void cancelMorphs() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(this.effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			switch (value.effect.getSourceId()) {
			case 5111005:
			case 5121003:
			case 15111002:
			case 13111005:
				return; // Since we can't have more than 1, save up on loops
			default:
				if (value.effect.isMorph()) {
					this.cancelEffect(value.effect, false, value.startTime);
					continue;
				}
			}
		}
	}

	public int getMorphState() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(this.effects.values());
		for (final BuffStatValueHolder value : allBuffs) {
			if (value.effect.isMorph()) {
				return value.effect.getSourceId();
			}
		}
		return -1;
	}

	public void silentGiveBuffs(final Collection<PlayerBuffValueHolder> buffs) {
		for (final PlayerBuffValueHolder value : buffs) {
			value.Effect.silentApplyBuff(this, value.StartTime);
		}
	}

	public List<PlayerBuffValueHolder> getAllBuffs() {
		final List<PlayerBuffValueHolder> ret = Lists.newArrayList();
		for (final BuffStatValueHolder value : this.effects.values()) {
			ret.add(new PlayerBuffValueHolder(value.startTime, value.effect));
		}
		return ret;
	}

	public void cancelMagicDoor() {
		final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(this.effects.values());

		for (final BuffStatValueHolder value : allBuffs) {
			if (value.effect.isMagicDoor()) {
				this.cancelEffect(value.effect, false, value.startTime);
				break;
			}
		}
	}

	public final void handleEnergyCharge(final int skillid, final byte targets) {
		final ISkill echskill = SkillFactory.getSkill(skillid);
		final byte skilllevel = this.getCurrentSkillLevel(echskill);
		if (skilllevel > 0) {
			if (targets > 0) {
				if (this.getBuffedValue(BuffStat.ENERGY_CHARGE) == null) {
					echskill.getEffect(skilllevel).applyEnergyBuff(this, true); // Infinity
																				// time
				} else {
					Integer energyLevel = this.getBuffedValue(BuffStat.ENERGY_CHARGE);

					if (energyLevel < 10000) {
						energyLevel += 100 * targets;
						this.setBuffedValue(BuffStat.ENERGY_CHARGE, energyLevel);
						this.client.write(ChannelPackets.giveEnergyChargeTest(energyLevel));

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
		final int orbcount = this.getBuffedValue(BuffStat.COMBO);
		ISkill comboSkill;
		ISkill advancedComboSkill;

		switch (this.getJobId()) {
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
		final int advComboSkillLevel = this.getCurrentSkillLevel(advancedComboSkill);
		if (advComboSkillLevel > 0) {
			ceffect = advancedComboSkill.getEffect(advComboSkillLevel);
		} else {
			ceffect = comboSkill.getEffect(this.getCurrentSkillLevel(comboSkill));
		}

		if (orbcount < ceffect.getX() + 1) {
			int neworbcount = orbcount + 1;
			if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
				if (neworbcount < ceffect.getX() + 1) {
					neworbcount++;
				}
			}
			final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.COMBO, neworbcount));
			this.setBuffedValue(BuffStat.COMBO, neworbcount);
			int duration = ceffect.getDuration();
			duration += (int) (this.getBuffedStartTime(BuffStat.COMBO) - System.currentTimeMillis());

			this.client.write(ChannelPackets.giveBuff(1111002, duration, stat, ceffect));
			this.map.broadcastMessage(this, ChannelPackets.giveForeignBuff(this.getId(), stat, ceffect), false);
		}
	}

	public void handleOrbconsume() {
		ISkill comboSkill;

		switch (this.getJobId()) {
		case 1110:
		case 1111:
			comboSkill = SkillFactory.getSkill(11111001);
			break;
		default:
			comboSkill = SkillFactory.getSkill(1111002);
			break;
		}

		final StatEffect ceffect = comboSkill.getEffect(this.getCurrentSkillLevel(comboSkill));
		final List<BuffStatValue> stat = Collections.singletonList(new BuffStatValue(BuffStat.COMBO, 1));
		this.setBuffedValue(BuffStat.COMBO, 1);
		int duration = ceffect.getDuration();
		duration += (int) (this.getBuffedStartTime(BuffStat.COMBO) - System.currentTimeMillis());

		this.client.write(ChannelPackets.giveBuff(1111002, duration, stat, ceffect));
		this.map.broadcastMessage(this, ChannelPackets.giveForeignBuff(this.getId(), stat, ceffect), false);
	}

	private void silentEnforceMaxHpMp() {
		this.stats.setMp(this.stats.getMp());
		this.stats.setHp(this.stats.getHp(), true);
	}

	private void enforceMaxHpMp() {
		final List<StatValue> statups = Lists.newArrayListWithCapacity(2);
		if (this.stats.getMp() > this.stats.getCurrentMaxMp()) {
			this.stats.setMp(this.stats.getMp());
			statups.add(new StatValue(Stat.MP, Integer.valueOf(this.stats.getMp())));
		}
		if (this.stats.getHp() > this.stats.getCurrentMaxHp()) {
			this.stats.setHp(this.stats.getHp());
			statups.add(new StatValue(Stat.HP, Integer.valueOf(this.stats.getHp())));
		}
		if (statups.size() > 0) {
			this.client.write(ChannelPackets.updatePlayerStats(statups, this.getJobId()));
		}
	}

	public GameMap getMap() {
		return this.map;
	}

	public MonsterBook getMonsterBook() {
		return this.monsterBook;
	}

	public void setMap(final GameMap newMap) {
		this.map = newMap;
	}

	public void setMap(final int newMapId) {
		this.mapId = newMapId;
	}

	@Override
	public int getMapId() {
		if (this.map != null) {
			return this.map.getId();
		}
		return this.mapId;
	}

	@Override
	public int getInitialSpawnPoint() {
		return this.initialSpawnPoint;
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getWorldName() {
		return this.name;
	}

	public static int getDamageCap() {
		return damageCap;
	}

	public final String getBlessOfFairyOrigin() {
		return this.BlessOfFairy_Origin;
	}

	@Override
	public final int getLevel() {
		return this.level;
	}

	@Override
	public int getFame() {
		return this.fame;
	}

	public final int getDojo() {
		return this.dojo;
	}

	public final int getDojoRecord() {
		return this.dojoRecord;
	}

	public final int getFallCounter() {
		return this.fallcounter;
	}

	public final ChannelClient getClient() {
		return this.client;
	}

	public final void setClient(final ChannelClient client) {
		this.client = client;
	}

	@Override
	public int getExp() {
		return this.exp;
	}

	@Override
	public int getRemainingAp() {
		return this.remainingAp;
	}

	@Override
	public int[] getRemainingSps() {
		return this.remainingSp;
	}

	@Override
	public int getRemainingSp() {
		return this.remainingSp[Skills.getSkillbook(this.jobId)]; // default
	}

	@Override
	public int getRemainingSp(final int skillbook) {
		return this.remainingSp[skillbook];
	}

	@Override
	public int getRemainingSpSize() {
		int ret = 0;
		for (final int element : this.remainingSp) {
			if (element > 0) {
				ret++;
			}

		}
		return ret;
	}

	public int getMpApUsed() {
		return this.mpApUsed;
	}

	public void setMpApUsed(final int mpApUsed) {
		this.mpApUsed = mpApUsed;
	}

	public int getHpApUsed() {
		return this.hpApUsed;
	}

	public boolean isHidden() {
		return this.hidden;
	}

	public void setHpApUsed(final int hpApUsed) {
		this.hpApUsed = hpApUsed;
	}

	@Override
	public int getSkinColorId() {
		return this.skinColorId;
	}

	public void setSkinColorId(final int skinColorId) {
		this.skinColorId = skinColorId;
	}

	@Override
	public int getJobId() {
		return this.jobId;
	}

	@Override
	public Gender getGender() {
		return this.gender;
	}

	@Override
	public int getHairId() {
		return this.hairId;
	}

	@Override
	public int getFaceId() {
		return this.faceId;
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
			this.dojo = 0;
			this.dojoRecord = 0;
		} else {
			this.dojoRecord++;
		}
	}

	public void setFallCounter(final int fallcounter) {
		this.fallcounter = fallcounter;
	}

	public void setRemainingAp(final int remainingAp) {
		this.remainingAp = remainingAp;
	}

	public void setRemainingSp(final int remainingSp) {
		this.remainingSp[Skills.getSkillbook(this.jobId)] = remainingSp; // default
	}

	public void setRemainingSp(final int remainingSp, final int skillbook) {
		this.remainingSp[skillbook] = remainingSp;
	}

	public void setGender(final Gender gender) {
		this.gender = gender;
	}

	public CheatTracker getCheatTracker() {
		return this.cheatTracker;
	}

	public BuddyList getBuddyList() {
		return this.buddies;
	}

	public void addFame(final int famechange) {
		this.fame += famechange;
	}

	public void changeMapBanish(final int mapid, final String portal, final String msg) {
		this.sendNotice(5, msg);
		final GameMap newMap = ChannelServer.getMapFactory().getMap(mapid);
		this.changeMap(newMap, newMap.getPortal(portal));
	}

	public void changeMap(final GameMap to, final Point pos) {
		this.changeMapInternal(to, pos, ChannelPackets.getWarpToMap(to, 0x81, this));
	}

	public void changeMap(final GameMap to, final Portal pto) {
		this.changeMapInternal(to, pto.getPosition(), ChannelPackets.getWarpToMap(to, pto.getId(), this));
	}

	private void changeMapInternal(final GameMap to, final Point pos, final GamePacket warpPacket) {
		if (this.eventInstance != null) {
			this.eventInstance.changedMap(this, to.getId());
		}
		this.client.write(warpPacket);
		this.map.removePlayer(this);
		if (ChannelServer.getPlayerStorage().getCharacterById(this.getId()) != null) {
			this.map = to;
			this.setPosition(pos);
			to.addPlayer(this);
			this.stats.relocHeal();
		}
	}

	public void leaveMap() {
		this.controlledMonsters.clear();
		this.visibleMapObjects.clear();
		if (this.chair != 0) {
			this.cancelFishingTask();
			this.chair = 0;
		}
		if (this.hpDecreaseTask != null) {
			this.hpDecreaseTask.cancel(false);
		}
		this.cancelMapTimeLimitTask();
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
		this.client.write(ChannelPackets.updatePlayerStats(newStats, false, chr.getJobId()));
	}

	public void startHurtHp() {
		this.hpDecreaseTask = TimerManager.getInstance().register(new Runnable() {

			@Override
			public void run() {
				if (ChannelCharacter.this.map.getHPDec() < 1 || !ChannelCharacter.this.isAlive()) {
					return;
				} else if (ChannelCharacter.this.getEquippedItemsInventory().findById(ChannelCharacter.this.map.getHPDecProtect()) == null) {
					ChannelCharacter.this.addHP(-ChannelCharacter.this.map.getHPDec());
				}
			}
		}, 10000);
	}

	public void changeJob(final int newJob) {
		final boolean wasEvan = Jobs.isEvan(this.jobId);
		this.jobId = (short) newJob;
		if (Jobs.isBeginner(newJob)) {
			if (Jobs.isEvan(newJob)) {
				this.remainingSp[Skills.getSkillbook(newJob)] += 3;
			} else {
				this.remainingSp[Skills.getSkillbook(newJob)]++;
				if (newJob % 10 >= 2) {
					this.remainingSp[Skills.getSkillbook(newJob)] += 2;
				}
			}
		}
		if (!this.isGM()) {
			if (newJob % 1000 == 100) { // first job = warrior
				this.resetStats(25, 4, 4, 4);
			} else if (newJob % 1000 == 200) {
				this.resetStats(4, 4, 20, 4);
			} else if (newJob % 1000 == 300 || newJob % 1000 == 400) {
				this.resetStats(4, 25, 4, 4);
			} else if (newJob % 1000 == 500) {
				this.resetStats(4, 20, 4, 4);
			}
		}
		this.client.write(ChannelPackets.updateSp(this, false, wasEvan));
		this.updateSingleStat(Stat.JOB, newJob);

		int maxhp = this.stats.getMaxHp(), maxmp = this.stats.getMaxMp();

		switch (this.jobId) {
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
		this.stats.setMaxHp(maxhp);
		this.stats.setMaxMp(maxmp);
		this.stats.setHp(maxhp);
		this.stats.setMp(maxmp);
		final List<StatValue> statup = Lists.newArrayList();
		statup.add(new StatValue(Stat.MAX_HP, Integer.valueOf(maxhp)));
		statup.add(new StatValue(Stat.MAX_MP, Integer.valueOf(maxmp)));
		this.stats.recalcLocalStats();
		this.client.write(ChannelPackets.updatePlayerStats(statup, this.getJobId()));
		this.map.broadcastMessage(this, ChannelPackets.showForeignEffect(this.getId(), 8), false);
		this.silentPartyUpdate();
		this.updateJob();
		if (this.dragon != null) {
			this.map.broadcastMessage(ChannelPackets.removeDragon(this.id));
			this.map.removeMapObject(this.dragon);
			this.dragon = null;
		}
		if (newJob >= 2200 && newJob <= 2218) { // make new
			if (this.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
				this.cancelBuffStats(BuffStat.MONSTER_RIDING);
			}
			this.makeDragon();
			this.map.spawnDragon(this.dragon);
			if (newJob == 2217) {
				for (final int id : Skill.EVAN_SKILLS_1) {
					final ISkill skill = SkillFactory.getSkill(id);
					if (skill != null && this.getCurrentSkillLevel(skill) <= 0 && this.getMasterSkillLevel(skill) <= 0) {
						this.changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel());
					}
				}
			} else if (newJob == 2218) {
				for (final int id : Skill.EVAN_SKILLS_2) {
					final ISkill skill = SkillFactory.getSkill(id);
					if (skill != null && this.getCurrentSkillLevel(skill) <= 0 && this.getMasterSkillLevel(skill) <= 0) {
						this.changeSkillLevel(skill, skill.getMaxLevel(), skill.getMaxLevel());
					}
				}
			}

		} else if (newJob >= 431 && newJob <= 434) { // master skills
			for (final int id : Skill.DUALBLADE_SKILLS) {
				final ISkill skill = SkillFactory.getSkill(id);
				if (skill != null && this.getCurrentSkillLevel(skill) <= 0 && this.getMasterSkillLevel(skill) <= 0) {
					this.changeSkillLevel(skill, (byte) 0, (byte) skill.getMasterLevel());
				}
			}
		}
	}

	public void makeDragon() {
		this.dragon = new Dragon(this);
	}

	public Dragon getDragon() {
		return this.dragon;
	}

	public void gainAp(final int ap) {
		this.remainingAp += ap;
		this.updateSingleStat(Stat.AVAILABLE_AP, this.remainingAp);
	}

	public void gainSP(final int sp) {
		this.remainingSp[Skills.getSkillbook(this.jobId)] += sp; // default
		this.client.write(ChannelPackets.updateSp(this, false));
		this.client.write(UIPacket.getSPMsg((byte) sp));
	}

	public void changeSkillLevel(final ISkill skill, byte newLevel, byte newMasterlevel) {
		if (skill == null || !GameConstants.isApplicableSkill(skill.getId()) && !GameConstants.isApplicableSkill_(skill.getId())) {

			return;

		}
		if (newLevel == 0 && newMasterlevel == 0) {
			if (this.skills.containsKey(skill)) {
				this.skills.remove(skill);
			}
		} else {
			if (newLevel < 0) {
				newLevel = 0;
			}
			if (newMasterlevel < 0) {
				newMasterlevel = 0;
			}
			this.skills.put(skill, new SkillEntry(newLevel, newMasterlevel));
		}
		if (Skills.isRecoveryIncSkill(skill.getId())) {
			this.stats.relocHeal();
		} else if (Skills.isElementAmplification(skill.getId())) {
			this.stats.recalcLocalStats();
		}
		this.client.write(ChannelPackets.updateSkill(skill.getId(), newLevel, newMasterlevel));
	}

	public void playerDead() {
		if (this.getEventInstance() != null) {
			this.getEventInstance().playerKilled(this);
		}
		this.dispelSkill(0);
		if (this.getBuffedValue(BuffStat.MORPH) != null) {
			this.cancelEffectFromBuffStat(BuffStat.MORPH);
		}
		if (this.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
			this.cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING);
		}
		if (this.getBuffedValue(BuffStat.SUMMON) != null) {
			this.cancelEffectFromBuffStat(BuffStat.SUMMON);
		}
		if (this.getBuffedValue(BuffStat.PUPPET) != null) {
			this.cancelEffectFromBuffStat(BuffStat.PUPPET);
		}
		if (this.getBuffedValue(BuffStat.MIRROR_TARGET) != null) {
			this.cancelEffectFromBuffStat(BuffStat.MIRROR_TARGET);
		}

		if (this.jobId != 0 && this.jobId != 1000 && this.jobId != 2000 && this.jobId != 2001) {
			int charms = this.getItemQuantity(5130000, false);
			if (charms > 0) {
				InventoryManipulator.removeById(this.client, this.getCashInventory(), 5130000, 1, true, false);

				charms--;
				if (charms > 0xFF) {
					charms = 0xFF;
				}
				this.client.write(MTSCSPacket.useCharm((byte) charms, (byte) 0));
			} else {
				float diepercentage = 0.0f;
				final int expforlevel = GameConstants.getExpNeededForLevel(this.level);
				if (this.map.isTown() || FieldLimitType.RegularExpLoss.check(this.map.getFieldLimit())) {
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
				int v10 = (int) (this.exp - (long) ((double) expforlevel * diepercentage));
				if (v10 < 0) {
					v10 = 0;
				}
				this.exp = v10;
			}
		}
		this.updateSingleStat(Stat.EXP, this.exp);
	}

	public void updatePartyMemberHP() {
		if (this.party != null) {
			final int channel = this.client.getChannelId();
			for (final PartyMember partychar : this.party.getMembers()) {
				if (partychar.getMapId() == this.getMapId() && partychar.getChannel() == channel) {
					final ChannelCharacter other = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
					if (other != null) {
						final GamePacket packet = ChannelPackets.updatePartyMemberHP(this.getId(), this.stats.getHp(), this.stats.getCurrentMaxHp());
						other.getClient().write(packet);
					}
				}
			}
		}
	}

	public void receivePartyMemberHP() {
		final int channel = this.client.getChannelId();
		for (final PartyMember partychar : this.party.getMembers()) {
			if (partychar.getMapId() == this.getMapId() && partychar.getChannel() == channel) {
				final ChannelCharacter other = ChannelServer.getPlayerStorage().getCharacterByName(partychar.getName());
				if (other != null) {
					final GamePacket packet = ChannelPackets.updatePartyMemberHP(other.getId(), other.getStats().getHp(), other.getStats().getCurrentMaxHp());
					this.client.write(packet);
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
		if (this.stats.setHp(this.stats.getHp() + delta)) {
			this.updateSingleStat(Stat.HP, this.stats.getHp());
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
		if (this.stats.setMp(this.stats.getMp() + delta)) {
			this.updateSingleStat(Stat.MP, this.stats.getMp());
		}
	}

	public void addMPHP(final int hpDiff, final int mpDiff) {
		final List<StatValue> statups = Lists.newArrayList();

		if (this.stats.setHp(this.stats.getHp() + hpDiff)) {
			statups.add(new StatValue(Stat.HP, Integer.valueOf(this.stats.getHp())));
		}
		if (this.stats.setMp(this.stats.getMp() + mpDiff)) {
			statups.add(new StatValue(Stat.MP, Integer.valueOf(this.stats.getMp())));
		}
		if (statups.size() > 0) {
			this.client.write(ChannelPackets.updatePlayerStats(statups, this.getJobId()));
		}
	}

	public void updateSingleStat(final Stat stat, final int newval) {
		this.updateSingleStat(stat, newval, false);
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
			this.client.write(ChannelPackets.updateSp(this, itemReaction, false));
			return;
		}
		final StatValue value = new StatValue(stat, Integer.valueOf(newval));
		this.client.write(ChannelPackets.updatePlayerStats(Collections.singletonList(value), itemReaction, this.getJobId()));
	}

	public void gainExp(final int total, final boolean show, final boolean inChat, final boolean white) {
		if (this.level == 200 || Jobs.isCygnus(this.jobId) && this.level == 120) {
			final int needed = GameConstants.getExpNeededForLevel(this.level);
			if (this.exp + total > needed) {
				this.setExp(needed);
			} else {
				this.exp += total;
			}
		} else {
			if (this.exp + total >= GameConstants.getExpNeededForLevel(this.level)) {
				this.exp += total;
				this.levelUp();

				final int needed = GameConstants.getExpNeededForLevel(this.level);
				if (this.exp > needed) {
					this.setExp(needed);
				}
			} else {
				this.exp += total;
			}
		}
		if (total != 0) {
			if (this.exp < 0) { // After adding, and negative
				if (total > 0) {
					this.setExp(GameConstants.getExpNeededForLevel(this.level));
				} else if (total < 0) {
					this.setExp(0);
				}
			}
			this.updateSingleStat(Stat.EXP, this.getExp());
			if (show) { // still show the expgain even if it's not there
				this.client.write(ChannelPackets.GainEXP_Others(total, inChat, white));
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
		if (this.haveItem(5210006, 1, false, true) && hour > 22 && hour < 2 || this.haveItem(5210007, 1, false, true) && hour > 2 && hour < 6
			|| this.haveItem(5210008, 1, false, true) && hour > 6 && hour < 10 || this.haveItem(5210009, 1, false, true) && hour > 10 && hour < 14
			|| this.haveItem(5210010, 1, false, true) && hour > 14 && hour < 18 || this.haveItem(5210011, 1, false, true) && hour > 18 && hour < 22) {
			baseExp *= 2;
		}

		if (this.level >= 1 && this.level <= 10) {
			eventExp = (int) (baseExp * 0.1);
			premiumExp = (int) (baseExp * 0.1);
			itemExp = (int) (baseExp * 0.1);
		}

		if (this.haveItem(1112127, 1, true, true)) {
			// Welcome Back Ring | must be equipped in order to work
			partyRingExp = (int) (baseExp * 0.8);
		}

		if (partyMembers > 1) {
			partyExp = (int) ((baseExp / 10.0f) * (partyMembers + 1)); // 10%
		}
		if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
			// Saturday and Sunday
			rainbowExp = (int) (baseExp * 0.1);
		}
		if (this.level == 200 || Jobs.isCygnus(this.jobId) && this.level == 120) {
			final int needed = GameConstants.getExpNeededForLevel(this.level);
			if (this.exp + baseExp > needed) {
				this.setExp(needed);
			} else {
				this.exp += baseExp + eventExp + weddingExp + partyRingExp + partyExp + premiumExp + itemExp + rainbowExp + CLASS_EXP;
			}
		} else {
			if (this.exp + baseExp >= GameConstants.getExpNeededForLevel(this.level)) {
				this.exp += baseExp;
				this.levelUp();
				final int needed = GameConstants.getExpNeededForLevel(this.level);
				if (this.exp > needed) {
					this.setExp(needed);
				}
			} else {
				this.exp += baseExp + eventExp + weddingExp + partyRingExp + partyExp + premiumExp + itemExp + rainbowExp + CLASS_EXP;
			}
		}
		if (gain != 0) {
			if (this.exp < 0) {
				// After adding, and negative
				if (gain > 0) {
					this.setExp(GameConstants.getExpNeededForLevel(this.level));
				} else if (gain < 0) {
					this.setExp(0);
				}
			}
			this.updateSingleStat(Stat.EXP, this.getExp());
			if (show) {
				// still show the expgain even if it's not there
				this.client.write(ChannelPackets.GainEXP_Monster(baseExp, white, eventExp, weddingExp, partyRingExp, partyExp, premiumExp, itemExp, rainbowExp,
					CLASS_EXP));
			}
		}
	}

	public void silentPartyUpdate() {
		if (this.party != null) {
			try {
				final PartyMember newMember = new PartyMember(this.party.getId(), this);
				ChannelServer.getWorldInterface().updateParty(this.party.getId(), PartyOperation.SILENT_UPDATE, newMember);
			} catch (final RemoteException e) {
				System.err.println("REMOTE THROW, silentPartyUpdate" + e);
				ChannelServer.pingWorld();
			}
		}
	}

	public boolean isGM() {
		return this.gmLevel > 0;
	}

	@Override
	public int getGmLevel() {
		return this.gmLevel;
	}

	public boolean hasGmLevel(final int level) {
		return this.gmLevel >= level;
	}

	public final Inventory getEquipInventory() {
		return this.inventory.get(InventoryType.EQUIP);
	}

	public final Inventory getUseInventory() {
		return this.inventory.get(InventoryType.USE);
	}

	public final Inventory getSetupInventory() {
		return this.inventory.get(InventoryType.SETUP);
	}

	public final Inventory getEtcInventory() {
		return this.inventory.get(InventoryType.ETC);
	}

	public final Inventory getCashInventory() {
		return this.inventory.get(InventoryType.CASH);
	}

	@Override
	public final Inventory getEquippedItemsInventory() {
		return this.inventory.get(InventoryType.EQUIPPED);
	}

	public final Inventory getInventoryForItem(final int itemId) {
		return this.inventory.get(GameConstants.getInventoryType(itemId));
	}

	public final Inventory getInventoryByType(final InventoryType type) {
		return this.inventory.get(type);
	}

	public final MultiInventory getInventories() {
		return this.inventory;
	}

	public final void expirationTask() {
		long expiration;
		final long currenttime = System.currentTimeMillis();
		// This is here to prevent deadlock.
		final List<Item> toberemove = Lists.newArrayList();

		for (final Inventory inv : this.inventory) {
			for (final Item item : inv) {
				expiration = item.getExpiration();

				if (expiration != -1 && !GameConstants.isPet(item.getItemId())) {
					final byte flag = item.getFlag();

					if (ItemFlag.LOCK.check(flag)) {
						if (currenttime > expiration) {
							item.setExpiration(-1);
							item.setFlag((byte) (flag - ItemFlag.LOCK.getValue()));
							this.client.write(ChannelPackets.updateSpecialItemUse(item, item.getType().asNumber()));
						}
					} else if (currenttime > expiration) {
						this.client.write(MTSCSPacket.itemExpired(item.getItemId()));
						toberemove.add(item);
					}
				}
			}
			for (final Item item : toberemove) {
				InventoryManipulator.removeFromSlot(this.client, inv, item.getPosition(), item.getQuantity(), false);
			}
		}
	}

	public Shop getShop() {
		return this.shop;
	}

	public void setShop(final Shop shop) {
		this.shop = shop;
	}

	@Override
	public int getMeso() {
		return this.meso;
	}

	public final EnumMap<SavedLocationType, Integer> getSavedLocations() {
		return Maps.newEnumMap(this.savedLocations);
	}

	public int getSavedLocation(final SavedLocationType type) {
		return this.savedLocations.get(type).intValue();
	}

	public void saveLocation(final SavedLocationType type) {
		this.savedLocations.put(type, this.mapId);
	}

	public void clearSavedLocation(final SavedLocationType type) {
		this.savedLocations.remove(type);
	}

	public void gainMeso(final int gain, final boolean show) {
		this.gainMeso(gain, show, false, false);
	}

	public void gainMeso(final int gain, final boolean show, final boolean enableActions) {
		this.gainMeso(gain, show, enableActions, false);
	}

	public void gainMeso(final int gain, final boolean show, final boolean enableActions, final boolean inChat) {
		if (this.meso + gain < 0) {
			this.client.write(ChannelPackets.enableActions());
			return;
		}
		this.meso += gain;
		this.updateSingleStat(Stat.MESO, this.meso, enableActions);
		if (show) {
			this.client.write(ChannelPackets.showMesoGain(gain, inChat));
		}
	}

	public void controlMonster(final Monster monster, final boolean aggro) {
		monster.setController(this);
		this.controlledMonsters.add(monster);
		this.client.write(MobPacket.controlMonster(monster, false, aggro));
	}

	public void stopControllingMonster(final Monster monster) {
		this.controlledMonsters.remove(monster);
	}

	public void checkMonsterAggro(final Monster monster) {
		if (monster.getController() == this) {
			monster.setControllerHasAggro(true);
		} else {
			monster.switchController(this, true);
		}
	}

	public Collection<Monster> getControlledMonsters() {
		return Collections.unmodifiableCollection(this.controlledMonsters);
	}

	public int getAccountId() {
		return this.accountId;
	}

	public void mobKilled(final int id) {
		for (final Map.Entry<Integer, QuestStatus> entry : this.quests.entrySet()) {
			final QuestStatus status = entry.getValue();
			if (status.getState() != 1 || !status.hasMobKills()) {
				continue;
			}
			if (status.mobKilled(id)) {
				this.client.write(ChannelPackets.updateQuestMobKills(status));
				final int questId = status.getQuestId();
				final QuestInfo info = QuestInfoProvider.getInfo(questId);
				if (info.canComplete(this, null)) {
					this.client.write(ChannelPackets.getShowQuestCompletion(questId));
				}
			}
		}
	}

	public final List<QuestStatus> getStartedQuests() {
		final List<QuestStatus> ret = Lists.newLinkedList();
		for (final QuestStatus q : this.quests.values()) {
			if (q.getState() == 1) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public final List<QuestStatus> getCompletedQuests() {
		final List<QuestStatus> ret = Lists.newLinkedList();
		for (final QuestStatus q : this.quests.values()) {
			if (q.getState() == 2) {
				ret.add(q);
			}
		}
		return Collections.unmodifiableList(ret);
	}

	public Map<ISkill, SkillEntry> getSkills() {
		return Collections.unmodifiableMap(this.skills);
	}

	public byte getCurrentSkillLevel(final ISkill skill) {
		final SkillEntry ret = this.skills.get(skill);
		if (ret == null) {
			return 0;
		}
		return ret.getCurrentLevel();
	}

	public byte getMasterSkillLevel(final ISkill skill) {
		final SkillEntry ret = this.skills.get(skill);
		if (ret == null) {
			return 0;
		}
		return ret.getMasterLevel();
	}

	public void levelUp() {
		if (Jobs.isCygnus(this.jobId)) {
			if (this.level <= 70) {
				this.remainingAp += 6;
			} else {
				this.remainingAp += 5;
			}
		} else {
			this.remainingAp += 5;
		}
		int maxhp = this.stats.getMaxHp();
		int maxmp = this.stats.getMaxMp();
		if (this.jobId == 0 || this.jobId == 1000 || this.jobId == 2000) {
			// Beginner
			maxhp += Randomizer.rand(12, 16);
			maxmp += Randomizer.rand(10, 12);
		} else if (this.jobId >= 100 && this.jobId <= 132) {
			// Warrior
			final ISkill improvingMaxHP = SkillFactory.getSkill(1000001);
			final int slevel = this.getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(24, 28);
			maxmp += Randomizer.rand(4, 6);
		} else if (this.jobId >= 200 && this.jobId <= 232) {
			// Magician
			final ISkill improvingMaxMP = SkillFactory.getSkill(2000001);
			final int skillLevel = this.getCurrentSkillLevel(improvingMaxMP);
			if (skillLevel > 0) {
				maxmp += improvingMaxMP.getEffect(skillLevel).getX() * 2;
			}
			maxhp += Randomizer.rand(10, 14);
			maxmp += Randomizer.rand(22, 24);
		} else if (this.jobId >= 300 && this.jobId <= 322 || this.jobId >= 400 && this.jobId <= 434 || this.jobId >= 1300 && this.jobId <= 1311
			|| this.jobId >= 1400 && this.jobId <= 1411) {
			// Bowman, Thief, Wind Breaker and Night Walker
			maxhp += Randomizer.rand(20, 24);
			maxmp += Randomizer.rand(14, 16);
		} else if (this.jobId >= 500 && this.jobId <= 522) {
			// Pirate
			final ISkill improvingMaxHP = SkillFactory.getSkill(5100000);
			final int slevel = this.getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(22, 26);
			maxmp += Randomizer.rand(18, 22);
		} else if (this.jobId >= 1100 && this.jobId <= 1111) {
			// Soul Master
			final ISkill improvingMaxHP = SkillFactory.getSkill(11000000);
			final int slevel = this.getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(24, 28);
			maxmp += Randomizer.rand(4, 6);
		} else if (this.jobId >= 1200 && this.jobId <= 1211) {
			// Flame Wizard
			final ISkill improvingMaxMP = SkillFactory.getSkill(12000000);
			final int slevel = this.getCurrentSkillLevel(improvingMaxMP);
			if (slevel > 0) {
				maxmp += improvingMaxMP.getEffect(slevel).getX() * 2;
			}
			maxhp += Randomizer.rand(10, 14);
			maxmp += Randomizer.rand(22, 24);
		} else if (this.jobId >= 2200 && this.jobId <= 2218) {
			// Evan
			maxhp += Randomizer.rand(12, 16);
			maxmp += Randomizer.rand(50, 52);
		} else if (this.jobId >= 1500 && this.jobId <= 1512) {
			// Pirate
			final ISkill improvingMaxHP = SkillFactory.getSkill(15100000);
			final int slevel = this.getCurrentSkillLevel(improvingMaxHP);
			if (slevel > 0) {
				maxhp += improvingMaxHP.getEffect(slevel).getX();
			}
			maxhp += Randomizer.rand(22, 26);
			maxmp += Randomizer.rand(18, 22);
		} else if (this.jobId >= 2100 && this.jobId <= 2112) {
			// Aran
			maxhp += Randomizer.rand(50, 52);
			maxmp += Randomizer.rand(4, 6);
		} else {
			// GameMaster
			maxhp += Randomizer.rand(50, 100);
			maxmp += Randomizer.rand(50, 100);
		}
		maxmp += this.stats.getTotalInt() / 10;
		this.exp -= GameConstants.getExpNeededForLevel(this.level);
		this.level += 1;
		if (this.level == 200 && !this.isGM()) {
			try {
				final StringBuilder sb = new StringBuilder("[Congratulation] ");
				final Item medal = this.getEquippedItemsInventory().getItem((byte) -46);
				if (medal != null) { // Medal
					sb.append("<");
					sb.append(ItemInfoProvider.getInstance().getName(medal.getItemId()));
					sb.append("> ");
				}
				sb.append(this.getName());
				sb.append(" has achieved Level 200. Let us Celebrate! Maplers!");
				ChannelServer.getWorldInterface().broadcastMessage(ChannelPackets.serverNotice(6, sb.toString()));
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}
		maxhp = Math.min(30000, maxhp);
		maxmp = Math.min(30000, maxmp);
		final List<StatValue> statup = Lists.newArrayListWithCapacity(8);
		statup.add(new StatValue(Stat.MAX_HP, maxhp));
		statup.add(new StatValue(Stat.MAX_MP, maxmp));
		statup.add(new StatValue(Stat.HP, maxhp));
		statup.add(new StatValue(Stat.MP, maxmp));
		statup.add(new StatValue(Stat.EXP, this.exp));
		statup.add(new StatValue(Stat.LEVEL, this.level));
		if (this.jobId != 0 && this.jobId != 1000 && this.jobId != 2000 && this.jobId != 2001) { // Not
			// Beginner,
			// Nobless
			// and
			// Legend
			this.remainingSp[Skills.getSkillbook(this.jobId)] += 3;
			this.client.write(ChannelPackets.updateSp(this, false));
		} else {
			if (this.level <= 10) {
				this.stats.setStr(this.stats.getStr() + this.remainingAp);
				this.remainingAp = 0;
				statup.add(new StatValue(Stat.STR, this.stats.getStr()));
			}
		}
		statup.add(new StatValue(Stat.AVAILABLE_AP, this.remainingAp));
		this.stats.setMaxHp(maxhp);
		this.stats.setMaxMp(maxmp);
		this.stats.setHp(maxhp);
		this.stats.setMp(maxmp);
		this.client.write(ChannelPackets.updatePlayerStats(statup, this.getJobId()));
		this.map.broadcastMessage(this, ChannelPackets.showForeignEffect(this.getId(), 0), false);
		this.stats.recalcLocalStats();
		this.silentPartyUpdate();
		this.updateLevel();
		NpcScriptManager.getInstance().start(this.getClient(), 9105010); // Vavaan
	}

	public void changeKeybinding(final int key, final KeyBinding keybinding) {
		if (keybinding.getType() != 0) {
			this.keylayout.Layout().put(Integer.valueOf(key), keybinding);
		} else {
			this.keylayout.Layout().remove(Integer.valueOf(key));
		}
	}

	public void sendMacros() {
		for (int i = 0; i < 5; i++) {
			if (this.skillMacros[i] != null) {
				this.client.write(ChannelPackets.getMacros(this.skillMacros));
				break;
			}
		}
	}

	public void updateMacros(final int position, final SkillMacro updateMacro) {
		this.skillMacros[position] = updateMacro;
	}

	public final SkillMacro[] getMacros() {
		return this.skillMacros;
	}

	public void temporaryBan(final String reason, final Calendar duration, final int tempBanReason) {
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
			ps.setString(1, this.client.getSessionIP());
			ps.execute();
			ps.close();

			this.client.disconnect(true);

			ps = con.prepareStatement("UPDATE accounts SET tempban = ?, tempban_reason = ? WHERE id = ?");
			final Timestamp timestamp = new Timestamp(duration.getTimeInMillis());
			ps.setTimestamp(1, timestamp);
			ps.setString(2, reason);
			ps.setInt(3, tempBanReason);
			ps.setInt(4, this.accountId);
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
			ps.setInt(3, this.accountId);
			ps.execute();
		} catch (final SQLException ex) {
			System.err.println("Error while banning" + ex);
			return false;
		}
		return true;
	}

	@Override
	public int getObjectId() {
		return this.getId();
	}

	@Override
	public void setObjectId(final int id) {
		throw new UnsupportedOperationException();
	}

	public Storage getStorage() {
		return this.storage;
	}

	public void addVisibleMapObject(final GameMapObject mo) {
		this.visibleMapObjects.add(mo);
	}

	public void removeVisibleMapObject(final GameMapObject mo) {
		this.visibleMapObjects.remove(mo);
	}

	public boolean isMapObjectVisible(final GameMapObject mo) {
		return this.visibleMapObjects.contains(mo);
	}

	public Collection<GameMapObject> getVisibleMapObjects() {
		return Collections.unmodifiableCollection(this.visibleMapObjects);
	}

	public boolean isAlive() {
		return this.stats.getHp() > 0;
	}

	@Override
	public void sendDestroyData(final ChannelClient client) {
		client.write(ChannelPackets.removePlayerFromMap(this.getObjectId()));
	}

	@Override
	public void sendSpawnData(final ChannelClient client) {
		if (!this.isHidden()) {
			client.write(ChannelPackets.spawnPlayerMapObject(this));

			for (final Pet pet : this.pets) {
				if (pet.isSummoned()) {
					client.write(PetPacket.showPet(this, pet));
				}
			}

			if (this.dragon != null) {
				client.write(ChannelPackets.spawnDragon(this.dragon));
			}
		}
	}

	public void setDragon(final Dragon d) {
		this.dragon = d;
	}

	public final void equipChanged() {
		this.map.broadcastMessage(this, ChannelPackets.updateCharLook(this), false);
		this.stats.recalcLocalStats();
		this.enforceMaxHpMp();

		final ChannelCharacter player = this.client.getPlayer();
		if (player.getMessenger() != null) {
			final WorldChannelInterface wci = ChannelServer.getWorldInterface();
			try {
				wci.updateMessenger(player.getMessenger().getId(), player.getName(), this.client.getChannelId());
			} catch (final RemoteException e) {
				ChannelServer.pingWorld();
			}
		}
	}

	public final Pet getPet(final int index) {
		byte count = 0;
		for (final Pet pet : this.pets) {
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
		this.pets.remove(pet);
		this.pets.add(pet);
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
		for (final Pet pet : this.pets) {
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
		for (final Pet pet : this.pets) {
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
		return this.pets;
	}

	public final void unequipAllPets() {
		for (final Pet pet : this.pets) {
			if (pet != null) {
				this.unequipPet(pet, true, false);
			}
		}
	}

	public void unequipPet(final Pet pet, final boolean shiftLeft, final boolean hunger) {
		this.cancelFullnessSchedule(this.getPetIndex(pet));
		pet.saveToDb();

		this.map.broadcastMessage(this, PetPacket.removePet(this, pet, hunger), true);
		final List<StatValue> petStat = Lists.newArrayList();
		petStat.add(new StatValue(Stat.PET, Integer.valueOf(0)));
		this.client.write(PetPacket.petStatUpdate(this));
		this.client.write(ChannelPackets.enableActions());
		this.removePet(pet, shiftLeft);
	}

	/*
	 * public void shiftPetsRight() { if (pets[2] == null) { pets[2] = pets[1];
	 * pets[1] = pets[0]; pets[0] = null; } }
	 */
	public final long getLastFameTime() {
		return this.lastFameTime;
	}

	public final void setLastFameTime(final long timestamp) {
		this.lastFameTime = timestamp;
	}

	public final boolean hasFamedToday() {
		final long day = (long) Math.floor(this.lastFameTime / 86400000.0);
		final long today = (long) Math.floor(System.currentTimeMillis() / 86400000.0);
		return day < today;
	}

	public final KeyLayout getKeyLayout() {
		return this.keylayout;
	}

	public boolean hasParty() {
		return this.partyMember != null;
	}

	public PartyMember getPartyMembership() {
		return this.partyMember;
	}

	public PartyMember setPartyMembership(final int partyId) {
		this.partyMember = new PartyMember(partyId, this);
		return this.partyMember;
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
		return this.trade;
	}

	public void setTrade(final Trade trade) {
		this.trade = trade;
	}

	public EventInstanceManager getEventInstance() {
		return this.eventInstance;
	}

	public void setEventInstance(final EventInstanceManager eventInstance) {
		this.eventInstance = eventInstance;
	}

	public void addDoor(final Door door) {
		this.doors.add(door);
	}

	public void clearDoors() {
		this.doors.clear();
	}

	public ImmutableList<Door> getDoors() {
		return ImmutableList.copyOf(this.doors);
	}

	public void setSmega() {
		if (this.smega) {
			this.smega = false;
			this.sendNotice(5, "You have set megaphone to disabled mode");
		} else {
			this.smega = true;
			this.sendNotice(5, "You have set megaphone to enabled mode");
		}
	}

	public boolean getSmega() {
		return this.smega;
	}

	public boolean canDoor() {
		return this.canDoor;
	}

	public void disableDoor() {
		this.canDoor = false;
		TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				ChannelCharacter.this.canDoor = true;
			}
		}, 5000);
	}

	public Map<Integer, Summon> getSummons() {
		return this.summons;
	}

	public int getChair() {
		return this.chair;
	}

	public int getItemEffect() {
		return this.itemEffect;
	}

	public void setChair(final int chair) {
		this.chair = chair;
		this.stats.relocHeal();
	}

	public void setItemEffect(final int itemEffect) {
		this.itemEffect = itemEffect;
	}

	@Override
	public GameMapObjectType getType() {
		return GameMapObjectType.PLAYER;
	}

	public int getGuildId() {
		return this.guildId;
	}

	public MemberRank getGuildRank() {
		return this.guildRank;
	}

	public void setGuildId(final int _id) {
		this.guildId = _id;
		if (this.guildId > 0) {
			if (this.guildMember == null) {
				this.guildMember = new GuildMember(this);
			} else {
				this.guildMember.setGuildId(this.guildId);
			}
		} else {
			this.guildMember = null;
		}
	}

	public void setGuildRank(final MemberRank newRank) {
		this.guildRank = newRank;
		if (this.guildMember != null) {
			this.guildMember.setGuildRank(newRank);
		}
	}

	public GuildMember getGuildMembership() {
		return this.guildMember;
	}

	public Guild getGuild() {
		try {
			return ChannelServer.getWorldInterface().getGuild(this.getGuildId());
		} catch (final RemoteException e) {
			ChannelServer.pingWorld();
		}
		return null;
	}

	private void updateJob() {
		final WorldChannelInterface world = ChannelServer.getWorldInterface();
		if (this.guildMember == null) {
			return;
		} else {
			try {
				world.updateGuildMemberJob(this.guildId, this.hairId, this.jobId);
			} catch (final RemoteException ex) {
				System.err.println("Could not update level: " + ex);
			}
		}
	}

	private void updateLevel() {
		final WorldChannelInterface world = ChannelServer.getWorldInterface();
		if (this.guildMember == null) {
			return;
		} else {
			try {
				world.updateGuildMemberLevel(this.guildId, this.hairId, this.level);
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
			ps.setInt(1, this.guildId);
			ps.setInt(2, this.guildRank.asNumber());
			ps.setInt(3, this.id);
			ps.execute();
		} catch (final SQLException se) {
			System.err.println("SQL error: " + se.getLocalizedMessage() + se);
		}
	}

	public void modifyCSPoints(final int type, final int quantity, final boolean show) {
		if (this.getNX() < 0) {
			this.aCash = 0;
		}
		if (this.getNX() > 1000000) {
			this.aCash = 900000;
		}
		if (this.getNX() + quantity < 900000) {
			switch (type) {
			case 1:
				this.aCash += quantity;
				break;
			case 2:
				this.maplePoints += quantity;
				break;
			default:
				break;
			}
			if (show) {
				this.sendNotice(5, "You have gained " + quantity + " cash.");
				this.client.write(ChannelPackets.showSpecialEffect(19));
			}
		} else {
			this.sendNotice(5, "You have reached the maximum ammount of @cash");
		}
	}

	public int getCSPoints(final int type) {
		switch (type) {
		case 1:
			return this.aCash;
		case 2:
			return this.maplePoints;
		default:
			return 0;
		}
	}

	public final boolean haveItem(final int itemId, final int quantity, final boolean checkEquipped, final boolean greaterOrEquals) {
		int count = this.getInventoryForItem(itemId).countById(itemId);
		if (checkEquipped) {
			count += this.inventory.get(InventoryType.EQUIPPED).countById(itemId);
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
		final SkillEntry ret = this.skills.get(SkillFactory.getSkill(skill));
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
		return this.buddies.getCapacity();
	}

	public void setBuddyCapacity(final int capacity) {
		this.buddies.setCapacity(capacity);
		this.client.write(ChannelPackets.updateBuddyCapacity(capacity));
	}

	public Messenger getMessenger() {
		return this.messenger;
	}

	public void setMessenger(final Messenger messenger) {
		this.messenger = messenger;
	}

	public int getMessengerPosition() {
		return this.messengerPosition;
	}

	public void setMessengerPosition(final int position) {
		this.messengerPosition = position;
	}

	public void addCooldown(final int skillId, final long startTime, final long length, final ScheduledFuture<?> timer) {
		this.cooldowns.put(Integer.valueOf(skillId), new CooldownValueHolder(skillId, startTime, length, timer));
	}

	public void removeCooldown(final int skillId) {
		if (this.cooldowns.containsKey(Integer.valueOf(skillId))) {
			this.cooldowns.remove(Integer.valueOf(skillId));
		}
	}

	public boolean isInCooldown(final int skillId) {
		return this.cooldowns.containsKey(Integer.valueOf(skillId));
	}

	public void giveCooldowns(final int skillid, final long starttime, final long length) {
		final int time = (int) (length + starttime - System.currentTimeMillis());
		final ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time);
		this.addCooldown(skillid, System.currentTimeMillis(), time, timer);
	}

	public void giveCooldowns(final Collection<PlayerCooldownValueHolder> cooldowns) {
		int time;
		if (cooldowns != null) {
			for (final PlayerCooldownValueHolder cooldown : cooldowns) {
				time = (int) (cooldown.Length + cooldown.StartTime - System.currentTimeMillis());
				final ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, cooldown.SkillId), time);
				this.addCooldown(cooldown.SkillId, System.currentTimeMillis(), time, timer);
			}
		} else {
			final Connection con = Database.getConnection();
			try (	PreparedStatement ps = this.getSelectCooldowns(con);
					ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					final long length = rs.getLong("length");
					final long startTime = rs.getLong("StartTime");
					if (length + startTime - System.currentTimeMillis() <= 0) {
						continue;
					}

					final int skillId = rs.getInt("SkillID");
					this.giveCooldowns(skillId, startTime, length);
				}
				this.deleteByCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");

			} catch (final SQLException e) {
				System.err.println("Error while retriving cooldown from SQL storage");
			}
		}
	}

	private PreparedStatement getSelectCooldowns(final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
		ps.setInt(1, this.getId());
		return ps;
	}

	public List<PlayerCooldownValueHolder> getAllCooldowns() {
		final List<PlayerCooldownValueHolder> ret = Lists.newArrayList();
		for (final CooldownValueHolder mcdvh : this.cooldowns.values()) {
			ret.add(new PlayerCooldownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
		}
		return ret;
	}

	public final List<PlayerDiseaseValueHolder> getAllDiseases() {
		final List<PlayerDiseaseValueHolder> ret = Lists.newArrayListWithCapacity(5);

		DiseaseValueHolder vh;
		for (final Entry<Disease, DiseaseValueHolder> disease : this.diseases.entrySet()) {
			vh = disease.getValue();
			ret.add(new PlayerDiseaseValueHolder(disease.getKey(), vh.startTime, vh.length));
		}
		return ret;
	}

	public final boolean hasDisease(final Disease disease) {
		for (final Disease current : this.diseases.keySet()) {
			if (current == disease) {
				return true;
			}
		}
		return false;
	}

	public void giveDebuff(final Disease disease, final MobSkill skill) {
		final List<DiseaseValue> debuff = Lists.newArrayList(new DiseaseValue(disease, skill.getX()));

		if (!this.hasDisease(disease) && this.diseases.size() < 2) {
			if (!(disease == Disease.SEDUCE || disease == Disease.STUN)) {
				if (this.isActiveBuffedValue(2321005)) {
					return;
				}
			}
			TimerManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					ChannelCharacter.this.dispelDebuff(disease);
				}
			}, skill.getDuration());

			this.diseases.put(disease, new DiseaseValueHolder(System.currentTimeMillis(), skill.getDuration()));
			this.client.write(ChannelPackets.giveDebuff(debuff, skill));
			this.map.broadcastMessage(this, ChannelPackets.giveForeignDebuff(this.id, debuff, skill), false);
		}
	}

	public final void giveSilentDebuff(final Collection<PlayerDiseaseValueHolder> ld) {
		if (ld != null) {
			for (final PlayerDiseaseValueHolder disease : ld) {

				TimerManager.getInstance().schedule(new Runnable() {

					@Override
					public void run() {
						ChannelCharacter.this.dispelDebuff(disease.Disease);
					}
				}, disease.Length + disease.StartTime - System.currentTimeMillis());

				this.diseases.put(disease.Disease, new DiseaseValueHolder(disease.StartTime, disease.Length));
			}
		}
	}

	public void dispelDebuff(final Disease debuff) {
		if (this.hasDisease(debuff)) {
			final long mask = debuff.getValue();
			this.client.write(ChannelPackets.cancelDebuff(mask));
			this.map.broadcastMessage(this, ChannelPackets.cancelForeignDebuff(this.id, mask), false);

			this.diseases.remove(debuff);
		}
	}

	public void dispelDebuffs() {
		this.dispelDebuff(Disease.CURSE);
		this.dispelDebuff(Disease.DARKNESS);
		this.dispelDebuff(Disease.POISON);
		this.dispelDebuff(Disease.SEAL);
		this.dispelDebuff(Disease.WEAKEN);
	}

	public void cancelAllDebuffs() {
		this.diseases.clear();
	}

	public void setLevel(final short level) {
		this.level = (short) (level - 1);
	}

	public void sendNote(final String recepientName, final String message) {
		Notes.send(this.name, recepientName, message);
	}

	public void showNote() {
		final List<Note> notes = Notes.loadReceived(this.name);
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
			if (this.mulung_energy + 100 > 10000) {
				this.mulung_energy = 10000;
			} else {
				this.mulung_energy += 100;
			}
		} else {
			this.mulung_energy = 0;
		}
		this.client.write(ChannelPackets.MulungEnergy(this.mulung_energy));
	}

	public void writeMulungEnergy() {
		this.client.write(ChannelPackets.MulungEnergy(this.mulung_energy));
	}

	public final short getCombo() {
		return this.combo;
	}

	public void setCombo(final short combo) {
		this.combo = combo;
	}

	public final long getLastCombo() {
		return this.lastCombo;
	}

	public void setLastCombo(final long combo) {
		this.lastCombo = combo;
	}

	public final long getKeyDownSkill_Time() {
		return this.keydown_skill;
	}

	public void setKeyDownSkill_Time(final long keydown_skill) {
		this.keydown_skill = keydown_skill;
	}

	public void checkBerserk() {
		if (this.BerserkSchedule != null) {
			this.BerserkSchedule.cancel(false);
			this.BerserkSchedule = null;
		}

		final ISkill berserkX = SkillFactory.getSkill(1320006);
		final int skilllevel = this.getCurrentSkillLevel(berserkX);
		if (skilllevel >= 1) {
			final StatEffect ampStat = berserkX.getEffect(skilllevel);

			if (this.stats.getHp() * 100 / this.stats.getMaxHp() > ampStat.getX()) {
				this.berserk = false;
			} else {
				this.berserk = true;
			}

			this.client.write(ChannelPackets.showOwnBerserk(skilllevel, this.berserk));
			this.map.broadcastMessage(this, ChannelPackets.showBerserk(this.getId(), skilllevel, this.berserk), false);

			this.BerserkSchedule = TimerManager.getInstance().schedule(new Runnable() {

				@Override
				public void run() {
					ChannelCharacter.this.checkBerserk();
				}
			}, 10000);
		}
	}

	private void prepareBeholderEffect() {
		if (this.beholderHealingSchedule != null) {
			this.beholderHealingSchedule.cancel(false);
		}
		if (this.beholderBuffSchedule != null) {
			this.beholderBuffSchedule.cancel(false);
		}
		final ISkill bHealing = SkillFactory.getSkill(1320008);
		final int bHealingLvl = this.getCurrentSkillLevel(bHealing);
		if (bHealingLvl > 0) {
			final StatEffect healEffect = bHealing.getEffect(bHealingLvl);
			final int healInterval = healEffect.getX() * 1000;
			this.beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {

				@Override
				public void run() {
					ChannelCharacter.this.addHP(healEffect.getHp());
					ChannelCharacter.this.client.write(ChannelPackets.showOwnBuffEffect(1321007, 2));
					ChannelCharacter.this.map.broadcastMessage(ChannelCharacter.this, ChannelPackets.summonSkill(ChannelCharacter.this.getId(), 1321007, 5),
						true);
					ChannelCharacter.this.map.broadcastMessage(ChannelCharacter.this, ChannelPackets.showBuffeffect(ChannelCharacter.this.getId(), 1321007, 2),
						false);
				}
			}, healInterval, healInterval);
		}
		final ISkill bBuff = SkillFactory.getSkill(1320009);
		final int bBuffLvl = this.getCurrentSkillLevel(bBuff);
		if (bBuffLvl > 0) {
			final StatEffect buffEffect = bBuff.getEffect(bBuffLvl);
			final int buffInterval = buffEffect.getX() * 1000;
			this.beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {

				@Override
				public void run() {
					buffEffect.applyTo(ChannelCharacter.this);
					ChannelCharacter.this.client.write(ChannelPackets.showOwnBuffEffect(1321007, 2));
					ChannelCharacter.this.map.broadcastMessage(ChannelCharacter.this, ChannelPackets.summonSkill(ChannelCharacter.this.getId(), 1321007,
						(int) (Math.random() * 3) + 6), true);
					ChannelCharacter.this.map.broadcastMessage(ChannelCharacter.this, ChannelPackets.showBuffeffect(ChannelCharacter.this.getId(), 1321007, 2),
						false);
				}
			}, buffInterval, buffInterval);
		}
	}

	public void setChalkboard(final String text) {
		this.chalktext = text;
		this.map.broadcastMessage(MTSCSPacket.useChalkboard(this.getId(), text));
	}

	public String getChalkboard() {
		return this.chalktext;
	}

	public Mount getMount() {
		return this.mount;
	}

	public int[] getWishlist() {
		return this.wishlist;
	}

	public void clearWishlist() {
		for (int i = 0; i < 10; i++) {
			this.wishlist[i] = 0;
		}
	}

	public int getWishlistSize() {
		int ret = 0;
		for (int i = 0; i < 10; i++) {
			if (this.wishlist[i] > 0) {
				ret++;
			}
		}
		return ret;
	}

	public int[] getRocks() {
		return this.teleportRocks;
	}

	public int getRockSize() {
		int ret = 0;
		for (int i = 0; i < 10; i++) {
			if (this.teleportRocks[i] > 0) {
				ret++;
			}
		}
		return ret;
	}

	public void deleteFromRocks(final int map) {
		for (int i = 0; i < 10; i++) {
			if (this.teleportRocks[i] == map) {
				this.teleportRocks[i] = -1;
				break;
			}
		}
	}

	public void addRockMap() {
		if (this.getRockSize() >= 10) {
			return;
		}
		this.teleportRocks[this.getRockSize()] = this.getMapId();
	}

	public boolean isRockMap(final int id) {
		for (int i = 0; i < 10; i++) {
			if (this.teleportRocks[i] == id) {
				return true;
			}
		}
		return false;
	}

	public List<LifeMovementFragment> getLastRes() {
		return this.lastMovement;
	}

	public void setLastRes(final List<LifeMovementFragment> lastres) {
		this.lastMovement = lastres;
	}

	public void setMonsterBookCover(final int bookCover) {
		this.bookCover = bookCover;
	}

	public int getMonsterBookCover() {
		return this.bookCover;
	}

	public void sendNotice(final int type, final String message) {
		this.client.write(ChannelPackets.serverNotice(type, message));
	}

	public PlayerShop getPlayerShop() {
		return this.playerShop;
	}

	public void setPlayerShop(final PlayerShop playerShop) {
		this.playerShop = playerShop;
	}

	public int getConversationState() {
		return this.conversationState.get();
	}

	public void setConversationState(final int state) {
		this.conversationState.set(state);
	}

	public CarnivalParty getCarnivalParty() {
		return this.carnivalParty;
	}

	public void setCarnivalParty(final CarnivalParty party) {
		this.carnivalParty = party;
	}

	public void addCP(final int ammount) {
		this.totalCP += ammount;
		this.availableCP += ammount;
	}

	public void useCP(final int ammount) {
		this.availableCP -= ammount;
	}

	public int getAvailableCP() {
		return this.availableCP;
	}

	public int getTotalCP() {
		return this.totalCP;
	}

	public void resetCP() {
		this.totalCP = 0;
		this.availableCP = 0;
	}

	public void addCarnivalRequest(final CarnivalChallenge request) {
		this.pendingCarnivalRequests.add(request);
	}

	public final CarnivalChallenge getNextCarnivalRequest() {
		return this.pendingCarnivalRequests.pollLast();
	}

	public void clearCarnivalRequests() {
		this.pendingCarnivalRequests = Lists.newLinkedList();
	}

	public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
		this.client.write(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
	}

	public void CPUpdate(final boolean party, final int available, final int total, final int team) {
		this.client.write(MonsterCarnivalPacket.CPUpdate(party, available, total, team));
	}

	public void playerDiedCPQ(final String name, final int lostCP, final int team) {
		this.client.write(MonsterCarnivalPacket.playerDiedMessage(name, lostCP, team));
	}

	@Override
	public int getSubcategory() {
		if (this.jobId >= 430 && this.jobId <= 434) {
			return 1; // dont set it
		}
		return this.subcategory;
	}

	public int getLinkedMonsterId() {
		return this.linkedMonsterId;
	}

	public void setLinkedMonsterId(final int lm) {
		this.linkedMonsterId = lm;
	}

	public boolean isOnDMG() {
		return this.ondmg;
	}

	public boolean isCallGM() {
		return this.callgm;
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