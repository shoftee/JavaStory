package javastory.channel.shops;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.ref.WeakReference;

import client.IItem;
import client.Equip;
import javastory.channel.ChannelCharacter;
import javastory.client.ItemType;
import javastory.db.DatabaseConnection;
import handling.GamePacket;
import java.sql.Statement;
import javastory.channel.maps.AbstractGameMapObject;
import javastory.channel.packet.PlayerShopPacket;

public abstract class AbstractPlayerShop extends AbstractGameMapObject implements PlayerShop {

    private boolean open;
    private String ownerName, des;
    private int ownerId, owneraccount, itemId;
    private AtomicInteger meso = new AtomicInteger(0);
    protected WeakReference[] visitors = new WeakReference[3];
    protected List<PlayerShopItem> items = new LinkedList<>();

    public AbstractPlayerShop(ChannelCharacter owner, int itemId, String desc) {
        this.setPosition(owner.getPosition());
        this.ownerName = owner.getName();
        this.ownerId = owner.getId();
        this.owneraccount = owner.getAccountId();
        this.itemId = itemId;
        this.des = desc;
        this.open = false;
    }

    @Override
    public void broadcastToVisitors(GamePacket packet) {
        broadcastToVisitors(packet, true);
    }

    public void broadcastToVisitors(GamePacket packet, boolean owner) {
        ChannelCharacter visitor;
        for (int i = 0; i < 3; i++) {
            visitor = (ChannelCharacter) visitors[i].get();
            if (visitor != null) {
                visitor.getClient().write(packet);
            }
        }
        if (getShopType() == 2 && owner) {
            ((GenericPlayerStore) this).getMCOwner().getClient().write(packet);
        }
    }

    @Override
    public int getMeso() {
        return meso.get();
    }

    @Override
    public void setMeso(int meso) {
        this.meso.set(meso);
    }

    public void removeVisitors() {
        ChannelCharacter chr = (ChannelCharacter) visitors[0].get();
        if (chr != null) {
            removeVisitor(chr);
        }
        chr = (ChannelCharacter) visitors[1].get();
        if (chr != null) {
            removeVisitor(chr);
        }
        chr = (ChannelCharacter) visitors[2].get();
        if (chr != null) {
            removeVisitor(chr);
        }
    }

    @Override
    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    public boolean saveItems() {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO hiredmerch (characterid, accountid, Mesos, time) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, ownerId);
            ps.setInt(2, owneraccount);
            ps.setInt(3, meso.get());
            ps.setLong(4, System.currentTimeMillis());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            final int packageid = rs.getInt(1);
            rs.close();
            ps.close();

            PreparedStatement ps2;

            for (PlayerShopItem pItems : items) {
                if (pItems.bundles <= 0) {
                    continue;
                }
                final IItem item = pItems.item;

                if (item.getType() == ItemType.EQUIP) { // equips
                    ps2 = con.prepareStatement("INSERT INTO hiredmerchitems (PackageId, itemid, quantity, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, owner, GM_Log, flag, expiredate, ViciousHammer, itemLevel, itemEXP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    final Equip eq = (Equip) item;
                    ps2.setInt(1, packageid);
                    ps2.setInt(2, eq.getItemId());
                    ps2.setInt(3, 1); // Quantity
                    ps2.setInt(4, eq.getUpgradeSlots());
                    ps2.setInt(5, eq.getLevel());
                    ps2.setInt(6, eq.getStr());
                    ps2.setInt(7, eq.getDex());
                    ps2.setInt(8, eq.getInt());
                    ps2.setInt(9, eq.getLuk());
                    ps2.setInt(10, eq.getHp());
                    ps2.setInt(11, eq.getMp());
                    ps2.setInt(12, eq.getWatk());
                    ps2.setInt(13, eq.getMatk());
                    ps2.setInt(14, eq.getWdef());
                    ps2.setInt(15, eq.getMdef());
                    ps2.setInt(16, eq.getAcc());
                    ps2.setInt(17, eq.getAvoid());
                    ps2.setInt(18, eq.getHands());
                    ps2.setInt(19, eq.getSpeed());
                    ps2.setInt(20, eq.getJump());
                    ps2.setString(21, eq.getOwner());
                    ps2.setString(22, eq.getGMLog());
                    ps2.setInt(23, eq.getFlag());
                    ps2.setLong(24, eq.getExpiration());
                    ps2.setInt(25, eq.getViciousHammer());
                    ps2.setInt(26, eq.getItemLevel());
                    ps2.setInt(27, eq.getItemEXP());
                } else {
                    ps2 = con.prepareStatement("INSERT INTO hiredmerchitems (PackageId, itemid, quantity, owner, GM_Log, flag, expiredate) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    ps2.setInt(1, packageid);
                    ps2.setInt(2, item.getItemId());
                    ps2.setInt(3, pItems.bundles * item.getQuantity());
                    ps2.setString(4, item.getOwner());
                    ps2.setString(5, item.getGMLog());
                    ps2.setInt(6, item.getFlag());
                    ps2.setLong(7, item.getExpiration());
                }
                ps2.execute();
                ps2.close();
            }
            return true;
        } catch (SQLException se) {
            se.printStackTrace();
        }
        return false;
    }

    public ChannelCharacter getVisitor(int num) {
        if (1 <= num && num <= 3) {
            return (ChannelCharacter) visitors[num - 1].get();
        }
        return null;
    }

    @Override
    public void addVisitor(ChannelCharacter visitor) {
        int i = getFreeSlot();
        if (i > -1) {
            broadcastToVisitors(PlayerShopPacket.shopVisitorAdd(visitor, i));
            visitors[i - 1] = new WeakReference<ChannelCharacter>(visitor);

            if (i == 3) {
                if (getShopType() == 1) {
                    final HiredMerchantStore hiredMerchant = (HiredMerchantStore) this;
                    (hiredMerchant).getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant(hiredMerchant));
                } else {
                    final ChannelCharacter owner = ((GenericPlayerStore) this).getMCOwner();
                    owner.getMap().broadcastMessage(PlayerShopPacket.sendPlayerShopBox(owner));
                }
            }
        }
    }

    @Override
    public void removeVisitor(ChannelCharacter visitor) {
        final byte slot = getVisitorSlot(visitor);
        boolean shouldUpdate = getFreeSlot() == -1;
        if (slot != -1) {
            broadcastToVisitors(PlayerShopPacket.shopVisitorLeave(slot));
            visitors[slot - 1] = new WeakReference<ChannelCharacter>(null);

            if (shouldUpdate) {
                if (getShopType() == 1) {
                    final HiredMerchantStore hiredMerchant = (HiredMerchantStore) this;
                    hiredMerchant.getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant(hiredMerchant));
                } else {
                    final ChannelCharacter owner = ((GenericPlayerStore) this).getMCOwner();
                    owner.getMap().broadcastMessage(PlayerShopPacket.sendPlayerShopBox(owner));
                }
            }
        }
    }

    @Override
    public byte getVisitorSlot(ChannelCharacter visitor) {
        ChannelCharacter chr;
        for (byte i = 1; i <= 3; i++) {
            chr = (ChannelCharacter) visitors[i - 1].get();
            if (chr == visitor) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void removeAllVisitors(int error, int type) {
        for (int i = 1; i <= 3; i++) {
            ChannelCharacter visitor = getVisitor(i);
            if (visitor != null) {
                if (type != -1) {
                    visitor.getClient().write(PlayerShopPacket.shopErrorMessage(error, type));
                }
                visitor.setPlayerShop(null);
                visitors[i - 1] = new WeakReference<>(null);
            }
        }
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public int getOwnerAccountId() {
        return owneraccount;
    }

    @Override
    public String getDescription() {
        if (des == null) {
            return "";
        }
        return des;
    }

    @Override
    public List<ChannelCharacter> getVisitors() {
        List<ChannelCharacter> chars = new LinkedList<>();
        ChannelCharacter visitor;
        for (int i = 1; i <= 3; i++) {
            visitor = (ChannelCharacter) visitors[i - 1].get();
            if (visitor != null) {
                chars.add(visitor);
            }
        }
        return chars;
    }

    @Override
    public List<PlayerShopItem> getItems() {
        return items;
    }

    @Override
    public void addItem(PlayerShopItem item) {
        items.add(item);
    }

    @Override
    public boolean removeItem(int item) {
        return false;
    }

    @Override
    public void removeFromSlot(int slot) {
        items.remove(slot);
    }

    @Override
    public byte getFreeSlot() {
        for (byte i = 1; i <= 3; i ++) {
            if (visitors[i].get() == null) return i;
        }
        return -1;
    }

    @Override
    public int getItemId() {
        return itemId;
    }

    @Override
    public boolean isOwner(ChannelCharacter chr) {
        return chr.getId() == ownerId && chr.getName().equals(ownerName);
    }
}
