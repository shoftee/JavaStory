package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import client.IItem;
import client.Item;
import client.SkillFactory;
import client.GameConstants;
import client.GameClient;
import client.InventoryType;
import client.Pet;
import database.DatabaseConnection;
import tools.MaplePacketCreator;

public class Shop {
    private static final Set<Integer> rechargeableItems = new LinkedHashSet<Integer>();
    private int id;
    private int npcId;
    private List<ShopItem> items;

    static {
	rechargeableItems.add(2070000);
	rechargeableItems.add(2070001);
	rechargeableItems.add(2070002);
	rechargeableItems.add(2070003);
	rechargeableItems.add(2070004);
	rechargeableItems.add(2070005);
	rechargeableItems.add(2070006);
	rechargeableItems.add(2070007);
	rechargeableItems.add(2070008);
	rechargeableItems.add(2070009);
	rechargeableItems.add(2070010);
	rechargeableItems.add(2070011);
	rechargeableItems.add(2070012);
	rechargeableItems.add(2070013);
//	rechargeableItems.add(2070014); // Doesn't Exist [Devil Rain]
//	rechargeableItems.add(2070015); // Beginner Star
	rechargeableItems.add(2070016);
//	rechargeableItems.add(2070017); // Doesn't Exist
	rechargeableItems.add(2070018); // Balanced Fury
	rechargeableItems.add(2070019); // Magic Throwing Star

	rechargeableItems.add(2330000);
	rechargeableItems.add(2330001);
	rechargeableItems.add(2330002);
	rechargeableItems.add(2330003);
	rechargeableItems.add(2330004);
	rechargeableItems.add(2330005);
//	rechargeableItems.add(2330006); // Beginner Bullet
	rechargeableItems.add(2330007);

	rechargeableItems.add(2331000); // Capsules
	rechargeableItems.add(2332000); // Capsules
    }

    /** Creates a new instance of MapleShop */
    private Shop(int id, int npcId) {
	this.id = id;
	this.npcId = npcId;
	items = new LinkedList<ShopItem>();
    }

    public void addItem(ShopItem item) {
	items.add(item);
    }

    public void sendShop(GameClient c) {
	c.getPlayer().setShop(this);
	c.write(MaplePacketCreator.getNPCShop(c, getNpcId(), items));
    }

    public void buy(GameClient c, int itemId, short quantity) {
	if (quantity <= 0) {
	    AutobanManager.getInstance().addPoints(c, 1000, 0, "Buying " + quantity + " " + itemId);
	    return;
	}
	ShopItem item = findById(itemId);
	if (item != null && item.getPrice() > 0) {
	    if (c.getPlayer().getMeso() >= item.getPrice() * quantity) {
		if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
		    if (GameConstants.isPet(itemId)) {
			InventoryManipulator.addById(c, itemId, quantity, null, Pet.createPet(itemId));
		    } else {
			ItemInfoProvider ii = ItemInfoProvider.getInstance();

			if (GameConstants.isRechargable(itemId)){
			    quantity = ii.getSlotMax(c, item.getItemId());
			    c.getPlayer().gainMeso(-(item.getPrice()), false);
			    InventoryManipulator.addById(c, itemId, quantity);
			} else {
			    c.getPlayer().gainMeso(-(item.getPrice() * quantity), false);
			    InventoryManipulator.addById(c, itemId, quantity);
			}
		    }
		} else {
		    c.getPlayer().dropMessage(1, "Your Inventory is full");
		}
		c.write(MaplePacketCreator.confirmShopTransaction((byte) 0));
	    }
	}
    }

    public void sell(GameClient c, InventoryType type, byte slot, short quantity) {
	if (quantity == 0xFFFF || quantity == 0) {
	    quantity = 1;
	}
	IItem item = c.getPlayer().getInventory(type).getItem(slot);

	if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
	    quantity = item.getQuantity();
	}
	if (quantity < 0) {
	    AutobanManager.getInstance().addPoints(c, 1000,	0, "Selling " + quantity + " " + item.getItemId() + " (" + type.name() + "/" + slot + ")");
	    return;
	}
	short iQuant = item.getQuantity();
	if (iQuant == 0xFFFF) {
	    iQuant = 1;
	}
	final ItemInfoProvider ii = ItemInfoProvider.getInstance();
	if (quantity <= iQuant && iQuant > 0) {
	    InventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
	    double price;
	    if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
		price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(c, item.getItemId());
	    } else {
		price = ii.getPrice(item.getItemId());
	    }
	    final int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
	    if (price != -1 && recvMesos > 0) {
		c.getPlayer().gainMeso(recvMesos, false);
	    }
	    c.write(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
	}
    }

    public void recharge(final GameClient c, final byte slot) {
	final IItem item = c.getPlayer().getInventory(InventoryType.USE).getItem(slot);

	if (item == null || (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId()))) {
	    return;
	}
	final ItemInfoProvider ii = ItemInfoProvider.getInstance();
	short slotMax = ii.getSlotMax(c, item.getItemId());
	final int skill = GameConstants.getMasterySkill(c.getPlayer().getJob());

	if (skill != 0) {
	    slotMax += c.getPlayer().getSkillLevel(SkillFactory.getSkill(skill)) * 10;
	}
	if (item.getQuantity() < slotMax) {
	    final int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
	    if (c.getPlayer().getMeso() >= price) {
		item.setQuantity(slotMax);
		c.write(MaplePacketCreator.updateInventorySlot(InventoryType.USE, (Item) item, false));
		c.getPlayer().gainMeso(-price, false, true, false);
		c.write(MaplePacketCreator.confirmShopTransaction((byte) 0x8));
	    }
	}
    }

    protected ShopItem findById(int itemId) {
	for (ShopItem item : items) {
	    if (item.getItemId() == itemId)
		return item;
	}
	return null;
    }

    public static Shop createFromDB(int id, boolean isShopId) {
	Shop ret = null;
	int shopId;

	try {
	    Connection con = DatabaseConnection.getConnection();
	    PreparedStatement ps = con.prepareStatement(isShopId ? "SELECT * FROM shops WHERE shopid = ?" : "SELECT * FROM shops WHERE npcid = ?");
	    
	    ps.setInt(1, id);
	    ResultSet rs = ps.executeQuery();
	    if (rs.next()) {
		shopId = rs.getInt("shopid");
		ret = new Shop(shopId, rs.getInt("npcid"));
		rs.close();
		ps.close();
	    } else {
		rs.close();
		ps.close();
		return null;
	    }
	    ps = con.prepareStatement("SELECT * FROM shopitems WHERE shopid = ? ORDER BY position ASC");
	    ps.setInt(1, shopId);
	    rs = ps.executeQuery();
	    List<Integer> recharges = new ArrayList<Integer>(rechargeableItems);
	    while (rs.next()) {
		if (GameConstants.isThrowingStar(rs.getInt("itemid")) || GameConstants.isBullet(rs.getInt("itemid"))) {
		    ShopItem starItem = new ShopItem((short) 1, rs.getInt("itemid"), rs.getInt("price"));
		    ret.addItem(starItem);
		    if (rechargeableItems.contains(starItem.getItemId())) {
			recharges.remove(Integer.valueOf(starItem.getItemId()));
		    }
		} else {
		    ret.addItem(new ShopItem((short) 1000, rs.getInt("itemid"), rs.getInt("price")));
		}
	    }
	    for (Integer recharge : recharges) {
		ret.addItem(new ShopItem((short) 1000, recharge.intValue(), 0));
	    }
	    rs.close();
	    ps.close();
	} catch (SQLException e) {
	    System.err.println("Could not load shop" + e);
	}
	return ret;
    }

    public int getNpcId() {
	return npcId;
    }

    public int getId() {
	return id;
    }
}