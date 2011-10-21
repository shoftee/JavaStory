package javastory.channel.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.Pet;
import javastory.channel.client.SkillFactory;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.db.Database;
import javastory.game.GameConstants;
import javastory.game.InventoryType;
import javastory.game.Skills;
import javastory.server.ItemInfoProvider;
import javastory.server.ShopItem;
import javastory.tools.packets.ChannelPackets;

public class Shop {
    private static final Set<Integer> rechargeableItems = new LinkedHashSet<>();
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
	items = new LinkedList<>();
    }

    public void addItem(ShopItem item) {
	items.add(item);
    }

    public void sendShop(ChannelClient c) {
	c.getPlayer().setShop(this);
	c.write(ChannelPackets.getNPCShop(c, getNpcId(), items));
    }

    public void buy(ChannelClient c, int itemId, short quantity) {
	if (quantity <= 0) {
	    AutobanManager.getInstance().addPoints(c, 1000, 0, "Buying " + quantity + " " + itemId);
	    return;
	}
	ShopItem item = findById(itemId);
	if (item != null && item.getPrice() > 0) {
            final ChannelCharacter player = c.getPlayer();
	    if (player.getMeso() >= item.getPrice() * quantity) {
		if (InventoryManipulator.checkSpace(c, itemId, quantity, "")) {
		    if (GameConstants.isPet(itemId)) {
			InventoryManipulator.addById(c, itemId, quantity, null, Pet.createPet(itemId));
		    } else {
			ItemInfoProvider ii = ItemInfoProvider.getInstance();

			if (GameConstants.isRechargable(itemId)){
			    quantity = ii.getSlotMax(item.getItemId());
			    player.gainMeso(-(item.getPrice()), false);
			    InventoryManipulator.addById(c, itemId, quantity);
			} else {
			    player.gainMeso(-(item.getPrice() * quantity), false);
			    InventoryManipulator.addById(c, itemId, quantity);
			}
		    }
		} else {
		    player.sendNotice(1, "Your Inventory is full");
		}
		c.write(ChannelPackets.confirmShopTransaction((byte) 0));
	    }
	}
    }

    public void sell(ChannelClient c, Inventory inventory, byte slot, short quantity) {
	if (quantity == 0xFFFF || quantity == 0) {
	    quantity = 1;
	}
	IItem item = inventory.getItem(slot);

	if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
	    quantity = item.getQuantity();
	}
	if (quantity < 0) {
	    AutobanManager.getInstance().addPoints(c, 1000,	0, "Selling " + quantity + " " + item.getItemId() + " (" + inventory.getType().name() + "/" + slot + ")");
	    return;
	}
	short iQuant = item.getQuantity();
	if (iQuant == 0xFFFF) {
	    iQuant = 1;
	}
	final ItemInfoProvider ii = ItemInfoProvider.getInstance();
	if (quantity <= iQuant && iQuant > 0) {
	    InventoryManipulator.removeFromSlot(c, inventory, slot, quantity, false);
	    double price;
	    if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
		price = ii.getWholePrice(item.getItemId()) / (double) ii.getSlotMax(item.getItemId());
	    } else {
		price = ii.getPrice(item.getItemId());
	    }
	    final int recvMesos = (int) Math.max(Math.ceil(price * quantity), 0);
	    if (price != -1 && recvMesos > 0) {
		c.getPlayer().gainMeso(recvMesos, false);
	    }
	    c.write(ChannelPackets.confirmShopTransaction((byte) 0x8));
	}
    }

    public void recharge(final ChannelClient c, final byte slot) {
        final ChannelCharacter player = c.getPlayer();
	final IItem item = player.getUseInventory().getItem(slot);

	if (item == null || (!GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId()))) {
	    return;
	}
	final ItemInfoProvider ii = ItemInfoProvider.getInstance();
	short slotMax = ii.getSlotMax(item.getItemId());
	final int skill = Skills.getMasterySkillId(player.getJobId());

	if (skill != 0) {
	    slotMax += player.getCurrentSkillLevel(SkillFactory.getSkill(skill)) * 10;
	}
	if (item.getQuantity() < slotMax) {
	    final int price = (int) Math.round(ii.getPrice(item.getItemId()) * (slotMax - item.getQuantity()));
	    if (player.getMeso() >= price) {
		item.setQuantity(slotMax);
		c.write(ChannelPackets.updateInventorySlot(InventoryType.USE, item, false));
		player.gainMeso(-price, false, true, false);
		c.write(ChannelPackets.confirmShopTransaction((byte) 0x8));
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
	    Connection con = Database.getConnection();
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
	    List<Integer> recharges = new ArrayList<>(rechargeableItems);
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