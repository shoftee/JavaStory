package javastory.channel.server;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javastory.channel.ChannelClient;
import javastory.db.Database;
import javastory.db.DatabaseException;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.ItemType;
import javastory.tools.packets.ChannelPackets;

import com.google.common.collect.Maps;

public class Storage implements Serializable {

	private static final long serialVersionUID = 9179541993413738569L;
	private final int id;
	private final List<Item> items;
	private int meso;
	private byte slots;
	private boolean hasChanged = false;
	private final Map<InventoryType, List<Item>> typeItems = Maps.newEnumMap(InventoryType.class);

	private Storage(final int id, final byte slots, final int meso) {
		this.id = id;
		this.slots = slots;
		this.items = new LinkedList<>();
		this.meso = meso;
	}
	
	// TODO: Create prepared statement in a method. Saves me nested try-with-resources blocks.
	public static int create(final int id) throws SQLException {
		final Connection connection = Database.getConnection();
		final PreparedStatement ps = connection.prepareStatement("INSERT INTO storages (accountid, slots, meso) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		ps.setInt(1, id);
		ps.setInt(2, 4);
		ps.setInt(3, 0);
		ps.executeUpdate();

		int storageid;
		final ResultSet rs = ps.getGeneratedKeys();
		if (rs.next()) {
			storageid = rs.getInt(1);
			ps.close();
			rs.close();
			return storageid;
		}
		ps.close();
		rs.close();
		throw new DatabaseException("Inserting char failed.");
	}

	// TODO: Create prepared statement in a method. Saves me nested try-with-resources blocks.

	public static Storage loadStorage(final int id) {
		Storage ret = null;
		int storeId;
		try {
			final Connection con = Database.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM storages WHERE accountid = ?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				storeId = rs.getInt("storageid");
				ret = new Storage(storeId, rs.getByte("slots"), rs.getInt("meso"));
				rs.close();
				ps.close();

				ps = con.prepareStatement("SELECT * FROM inventoryitems LEFT JOIN inventoryequipment USING (inventoryitemid) WHERE storageid = ?");
				ps.setInt(1, storeId);
				rs = ps.executeQuery();
				while (rs.next()) {
					final InventoryType type = InventoryType.fromNumber((byte) rs.getInt("inventorytype"));
					if (type.equals(InventoryType.EQUIP) || type.equals(InventoryType.EQUIPPED)) {
						final int itemid = rs.getInt("itemid");
						final Equip equip = new Equip(itemid, rs.getByte("position"), rs.getInt("ringid"), rs.getByte("flag"));
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
						equip.setUpgradeSlots(rs.getByte("upgradeslots"));
						equip.setLevel(rs.getByte("level"));
						equip.setViciousHammer(rs.getByte("ViciousHammer"));
						equip.setExpiration(rs.getLong("expiredate"));
						equip.setFlag(rs.getByte("flag"));
						ret.items.add(equip);
					} else {
						final Item item = new Item(rs.getInt("itemid"), (byte) rs.getInt("position"), (short) rs.getInt("quantity"), rs.getByte("flag"));
						item.setOwner(rs.getString("owner"));
						item.setExpiration(rs.getLong("expiredate"));
						item.setFlag(rs.getByte("flag"));
						ret.items.add(item);
					}
				}
				rs.close();
				ps.close();
			} else {
				storeId = create(id);
				ret = new Storage(storeId, (byte) 4, 0);
			}
		} catch (final SQLException ex) {
			System.err.println("Error loading storage" + ex);
		}
		return ret;
	}

	// TODO: Create prepared statement in a method. Saves me nested try-with-resources blocks.
	public void saveToDB() {
		if (!this.hasChanged) {
			return;
		}
		try {
			final Connection con = Database.getConnection();

			PreparedStatement ps = con.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?");
			ps.setInt(1, this.slots);
			ps.setInt(2, this.meso);
			ps.setInt(3, this.id);
			ps.executeUpdate();
			ps.close();

			ps = con.prepareStatement("DELETE FROM inventoryitems WHERE storageid = ?");
			ps.setInt(1, this.id);
			ps.executeUpdate();
			ps.close();

			ps = con
				.prepareStatement(
					"INSERT INTO inventoryitems (storageid, itemid, inventorytype, position, quantity, owner, GM_Log, expiredate, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			final PreparedStatement pse = con
				.prepareStatement("INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			for (final Item item : this.items) {
				ps.setInt(1, this.id);
				ps.setInt(2, item.getItemId());
				ps.setInt(3, item.getType().asByte()); // type.getType()
				ps.setInt(4, item.getPosition());
				ps.setInt(5, item.getQuantity());
				ps.setString(6, item.getOwner());
				ps.setString(7, item.getGMLog());
				ps.setLong(8, item.getExpiration());
				ps.setByte(9, item.getFlag());
				ps.executeUpdate();

				final ResultSet rs = ps.getGeneratedKeys();
				int itemid;
				if (rs.next()) {
					itemid = rs.getInt(1);
				} else {
					throw new DatabaseException("Inserting char failed.");
				}
				if (item.getType() == ItemType.EQUIP) {
					pse.setInt(1, itemid);
					final Equip equip = (Equip) item;
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
			ps.close();
			pse.close();
		} catch (final SQLException ex) {
			System.err.println("Error saving storage" + ex);
		}
	}

	public Item takeOut(final byte slot) {
		this.hasChanged = true;
		final Item ret = this.items.remove(slot);
		final InventoryType type = GameConstants.getInventoryType(ret.getItemId());
		this.typeItems.put(type, new ArrayList<>(this.filterItems(type)));
		return ret;
	}

	public void store(final Item item) {
		this.hasChanged = true;
		this.items.add(item);
		final InventoryType type = GameConstants.getInventoryType(item.getItemId());
		this.typeItems.put(type, new ArrayList<>(this.filterItems(type)));
	}

	public List<Item> getItems() {
		return Collections.unmodifiableList(this.items);
	}

	private List<Item> filterItems(final InventoryType type) {
		final List<Item> ret = new LinkedList<>();

		for (final Item item : this.items) {
			if (GameConstants.getInventoryType(item.getItemId()) == type) {
				ret.add(item);
			}
		}
		return ret;
	}

	public byte getSlot(final InventoryType type, final byte slot) {
		// MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
		byte ret = 0;
		for (final Item item : this.items) {
			if (item == this.typeItems.get(type).get(slot)) {
				return ret;
			}
			ret++;
		}
		return -1;
	}

	public void sendStorage(final ChannelClient c, final int npcId) {
		// sort by inventorytype to avoid confusion
		Collections.sort(this.items, new Comparator<Item>() {

			@Override
			public int compare(final Item item1, final Item item2) {
				final InventoryType item1Type = GameConstants.getInventoryType(item1.getItemId());
				final InventoryType item2Type = GameConstants.getInventoryType(item2.getItemId());
				if (item1Type.asNumber() < item2Type.asNumber()) {
					return -1;
				} else if (item1Type == item2Type) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		for (final InventoryType type : InventoryType.values()) {
			this.typeItems.put(type, new ArrayList<>(this.items));
		}
		c.write(ChannelPackets.getStorage(npcId, this.slots, this.items, this.meso));
	}

	public void sendStored(final ChannelClient c, final InventoryType type) {
		c.write(ChannelPackets.storeStorage(this.slots, type, this.typeItems.get(type)));
	}

	public void sendTakenOut(final ChannelClient c, final InventoryType type) {
		c.write(ChannelPackets.takeOutStorage(this.slots, type, this.typeItems.get(type)));
	}

	public int getMeso() {
		return this.meso;
	}

	public void setMeso(final int meso) {
		if (meso < 0) {
			return;
		}
		this.hasChanged = true;
		this.meso = meso;
	}

	public void sendMeso(final ChannelClient c) {
		c.write(ChannelPackets.mesoStorage(this.slots, this.meso));
	}

	public boolean isFull() {
		return this.items.size() >= this.slots;
	}

	public int getSlots() {
		return this.slots;
	}

	public void increaseSlots(final byte gain) {
		this.hasChanged = true;
		this.slots += gain;
	}

	public void setSlots(final byte set) {
		this.hasChanged = true;
		this.slots = set;
	}

	public void close() {
		this.typeItems.clear();
	}
}
