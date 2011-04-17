package handling.channel.handler;

import client.IItem;
import client.InventoryType;
import client.GameClient;
import client.GameCharacter;
import client.GameConstants;
import handling.ServerPacketOpcode;
import org.javastory.io.PacketFormatException;
import server.AutobanManager;
import server.Shop;
import server.InventoryManipulator;
import server.Storage;
import server.life.Npc;
import server.quest.Quest;
import scripting.NpcScriptManager;
import scripting.NpcConversationManager;
import tools.MaplePacketCreator;
import org.javastory.io.PacketReader;
import org.javastory.io.PacketBuilder;
import org.javastory.io.PacketReader;

public class NpcHandler {

    public static final void handleNpcAnimation(final PacketReader reader, final GameClient c) throws PacketFormatException {
        PacketBuilder builder = new PacketBuilder();
        final int length = (int) reader.remaining();

        if (length == 6) { // NPC Talk
            builder.writeAsShort(ServerPacketOpcode.NPC_ACTION.getValue());
            builder.writeInt(reader.readInt());
            builder.writeAsShort(reader.readShort());
            c.getSession().write(builder.getPacket());
        } else if (length > 6) { // NPC Move
            final byte[] bytes = reader.readBytes(length - 9);
            builder.writeAsShort(ServerPacketOpcode.NPC_ACTION.getValue());
            builder.writeBytes(bytes);
            c.getSession().write(builder.getPacket());
        }
    }

    public static final void handleNpcShop(final PacketReader reader, final GameClient c, final GameCharacter chr) throws PacketFormatException {
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
                shop.sell(c, GameConstants.getInventoryType(itemId), slot, quantity);
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
                chr.setConversation(0);
                break;
        }
    }

    public static final void handleNpcTalk(final PacketReader reader, final GameClient c, final GameCharacter chr) throws PacketFormatException {
        final Npc npc = (Npc) chr.getMap().getNPCByOid(reader.readInt());
        if (npc == null || chr.getConversation() != 0) {
            return;
        }
        if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(c);
        } else {
            NpcScriptManager.getInstance().start(c, npc.getId());
        }
    }

    public static final void handleQuestAction(final PacketReader reader, final GameClient c, final GameCharacter chr) throws PacketFormatException {
        final byte action = reader.readByte();
        final int quest = reader.readUnsignedShort();
        switch (action) {
            case 0: { // Restore lost item
                reader.skip(4);
                final int itemid = reader.readInt();
                Quest.getInstance(quest).RestoreLostItem(chr, itemid);
                break;
            }
            case 1: { // Start Quest
                final int npc = reader.readInt();
                reader.skip(4);
                Quest.getInstance(quest).start(chr, npc);
                break;
            }
            case 2: { // Complete Quest
                final int npc = reader.readInt();
                reader.skip(4);
                if (reader.remaining() >= 4) {
                    Quest.getInstance(quest).complete(chr, npc, reader.readInt());
                } else {
                    Quest.getInstance(quest).complete(chr, npc);
                }
                // c.getSession().writeAsByte(MaplePacketCreator.completeQuest(c.getPlayer(), quest));
                //c.getSession().writeAsByte(MaplePacketCreator.updateQuestInfo(c.getPlayer(), quest, npc, (byte)14));
                // 6 = start quest
                // 7 = unknown error
                // 8 = equip is full
                // 9 = not enough mesos
                // 11 = due to the equipment currently being worn wtf o.o
                // 12 = you may not posess more than one of this item
                break;
            }
            case 3: { // Forefit Quest
                Quest.getInstance(quest).forfeit(chr);
                break;
            }
            case 4: { // Scripted Start Quest
                final int npc = reader.readInt();
                reader.skip(4);
                NpcScriptManager.getInstance().startQuest(c, npc, quest);
                break;
            }
            case 5: { // Scripted End Quest
                final int npc = reader.readInt();
                reader.skip(4);
                NpcScriptManager.getInstance().endQuest(c, npc, quest, false);
                c.getSession().write(MaplePacketCreator.showSpecialEffect(9)); // Quest completion
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showSpecialEffect(chr.getId(), 9), false);
                break;
            }
        }
    }

    public static final void handleStorage(final PacketReader reader, final GameClient c, final GameCharacter chr) throws PacketFormatException {
        final byte mode = reader.readByte();
        final Storage storage = chr.getStorage();

        switch (mode) {
            case 4: { // Take Out
                final byte type = reader.readByte();
                final byte slot = storage.getSlot(InventoryType.getByType(type), reader.readByte());
                final IItem item = storage.takeOut(slot);

                if (item != null) {
                    if (InventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                        InventoryManipulator.addFromDrop(c, item, false);
                    } else {
                        storage.store(item);
                        chr.dropMessage(1, "Your inventory is full");
                    }
                    storage.sendTakenOut(c, GameConstants.getInventoryType(item.getItemId()));
                } else {
                    //AutobanManager.getInstance().autoban(c, "Trying to take out item from storage which does not exist.");
                    return;
                }
                break;
            }
            case 5: { // Store
                final byte slot = (byte) reader.readShort();
                final int itemId = reader.readInt();
                short quantity = reader.readShort();
                if (quantity < 1) {
                    //AutobanManager.getInstance().autoban(c, "Trying to store " + quantity + " of " + itemId);
                    return;
                }
                if (storage.isFull()) {
                    c.getSession().write(MaplePacketCreator.getStorageFull());
                    return;
                }

                if (chr.getMeso() < 100) {
                    chr.dropMessage(1, "You don't have enough mesos to store the item");
                } else {
                    InventoryType type = GameConstants.getInventoryType(itemId);
                    IItem item = chr.getInventory(type).getItem(slot).copy();

                    if (GameConstants.isPet(item.getItemId())) {
                        c.getSession().write(MaplePacketCreator.enableActions());
                        return;
                    }
                    if (item.getItemId() == itemId && (item.getQuantity() >= quantity || GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId))) {
                        if (GameConstants.isThrowingStar(itemId) || GameConstants.isBullet(itemId)) {
                            quantity = item.getQuantity();
                        }
                        chr.gainMeso(-100, false, true, false);
                        InventoryManipulator.removeFromSlot(c, type, slot, quantity, false);
                        item.setQuantity(quantity);
                        storage.store(item);
                    } else {
                        AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store non-matching itemid (" + itemId + "/" + item.getItemId() + ") or quantity not in posession (" + quantity + "/" + item.getQuantity() + ")");
                        return;
                    }
                }
                storage.sendStored(c, GameConstants.getInventoryType(itemId));
                break;
            }
            case 7: {
                int meso = reader.readInt();
                final int storageMesos = storage.getMeso();
                final int playerMesos = chr.getMeso();

                if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                    if (meso < 0 && (storageMesos - meso) < 0) { // storing with overflow
                        meso = -(Integer.MAX_VALUE - storageMesos);
                        if ((-meso) > playerMesos) { // should never happen just a failsafe
                            return;
                        }
                    } else if (meso > 0 && (playerMesos + meso) < 0) { // taking out with overflow
                        meso = (Integer.MAX_VALUE - playerMesos);
                        if ((meso) > storageMesos) { // should never happen just a failsafe
                            return;
                        }
                    }
                    storage.setMeso(storageMesos - meso);
                    chr.gainMeso(meso, false, true, false);
                } else {
                    AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store or take out unavailable amount of mesos (" + meso + "/" + storage.getMeso() + "/" + c.getPlayer().getMeso() + ")");
                    return;
                }
                storage.sendMeso(c);
                break;
            }
            case 8: {
                storage.close();
                chr.setConversation(0);
                break;
            }
            default:
                System.out.println("Unhandled Storage mode : " + mode);
                break;
        }
    }

    public static final void handleNpcTalkMore(final PacketReader reader, final GameClient c) throws PacketFormatException {
        final byte lastMsg = reader.readByte(); // 00 (last msg type I think)
        final byte action = reader.readByte(); // 00 = end chat, 01 == follow

        final NpcConversationManager cm = NpcScriptManager.getInstance().getCM(c);

        if (cm == null || c.getPlayer().getConversation() == 0) {
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
    /*    @Override
    public void handlePacket(PacketReader reader, GameClient c) {
    final NPC npc = c.getPlayer().getNpc();
    if (npc == null) {
    return;
    }
    
    final byte type = reader.readByte();
    if (type != npc.getSentDialogue()) {
    return;
    }
    final byte what = reader.readByte();
    
    switch (type) {
    case 0x00: // NPCDialogs::normal
    switch (what) {
    case 0:
    //			npc->proceedBack();
    break;
    case 1:
    //			npc->proceedNext();
    break;
    default:
    //			npc->end();
    break;
    }
    break;
    case 0x01: // NPCDialogs::yesNo
    case 0x0c: // NPCDialogs::acceptDecline
    switch (what) {
    case 0:
    //			npc->proceedSelection(0);
    break;
    case 1:
    //			npc->proceedSelection(1);
    break;
    default:
    //			npc->end();
    break;
    }
    break;
    case 0x02: // NPCDialogs::getText
    if (what != 0) {
    //		    npc->proceedText(packet.getString());
    } else {
    //		    npc->end();
    }
    break;
    case 0x03: // NPCDialogs::getNumber
    if (what == 1) {
    //		    npc->proceedNumber(packet.get<int32_t>());
    } else {
    //		    npc->end();
    }
    break;
    case 0x04: // NPCDialogs::simple
    if (what == 0) {
    //		    npc->end();
    } else {
    //		    npc->proceedSelection(packet.get<uint8_t>());
    }
    break;
    case 0x07: // NPCDialogs::style
    if (what == 1) {
    //		    npc->proceedSelection(packet.get<uint8_t>());
    } else {
    //		    npc->end();
    }
    break;
    default:
    //		npc->end();
    break;
    }
    }*/
}
