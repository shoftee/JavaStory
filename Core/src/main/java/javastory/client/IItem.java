package javastory.client;

import javastory.channel.client.Pet;


public interface IItem extends Comparable<IItem> {

	ItemType getType();

	short getPosition();

	byte getFlag();

	short getQuantity();

	String getOwner();

	String getGMLog();

	int getItemId();

	Pet getPet();

	int getUniqueId();

	IItem copy();

	long getExpiration();

	void setFlag(byte flag);

	void setUniqueId(int id);

	void setPosition(short position);

	void setExpiration(long expire);

	void setOwner(String owner);

	void setGMLog(String GameMaster_log);

	void setQuantity(short quantity);

	// Cash Item Information
	int getCashId();
}