package javastory.channel.handling;

import java.util.Arrays;
import java.util.List;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.ChannelServer;
import javastory.channel.maps.GameMapObject;
import javastory.channel.maps.GameMapObjectType;
import javastory.channel.packet.PlayerShopPacket;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.Trade;
import javastory.channel.shops.AbstractPlayerShop;
import javastory.channel.shops.GenericPlayerStore;
import javastory.channel.shops.HiredMerchantStore;
import javastory.channel.shops.PlayerShop;
import javastory.channel.shops.PlayerShopItem;
import javastory.game.GameConstants;
import javastory.game.IItem;
import javastory.game.Inventory;
import javastory.game.ItemFlag;
import javastory.game.data.ItemInfoProvider;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.tools.packets.ChannelPackets;

public class PlayerInteractionHandler {

	public static void handlePlayerInteraction(final PacketReader reader, final ChannelClient client, final ChannelCharacter player)
		throws PacketFormatException {
		final byte action = reader.readByte();
		PlayerInteractionType type = PlayerInteractionType.fromNumber(action);
		switch (type) { // Mode
		case CREATE:
			create(reader, client, player);
			break;
		case TRADE_INVITE:
			inviteTrade(reader, player);
			break;
		case TRADE_DECLINE:
			tradeDecline(player);
			break;
		case VISIT:
			visit(reader, client, player);
			break;
		case CHAT:
			chat(reader, player);
			break;
		case EXIT:
			exit(client, player);
			break;
		case OPEN:
			open(reader, client, player);
			break;
		case TRADE_SET_ITEMS:
			tradeSetItems(reader, client, player);
			break;
		case TRADE_SET_MESO:
			tradeSetMeso(reader, player);
			break;
		case TRADE_CONFIRM:
			tradeConfirm(player);
			break;
		case MERCHANT_EXIT:
			exitMerchant(player);
			break;
		case ADD_ITEM:
			addItem(reader, client, player);
			break;
		case BUY_ITEM_STORE:
		case BUY_ITEM_HIREDMERCHANT:
			// Buy and Merchant buy
			buyItem(reader, client, player);
			break;
		case REMOVE_ITEM:
			removeItem(reader, client, player);
			break;
		case MAINTENANCE_OFF:
			shopMaintenanceOff(player);
			break;
		case MAINTENANCE_ORGANISE:
			shopMaintenance(client, player);
			break;
		case CLOSE_MERCHANT:
			closeMerchant(client, player);
			break;
		case ADMIN_STORE_NAMECHANGE:
			// Changing store name, only Admin
			// 01 00 00 00
		case VIEW_MERCHANT_VISITOR:
		case VIEW_MERCHANT_BLACKLIST:
			break;
		default:
			System.out.println("Unhandled interaction action : " + action + ", " + reader.toString());
			break;
		}
	}

	private static void create(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final byte createType = reader.readByte();
		if (createType == 1) { // omok
			// nvm
		} else if (createType == 3) { // trade
			Trade.startTrade(chr);
		} else if (createType == 4 || createType == 5) { // shop
			final List<GameMapObjectType> filter = Arrays.asList(GameMapObjectType.SHOP, GameMapObjectType.HIRED_MERCHANT);
			final List<GameMapObject> objects = chr.getMap().getMapObjectsInRange(chr.getPosition(), 19500, filter);
			if (!objects.isEmpty()) {
				chr.sendNotice(1, "You may not establish a store here.");
			} else {
				final String desc = reader.readLengthPrefixedString();
				reader.skip(3);
				final int itemId = reader.readInt();
				if (createType == 4) {
					chr.setPlayerShop(new GenericPlayerStore(chr, itemId, desc));
					c.write(PlayerShopPacket.getPlayerStore(chr, true));
				} else {
					final HiredMerchantStore merch = new HiredMerchantStore(chr, itemId, desc);
					chr.setPlayerShop(merch);
					c.write(PlayerShopPacket.getHiredMerch(chr, merch, true));
				}
			}
		}
	}

	private static void inviteTrade(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		ChannelCharacter ochr = chr.getMap().getCharacterById_InMap(reader.readInt());
		if (ochr.getWorldId() != chr.getWorldId()) {
			chr.getClient().write(ChannelPackets.serverNotice(5, "Cannot find player"));
			return;
		}
		Trade.inviteTrade(chr, ochr);
	}

	private static void tradeDecline(final ChannelCharacter chr) {
		Trade.declineTrade(chr);
	}

	private static void visit(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		if (chr.getTrade() != null && chr.getTrade().getPartner() != null) {
			Trade.visitTrade(chr, chr.getTrade().getPartner().getChr());
		} else {
			final GameMapObject ob = chr.getMap().getMapObject(reader.readInt());

			if (ob instanceof PlayerShop && chr.getPlayerShop() == null) {
				final PlayerShop ips = (PlayerShop) ob;

				if (ob instanceof HiredMerchantStore) {
					final HiredMerchantStore merchant = (HiredMerchantStore) ips;
					if (merchant.isOwner(chr)) {
						merchant.setOpen(false);
						merchant.broadcastToVisitors(PlayerShopPacket.shopErrorMessage(0x0D, 1));
						merchant.removeAllVisitors((byte) 16, (byte) 0);
						chr.setPlayerShop(ips);
						c.write(PlayerShopPacket.getHiredMerch(chr, merchant, false));
					} else {
						if (!merchant.isOpen()) {
							chr.sendNotice(1, "This shop is in maintenance, please come by later.");
						} else {
							if (ips.getFreeSlot() == -1) {
								chr.sendNotice(1, "This shop has reached it's maximum capacity, please come by later.");
							} else {
								chr.setPlayerShop(ips);
								merchant.addVisitor(chr);
								c.write(PlayerShopPacket.getHiredMerch(chr, merchant, false));
							}
						}
					}
				} else if (ips.getShopType() == 2) {
					if (((GenericPlayerStore) ips).isBanned(chr.getName())) {
						chr.sendNotice(1, "You have been banned from this store.");
						return;
					}
				} else {
					if (ips.getFreeSlot() == -1) {
						chr.sendNotice(1, "This shop has reached it's maximum capacity, please come by later.");
					} else {
						chr.setPlayerShop(ips);
						ips.addVisitor(chr);
						c.write(PlayerShopPacket.getPlayerStore(chr, false));
					}
				}
			}
		}
	}

	private static void chat(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		reader.readInt();
		if (chr.getTrade() != null) {
			chr.getTrade().chat(reader.readLengthPrefixedString());
		} else if (chr.getPlayerShop() != null) {
			final PlayerShop ips = chr.getPlayerShop();
			final String message = reader.readLengthPrefixedString();
			ips.broadcastToVisitors(PlayerShopPacket.shopChat(chr.getName() + " : " + message, ips.isOwner(chr) ? 0 : ips.getVisitorSlot(chr)));
		}
	}

	private static void exit(final ChannelClient c, final ChannelCharacter chr) {
		if (chr.getTrade() != null) {
			Trade.cancelTrade(chr.getTrade());
		} else {
			final PlayerShop ips = chr.getPlayerShop();
			if (ips == null) {
				return;
			}
			if (ips.isOwner(chr)) {
				if (ips.getShopType() == 2) {
					boolean save = false;
					for (PlayerShopItem items : ips.getItems()) {
						if (items.bundles > 0) {
							if (InventoryManipulator.addFromDrop(c, items.item, false)) {
								items.bundles = 0;
							} else {
								save = true;
								break;
							}
						}
					}
					ips.removeAllVisitors(3, 1);
					ips.closeShop(save, true);
				}
			} else {
				ips.removeVisitor(chr);
			}
			chr.setPlayerShop(null);
		}
	}

	private static void open(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		// c.getPlayer().haveItem(mode, 1, false, true)
		if (chr.getMap().allowPersonalShop()) {
			final PlayerShop shop = chr.getPlayerShop();
			if (shop != null && shop.isOwner(chr)) {
				chr.getMap().addMapObject((AbstractPlayerShop) shop);

				if (shop.getShopType() == 1) {
					final HiredMerchantStore merchant = (HiredMerchantStore) shop;
					merchant.setStoreid(ChannelServer.getInstance().addMerchant(merchant));
					merchant.setOpen(true);
					chr.getMap().broadcastMessage(PlayerShopPacket.spawnHiredMerchant(merchant));
					chr.setPlayerShop(null);

				} else if (shop.getShopType() == 2) {
					chr.getMap().broadcastMessage(PlayerShopPacket.sendPlayerShopBox(chr));
				}
				reader.readByte();
			}
		} else {
			c.disconnect(true);
		}
	}

	private static void tradeSetItems(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final ItemInfoProvider ii = ItemInfoProvider.getInstance();
		final byte typeByte = reader.readByte();
		final Inventory inventory = chr.getInventoryByTypeByte(typeByte);
		final IItem item = inventory.getItem((byte) reader.readShort());
		final short quantity = reader.readShort();
		final byte targetSlot = reader.readByte();

		if (chr.getTrade() != null && item != null) {
			if ((quantity <= item.getQuantity() && quantity >= 0) || GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
				final byte flag = item.getFlag();

				if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
					c.write(ChannelPackets.enableActions());
					return;
				}
				if (ii.isDropRestricted(item.getItemId())) {
					if (!(ItemFlag.KARMA_EQ.check(flag) || ItemFlag.KARMA_USE.check(flag))) {
						c.write(ChannelPackets.enableActions());
						return;
					}
				}
				IItem tradeItem = item.copy();
				if (GameConstants.isThrowingStar(item.getItemId()) || GameConstants.isBullet(item.getItemId())) {
					tradeItem.setQuantity(item.getQuantity());
					InventoryManipulator.removeFromSlot(c, inventory, item.getPosition(), item.getQuantity(), true);
				} else {
					tradeItem.setQuantity(quantity);
					InventoryManipulator.removeFromSlot(c, inventory, item.getPosition(), quantity, true);
				}
				tradeItem.setPosition(targetSlot);
				chr.getTrade().addItem(tradeItem);
			}
		}
	}

	private static void tradeSetMeso(final PacketReader reader, final ChannelCharacter chr) throws PacketFormatException {
		final Trade trade = chr.getTrade();
		if (trade != null) {
			trade.setMeso(reader.readInt());
		}
	}

	private static void tradeConfirm(final ChannelCharacter chr) {
		if (chr.getTrade() != null) {
			Trade.completeTrade(chr);
		}
	}

	private static void exitMerchant(ChannelCharacter chr) {
//		final PlayerShop shop = chr.getPlayerShop();
//		if (shop != null && shop instanceof HiredMerchantStore && shop.isOwner(chr)) {
//			shop.setOpen(true);
//			chr.setPlayerShop(null);
//		}
	}

	private static void addItem(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final byte typeByte = reader.readByte();
		final Inventory inventory = chr.getInventoryByTypeByte(typeByte);
		final byte slot = (byte) reader.readShort();
		final short bundles = reader.readShort(); // How many in a bundle
		final short perBundle = reader.readShort(); // Price per bundle
		final int price = reader.readInt();

		if (price <= 0 || bundles <= 0 || perBundle <= 0) {
			return;
		}
		final PlayerShop shop = chr.getPlayerShop();

		if (shop == null || !shop.isOwner(chr)) {
			return;
		}
		final IItem ivItem = inventory.getItem(slot);

		if (ivItem != null) {
			final short bundles_perbundle = (short) (bundles * perBundle);
			if (bundles_perbundle < 0) { // int_16 overflow
				return;
			}
			if (ivItem.getQuantity() >= bundles_perbundle) {
				final byte flag = ivItem.getFlag();

				if (ItemFlag.UNTRADEABLE.check(flag) || ItemFlag.LOCK.check(flag)) {
					c.write(ChannelPackets.enableActions());
					return;
				}
				if (ItemInfoProvider.getInstance().isDropRestricted(ivItem.getItemId())) {
					if (!(ItemFlag.KARMA_EQ.check(flag) || ItemFlag.KARMA_USE.check(flag))) {
						c.write(ChannelPackets.enableActions());
						return;
					}
				}
				if (GameConstants.isThrowingStar(ivItem.getItemId()) || GameConstants.isBullet(ivItem.getItemId())) {
					// Ignore the bundles
					InventoryManipulator.removeFromSlot(c, inventory, slot, ivItem.getQuantity(), true);

					final IItem sellItem = ivItem.copy();
					shop.addItem(new PlayerShopItem(sellItem, (short) 1, price));
				} else {
					InventoryManipulator.removeFromSlot(c, inventory, slot, bundles_perbundle, true);

					final IItem sellItem = ivItem.copy();
					sellItem.setQuantity(perBundle);
					shop.addItem(new PlayerShopItem(sellItem, bundles, price));
				}
				c.write(PlayerShopPacket.shopItemUpdate(shop));
			}
		}
	}

	private static void buyItem(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final int item = reader.readByte();
		final short quantity = reader.readShort();
		final PlayerShop shop = chr.getPlayerShop();

		if (shop == null || shop.isOwner(chr)) {
			return;
		}
		final PlayerShopItem tobuy = shop.getItems().get(item);

		if (quantity < 0 || tobuy == null || tobuy.bundles < quantity
			|| (tobuy.bundles % quantity != 0 && GameConstants.isEquip(tobuy.item.getItemId())) // Buying
			|| ((short) (tobuy.bundles * quantity)) < 0 || (quantity * tobuy.price) < 0 || quantity * tobuy.item.getQuantity() < 0
			|| chr.getMeso() - (quantity * tobuy.price) < 0 || shop.getMeso() + (quantity * tobuy.price) < 0) {
			return;
		}
		shop.buy(c, item, quantity);
		shop.broadcastToVisitors(PlayerShopPacket.shopItemUpdate(shop));
	}

	private static void removeItem(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final int slot = reader.readShort();
		final PlayerShop shop = chr.getPlayerShop();

		if (shop == null || !shop.isOwner(chr)) {
			return;
		}
		final PlayerShopItem item = shop.getItems().get(slot);

		if (item != null) {
			if (item.bundles > 0) {
				IItem item_get = item.item.copy();
				item_get.setQuantity((short) (item.bundles * item.item.getQuantity()));
				if (InventoryManipulator.addFromDrop(c, item_get, false)) {
					item.bundles = 0;
					shop.removeFromSlot(slot);
				}
			}
		}
		c.write(PlayerShopPacket.shopItemUpdate(shop));
	}

	private static void shopMaintenanceOff(final ChannelCharacter chr) {
		final PlayerShop shop = chr.getPlayerShop();
		// TS TODO: If the player is NOT the owner, should track a cheating
		// offense.
		if (shop != null && shop instanceof HiredMerchantStore && shop.isOwner(chr)) {
			shop.setOpen(true);
			chr.setPlayerShop(null);
		}
	}

	private static void shopMaintenance(final ChannelClient c, final ChannelCharacter chr) {
		final PlayerShop imps = chr.getPlayerShop();
		// TS TODO: If the player is NOT the owner, should track a cheating
		// offense.
		if (imps.isOwner(chr)) {
			for (int i = 0; i < imps.getItems().size(); i++) {
				if (imps.getItems().get(i).bundles == 0) {
					imps.getItems().remove(i);
				}
			}
			if (chr.getMeso() + imps.getMeso() < 0) {
				c.write(PlayerShopPacket.shopItemUpdate(imps));
			} else {
				chr.gainMeso(imps.getMeso(), false);
				imps.setMeso(0);
				c.write(PlayerShopPacket.shopItemUpdate(imps));
			}
		}
	}

	private static void closeMerchant(final ChannelClient c, final ChannelCharacter chr) {
		final PlayerShop merchant = chr.getPlayerShop();
		// TS TODO: If the player is NOT the owner, should track a cheating
		// offense.
		if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
			boolean save = false;

			if (chr.getMeso() + merchant.getMeso() < 0) {
				save = true;
			} else {
				if (merchant.getMeso() > 0) {
					chr.gainMeso(merchant.getMeso(), false);
				}
				merchant.setMeso(0);

				if (merchant.getItems().size() > 0) {
					for (PlayerShopItem items : merchant.getItems()) {
						if (items.bundles > 0) {
							IItem item_get = items.item.copy();
							item_get.setQuantity((short) (items.bundles * items.item.getQuantity()));
							if (InventoryManipulator.addFromDrop(c, item_get, false)) {
								items.bundles = 0;
							} else {
								save = true;
								break;
							}
						}
					}
				}
			}
			c.write(PlayerShopPacket.shopErrorMessage(0x10, 0));
			merchant.closeShop(save, true);
			chr.setPlayerShop(null);
		}
	}
}