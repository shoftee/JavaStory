package handling.channel.handler;

import java.rmi.RemoteException;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.Equip;
import client.IItem;
import client.Item;
import client.InventoryType;
import client.ChannelClient;
import client.ChannelCharacter;
import client.GameConstants;
import database.DatabaseConnection;
import org.javastory.io.PacketFormatException;
import server.InventoryManipulator;
import server.MerchItemPackage;
import tools.packet.PlayerShopPacket;
import org.javastory.io.PacketReader;

public final class HiredMerchantHandler {

    private HiredMerchantHandler() {
    }

    public static void handleUseHiredMerchant(final PacketReader reader, final ChannelClient c) {
//	reader.readInt(); // TimeStamp

        if (c.getPlayer().getMap().allowPersonalShop()) {
            final byte state = checkExistance(c.getAccountId());

            switch (state) {
                case 1:
                    c.getPlayer().sendNotice(1, "Please claim your items from Fredrick first.");
                    break;
                case 0:
                    boolean merch = true;
                    try {
                        merch = c.getChannelServer().getWorldInterface().hasMerchant(c.getAccountId());
                    } catch (RemoteException re) {
                        c.getChannelServer().pingWorld();
                    }
                    if (!merch) {
//		    c.getPlayer().dropMessage(1, "The Hired Merchant is temporary disabled until it's fixed.");
                        c.write(PlayerShopPacket.sendTitleBox());
                    } else {
                        c.getPlayer().sendNotice(1, "Please close the existing store and try again.");
                    }
                    break;
                default:
                    c.getPlayer().sendNotice(1, "An unknown error occured.");
                    break;
            }
        } else {
            c.disconnect(true);
        }
    }

    private static byte checkExistance(final int accid) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * from hiredmerch where accountid = ?");
            ps.setInt(1, accid);

            if (ps.executeQuery().next()) {
                ps.close();
                return 1;
            }
            ps.close();
            return 0;
        } catch (SQLException se) {
            return -1;
        }
    }

    public static void handleMerchantItemStore(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
        final byte operation = reader.readByte();

        switch (operation) {
            case 20: {
                final String passport = reader.readLengthPrefixedString();

                final int conv = c.getPlayer().getConversationState();

                if (conv == 3) { // Hired Merch
                    final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getId());

                    if (pack == null) {
                        c.getPlayer().sendNotice(1, "You do not have any item(s) with Fredrick.");
                        c.getPlayer().setConversationState(0);
                    } else {
                        c.write(PlayerShopPacket.merchItemStore_ItemData(pack));
                    }
                }
                break;
            }
            case 25: { // Request take out iteme
                if (c.getPlayer().getConversationState() != 3) {
                    return;
                }
                c.write(PlayerShopPacket.merchItemStore((byte) 0x24));
                break;
            }
            case 26: { // Take out item
                if (c.getPlayer().getConversationState() != 3) {
                    return;
                }
                final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getId());

                if (!check(c.getPlayer(), pack)) {
                    c.write(PlayerShopPacket.merchItem_Message((byte) 0x21));
                    return;
                }
                if (deletePackage(c.getPlayer().getId())) {
                    c.getPlayer().gainMeso(pack.getMesos(), false);
                    for (IItem item : pack.getItems()) {
                        InventoryManipulator.addFromDrop(c, item, false);
                    }
                    c.write(PlayerShopPacket.merchItem_Message((byte) 0x1d));
                } else {
                    c.getPlayer().sendNotice(1, "An unknown error occured.");
                }
                break;
            }
            case 27: { // Exit
                c.getPlayer().setConversationState(0);
                break;
            }
        }
    }

    private static boolean check(final ChannelCharacter chr, final MerchItemPackage pack) {
        if (chr.getMeso() + pack.getMesos() < 0) {
            return false;
        }
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        for (IItem item : pack.getItems()) {
            final InventoryType invtype = GameConstants.getInventoryType(item.getItemId());
            if (invtype == InventoryType.EQUIP) {
                eq++;
            } else if (invtype == InventoryType.USE) {
                use++;
            } else if (invtype == InventoryType.SETUP) {
                setup++;
            } else if (invtype == InventoryType.ETC) {
                etc++;
            } else if (invtype == InventoryType.CASH) {
                cash++;
            }
        }
        if (chr.getEquipInventory().getNumFreeSlot() <= eq ||
                chr.getUseInventory().getNumFreeSlot() <= use ||
                chr.getSetupInventory().getNumFreeSlot() <= setup ||
                chr.getEtcInventory().getNumFreeSlot() <= etc ||
                chr.getCashInventory().getNumFreeSlot() <= cash) {
            return false;
        }
        return true;
    }

    private static boolean deletePackage(final int charid) {
        final Connection con = DatabaseConnection.getConnection();

        try {
            PreparedStatement ps = con.prepareStatement("DELETE from hiredmerch where characterid = ?");
            ps.setInt(1, charid);
            ps.execute();
            ps.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static MerchItemPackage loadItemFrom_Database(final int charid) {
        final Connection con = DatabaseConnection.getConnection();

        try {
            PreparedStatement ps = con.prepareStatement("SELECT * from hiredmerch where characterid = ?");
            ps.setInt(1, charid);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                ps.close();
                rs.close();
                return null;
            }
            final int packageid = rs.getInt("PackageId");

            final MerchItemPackage pack = new MerchItemPackage();
            pack.setPackageid(packageid);
            pack.setMesos(rs.getInt("Mesos"));
            pack.setSentTime(rs.getLong("time"));

            ps.close();
            rs.close();

            List<IItem> items = new ArrayList<IItem>();

            PreparedStatement ps2 = con.prepareStatement("SELECT * from hiredmerchitems where PackageId = ?");
            ps2.setInt(1, packageid);
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
                final int itemid = rs2.getInt("itemid");
                final InventoryType type = GameConstants.getInventoryType(itemid);

                if (type == InventoryType.EQUIP) {
                    final Equip equip = new Equip(rs2.getInt("itemid"), (byte) 0, -1, rs2.getByte("flag"));
                    equip.setOwner(rs2.getString("owner"));
                    equip.setQuantity(rs2.getShort("quantity"));
                    equip.setAcc(rs2.getShort("acc"));
                    equip.setAvoid(rs2.getShort("avoid"));
                    equip.setDex(rs2.getShort("dex"));
                    equip.setHands(rs2.getShort("hands"));
                    equip.setHp(rs2.getShort("hp"));
                    equip.setInt(rs2.getShort("int"));
                    equip.setJump(rs2.getShort("jump"));
                    equip.setLuk(rs2.getShort("luk"));
                    equip.setMatk(rs2.getShort("matk"));
                    equip.setMdef(rs2.getShort("mdef"));
                    equip.setMp(rs2.getShort("mp"));
                    equip.setSpeed(rs2.getShort("speed"));
                    equip.setStr(rs2.getShort("str"));
                    equip.setWatk(rs2.getShort("watk"));
                    equip.setWdef(rs2.getShort("wdef"));
                    equip.setItemLevel(rs2.getByte("itemLevel"));
                    equip.setItemEXP(rs2.getShort("itemEXP"));
                    equip.setViciousHammer(rs2.getByte("ViciousHammer"));
                    equip.setUpgradeSlots(rs2.getByte("upgradeslots"));
                    equip.setLevel(rs2.getByte("level"));
                    equip.setFlag(rs2.getByte("flag"));
                    equip.setExpiration(rs2.getLong("expiredate"));
                    equip.setGMLog(rs2.getString("GM_Log"));

                    items.add(equip);
                } else {
                    final IItem item = new Item(rs2.getInt("itemid"), (byte) 0, rs2.getShort("quantity"), rs2.getByte("flag"));
                    item.setOwner(rs2.getString("owner"));
                    item.setFlag(rs2.getByte("flag"));
                    item.setExpiration(rs2.getLong("expiredate"));
                    item.setGMLog(rs2.getString("GM_Log"));

                    items.add(item);
                }
            }
            ps.close();
            rs.close();

            pack.setItems(items);

            return pack;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}