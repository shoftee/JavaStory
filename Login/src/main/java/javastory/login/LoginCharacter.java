package javastory.login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javastory.client.GameCharacter;
import javastory.db.Database;
import javastory.db.DatabaseException;
import javastory.game.Equip;
import javastory.game.Gender;
import javastory.game.IEquip;
import javastory.game.IItem;
import javastory.game.Inventory;
import javastory.game.InventoryType;
import javastory.game.PlayerStats;
import javastory.game.Skills;

import com.google.common.collect.Lists;

/**
 *
 * @author shoftee
 */
public class LoginCharacter implements GameCharacter {

    private int id;
    private String name;
    private int level;
    private int fame;
    private int exp;
    private byte gmLevel;
    private Gender gender;
    private int worldId;
    //
    private final PlayerStats stats;
    //
    private int accountId, meso, jobId, hairId, faceId, skinColorId,
            remainingAp, mapId, initialSpawnPoint, subcategory;
    //
    private int worldRank, worldRankMove, jobRank, jobRankMove;
    private final int[] remainingSp;
    //
    private final Inventory equips;

    private LoginCharacter() {
        this.stats = new PlayerStats();
        this.equips = new Inventory(InventoryType.EQUIPPED);
        this.remainingSp = new int[10];
    }

    private static LoginCharacter loadFromRecord(final ResultSet charResultSet) {
        final LoginCharacter character = new LoginCharacter();
        PreparedStatement selectEquips = null;
        ResultSet equipsResultSet = null;
        try {
            character.id = charResultSet.getInt("id");
            character.name = charResultSet.getString("name");
            character.level = charResultSet.getShort("level");
            character.fame = charResultSet.getInt("fame");

            character.stats.STR = charResultSet.getInt("str");
            character.stats.DEX = charResultSet.getInt("dex");
            character.stats.INT = charResultSet.getInt("int");
            character.stats.LUK = charResultSet.getInt("luk");
            character.stats.MaxHP = charResultSet.getInt("maxhp");
            character.stats.MaxMP = charResultSet.getInt("maxmp");
            character.stats.HP = charResultSet.getInt("hp");
            character.stats.MP = charResultSet.getInt("mp");

            character.exp = charResultSet.getInt("exp");
            final String[] sp = charResultSet.getString("sp").split(",");
            for (int i = 0; i < character.remainingSp.length; i++) {
                character.remainingSp[i] = Integer.parseInt(sp[i]);
            }
            character.remainingAp = charResultSet.getInt("ap");
            character.subcategory = charResultSet.getInt("subcategory");
            character.meso = charResultSet.getInt("meso");
            character.gmLevel = charResultSet.getByte("gm");
            character.skinColorId = charResultSet.getInt("skincolor");
            character.gender = Gender.fromNumber(charResultSet.getByte("gender"));
            character.jobId = charResultSet.getInt("job");
            character.hairId = charResultSet.getInt("hair");
            character.faceId = charResultSet.getInt("face");
            character.mapId = charResultSet.getInt("map");
            character.initialSpawnPoint = charResultSet.getInt("spawnpoint");
            character.worldId = charResultSet.getInt("world");
            character.worldRank = charResultSet.getInt("rank");
            character.worldRankMove = charResultSet.getInt("rankMove");
            character.jobRank = charResultSet.getInt("jobRank");
            character.jobRankMove = charResultSet.getInt("jobRankMove");

            selectEquips = Database.getConnection().prepareStatement(
                    "SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING (inventoryitemid) "
                    + "WHERE `characterid` = ? AND `inventorytype` = -1");
            selectEquips.setInt(1, character.id);
            equipsResultSet = selectEquips.executeQuery();

            while (equipsResultSet.next()) {
                final int itemId = equipsResultSet.getInt("itemid");
                final byte slot = equipsResultSet.getByte("position");
                final int ringId = equipsResultSet.getInt("ringid");
                final byte flags = equipsResultSet.getByte("flag");
                final Equip equip = new Equip(itemId, slot, ringId, flags);
                character.equips.addFromDb(equip);
            }
        } catch (final SQLException ex) {
            System.out.println("Failed to load character: " + ex);
        } finally {
            try {
                if (selectEquips != null) {
                    selectEquips.close();
                }
                if (equipsResultSet != null) {
                    equipsResultSet.close();
                }
            } catch (final SQLException ex) {
            }

        }
        return character;
    }

    public static Collection<LoginCharacter> loadCharacters(final int accountId, final int worldId) {
        final List<LoginCharacter> characters = Lists.newLinkedList();
        final Connection connection = Database.getConnection();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM `characters` WHERE `accountid` = ? AND `world` = ? ORDER BY `id` ASC")) {
            ps.setInt(1, accountId);
            ps.setInt(2, worldId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final LoginCharacter character = loadFromRecord(rs);
                    characters.add(character);
                }
            }
        } catch (final SQLException e) {
            System.err.println("error loading characters internal" + e);
        }
        return Collections.unmodifiableCollection(characters);
    }

    public static LoginCharacter getDefault(final int type) {
        final LoginCharacter character = new LoginCharacter();
        character.exp = 0;
        character.gmLevel = 0;
        character.jobId = type == 0 ? 1000 : type == 1 ? 0 : type == 2 ? 2000 : 2001;
        character.meso = 0;
        character.level = 1;
        character.remainingAp = 0;
        character.fame = 0;
        character.stats.STR = 12;
        character.stats.DEX = 4;
        character.stats.INT = 4;
        character.stats.LUK = 4;
        character.stats.HP = 50;
        character.stats.MaxHP = 50;
        character.stats.MP = 5;
        character.stats.MaxMP = 5;
        return character;
    }

    public static void saveNewCharacterToDb(final LoginCharacter chr, final int type, final boolean isDualBlader) {
        final Connection con = Database.getConnection();

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);
            ps = con.prepareStatement("INSERT INTO characters (level, fame, str, dex, luk, `int`, exp, hp, mp, maxhp, maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, meso, hpApUsed, mpApUsed, spawnpoint, party, buddyCapacity, messengerid, messengerposition, monsterbookcover, dojo_pts, dojoRecord, accountid, name, world, reborns, subcategory) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, 1); // Level
            ps.setInt(2, 0); // Fame
            ps.setInt(3, chr.stats.STR); // Str
            ps.setInt(4, chr.stats.DEX); // Dex
            ps.setInt(5, chr.stats.INT); // Int
            ps.setInt(6, chr.stats.LUK); // Luk
            ps.setInt(7, 0); // EXP
            ps.setInt(8, chr.stats.HP);
            ps.setInt(9, chr.stats.MP);
            ps.setInt(10, chr.stats.MaxHP);
            ps.setInt(11, chr.stats.MaxMP);
            ps.setString(12, "0,0,0,0,0,0,0,0,0,0"); // Remaining SP
            ps.setInt(13, 0); // Remaining AP
            ps.setInt(14, 0); // GM Level
            ps.setInt(15, chr.skinColorId);
            ps.setInt(16, chr.gender.asNumber());
            ps.setInt(17, chr.jobId);
            ps.setInt(18, chr.hairId);
            ps.setInt(19, chr.faceId);
            // TODO: proper constants.
            ps.setInt(20, type == 1 ? 10000 : type == 0 ? 130030000 : type
                    == 2 ? 914000000 : 900010000); // 0-KOC 1-Adventurer 2-Aran 3-Evan 4-DB
            ps.setInt(21, chr.meso); // Meso
            ps.setInt(22, 0); // HP ap used
            ps.setInt(23, 0); // MP ap used
            ps.setInt(24, 0); // Spawnpoint
            ps.setInt(25, -1); // Party
            ps.setInt(26, 20); // Buddy list capacity
            ps.setInt(27, 0); // MessengerId
            ps.setInt(28, 4); // Messenger Position
            ps.setInt(29, 0); // Monster book cover
            ps.setInt(30, 0); // Dojo
            ps.setInt(31, 0); // Dojo record
            ps.setInt(32, chr.getAccountId());
            ps.setString(33, chr.name);
            ps.setInt(34, chr.worldId);
            ps.setInt(35, 0);
            ps.setInt(36, isDualBlader ? 1 : 0); //for now
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                throw new DatabaseException(":: Failed to create new character ::");
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setInt(2, 24); // Eq
            ps.setInt(3, 24); // Use
            ps.setInt(4, 24); // Setup
            ps.setInt(5, 24); // ETC
            ps.setInt(6, 96); // Cash
            ps.execute();
            ps.close();
            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.execute();
            ps.close();
            ps = con.prepareStatement("INSERT INTO inventoryitems (characterid, inventorytype, itemid, position, quantity, owner, GM_Log, petid, expiredate, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            IEquip equip;

            ps.setInt(1, chr.id);
            ps.setInt(2, chr.equips.getType().asNumber());
            for (final IItem item : chr.equips) {
                ps.setInt(3, item.getItemId());
                ps.setInt(4, item.getPosition());
                ps.setInt(5, item.getQuantity());
                ps.setString(6, item.getOwner());
                ps.setString(7, item.getGMLog());
                ps.setInt(8, -1); // Pet cant be loaded on logins + new char doesn't have.
                ps.setLong(9, item.getExpiration());
                ps.setByte(10, item.getFlag());
                ps.executeUpdate();

                rs = ps.getGeneratedKeys();
                int newItemId;
                if (rs.next()) {
                    newItemId = rs.getInt(1);
                } else {
                    throw new DatabaseException("Inserting char failed.");
                }
                rs.close();

                pse.setInt(1, newItemId);
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
            ps.close();
            pse.close();

            ps = con.prepareStatement("INSERT INTO `mountdata` (`characterid`, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.execute();
            ps.close();

            final int[] array1 = {2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 23, 25, 26, 27, 31, 34, 37, 38, 41, 44, 45, 46, 50, 57, 59, 60, 61, 62, 63, 64, 65, 8, 9, 24, 30};
            final int[] array2 = {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 4, 5, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4};
            final int[] array3 = {10, 12, 13, 18, 6, 11, 8, 5, 0, 4, 1, 19, 14, 15, 3, 17, 9, 20, 22, 50, 51, 52, 7, 53, 100, 101, 102, 103, 104, 105, 106, 16, 23, 24, 2};

            ps = con.prepareStatement("INSERT INTO `keymap` (`characterid`, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (int i = 0; i < array1.length; i++) {
                ps.setInt(2, array1[i]);
                ps.setInt(3, array2[i]);
                ps.setInt(4, array3[i]);
                ps.execute();
            }
            ps.close();

            con.commit();
        } catch (SQLException | DatabaseException e) {
            System.err.println("[charsave] Error saving character data: " + e);
            try {
                con.rollback();
            } catch (final SQLException ex) {
                System.err.println("[charsave] Error Rolling Back: " + ex);
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
            } catch (final SQLException e) {
                System.err.println("[charsave] Error going back to autocommit mode: " + e);
            }
        }
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
        return this.remainingSp[Skills.getSkillbook(this.jobId)]; //default
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

    @Override
	public int getId() {
        return this.id;
    }

    @Override
	public String getName() {
        return this.name;
    }

    @Override
	public int getLevel() {
        return this.level;
    }

    @Override
	public int getFame() {
        return this.fame;
    }

    @Override
	public int getGmLevel() {
        return this.gmLevel;
    }

    @Override
	public Gender getGender() {
        return this.gender;
    }

    @Override
	public int getWorldId() {
        return this.worldId;
    }

    public int getAccountId() {
        return this.accountId;
    }

    @Override
	public int getMeso() {
        return this.meso;
    }

    @Override
	public int getJobId() {
        return this.jobId;
    }

    @Override
	public int getHairId() {
        return this.hairId;
    }

    @Override
	public int getFaceId() {
        return this.faceId;
    }

    @Override
	public int getSkinColorId() {
        return this.skinColorId;
    }

    @Override
	public int getMapId() {
        return this.mapId;
    }

    @Override
	public int getInitialSpawnPoint() {
        return this.initialSpawnPoint;
    }

    @Override
	public int getSubcategory() {
        return this.subcategory;
    }

    public int getWorldRank() {
        return this.worldRank;
    }

    public int getWorldRankMove() {
        return this.worldRankMove;
    }

    public int getJobRank() {
        return this.jobRank;
    }

    public int getJobRankMove() {
        return this.jobRankMove;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setLevel(final int level) {
        this.level = (short) level;
    }

    public void setFame(final int fame) {
        this.fame = fame;
    }

    public void setExp(final int exp) {
        this.exp = exp;
    }

    public void setGmLevel(final byte gmLevel) {
        this.gmLevel = gmLevel;
    }

    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    public void setWorldId(final int worldId) {
        this.worldId = worldId;
    }

    public void setMeso(final int meso) {
        this.meso = meso;
    }

    public void setJobId(final int jobId) {
        this.jobId = jobId;
    }

    public void setHairId(final int hairId) {
        this.hairId = hairId;
    }

    public void setFaceId(final int faceId) {
        this.faceId = faceId;
    }

    public void setSkinColorId(final int skinColorId) {
        this.skinColorId = skinColorId;
    }

    public void setRemainingAp(final int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setSubcategory(final int subcategory) {
        this.subcategory = subcategory;
    }

    @Override
	public Inventory getEquippedItemsInventory() {
        return this.equips;
    }

    @Override
	public PlayerStats getStats() {
        return this.stats;
    }
}
