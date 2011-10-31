/*
 * This file is part of the OdinMS Maple Story Server Copyright (C) 2008 ~ 2010
 * Patrick Huy <patrick.huy@frz.cc> Matthias Butz <matze@odinms.de> Jan
 * Christian Meyer <vimes@odinms.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute this
 * program under any other version of the GNU Affero General Public License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package javastory.channel.packet;

import javastory.channel.ChannelCharacter;
import javastory.channel.shops.GenericPlayerStore;
import javastory.channel.shops.HiredMerchantStore;
import javastory.channel.shops.PlayerShop;
import javastory.channel.shops.PlayerShopItem;
import javastory.game.IItem;
import javastory.io.GamePacket;
import javastory.io.PacketBuilder;
import javastory.server.MerchItemPackage;
import javastory.server.handling.ServerPacketOpcode;
import javastory.tools.packets.GameCharacterPacket;

public final class PlayerShopPacket {

	private PlayerShopPacket() {
	}

	private static void addAnnounceBox(final PacketBuilder builder, final PlayerShop shop) {
		builder.writeAsByte(4);
		builder.writeInt(((GenericPlayerStore) shop).getObjectId());
		builder.writeLengthPrefixedString(shop.getDescription());
		builder.writeAsByte(0);
		builder.writeAsByte(shop.getItemId() % 10);
		builder.writeAsByte(1);
		builder.writeAsByte(shop.getFreeSlot() > -1 ? 4 : 1);
		builder.writeAsByte(0);
	}

	public static GamePacket addCharBox(final ChannelCharacter c, final int type) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_CHAR_BOX.getValue());
		builder.writeInt(c.getId());
		addAnnounceBox(builder, c.getPlayerShop());

		return builder.getPacket();
	}

	public static GamePacket removeCharBox(final ChannelCharacter c) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_CHAR_BOX.getValue());
		builder.writeInt(c.getId());
		builder.writeAsByte(0);

		return builder.getPacket();
	}

	public static GamePacket sendTitleBox() {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SEND_TITLE_BOX.getValue());
		builder.writeAsByte(7);

		return builder.getPacket();
	}

	public static GamePacket sendPlayerShopBox(final ChannelCharacter c) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_CHAR_BOX.getValue());
		builder.writeInt(c.getId());
		addAnnounceBox(builder, c.getPlayerShop());

		return builder.getPacket();
	}

	public static GamePacket getHiredMerch(final ChannelCharacter chr, final HiredMerchantStore merch, final boolean firstTime) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());

		builder.writeAsByte(5);
		builder.writeAsByte(5);
		builder.writeAsByte(4);
		builder.writeAsShort(merch.getVisitorSlot(chr));
		builder.writeInt(merch.getItemId());
		builder.writeLengthPrefixedString("Hired Merchant");

		int i = 1;
		for (final ChannelCharacter character : merch.getVisitors()) {
			builder.writeAsByte(i++);
			GameCharacterPacket.addCharLook(builder, character, false);
			builder.writeLengthPrefixedString(character.getName());
			builder.writeAsShort(character.getJobId());
		}
		builder.writeAsByte(-1);
		builder.writeAsShort(0);
		builder.writeLengthPrefixedString(merch.getOwnerName());
		if (merch.isOwner(chr)) {
			builder.writeInt(merch.getTimeLeft());
			builder.writeAsByte(firstTime ? 1 : 0);
			builder.writeInt(0);
			builder.writeAsByte(0);
		}
		builder.writeLengthPrefixedString(merch.getDescription());
		builder.writeAsByte(10);
		builder.writeInt(merch.getMeso()); // meso
		builder.writeAsByte(merch.getItems().size());

		for (final PlayerShopItem item : merch.getItems()) {
			builder.writeAsShort(item.bundles);
			builder.writeAsShort(item.item.getQuantity());
			builder.writeInt(item.price);
			PacketHelper.addItemInfo(builder, item.item, true, true);
		}
		return builder.getPacket();
	}

	public static GamePacket getPlayerStore(final ChannelCharacter chr, final boolean firstTime) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		final PlayerShop ips = chr.getPlayerShop();

		switch (ips.getShopType()) {
		case 2:
			builder.writeAsByte(5);
			builder.writeAsByte(4);
			builder.writeAsByte(4);
			break;
		case 3:
			builder.writeAsByte(5);
			builder.writeAsByte(2);
			builder.writeAsByte(2);
			break;
		case 4:
			builder.writeAsByte(5);
			builder.writeAsByte(1);
			builder.writeAsByte(2);
			break;
		}
		builder.writeAsShort(ips.getVisitorSlot(chr));

		GameCharacterPacket.addCharLook(builder, ((GenericPlayerStore) ips).getMCOwner(), false);
		builder.writeLengthPrefixedString(ips.getOwnerName());

		int i = 1;
		for (final ChannelCharacter character : ips.getVisitors()) {
			builder.writeAsByte(i++);
			GameCharacterPacket.addCharLook(builder, character, false);
			builder.writeLengthPrefixedString(character.getName());
		}
		builder.writeAsByte(0xFF);
		builder.writeLengthPrefixedString(ips.getDescription());
		builder.writeAsByte(10);
		builder.writeAsByte(ips.getItems().size());

		for (final PlayerShopItem item : ips.getItems()) {
			builder.writeAsShort(item.bundles);
			builder.writeAsShort(item.item.getQuantity());
			builder.writeInt(item.price);
			PacketHelper.addItemInfo(builder, item.item, true, true);
		}
		return builder.getPacket();
	}

	public static GamePacket shopChat(final String message, final int slot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(6);
		builder.writeAsByte(8);
		builder.writeAsByte(slot);
		builder.writeLengthPrefixedString(message);

		return builder.getPacket();
	}

	public static GamePacket shopErrorMessage(final int error, final int type) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0x0A);
		builder.writeAsByte(type);
		builder.writeAsByte(error);

		return builder.getPacket();
	}

	public static GamePacket spawnHiredMerchant(final HiredMerchantStore hm) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.SPAWN_HIRED_MERCHANT.getValue());
		builder.writeInt(hm.getOwnerId());
		builder.writeInt(hm.getItemId());
		builder.writeVector(hm.getPosition());
		builder.writeAsShort(0);
		builder.writeLengthPrefixedString(hm.getOwnerName());
		builder.writeAsByte(5);
		builder.writeInt(hm.getObjectId());
		builder.writeLengthPrefixedString(hm.getDescription());
		builder.writeAsByte(hm.getItemId() % 10);
		builder.writeAsByte(1);
		builder.writeAsByte(4);

		return builder.getPacket();
	}

	public static GamePacket destroyHiredMerchant(final int id) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
		builder.writeInt(id);

		return builder.getPacket();
	}

	public static GamePacket shopItemUpdate(final PlayerShop shop) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0x17);
		if (shop.getShopType() == 1) {
			builder.writeInt(0);
		}
		builder.writeAsByte(shop.getItems().size());

		for (final PlayerShopItem item : shop.getItems()) {
			builder.writeAsShort(item.bundles);
			builder.writeAsShort(item.item.getQuantity());
			builder.writeInt(item.price);
			PacketHelper.addItemInfo(builder, item.item, true, true);
		}
		return builder.getPacket();
	}

	public static GamePacket shopVisitorAdd(final ChannelCharacter chr, final int slot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(4);
		builder.writeAsByte(slot);
		GameCharacterPacket.addCharLook(builder, chr, false);
		builder.writeLengthPrefixedString(chr.getName());
		builder.writeAsShort(chr.getJobId());

		return builder.getPacket();
	}

	public static GamePacket shopVisitorLeave(final byte slot) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0x0A);
		builder.writeByte(slot);

		return builder.getPacket();
	}

	public static GamePacket Merchant_Buy_Error(final byte message) {
		final PacketBuilder builder = new PacketBuilder();

		// 2 = You have not enough meso
		builder.writeAsShort(ServerPacketOpcode.PLAYER_INTERACTION.getValue());
		builder.writeAsByte(0x16);
		builder.writeByte(message);

		return builder.getPacket();
	}

	public static GamePacket updateHiredMerchant(final HiredMerchantStore shop) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.UPDATE_HIRED_MERCHANT.getValue());
		builder.writeInt(shop.getOwnerId());
		builder.writeAsByte(0x05);
		builder.writeInt(shop.getObjectId());
		builder.writeLengthPrefixedString(shop.getDescription());
		builder.writeAsByte(shop.getItemId() % 10);
		builder.writeAsByte(shop.getFreeSlot() > -1 ? 3 : 2);
		builder.writeAsByte(0x04);

		return builder.getPacket();
	}

	public static GamePacket merchItem_Message(final byte op) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MERCH_ITEM_MSG.getValue());
		builder.writeByte(op);

		return builder.getPacket();
	}

	public static GamePacket merchItemStore(final byte op) {
		final PacketBuilder builder = new PacketBuilder();
		// [28 01] [22 01] - Invalid Asiasoft Passport
		// [28 01] [22 00] - Open Asiasoft pin typing
		builder.writeAsShort(ServerPacketOpcode.MERCH_ITEM_STORE.getValue());
		builder.writeByte(op);

		switch (op) {
		case 0x24:
			builder.writeZeroBytes(8);
			break;
		default:
			builder.writeAsByte(0);
			break;
		}

		return builder.getPacket();
	}

	public static GamePacket merchItemStore_ItemData(final MerchItemPackage pack) {
		final PacketBuilder builder = new PacketBuilder();

		builder.writeAsShort(ServerPacketOpcode.MERCH_ITEM_STORE.getValue());
		builder.writeAsByte(0x23);
		builder.writeInt(9030000); // Fredrick
		builder.writeInt(32272); // pack.getPackageid()
		builder.writeZeroBytes(5);
		builder.writeInt(pack.getMesos());
		builder.writeAsByte(0);
		builder.writeAsByte(pack.getItems().size());

		for (final IItem item : pack.getItems()) {
			PacketHelper.addItemInfo(builder, item, true, true);
		}
		builder.writeZeroBytes(3);

		return builder.getPacket();
	}
}
