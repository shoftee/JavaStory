package javastory.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Inventory implements Iterable<Item>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4705767500488434915L;

	private static final int MAX_CAPACITY = 96;
	//
	private final Map<Short, Item> inventory;
	private byte capacity = 0;
	private final InventoryType type;

	/** Creates a new instance of Inventory */
	public Inventory(final InventoryType type) {
		this.inventory = Maps.newHashMap();
		this.type = type;
	}

	public void addSlot(final byte slot) {
		this.capacity += slot;

		if (this.capacity > MAX_CAPACITY) {
			this.capacity = MAX_CAPACITY;
		}
	}

	public byte getSlotLimit() {
		return this.capacity;
	}

	public void setSlotLimit(byte slot) {
		if (slot > MAX_CAPACITY) {
			slot = MAX_CAPACITY;
		}
		this.capacity = slot;
	}

	public Item findById(final int itemId) {
		for (final Item item : this.inventory.values()) {
			if (item.getItemId() == itemId) {
				return item;
			}
		}
		return null;
	}

	public int countById(final int itemId) {
		int quantity = 0;
		for (final Item item : this.inventory.values()) {
			if (item.getItemId() == itemId) {
				quantity += item.getQuantity();
			}
		}
		return quantity;
	}

	public List<Item> listById(final int itemId) {
		final List<Item> ret = Lists.newArrayList();
		for (final Item item : this.inventory.values()) {
			if (item.getItemId() == itemId) {
				ret.add(item);
			}
		}
		// the linkedhashmap does impose insert order as returned order but we
		// can not guarantee that this is still the
		// correct order - blargh, we could empty the map and reinsert in the
		// correct order after each inventory
		// addition, or we could use an array/list, it's only 255 entries
		// anyway...
		if (ret.size() > 1) {
			Collections.sort(ret);
		}
		return ret;
	}

	public short addItem(final Item item) {
		final short slotId = this.getNextFreeSlot();
		if (slotId < 0) {
			return -1;
		}
		this.inventory.put(slotId, item);
		item.setPosition(slotId);
		return slotId;
	}

	public void addFromDb(final Item item) {
		if (item.getPosition() < 0 && !this.type.equals(InventoryType.EQUIPPED)) {
			// This causes a lot of stuck problem, until we are done with
			// position checking
			return;
		}
		this.inventory.put(item.getPosition(), item);
	}

	public void move(final short sSlot, final short dSlot, final short slotMax) {
		if (dSlot > this.capacity) {
			return;
		}
		final Item source = this.inventory.get(sSlot);
		final Item target = this.inventory.get(dSlot);
		if (source == null) {
			throw new IllegalStateException("The source slot is empty.");
		}
		if (target == null) {
			source.setPosition(dSlot);
			this.inventory.put(dSlot, source);
			this.inventory.remove(sSlot);
		} else if (target.getItemId() == source.getItemId()
				&& !GameConstants.isThrowingStar(source.getItemId())
				&& !GameConstants.isBullet(source.getItemId())) {
			if (this.type.asNumber() == InventoryType.EQUIP.asNumber()) {
				this.swap(target, source);
			}
			if (source.getQuantity() + target.getQuantity() > slotMax) {
				source.setQuantity((short) (source.getQuantity()
						+ target.getQuantity() - slotMax));
				target.setQuantity(slotMax);
			} else {
				target.setQuantity((short) (source.getQuantity()
						+ target.getQuantity()));
				this.inventory.remove(sSlot);
			}
		} else {
			this.swap(target, source);
		}
	}

	private void swap(final Item source, final Item target) {
		this.inventory.remove(source.getPosition());
		this.inventory.remove(target.getPosition());
		final short swapPos = source.getPosition();
		source.setPosition(target.getPosition());
		target.setPosition(swapPos);
		this.inventory.put(source.getPosition(), source);
		this.inventory.put(target.getPosition(), target);
	}

	public Item getItem(final short slot) {
		return this.inventory.get(slot);
	}

	public void removeItem(final short slot) {
		this.removeItem(slot, (short) 1, false);
	}

	public void removeItem(final short slot, final short quantity, final boolean allowZero) {
		final Item item = this.inventory.get(slot);
		if (item == null) { // TODO is it ok not to throw an exception here?
			return;
		}
		item.setQuantity((short) (item.getQuantity() - quantity));
		if (item.getQuantity() < 0) {
			item.setQuantity((short) 0);
		}
		if (item.getQuantity() == 0 && !allowZero) {
			this.removeSlot(slot);
		}
	}

	public void removeSlot(final short slot) {
		this.inventory.remove(slot);
	}

	public boolean isFull() {
		return this.inventory.size() >= this.capacity;
	}

	public boolean isFull(final int margin) {
		return this.inventory.size() + margin >= this.capacity;
	}

	public short getNextFreeSlot() {
		if (this.isFull()) {
			return -1;
		}
		for (short i = 1; i <= this.capacity; i++) {
			if (!this.inventory.keySet().contains(i)) {
				return i;
			}
		}
		return -1;
	}

	public short getNumFreeSlot() {
		if (this.isFull()) {
			return 0;
		}
		byte free = 0;
		for (short i = 1; i <= this.capacity; i++) {
			if (!this.inventory.keySet().contains(i)) {
				free++;
			}
		}
		return free;
	}

	public InventoryType getType() {
		return this.type;
	}

	@Override
	public Iterator<Item> iterator() {
		return Collections.unmodifiableCollection(this.inventory.values())
				.iterator();
	}
}
