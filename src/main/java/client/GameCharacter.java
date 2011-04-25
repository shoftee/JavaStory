package client;

import java.awt.Point;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Deque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.Serializable;

import client.anticheat.CheatTracker;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import database.DatabaseConnection;
import database.DatabaseException;
import handling.GamePacket;
import handling.world.CharacterTransfer;
import handling.world.Messenger;
import handling.world.Party;
import handling.world.PartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffValueHolder;
import handling.world.PlayerCooldownValueHolder;
import handling.world.PlayerDiseaseValueHolder;
import handling.world.guild.GuildUnion;
import handling.world.guild.Guild;
import handling.world.guild.GuildCharacter;
import handling.world.remote.WorldChannelInterface;
import java.sql.Statement;
import java.util.TimeZone;
import org.javastory.server.channel.ChannelManager;
import scripting.EventInstanceManager;
import scripting.NpcScriptManager;
import server.Portal;
import server.Shop;
import server.StatEffect;
import server.Storage;
import server.Trade;
import server.TimerManager;
import org.javastory.tools.Randomizer;
import server.RandomRewards;
import server.CarnivalParty;
import server.ItemInfoProvider;
import server.life.Monster;
import server.maps.AbstractAnimatedGameMapObject;
import server.maps.Door;
import server.maps.GameMap;
import server.maps.GameMapFactory;
import server.maps.GameMapObject;
import server.maps.GameMapObjectType;
import server.maps.Summon;
import server.maps.FieldLimitType;
import server.maps.SavedLocationType;
import server.quest.Quest;
import server.shops.PlayerShop;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.MTSCSPacket;
import tools.packet.MobPacket;
import tools.packet.PetPacket;
import tools.packet.MonsterCarnivalPacket;
import tools.packet.UIPacket;
import server.CarnivalChallenge;
import server.InventoryManipulator;
import server.life.MobSkill;
import server.maps.Dragon;
import server.movement.LifeMovementFragment;
import org.javastory.io.PacketBuilder;
import org.javastory.server.FameLog;

public class GameCharacter extends AbstractAnimatedGameMapObject implements Serializable {

    public static int damageCap = 100000000; //Global damage cap
    public static int magicCap = 999999999;//
    public static int unlimitedSlotItem = 1082174; //Lunar Glove
    //
    private transient int linkMid = 0;
    private transient Dragon dragon;
    private transient AtomicInteger conversationState;
    private transient List<LifeMovementFragment> lastres;
    private transient Set<Monster> controlledMonsters;
    private transient Set<GameMapObject> visibleMapObjects;
    private transient Map<Integer, Summon> summons;
    private transient Map<Integer, CooldownValueHolder> cooldowns;
    private transient Map<Disease, DiseaseValueHolder> diseases;
    private transient Map<BuffStat, BuffStatValueHolder> effects;
    private transient Deque<CarnivalChallenge> pendingCarnivalRequests;
    private transient EventInstanceManager eventInstance;
    private transient CarnivalParty carnivalParty;
    private transient CheatTracker cheatTracker;
    private transient GameMap map;
    private transient Shop shop;
    private transient Trade trade;
    //
    private transient ScheduledFuture<?> fullnessSchedule, fullnessSchedule_1, fullnessSchedule_2, hpDecreaseTask;
    private transient ScheduledFuture<?> beholderHealingSchedule, beholderBuffSchedule, BerserkSchedule;
    private transient ScheduledFuture<?> dragonBloodSchedule;
    private transient ScheduledFuture<?> mapTimeLimitTask, fishing;
    //
    private boolean canDoor, Berserk, smega, hidden;
    private boolean ondmg = true, callgm = true;
    //
    private byte dojoRecord, gmLevel, gender; // Make this a quest record, TODO : Transfer it somehow with the current data
    private short level, mulung_energy, combo, availableCP, totalCP;
    public int vpoints;
    public int reborns;
    public int world;
    //
    private int accountId, id, meso, exp, jobId, rank, rankMove, jobRank,
            jobRankMove, mpApUsed, hpApUsed, hairId, faceId, skinColorId,
            remainingAp, fame, mapId, initialSpawnPoint, bookCover, dojo,
            fallcounter, maplePoints, aCash, chair, itemEffect, subcategory;
    //
    private int[] remainingSp = new int[10];
    private int[] wishlist, rocks, savedLocations;
    private long lastCombo, lastFameTime, keydown_skill;
    private String name, chalktext, BlessOfFairy_Origin;
    //
    private Map<Quest, QuestStatus> quests;
    private Map<Integer, String> questInfo;
    private Map<ISkill, SkillEntry> skills;
    private List<Door> doors;
    private List<Pet> pets;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    //
    private int guildId;
    private int guildRank;
    private GuildCharacter guildCharacter;
    //
    private GuildUnion guildUnion;
    private int guildUnionRank;
    //
    private BuddyList buddies;
    private MonsterBook monsterBook;
    private PlayerStats stats;
    private PlayerRandomStream randomStream;
    private Storage storage;
    private Mount mount;
    //
    private Messenger messenger;
    private int messengerPosition;
    //
    private PlayerShop playerShop;
    private Party party;
    private KeyLayout keylayout;
    private PlayerInventory inventory;
    private GameClient client;

    private GameCharacter() {
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
        Berserk = false;
        smega = true;
        wishlist = new int[10];
        rocks = new int[10];

        // 1 = NPC/Quest, 
        // 2 = Duey, 
        // 3 = Hired Merch store, 
        // 4 = Storage
        conversationState = new AtomicInteger();

        keylayout = new KeyLayout();
        inventory = new PlayerInventory();
        stats = new PlayerStats(this);
        cheatTracker = new CheatTracker(this);

        effects = Maps.newEnumMap(BuffStat.class);
        diseases = Maps.newEnumMap(Disease.class);
        doors = Lists.newArrayList();
        pets = Lists.newArrayList();
        pendingCarnivalRequests = Lists.newLinkedList();
        controlledMonsters = Sets.newLinkedHashSet();
        visibleMapObjects = Sets.newLinkedHashSet();
        quests = Maps.newLinkedHashMap(); // Stupid erev quest.
        summons = Maps.newLinkedHashMap();
        skills = Maps.newLinkedHashMap();
        cooldowns = Maps.newLinkedHashMap();
        questInfo = Maps.newLinkedHashMap();
        savedLocations = new int[SavedLocationType.values().length];
        for (int i = 0; i < SavedLocationType.values().length; i++) {
            savedLocations[i] = -1;
        }
    }

    public static GameCharacter getDefault(final GameClient client, final int type) {
        GameCharacter ret = new GameCharacter();
        ret.client = client;
        ret.map = null;
        ret.exp = 0;
        ret.gmLevel = 0;
        ret.jobId = type == 0 ? 1000 : (type == 1 ? 0 : (type == 2 ? 2000 : 2001));
        ret.meso = 0;
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountId = client.getAccountId();
        ret.buddies = new BuddyList(100);
        ret.stats.str = 12;
        ret.stats.dex = 4;
        ret.stats.int_ = 4;
        ret.stats.luk = 4;
        ret.stats.hp = 50;
        ret.stats.maxhp = 50;
        ret.stats.mp = 5;
        ret.stats.maxmp = 5;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.aCash = rs.getInt("ACash");
                ret.maplePoints = rs.getInt("mPoints");
                ret.vpoints = rs.getInt("vpoints");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
        }
        return ret;
    }

    public static GameCharacter reconstructCharacter(final CharacterTransfer ct, final GameClient client) {
        final GameCharacter ret = new GameCharacter();
        ret.client = client;
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.fame = ct.fame;

        ret.randomStream = new PlayerRandomStream();

        ret.stats.setStr(ct.str);
        ret.stats.setDex(ct.dex);
        ret.stats.setInt(ct.int_);
        ret.stats.setLuk(ct.luk);
        ret.stats.setMaxHp(ct.maxhp);
        ret.stats.setMaxMp(ct.maxmp);
        ret.stats.setHp(ct.hp);
        ret.stats.setMp(ct.mp);

        ret.exp = ct.exp;
        ret.hpApUsed = ct.hpApUsed;
        ret.mpApUsed = ct.mpApUsed;
        ret.remainingSp = (int[]) ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso = ct.meso;
        ret.gmLevel = ct.gmLevel;
        ret.skinColorId = ct.skinColor;
        ret.gender = ct.gender;
        ret.jobId = ct.job;
        ret.hairId = ct.hair;
        ret.faceId = ct.face;
        ret.accountId = ct.accountid;
        ret.mapId = ct.mapId;
        ret.initialSpawnPoint = ct.initialSpawnPoint;
        ret.world = ct.worldId;

        ret.rank = ct.rank;
        ret.rankMove = ct.rankMove;
        ret.jobRank = ct.jobRank;
        ret.jobRankMove = ct.jobRankMove;

        ret.bookCover = ct.monsterBookCover;
        ret.dojo = ct.dojo;
        ret.dojoRecord = ct.dojoRecord;
        ret.reborns = ct.reborns;

        ret.guildId = ct.guildId;
        ret.guildRank = ct.guildRank;

        ret.guildUnionRank = ct.unionRank;
        ret.subcategory = ct.subcategory;
        ret.ondmg = ct.ondmg;
        ret.callgm = ct.callgm;
        if (ret.guildId > 0) {
            ret.guildCharacter = new GuildCharacter(ret);
        }
        ret.buddies = new BuddyList(ct.buddyListCapacity);

        final GameMapFactory mapFactory = ChannelManager.getInstance(client.getChannelId()).getMapFactory(ret.world);
        ret.map = mapFactory.getMap(ret.mapId);
        if (ret.map == null) {
            //char is on a map that doesn't exist warp it to henesys
            ret.map = mapFactory.getMap(100000000);
        } else {
            if (ret.map.getForcedReturnId() != 999999999) {
                ret.map = ret.map.getForcedReturnMap();
            }
        }
        Portal portal = ret.map.getPortal(ret.initialSpawnPoint);
        if (portal == null) {
            portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
            ret.initialSpawnPoint = 0;
        }
        ret.setPosition(portal.getPosition());

        int partyid = ct.partyId;
        if (partyid >= 0) {
            try {
                Party party = client.getChannelServer().getWorldInterface().getParty(partyid);
                if (party != null && party.getMemberById(ret.id) != null) {
                    ret.party = party;
                }
            } catch (RemoteException e) {
                client.getChannelServer().pingWorld();
            }
        }

        final int messengerid = ct.messengerId;
        final int position = ct.messengerPosition;
        if (messengerid > 0 && position < 4 && position > -1) {
            try {
                WorldChannelInterface wci = ChannelManager.getInstance(client.getChannelId()).getWorldInterface();
                Messenger messenger = wci.getMessenger(messengerid);
                if (messenger != null) {
                    ret.messenger = messenger;
                    ret.messengerPosition = position;
                }
            } catch (RemoteException e) {
                client.getChannelServer().pingWorld();
            }
        }

        QuestStatus queststatus;
        QuestStatus queststatus_from;
        Quest quest;
        for (final Map.Entry<Integer, Object> qs : ct.quest.entrySet()) {
            quest = Quest.getInstance(qs.getKey());
            queststatus_from = (QuestStatus) qs.getValue();

            queststatus = new QuestStatus(quest, queststatus_from.getStatus());
            queststatus.setForfeited(queststatus_from.getForfeited());
            queststatus.setCustomData(queststatus_from.getCustomData());
            queststatus.setCompletionTime(queststatus_from.getCompletionTime());

            if (queststatus_from.getMobKills() != null) {
                for (final Map.Entry<Integer, Integer> mobkills : queststatus_from.getMobKills().entrySet()) {
                    queststatus.setMobKills(mobkills.getKey(), mobkills.getValue());
                }
            }
            ret.quests.put(quest, queststatus);
        }
        for (final Map.Entry<Integer, Object> qs : ct.skills.entrySet()) {
            ret.skills.put(SkillFactory.getSkill(qs.getKey()), (SkillEntry) qs.getValue());
        }
        ret.monsterBook = (MonsterBook) ct.monsterbook;
        ret.inventory = (PlayerInventory) ct.inventorys;
        ret.BlessOfFairy_Origin = ct.blessOfFairy;
        ret.skillMacros = (SkillMacro[]) ct.skillmacro;
        ret.keylayout = (KeyLayout) ct.keyLayout;
        ret.questInfo = (Map<Integer, String>) ct.questInfo;
        ret.savedLocations = (int[]) ct.savedlocation;
        ret.wishlist = (int[]) ct.wishlist;
        ret.rocks = (int[]) ct.rocks;
        ret.buddies.loadFromTransfer(ct.buddies);
        ret.keydown_skill = 0; // Keydown skill can't be brought over
        ret.lastFameTime = ct.lastFameTime;
        ret.storage = (Storage) ct.storage;
        client.setAccountName(ct.accountname);
        ret.aCash = ct.ACash;
        ret.vpoints = ct.vpoints;
        ret.maplePoints = ct.MaplePoints;

        ret.mount = new Mount(ret, ct.mount_itemid, 1004, ct.mount_Fatigue, ct.mount_level, ct.mount_exp);

        ret.stats.recalcLocalStats();
        ret.silentEnforceMaxHpMp();

        return ret;
    }

    public static GameCharacter loadFromDb(int characterId, GameClient client, boolean channelserver) {
        final GameCharacter ret = new GameCharacter();
        ret.client = client;
        ret.id = characterId;

        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, characterId);
            rs = ps.executeQuery();
            if (!rs.next()) {
                throw new RuntimeException("Loading the Char Failed (char not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getShort("level");
            ret.fame = rs.getInt("fame");

            ret.stats = new PlayerStats(ret);

            if (channelserver) {
                ret.stats.setStr(rs.getInt("str"));
                ret.stats.setDex(rs.getInt("dex"));
                ret.stats.setInt(rs.getInt("int"));
                ret.stats.setLuk(rs.getInt("luk"));
                ret.stats.setMaxHp(rs.getInt("maxhp"));
                ret.stats.setMaxMp(rs.getInt("maxmp"));
                ret.stats.setHp(rs.getInt("hp"));
                ret.stats.setMp(rs.getInt("mp"));
            } else {
                ret.stats.str = rs.getInt("str");
                ret.stats.dex = rs.getInt("dex");
                ret.stats.int_ = rs.getInt("int");
                ret.stats.luk = rs.getInt("luk");
                ret.stats.maxhp = rs.getInt("maxhp");
                ret.stats.maxmp = rs.getInt("maxmp");
                ret.stats.hp = rs.getInt("hp");
                ret.stats.mp = rs.getInt("mp");
            }

            ret.exp = rs.getInt("exp");
            ret.hpApUsed = rs.getInt("hpApUsed");
            ret.mpApUsed = rs.getInt("mpApUsed");
            final String[] sp = rs.getString("sp").split(",");

            for (int i = 0; i < ret.remainingSp.length; i++) {

                ret.remainingSp[i] = Integer.parseInt(sp[i]);

            }
            ret.remainingAp = rs.getInt("ap");
            ret.subcategory = rs.getInt("subcategory");
            ret.meso = rs.getInt("meso");
            ret.gmLevel = rs.getByte("gm");
            ret.skinColorId = rs.getInt("skincolor");
            ret.gender = rs.getByte("gender");
            ret.jobId = rs.getInt("job");
            ret.hairId = rs.getInt("hair");
            ret.faceId = rs.getInt("face");
            ret.accountId = rs.getInt("accountid");
            ret.mapId = rs.getInt("map");
            ret.initialSpawnPoint = rs.getInt("spawnpoint");
            ret.world = rs.getInt("world");
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            ret.guildId = rs.getInt("guildid");
            ret.guildRank = rs.getInt("guildrank");
            ret.guildUnionRank = rs.getInt("allianceRank");
            ret.reborns = rs.getInt("reborns");
            if (ret.guildId > 0) {
                ret.guildCharacter = new GuildCharacter(ret);
            }
            ret.buddies = new BuddyList(rs.getInt("buddyCapacity"));

            if (channelserver) {
                GameMapFactory mapFactory = ChannelManager.getInstance(client.getChannelId()).getMapFactory(ret.world);
                ret.map = mapFactory.getMap(ret.mapId);
                if (ret.map == null) {
                    //char is on a map that doesn't exist warp it to henesys
                    ret.map = mapFactory.getMap(100000000);
                }
                Portal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                    portal = ret.map.getPortal(0);
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    try {
                        Party party = client.getChannelServer().getWorldInterface().getParty(partyid);
                        if (party != null && party.getMemberById(ret.id) != null) {
                            ret.party = party;
                        }
                    } catch (RemoteException e) {
                        client.getChannelServer().pingWorld();
                    }
                }

                final int messengerid = rs.getInt("messengerid");
                final int position = rs.getInt("messengerposition");
                if (messengerid > 0 && position < 4 && position > -1) {
                    try {
                        WorldChannelInterface wci = ChannelManager.getInstance(client.getChannelId()).getWorldInterface();
                        Messenger messenger = wci.getMessenger(messengerid);
                        if (messenger != null) {
                            ret.messenger = messenger;
                            ret.messengerPosition = position;
                        }
                    } catch (RemoteException e) {
                        client.getChannelServer().pingWorld();
                    }
                }
                ret.bookCover = rs.getInt("monsterbookcover");
                ret.dojo = rs.getInt("dojo_pts");
                ret.dojoRecord = rs.getByte("dojoRecord");

            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, characterId);
            rs = ps.executeQuery();
            pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");

            while (rs.next()) {
                final int id = rs.getInt("quest");
                final Quest q = Quest.getInstance(id);
                final QuestStatus status = new QuestStatus(q, rs.getByte("status"));
                final long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                status.setCustomData(rs.getString("customData"));
                ret.quests.put(q, status);
                pse.setInt(1, rs.getInt("queststatusid"));
                final ResultSet rsMobs = pse.executeQuery();

                while (rsMobs.next()) {
                    status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                }
                rsMobs.close();
            }
            rs.close();
            ps.close();
            pse.close();

            if (channelserver) {
                ret.randomStream = new PlayerRandomStream();
                ret.monsterBook = new MonsterBook();
                ret.monsterBook.loadCards(characterId);

                ps = con.prepareStatement("SELECT * FROM inventoryslot where characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    throw new RuntimeException("No Inventory slot column found in SQL. [inventoryslot]");
                } else {
                    ret.getInventoryType(InventoryType.EQUIP).setSlotLimit(rs.getByte("equip"));
                    ret.getInventoryType(InventoryType.USE).setSlotLimit(rs.getByte("use"));
                    ret.getInventoryType(InventoryType.SETUP).setSlotLimit(rs.getByte("setup"));
                    ret.getInventoryType(InventoryType.ETC).setSlotLimit(rs.getByte("etc"));
                    ret.getInventoryType(InventoryType.CASH).setSlotLimit(rs.getByte("cash"));
                }
                ps.close();
                rs.close();

                ps = con.prepareStatement("SELECT * FROM inventoryitems LEFT JOIN inventoryequipment USING (inventoryitemid) WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();

                long expiration;
                InventoryType type;
                Item item;
                Pet pet;

                while (rs.next()) {
                    type = InventoryType.getByType(rs.getByte("inventorytype"));
                    expiration = rs.getLong("expiredate");

                    if (type.equals(InventoryType.EQUIP) ||
                            type.equals(InventoryType.EQUIPPED)) {
                        final Equip equip = new Equip(rs.getInt("itemid"), rs.getByte("position"), rs.getInt("ringid"), rs.getByte("flag"));
                        equip.setOwner(rs.getString("owner"));
                        equip.setQuantity(rs.getShort("quantity"));
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
                        equip.setExpiration(expiration);
                        equip.setGMLog(rs.getString("GM_Log"));
                        ret.getInventoryType(type).addFromDb(equip);
                    } else {
                        item = new Item(rs.getInt("itemid"), rs.getByte("position"), rs.getShort("quantity"), rs.getByte("flag"));
                        item.setOwner(rs.getString("owner"));
                        item.setExpiration(expiration);
                        item.setGMLog(rs.getString("GM_Log"));
                        ret.getInventoryType(type).addFromDb(item);

                        if (rs.getInt("petid") > -1) {
                            pet = Pet.loadFromDb(item.getItemId(), rs.getInt("petid"), item.getPosition());
                            ret.pets.add(pet);
                            item.setPet(pet);
                        }
                    }
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                ps.setInt(1, ret.accountId);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.getClient().setAccountName(rs.getString("name"));
                    ret.aCash = rs.getInt("ACash");
                    ret.vpoints = rs.getInt("vpoints");
                    ret.maplePoints = rs.getInt("mPoints");
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.questInfo.put(rs.getInt("quest"), rs.getString("customData"));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel FROM skills WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (GameConstants.isApplicableSkill(rs.getInt("skillid"))) {
                        ret.skills.put(SkillFactory.getSkill(rs.getInt("skillid")), new SkillEntry(rs.getByte("skilllevel"), rs.getByte("masterlevel")));
                    }
                }
                rs.close();
                ps.close();

                // Bless of Fairy handling
                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                ps.setInt(1, ret.accountId);
                rs = ps.executeQuery();

                while (rs.next()) {
                    if (rs.getInt("id") != characterId) { // Not this character
                        byte maxlevel = (byte) (rs.getInt("level") / 10);

                        //if (!GameConstants.isKOC(ret.job)) {
                        if (maxlevel > 20) {
                            maxlevel = 20;
                        }
                        //}
                        ret.BlessOfFairy_Origin = rs.getString("name");
                        ret.skills.put(SkillFactory.getSkill(GameConstants.getBOF_ForJob(ret.jobId)), new SkillEntry(maxlevel, (byte) 0));
                        break;
                    }
                }
                ps.close();
                rs.close();
                // END

                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();
                int position;
                while (rs.next()) {
                    position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();

                final Map<Integer, KeyBinding> keyb = ret.keylayout.Layout();
                while (rs.next()) {
                    keyb.put(Integer.valueOf(rs.getInt("key")), new KeyBinding(rs.getInt("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                }
                rs.close();
                ps.close();

                ret.lastFameTime = FameLog.getLastTimestamp(characterId);

                ret.buddies.loadFromDb(characterId);
                ret.storage = Storage.loadStorage(ret.accountId);

                ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    ret.wishlist[i] = rs.getInt("sn");
                    i++;
                }
                while (i < 10) {
                    ret.wishlist[i] = 0;
                    i++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();
                int r = 0;
                while (rs.next()) {
                    ret.rocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 10) {
                    ret.rocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("No mount data found on SQL column");
                }
                final IItem mount = ret.getInventoryType(InventoryType.EQUIPPED).getItem((byte) -22);
                ret.mount = new Mount(ret, mount != null ? mount.getItemId() : 0, 1004, rs.getInt("Fatigue"), rs.getInt("Level"), rs.getInt("Exp"));
                ps.close();
                rs.close();

                ret.stats.recalcLocalStats();
                ret.silentEnforceMaxHpMp();
            } else { // Not channel server
                ps = con.prepareStatement("SELECT * FROM inventoryitems LEFT JOIN inventoryequipment USING (inventoryitemid) WHERE characterid = ? AND inventorytype = -1");
                ps.setInt(1, characterId);
                rs = ps.executeQuery();

                InventoryType type;
                while (rs.next()) {
                    type = InventoryType.getByType(rs.getByte("inventorytype"));
                    if (type.equals(InventoryType.EQUIP) ||
                            type.equals(InventoryType.EQUIPPED)) {
                        final Equip equip = new Equip(rs.getInt("itemid"), rs.getByte("position"), rs.getInt("ringid"), rs.getByte("flag"));
                        /*		    equip.setOwner(rs.getString("owner"));
                        equip.setQuantity(rs.getShort("quantity"));
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
                        equip.setViciousHammer(rs.getByte("ViciousHammer"));
                        equip.setItemLevel(rs.getByte("itemLevel"));
                        equip.setItemEXP(rs.getShort("itemEXP"));
                        equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                        equip.setLevel(rs.getByte("level"));*/
                        ret.getInventoryType(type).addFromDb(equip);
                        /*		} else {
                        Item item = new Item(rs.getInt("itemid"), rs.getByte("position"), rs.getShort("quantity"), rs.getInt("petid"), rs.getByte("flag"));
                        item.setOwner(rs.getString("owner"));
                        ret.getInventory(type).addFromDb(item);*/
                    }
                }
                rs.close();
                ps.close();
            }
        } catch (SQLException ess) {
            ess.printStackTrace();
            System.out.println("Failed to load character..");
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignore) {
            }
        }
        return ret;
    }

    public static void saveNewCharacterToDb(final GameCharacter chr, final int type, final boolean db) {
        Connection con = DatabaseConnection.getConnection();

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            ps = con.prepareStatement("INSERT INTO characters (level, fame, str, dex, luk, `int`, exp, hp, mp, maxhp, maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, meso, hpApUsed, mpApUsed, spawnpoint, party, buddyCapacity, messengerid, messengerposition, monsterbookcover, dojo_pts, dojoRecord, accountid, name, world, reborns, subcategory) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, 1); // Level
            ps.setInt(2, 0); // Fame
            final PlayerStats stat = chr.stats;
            ps.setInt(3, stat.getStr()); // Str
            ps.setInt(4, stat.getDex()); // Dex
            ps.setInt(5, stat.getInt()); // Int
            ps.setInt(6, stat.getLuk()); // Luk
            ps.setInt(7, 0); // EXP
            ps.setInt(8, stat.getHp());
            ps.setInt(9, stat.getMp());
            ps.setInt(10, stat.getMaxHp());
            ps.setInt(11, stat.getMaxMp());
            ps.setString(12, "0,0,0,0,0,0,0,0,0,0"); // Remaining SP
            ps.setInt(13, 0); // Remaining AP
            ps.setInt(14, 0); // GM Level
            ps.setInt(15, chr.skinColorId);
            ps.setInt(16, chr.gender);
            ps.setInt(17, chr.jobId);
            ps.setInt(18, chr.hairId);
            ps.setInt(19, chr.faceId);
            ps.setInt(20, type == 1 ? 10000 : (type == 0 ? 130030000 : (type ==
                    2 ? 914000000 : 900010000))); // 0-KOC 1-Adventurer 2-Aran 3-Evan 4-DB
            ps.setInt(21, chr.meso); // Meso
            ps.setInt(22, 0); // HP ap used
            ps.setInt(23, 0); // MP ap used
            ps.setInt(24, 0); // Spawnpoint
            ps.setInt(25, -1); // Party
            ps.setInt(26, chr.buddies.getCapacity()); // Buddylist
            ps.setInt(27, 0); // MessengerId
            ps.setInt(28, 4); // Messenger Position
            ps.setInt(29, 0); // Monster book cover
            ps.setInt(30, 0); // Dojo
            ps.setInt(31, 0); // Dojo record
            ps.setInt(32, chr.getAccountId());
            ps.setString(33, chr.name);
            ps.setInt(34, chr.world);
            ps.setInt(35, chr.reborns);
            ps.setInt(36, db ? 1 : 0); //for now
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                throw new DatabaseException(":: Failed to create new character ::");
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (final QuestStatus q : chr.quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.executeUpdate();
                rs = ps.getGeneratedKeys();
                rs.next();
                if (q.hasMobKills()) {
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.executeUpdate();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();
            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setInt(2, 60); // Eq
            ps.setInt(3, 60); // Use
            ps.setInt(4, 60); // Setup
            ps.setInt(5, 60); // ETC
            ps.setInt(6, 60); // Cash
            ps.execute();
            ps.close();
            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("INSERT INTO inventoryitems (characterid, itemid, inventorytype, position, quantity, owner, GM_Log, petid, expiredate, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            IEquip equip;

            for (final Inventory iv : chr.inventory) {
                ps.setInt(3, iv.getType().asByte());
                for (final IItem item : iv.list()) {
                    ps.setInt(1, chr.id);
                    ps.setInt(2, item.getItemId());
                    ps.setInt(4, item.getPosition());
                    ps.setInt(5, item.getQuantity());
                    ps.setString(6, item.getOwner());
                    ps.setString(7, item.getGMLog());
                    ps.setInt(8, -1); // Pet cant be loaded on logins + new char doesn't have.
                    ps.setLong(9, item.getExpiration());
                    ps.setByte(10, item.getFlag());
                    ps.executeUpdate();

                    rs = ps.getGeneratedKeys();
                    int itemid;
                    if (rs.next()) {
                        itemid = rs.getInt(1);
                    } else {
                        throw new DatabaseException("Inserting char failed.");
                    }
                    rs.close();

                    if (iv.getType().equals(InventoryType.EQUIP) ||
                            iv.getType().equals(InventoryType.EQUIPPED)) {
                        pse.setInt(1, itemid);
                        equip = (IEquip) item;
                        pse.setInt(2, equip.getUpgradeSlots());
                        pse.setInt(3, equip.getLevel());
                        pse.setInt(4, equip.getStr());
                        pse.setInt(5, equip.getDex());
                        pse.setInt(6, equip.getInt());
                        pse.setInt(7, equip.getLuk());
                        pse.setInt(8, equip.getHp());
                        pse.setInt(9, equip.getMp());
                        pse.setInt(10, equip.getWatk());
                        pse.setInt(11, equip.getMatk());
                        pse.setInt(12, equip.getWdef());
                        pse.setInt(13, equip.getMdef());
                        pse.setInt(14, equip.getAcc());
                        pse.setInt(15, equip.getAvoid());
                        pse.setInt(16, equip.getHands());
                        pse.setInt(17, equip.getSpeed());
                        pse.setInt(18, equip.getJump());
                        pse.setInt(19, equip.getRingId());
                        pse.setInt(20, equip.getViciousHammer());
                        pse.setInt(21, 0);
                        pse.setInt(22, 0);
                        pse.executeUpdate();
                    }
                }
            }
            ps.close();
            pse.close();

            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.execute();
            ps.close();

            final int[] array1 = {2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 23, 25, 26, 27, 31, 34, 37, 38, 41, 44, 45, 46, 50, 57, 59, 60, 61, 62, 63, 64, 65, 8, 9, 24, 30};
            final int[] array2 = {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 4, 5, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4};
            final int[] array3 = {10, 12, 13, 18, 6, 11, 8, 5, 0, 4, 1, 19, 14, 15, 3, 17, 9, 20, 22, 50, 51, 52, 7, 53, 100, 101, 102, 103, 104, 105, 106, 16, 23, 24, 2};

            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (int i = 0; i < array1.length; i++) {
                ps.setInt(2, array1[i]);
                ps.setInt(3, array2[i]);
                ps.setInt(4, array3[i]);
                ps.execute();
            }
            ps.close();

            con.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[charsave] Error saving character data");
            try {
                con.rollback();
            } catch (SQLException ex) {
                e.printStackTrace();
                System.err.println("[charsave] Error Rolling Back");
            }
        } finally {
            try {
                if (pse != null) {
                    pse.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("[charsave] Error going back to autocommit mode");
            }
        }
    }

    public void saveToDb(boolean dc) {
        Connection con = DatabaseConnection.getConnection();

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, map = ?, meso = ?, hpApUsed = ?, mpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, messengerid = ?, messengerposition = ?, monsterbookcover = ?, dojo_pts = ?, dojoRecord = ?, reborns = ?, subcategory = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS);
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
            final StringBuilder sps = new StringBuilder();

            for (int i = 0; i < remainingSp.length; i++) {

                sps.append(remainingSp[i]);
                sps.append(",");

            }

            final String sp = sps.toString();

            ps.setString(12, sp.substring(0, sp.length() - 1));
            ps.setInt(13, remainingAp);
            ps.setInt(14, gmLevel);
            ps.setInt(15, skinColorId);
            ps.setInt(16, gender);
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
                throw new DatabaseException("Character not in database (" + id +
                        ")");
            }
            ps.close();

            for (final Pet pet : pets) {
                if (pet.getSummoned()) {
                    pet.saveToDb(); // Only save those summoned :P
                }
            }
            deleteByCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
            for (int i = 0; i < 5; i++) {
                final SkillMacro macro = skillMacros[i];
                if (macro != null) {
                    ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, id);
                    ps.setInt(2, macro.getSkill1());
                    ps.setInt(3, macro.getSkill2());
                    ps.setInt(4, macro.getSkill3());
                    ps.setString(5, macro.getName());
                    ps.setInt(6, macro.getShout());
                    ps.setInt(7, i);
                    ps.execute();
                    ps.close();
                }
            }

            deleteByCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setInt(2, getInventoryType(InventoryType.EQUIP).getSlotLimit());
            ps.setInt(3, getInventoryType(InventoryType.USE).getSlotLimit());
            ps.setInt(4, getInventoryType(InventoryType.SETUP).getSlotLimit());
            ps.setInt(5, getInventoryType(InventoryType.ETC).getSlotLimit());
            ps.setInt(6, getInventoryType(InventoryType.CASH).getSlotLimit());
            ps.execute();
            ps.close();

            deleteByCharacterId(con, "DELETE FROM inventoryitems WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO inventoryitems (characterid, itemid, inventorytype, position, quantity, owner, GM_Log, petid, expiredate, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            for (final Inventory iv : inventory) {
                ps.setInt(3, iv.getType().asByte());
                for (final IItem item : iv.list()) {
                    ps.setInt(1, id);
                    ps.setInt(2, item.getItemId());
                    ps.setInt(4, item.getPosition());
                    ps.setInt(5, item.getQuantity());
                    ps.setString(6, item.getOwner());
                    ps.setString(7, item.getGMLog());
                    ps.setInt(8, item.getPet() != null ? item.getPet().getUniqueId() : -1);
                    ps.setLong(9, item.getExpiration());
                    ps.setByte(10, item.getFlag());
                    ps.executeUpdate();

                    rs = ps.getGeneratedKeys();
                    int itemid;
                    if (rs.next()) {
                        itemid = rs.getInt(1);
                    } else {
                        throw new DatabaseException("Inserting char failed.");
                    }

                    if (iv.getType().equals(InventoryType.EQUIP) ||
                            iv.getType().equals(InventoryType.EQUIPPED)) {
                        pse.setInt(1, itemid);
                        IEquip equip = (IEquip) item;
                        pse.setInt(2, equip.getUpgradeSlots());
                        pse.setInt(3, equip.getLevel());
                        pse.setInt(4, equip.getStr());
                        pse.setInt(5, equip.getDex());
                        pse.setInt(6, equip.getInt());
                        pse.setInt(7, equip.getLuk());
                        pse.setInt(8, equip.getHp());
                        pse.setInt(9, equip.getMp());
                        pse.setInt(10, equip.getWatk());
                        pse.setInt(11, equip.getMatk());
                        pse.setInt(12, equip.getWdef());
                        pse.setInt(13, equip.getMdef());
                        pse.setInt(14, equip.getAcc());
                        pse.setInt(15, equip.getAvoid());
                        pse.setInt(16, equip.getHands());
                        pse.setInt(17, equip.getSpeed());
                        pse.setInt(18, equip.getJump());
                        pse.setInt(19, equip.getRingId());
                        pse.setInt(20, equip.getViciousHammer());
                        pse.setInt(21, equip.getItemLevel());
                        pse.setInt(22, equip.getItemEXP());
                        pse.executeUpdate();
                    }
                }
            }
            ps.close();
            pse.close();

            deleteByCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `data`) VALUES (?, ?, ?)");
            ps.setInt(1, id);
            for (final Entry<Integer, String> q : questInfo.entrySet()) {
                ps.setInt(2, q.getKey());
                ps.setString(3, q.getValue());
                ps.execute();
            }
            ps.close();

            deleteByCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, id);
            for (final QuestStatus q : quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.executeUpdate();
                rs = ps.getGeneratedKeys();
                rs.next();

                if (q.hasMobKills()) {
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.executeUpdate();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

            deleteByCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);

            for (final Entry<ISkill, SkillEntry> skill : skills.entrySet()) {
                ps.setInt(2, skill.getKey().getId());
                ps.setInt(3, skill.getValue().getCurrentLevel());
                ps.setInt(4, skill.getValue().getMasterLevel());
                ps.execute();
            }
            ps.close();

            if (dc && getAllCooldowns().size() > 0) {
                for (final PlayerCooldownValueHolder cooling : getAllCooldowns()) {
                    ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                    ps.setInt(1, getId());
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.execute();
                }
                ps.close();
            }

            deleteByCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
            ps.setInt(1, id);
            for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                if (savedLocations[savedLocationType.ordinal()] != -1) {
                    ps.setInt(2, savedLocationType.ordinal());
                    ps.setInt(3, savedLocations[savedLocationType.ordinal()]);
                    ps.execute();
                }
            }
            ps.close();

            deleteByCharacterId(con, "DELETE FROM buddies WHERE characterid = ? AND pending = 0");
            ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, 0)");
            ps.setInt(1, id);
            for (BuddyListEntry entry : buddies.getBuddies()) {
                if (entry.isVisible()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.execute();
                }
            }
            ps.close();

            ps = con.prepareStatement("UPDATE accounts SET `ACash` = ?, `mPoints` = ?, `vpoints` = ? WHERE id = ?");
            ps.setInt(1, aCash);
            ps.setInt(2, maplePoints);
            ps.setInt(3, getVPoints());
            ps.setInt(4, client.getAccountId());
            ps.execute();
            ps.close();

            if (storage != null) {
                storage.saveToDB();
            }
            keylayout.saveKeys(id);
            mount.saveMount(id);
            monsterBook.saveCards(id);

            deleteByCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
            for (int i = 0; i < getWishlistSize(); i++) {
                ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ");
                ps.setInt(1, getId());
                ps.setInt(2, wishlist[i]);
                ps.execute();
                ps.close();
            }

            deleteByCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
            for (int i = 0; i < getRockSize(); i++) {
                if (rocks[i] != 999999999) {
                    ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ");
                    ps.setInt(1, getId());
                    ps.setInt(2, rocks[i]);
                    ps.execute();
                    ps.close();
                }
            }

            con.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(GameClient.getLogMessage(this, "[charsave] Error saving character data") +
                    e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println(GameClient.getLogMessage(this, "[charsave] Error Rolling Back") +
                        e);
            }
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (pse != null) {
                    pse.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                System.err.println(GameClient.getLogMessage(this, "[charsave] Error going back to autocommit mode") +
                        e);
            }
        }
    }

    private void deleteByCharacterId(Connection con, String sql) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    public final PlayerStats getStat() {
        return stats;
    }

    public final PlayerRandomStream getRandomStream() {
        return randomStream;
    }

    public final void QuestInfoPacket(final PacketBuilder builder) {
        builder.writeAsShort(questInfo.size());

        for (final Entry<Integer, String> q : questInfo.entrySet()) {
            builder.writeAsShort(q.getKey());
            builder.writeLengthPrefixedString(q.getValue() == null ? "" : q.getValue());
        }
        builder.writeInt(0); //PQ rank and stuff
    }

    public final void updateInfoQuest(final int questid, final String data) {
        questInfo.put(questid, data);
        client.write(MaplePacketCreator.updateInfoQuest(questid, data));
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
            if (q.getStatus() == 2 && !q.isCustomQuest()) {
                i++;
            }
        }
        return i;
    }

    public final byte getQuestStatus(final int quest) {
        for (final QuestStatus q : quests.values()) {
            if (q.getQuest().getId() == quest) {
                return q.getStatus();
            }
        }
        return 0;
    }

    public final QuestStatus getQuest(final Quest quest) {
        if (!quests.containsKey(quest)) {
            return new QuestStatus(quest, (byte) 0);
        }
        return quests.get(quest);
    }

    public final QuestStatus getQuestNAdd(final Quest quest) {
        if (!quests.containsKey(quest)) {
            final QuestStatus status = new QuestStatus(quest, (byte) 0);
            quests.put(quest, status);
            return status;
        }
        return quests.get(quest);
    }

    public final void updateQuest(final QuestStatus quest) {
        quests.put(quest.getQuest(), quest);
        if (!quest.isCustomQuest()) {

            if (quest.getStatus() == 1) {
                client.write(MaplePacketCreator.startQuest(this, (short) quest.getQuest().getId(), quest.getCustomData()));
                client.write(MaplePacketCreator.updateQuestInfo(this, (short) quest.getQuest().getId(), quest.getNpc(), (byte) 8));
            } else if (quest.getStatus() == 2) {
                client.write(MaplePacketCreator.completeQuest((short) quest.getQuest().getId()));
            } else if (quest.getStatus() == 0) {
                client.write(MaplePacketCreator.forfeitQuest(this, (short) quest.getQuest().getId()));
            }
        }
    }

    public final Map<Integer, String> getInfoQuest_Map() {
        return questInfo;
    }

    public final Map<Quest, QuestStatus> getQuest_Map() {
        return quests;
    }

    public boolean isActiveBuffedValue(int skillid) {
        LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());
        for (BuffStatValueHolder value : allBuffs) {
            if (value.effect.isSkill() && value.effect.getSourceId() == skillid) {
                return true;
            }
        }
        return false;
    }

    public Integer getBuffedValue(BuffStat effect) {
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

    public boolean isBuffFrom(BuffStat stat, ISkill skill) {
        final BuffStatValueHolder value = effects.get(stat);
        if (value == null) {
            return false;
        }
        return value.effect.isSkill() && value.effect.getSourceId() ==
                skill.getId();
    }

    public int getBuffSource(BuffStat stat) {
        final BuffStatValueHolder value = effects.get(stat);
        return value == null ? -1 : value.effect.getSourceId();
    }

    public int getItemQuantity(int itemId, boolean checkEquipped) {
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
        return (getJob() > 999 && getJob() < 2000 ? 15000 : 32000);

    }

    public int getNX() {
        return aCash;
    }

    public void gainVPoints(int gainedpoints) {
        this.vpoints += gainedpoints;
    }

    public int getWorld() {
        return world;
    }

    public void setBuffedValue(BuffStat effect, int statValue) {
        final BuffStatValueHolder value = effects.get(effect);
        if (value == null) {
            return;
        }
        value.value = statValue;
    }

    public Long getBuffedStartTime(BuffStat effect) {
        final BuffStatValueHolder value = effects.get(effect);
        return value == null ? null : Long.valueOf(value.startTime);
    }

    public StatEffect getStatForBuff(BuffStat effect) {
        final BuffStatValueHolder value = effects.get(effect);
        return value == null ? null : value.effect;
    }

    private void prepareDragonBlood(final StatEffect bloodEffect) {
        if (dragonBloodSchedule != null) {
            dragonBloodSchedule.cancel(false);
        }
        dragonBloodSchedule = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                if (stats.getHp() - bloodEffect.getX() > 1) {
                    cancelBuffStats(BuffStat.DRAGONBLOOD);
                } else {
                    addHP(-bloodEffect.getX());
                    client.write(MaplePacketCreator.showOwnBuffEffect(bloodEffect.getSourceId(), 5));
                    map.broadcastMessage(GameCharacter.this, MaplePacketCreator.showBuffeffect(getId(), bloodEffect.getSourceId(), 5), false);
                }
            }
        }, 4000, 4000);
    }

    public void startFullnessSchedule(final int decrease, final Pet pet, int petSlot) {
        ScheduledFuture<?> schedule = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                int newFullness = pet.getFullness() - decrease;
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

    public void cancelFullnessSchedule(int petSlot) {
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
        client.write(MaplePacketCreator.getClock(time));

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
                InventoryManipulator.removeById(client, InventoryType.USE, 2300000, 1, false, false);

                final int randval = RandomRewards.getInstance().getFishingReward();

                switch (randval) {
                    case 0: // Meso
                        final int money = Randomizer.rand(10, 50000);
                        gainMeso(money, true);
                        client.write(UIPacket.fishingUpdate((byte) 1, money));
                        break;
                    case 1: // EXP
                        final int experi = Randomizer.nextInt(GameConstants.getExpNeededForLevel(level) /
                                200);
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

    public void registerEffect(StatEffect effect, long starttime, ScheduledFuture<?> schedule) {
        if (effect.isHide()) {
            this.hidden = true;
            map.broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood(effect);
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isBeholder()) {
            prepareBeholderEffect();
        }
        for (Pair<BuffStat, Integer> statup : effect.getStatups()) {
            effects.put(statup.getLeft(), new BuffStatValueHolder(effect, starttime, schedule, statup.getRight().intValue()));
        }
        stats.recalcLocalStats();
    }

    public List<BuffStat> getBuffStats(final StatEffect effect, final long startTime) {
        final List<BuffStat> bstats = new ArrayList<BuffStat>();

        for (Entry<BuffStat, BuffStatValueHolder> stateffect : effects.entrySet()) {
            final BuffStatValueHolder value = stateffect.getValue();
            if (value.effect.sameSource(effect) && (startTime == -1 || startTime ==
                    value.startTime)) {
                bstats.add(stateffect.getKey());
            }
        }
        return bstats;
    }

    private void deregisterBuffStats(List<BuffStat> stats) {
        List<BuffStatValueHolder> effectsToCancel = new ArrayList<BuffStatValueHolder>(stats.size());
        for (BuffStat stat : stats) {
            final BuffStatValueHolder value = effects.get(stat);
            if (value != null) {
                effects.remove(stat);
                boolean addMbsvh = true;
                for (BuffStatValueHolder contained : effectsToCancel) {
                    if (value.startTime == contained.startTime &&
                            contained.effect == value.effect) {
                        addMbsvh = false;
                    }
                }
                if (addMbsvh) {
                    effectsToCancel.add(value);
                }
                if (stat == BuffStat.SUMMON || stat == BuffStat.PUPPET ||
                        stat == BuffStat.MIRROR_TARGET) {
                    final int summonId = value.effect.getSourceId();
                    final Summon summon = summons.get(summonId);
                    if (summon != null) {
                        map.broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
                        map.removeMapObject(summon);
                        removeVisibleMapObject(summon);
                        summons.remove(summonId);
                    }
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
                } else if (stat == BuffStat.DRAGONBLOOD) {
                    if (dragonBloodSchedule != null) {
                        dragonBloodSchedule.cancel(false);
                        dragonBloodSchedule = null;
                    }
                }
            }
        }
        for (BuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
            if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).isEmpty()) {
                if (cancelEffectCancelTasks.schedule != null) {
                    cancelEffectCancelTasks.schedule.cancel(false);
                }
            }
        }
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(StatEffect effect, boolean overwrite, long startTime) {
        List<BuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            List<Pair<BuffStat, Integer>> statups = effect.getStatups();
            buffstats = new ArrayList<BuffStat>(statups.size());
            for (Pair<BuffStat, Integer> statup : statups) {
                buffstats.add(statup.getLeft());
            }
        }
        deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            // remove for all on maps
            if (!getDoors().isEmpty()) {
                final Door door = getDoors().iterator().next();
                for (final GameCharacter chr : door.getTarget().getCharacters()) {
                    door.sendDestroyData(chr.getClient());
                }
                for (final GameCharacter chr : door.getTown().getCharacters()) {
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
//	    if (effect.getSourceId() != 5221006) {
//		getMount().cancelSchedule();
//	    }
        } else if (effect.isAranCombo()) {
            combo = 0;
        }
        // check if we are still logged in o.o
        if (!overwrite) {
            cancelPlayerBuffs(buffstats);
            if (effect.isHide() &&
                    (GameCharacter) map.getMapObject(getObjectId()) != null) {
                this.hidden = false;
                map.broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);

                for (final Pet pet : pets) {
                    if (pet.getSummoned()) {
                        map.broadcastMessage(this, PetPacket.showPet(this, pet, false, false), false);
                    }
                }
            }
        }
    }

    public void cancelBuffStats(BuffStat stat) {
        List<BuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList);
    }

    public void cancelEffectFromBuffStat(BuffStat stat) {
        cancelEffect(effects.get(stat).effect, false, -1);
    }

    private void cancelPlayerBuffs(List<BuffStat> buffstats) {
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) !=
                null) {
            // are we still connected ?
            if (buffstats.contains(BuffStat.HOMING_BEACON)) {
                client.write(MaplePacketCreator.cancelHoming());
            } else {
                stats.recalcLocalStats();
                enforceMaxHpMp();
                client.write(MaplePacketCreator.cancelBuff(buffstats, buffstats.contains(BuffStat.MONSTER_RIDING)));
                map.broadcastMessage(this, MaplePacketCreator.cancelForeignBuff(getId(), buffstats), false);
            }
        }
        if (buffstats.contains(BuffStat.MONSTER_RIDING) &&
                GameConstants.isEvan(jobId) && jobId >= 2200) {
            makeDragon();
            map.spawnDragon(dragon);
        }
    }

    public void dispel() {
        if (!isHidden()) {
            final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());
            for (BuffStatValueHolder value : allBuffs) {
                if (value.effect.isSkill() && value.schedule != null &&
                        !value.effect.isMorph()) {
                    cancelEffect(value.effect, false, value.startTime);
                }
            }
        }
    }

    public void dispelSkill(int skillid) {
        final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());

        for (BuffStatValueHolder value : allBuffs) {
            if (skillid == 0) {
                if (value.effect.isSkill() && (value.effect.getSourceId() ==
                        1004 || value.effect.getSourceId() == 10001004 ||
                        value.effect.getSourceId() == 20001004 ||
                        value.effect.getSourceId() == 20011004 ||
                        value.effect.getSourceId() == 1321007 ||
                        value.effect.getSourceId() == 2121005 ||
                        value.effect.getSourceId() == 2221005 ||
                        value.effect.getSourceId() == 2311006 ||
                        value.effect.getSourceId() == 2321003 ||
                        value.effect.getSourceId() == 3111002 ||
                        value.effect.getSourceId() == 3111005 ||
                        value.effect.getSourceId() == 3211002 ||
                        value.effect.getSourceId() == 3211005 ||
                        value.effect.getSourceId() == 4111002)) {
                    cancelEffect(value.effect, false, value.startTime);
                    break;
                }
            } else {
                if (value.effect.isSkill() && value.effect.getSourceId() ==
                        skillid) {
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

        for (BuffStatValueHolder value : allBuffs) {
            cancelEffect(value.effect, false, value.startTime);
        }
    }

    public void cancelMorphs() {
        final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());

        for (BuffStatValueHolder value : allBuffs) {
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
        LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());
        for (BuffStatValueHolder value : allBuffs) {
            if (value.effect.isMorph()) {
                return value.effect.getSourceId();
            }
        }
        return -1;
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        for (PlayerBuffValueHolder value : buffs) {
            value.effect.silentApplyBuff(this, value.startTime);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        List<PlayerBuffValueHolder> ret = Lists.newArrayList();
        for (BuffStatValueHolder value : effects.values()) {
            ret.add(new PlayerBuffValueHolder(value.startTime, value.effect));
        }
        return ret;
    }

    public void cancelMagicDoor() {
        final LinkedList<BuffStatValueHolder> allBuffs = Lists.newLinkedList(effects.values());

        for (BuffStatValueHolder value : allBuffs) {
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
                    echskill.getEffect(skilllevel).applyEnergyBuff(this, true); // Infinity time
                } else {
                    Integer energyLevel = getBuffedValue(BuffStat.ENERGY_CHARGE);

                    if (energyLevel < 10000) {
                        energyLevel += (100 * targets);
                        setBuffedValue(BuffStat.ENERGY_CHARGE, energyLevel);
                        client.write(MaplePacketCreator.giveEnergyChargeTest(energyLevel));

                        if (energyLevel >= 10000) {
                            energyLevel = 10001;
                        }
                    } else if (energyLevel == 10001) {
                        echskill.getEffect(skilllevel).applyEnergyBuff(this, false); // One with time
                        energyLevel = 10002;
                    }
                }
            }
        }
    }

    public final void handleOrbgain() {
        int orbcount = getBuffedValue(BuffStat.COMBO);
        ISkill comboSkill;
        ISkill advancedComboSkill;

        switch (getJob()) {
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
        int advComboSkillLevel = getCurrentSkillLevel(advancedComboSkill);
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
            List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<BuffStat, Integer>(BuffStat.COMBO, neworbcount));
            setBuffedValue(BuffStat.COMBO, neworbcount);
            int duration = ceffect.getDuration();
            duration += (int) ((getBuffedStartTime(BuffStat.COMBO) -
                    System.currentTimeMillis()));

            client.write(MaplePacketCreator.giveBuff(1111002, duration, stat, ceffect));
            map.broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat, ceffect), false);
        }
    }

    public void handleOrbconsume() {
        ISkill comboSkill;

        switch (getJob()) {
            case 1110:
            case 1111:
                comboSkill = SkillFactory.getSkill(11111001);
                break;
            default:
                comboSkill = SkillFactory.getSkill(1111002);
                break;
        }

        StatEffect ceffect = comboSkill.getEffect(getCurrentSkillLevel(comboSkill));
        List<Pair<BuffStat, Integer>> stat = Collections.singletonList(new Pair<BuffStat, Integer>(BuffStat.COMBO, 1));
        setBuffedValue(BuffStat.COMBO, 1);
        int duration = ceffect.getDuration();
        duration += (int) ((getBuffedStartTime(BuffStat.COMBO) -
                System.currentTimeMillis()));

        client.write(MaplePacketCreator.giveBuff(1111002, duration, stat, ceffect));
        map.broadcastMessage(this, MaplePacketCreator.giveForeignBuff(getId(), stat, ceffect), false);
    }

    private void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp());
        stats.setHp(stats.getHp(), true);
    }

    private void enforceMaxHpMp() {
        List<Pair<Stat, Integer>> statups = new ArrayList<Pair<Stat, Integer>>(2);
        if (stats.getMp() > stats.getCurrentMaxMp()) {
            stats.setMp(stats.getMp());
            statups.add(new Pair<Stat, Integer>(Stat.MP, Integer.valueOf(stats.getMp())));
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp());
            statups.add(new Pair<Stat, Integer>(Stat.HP, Integer.valueOf(stats.getHp())));
        }
        if (statups.size() > 0) {
            client.write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
        }
    }

    public GameMap getMap() {
        return map;
    }

    public MonsterBook getMonsterBook() {
        return monsterBook;
    }

    public void setMap(GameMap newMap) {
        this.map = newMap;
    }

    public void setMap(int newMapId) {
        this.mapId = newMapId;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapId;
    }

    public int getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getId() {
        return id;
    }

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

    public final short getLevel() {
        return level;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

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

    public final GameClient getClient() {
        return client;
    }

    public final void setClient(final GameClient client) {
        this.client = client;
    }

    public int getExp() {
        return exp;
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSp() {
        return remainingSp[GameConstants.getSkillBook(jobId)]; //default
    }

    public int getRemainingSp(final int skillbook) {
        return remainingSp[skillbook];
    }

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

    public void setMpApUsed(int mpApUsed) {
        this.mpApUsed = mpApUsed;
    }

    public int getHpApUsed() {
        return hpApUsed;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHpApUsed(int hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public int getSkinColor() {
        return skinColorId;
    }

    public void setSkinColor(int skinColor) {
        this.skinColorId = skinColor;
    }

    public int getJob() {
        return jobId;
    }

    public int getGender() {
        return gender;
    }

    public int getHair() {
        return hairId;
    }

    public int getFace() {
        return faceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public void setHair(int hair) {
        this.hairId = hair;
    }

    public void setFace(int face) {
        this.faceId = face;
    }

    public void setFame(int fame) {
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

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[GameConstants.getSkillBook(jobId)] = remainingSp; //default
    }

    public void setRemainingSp(int remainingSp, final int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public CheatTracker getCheatTracker() {
        return cheatTracker;
    }

    public BuddyList getBuddylist() {
        return buddies;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        dropMessage(5, msg);
        final GameMap map = client.getChannelServer().getMapFactory(world).getMap(mapid);
        changeMap(map, map.getPortal(portal));
    }

    public void changeMap(final GameMap to, final Point pos) {
        changeMapInternal(to, pos, MaplePacketCreator.getWarpToMap(to, 0x81, this));
    }

    public void changeMap(final GameMap to, final Portal pto) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(to, pto.getId(), this));
    }

    private void changeMapInternal(final GameMap to, final Point pos, GamePacket warpPacket) {
        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }
        client.write(warpPacket);
        map.removePlayer(this);
        if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) !=
                null) {
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
        List<Pair<Stat, Integer>> stats = Lists.newArrayList();
        final GameCharacter chr = this;
        int total = chr.getStat().getStr() + chr.getStat().getDex() +
                chr.getStat().getLuk() + chr.getStat().getInt() +
                chr.getRemainingAp();
        total -= str;
        chr.getStat().setStr(str);
        total -= dex;
        chr.getStat().setDex(dex);
        total -= int_;
        chr.getStat().setInt(int_);
        total -= luk;
        chr.getStat().setLuk(luk);
        chr.setRemainingAp(total);
        stats.add(new Pair<Stat, Integer>(Stat.STR, str));
        stats.add(new Pair<Stat, Integer>(Stat.DEX, dex));
        stats.add(new Pair<Stat, Integer>(Stat.INT, int_));
        stats.add(new Pair<Stat, Integer>(Stat.LUK, luk));
        stats.add(new Pair<Stat, Integer>(Stat.AVAILABLEAP, total));
        client.write(MaplePacketCreator.updatePlayerStats(stats, false, chr.getJob()));
    }

    public void startHurtHp() {
        hpDecreaseTask = TimerManager.getInstance().register(new Runnable() {

            @Override
            public void run() {
                if (map.getHPDec() < 1 || !isAlive()) {
                    return;
                } else if (getInventoryType(InventoryType.EQUIPPED).findById(map.getHPDecProtect()) ==
                        null) {
                    addHP(-map.getHPDec());
                }
            }
        }, 10000);
    }

    public void changeJob(int newJob) {
        final boolean isEv = GameConstants.isEvan(jobId);
        this.jobId = (short) newJob;
        if (newJob != 0 && newJob != 1000 && newJob != 2000 && newJob != 2001) {
            if (GameConstants.isEvan(newJob)) {
                remainingSp[GameConstants.getSkillBook(newJob)] += 3;
            } else {
                remainingSp[GameConstants.getSkillBook(newJob)]++;
                if (newJob % 10 >= 2) {
                    remainingSp[GameConstants.getSkillBook(newJob)] += 2;
                }
            }
        }
        if (!isGM()) {
            if (newJob % 1000 == 100) { //first job = warrior
                resetStats(25, 4, 4, 4);
            } else if (newJob % 1000 == 200) {
                resetStats(4, 4, 20, 4);
            } else if (newJob % 1000 == 300 || newJob % 1000 == 400) {
                resetStats(4, 25, 4, 4);
            } else if (newJob % 1000 == 500) {
                resetStats(4, 20, 4, 4);
            }
        }
        client.write(MaplePacketCreator.updateSp(this, false, isEv));
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
        List<Pair<Stat, Integer>> statup = Lists.newArrayList();
        statup.add(new Pair<Stat, Integer>(Stat.MAXHP, Integer.valueOf(maxhp)));
        statup.add(new Pair<Stat, Integer>(Stat.MAXMP, Integer.valueOf(maxmp)));
        stats.recalcLocalStats();
        client.write(MaplePacketCreator.updatePlayerStats(statup, getJob()));
        map.broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 8), false);
        silentPartyUpdate();
        guildUpdate();
        if (dragon != null) {
            map.broadcastMessage(MaplePacketCreator.removeDragon(this.id));
            map.removeMapObject(dragon);
            dragon = null;
        }
        if (newJob >= 2200 && newJob <= 2218) { //make new
            if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
                cancelBuffStats(BuffStat.MONSTER_RIDING);
            }
            makeDragon();
            map.spawnDragon(dragon);
            if (newJob == 2217) {
                for (int i : Skill.evanskills1) {
                    final ISkill skil = SkillFactory.getSkill(i);
                    if (skil != null && getCurrentSkillLevel(skil) <= 0 &&
                            getMasterSkillLevel(skil) <= 0) {
                        changeSkillLevel(skil, (byte) skil.getMaxLevel(), (byte) skil.getMaxLevel());
                    }
                }
            } else if (newJob == 2218) {
                for (int i : Skill.evanskills2) {
                    final ISkill skil = SkillFactory.getSkill(i);
                    if (skil != null && getCurrentSkillLevel(skil) <= 0 &&
                            getMasterSkillLevel(skil) <= 0) {
                        changeSkillLevel(skil, (byte) skil.getMaxLevel(), (byte) skil.getMaxLevel());
                    }
                }
            }

        } else if (newJob >= 431 && newJob <= 434) { //master skills
            for (int i : Skill.skills) {
                final ISkill skil = SkillFactory.getSkill(i);
                if (skil != null && getCurrentSkillLevel(skil) <= 0 &&
                        getMasterSkillLevel(skil) <= 0) {
                    changeSkillLevel(skil, (byte) 0, (byte) skil.getMasterLevel());
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

    public void gainAp(int ap) {
        this.remainingAp += ap;
        updateSingleStat(Stat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[GameConstants.getSkillBook(jobId)] += sp; //default
        client.write(MaplePacketCreator.updateSp(this, false));
        client.write(UIPacket.getSPMsg((byte) sp));
    }

    public void changeSkillLevel(final ISkill skill, byte newLevel, byte newMasterlevel) {
        if (skill == null || (!GameConstants.isApplicableSkill(skill.getId()) &&
                !GameConstants.isApplicableSkill_(skill.getId()))) {

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
        if (GameConstants.isRecoveryIncSkill(skill.getId())) {
            stats.relocHeal();
        } else if (GameConstants.isElementAmp_Skill(skill.getId())) {
            stats.recalcLocalStats();
        }
        client.write(MaplePacketCreator.updateSkill(skill.getId(), newLevel, newMasterlevel));
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
                InventoryManipulator.removeById(client, InventoryType.CASH, 5130000, 1, true, false);

                charms--;
                if (charms > 0xFF) {
                    charms = 0xFF;
                }
                client.write(MTSCSPacket.useCharm((byte) charms, (byte) 0));
            } else {
                float diepercentage = 0.0f;
                int expforlevel = GameConstants.getExpNeededForLevel(level);
                if (map.isTown() ||
                        FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
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
                int v10 = (int) (exp - (long) ((double) expforlevel *
                        diepercentage));
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
            for (PartyCharacter partychar : party.getMembers()) {
                if (partychar.getMapid() == getMapId() && partychar.getChannel() ==
                        channel) {
                    final GameCharacter other = ChannelManager.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().write(MaplePacketCreator.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        int channel = client.getChannelId();
        for (PartyCharacter partychar : party.getMembers()) {
            if (partychar.getMapid() == getMapId() && partychar.getChannel() ==
                    channel) {
                GameCharacter other = ChannelManager.getInstance(channel).getPlayerStorage().getCharacterByName(partychar.getName());
                if (other != null) {
                    client.write(MaplePacketCreator.updatePartyMemberHP(other.getId(), other.getStat().getHp(), other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp then directly does a updateSingleStat.
     *
     * @see GameCharacter#setHp(int)
     * @param delta
     */
    public void addHP(int delta) {
        if (stats.setHp(stats.getHp() + delta)) {
            updateSingleStat(Stat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp then directly does a updateSingleStat.
     *
     * @see GameCharacter#setMp(int)
     * @param delta
     */
    public void addMP(int delta) {
        if (stats.setMp(stats.getMp() + delta)) {
            updateSingleStat(Stat.MP, stats.getMp());
        }
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        List<Pair<Stat, Integer>> statups = new ArrayList<Pair<Stat, Integer>>();

        if (stats.setHp(stats.getHp() + hpDiff)) {
            statups.add(new Pair<Stat, Integer>(Stat.HP, Integer.valueOf(stats.getHp())));
        }
        if (stats.setMp(stats.getMp() + mpDiff)) {
            statups.add(new Pair<Stat, Integer>(Stat.MP, Integer.valueOf(stats.getMp())));
        }
        if (statups.size() > 0) {
            client.write(MaplePacketCreator.updatePlayerStats(statups, getJob()));
        }
    }

    public void updateSingleStat(Stat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    /**
     * Updates a single stat of this GameCharacter for the client. This method only creates and sends an update packet,
     * it does not update the stat stored in this GameCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(Stat stat, int newval, boolean itemReaction) {
        if (stat == Stat.AVAILABLESP) {
            client.write(MaplePacketCreator.updateSp(this, itemReaction, false));
            return;
        }
        Pair<Stat, Integer> statpair = new Pair<Stat, Integer>(stat, Integer.valueOf(newval));
        client.write(MaplePacketCreator.updatePlayerStats(Collections.singletonList(statpair), itemReaction, getJob()));
    }

    public void gainExp(final int total, final boolean show, final boolean inChat, final boolean white) {
        if (level == 200 || (GameConstants.isKOC(jobId) && level == 120)) {
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
                client.write(MaplePacketCreator.GainEXP_Others(total, inChat, white));
            }
        }
    }

    public void gainExpMonster(final int gain, final boolean show, final boolean white, final byte partyMembers, final int CLASS_EXP) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getDefault());
        int day = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int eventExp = 0;
        int weddingExp = 0;
        int partyRingExp = 0;
        int partyExp = 0;
        int premiumExp = 0;
        int itemExp = 0;
        int rainbowExp = 0;

        int baseExp = gain;
        if ((haveItem(5210006, 1, false, true) && hour > 22 && hour < 2) ||
                (haveItem(5210007, 1, false, true) && hour > 2 && hour < 6) ||
                (haveItem(5210008, 1, false, true) && hour > 6 && hour < 10) ||
                (haveItem(5210009, 1, false, true) && hour > 10 && hour < 14) ||
                (haveItem(5210010, 1, false, true) && hour > 14 && hour < 18) ||
                (haveItem(5210011, 1, false, true) && hour > 18 && hour < 22)) {
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
        if (level == 200 || (GameConstants.isKOC(jobId) && level == 120)) {
            final int needed = GameConstants.getExpNeededForLevel(level);
            if (exp + baseExp > needed) {
                setExp(needed);
            } else {
                exp += baseExp + eventExp + weddingExp + partyRingExp +
                        partyExp + premiumExp + itemExp + rainbowExp +
                        CLASS_EXP;
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
                exp += baseExp + eventExp + weddingExp + partyRingExp +
                        partyExp + premiumExp + itemExp + rainbowExp +
                        CLASS_EXP;
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
                client.write(MaplePacketCreator.GainEXP_Monster(baseExp, white, eventExp, weddingExp, partyRingExp, partyExp, premiumExp, itemExp, rainbowExp, CLASS_EXP));
            }
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            try {
                client.getChannelServer().getWorldInterface().updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new PartyCharacter(this));
            } catch (RemoteException e) {
                System.err.println("REMOTE THROW, silentPartyUpdate" + e);
                client.getChannelServer().pingWorld();
            }
        }
    }

    public boolean isGM() {
        return gmLevel > 0;
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public final Inventory getInventoryType(InventoryType type) {
        return inventory.get(type);
    }

    public final PlayerInventory getInventories() {
        return inventory;
    }

    public final void expirationTask() {
        long expiration;
        final long currenttime = System.currentTimeMillis();
        final List<IItem> toberemove = new ArrayList<IItem>(); // This is here to prevent deadlock.

        for (final Inventory inv : inventory) {
            for (final IItem item : inv.list()) {
                expiration = item.getExpiration();

                if (expiration != -1 && !GameConstants.isPet(item.getItemId())) {
                    byte flag = item.getFlag();

                    if (ItemFlag.LOCK.check(flag)) {
                        if (currenttime > expiration) {
                            item.setExpiration(-1);
                            item.setFlag((byte) (flag - ItemFlag.LOCK.getValue()));
                            client.write(MaplePacketCreator.updateSpecialItemUse(item, item.getType()));
                        }
                    } else if (currenttime > expiration) {
                        client.write(MTSCSPacket.itemExpired(item.getItemId()));
                        toberemove.add(item);
                    }
                }
            }
            for (final IItem item : toberemove) {
                InventoryManipulator.removeFromSlot(client, inv.getType(), item.getPosition(), item.getQuantity(), false);
            }
        }
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    public int getMeso() {
        return meso;
    }

    public final int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.ordinal()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = getMapId();
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.ordinal()] = -1;
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions) {
        gainMeso(gain, show, enableActions, false);
    }

    public void gainMeso(int gain, boolean show, boolean enableActions, boolean inChat) {
        if (meso + gain < 0) {
            client.write(MaplePacketCreator.enableActions());
            return;
        }
        meso += gain;
        updateSingleStat(Stat.MESO, meso, enableActions);
        if (show) {
            client.write(MaplePacketCreator.showMesoGain(gain, inChat));
        }
    }

    public void controlMonster(Monster monster, boolean aggro) {
        monster.setController(this);
        controlledMonsters.add(monster);
        client.write(MobPacket.controlMonster(monster, false, aggro));
    }

    public void stopControllingMonster(Monster monster) {
        controlledMonsters.remove(monster);
    }

    public void checkMonsterAggro(Monster monster) {
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
        try {
            for (QuestStatus q : quests.values()) {
                if (q.getStatus() != 1 || !q.hasMobKills()) {
                    continue;
                }
                if (q.mobKilled(id)) {
                    client.write(MaplePacketCreator.updateQuestMobKills(q));
                    if (q.getQuest().canComplete(this, null)) {
                        client.write(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
                    }
                }
            }
        } catch (NullPointerException e) {
        }
    }

    public final List<QuestStatus> getStartedQuests() {
        List<QuestStatus> ret = new LinkedList<QuestStatus>();
        for (QuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustomQuest()) {
                ret.add(q);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public final List<QuestStatus> getCompletedQuests() {
        List<QuestStatus> ret = new LinkedList<QuestStatus>();
        for (QuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustomQuest()) {
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
        if (GameConstants.isKOC(jobId)) {
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
        } else if ((jobId >= 300 && jobId <= 322) ||
                (jobId >= 400 && jobId <= 434) ||
                (jobId >= 1300 && jobId <= 1311) ||
                (jobId >= 1400 && jobId <= 1411)) {
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
                final IItem medal = getInventoryType(InventoryType.EQUIPPED).getItem((byte) -46);
                if (medal != null) { // Medal
                    sb.append("<");
                    sb.append(ItemInfoProvider.getInstance().getName(medal.getItemId()));
                    sb.append("> ");
                }
                sb.append(getName());
                sb.append(" has achieved Level 200. Let us Celebrate! Maplers!");
                client.getChannelServer().getWorldInterface().broadcastMessage(MaplePacketCreator.serverNotice(6, sb.toString()).getBytes());
            } catch (RemoteException e) {
                client.getChannelServer().pingWorld();
            }
        }
        maxhp = Math.min(30000, maxhp);
        maxmp = Math.min(30000, maxmp);
        final List<Pair<Stat, Integer>> statup = new ArrayList<Pair<Stat, Integer>>(8);
        statup.add(new Pair<Stat, Integer>(Stat.MAXHP, maxhp));
        statup.add(new Pair<Stat, Integer>(Stat.MAXMP, maxmp));
        statup.add(new Pair<Stat, Integer>(Stat.HP, maxhp));
        statup.add(new Pair<Stat, Integer>(Stat.MP, maxmp));
        statup.add(new Pair<Stat, Integer>(Stat.EXP, exp));
        statup.add(new Pair<Stat, Integer>(Stat.LEVEL, (int) level));
        if (jobId != 0 && jobId != 1000 && jobId != 2000 && jobId != 2001) { // Not Beginner, Nobless and Legend
            remainingSp[GameConstants.getSkillBook(jobId)] += 3;
            client.write(MaplePacketCreator.updateSp(this, false));
        } else {
            if (level <= 10) {
                stats.setStr(stats.getStr() + remainingAp);
                remainingAp = 0;
                statup.add(new Pair<Stat, Integer>(Stat.STR, stats.getStr()));
            }
        }
        statup.add(new Pair<Stat, Integer>(Stat.AVAILABLEAP, remainingAp));
        stats.setMaxHp(maxhp);
        stats.setMaxMp(maxmp);
        stats.setHp(maxhp);
        stats.setMp(maxmp);
        client.write(MaplePacketCreator.updatePlayerStats(statup, getJob()));
        map.broadcastMessage(this, MaplePacketCreator.showForeignEffect(getId(), 0), false);
        stats.recalcLocalStats();
        silentPartyUpdate();
        guildUpdate();
        NpcScriptManager.getInstance().start(getClient(), 9105010); // Vavaan
    }

    public void changeKeybinding(int key, KeyBinding keybinding) {
        if (keybinding.getType() != 0) {
            keylayout.Layout().put(Integer.valueOf(key), keybinding);
        } else {
            keylayout.Layout().remove(Integer.valueOf(key));
        }
    }

    public void sendMacros() {
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                client.write(MaplePacketCreator.getMacros(skillMacros));
                break;
            }
        }
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }

    public void temporaryBan(String reason, Calendar duration, int greason, boolean IPMac) {
        if (IPMac) {
            client.banMacs();
        }

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            ps.setString(1, client.getSessionIP());
            ps.execute();
            ps.close();

            client.disconnect(true);

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp timestamp = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, timestamp);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountId);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        }

    }

    public final boolean ban(String reason, boolean IPMac, boolean autoban) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setInt(3, accountId);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
            return false;
        }
        return true;
    }

    /**
     * Oid of players is always = the cid
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public Storage getStorage() {
        return storage;
    }

    public void addVisibleMapObject(GameMapObject mo) {
        visibleMapObjects.add(mo);
    }

    public void removeVisibleMapObject(GameMapObject mo) {
        visibleMapObjects.remove(mo);
    }

    public boolean isMapObjectVisible(GameMapObject mo) {
        return visibleMapObjects.contains(mo);
    }

    public Collection<GameMapObject> getVisibleMapObjects() {
        return Collections.unmodifiableCollection(visibleMapObjects);
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    @Override
    public void sendDestroyData(GameClient client) {
        client.write(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    @Override
    public void sendSpawnData(GameClient client) {
        if (!isHidden()) {
            client.write(MaplePacketCreator.spawnPlayerMapobject(this));

            for (final Pet pet : pets) {
                if (pet.getSummoned()) {
                    client.write(PetPacket.showPet(this, pet, false, false));
                }
            }
            if (dragon != null) {
                client.write(MaplePacketCreator.spawnDragon(dragon));
            }
        }
    }

    public void setDragon(Dragon d) {
        this.dragon = d;
    }

    public final void equipChanged() {
        map.broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
        stats.recalcLocalStats();
        enforceMaxHpMp();
        if (client.getPlayer().getMessenger() != null) {
            WorldChannelInterface wci = ChannelManager.getInstance(client.getChannelId()).getWorldInterface();
            try {
                wci.updateMessenger(client.getPlayer().getMessenger().getId(), client.getPlayer().getName(), client.getChannelId());
            } catch (final RemoteException e) {
                client.getChannelServer().pingWorld();
            }
        }
    }

    public final Pet getPet(final int index) {
        byte count = 0;
        for (final Pet pet : pets) {
            if (pet.getSummoned()) {
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

    public void removePet(Pet pet, boolean shiftLeft) {
        pet.setSummoned(false);
        /*	int slot = -1;
        for (int i = 0; i < 3; i++) {
        if (pets[i] != null) {
        if (pets[i].getUniqueId() == pet.getUniqueId()) {
        pets[i] = null;
        slot = i;
        break;
        }
        }
        }
        if (shiftLeft) {
        if (slot > -1) {
        for (int i = slot; i < 3; i++) {
        if (i != 2) {
        pets[i] = pets[i + 1];
        } else {
        pets[i] = null;
        }
        }
        }
        }*/
    }

    public final byte getPetIndex(final Pet petz) {
        byte count = 0;
        for (final Pet pet : pets) {
            if (pet.getSummoned()) {
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
            if (pet.getSummoned()) {
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

    public void unequipPet(Pet pet, boolean shiftLeft, boolean hunger) {
        cancelFullnessSchedule(getPetIndex(pet));
        pet.saveToDb();
        map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger), true);
        List<Pair<Stat, Integer>> petStat = Lists.newArrayList();
        petStat.add(new Pair<Stat, Integer>(Stat.PET, Integer.valueOf(0)));
        client.write(PetPacket.petStatUpdate(this));
        client.write(MaplePacketCreator.enableActions());
        removePet(pet, shiftLeft);
    }

    /*    public void shiftPetsRight() {
    if (pets[2] == null) {
    pets[2] = pets[1];
    pets[1] = pets[0];
    pets[0] = null;
    }
    }*/
    public final long getLastFameTime() {
        return lastFameTime;
    }

    public final void setLastFameTime(long timestamp) {
        this.lastFameTime = timestamp;
    }

    public final boolean hasFamedToday() {
        long day = (long) Math.floor(lastFameTime / 86400000.0);
        long today = (long) Math.floor(System.currentTimeMillis() / 86400000.0);
        return day < today;
    }

    public final KeyLayout getKeyLayout() {
        return this.keylayout;
    }

    public Party getParty() {
        return party;
    }

    public int getPartyId() {
        return (party != null ? party.getId() : -1);
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public void setParty(Party party) {
        this.party = party;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(Door door) {
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
            dropMessage(5, "You have set megaphone to disabled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
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

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal();
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public GameMapObjectType getType() {
        return GameMapObjectType.PLAYER;
    }

    public int getGuildId() {
        return guildId;
    }

    public int getGuildRank() {
        return guildRank;
    }

    public void setGuildId(int _id) {
        guildId = _id;
        if (guildId > 0) {
            if (guildCharacter == null) {
                guildCharacter = new GuildCharacter(this);
            } else {
                guildCharacter.setGuildId(guildId);
            }
        } else {
            guildCharacter = null;
        }
    }

    public void setGuildRank(int _rank) {
        guildRank = _rank;
        if (guildCharacter != null) {
            guildCharacter.setGuildRank(_rank);
        }
    }

    public GuildCharacter getMGC() {
        return guildCharacter;
    }

    public void setGuildUnionRank(int rank) {
        guildUnionRank = rank;
    }

    public int getGuildUnionRank() {
        return guildUnionRank;
    }

    public Guild getGuild() {
        try {
            return client.getChannelServer().getWorldInterface().getGuild(getGuildId(), null);
        } catch (RemoteException e) {
            client.getChannelServer().pingWorld();
        }
        return null;
    }

    public void guildUpdate() {
        if (guildId <= 0) {
            return;
        }
        guildCharacter.setLevel((short) level);
        guildCharacter.setJobId(jobId);

        try {
            client.getChannelServer().getWorldInterface().memberLevelJobUpdate(guildCharacter);
        } catch (RemoteException re) {
            System.err.println("RemoteExcept while trying to update level/job in guild." +
                    re);
        }
    }

    public void setReborns(int reborns) {
        this.reborns = reborns;
    }

    public void saveGuildStatus() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ? WHERE id = ?");
            ps.setInt(1, guildId);
            ps.setInt(2, guildRank);
            ps.setInt(3, id);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            System.err.println("SQL error: " + se.getLocalizedMessage() + se);
        }
    }

    public GuildUnion getGuildUnion() {
        return guildUnion;
    }

    public void modifyCSPoints(int type, int quantity, boolean show) {
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
                dropMessage(5, "You have gained " + quantity + " cash.");
                client.write(MaplePacketCreator.showSpecialEffect(19));
            }
        } else {
            dropMessage(5, "You have reached the maximum ammount of @cash");
        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return aCash;
            case 2:
                return maplePoints;
            default:
                return 0;
        }
    }

    public final boolean haveItem(int itemId, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        int count = inventory.get(GameConstants.getInventoryType(itemId)).countById(itemId);
        if (checkEquipped) {
            count += inventory.get(InventoryType.EQUIPPED).countById(itemId);
        }
        if (greaterOrEquals) {
            return count >= quantity;
        } else {
            return count == quantity;
        }
    }

    public void setLevel(int level) {
        this.level = (short) level;
    }

    public int getSkillLevel(int skill) {
        SkillEntry ret = skills.get(SkillFactory.getSkill(skill));
        if (ret == null) {
            return 0;
        }
        return ret.getCurrentLevel();
    }

    public static enum FameStatus {

        OK,
        NOT_TODAY,
        NOT_THIS_MONTH
    }

    public int getBuddyCapacity() {
        return buddies.getCapacity();
    }

    public void setBuddyCapacity(int capacity) {
        buddies.setCapacity(capacity);
        client.write(MaplePacketCreator.updateBuddyCapacity(capacity));
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public void setMessenger(Messenger messenger) {
        this.messenger = messenger;
    }

    public int getMessengerPosition() {
        return messengerPosition;
    }

    public void setMessengerPosition(int position) {
        this.messengerPosition = position;
    }

    public void addCooldown(int skillId, long startTime, long length, ScheduledFuture<?> timer) {
        cooldowns.put(Integer.valueOf(skillId), new CooldownValueHolder(skillId, startTime, length, timer));
    }

    public void removeCooldown(int skillId) {
        if (cooldowns.containsKey(Integer.valueOf(skillId))) {
            cooldowns.remove(Integer.valueOf(skillId));
        }
    }

    public boolean isInCooldown(int skillId) {
        return cooldowns.containsKey(Integer.valueOf(skillId));
    }

    public void giveCooldowns(final int skillid, long starttime, long length) {
        int time = (int) ((length + starttime) - System.currentTimeMillis());
        ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, skillid), time);
        addCooldown(skillid, System.currentTimeMillis(), time, timer);
    }

    public void giveCooldowns(final List<PlayerCooldownValueHolder> cooldowns) {
        int time;
        if (cooldowns != null) {
            for (PlayerCooldownValueHolder cooldown : cooldowns) {
                time = (int) ((cooldown.length + cooldown.startTime) -
                        System.currentTimeMillis());
                ScheduledFuture<?> timer = TimerManager.getInstance().schedule(new CancelCooldownAction(this, cooldown.skillId), time);
                addCooldown(cooldown.skillId, System.currentTimeMillis(), time, timer);
            }
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
                ps.setInt(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("length") + rs.getLong("StartTime") -
                            System.currentTimeMillis() <= 0) {
                        continue;
                    }
                    giveCooldowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                }
                deleteByCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");

            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            }
        }
    }

    public List<PlayerCooldownValueHolder> getAllCooldowns() {
        List<PlayerCooldownValueHolder> ret = new ArrayList<PlayerCooldownValueHolder>();
        for (CooldownValueHolder mcdvh : cooldowns.values()) {
            ret.add(new PlayerCooldownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public final List<PlayerDiseaseValueHolder> getAllDiseases() {
        final List<PlayerDiseaseValueHolder> ret = new ArrayList<PlayerDiseaseValueHolder>(5);

        DiseaseValueHolder vh;
        for (Entry<Disease, DiseaseValueHolder> disease : diseases.entrySet()) {
            vh = disease.getValue();
            ret.add(new PlayerDiseaseValueHolder(disease.getKey(), vh.startTime, vh.length));
        }
        return ret;
    }

    public final boolean hasDisease(final Disease dis) {
        for (final Disease disease : diseases.keySet()) {
            if (disease == dis) {
                return true;
            }
        }
        return false;
    }

    public void giveDebuff(final Disease disease, MobSkill skill) {
        final List<Pair<Disease, Integer>> debuff = Collections.singletonList(new Pair<Disease, Integer>(disease, Integer.valueOf(skill.getX())));

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
            client.write(MaplePacketCreator.giveDebuff(debuff, skill));
            map.broadcastMessage(this, MaplePacketCreator.giveForeignDebuff(id, debuff, skill), false);
        }
    }

    public final void giveSilentDebuff(final List<PlayerDiseaseValueHolder> ld) {
        if (ld != null) {
            for (final PlayerDiseaseValueHolder disease : ld) {

                TimerManager.getInstance().schedule(new Runnable() {

                    @Override
                    public void run() {
                        dispelDebuff(disease.disease);
                    }
                }, (disease.length + disease.startTime) -
                        System.currentTimeMillis());

                diseases.put(disease.disease, new DiseaseValueHolder(disease.startTime, disease.length));
            }
        }
    }

    public void dispelDebuff(Disease debuff) {
        if (hasDisease(debuff)) {
            long mask = debuff.getValue();
            client.write(MaplePacketCreator.cancelDebuff(mask));
            map.broadcastMessage(this, MaplePacketCreator.cancelForeignDebuff(id, mask), false);

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

    public void sendNote(String to, String msg) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO notes (`to`, `from`, `message`, `timestamp`) VALUES (?, ?, ?, ?)");
            ps.setString(1, to);
            ps.setString(2, getName());
            ps.setString(3, msg);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to send note" + e);
        }
    }

    public void showNote() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, getName());
            ResultSet rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.write(MTSCSPacket.showNotes(rs, count));
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        }
    }

    public void deleteNote(int id) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        }
    }

    public void mulung_EnergyModify(boolean inc) {
        if (inc) {
            if (mulung_energy + 100 > 10000) {
                mulung_energy = 10000;
            } else {
                mulung_energy += 100;
            }
        } else {
            mulung_energy = 0;
        }
        client.write(MaplePacketCreator.MulungEnergy(mulung_energy));
    }

    public void writeMulungEnergy() {
        client.write(MaplePacketCreator.MulungEnergy(mulung_energy));
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

        ISkill BerserkX = SkillFactory.getSkill(1320006);
        final int skilllevel = getCurrentSkillLevel(BerserkX);
        if (skilllevel >= 1) {
            StatEffect ampStat = BerserkX.getEffect(skilllevel);

            if (stats.getHp() * 100 / stats.getMaxHp() > ampStat.getX()) {
                Berserk = false;
            } else {
                Berserk = true;
            }

            client.write(MaplePacketCreator.showOwnBerserk(skilllevel, Berserk));
            map.broadcastMessage(this, MaplePacketCreator.showBerserk(getId(), skilllevel, Berserk), false);

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
        ISkill bHealing = SkillFactory.getSkill(1320008);
        int bHealingLvl = getCurrentSkillLevel(bHealing);
        if (bHealingLvl > 0) {
            final StatEffect healEffect = bHealing.getEffect(bHealingLvl);
            int healInterval = healEffect.getX() * 1000;
            beholderHealingSchedule = TimerManager.getInstance().register(new Runnable() {

                @Override
                public void run() {
                    addHP(healEffect.getHp());
                    client.write(MaplePacketCreator.showOwnBuffEffect(1321007, 2));
                    map.broadcastMessage(GameCharacter.this, MaplePacketCreator.summonSkill(getId(), 1321007, 5), true);
                    map.broadcastMessage(GameCharacter.this, MaplePacketCreator.showBuffeffect(getId(), 1321007, 2), false);
                }
            }, healInterval, healInterval);
        }
        ISkill bBuff = SkillFactory.getSkill(1320009);
        int bBuffLvl = getCurrentSkillLevel(bBuff);
        if (bBuffLvl > 0) {
            final StatEffect buffEffect = bBuff.getEffect(bBuffLvl);
            int buffInterval = buffEffect.getX() * 1000;
            beholderBuffSchedule = TimerManager.getInstance().register(new Runnable() {

                @Override
                public void run() {
                    buffEffect.applyTo(GameCharacter.this);
                    client.write(MaplePacketCreator.showOwnBuffEffect(1321007, 2));
                    map.broadcastMessage(GameCharacter.this, MaplePacketCreator.summonSkill(getId(), 1321007, (int) (Math.random() *
                            3) + 6), true);
                    map.broadcastMessage(GameCharacter.this, MaplePacketCreator.showBuffeffect(getId(), 1321007, 2), false);
                }
            }, buffInterval, buffInterval);
        }
    }

    public void setChalkboard(String text) {
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
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = -1;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        rocks[getRockSize()] = getMapId();
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public List<LifeMovementFragment> getLastRes() {
        return lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }

    public void setMonsterBookCover(int bookCover) {
        this.bookCover = bookCover;
    }

    public int getMonsterBookCover() {
        return bookCover;
    }

    public void dropMessage(int type, String message) {
        client.write(MaplePacketCreator.serverNotice(type, message));
    }

    public PlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(PlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversationState() {
        return conversationState.get();
    }

    public void setConversationState(int state) {
        this.conversationState.set(state);
    }

    public CarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(CarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
    }

    public void useCP(int ammount) {
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

    public void addCarnivalRequest(CarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final CarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<CarnivalChallenge>();
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

    public int getSubcategory() {
        if (jobId >= 430 && jobId <= 434) {
            return 1; //dont set it
        }
        return subcategory;
    }

    public int getLinkMid() {
        return linkMid;
    }

    public void setLinkMid(int lm) {
        this.linkMid = lm;
    }

    public boolean isOnDMG() {
        return ondmg;
    }

    public boolean isCallGM() {
        return callgm;
    }

    public void setCallGM(boolean b) {
        this.callgm = b;
    }

    public void setOnDMG(boolean b) {
        this.ondmg = b;
    }
}