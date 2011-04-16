package handling.cashshop.handler;

import java.rmi.RemoteException;
import java.util.Calendar;

import client.GameConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleInventoryType;
import client.MaplePet;
import handling.world.CharacterTransfer;
import handling.world.remote.CashShopInterface;
import org.javastory.io.PacketFormatException;
import org.javastory.server.cashshop.CashShopServer;
import server.CashItemFactory;
import server.CashItemInfo;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.packet.MTSCSPacket;
import org.javastory.io.PacketReader;

public class CashShopOperation {

    public static void leaveCashShop(final PacketReader reader, final MapleClient c, final MapleCharacter chr) {
        final CashShopServer cs = CashShopServer.getInstance();
        cs.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        try {
            final CashShopInterface wci = cs.getCSInterface();
            wci.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannelId());
            final String ip = wci.getChannelIP(c.getChannelId());
            final String[] socket = ip.split(":");
            c.getSession().write(MaplePacketCreator.getChannelChange(Integer.parseInt(socket[1])));
        } catch (RemoteException e) {
            c.getChannelServer().pingWorld();
        } finally {
            c.getSession().close(true);
            chr.saveToDb(false, true);
            c.setPlayer(null);
        }
    }

    public static void enterCashShop(final int playerid, final MapleClient c) {
        final CashShopServer cs = CashShopServer.getInstance();
        final CharacterTransfer transfer = cs.getPlayerStorage().getPendingCharacter(playerid);
        if (transfer == null) {
            c.getSession().close(true);
            return;
        }
        MapleCharacter chr = MapleCharacter.reconstructCharacter(transfer, c, false);
        c.setPlayer(chr);
        c.setAccID(chr.getAccountID());
        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close(true);
            return;
        }
        final int state = c.getLoginState();
        boolean allowLogin = false;
        try {
            if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
                if (!cs.getCSInterface().isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                    allowLogin = true;
                }
            }
        } catch (RemoteException e) {
            cs.pingWorld();
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close(true);
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        cs.getPlayerStorage().registerPlayer(chr);
        c.getSession().write(MTSCSPacket.warpCS(c));
        c.getSession().write(MTSCSPacket.enableUse0());
        c.getSession().write(MTSCSPacket.enableUse1());
        c.getSession().write(MTSCSPacket.enableUse2());
        c.getSession().write(MTSCSPacket.enableUse3());
        c.getSession().write(MTSCSPacket.showNXMapleTokens(chr));
        c.getSession().write(MTSCSPacket.sendWishList(chr, false));
    }

    public static final void handleCashShopUpdate(final MapleClient c, final MapleCharacter chr) {
        c.getSession().write(MTSCSPacket.showNXMapleTokens(c.getPlayer()));
    }

    public static final void handleBuyCashItem(final PacketReader reader, final MapleClient c, final MapleCharacter chr) throws PacketFormatException {
        final int action = reader.readByte();
        if (action == 3) {
            reader.skip(1);
            final CashItemInfo item = CashItemFactory.getInstance().getItem(reader.readInt());
            if (item != null && chr.getCSPoints(1) >= item.getPrice()) {
                if (chr.getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() > -1) {
                    chr.modifyCSPoints(1, -item.getPrice(), false);
                    if (GameConstants.isPet(item.getId())) {
                        final MaplePet pet = MaplePet.createPet(item.getId());
                        if (pet != null) {
                            MapleInventoryManipulator.addById(c, item.getId(), (short) 1, null, pet, item.getPeriod());
                        }
                    } else {
                        MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount(), null, null, item.getPeriod());
                    }
                    c.getSession().write(MTSCSPacket.showBoughtCSItem(item.getId(), c.getAccID()));
                    System.out.println(":: " + c.getAccID() + " has bought " + item.getId() + " from the cash shop ::");
                }
            }
        } else if (action == 5) { // Wishlist
            chr.clearWishlist();
            int[] wishlist = new int[10];
            for (int i = 0; i < 10; i++) {
                wishlist[i] = reader.readInt();
            }
            c.getSession().write(MTSCSPacket.sendWishList(chr, true));
        } else if (action == 6) { // Increase inv
            reader.skip(1);
            final boolean coupon = reader.readByte() > 0;
            if (coupon) {
                final MapleInventoryType type = getInventoryType(reader.readInt());
                if (chr.getCSPoints(1) >= 12000 && chr.getInventory(type).getSlotLimit() < 96) {
                    chr.modifyCSPoints(1, -12000, false);
                    chr.getInventory(type).addSlot((byte) 8);
                    chr.dropMessage(1, "Inventory slot increased");
                } else {
                    chr.dropMessage(1, "You have reached the Maxinum inventory slot.");
                }
            } else {
                final MapleInventoryType type = MapleInventoryType.getByType(reader.readByte());
                if (chr.getCSPoints(1) >= 8000 && chr.getInventory(type).getSlotLimit() < 96) {
                    chr.modifyCSPoints(1, -8000, false);
                    chr.getInventory(type).addSlot((byte) 4);
                    chr.dropMessage(1, "Inventory slot increased");
                } else {
                    chr.dropMessage(1, "You have reached the Maxinum inventory slot.");
                }
            }
        } else if (action == 7) { // Increase slot space
            if (chr.getCSPoints(1) >= 8000 && chr.getStorage().getSlots() < 48) {
                chr.modifyCSPoints(1, -8000, false);
                chr.getStorage().increaseSlots((byte) 4);
                chr.dropMessage(1, "Storage slot increased");
            } else {
                chr.dropMessage(1, "You have reached the max storage slot.");
            }
        } else if (action == 14) { // transferFromCSToInv
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (action == 15) { // transferFromInvToCS
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (action == 30 || action == 36) {
            final int idate = reader.readInt();
            final int toCharge = reader.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(reader.readInt());
            final String recipient = reader.readLengthPrefixedString();
            final String msg = reader.readLengthPrefixedString();
            final int year = idate / 10000;
            final int month = (idate - year * 10000) / 100;
            final int day = idate - year * 10000 - month * 100;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(0);
            cal.set(year, month - 1, day);
        } else if (action == 31) { // cash package
            c.getSession().write(MaplePacketCreator.enableActions());
        } else if (action == 33) { // quest item
            final CashItemInfo item = CashItemFactory.getInstance().getItem(reader.readInt());
            if (item != null && chr.getMeso() >= item.getPrice()) {
                if (MapleItemInformationProvider.getInstance().isQuestItem(item.getId())) {
                    if (chr.getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() > -1) {
                        chr.gainMeso(-item.getPrice(), false);
                        MapleInventoryManipulator.addById(c, item.getId(), (short) item.getCount());
                        c.getSession().write(MTSCSPacket.showBoughtCSQuestItem(chr.getInventory(MapleInventoryType.ETC).findById(item.getId()).getPosition(), item.getId()));
                    }
                }
            }
        }
        c.getSession().write(MTSCSPacket.showNXMapleTokens(chr));
    }

    private static final MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 50200075:
                return MapleInventoryType.EQUIP;
            case 50200074:
                return MapleInventoryType.USE;
            case 50200073:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }
}