package javastory.channel.handling;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.life.Npc;
import javastory.channel.server.AutobanManager;
import javastory.channel.server.InventoryManipulator;
import javastory.channel.server.Shop;
import javastory.channel.server.Storage;
import javastory.game.GameConstants;
import javastory.game.Inventory;
import javastory.game.InventoryType;
import javastory.game.Item;
import javastory.game.quest.QuestInfoProvider;
import javastory.game.quest.QuestInfoProvider.QuestInfo;
import javastory.io.PacketBuilder;
import javastory.io.PacketFormatException;
import javastory.io.PacketReader;
import javastory.scripting.NpcConversationManager;
import javastory.scripting.NpcScriptManager;
import javastory.server.handling.ServerPacketOpcode;
import javastory.tools.packets.ChannelPackets;

public class NpcHandler {

	public static void handleNpcAnimation(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		final PacketBuilder builder = new PacketBuilder();
		final int length = (int) reader.remaining();

		if (length == 6) { // NPC Talk
			builder.writeAsShort(ServerPacketOpcode.NPC_ACTION.getValue());
			builder.writeInt(reader.readInt());
			builder.writeAsShort(reader.readShort());
			c.write(builder.getPacket());
		} else if (length > 6) { // NPC Move
			final byte[] bytes = reader.readBytes(length - 9);
			builder.writeAsShort(ServerPacketOpcode.NPC_ACTION.getValue());
			builder.writeBytes(bytes);
			c.write(builder.getPacket());
		}
	}

	public static void handleNpcShop(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final byte bmode = reader.readByte();

		switch (bmode) {
		case 0: {
			final Shop shop = chr.getShop();
			if (shop == null) {
				return;
			}
			reader.skip(2);
			final int itemId = reader.readInt();
			final short quantity = reader.readShort();
			shop.buy(c, itemId, quantity);
			break;
		}
		case 1: {
			final Shop shop = chr.getShop();
			if (shop == null) {
				return;
			}
			final byte slot = (byte) reader.readShort();
			final int itemId = reader.readInt();
			final short quantity = reader.readShort();
			shop.sell(c, chr.getInventoryForItem(itemId), slot, quantity);
			break;
		}
		case 2: {
			final Shop shop = chr.getShop();
			if (shop == null) {
				return;
			}
			final byte slot = (byte) reader.readShort();
			shop.recharge(c, slot);
			break;
		}
		default:
			chr.setConversationState(0);
			break;
		}
	}

	public static void handleNpcTalk(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final Npc npc = chr.getMap().getNPCByOid(reader.readInt());
		if (npc == null || chr.getConversationState() != 0) {
			return;
		}
		if (npc.hasShop()) {
			chr.setConversationState(1);
			npc.sendShop(c);
		} else {
			NpcScriptManager.getInstance().start(c, npc.getId());
		}
	}

	public static void handleQuestAction(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final byte action = reader.readByte();
		final int questId = reader.readUnsignedShort();
		final QuestInfo quest = QuestInfoProvider.getInfo(questId);
		switch (action) {
		case 0: {
			// Restore lost item
			reader.skip(4);
			final int itemid = reader.readInt();
			quest.restoreLostItems(chr, itemid);
			break;
		}
		case 1: {
			// Start Quest
			final int npc = reader.readInt();
			reader.skip(4);
			quest.start(chr, npc);
			break;
		}
		case 2: {
			// Complete Quest
			final int npcId = reader.readInt();
			reader.skip(4);
			if (reader.remaining() >= 4) {
				final int selection = reader.readInt();
				quest.complete(chr, npcId, selection);
			} else {
				quest.complete(chr, npcId);
			}
			// c.getSession().writeAsByte(MaplePacketCreator.completeQuest(c.getPlayer(),
			// quest));
			// c.getSession().writeAsByte(MaplePacketCreator.updateQuestInfo(c.getPlayer(),
			// quest, npc, (byte)14));
			// 6 = start quest
			// 7 = unknown error
			// 8 = equip is full
			// 9 = not enough mesos
			// 11 = due to the equipment currently being worn wtf o.o
			// 12 = you may not posess more than one of this item
			break;
		}
		case 3: {
			// Forefit Quest
			quest.forfeit(chr);
			break;
		}
		case 4: {
			// Scripted Start Quest
			final int npc = reader.readInt();
			reader.skip(4);
			NpcScriptManager.getInstance().startQuest(c, npc, questId);
			break;
		}
		case 5: {
			// Scripted End Quest
			final int npc = reader.readInt();
			reader.skip(4);
			NpcScriptManager.getInstance().endQuest(c, npc, questId, false);
			c.write(ChannelPackets.showSpecialEffect(9)); // Quest completion
			chr.getMap().broadcastMessage(chr, ChannelPackets.showSpecialEffect(chr.getId(), 9), false);
			break;
		}
		}
	}

	public static void handleStorage(final PacketReader reader, final ChannelClient c, final ChannelCharacter chr) throws PacketFormatException {
		final byte mode = reader.readByte();
		final Storage storage = chr.getStorage();

		switch (mode) {
		case 4: {
			// Take Out
			handleStorageTakeOutItem(reader, storage, c, chr);
			break;
		}
		case 5: {
			// Store
			handleStoragePutInItem(reader, storage, c, chr);
			break;
		}
		case 7: {
			handleStorageMesoTransaction(reader, storage, chr, c);
			break;
		}
		case 8: {
			storage.close();
			chr.setConversationState(0);
			break;
		}
		default:
			System.out.println("Unhandled Storage mode : " + mode);
			break;
		}
	}

	private static void handleStorageMesoTransaction(final PacketReader reader, final Storage storage, final ChannelCharacter chr, final ChannelClient c)
		throws PacketFormatException {
		int meso = reader.readInt();
		final int storageMesos = storage.getMeso();
		final int playerMesos = chr.getMeso();
		if (meso > 0 && storageMesos >= meso || meso < 0 && playerMesos >= -meso) {
			if (meso < 0 && storageMesos - meso < 0) {
				// storing with overflow
				meso = -(Integer.MAX_VALUE - storageMesos);
				if (-meso > playerMesos) {
					return;
				}
			} else if (meso > 0 && playerMesos + meso < 0) {
				// taking out with overflow
				meso = Integer.MAX_VALUE - playerMesos;
				if (meso > storageMesos) {
					return;
				}
			}
			storage.setMeso(storageMesos - meso);
			chr.gainMeso(meso, false, true, false);
		} else {
			final StringBuilder builder = new StringBuilder();
			builder.append("Trying to store or take out unavailable amount of mesos (");
			builder.append(meso).append("/");
			builder.append(storage.getMeso()).append("/");
			builder.append(c.getPlayer().getMeso()).append(")");
			AutobanManager.getInstance().addPoints(c, 1000, 0, builder.toString());
			return;
		}
		storage.sendMeso(c);
	}

	private static void handleStoragePutInItem(final PacketReader reader, final Storage storage, final ChannelClient c, final ChannelCharacter chr)
		throws PacketFormatException {
		final byte slot = (byte) reader.readShort();
		final int itemId = reader.readInt();
		short quantity = reader.readShort();
		if (quantity < 1) {
			return;
		}
		if (storage.isFull()) {
			c.write(ChannelPackets.getStorageFull());
			return;
		}
		if (chr.getMeso() < 100) {
			chr.sendNotice(1, "You don't have enough mesos to store the item");
		} else {
			final Inventory inventory = chr.getInventoryForItem(itemId);
			final Item item = inventory.getItem(slot).copy();
			if (GameConstants.isPet(item.getItemId())) {
				c.write(ChannelPackets.enableActions());
				return;
			}
			if (item.getItemId() == itemId && (item.getQuantity() >= quantity || GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId))) {
				if (GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId)) {
					quantity = item.getQuantity();
				}
				chr.gainMeso(-100, false, true, false);
				InventoryManipulator.removeFromSlot(c, inventory, slot, quantity, false);
				item.setQuantity(quantity);
				storage.store(item);
			} else {
				AutobanManager.getInstance().addPoints(
					c,
					1000,
					0,
					"Trying to store non-matching itemid (" + itemId + "/" + item.getItemId() + ") or quantity not in posession (" + quantity + "/"
						+ item.getQuantity() + ")");
				return;
			}
		}
		storage.sendStored(c, GameConstants.getInventoryType(itemId));
	}

	private static void handleStorageTakeOutItem(final PacketReader reader, final Storage storage, final ChannelClient c, final ChannelCharacter chr)
		throws PacketFormatException {
		final byte type = reader.readByte();
		final byte slot = storage.getSlot(InventoryType.fromNumber(type), reader.readByte());
		final Item item = storage.takeOut(slot);
		if (item != null) {
			if (InventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
				InventoryManipulator.addFromDrop(c, item, false);
			} else {
				storage.store(item);
				chr.sendNotice(1, "Your inventory is full");
			}
			storage.sendTakenOut(c, GameConstants.getInventoryType(item.getItemId()));
		}
	}

	public static void handleNpcTalkMore(final PacketReader reader, final ChannelClient c) throws PacketFormatException {
		final byte lastMsg = reader.readByte(); // 00 (last msg type I think)
		final byte action = reader.readByte(); // 00 = end chat, 01 == follow

		final NpcConversationManager cm = NpcScriptManager.getInstance().getConversationManager(c);

		if (cm == null || c.getPlayer().getConversationState() == 0) {
			return;
		}
		if (lastMsg == 3) {
			if (action != 0) {
				cm.setGetText(reader.readLengthPrefixedString());
				if (cm.getType() == 0) {
					NpcScriptManager.getInstance().startQuest(c, action, lastMsg, -1);
				} else if (cm.getType() == 1) {
					NpcScriptManager.getInstance().endQuest(c, action, lastMsg, -1);
				} else {
					NpcScriptManager.getInstance().action(c, action, lastMsg, -1);
				}
			} else {
				cm.dispose();
			}
		} else {
			int selection = -1;
			if (reader.remaining() >= 4) {
				selection = reader.readInt();
			} else if (reader.remaining() > 0) {
				selection = reader.readByte();
			}
			if (action != -1) {
				if (cm.getType() == 0) {
					NpcScriptManager.getInstance().startQuest(c, action, lastMsg, selection);
				} else if (cm.getType() == 1) {
					NpcScriptManager.getInstance().endQuest(c, action, lastMsg, selection);
				} else {
					NpcScriptManager.getInstance().action(c, action, lastMsg, selection);
				}
			} else {
				cm.dispose();
			}
		}
	}
}
