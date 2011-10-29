package javastory.channel.handling;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.server.InventoryManipulator;
import javastory.client.GameCharacterUtil;
import javastory.db.Database;
import javastory.game.Equip;
import javastory.game.GameConstants;
import javastory.game.IItem;
import javastory.game.Inventory;
import javastory.game.Item;
import javastory.game.ItemFlag;
import javastory.game.ItemType;
import javastory.game.data.ItemInfoProvider;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.server.DueyActions;
import javastory.tools.packets.ChannelPackets;

public class DueyHandler {

	/*
	 * 19 = Successful 18 = One-of-a-kind Item is already in Reciever's delivery
	 * 17 = The Character is unable to recieve the parcel 15 = Same account 14 =
	 * Name does not exist
	 */
	public static void handleDueyOperation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		final byte operation = reader.readByte();
		final ChannelCharacter player = c.getPlayer();

		switch (operation) {
		case 1: {
			// Start Duey, 13 digit AS
			// Read 13-digit passport ID.
			reader.readLengthPrefixedString();
			// Theres an int here, value = 1
			// int unk = reader.readInt();
			// 9 = error
			final int conv = player.getConversationState();

			if (conv == 2) { // Duey
				c.write(ChannelPackets.sendDuey((byte) 10, loadItems(player)));
			}
			break;
		}
		case 3: { // Send Item
			if (player.getConversationState() != 2) {
				return;
			}
			final byte inventoryId = reader.readByte();
			final short itemPos = reader.readShort();
			final short amount = reader.readShort();
			final int mesos = reader.readInt();
			final String recipient = reader.readLengthPrefixedString();
			boolean quickdelivery = reader.readByte() > 0;

			final int finalcost = mesos + GameConstants.getTaxAmount(mesos) + (quickdelivery ? 0 : 5000);

			if (mesos >= 0 && mesos <= 100000000 && player.getMeso() >= finalcost) {
				final int accid = GameCharacterUtil.getIdByName(recipient);
				if (accid != -1) {
					if (accid != c.getAccountId()) {
						boolean recipientOn = false;
						/*
						 * GameClient rClient = null; try { int channel =
						 * ChannelServer
						 * .getInstance().getWorldInterface().find(recipient);
						 * if (channel > -1) { recipientOn = true; ChannelServer
						 * rcserv = ChannelServer.getInstance(channel); rClient
						 * =
						 * rcserv.getPlayerStorage().getCharacterByName(recipient
						 * ).getClient(); } } catch (RemoteException re) {
						 * ChannelServer.getInstance().reconnectWorld(); }
						 */

						if (inventoryId > 0) {
							final Inventory inventory = player.getInventoryByTypeByte(inventoryId);
							final IItem item = inventory.getItem((byte) itemPos);
							if (item == null) {
								c.write(ChannelPackets.sendDuey((byte) 17, null)); // Unsuccessfull
								return;
							}
							final byte flag = item.getFlag();
							if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
								c.write(ChannelPackets.enableActions());
								return;
							}
							if (player.getItemQuantity(item.getItemId(), false) >= amount) {
								final ItemInfoProvider ii = ItemInfoProvider.getInstance();
								if (!ii.isDropRestricted(item.getItemId())) {
									if (addItemToDB(item, amount, mesos, player.getName(), accid, recipientOn)) {
										if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
											InventoryManipulator.removeFromSlot(c, inventory, (byte) itemPos, item.getQuantity(), true);
										} else {
											InventoryManipulator.removeFromSlot(c, inventory, (byte) itemPos, amount, true, false);
										}
										player.gainMeso(-finalcost, false);

										c.write(ChannelPackets.sendDuey((byte) 19, null)); // Successfull
									} else {
										c.write(ChannelPackets.sendDuey((byte) 17, null)); // Unsuccessful
									}
								} else {
									c.write(ChannelPackets.sendDuey((byte) 17, null)); // Unsuccessfull
								}
							} else {
								c.write(ChannelPackets.sendDuey((byte) 17, null)); // Unsuccessfull
							}
						} else {
							if (addMesoToDB(mesos, player.getName(), accid, recipientOn)) {
								player.gainMeso(-finalcost, false);

								c.write(ChannelPackets.sendDuey((byte) 19, null)); // Successfull
							} else {
								c.write(ChannelPackets.sendDuey((byte) 17, null)); // Unsuccessfull
							}
						}
//                            if (recipientOn && rClient != null) {
						// rClient.write(MaplePacketCreator.sendDueyMSG(Actions.PACKAGE_MSG.getCode()));
						// }
					} else {
						c.write(ChannelPackets.sendDuey((byte) 15, null)); // Same
																			// acc
																			// error
					}
				} else {
					c.write(ChannelPackets.sendDuey((byte) 14, null)); // Name
																		// does
																		// not
																		// exist
				}
			} else {
				c.write(ChannelPackets.sendDuey((byte) 12, null)); // Not enough
																	// mesos
			}
			break;
		}
		case 5: { // Recieve Package
			if (player.getConversationState() != 2) {
				return;
			}
			final int packageid = reader.readInt();
			final DueyActions dp = loadSingleItem(packageid, player.getId());
			if (dp == null) {
				return;
			}
			if (dp.getItem() != null && !InventoryManipulator.checkSpace(c, dp.getItem().getItemId(), dp.getItem().getQuantity(), dp.getItem().getOwner())) {
				c.write(ChannelPackets.sendDuey((byte) 16, null)); // Not enough
																	// Space
				return;
			} else if (dp.getMesos() < 0 || (dp.getMesos() + player.getMeso()) < 0) {
				c.write(ChannelPackets.sendDuey((byte) 17, null)); // Unsuccessfull
				return;
			}
			removeItemFromDB(packageid, player.getId()); // Remove first
			if (dp.getItem() != null) {
				InventoryManipulator.addFromDrop(c, dp.getItem(), false);
			}
			if (dp.getMesos() != 0) {
				player.gainMeso(dp.getMesos(), false);
			}
			c.write(ChannelPackets.removeItemFromDuey(false, packageid));
			break;
		}
		case 6: { // Remove package
			if (player.getConversationState() != 2) {
				return;
			}
			final int packageid = reader.readInt();
			removeItemFromDB(packageid, player.getId());
			c.write(ChannelPackets.removeItemFromDuey(true, packageid));
			break;
		}
		case 8: { // Close Duey
			player.setConversationState(0);
			break;
		}
		default: {
			System.out.println("Unhandled Duey operation : " + reader.toString());
			break;
		}
		}
	}

	private static boolean addMesoToDB(final int mesos, final String sName, final int recipientID, final boolean isOn) {
		Connection con = Database.getConnection();
		try {
			final String insertPackage = "INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type) VALUES (?, ?, ?, ?, ?, ?)";
			PreparedStatement ps = con.prepareStatement(insertPackage);
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

	private static boolean addItemToDB(final IItem item, final int quantity, final int mesos, final String sName, final int recipientID, final boolean isOn) {
		Connection con = Database.getConnection();
		try {
			final String insertPackage = "INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type) VALUES (?, ?, ?, ?, ?, ?)";
			PreparedStatement ps = con.prepareStatement(insertPackage, Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, recipientID);
			ps.setString(2, sName);
			ps.setInt(3, mesos);
			ps.setLong(4, System.currentTimeMillis());
			ps.setInt(5, isOn ? 0 : 1);

			ps.setInt(6, item.getType().asByte());
			ps.executeUpdate();

			ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			PreparedStatement ps2;

			if (item.getType() == ItemType.EQUIP) {
				// equips
				final String insertEquip = "INSERT INTO dueyitems (PackageId, itemid, quantity, upgradeslots, level, str, dex, `int`, luk, hp, mp, watk, matk, wdef, mdef, acc, avoid, hands, speed, jump, owner, GM_Log, flag, expiredate, ViciousHammer, itemLevel, itemEXP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				ps2 = con.prepareStatement(insertEquip);
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
				final String insertItem = "INSERT INTO dueyitems (PackageId, itemid, quantity, owner, GM_Log, flag, expiredate) VALUES (?, ?, ?, ?, ?, ?, ?)";
				ps2 = con.prepareStatement(insertItem);
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

	public static List<DueyActions> loadItems(final ChannelCharacter chr) {
		List<DueyActions> packages = new LinkedList<>();
		Connection con = Database.getConnection();
		final String selectPackageByReceiver = "SELECT * FROM dueypackages LEFT JOIN dueyitems USING (PackageId) WHERE RecieverId = ?";
		try (PreparedStatement ps = con.prepareStatement(selectPackageByReceiver)) {
			ps.setInt(1, chr.getId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				DueyActions dueypack = getItemByPID(rs);
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

	public static DueyActions loadSingleItem(final int packageid, final int charid) {
		List<DueyActions> packages = new LinkedList<>();
		Connection con = Database.getConnection();
		try {
			final String selectPackageByIdAndReceiver = "SELECT * FROM dueypackages LEFT JOIN dueyitems USING (PackageId) WHERE PackageId = ? and RecieverId = ?";
			PreparedStatement ps = con.prepareStatement(selectPackageByIdAndReceiver);
			ps.setInt(1, packageid);
			ps.setInt(2, charid);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				DueyActions dueypack = getItemByPID(rs);
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

	public static void reciveMsg(final ChannelClient c, final int recipientId) {
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("UPDATE dueypackages SET Checked = 0 where RecieverId = ?")) {
			ps.setInt(1, recipientId);
			ps.executeUpdate();
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private static void removeItemFromDB(final int packageid, final int charid) {
		Connection con = Database.getConnection();
		try (PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ? and RecieverId = ?")) {
			ps.setInt(1, packageid);
			ps.setInt(2, charid);
			ps.executeUpdate();
		} catch (SQLException se) {
			se.printStackTrace();
		}
	}

	private static DueyActions getItemByPID(final ResultSet rs) {
		try {
			DueyActions dueypack;
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
				dueypack = new DueyActions(rs.getInt("PackageId"), eq);

			} else if (rs.getInt("type") == 2) {
				Item newItem = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"), (byte) 0);
				newItem.setOwner(rs.getString("owner"));
				newItem.setFlag(rs.getByte("flag"));
				newItem.setExpiration(rs.getLong("expiredate"));
				newItem.setGMLog(rs.getString("GM_Log"));
				dueypack = new DueyActions(rs.getInt("PackageId"), newItem);
			} else {
				dueypack = new DueyActions(rs.getInt("PackageId"));
			}
			return dueypack;
		} catch (SQLException se) {
			se.printStackTrace();
			return null;
		}
	}
}