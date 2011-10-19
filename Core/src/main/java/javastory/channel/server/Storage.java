package javastory.channel.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import javastory.game.GameConstants;
import client.Equip;
import client.IEquip;
import client.IItem;
import client.Item;
import javastory.channel.ChannelClient;
import javastory.game.InventoryType;
import javastory.client.ItemType;
import com.google.common.collect.Maps;
import javastory.db.DatabaseConnection;
import javastory.db.DatabaseException;
import java.sql.Statement;
import tools.MaplePacketCreator;

public class Storage implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private int id;
    private List<IItem> items;
    private int meso;
    private byte slots;
    private boolean hasChanged = false;
    private Map<InventoryType, List<IItem>> typeItems = Maps.newEnumMap(InventoryType.class);

    private Storage(int id, byte slots, int meso) {
        this.id = id;
        this.slots = slots;
        this.items = new LinkedList<>();
        this.meso = meso;
    }

    public static int create(int id) throws SQLException {
        Connection connection = DatabaseConnection.getConnection();
        PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO storages (accountid, slots, meso) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, id);
        ps.setInt(2, 4);
        ps.setInt(3, 0);
        ps.executeUpdate();

        int storageid;
        ResultSet rs = ps.getGeneratedKeys();
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

    public static Storage loadStorage(int id) {
        Storage ret = null;
        int storeId;
        try {
            Connection con = DatabaseConnection.getConnection();
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
                    InventoryType type = InventoryType.fromByte((byte) rs.getInt("inventorytype"));
                    if (type.equals(InventoryType.EQUIP) ||
                            type.equals(InventoryType.EQUIPPED)) {
                        int itemid = rs.getInt("itemid");
                        Equip equip = new Equip(itemid, rs.getByte("position"), rs.getInt("ringid"), rs.getByte("flag"));
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
                        Item item = new Item(rs.getInt("itemid"), (byte) rs.getInt("position"), (short) rs.getInt("quantity"), rs.getByte("flag"));
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
        } catch (SQLException ex) {
            System.err.println("Error loading storage" + ex);
        }
        return ret;
    }

    public void saveToDB() {
        if (!hasChanged) {
            return;
        }
        try {
            Connection con = DatabaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?");
            ps.setInt(1, slots);
            ps.setInt(2, meso);
            ps.setInt(3, id);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("DELETE FROM inventoryitems WHERE storageid = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("INSERT INTO inventoryitems (storageid, itemid, inventorytype, position, quantity, owner, GM_Log, expiredate, flag) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement pse = con.prepareStatement("INSERT INTO inventoryequipment VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            for (IItem item : items) {
                ps.setInt(1, id);
                ps.setInt(2, item.getItemId());
                ps.setInt(3, item.getType().asByte()); // type.getType()
                ps.setInt(4, item.getPosition());
                ps.setInt(5, item.getQuantity());
                ps.setString(6, item.getOwner());
                ps.setString(7, item.getGMLog());
                ps.setLong(8, item.getExpiration());
                ps.setByte(9, item.getFlag());
                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                int itemid;
                if (rs.next()) {
                    itemid = rs.getInt(1);
                } else {
                    throw new DatabaseException("Inserting char failed.");
                }
                if (item.getType() == ItemType.EQUIP) {
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
            ps.close();
            pse.close();
        } catch (SQLException ex) {
            System.err.println("Error saving storage" + ex);
        }
    }

    public IItem takeOut(byte slot) {
        hasChanged = true;
        IItem ret = items.remove(slot);
        InventoryType type = GameConstants.getInventoryType(ret.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
        return ret;
    }

    public void store(IItem item) {
        hasChanged = true;
        items.add(item);
        InventoryType type = GameConstants.getInventoryType(item.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
    }

    public List<IItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private List<IItem> filterItems(InventoryType type) {
        List<IItem> ret = new LinkedList<>();

        for (IItem item : items) {
            if (GameConstants.getInventoryType(item.getItemId()) == type) {
                ret.add(item);
            }
        }
        return ret;
    }

    public byte getSlot(InventoryType type, byte slot) {
        // MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte ret = 0;
        for (IItem item : items) {
            if (item == typeItems.get(type).get(slot)) {
                return ret;
            }
            ret++;
        }
        return -1;
    }

    public void sendStorage(ChannelClient c, int npcId) {
        // sort by inventorytype to avoid confusion
        Collections.sort(items, new Comparator<IItem>() {

            @Override
			public int compare(IItem item1, IItem item2) {
                final InventoryType item1Type = GameConstants.getInventoryType(item1.getItemId());
                final InventoryType item2Type = GameConstants.getInventoryType(item2.getItemId());
                if (item1Type.asByte() < item2Type.asByte()) {
                    return -1;
                } else if (item1Type == item2Type) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        for (InventoryType type : InventoryType.values()) {
            typeItems.put(type, new ArrayList<>(items));
        }
        c.write(MaplePacketCreator.getStorage(npcId, slots, items, meso));
    }

    public void sendStored(ChannelClient c, InventoryType type) {
        c.write(MaplePacketCreator.storeStorage(slots, type, typeItems.get(type)));
    }

    public void sendTakenOut(ChannelClient c, InventoryType type) {
        c.write(MaplePacketCreator.takeOutStorage(slots, type, typeItems.get(type)));
    }

    public int getMeso() {
        return meso;
    }

    public void setMeso(int meso) {
        if (meso < 0) {
            return;
        }
        hasChanged = true;
        this.meso = meso;
    }

    public void sendMeso(ChannelClient c) {
        c.write(MaplePacketCreator.mesoStorage(slots, meso));
    }

    public boolean isFull() {
        return items.size() >= slots;
    }

    public int getSlots() {
        return slots;
    }

    public void increaseSlots(byte gain) {
        hasChanged = true;
        this.slots += gain;
    }

    public void setSlots(byte set) {
        hasChanged = true;
        this.slots = set;
    }

    public void close() {
        typeItems.clear();
    }
}
