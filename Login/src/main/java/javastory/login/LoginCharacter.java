package javastory.login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javastory.client.GameCharacter;
import javastory.db.Database;
import javastory.db.DatabaseException;
import javastory.game.Equip;
import javastory.game.Gender;
import javastory.game.Inventory;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.PlayerStats;
import javastory.game.Skills;

import com.google.common.collect.Lists;

/**
 * 
 * @author shoftee
 */
public class LoginCharacter implements GameCharacter {

	private static final int[] KeyIdDefaults = {
		2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 23, 25, 26, 27, 31, 34, 37, 38, 41, 44, 45, 46, 50, 57, 59, 60, 61, 62, 63, 64, 65, 8, 9, 24, 30 };
	private static final int[] KeyTypeDefaults = { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 4, 5, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4 };
	private static final int[] KeyActionDefaults = {
		10, 12, 13, 18, 6, 11, 8, 5, 0, 4, 1, 19, 14, 15, 3, 17, 9, 20, 22, 50, 51, 52, 7, 53, 100, 101, 102, 103, 104, 105, 106, 16, 23, 24, 2 };

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
	private int accountId, meso, jobId, hairId, faceId, skinColorId, remainingAp, mapId, initialSpawnPoint, subcategory;
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

	private void loadFromRecord(final ResultSet rs) throws SQLException {
		this.id = rs.getInt("id");
		this.name = rs.getString("name");
		this.level = rs.getShort("level");
		this.fame = rs.getInt("fame");

		this.stats.STR = rs.getInt("str");
		this.stats.DEX = rs.getInt("dex");
		this.stats.INT = rs.getInt("int");
		this.stats.LUK = rs.getInt("luk");
		this.stats.MaxHP = rs.getInt("maxhp");
		this.stats.MaxMP = rs.getInt("maxmp");
		this.stats.HP = rs.getInt("hp");
		this.stats.MP = rs.getInt("mp");

		this.exp = rs.getInt("exp");
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
		this.mapId = rs.getInt("map");
		this.initialSpawnPoint = rs.getInt("spawnpoint");
		this.worldId = rs.getInt("world");
		this.worldRank = rs.getInt("rank");
		this.worldRankMove = rs.getInt("rankMove");
		this.jobRank = rs.getInt("jobRank");
		this.jobRankMove = rs.getInt("jobRankMove");

		try (	PreparedStatement selectEquips = getSelectEquippedItems();
				ResultSet equipsResultSet = selectEquips.executeQuery()) {
			while (equipsResultSet.next()) {
				final int itemId = equipsResultSet.getInt("itemid");
				final byte slot = equipsResultSet.getByte("position");
				final int ringId = equipsResultSet.getInt("ringid");
				final byte flags = equipsResultSet.getByte("flag");
				final Equip equip = new Equip(itemId, slot, ringId, flags);
				this.equips.addFromDb(equip);
			}
		}
	}

	private PreparedStatement getSelectEquippedItems() throws SQLException {
		final String sql =
			"SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING (inventoryitemid) " + "WHERE `characterid` = ? AND `inventorytype` = ?";
		final PreparedStatement ps = Database.getConnection().prepareStatement(sql);
		ps.setInt(1, this.id);

		// Equipped inventory type.
		ps.setInt(2, -1);
		return ps;
	}

	public static List<LoginCharacter> loadCharacters(final int accountId, final int worldId) {
		final List<LoginCharacter> characters = Lists.newLinkedList();
		final Connection connection = Database.getConnection();
		try (	final PreparedStatement ps = getSelectCharacters(connection, accountId, worldId);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				final LoginCharacter character = new LoginCharacter();
				character.loadFromRecord(rs);
				characters.add(character);
			}
		} catch (final SQLException e) {
			System.err.println("error loading characters internal" + e);
		}
		return characters;
	}

	private static PreparedStatement getSelectCharacters(final Connection connection, final int accountId, final int worldId) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("SELECT * FROM `characters` WHERE `accountid` = ? AND `world` = ? ORDER BY `id` ASC");
		ps.setInt(1, accountId);
		ps.setInt(2, worldId);
		return ps;
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

		try {
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);

			try (final PreparedStatement ps = chr.getInsertCharacterData(type, isDualBlader, con)) {
				ps.executeUpdate();
				try (final ResultSet rs = ps.getGeneratedKeys()) {
					if (rs.next()) {
						chr.id = rs.getInt(1);
					} else {
						throw new DatabaseException(":: Failed to create new character ::");
					}
				}
			}

			try (final PreparedStatement capacitiesStatement = chr.getInsertInventoryCapacities(con)) {
				capacitiesStatement.execute();
			}

			try (final PreparedStatement mountStatement = chr.getInsertMountData(con)) {
				mountStatement.execute();
			}

			try (	final PreparedStatement itemStatement = chr.getInsertInventoryItems(con);
					final PreparedStatement equipStatement = chr.getInsertEquip(con)) {

				for (final Item item : chr.equips) {
					setItemData(itemStatement, item);
					itemStatement.executeUpdate();

					int newItemId;
					try (final ResultSet rs = itemStatement.getGeneratedKeys()) {
						if (rs.next()) {
							newItemId = rs.getInt(1);
						} else {
							throw new DatabaseException("Inserting char failed.");
						}
					}

					final Equip equip = (Equip) item;
					setEquipData(equipStatement, newItemId, equip);
					equipStatement.executeUpdate();
				}
			}

			try (final PreparedStatement ps = getInsertKeyMapping(chr, con)) {
				for (int i = 0; i < KeyIdDefaults.length; i++) {
					setKeyMappingData(ps, i);
					ps.execute();
				}
			}

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
				con.setAutoCommit(true);
				con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			} catch (final SQLException e) {
				System.err.println("[charsave] Error going back to autocommit mode: " + e);
			}
		}
	}

	private static void setKeyMappingData(final PreparedStatement ps, int i) throws SQLException {
		ps.setInt(2, KeyIdDefaults[i]);
		ps.setInt(3, KeyTypeDefaults[i]);
		ps.setInt(4, KeyActionDefaults[i]);
	}

	private static PreparedStatement getInsertKeyMapping(final LoginCharacter chr, final Connection con) throws SQLException {
		final PreparedStatement ps = con.prepareStatement("INSERT INTO `keymap` (`characterid`, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
		ps.setInt(1, chr.id);
		return ps;
	}

	private PreparedStatement getInsertEquip(final Connection con) throws SQLException {
		final String sql = "INSERT INTO `inventoryequipment` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		final PreparedStatement ps = con.prepareStatement(sql);
		return ps;
	}

	private static void setEquipData(final PreparedStatement ps, int newItemId, final Equip equip) throws SQLException {
		ps.setInt(1, newItemId);
		ps.setInt(2, equip.getUpgradeSlots());
		ps.setInt(3, equip.getLevel());
		ps.setInt(4, equip.getStr());
		ps.setInt(5, equip.getDex());
		ps.setInt(6, equip.getInt());
		ps.setInt(7, equip.getLuk());
		ps.setInt(8, equip.getHp());
		ps.setInt(9, equip.getMp());
		ps.setInt(10, equip.getWatk());
		ps.setInt(11, equip.getMatk());
		ps.setInt(12, equip.getWdef());
		ps.setInt(13, equip.getMdef());
		ps.setInt(14, equip.getAcc());
		ps.setInt(15, equip.getAvoid());
		ps.setInt(16, equip.getHands());
		ps.setInt(17, equip.getSpeed());
		ps.setInt(18, equip.getJump());
		ps.setInt(19, equip.getRingId());
		ps.setInt(20, equip.getViciousHammer());
		ps.setInt(21, 0);
		ps.setInt(22, 0);
	}

	private static void setItemData(final PreparedStatement ps, final Item item) throws SQLException {
		ps.setInt(3, item.getItemId());
		ps.setInt(4, item.getPosition());
		ps.setInt(5, item.getQuantity());
		ps.setString(6, item.getOwner());
		ps.setString(7, item.getGMLog());
		ps.setInt(8, -1); // Pet cant be loaded on logins + new char doesn't have.
		ps.setLong(9, item.getExpiration());
		ps.setByte(10, item.getFlag());
	}

	private PreparedStatement getInsertInventoryItems(final Connection con) throws SQLException {
		final String sql =
			"INSERT INTO `inventoryitems` (`characterid`, `inventorytype`, `itemid`, `position`, `quantity`, `owner`, `GM_Log`, `petid`, `expiredate`, `flag`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		final PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, this.id);
		ps.setInt(2, this.equips.getType().asNumber());
		return ps;
	}

	private PreparedStatement getInsertMountData(final Connection con) throws SQLException {
		final String sql = "INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)";
		final PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, this.id);
		ps.setInt(2, 1);
		ps.setInt(3, 0);
		ps.setInt(4, 0);
		return ps;
	}

	private PreparedStatement getInsertInventoryCapacities(final Connection con) throws SQLException {
		final String sql = "INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setInt(1, this.id);
		ps.setInt(2, 24); // Eq
		ps.setInt(3, 24); // Use
		ps.setInt(4, 24); // Setup
		ps.setInt(5, 24); // ETC
		ps.setInt(6, 96); // Cash
		return ps;
	}

	private PreparedStatement getInsertCharacterData(final int type, final boolean isDualBlader, final Connection con) throws SQLException {
		final String sql =
			"INSERT INTO characters (level, fame, str, dex, luk, `int`, exp, hp, mp, maxhp, maxmp, sp, ap, gm, skincolor, gender, job, hair, face, map, meso, hpApUsed, mpApUsed, spawnpoint, party, buddyCapacity, messengerid, messengerposition, monsterbookcover, dojo_pts, dojoRecord, accountid, name, world, reborns, subcategory) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, 1); // Level
		ps.setInt(2, 0); // Fame
		ps.setInt(3, this.stats.STR); // Str
		ps.setInt(4, this.stats.DEX); // Dex
		ps.setInt(5, this.stats.INT); // Int
		ps.setInt(6, this.stats.LUK); // Luk
		ps.setInt(7, 0); // EXP
		ps.setInt(8, this.stats.HP);
		ps.setInt(9, this.stats.MP);
		ps.setInt(10, this.stats.MaxHP);
		ps.setInt(11, this.stats.MaxMP);
		ps.setString(12, "0,0,0,0,0,0,0,0,0,0"); // Remaining SP
		ps.setInt(13, 0); // Remaining AP
		ps.setInt(14, 0); // GM Level
		ps.setInt(15, this.skinColorId);
		ps.setInt(16, this.gender.asNumber());
		ps.setInt(17, this.jobId);
		ps.setInt(18, this.hairId);
		ps.setInt(19, this.faceId);
		// TODO: proper constants.
		ps.setInt(20, type == 1 ? 10000 : type == 0 ? 130030000 : type == 2 ? 914000000 : 900010000); // 0-KOC 1-Adventurer 2-Aran 3-Evan 4-DB
		ps.setInt(21, this.meso); // Meso
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
		ps.setInt(32, this.getAccountId());
		ps.setString(33, this.name);
		ps.setInt(34, this.worldId);
		ps.setInt(35, 0);
		ps.setInt(36, isDualBlader ? 1 : 0); //for now
		return ps;
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
