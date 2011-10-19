package client;

import javastory.game.GameConstants;
import javastory.game.InventoryType;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

public class Inventory implements Iterable<IItem>, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 4705767500488434915L;
	
	private static final int MAX_CAPACITY = 96;
    //
    private Map<Short, IItem> inventory;
    private byte capacity = 0;
    private InventoryType type;

    /** Creates a new instance of Inventory */
    public Inventory(InventoryType type) {
        this.inventory = Maps.newHashMap();
        this.type = type;
    }

    public void addSlot(byte slot) {
        this.capacity += slot;

        if (capacity > MAX_CAPACITY) {
            capacity = MAX_CAPACITY;
        }
    }

    public byte getSlotLimit() {
        return capacity;
    }

    public void setSlotLimit(byte slot) {
        if (slot > MAX_CAPACITY) {
            slot = MAX_CAPACITY;
        }
        capacity = slot;
    }

    public IItem findById(int itemId) {
        for (IItem item : inventory.values()) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public int countById(int itemId) {
        int quantity = 0;
        for (IItem item : inventory.values()) {
            if (item.getItemId() == itemId) {
                quantity += item.getQuantity();
            }
        }
        return quantity;
    }

    public List<IItem> listById(int itemId) {
        List<IItem> ret = new ArrayList<>();
        for (IItem item : inventory.values()) {
            if (item.getItemId() == itemId) {
                ret.add(item);
            }
        }
        // the linkedhashmap does impose insert order as returned order but we can not guarantee that this is still the
        // correct order - blargh, we could empty the map and reinsert in the correct order after each inventory
        // addition, or we could use an array/list, it's only 255 entries anyway...
        if (ret.size() > 1) {
            Collections.sort(ret);
        }
        return ret;
    }

    public short addItem(IItem item) {
        short slotId = getNextFreeSlot();
        if (slotId < 0) {
            return -1;
        }
        inventory.put(slotId, item);
        item.setPosition(slotId);
        return slotId;
    }

    public void addFromDb(IItem item) {
        if (item.getPosition() < 0 && !type.equals(InventoryType.EQUIPPED)) {
            // This causes a lot of stuck problem, until we are done with position checking
            return;
        }
        inventory.put(item.getPosition(), item);
    }

    public void move(short sSlot, short dSlot, short slotMax) {
        if (dSlot > capacity) {
            return;
        }
        Item source = (Item) inventory.get(sSlot);
        Item target = (Item) inventory.get(dSlot);
        if (source == null) {
            throw new IllegalStateException("The source slot is empty.");
        }
        if (target == null) {
            source.setPosition(dSlot);
            inventory.put(dSlot, source);
            inventory.remove(sSlot);
        } else if (target.getItemId() == source.getItemId()
                && !GameConstants.isThrowingStar(source.getItemId())
                && !GameConstants.isBullet(source.getItemId())) {
            if (type.asByte() == InventoryType.EQUIP.asByte()) {
                swap(target, source);
            }
            if (source.getQuantity() + target.getQuantity() > slotMax) {
                source.setQuantity((short) ((source.getQuantity()
                        + target.getQuantity()) - slotMax));
                target.setQuantity(slotMax);
            } else {
                target.setQuantity((short) (source.getQuantity()
                        + target.getQuantity()));
                inventory.remove(sSlot);
            }
        } else {
            swap(target, source);
        }
    }

    private void swap(IItem source, IItem target) {
        inventory.remove(source.getPosition());
        inventory.remove(target.getPosition());
        short swapPos = source.getPosition();
        source.setPosition(target.getPosition());
        target.setPosition(swapPos);
        inventory.put(source.getPosition(), source);
        inventory.put(target.getPosition(), target);
    }

    public IItem getItem(short slot) {
        return inventory.get(slot);
    }

    public void removeItem(short slot) {
        removeItem(slot, (short) 1, false);
    }

    public void removeItem(short slot, short quantity, boolean allowZero) {
        IItem item = inventory.get(slot);
        if (item == null) { // TODO is it ok not to throw an exception here?
            return;
        }
        item.setQuantity((short) (item.getQuantity() - quantity));
        if (item.getQuantity() < 0) {
            item.setQuantity((short) 0);
        }
        if (item.getQuantity() == 0 && !allowZero) {
            removeSlot(slot);
        }
    }

    public void removeSlot(short slot) {
        inventory.remove(slot);
    }

    public boolean isFull() {
        return inventory.size() >= capacity;
    }

    public boolean isFull(int margin) {
        return inventory.size() + margin >= capacity;
    }

    public short getNextFreeSlot() {
        if (isFull()) {
            return -1;
        }
        for (short i = 1; i <= capacity; i++) {
            if (!inventory.keySet().contains(i)) {
                return i;
            }
        }
        return -1;
    }

    public short getNumFreeSlot() {
        if (isFull()) {
            return 0;
        }
        byte free = 0;
        for (short i = 1; i <= capacity; i++) {
            if (!inventory.keySet().contains(i)) {
                free++;
            }
        }
        return free;
    }

    public InventoryType getType() {
        return type;
    }

    @Override
    public Iterator<IItem> iterator() {
        return Collections.unmodifiableCollection(inventory.values()).iterator();
    }
}
