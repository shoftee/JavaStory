package javastory.game;

import java.io.Serializable;

import javastory.channel.client.Pet;
import javastory.tools.Randomizer;

public class Item implements IItem, Serializable {

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

	@Override
	public IItem copy() {
		final Item ret = new Item(this.id, this.position, this.quantity, this.flag);
		ret.petId = this.petId;
		ret.pet = this.pet;
		ret.owner = this.owner;
		ret.GameMaster_log = this.GameMaster_log;
		ret.expiration = this.expiration;
		return ret;
	}

	@Override
	public final void setPosition(final short position) {
		this.position = position;
		if (this.pet != null) {
			this.pet.setInventoryPosition(position);
		}
	}

	@Override
	public void setQuantity(final short quantity) {
		this.quantity = quantity;
	}

	@Override
	public final int getItemId() {
		return this.id;
	}

	@Override
	public final short getPosition() {
		return this.position;
	}

	@Override
	public final byte getFlag() {
		return this.flag;
	}

	@Override
	public final short getQuantity() {
		return this.quantity;
	}

	@Override
	public ItemType getType() {
		return ItemType.NORMAL_ITEM; // An Item
	}

	@Override
	public final String getOwner() {
		return this.owner;
	}

	@Override
	public final void setOwner(final String owner) {
		this.owner = owner;
	}

	@Override
	public final void setFlag(final byte flag) {
		this.flag = flag;
	}

	@Override
	public final long getExpiration() {
		return this.expiration;
	}

	@Override
	public final void setExpiration(final long expire) {
		this.expiration = expire;
	}

	@Override
	public final String getGMLog() {
		return this.GameMaster_log;
	}

	@Override
	public void setGMLog(final String GameMaster_log) {
		this.GameMaster_log = GameMaster_log;
	}

	@Override
	public final int getUniqueId() {
		return this.uniqueid;
	}

	@Override
	public final void setUniqueId(final int id) {
		this.uniqueid = id;
	}

	@Override
	public final Pet getPet() {
		return this.pet;
	}

	public final void setPet(final Pet pet) {
		this.pet = pet;
	}

	@Override
	public int compareTo(final IItem other) {
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

	@Override
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