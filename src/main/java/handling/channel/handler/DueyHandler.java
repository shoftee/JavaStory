/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import client.Equip;
import client.IItem;
import client.ItemFlag;
import client.GameConstants;
import client.Item;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleInventoryType;
import database.DatabaseConnection;
import org.javastory.io.PacketFormatException;
import org.javastory.io.PacketReader;
import server.MapleDueyActions;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;

public class DueyHandler {

    /*
     * 19 = Successful
     * 18 = One-of-a-kind Item is already in Reciever's delivery
     * 17 = The Character is unable to recieve the parcel
     * 15 = Same account
     * 14 = Name does not exist
     */
    public static final void handleDueyOperation(final PacketReader reader, final MapleClient c) throws PacketFormatException {
        final byte operation = reader.readByte();

        switch (operation) {
            case 1: { // Start Duey, 13 digit AS
                final String AS13Digit = reader.readLengthPrefixedString();
//		int unk = reader.readInt(); // Theres an int here, value = 1
                //  9 = error
                final int conv = c.getPlayer().getConversation();

                if (conv == 2) { // Duey
                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 10, loadItems(c.getPlayer())));
                }
                break;
            }
            case 3: { // Send Item
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final byte inventId = reader.readByte();
                final short itemPos = reader.readShort();
                final short amount = reader.readShort();
                final int mesos = reader.readInt();
                final String recipient = reader.readLengthPrefixedString();
                boolean quickdelivery = reader.readByte() > 0;

                final int finalcost = mesos + GameConstants.getTaxAmount(mesos) + (quickdelivery ? 0 : 5000);

                if (mesos >= 0 && mesos <= 100000000 && c.getPlayer().getMeso() >= finalcost) {
                    final int accid = MapleCharacterUtil.getIdByName(recipient);
                    if (accid != -1) {
                        if (accid != c.getAccID()) {
                            boolean recipientOn = false;
                            /*			    MapleClient rClient = null;
                            try {
                            int channel = c.getChannelServer().getWorldInterface().find(recipient);
                            if (channel > -1) {
                            recipientOn = true;
                            ChannelServer rcserv = ChannelServer.getInstance(channel);
                            rClient = rcserv.getPlayerStorage().getCharacterByName(recipient).getClient();
                            }
                            } catch (RemoteException re) {
                            c.getChannelServer().reconnectWorld();
                            }*/

                            if (inventId > 0) {
                                final MapleInventoryType inv = MapleInventoryType.getByType(inventId);
                                final IItem item = c.getPlayer().getInventory(inv).getItem((byte) itemPos);
                                if (item == null) {
                                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                                    return;
                                }
                                final byte flag = item.getFlag();
                                if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
                                    c.getSession().write(MaplePacketCreator.enableActions());
                                    return;
                                }
                                if (c.getPlayer().getItemQuantity(item.getItemId(), false) >= amount) {
                                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                    if (!ii.isDropRestricted(item.getItemId())) {
                                        if (addItemToDB(item, amount, mesos, c.getPlayer().getName(), accid, recipientOn)) {
                                            if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, item.getQuantity(), true);
                                            } else {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, amount, true, false);
                                            }
                                            c.getPlayer().gainMeso(-finalcost, false);

                                            c.getSession().write(MaplePacketCreator.sendDuey((byte) 19, null)); // Successfull
                                        } else {
                                            c.getSession().write(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessful
                                        }
                                    } else {
                                        c.getSession().write(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                                    }
                                } else {
                                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                                }
                            } else {
                                if (addMesoToDB(mesos, c.getPlayer().getName(), accid, recipientOn)) {
                                    c.getPlayer().gainMeso(-finalcost, false);

                                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 19, null)); // Successfull
                                } else {
                                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                                }
                            }
//                            if (recipientOn && rClient != null) {
                            //                              rClient.getSession().write(MaplePacketCreator.sendDueyMSG(Actions.PACKAGE_MSG.getCode()));
                            //                        }
                        } else {
                            c.getSession().write(MaplePacketCreator.sendDuey((byte) 15, null)); // Same acc error
                        }
                    } else {
                        c.getSession().write(MaplePacketCreator.sendDuey((byte) 14, null)); // Name does not exist
                    }
                } else {
                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 12, null)); // Not enough mesos
                }
                break;
            }
            case 5: { // Recieve Package
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final int packageid = reader.readInt();
                final MapleDueyActions dp = loadSingleItem(packageid, c.getPlayer().getId());
                if (dp == null) {
                    return;
                }
                if (dp.getItem() != null && !MapleInventoryManipulator.checkSpace(c, dp.getItem().getItemId(), dp.getItem().getQuantity(), dp.getItem().getOwner())) {
                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 16, null)); // Not enough Space
                    return;
                } else if (dp.getMesos() < 0 || (dp.getMesos() + c.getPlayer().getMeso()) < 0) {
                    c.getSession().write(MaplePacketCreator.sendDuey((byte) 17, null)); // Unsuccessfull
                    return;
                }
                removeItemFromDB(packageid, c.getPlayer().getId()); // Remove first
                if (dp.getItem() != null) {
                    MapleInventoryManipulator.addFromDrop(c, dp.getItem(), false);
                }
                if (dp.getMesos() != 0) {
                    c.getPlayer().gainMeso(dp.getMesos(), false);
                }
                c.getSession().write(MaplePacketCreator.removeItemFromDuey(false, packageid));
                break;
            }
            case 6: { // Remove package
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                final int packageid = reader.readInt();
                removeItemFromDB(packageid, c.getPlayer().getId());
                c.getSession().write(MaplePacketCreator.removeItemFromDuey(true, packageid));
                break;
            }
            case 8: { // Close Duey
                c.getPlayer().setConversation(0);
                break;
            }
            default: {
                System.out.println("Unhandled Duey operation : " + reader.toString());
                break;
            }
        }
    }

    private static final boolean addMesoToDB(final int mesos, final String sName, final int recipientID, final boolean isOn) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, recipientID);
            ps.setString(2, sName);
            ps.setInt(3, mesos);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, isOn ? 0 : 1);
            ps.setInt(6, 3);

            ps.executeUpdate();
            ps.close();

            return true;
        } catch (SQLException se) {
            return false;
        }
    }

    private static final boolean addItemToDB(final IItem item, final int quantity, final int mesos, final String sName, final int recipientID, final boolean isOn) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type) VALUES (?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, recipientID);
            ps.setString(2, sName);
            ps.setInt(3, mesos);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, isOn ? 0 : 1);

            ps.setInt(6, item.getType());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            PreparedStatement ps2;

            if (item.getType() == 1) { // equips
                ps2 = con.prepareStatement("INSERT INTO dueyitems (PackageId, itemid, quantity, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, owner, GM_Log, flag, expiredate, ViciousHammer, itemLevel, itemEXP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                Equip eq = (Equip) item;
                ps2.setInt(1, rs.getInt(1));
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
                ps2 = con.prepareStatement("INSERT INTO dueyitems (PackageId, itemid, quantity, owner, GM_Log, flag, expiredate) VALUES (?, ?, ?, ?, ?, ?, ?)");
                ps2.setInt(1, rs.getInt(1));
                ps2.setInt(2, item.getItemId());
                ps2.setInt(3, quantity);
                ps2.setString(4, item.getOwner());
                ps2.setString(5, item.getGMLog());
                ps2.setInt(6, item.getFlag());
                ps2.setLong(7, item.getExpiration());
            }
            ps2.executeUpdate();
            ps2.close();
            rs.close();
            ps.close();

            return true;
        } catch (SQLException se) {
            se.printStackTrace();
            return false;
        }
    }

    public static final List<MapleDueyActions> loadItems(final MapleCharacter chr) {
        List<MapleDueyActions> packages = new LinkedList<MapleDueyActions>();
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages LEFT JOIN dueyitems USING (PackageId) WHERE RecieverId = ?");
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MapleDueyActions dueypack = getItemByPID(rs);
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                packages.add(dueypack);
            }
            rs.close();
            ps.close();
            return packages;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }

    public static final MapleDueyActions loadSingleItem(final int packageid, final int charid) {
        List<MapleDueyActions> packages = new LinkedList<MapleDueyActions>();
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages LEFT JOIN dueyitems USING (PackageId) WHERE PackageId = ? and RecieverId = ?");
            ps.setInt(1, packageid);
            ps.setInt(2, charid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                MapleDueyActions dueypack = getItemByPID(rs);
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                packages.add(dueypack);
                rs.close();
                ps.close();
                return dueypack;
            } else {
                rs.close();
                ps.close();
                return null;
            }
        } catch (SQLException se) {
//	    se.printStackTrace();
            return null;
        }
    }

    public static final void reciveMsg(final MapleClient c, final int recipientId) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE dueypackages SET Checked = 0 where RecieverId = ?");
            ps.setInt(1, recipientId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static final void removeItemFromDB(final int packageid, final int charid) {
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ? and RecieverId = ?");
            ps.setInt(1, packageid);
            ps.setInt(2, charid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static final MapleDueyActions getItemByPID(final ResultSet rs) {
        try {
            MapleDueyActions dueypack;
            if (rs.getInt("type") == 1) {
                Equip eq = new Equip(rs.getInt("itemid"), (byte) 0, -1, (byte) 0);
                eq.setUpgradeSlots(rs.getByte("upgradeslots"));
                eq.setLevel(rs.getByte("level"));
                eq.setStr(rs.getShort("str"));
                eq.setDex(rs.getShort("dex"));
                eq.setInt(rs.getShort("int"));
                eq.setLuk(rs.getShort("luk"));
                eq.setHp(rs.getShort("hp"));
                eq.setMp(rs.getShort("mp"));
                eq.setWatk(rs.getShort("watk"));
                eq.setMatk(rs.getShort("matk"));
                eq.setWdef(rs.getShort("wdef"));
                eq.setMdef(rs.getShort("mdef"));
                eq.setAcc(rs.getShort("acc"));
                eq.setAvoid(rs.getShort("avoid"));
                eq.setHands(rs.getShort("hands"));
                eq.setSpeed(rs.getShort("speed"));
                eq.setJump(rs.getShort("jump"));
                eq.setOwner(rs.getString("owner"));
                eq.setFlag(rs.getByte("flag"));
                eq.setExpiration(rs.getLong("expiredate"));
                eq.setViciousHammer(rs.getByte("ViciousHammer"));
                eq.setItemLevel(rs.getByte("itemLevel"));
                eq.setItemEXP(rs.getShort("itemEXP"));
                eq.setGMLog(rs.getString("GM_Log"));
                dueypack = new MapleDueyActions(rs.getInt("PackageId"), eq);

            } else if (rs.getInt("type") == 2) {
                Item newItem = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"), (byte) 0);
                newItem.setOwner(rs.getString("owner"));
                newItem.setFlag(rs.getByte("flag"));
                newItem.setExpiration(rs.getLong("expiredate"));
                newItem.setGMLog(rs.getString("GM_Log"));
                dueypack = new MapleDueyActions(rs.getInt("PackageId"), newItem);
            } else {
                dueypack = new MapleDueyActions(rs.getInt("PackageId"));
            }
            return dueypack;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }
}