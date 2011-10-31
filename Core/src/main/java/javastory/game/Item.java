package javastory.game;

import java.io.Serializable;

import javastory.channel.client.Pet;
import javastory.tools.Randomizer;

public class Item implements Serializable, Comparable<Item> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2306229158408260452L;

	private final int id;
	private short position;
	private short quantity;
	private byte flag;
	
	private int uniqueid = 0;

	private long expiration = -1;
	
	private int cashId;
	private int petId = -1;
	private Pet pet = null;

	private String owner = "";
	private String GameMaster_log = null;
	
	public Item(final int id, final short position, final short quantity,
			final byte flag) {
		super();
		this.id = id;
		this.position = position;
		this.quantity = quantity;
		this.pet = null;
		this.flag = flag;
	}

	public Item copy() {
		final Item ret = new Item(this.id, this.position, this.quantity, this.flag);
		ret.petId = this.petId;
		ret.pet = this.pet;
		ret.owner = this.owner;
		ret.GameMaster_log = this.GameMaster_log;
		ret.expiration = this.expiration;
		return ret;
	}

	public final void setPosition(final short position) {
		this.position = position;
		if (this.pet != null) {
			this.pet.setInventoryPosition(position);
		}
	}

	public void setQuantity(final short quantity) {
		this.quantity = quantity;
	}

	public final int getItemId() {
		return this.id;
	}

	public final short getPosition() {
		return this.position;
	}

	public final byte getFlag() {
		return this.flag;
	}

	public final short getQuantity() {
		return this.quantity;
	}

	public ItemType getType() {
		return ItemType.NORMAL_ITEM; // An Item
	}

	public final String getOwner() {
		return this.owner;
	}

	public final void setOwner(final String owner) {
		this.owner = owner;
	}

	public final void setFlag(final byte flag) {
		this.flag = flag;
	}

	public final long getExpiration() {
		return this.expiration;
	}

	public final void setExpiration(final long expire) {
		this.expiration = expire;
	}

	public final String getGMLog() {
		return this.GameMaster_log;
	}

	public void setGMLog(final String GameMaster_log) {
		this.GameMaster_log = GameMaster_log;
	}

	public final int getUniqueId() {
		return this.uniqueid;
	}

	public final void setUniqueId(final int id) {
		this.uniqueid = id;
	}

	public final Pet getPet() {
		return this.pet;
	}

	public final void setPet(final Pet pet) {
		this.pet = pet;
	}

	@Override
	public int compareTo(final Item other) {
		final int absPosition = Math.abs(this.position);
		final int otherAbsPosition = Math.abs(other.getPosition());
		if (absPosition < otherAbsPosition) {
			return -1;
		} else if (absPosition == otherAbsPosition) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public String toString() {
		return "Item: " + this.id + " quantity: " + this.quantity;
	}

	public int getCashId() {
		if (this.cashId == 0) {
			this.cashId = Randomizer.nextInt(Integer.MAX_VALUE) + 1;
		}
		return this.cashId;
	}

	public int getPetId() {
		return this.petId;
	}

	public void setPetId(final int petId) {
		this.petId = petId;
	}
}