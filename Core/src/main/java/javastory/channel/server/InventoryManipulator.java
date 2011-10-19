package javastory.channel.server;

import java.awt.Point;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javastory.channel.ChannelCharacter;
import javastory.channel.ChannelClient;
import javastory.channel.client.ActivePlayerStats;
import javastory.channel.client.BuffStat;
import javastory.channel.client.Pet;
import javastory.client.Equip;
import javastory.client.IItem;
import javastory.client.Inventory;
import javastory.client.Item;
import javastory.game.GameConstants;
import javastory.game.InventoryType;
import javastory.game.ItemFlag;
import javastory.server.ItemInfoProvider;
import javastory.tools.packets.ChannelPackets;

public final class InventoryManipulator {

    public static boolean addRing(final ChannelCharacter chr, final int itemId, final int ringId) {
        ItemInfoProvider infoProvider = ItemInfoProvider.getInstance();
        Inventory inventory = chr.getInventoryForItem(itemId);
        IItem nEquip = infoProvider.getEquipById(itemId, ringId);

        short newSlot = inventory.addItem(nEquip);
        if (newSlot == -1) {
            return false;
        }
        chr.getClient().write(ChannelPackets.addInventorySlot(inventory.getType(), nEquip));
        return true;
    }

    public static boolean addbyItem(final ChannelClient c, final IItem item) {
        Inventory inventory = c.getPlayer().getInventoryForItem(item.getItemId());
        final short newSlot = inventory.addItem(item);
        if (newSlot == -1) {
            c.write(ChannelPackets.getInventoryFull());
            c.write(ChannelPackets.getShowInventoryFull());
            return false;
        }
        c.write(ChannelPackets.addInventorySlot(inventory.getType(), item));
        return true;
    }

    public static boolean addById(ChannelClient c, int itemId, short quantity) {
        return addById(c, itemId, quantity, null, null, 0);
    }

    public static boolean addById(ChannelClient c, int itemId, short quantity, String owner) {
        return addById(c, itemId, quantity, owner, null, 0);
    }

    public static boolean addById(ChannelClient c, int itemId, short quantity, String owner, Pet pet) {
        return addById(c, itemId, quantity, owner, pet, 0);
    }

    public static boolean addById(ChannelClient c, int itemId, short quantity, String owner, Pet pet, long period) {
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();

        final Inventory inventory = c.getPlayer().getInventoryForItem(itemId);

        if (!inventory.getType().equals(InventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemId);
            final List<IItem> existing = inventory.listById(itemId);
            if (!GameConstants.isThrowingStar(itemId)
                    && !GameConstants.isBullet(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<IItem> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = (Item) i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax
                                    && (eItem.getOwner().equals(owner) || owner
                                    == null) && eItem.getExpiration() == -1) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.write(ChannelPackets.updateInventorySlot(inventory.getType(), eItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                short inventorypos;
                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0);

                        inventorypos = inventory.addItem(nItem);
                        if (inventorypos == -1) {
                            c.write(ChannelPackets.getInventoryFull());
                            c.write(ChannelPackets.getShowInventoryFull());
                            return false;
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            nItem.setExpiration(System.currentTimeMillis()
                                    + (period * 24 * 60 * 60 * 1000));
                        }
                        if (pet != null) {
                            nItem.setPet(pet);
                            pet.setInventoryPosition(inventorypos);
                        }
                        c.write(ChannelPackets.addInventorySlot(inventory.getType(), nItem));
                        if ((GameConstants.isThrowingStar(itemId)
                                || GameConstants.isBullet(itemId)) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.write(ChannelPackets.enableActions());
                        return false;
                    }
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0);
                final short newSlot = inventory.addItem(nItem);

                if (newSlot == -1) {
                    c.write(ChannelPackets.getInventoryFull());
                    c.write(ChannelPackets.getShowInventoryFull());
                    return false;
                }
                if (period > 0) {
                    nItem.setExpiration(System.currentTimeMillis() + (period
                            * 24 * 60 * 60 * 1000));
                }

                c.write(ChannelPackets.addInventorySlot(inventory.getType(), nItem));
                c.write(ChannelPackets.enableActions());
            }
        } else {
            if (quantity == 1) {
                final IItem nEquip = ii.getEquipById(itemId);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                if (period > 0) {
                    nEquip.setExpiration(System.currentTimeMillis() + (period
                            * 24 * 60 * 60 * 1000));
                }
                short newSlot = inventory.addItem(nEquip);
                if (newSlot == -1) {
                    c.write(ChannelPackets.getInventoryFull());
                    c.write(ChannelPackets.getShowInventoryFull());
                    return false;
                }
                c.write(ChannelPackets.addInventorySlot(inventory.getType(), nEquip));
            } else {
                // TODO: Cheat tracking.
                // throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return true;
    }

    public static IItem addbyId_Gachapon(final ChannelClient c, final int itemId, short quantity) {
        ChannelCharacter chr = c.getPlayer();
        if (chr.getEquipInventory().getNextFreeSlot() == -1
                || chr.getUseInventory().getNextFreeSlot() == -1
                || chr.getEtcInventory().getNextFreeSlot() == -1
                || chr.getSetupInventory().getNextFreeSlot() == -1) {
            return null;
        }
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();
        final Inventory inventory = chr.getInventoryForItem(itemId);

        if (!inventory.getType().equals(InventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(itemId);
            final List<IItem> existing = inventory.listById(itemId);

            if (!GameConstants.isThrowingStar(itemId)
                    && !GameConstants.isBullet(itemId)) {
                IItem nItem = null;
                boolean recieved = false;

                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<IItem> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            nItem = i.next();
                            short oldQ = nItem.getQuantity();

                            if (oldQ < slotMax) {
                                recieved = true;

                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                nItem.setQuantity(newQ);
                                c.write(ChannelPackets.updateInventorySlot(inventory.getType(), nItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0);
                        final short newSlot = inventory.addItem(nItem);
                        if (newSlot == -1 && recieved) {
                            return nItem;
                        } else if (newSlot == -1) {
                            return null;
                        }
                        recieved = true;
                        c.write(ChannelPackets.addInventorySlot(inventory.getType(), nItem));
                        if ((GameConstants.isThrowingStar(itemId)
                                || GameConstants.isBullet(itemId)) && quantity == 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recieved) {
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0);
                final short newSlot = inventory.addItem(nItem);

                if (newSlot == -1) {
                    return null;
                }
                c.write(ChannelPackets.addInventorySlot(inventory.getType(), nItem));
                return nItem;
            }
        } else {
            if (quantity == 1) {
                final IItem item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                final short newSlot = inventory.addItem(item);

                if (newSlot == -1) {
                    return null;
                }
                c.write(ChannelPackets.addInventorySlot(inventory.getType(), item, true));
                return item;
            } else {
                // TODO: Cheat tracking
                //throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return null;
    }

    public static boolean addFromDrop(final ChannelClient c, final IItem item, final boolean show) {
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();
        final ChannelCharacter player = c.getPlayer();

        if (ii.isPickupRestricted(item.getItemId())
                && player.haveItem(item.getItemId(), 1, true, false)) {
            c.write(ChannelPackets.getInventoryFull());
            c.write(ChannelPackets.showItemUnavailable());
            return false;
        }

        short quantity = item.getQuantity();
        final Inventory inventory = player.getInventoryForItem(item.getItemId());

        if (!inventory.getType().equals(InventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(item.getItemId());
            final List<IItem> existing = inventory.listById(item.getItemId());
            if (!GameConstants.isThrowingStar(item.getItemId())
                    && !GameConstants.isBullet(item.getItemId())) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<IItem> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = (Item) i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax
                                    && item.getOwner().equals(eItem.getOwner())
                                    && item.getExpiration()
                                    == eItem.getExpiration()) {
                                final short newQ = (short) Math.min(oldQ
                                        + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.write(ChannelPackets.updateInventorySlot(inventory.getType(), eItem, true));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getItemId(), (byte) 0, newQ, (byte) 0);
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());

                    final short newSlot = inventory.addItem(nItem);
                    if (newSlot == -1) {
                        c.write(ChannelPackets.getInventoryFull());
                        c.write(ChannelPackets.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.write(ChannelPackets.addInventorySlot(inventory.getType(), nItem, true));
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(item.getItemId(), (byte) 0, quantity, (byte) 0);
                nItem.setExpiration(item.getExpiration());
                nItem.setOwner(item.getOwner());

                final short newSlot = inventory.addItem(nItem);
                if (newSlot == -1) {
                    c.write(ChannelPackets.getInventoryFull());
                    c.write(ChannelPackets.getShowInventoryFull());
                    return false;
                }
                c.write(ChannelPackets.addInventorySlot(inventory.getType(), nItem));
                c.write(ChannelPackets.enableActions());
            }
        } else {
            if (quantity == 1) {
                final short newSlot = inventory.addItem(item);

                if (newSlot == -1) {
                    c.write(ChannelPackets.getInventoryFull());
                    c.write(ChannelPackets.getShowInventoryFull());
                    return false;
                }
                c.write(ChannelPackets.addInventorySlot(inventory.getType(), item, true));
            } else {
                throw new RuntimeException("Trying to create equip with non-one quantity");
            }
        }
        if (show) {
            c.write(ChannelPackets.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    public static boolean checkSpace(final ChannelClient c, final int itemId, int quantity, final String owner) {
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();
        final Inventory inventory = c.getPlayer().getInventoryForItem(itemId);

        if (!inventory.getType().equals(InventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemId);
            final List<IItem> existing = inventory.listById(itemId);
            if (!GameConstants.isThrowingStar(itemId)
                    && !GameConstants.isBullet(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    for (IItem eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            // add new slots if there is still something left
            final int numSlotsNeeded;
            if (slotMax > 0) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !inventory.isFull(numSlotsNeeded - 1);
        } else {
            return !inventory.isFull();
        }
    }

    public static void removeFromSlot(final ChannelClient c, final Inventory type, final short slot, final short quantity, final boolean fromDrop) {
        removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static void removeFromSlot(final ChannelClient c, final Inventory inventory, final short slot, short quantity, final boolean fromDrop, final boolean consume) {
        final IItem item = inventory.getItem(slot);
        final boolean allowZero = consume
                && (GameConstants.isThrowingStar(item.getItemId())
                || GameConstants.isBullet(item.getItemId()));
        inventory.removeItem(slot, quantity, allowZero);

        if (item.getQuantity() == 0 && !allowZero) {
            c.write(ChannelPackets.clearInventoryItem(inventory.getType(), item.getPosition(), fromDrop));
        } else {
            c.write(ChannelPackets.updateInventorySlot(inventory.getType(), item, fromDrop));
        }
    }

    public static void removeById(final ChannelClient c, final Inventory inventory, final int itemId, final int quantity, final boolean fromDrop, final boolean consume) {
        int remremove = quantity;
        for (IItem item : inventory.listById(itemId)) {
            if (remremove <= item.getQuantity()) {
                removeFromSlot(c, inventory, item.getPosition(), (short) remremove, fromDrop, consume);
                remremove = 0;
                break;
            } else {
                remremove -= item.getQuantity();
                removeFromSlot(c, inventory, item.getPosition(), item.getQuantity(), fromDrop, consume);
            }
        }
        if (remremove > 0) {
            // TODO: Cheat tracking, and WTF IS THAT MESSAGE?!
            //throw new InventoryException("Not enough cheese available ( ItemID:" + itemId + ", Remove Amount:" + (quantity - remremove) + "| Current Amount:" + quantity + ")");
        }
    }

    public static void move(final ChannelClient c, final Inventory inventory, final byte src, final byte dst) {
        if (src < 0 || dst < 0) {
            return;
        }
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();
        final IItem source = inventory.getItem(src);
        final IItem initialTarget = inventory.getItem(dst);
        if (source == null) {
            return;
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        final short oldsrcQ = source.getQuantity();
        final short slotMax = ii.getSlotMax(source.getItemId());
        inventory.move(src, dst, slotMax);

        if (!inventory.getType().equals(InventoryType.EQUIP) && initialTarget != null
                && initialTarget.getItemId() == source.getItemId()
                && initialTarget.getExpiration() == source.getExpiration()
                && !GameConstants.isThrowingStar(source.getItemId())
                && !GameConstants.isBullet(source.getItemId())) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                c.write(ChannelPackets.moveAndMergeWithRestInventoryItem(inventory.getType(), src, dst, (short) ((olddstQ
                        + oldsrcQ) - slotMax), slotMax));
            } else {
                c.write(ChannelPackets.moveAndMergeInventoryItem(inventory.getType(), src, dst, ((Item) inventory.getItem(dst)).getQuantity()));
            }
        } else {
            c.write(ChannelPackets.moveInventoryItem(inventory.getType(), src, dst));
        }
    }

    public static void equip(final ChannelClient c, final byte src, byte dst) {
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();
        final ChannelCharacter chr = c.getPlayer();
        final ActivePlayerStats statst = chr.getStats();
        Equip source = (Equip) chr.getEquipInventory().getItem(src);
        Equip target = (Equip) chr.getEquippedItemsInventory().getItem(dst);

        if (source == null) {
            c.write(ChannelPackets.enableActions());
            return;
        }

        final Map<String, Integer> stats = ii.getEquipStats(source.getItemId());
        if (dst < -99 && stats.get("cash") == 0) {
            c.write(ChannelPackets.enableActions());
            return;
        }
        if (!ii.canEquip(stats, source.getItemId(), chr.getLevel(), chr.getJobId(), chr.getFame(), statst.getTotalStr(), statst.getTotalDex(), statst.getTotalLuk(), statst.getTotalInt())) {
            c.write(ChannelPackets.enableActions());
            return;
        }
        if (GameConstants.isWeapon(source.getItemId()) && (dst != -10 && dst
                != -11)) {
            ////AutobanManager.getInstance().autoban(c, "Equipment hack, itemid " + source.getItemId() + " to slot " + dst);
            return;
        }
        if (GameConstants.isKatara(source.getItemId())) {
            dst = (byte) -10; //shield slot
        }
        if (GameConstants.isEvanDragonItem(source.getItemId()) && (chr.getJobId()
                < 2200 || chr.getJobId() > 2218)) {
            c.write(ChannelPackets.enableActions());
            return;
        }
        switch (dst) {
            case -6: { // Top
                final IItem top = chr.getEquipInventory().getItem((byte) -5);
                if (top != null && GameConstants.isOverall(top.getItemId())) {
                    if (chr.getEquipInventory().isFull()) {
                        c.write(ChannelPackets.getInventoryFull());
                        c.write(ChannelPackets.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getEquipInventory().getNextFreeSlot());
                }
                break;
            }
            case -5: {
                final IItem top = chr.getEquipInventory().getItem((byte) -5);
                final IItem bottom = chr.getEquipInventory().getItem((byte) -6);
                if (top != null && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getEquipInventory().isFull(bottom
                            != null && GameConstants.isOverall(source.getItemId()) ? 1 : 0)) {
                        c.write(ChannelPackets.getInventoryFull());
                        c.write(ChannelPackets.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getEquipInventory().getNextFreeSlot());
                }
                if (bottom != null
                        && GameConstants.isOverall(source.getItemId())) {
                    if (chr.getEquipInventory().isFull()) {
                        c.write(ChannelPackets.getInventoryFull());
                        c.write(ChannelPackets.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, chr.getEquipInventory().getNextFreeSlot());
                }
                break;
            }
            case -10: { // Weapon
                IItem weapon = chr.getEquippedItemsInventory().getItem((byte) -11);
                if (GameConstants.isKatara(source.getItemId())) {
                    if ((chr.getJobId() != 900 && (chr.getJobId() < 430
                            || chr.getJobId() > 434)) || weapon == null
                            || !GameConstants.isDagger(weapon.getItemId())) {
                        c.write(ChannelPackets.getInventoryFull());
                        c.write(ChannelPackets.getShowInventoryFull());
                        return;
                    }
                } else if (weapon != null
                        && GameConstants.isTwoHanded(weapon.getItemId())) {
                    if (chr.getEquipInventory().isFull()) {
                        c.write(ChannelPackets.getInventoryFull());
                        c.write(ChannelPackets.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, chr.getEquipInventory().getNextFreeSlot());
                }
                break;
            }
            case -11: { // Shield
                IItem shield = chr.getEquippedItemsInventory().getItem((byte) -10);
                if (shield != null
                        && GameConstants.isTwoHanded(source.getItemId())) {
                    if (chr.getEquipInventory().isFull()) {
                        c.write(ChannelPackets.getInventoryFull());
                        c.write(ChannelPackets.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, chr.getEquipInventory().getNextFreeSlot());
                }
                break;
            }
        }
        source = (Equip) chr.getEquipInventory().getItem(src); // Equip
        target = (Equip) chr.getEquipInventory().getItem(dst); // Currently equipping

        chr.getEquipInventory().removeSlot(src);
        if (target != null) {
            chr.getEquippedItemsInventory().removeSlot(dst);
        }
        source.setPosition(dst);
        chr.getEquipInventory().addFromDb(source);
        if (target != null) {
            target.setPosition(src);
            chr.getEquipInventory().addFromDb(target);
        }
        if (chr.getBuffedValue(BuffStat.BOOSTER) != null
                && GameConstants.isWeapon(source.getItemId())) {
            chr.cancelBuffStats(BuffStat.BOOSTER);
        }
        c.write(ChannelPackets.moveInventoryItem(InventoryType.EQUIP, src, dst, (byte) 2));
        chr.equipChanged();

        if (stats.get("equipTradeBlock") == 1) { // Block trade when equipped.
            byte flag = source.getFlag();
            if (!ItemFlag.UNTRADEABLE.check(flag)) {
                flag |= ItemFlag.UNTRADEABLE.getValue();
                source.setFlag(flag);
                c.write(ChannelPackets.updateSpecialItemUse(target, GameConstants.getInventoryType(source.getItemId()).asByte()));
            }
        }
    }

    public static void unequip(final ChannelClient c, final short src, final short dst) {
        final ChannelCharacter player = c.getPlayer();
        Equip source = (Equip) player.getEquippedItemsInventory().getItem(src);
        Equip target = (Equip) player.getEquipInventory().getItem(dst);

        if (dst < 0 || source == null) {
            return;
        }
        if (target != null && src <= 0) { // do not allow switching with equip
            c.write(ChannelPackets.getInventoryFull());
            return;
        }
        player.getEquippedItemsInventory().removeSlot(src);
        if (target != null) {
            player.getEquipInventory().removeSlot(dst);
        }
        source.setPosition(dst);
        player.getEquipInventory().addFromDb(source);
        if (target != null) {
            target.setPosition(src);
            player.getEquippedItemsInventory().addFromDb(target);
        }

        if (player.getBuffedValue(BuffStat.BOOSTER) != null
                && GameConstants.isWeapon(source.getItemId())) {
            player.cancelBuffStats(BuffStat.BOOSTER);
        }

        c.write(ChannelPackets.moveInventoryItem(InventoryType.EQUIP, src, dst, (byte) 1));
        player.equipChanged();
    }

    public static void drop(final ChannelClient c, Inventory inventory, final short src, final short quantity) {
        final ItemInfoProvider ii = ItemInfoProvider.getInstance();

        final IItem source = inventory.getItem(src);
        if (quantity < 0 || source == null
                || GameConstants.isPet(source.getItemId()) || quantity == 0
                && !GameConstants.isThrowingStar(source.getItemId())
                && !GameConstants.isBullet(source.getItemId())) {
            c.write(ChannelPackets.enableActions());
            return;
        }
        final byte flag = source.getFlag();
        if (ItemFlag.LOCK.check(flag)) { // hack
            c.write(ChannelPackets.enableActions());
            return;
        }
        final ChannelCharacter player = c.getPlayer();
        final Point dropPos = new Point(player.getPosition());

        if (quantity < source.getQuantity()
                && !GameConstants.isThrowingStar(source.getItemId())
                && !GameConstants.isBullet(source.getItemId())) {
            final IItem target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.write(ChannelPackets.dropInventoryItemUpdate(inventory.getType(), source));

            if (ii.isDropRestricted(target.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    player.getMap().spawnItemDrop(player, player, target, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    player.getMap().spawnItemDrop(player, player, target, dropPos, true, true);
                } else {
                    player.getMap().disappearingItemDrop(player, player, target, dropPos);
                }
            } else {
                if (ItemFlag.UNTRADEABLE.check(flag)) {
                    player.getMap().disappearingItemDrop(player, player, target, dropPos);
                } else {
                    player.getMap().spawnItemDrop(player, player, target, dropPos, true, true);
                }
            }
        } else {
            inventory.removeSlot(src);
            c.write(ChannelPackets.dropInventoryItem(inventory.getType(), src));
            if (src < 0) {
                player.equipChanged();
            }
            if (ii.isDropRestricted(source.getItemId())) {
                if (ItemFlag.KARMA_EQ.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_EQ.getValue()));
                    player.getMap().spawnItemDrop(player, player, source, dropPos, true, true);
                } else if (ItemFlag.KARMA_USE.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.KARMA_USE.getValue()));
                    player.getMap().spawnItemDrop(player, player, source, dropPos, true, true);
                } else {
                    player.getMap().disappearingItemDrop(player, player, source, dropPos);
                }
            } else {
                if (ItemFlag.UNTRADEABLE.check(flag)) {
                    player.getMap().disappearingItemDrop(player, player, source, dropPos);
                } else {
                    player.getMap().spawnItemDrop(player, player, source, dropPos, true, true);
                }
            }
        }
    }
}
